import { act, Simulate } from 'react-dom/test-utils';
import { createRoot } from 'react-dom/client';
import { HelmetProvider } from 'react-helmet-async';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import Privacy from './Privacy';
import ThemeProvider from '../../theme';
import HttpService from '../../services/HttpService';
import AuthService from '../../services/AuthService';

jest.mock('../../services/HttpService', () => ({ postWithAuth: jest.fn(), getWithAuth: jest.fn() }));
jest.mock('../../services/AuthService', () => ({ logout: jest.fn() }));
jest.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key) => key, i18n: { language: 'pt' } }) }));

let root;
let container;
const byText = (label) => [...document.querySelectorAll('button')].find((item) => item.textContent === label);

beforeEach(async () => {
  globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);
  HttpService.postWithAuth.mockReset();
  HttpService.getWithAuth.mockReset();
  AuthService.logout.mockReset();
  await act(async () =>
    root.render(
      <MemoryRouter initialEntries={['/privacy']}>
        <HelmetProvider>
          <ThemeProvider>
            <Routes>
              <Route path="/privacy" element={<Privacy />} />
              <Route path="/login" element={<p>login-page</p>} />
            </Routes>
          </ThemeProvider>
        </HelmetProvider>
      </MemoryRouter>
    )
  );
});
afterEach(() => {
  act(() => root.unmount());
  container.remove();
});

async function typePasswordAndConfirm(value) {
  await act(async () => Simulate.click(byText('privacy.deleteButton')));
  const input = document.querySelector('input[type="password"]');
  await act(async () => Simulate.change(input, { target: { value } }));
  await act(async () => Simulate.submit(input.closest('form')));
}

test('a wrong password shows the error and keeps the session', async () => {
  HttpService.postWithAuth.mockRejectedValue({ response: { status: 403, data: { code: 'INVALID_PASSWORD' } } });

  await typePasswordAndConfirm('errada');

  expect(HttpService.postWithAuth).toHaveBeenCalledWith('/account/delete', { password: 'errada' });
  expect(document.body.textContent).toContain('app.errors.INVALID_PASSWORD');
  expect(AuthService.logout).not.toHaveBeenCalled();
});

test('the right password deletes, signs out and goes to the login page', async () => {
  HttpService.postWithAuth.mockResolvedValue(null);

  await typePasswordAndConfirm('Teste@12345');

  expect(AuthService.logout).toHaveBeenCalled();
  expect(container.textContent).toContain('login-page');
});

test('downloading the data saves the export as a JSON file and keeps the session', async () => {
  HttpService.getWithAuth.mockResolvedValue({ profile: { username: 'ana' }, transactions: [] });
  const saved = [];
  URL.createObjectURL = jest.fn(() => 'blob:export');
  URL.revokeObjectURL = jest.fn();
  const click = jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function record() {
    saved.push(this.download);
  });

  await act(async () => Simulate.click(byText('privacy.exportButton')));

  expect(HttpService.getWithAuth).toHaveBeenCalledWith('/account/export');
  expect(saved).toEqual([expect.stringMatching(/^wallia-dados-\d{4}-\d{2}-\d{2}\.json$/)]);
  expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:export');
  expect(AuthService.logout).not.toHaveBeenCalled();
  click.mockRestore();
});

test('a failed download says so', async () => {
  HttpService.getWithAuth.mockRejectedValue({ response: { status: 500 } });

  await act(async () => Simulate.click(byText('privacy.exportButton')));

  expect(document.body.textContent).toContain('app.errors.');
});
