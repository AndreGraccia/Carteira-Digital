import axios from './axios';

const login = async (body) => {
  // O token dura apenas durante a sessão da aba/navegador.
  localStorage.removeItem('user');
  sessionStorage.removeItem('user');

  const response = await axios.post('/auth/login', body);
  const user = response.data;

  if (typeof user?.token !== 'string' || !user.token.trim()) {
    throw Object.assign(new Error('Login response without a token'), { code: 'INVALID_TOKEN' });
  }

  sessionStorage.setItem('user', JSON.stringify(user));

  return user;
};

const signup = (body) => {
  const url = '/auth/signup';
  return axios.post(url, body).then((response) => response.data);
};

// The account only signs in after the e-mail code (ADR 0008). Verify answers 204 and never a token.
const verifyEmail = (login, code) => axios.post('/auth/email/verify', { login, code });
const resendCode = (login) => axios.post('/auth/email/resend', { login }).then((response) => response.data);

const logout = () => {
  sessionStorage.removeItem('user');
  localStorage.removeItem('user');
};

const getCurrentUser = () => {
  const storedUser = sessionStorage.getItem('user');

  // Remove tokens persistidos por versões anteriores.
  localStorage.removeItem('user');

  if (!storedUser) return null;

  try {
    const user = JSON.parse(storedUser);

    if (typeof user?.token !== 'string' || !user.token.trim()) {
      sessionStorage.removeItem('user');
      return null;
    }

    const payload = JSON.parse(atob(user.token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    if (!payload.exp || payload.exp * 1000 <= Date.now()) {
      sessionStorage.removeItem('user');
      return null;
    }
    return user;
  } catch {
    sessionStorage.removeItem('user');
    return null;
  }
};

const AuthService = {
  login,
  signup,
  verifyEmail,
  resendCode,
  logout,
  getCurrentUser,
};

export default AuthService;
