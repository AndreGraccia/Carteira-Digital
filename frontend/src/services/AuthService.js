import axios from './axios';

const login = async (body) => {
  // Limpa uma possível sessão anterior.
  localStorage.removeItem('user');

  const response = await axios.post('/auth/login', body);
  const user = response.data;

  if (typeof user?.token !== 'string' || !user.token.trim()) {
    throw new Error('O servidor não retornou um token válido.');
  }

  localStorage.setItem('user', JSON.stringify(user));

  return user;
};

const signup = (body) => {
  const url = '/auth/signup';
  return axios.post(url, body).then((response) => response.data);
};

const logout = () => {
  localStorage.removeItem('user');
};

const getCurrentUser = () => {
  const storedUser = localStorage.getItem('user');

  if (!storedUser) return null;

  try {
    const user = JSON.parse(storedUser);

    if (typeof user?.token !== 'string' || !user.token.trim()) {
      localStorage.removeItem('user');
      return null;
    }

    const payload = JSON.parse(atob(user.token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    if (!payload.exp || payload.exp * 1000 <= Date.now()) {
      localStorage.removeItem('user');
      return null;
    }
    return user;
  } catch {
    localStorage.removeItem('user');
    return null;
  }
};

const AuthService = {
  login,
  signup,
  logout,
  getCurrentUser,
};

export default AuthService;
