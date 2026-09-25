import { act } from 'react-dom/test-utils';
import { createRoot } from 'react-dom/client';
import Goals from './Goals';
import ThemeProvider from '../../theme';
import HttpService from '../../services/HttpService';

jest.mock('../../services/HttpService', () => ({ getWithAuth: jest.fn() }));
jest.mock('../../context/WalletContext', () => ({ useWallets: jest.fn() }));
jest.mock('notistack', () => ({ useSnackbar: () => ({ enqueueSnackbar: jest.fn(), closeSnackbar: jest.fn() }) }));
jest.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key) => key, i18n: { language: 'pt' } }) }));

test('"no goals" is never shown before the list arrives', async () => {
  globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  const container = document.createElement('div');
  const root = createRoot(container);
  let answer;
  HttpService.getWithAuth.mockReturnValue(
    new Promise((done) => {
      answer = done;
    })
  );
  await act(async () =>
    root.render(
      <ThemeProvider>
        <Goals onChanged={jest.fn()} />
      </ThemeProvider>
    )
  );
  expect(container.textContent).not.toContain('future.goals.empty');
  expect(container.querySelector('.MuiSkeleton-root')).not.toBeNull();

  await act(async () => answer([]));
  expect(container.textContent).toContain('future.goals.empty');
  act(() => root.unmount());
});

test('each goal says what 12 months at the CDI become, after inflation, with the Banco Central date', async () => {
  globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  const container = document.createElement('div');
  const root = createRoot(container);
  HttpService.getWithAuth.mockImplementation((url) =>
    Promise.resolve(
      url === '/market/overview'
        ? {
            indicators: [
              { code: 'CDI', value: 13.65, referenceDate: '2026-09-24' },
              { code: 'IPCA_12M', value: 4.22, referenceDate: '2026-08-01' },
            ],
          }
        : [{ id: 1, name: 'Reserva', monthlyAmount: '200.00', dayOfMonth: 10, nextDate: '2026-10-10' }]
    )
  );
  await act(async () =>
    root.render(
      <ThemeProvider>
        <Goals onChanged={jest.fn()} />
      </ThemeProvider>
    )
  );
  expect(container.textContent).toContain('future.goals.inAYear');

  act(() => root.unmount());
});
