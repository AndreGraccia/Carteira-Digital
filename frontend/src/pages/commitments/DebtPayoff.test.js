import { act, Simulate } from 'react-dom/test-utils';
import { createRoot } from 'react-dom/client';
import DebtPayoff from './DebtPayoff';
import ThemeProvider from '../../theme';
import HttpService from '../../services/HttpService';

jest.mock('../../services/HttpService', () => ({ getWithAuth: jest.fn() }));
jest.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key, options) => (options ? `${key}:${JSON.stringify(options)}` : key),
    i18n: { language: 'pt' },
  }),
}));

let root;
let container;

beforeEach(async () => {
  globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  container = document.createElement('div');
  root = createRoot(container);
  HttpService.getWithAuth.mockReset();
  await act(async () =>
    root.render(
      <ThemeProvider>
        <DebtPayoff />
      </ThemeProvider>
    )
  );
});
afterEach(() => act(() => root.unmount()));

const input = () => container.querySelector('input');
const submit = () => container.querySelector('button[type="submit"]');

test('it compares the two orders with paying only the instalments and lists the cheapest order', async () => {
  HttpService.getWithAuth.mockResolvedValue({
    loansWithoutRate: 1,
    plan: {
      baseline: { months: 24, interest: 900, order: [] },
      avalanche: {
        months: 15,
        interest: 520,
        order: [
          { id: '2', label: 'Cartão parcelado - Banco Aurora', month: 9 },
          { id: '1', label: 'Crédito pessoal - Banco Aurora', month: 15 },
        ],
      },
      snowball: { months: 15, interest: 560, order: [] },
      avalancheSaves: 380,
      snowballSaves: 340,
    },
  });

  await act(async () => Simulate.change(input(), { target: { value: '200,50' } }));
  await act(async () => Simulate.submit(submit().closest('form')));

  expect(HttpService.getWithAuth).toHaveBeenCalledWith('/future/debt-payoff?extra=200.5');
  const text = container.textContent;
  expect(text).toContain('commitments.payoff.baseline');
  expect(text).toContain('"count":24');
  expect(text).toContain('commitments.payoff.saves');
  expect(text).toContain('Cartão parcelado - Banco Aurora');
  expect(text).toContain('commitments.payoff.withoutRate');
  expect(text).toContain('commitments.payoff.note');
});

test('an amount it cannot read is refused before any request', async () => {
  await act(async () => Simulate.change(input(), { target: { value: 'dez reais' } }));

  expect(submit().disabled).toBe(true);
  expect(container.textContent).toContain('commitments.payoff.extraInvalid');
  expect(HttpService.getWithAuth).not.toHaveBeenCalled();
});
