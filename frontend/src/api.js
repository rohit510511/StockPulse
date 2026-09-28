const BASE_URL = 'http://localhost:8081';

async function apiFetch(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
  });
  if (!res.ok) {
    const text = await res.text();
    let msg = `HTTP ${res.status}`;
    try { msg = JSON.parse(text).message || msg; } catch (_) {}
    throw new Error(msg);
  }
  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  getProducts: (params = {}) => {
    const qs = new URLSearchParams();
    if (params.status) qs.set('status', params.status);
    if (params.category) qs.set('category', params.category);
    const q = qs.toString();
    return apiFetch(`/products${q ? '?' + q : ''}`);
  },
  simulateOrder: (id, quantity) =>
    apiFetch(`/products/${id}/orders`, {
      method: 'POST',
      body: JSON.stringify({ quantity }),
    }),
  updateStock: (id, stockLevel) =>
    apiFetch(`/products/${id}/stock`, {
      method: 'PATCH',
      body: JSON.stringify({ stockLevel }),
    }),
  getPricingSuggestions: (status) =>
    apiFetch(`/pricing-suggestions${status ? '?status=' + status : ''}`),
  getReorderSuggestions: (status) =>
    apiFetch(`/reorder-suggestions${status ? '?status=' + status : ''}`),
  resolvePricingSuggestion: (id, status) =>
    apiFetch(`/pricing-suggestions/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }),
  resolveReorderSuggestion: (id, status) =>
    apiFetch(`/reorder-suggestions/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }),
  getStrategy: () => apiFetch('/admin/strategy'),
  setStrategy: (strategy) =>
    apiFetch('/admin/strategy', {
      method: 'PUT',
      body: JSON.stringify({ strategy }),
    }),
};
