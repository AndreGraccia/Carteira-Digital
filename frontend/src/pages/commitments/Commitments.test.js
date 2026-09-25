import { act, Simulate } from 'react-dom/test-utils';
import { createRoot } from 'react-dom/client';
import { HelmetProvider } from 'react-helmet-async';
import { MemoryRouter } from 'react-router-dom';
import Commitments, { monthlyFromAnnual } from './Commitments';
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

test('the minimum payment warns about revolving credit and loans show CET and the market reference', async () => {
  globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  const container = document.createElement('div');
  const root = createRoot(container);
  const commitments = {
    cards: [
      {
        id: 1,
        name: 'Cartão Aurora',
        institution: 'Banco Aurora',
        lastDigits: '1234',
        creditLimit: '5000',
        committedLimit: '1200',
        closingDay: 25,
        dueDay: 5,
        invoices: [],
        nextBillDueDate: '2026-10-05',
        nextBillTotal: '1200.00',
        nextBillMinimum: '180.00',
      },
    ],
    loans: [
      {
        id: 2,
        product: 'Crédito pessoal',
        institution: 'Banco Aurora',
        contractedAmount: '10000',
        outstandingBalance: '6000',
        installmentAmount: '500',
        installmentsTotal: 24,
        installmentsPaid: 12,
        installmentsRemaining: 12,
        annualRate: '26.82',
        cet: '31.5',
      },
    ],
  };
  const market = {
    rates: [
      { modality: 'CARD_REVOLVING', periodStart: '2026-09-04', median: '3.3', bankRate: null },
      { modality: 'PERSONAL_LOAN', periodStart: '2026-09-04', median: '1.8', bankRate: null },
    ],
    indicators: [{ code: 'CDI', referenceDate: '2026-09-23', value: '13.65' }],
  };
  HttpService.getWithAuth.mockReset().mockImplementation((url) =>
    Promise.resolve(url === '/market/overview' ? market : commitments)
  );
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

  expect(container.textContent).toContain('commitments.minimum');
  expect(container.textContent).toContain('commitments.revolvingRate');
  expect(container.textContent).toContain('commitments.cet');
  expect(container.textContent).toContain('commitments.marketLoan');
  expect(container.textContent).toContain('commitments.indicators.CDI');
  act(() => root.unmount());
});

test('26,82% a year is 2% a month', () => {
  expect(monthlyFromAnnual('26.82')).toBeCloseTo(2, 2);
});
