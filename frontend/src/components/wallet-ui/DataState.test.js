import { act } from 'react-dom/test-utils';
import { createRoot } from 'react-dom/client';
import { DataState } from '.';
import ThemeProvider from '../../theme';
import { useWallets } from '../../context/WalletContext';

jest.mock('../../context/WalletContext', () => ({ useWallets: jest.fn() }));
jest.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key) => key, i18n: { language: 'pt' } }) }));

let root;
let container;
beforeEach(() => {
  globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);
});
afterEach(() => {
  act(() => root.unmount());
  container.remove();
});
const renderWith = (value) => {
  useWallets.mockReturnValue({ wallets: [], transactions: [], refresh: jest.fn(), error: null, ...value });
  act(() =>
    root.render(
      <ThemeProvider>
        <DataState>
          <p>page</p>
        </DataState>
      </ThemeProvider>
    )
  );
};

test('first load shows a skeleton, not the page', () => {
  renderWith({ loading: true });
  expect(container.querySelector('[aria-busy="true"] .MuiSkeleton-root')).not.toBeNull();
  expect(container.textContent).not.toContain('page');
});

test('a refresh with data on screen keeps the page, marked busy', () => {
  renderWith({ loading: true, wallets: [{ id: 1 }] });
  expect(container.textContent).toContain('page');
  expect(container.querySelector('.MuiSkeleton-root')).toBeNull();
  expect(container.querySelector('[aria-busy="true"]')).not.toBeNull();
});

test('an error without data offers a retry; with data it keeps the page too', () => {
  renderWith({ loading: false, error: 'app.errors.server' });
  expect(container.textContent).toContain('app.retry');
  expect(container.textContent).not.toContain('page');

  renderWith({ loading: false, error: 'app.errors.server', wallets: [{ id: 1 }] });
  expect(container.textContent).toContain('app.retry');
  expect(container.textContent).toContain('page');
});
