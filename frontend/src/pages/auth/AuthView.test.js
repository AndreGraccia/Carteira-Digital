import { act, Simulate } from 'react-dom/test-utils';
import { createRoot } from 'react-dom/client';
import { HelmetProvider } from 'react-helmet-async';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import AuthView from './AuthView';
import ThemeProvider from '../../theme';
import AuthService from '../../services/AuthService';

jest.mock('../../services/AuthService', () => ({
  signup: jest.fn(),
  login: jest.fn(),
  verifyEmail: jest.fn(),
  resendCode: jest.fn(),
  getCurrentUser: jest.fn(),
}));
jest.mock('../../services/HttpService', () => ({ safeReturnPath: (from) => from || '/' }));
jest.mock('../../context/WalletContext', () => ({ useWallets: jest.fn() }));
jest.mock('notistack', () => ({ enqueueSnackbar: jest.fn() }));
jest.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key, values) => (values ? `${key}:${Object.values(values).join('|')}` : key),
    i18n: { language: 'pt', resolvedLanguage: 'pt', changeLanguage: jest.fn() },
  }),
}));

let root;
let container;
const text = () => container.textContent;
const button = (label) => [...container.querySelectorAll('button')].find((item) => item.textContent.startsWith(label));
const type = (name, value) =>
  act(() => Simulate.change(container.querySelector(`input[name="${name}"]`), { target: { value } }));

beforeEach(() => {
  globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);
  Object.values(AuthService).forEach((fn) => fn.mockReset());
});
afterEach(() => {
  act(() => root.unmount());
  container.remove();
});
async function renderAt(signup) {
  await act(async () =>
    root.render(
      <MemoryRouter initialEntries={[signup ? '/signup' : '/login']}>
        <HelmetProvider>
          <ThemeProvider>
            <Routes>
              <Route path="/signup" element={<AuthView signup />} />
              <Route path="/login" element={<AuthView />} />
              <Route path="/" element={<p>HOME</p>} />
            </Routes>
          </ThemeProvider>
        </HelmetProvider>
      </MemoryRouter>
    )
  );
}
async function signUp() {
  await renderAt(true);
  type('firstName', 'Ana');
  type('lastName', 'Lima');
  type('username', ' ana ');
  type('email', 'ana@example.test');
  type('password', 'senha-forte-123');
  await act(async () => Simulate.submit(container.querySelector('form')));
}

test('sign-up goes to the code screen, and a valid code signs in with the password just typed', async () => {
  AuthService.signup.mockResolvedValue({ maskedEmail: 'a***@example.test', resendAvailableAt: null });
  await signUp();
  expect(AuthService.signup).toHaveBeenCalledWith(expect.not.objectContaining({ roles: expect.anything() }));
  expect(text()).toContain('auth.verify.sentTo:a***@example.test');
  const code = container.querySelector('input[name="code"]');
  expect(code.getAttribute('inputmode')).toBe('numeric');
  expect(code.getAttribute('autocomplete')).toBe('one-time-code');
  expect(sessionStorage.length + localStorage.length).toBe(0);

  AuthService.verifyEmail.mockResolvedValue({});
  AuthService.login.mockResolvedValue({ token: 't' });
  type('code', '1234-5678');
  await act(async () => Simulate.submit(container.querySelector('form')));

  expect(AuthService.verifyEmail).toHaveBeenCalledWith('ana', '12345678');
  expect(AuthService.login).toHaveBeenCalledWith({ username: 'ana', password: 'senha-forte-123' });
  expect(text()).toContain('HOME');
});

test('a wrong code shows its message and does not sign in', async () => {
  AuthService.signup.mockResolvedValue({ maskedEmail: 'a***@example.test', resendAvailableAt: null });
  await signUp();
  AuthService.verifyEmail.mockRejectedValue({ response: { status: 400, data: { code: 'EMAIL_CODE_INVALID' } } });
  type('code', '11111111');
  await act(async () => Simulate.submit(container.querySelector('form')));

  expect(text()).toContain('app.errors.EMAIL_CODE_INVALID');
  expect(AuthService.login).not.toHaveBeenCalled();
});

test('resend asks for a new code and counts down until the next one is allowed', async () => {
  AuthService.signup.mockResolvedValue({ maskedEmail: 'a***@example.test', resendAvailableAt: null });
  await signUp();
  AuthService.resendCode.mockResolvedValue({
    maskedEmail: null,
    resendAvailableAt: new Date(Date.now() + 60000).toISOString(),
  });
  await act(async () => Simulate.click(button('auth.verify.resend')));

  expect(AuthService.resendCode).toHaveBeenCalledWith('ana');
  expect(text()).toContain('auth.verify.resent');
  const wait = button('auth.verify.resendIn');
  expect(wait.disabled).toBe(true);
  expect(wait.textContent).toMatch(/auth\.verify\.resendIn:(59|60)/);
});

test('login into an unverified account leads to the same code screen', async () => {
  await renderAt(false);
  type('username', 'ana');
  type('password', 'senha-forte-123');
  AuthService.login.mockRejectedValue({ response: { status: 403, data: { code: 'EMAIL_NOT_VERIFIED' } } });
  await act(async () => Simulate.submit(container.querySelector('form')));

  expect(text()).toContain('auth.verify.sentUnknown:ana');
  expect(button('auth.verify.resend').disabled).toBe(false);
});

test('a taken username is reported as such', async () => {
  AuthService.signup.mockRejectedValue({ response: { status: 409, data: { code: 'USERNAME_TAKEN' } } });
  await signUp();
  expect(text()).toContain('app.errors.USERNAME_TAKEN');
});
