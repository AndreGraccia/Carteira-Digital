import { fetchAllTransactions } from './WalletContext';
import HttpService from '../services/HttpService';

jest.mock('../services/HttpService', () => ({ getWithAuth: jest.fn() }));
jest.mock('../services/AuthService', () => ({ getCurrentUser: jest.fn() }));

const rows = (from, count) => Array.from({ length: count }, (_, index) => ({ id: from + index }));

test('reads every page of the statement until the last one', async () => {
  HttpService.getWithAuth
    .mockResolvedValueOnce({ content: rows(0, 500), last: false })
    .mockResolvedValueOnce({ content: rows(500, 20), last: true });

  const all = await fetchAllTransactions(7);

  expect(all).toHaveLength(520);
  expect(HttpService.getWithAuth).toHaveBeenNthCalledWith(1, '/transactions/users/7?page=0&size=500');
  expect(HttpService.getWithAuth).toHaveBeenNthCalledWith(2, '/transactions/users/7?page=1&size=500');
});

test('an older API that answers a plain list is read as is', async () => {
  HttpService.getWithAuth.mockReset().mockResolvedValueOnce(rows(0, 3));

  expect(await fetchAllTransactions(7)).toHaveLength(3);
  expect(HttpService.getWithAuth).toHaveBeenCalledTimes(1);
});
