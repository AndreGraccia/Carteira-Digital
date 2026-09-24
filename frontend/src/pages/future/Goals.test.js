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
