import axios from './axios';
import { safeReturnPath } from './HttpService';

jest.mock('./axios', () => ({ interceptors: { response: { use: jest.fn() } } }));

const [, onError] = axios.interceptors.response.use.mock.calls[0];
const failure = (status, headers = {}) => Object.assign(new Error('x'), { response: { status }, config: { headers } });

let assign;
beforeEach(() => {
  sessionStorage.setItem('user', JSON.stringify({ token: 'a.b.c' }));
  assign = jest.fn();
  delete window.location;
  window.location = { pathname: '/future', search: '?horizon=60', assign };
});

test('an expired session ends it and goes to login keeping the way back', async () => {
  await expect(onError(failure(401, { Authorization: 'Bearer x' }))).rejects.toThrow();
  expect(sessionStorage.getItem('user')).toBeNull();
  expect(assign).toHaveBeenCalledWith(`/login?from=${encodeURIComponent('/future?horizon=60')}`);
});

test('a 401 on the login itself (no token sent) is a wrong password, not a redirect', async () => {
  await expect(onError(failure(401))).rejects.toThrow();
  expect(assign).not.toHaveBeenCalled();
  expect(sessionStorage.getItem('user')).not.toBeNull();
});

test('other errors pass through untouched', async () => {
  await expect(onError(failure(500, { Authorization: 'Bearer x' }))).rejects.toThrow();
  expect(assign).not.toHaveBeenCalled();
});

test('only internal paths are accepted as the way back', () => {
  expect(safeReturnPath('/future?horizon=60')).toBe('/future?horizon=60');
  expect(safeReturnPath('//evil.example')).toBe('/');
  expect(safeReturnPath('https://evil.example')).toBe('/');
  expect(safeReturnPath('/login')).toBe('/');
  expect(safeReturnPath(null)).toBe('/');
});
