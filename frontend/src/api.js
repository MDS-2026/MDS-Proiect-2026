const API_URL = 'http://localhost:8080/api';

function getToken() {
  return localStorage.getItem('fairpay_token');
}

function setToken(token) {
  localStorage.setItem('fairpay_token', token);
}

function clearToken() {
  localStorage.removeItem('fairpay_token');
  localStorage.removeItem('fairpay_email');
}

async function request(path, options = {}) {
  const token = getToken();
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${API_URL}${path}`, { ...options, headers });

  if (res.status === 401) {
    clearToken();
    window.location.href = '/login';
    return;
  }

  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Request failed');
  return data;
}

const api = {
  // Auth
  register: (email, password) =>
    request('/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  login: (email, password) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),

  // Groups
  getGroups: () => request('/groups'),
  getGroup: (id) => request(`/groups/${id}`),
  createGroup: (name) =>
    request('/groups', { method: 'POST', body: JSON.stringify({ name }) }),
  joinGroup: (inviteCode) =>
    request('/groups/join', { method: 'POST', body: JSON.stringify({ inviteCode }) }),

  // Assets
  getAssets: (groupId) => request(`/groups/${groupId}/assets`),
  addAsset: (groupId, data) =>
    request(`/groups/${groupId}/assets`, { method: 'POST', body: JSON.stringify(data) }),

  // Wallets
  getWallets: (groupId) => request(`/groups/${groupId}/wallets`),
  createWallet: (groupId, data) =>
    request(`/groups/${groupId}/wallets`, { method: 'POST', body: JSON.stringify(data) }),

  // Transactions
  getWalletTransactions: (walletId) => request(`/wallets/${walletId}/transactions`),
  getGroupTransactions: (groupId) => request(`/groups/${groupId}/transactions`),
  createTransaction: (walletId, data) =>
    request(`/wallets/${walletId}/transactions`, { method: 'POST', body: JSON.stringify(data) }),
  approveTransaction: (id) =>
    request(`/transactions/${id}/approve`, { method: 'PATCH' }),
  declineTransaction: (id) =>
    request(`/transactions/${id}/decline`, { method: 'PATCH' }),

  getToken,
  setToken,
  clearToken,
};

export default api;
