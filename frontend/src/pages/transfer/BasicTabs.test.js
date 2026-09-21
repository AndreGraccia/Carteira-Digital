import { act, Simulate } from 'react-dom/test-utils';
import { createRoot } from 'react-dom/client';
import { HelmetProvider } from 'react-helmet-async';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import BasicTabs from './BasicTabs';
import HttpService from '../../services/HttpService';
import { useWallets } from '../../context/WalletContext';

jest.mock('../../services/HttpService', () => ({ postWithAuth: jest.fn() }));
jest.mock('../../context/WalletContext', () => ({ useWallets: jest.fn() }));
jest.mock('notistack', () => ({ enqueueSnackbar: jest.fn() }));
jest.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key) => key, i18n: { language: 'pt' } }) }));

const wallet = {
  id: 1,
  name: 'Principal',
  iban: 'BR9700360305000010009795493P1',
  balance: '100.00',
  source: 'LOCAL',
};
let container;
let root;
let refresh;

beforeEach(() => {
  globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  Object.defineProperty(globalThis, 'crypto', {
    configurable: true,
    value: { randomUUID: () => 'f0a58f88-01a1-4e60-a5bb-876c6b5237e3' },
  });
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);
  refresh = jest.fn().mockResolvedValue(undefined);
  useWallets.mockReturnValue({ wallets: [wallet], transactions: [], refresh, loading: false, error: null });
  HttpService.postWithAuth.mockReset();
});

afterEach(() => {
  act(() => root.unmount());
  container.remove();
});

function renderMode(mode) {
  act(() =>
    root.render(
      <HelmetProvider>
        <MemoryRouter initialEntries={[`/transfers?mode=${mode}`]}>
          <Routes>
            <Route path="/transfers" element={<BasicTabs />} />
            <Route path="/wallets/:id" element={<div>Wallet detail</div>} />
          </Routes>
        </MemoryRouter>
      </HelmetProvider>
    )
  );
}

function amount(value) {
  act(() => Simulate.change(container.querySelector('input[placeholder="0,00"]'), { target: { value } }));
}

function review() {
  act(() => Simulate.submit(container.querySelector('form'), { preventDefault() {} }));
}

function confirmButton() {
  return [...document.querySelectorAll('button')].find((button) => button.textContent === 'app.confirm');
}

test('a withdrawal exceeding the balance never reaches the API', () => {
  renderMode('withdraw');
  amount('100,01');
  review();
  expect(container.textContent).toContain('app.errors.balance');
  expect(confirmButton()).toBeUndefined();
  expect(HttpService.postWithAuth).not.toHaveBeenCalled();
});

test('a withdrawal requires review and sends exact decimal data once', async () => {
  renderMode('withdraw');
  amount('25,50');
  review();
  let finish;
  HttpService.postWithAuth.mockReturnValue(new Promise((resolve) => { finish = resolve; }));
  const confirm = confirmButton();
  act(() => {
    Simulate.click(confirm);
    Simulate.click(confirm);
  });
  expect(HttpService.postWithAuth).toHaveBeenCalledTimes(1);
  expect(HttpService.postWithAuth).toHaveBeenCalledWith(
    '/wallets/withdrawFunds',
    expect.objectContaining({
      amount: '25.50',
      fromWalletIban: wallet.iban,
      toWalletIban: wallet.iban,
      requestId: expect.any(String),
    })
  );
  await act(async () => { finish({ id: 99 }); });
  expect(refresh).toHaveBeenCalledTimes(1);
  expect(container.textContent).toContain('Wallet detail');
});

test('Open Finance accounts are not offered for simulated transfers', () => {
  useWallets.mockReturnValue({
    wallets: [{ ...wallet, id: 2, source: 'OPEN_FINANCE' }],
    transactions: [],
    refresh,
    loading: false,
    error: null,
  });
  renderMode('transfer');
  expect(container.querySelector('form')).toBeNull();
  expect(HttpService.postWithAuth).not.toHaveBeenCalled();
});
