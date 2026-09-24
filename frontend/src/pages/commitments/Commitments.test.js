import { act, Simulate } from 'react-dom/test-utils';
import { createRoot } from 'react-dom/client';
import { HelmetProvider } from 'react-helmet-async';
import { MemoryRouter } from 'react-router-dom';
import Commitments from './Commitments';
import ThemeProvider from '../../theme';
import HttpService from '../../services/HttpService';

jest.mock('../../services/HttpService', () => ({ getWithAuth: jest.fn() }));
jest.mock('../../context/WalletContext', () => ({ useWallets: jest.fn() }));
jest.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key) => key, i18n: { language: 'pt' } }) }));

test('"try again" clears the error and shows it is loading right away', async () => {
  globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  const container = document.createElement('div');
  const root = createRoot(container);
  HttpService.getWithAuth.mockRejectedValue({ response: { status: 500 } });
  await act(async () =>
    root.render(
      <MemoryRouter>
        <HelmetProvider>
          <ThemeProvider>
            <Commitments />
          </ThemeProvider>
        </HelmetProvider>
      </MemoryRouter>
    )
  );
  expect(container.textContent).toContain('app.errors.server');

  HttpService.getWithAuth.mockReturnValue(new Promise(() => {}));
  const retry = [...container.querySelectorAll('button')].find((item) => item.textContent === 'app.retry');
  await act(async () => Simulate.click(retry));
  expect(container.textContent).not.toContain('app.errors.server');
  expect(container.querySelector('.MuiSkeleton-root')).not.toBeNull();
  act(() => root.unmount());
});
