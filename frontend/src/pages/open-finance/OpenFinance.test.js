import { act, Simulate } from 'react-dom/test-utils';
import { createRoot } from 'react-dom/client';
import { HelmetProvider } from 'react-helmet-async';
import OpenFinance from './OpenFinance';
import ThemeProvider from '../../theme';
import HttpService from '../../services/HttpService';
import { useWallets } from '../../context/WalletContext';

jest.mock('../../services/HttpService', () => ({ getWithAuth: jest.fn(), postWithAuth: jest.fn() }));
jest.mock('../../context/WalletContext', () => ({ useWallets: jest.fn() }));
jest.mock('notistack', () => ({ enqueueSnackbar: jest.fn() }));
jest.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key) => key, i18n: { language: 'pt' } }) }));

let root;
let container;
let refresh;
const challenge = () => ({
  pending: true,
  maskedEmail: 'a***@example.test',
  expiresAt: new Date(Date.now() + 600000).toISOString(),
  resendAvailableAt: new Date(Date.now() + 60000).toISOString(),
});
const status = (verification = null) => ({
  status: verification ? 'PENDING_EMAIL_VERIFICATION' : 'DISCONNECTED',
  simulated: true,
  permissions: [],
  verification,
});
const button = (key) => [...document.querySelectorAll('button')].find((item) => item.textContent === key);

beforeEach(() => {
  globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);
  refresh = jest.fn().mockResolvedValue(undefined);
  useWallets.mockReturnValue({ refresh });
  HttpService.getWithAuth.mockReset();
  HttpService.postWithAuth.mockReset();
});
afterEach(() => {
  act(() => root.unmount());
  container.remove();
});
async function renderPage() {
  await act(async () =>
    root.render(
      <HelmetProvider>
        <ThemeProvider>
          <OpenFinance />
        </ThemeProvider>
      </HelmetProvider>
    )
  );
}

test('pending connection reopens its code dialog without sending another email', async () => {
  HttpService.getWithAuth.mockResolvedValue(status(challenge()));
  await renderPage();
  await act(async () => Simulate.click(button('openFinance.verification.enterCode')));
  expect(document.querySelector('input[autocomplete="one-time-code"]')).not.toBeNull();
  expect(HttpService.postWithAuth).not.toHaveBeenCalled();
  expect(button('openFinance.verification.resendIn').disabled).toBe(true);
});

test('confirmation sends one request even on double click and refreshes imported accounts', async () => {
  HttpService.getWithAuth.mockResolvedValue(status(challenge()));
  await renderPage();
  await act(async () => Simulate.click(button('openFinance.verification.enterCode')));
  act(() => Simulate.change(document.querySelector('input'), { target: { value: '12345678' } }));
  let finish;
  HttpService.postWithAuth.mockReturnValue(
    new Promise((resolve) => {
      finish = resolve;
    })
  );
  const confirm = button('openFinance.verification.confirm');
  act(() => {
    Simulate.click(confirm);
    Simulate.click(confirm);
  });
  expect(HttpService.postWithAuth).toHaveBeenCalledTimes(1);
  expect(HttpService.postWithAuth).toHaveBeenCalledWith('/open-finance/connection/confirm', { code: '12345678' });
  HttpService.getWithAuth.mockResolvedValue({ ...status(), status: 'AUTHORISED', accountCount: 2 });
  await act(async () => finish({ accountsSynced: 2, newTransactions: 15 }));
  expect(refresh).toHaveBeenCalledTimes(1);
  expect(container.textContent).toContain('openFinance.sync');
});

test('expired pending code requests a fresh challenge instead of trapping the user', async () => {
  HttpService.getWithAuth.mockResolvedValue(
    status({ ...challenge(), expiresAt: new Date(Date.now() - 1000).toISOString() })
  );
  await renderPage();
  HttpService.postWithAuth.mockResolvedValue(challenge());
  HttpService.getWithAuth.mockResolvedValue(status(challenge()));
  await act(async () => Simulate.click(button('openFinance.connect')));
  expect(HttpService.postWithAuth).toHaveBeenCalledWith('/open-finance/connection/challenge', {});
  expect(document.querySelector('input[autocomplete="one-time-code"]').disabled).toBe(false);
});

test('an invalid code keeps the dialog available with the specific error', async () => {
  HttpService.getWithAuth.mockResolvedValue(status(challenge()));
  await renderPage();
  await act(async () => Simulate.click(button('openFinance.verification.enterCode')));
  act(() => Simulate.change(document.querySelector('input'), { target: { value: '11111111' } }));
  HttpService.postWithAuth.mockRejectedValue({
    response: { status: 400, data: { code: 'OPEN_FINANCE_CODE_INVALID' } },
  });
  await act(async () => Simulate.click(button('openFinance.verification.confirm')));
  expect(document.body.textContent).toContain('app.errors.OPEN_FINANCE_CODE_INVALID');
  expect(button('openFinance.verification.confirm').disabled).toBe(false);
  expect(refresh).not.toHaveBeenCalled();
});

test('without a status the card claims nothing while loading, and a failure offers a retry', async () => {
  let fail;
  HttpService.getWithAuth.mockReturnValue(
    new Promise((_, reject) => {
      fail = reject;
    })
  );
  await renderPage();
  expect(container.querySelector('.MuiSkeleton-root')).not.toBeNull();
  expect(container.textContent).not.toContain('openFinance.participantSandbox');
  expect(container.textContent).not.toContain('openFinance.bankPlaceholder');

  await act(async () => fail({ response: { status: 500 } }));
  expect(container.textContent).toContain('app.errors.server');
  expect(container.textContent).not.toContain('openFinance.participantSandbox');

  HttpService.getWithAuth.mockResolvedValue(status());
  await act(async () => Simulate.click(button('app.retry')));
  expect(container.textContent).not.toContain('app.errors.server');
  expect(container.textContent).toContain('openFinance.contractSandbox');
  // The notice is translated from the mode, never the raw PT text sent by the server.
  expect(container.textContent).toContain('openFinance.notice.contract');
});
