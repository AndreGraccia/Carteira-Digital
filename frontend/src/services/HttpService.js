import AuthHeader from './AuthHeader';
import AuthService from './AuthService';
import axios from './axios';

/** Where to come back after logging in again: only an internal path, never the login itself. */
export const safeReturnPath = (from) =>
  typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') && !from.startsWith('/login') ? from : '/';

// An authenticated call answering 401 means the session is over: every screen goes back to the login and
// returns here afterwards, instead of a "try again" that fails forever. Calls sent without a token
// (login, signup) keep their own 401, which means "wrong password".
axios.interceptors.response.use(undefined, (error) => {
  if (error.response?.status === 401 && error.config?.headers?.Authorization) {
    AuthService.logout();
    const { pathname, search } = window.location;
    if (!pathname.startsWith('/login'))
      window.location.assign(`/login?from=${encodeURIComponent(safeReturnPath(pathname + search))}`);
  }
  return Promise.reject(error);
});

const postWithoutAuth = (url, body) => {
  const request = axios.post(url, body);
  return request.then((response) => response.data);
};

const getWithAuth = (url) => {
  const request = axios.get(url, { headers: AuthHeader() });
  return request.then((response) => response.data);
};

const postWithAuth = (url, body) => {
  const request = axios.post(url, body, { headers: AuthHeader() });
  return request.then((response) => response.data);
};

const putWithAuth = (url, body) => {
  const request = axios.put(url, body, { headers: AuthHeader() });
  return request.then((response) => response.data);
};

const deleteWithAuth = (url) => {
  const request = axios.delete(url, { headers: AuthHeader() });
  return request.then((response) => response.data);
};

const HttpService = {
  postWithoutAuth,
  getWithAuth,
  postWithAuth,
  putWithAuth,
  deleteWithAuth,
};

export default HttpService;
