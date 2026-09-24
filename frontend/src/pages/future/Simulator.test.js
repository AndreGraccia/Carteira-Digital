import { act, Simulate } from 'react-dom/test-utils';
import { createRoot } from 'react-dom/client';
import Simulator from './Simulator';
import ThemeProvider from '../../theme';
import HttpService from '../../services/HttpService';

jest.mock('../../services/HttpService', () => ({ postWithAuth: jest.fn() }));
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

test('an answer for the previous tab is dropped when the tab changes mid-flight', async () => {
  let resolve;
  HttpService.postWithAuth.mockReturnValue(
    new Promise((done) => {
      resolve = done;
    })
  );
  await act(async () =>
    root.render(
      <ThemeProvider>
        <Simulator incomes={[]} hasCard={false} />
      </ThemeProvider>
    )
  );
  const amount = [...container.querySelectorAll('input')].find(
    (input) => input.getAttribute('inputmode') === 'decimal'
  );
  await act(async () => Simulate.change(amount, { target: { value: '100' } }));
  await act(async () => Simulate.submit(container.querySelector('form')));
  expect(HttpService.postWithAuth).toHaveBeenCalledWith(
    '/future/scenarios',
    expect.objectContaining({ type: 'PURCHASE' })
  );

  const savingsTab = [...container.querySelectorAll('[role="tab"]')].find(
    (tab) => tab.textContent === 'future.scenarioTypes.SAVINGS'
  );
  await act(async () => Simulate.click(savingsTab));
  const summary = { minBalance30: 1, minBalance90: 1, negativeDays: 0 };
  await act(async () => resolve({ baseline: summary, scenario: summary, newMonthlyCommitment: 0 }));

  expect(container.textContent).not.toContain('future.resultTitle');
});
