import { useState, useEffect, useCallback, useRef } from 'react';
import { api } from './api.js';

/* ── Toast helper ────────────────────────────────────── */
function useToasts() {
  const [toasts, setToasts] = useState([]);
  const add = useCallback((msg, type = 'success') => {
    const id = Date.now();
    setToasts(ts => [...ts, { id, msg, type }]);
    setTimeout(() => setToasts(ts => ts.filter(t => t.id !== id)), 3500);
  }, []);
  return { toasts, add };
}

/* ── Loading + error aware polling hook ──────────────── */
function usePolled(fetchFn, intervalMs = 3000) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const savedFn = useRef(fetchFn);
  useEffect(() => { savedFn.current = fetchFn; }, [fetchFn]);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      try {
        const d = await savedFn.current();
        if (!cancelled) { setData(d); setError(null); }
      } catch (e) {
        if (!cancelled) setError(e.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    run();
    const id = setInterval(run, intervalMs);
    return () => { cancelled = true; clearInterval(id); };
  }, [intervalMs]);

  return { data, error, loading, refresh: () => savedFn.current().then(setData).catch(e => setError(e.message)) };
}

/* ── Status / source badge ───────────────────────────── */
function Badge({ value }) {
  return <span className={`badge badge-${value}`}>{value}</span>;
}

/* ── Confidence bar ──────────────────────────────────── */
function ConfidenceBar({ value }) {
  const pct = Math.round((value || 0) * 100);
  return (
    <div className="confidence-bar-wrapper">
      <div className="confidence-label">
        <span>Confidence</span>
        <span>{pct}%</span>
      </div>
      <div className="confidence-bar">
        <div className="confidence-fill" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

/* ── Pricing Suggestion Card ─────────────────────────── */
function PricingCard({ s, onAccept, onReject, loading }) {
  const dir = s.direction;
  const priceClass = dir === 'INCREASE' ? 'up' : dir === 'DECREASE' ? 'down' : '';
  return (
    <div className="suggestion-card">
      <div className="card-header">
        <div>
          <div className="card-product">{s.productName}</div>
          <div className="card-sku">{s.productSku}</div>
        </div>
        <div className="card-badges">
          <Badge value={s.triggerReason} />
          <Badge value={s.source} />
        </div>
      </div>
      <div className="card-price-row">
        <span className="price-old">${Number(s.currentPrice).toFixed(2)}</span>
        <span className="price-arrow">to</span>
        <span className={`price-new ${priceClass}`}>${Number(s.recommendedPrice).toFixed(2)}</span>
        <span className={`dir-${dir.toLowerCase()}`}>{dir}</span>
      </div>
      <ConfidenceBar value={s.confidence} />
      <div className="reasoning">{s.reasoning}</div>
      {s.status === 'PENDING' && (
        <div className="card-actions">
          <button className="btn btn-accept btn-sm" disabled={loading} onClick={() => onAccept(s.id)}>
            ✓ Accept
          </button>
          <button className="btn btn-reject btn-sm" disabled={loading} onClick={() => onReject(s.id)}>
            ✗ Reject
          </button>
        </div>
      )}
      {s.status !== 'PENDING' && <Badge value={s.status} />}
    </div>
  );
}

/* ── Reorder Suggestion Card ─────────────────────────── */
function ReorderCard({ s, onAccept, onReject, loading }) {
  return (
    <div className="suggestion-card">
      <div className="card-header">
        <div>
          <div className="card-product">{s.productName}</div>
          <div className="card-sku">{s.productSku}</div>
        </div>
        <div className="card-badges">
          <Badge value={s.triggerReason} />
          <Badge value={s.source} />
        </div>
      </div>
      <div className="card-qty-row">
        Stock now: <strong>{s.currentStock}</strong> &nbsp;→&nbsp; Order: <strong>{s.recommendedQuantity} units</strong>
        &nbsp;· Lead time: <strong>{s.suggestedLeadTimeDays}d</strong>
      </div>
      <ConfidenceBar value={s.confidence} />
      <div className="reasoning">{s.reasoning}</div>
      {s.status === 'PENDING' && (
        <div className="card-actions">
          <button className="btn btn-accept btn-sm" disabled={loading} onClick={() => onAccept(s.id)}>
            ✓ Accept
          </button>
          <button className="btn btn-reject btn-sm" disabled={loading} onClick={() => onReject(s.id)}>
            ✗ Reject
          </button>
        </div>
      )}
      {s.status !== 'PENDING' && <Badge value={s.status} />}
    </div>
  );
}

/* ── Main App ─────────────────────────────────────────── */
export default function App() {
  const { toasts, add: toast } = useToasts();
  const [actionLoading, setActionLoading] = useState(false);

  // Strategy state
  const [strategy, setStrategy] = useState(null);
  const [strategyLoading, setStrategyLoading] = useState(false);

  useEffect(() => {
    api.getStrategy().then(d => setStrategy(d.strategy)).catch(() => {});
  }, []);

  const handleToggleStrategy = async () => {
    if (strategyLoading) return;
    const next = strategy === 'AI' ? 'RULE_BASED' : 'AI';
    setStrategyLoading(true);
    try {
      const d = await api.setStrategy(next);
      setStrategy(d.strategy);
      toast(`Strategy switched to ${d.strategy}`);
    } catch (e) {
      toast(`Failed to switch strategy: ${e.message}`, 'error');
    } finally {
      setStrategyLoading(false);
    }
  };

  // Polled data
  const products = usePolled(() => api.getProducts());
  const pricingSugg = usePolled(() => api.getPricingSuggestions('PENDING'));
  const reorderSugg = usePolled(() => api.getReorderSuggestions('PENDING'));

  const refresh = () => {
    products.refresh();
    pricingSugg.refresh();
    reorderSugg.refresh();
  };

  // Order simulation
  const handleOrder = async (product) => {
    setActionLoading(true);
    try {
      const res = await api.simulateOrder(product.id, 1);
      toast(`Sold 1 × ${product.name}. Stock now ${res.remainingStock}. Velocity ${res.newDemandVelocity.toFixed(1)}`);
      refresh();
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      setActionLoading(false);
    }
  };

  // Suggestion resolution
  const handlePricingResolve = async (id, status) => {
    setActionLoading(true);
    try {
      await api.resolvePricingSuggestion(id, status);
      toast(`Pricing suggestion ${status.toLowerCase()}.`);
      refresh();
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReorderResolve = async (id, status) => {
    setActionLoading(true);
    try {
      await api.resolveReorderSuggestion(id, status);
      toast(`Reorder suggestion ${status.toLowerCase()}.`);
      refresh();
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const pricingList = pricingSugg.data || [];
  const reorderList = reorderSugg.data || [];
  const productList = products.data || [];

  return (
    <div className="app">
      {/* ── Top bar ──────────────────────────────────── */}
      <header className="topbar">
        <div className="topbar-brand">
          <div className="pulse-dot" />
          <h1>StockPulse</h1>
          <span>Merchandising Console</span>
        </div>
        <div className="topbar-right">
          <div className="strategy-toggle">
            <span>Strategy:</span>
            <button
              id="strategy-toggle-btn"
              className={`strategy-btn ${strategy === 'AI' ? 'active-ai' : 'active-rule'}`}
              onClick={handleToggleStrategy}
              disabled={strategyLoading}
              title="Click to switch strategy"
            >
              {strategyLoading ? '...' : strategy === 'AI' ? 'AI' : 'Rule-Based'}
            </button>
          </div>
        </div>
      </header>

      <main className="content">
        {/* ── Catalog Table ─────────────────────────── */}
        <section id="products-section">
          <div className="section-header">
            <h2>Product Catalog</h2>
            {!products.loading && <span className="count-badge">{productList.length}</span>}
          </div>
          {products.error && <div className="error-banner">Error loading products: {products.error}</div>}
          {products.loading && <div className="loading-text">Loading products…</div>}
          {!products.loading && !products.error && (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>SKU</th>
                    <th>Name</th>
                    <th>Category</th>
                    <th>Stock</th>
                    <th>Threshold</th>
                    <th>Price</th>
                    <th>Velocity</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {productList.length === 0 && (
                    <tr><td colSpan="9" style={{ textAlign: 'center', color: 'var(--text-dim)', padding: '24px' }}>No products found.</td></tr>
                  )}
                  {productList.map(p => (
                    <tr key={p.id}>
                      <td className="sku-cell">{p.sku}</td>
                      <td>{p.name}</td>
                      <td><Badge value={p.category} /></td>
                      <td className={p.stockLevel < p.reorderThreshold ? 'stock-low' : 'stock-ok'}>
                        {p.stockLevel}
                      </td>
                      <td>{p.reorderThreshold}</td>
                      <td className="price-cell">${Number(p.currentPrice).toFixed(2)}</td>
                      <td className="velocity-cell">{Number(p.demandVelocity).toFixed(1)}</td>
                      <td><Badge value={p.status} /></td>
                      <td>
                        <button
                          id={`order-btn-${p.id}`}
                          className="btn btn-sale btn-sm"
                          disabled={actionLoading || p.stockLevel === 0}
                          onClick={() => handleOrder(p)}
                          title="Simulate a sale of 1 unit"
                        >
                          Sell 1
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* ── Suggestions Feed ──────────────────────── */}
        <section id="suggestions-section">
          <div className="section-header">
            <h2>Pending Suggestions</h2>
            {!pricingSugg.loading && !reorderSugg.loading && (
              <span className="count-badge">{pricingList.length + reorderList.length}</span>
            )}
          </div>
          {(pricingSugg.error || reorderSugg.error) && (
            <div className="error-banner">Error loading suggestions: {pricingSugg.error || reorderSugg.error}</div>
          )}
          <div className="suggestions-grid">
            {/* Pricing column */}
            <div className="suggestions-col">
              <div className="section-header">
                <h2 style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Dynamic Pricing
                </h2>
                <span className="count-badge">{pricingList.length}</span>
              </div>
              {pricingSugg.loading && <div className="loading-text">Loading…</div>}
              {!pricingSugg.loading && pricingList.length === 0 && (
                <div className="empty-state">No pending pricing suggestions</div>
              )}
              {pricingList.map(s => (
                <PricingCard
                  key={s.id}
                  s={s}
                  loading={actionLoading}
                  onAccept={(id) => handlePricingResolve(id, 'ACCEPTED')}
                  onReject={(id) => handlePricingResolve(id, 'REJECTED')}
                />
              ))}
            </div>

            {/* Reorder column */}
            <div className="suggestions-col">
              <div className="section-header">
                <h2 style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Inbound Replenishment
                </h2>
                <span className="count-badge">{reorderList.length}</span>
              </div>
              {reorderSugg.loading && <div className="loading-text">Loading…</div>}
              {!reorderSugg.loading && reorderList.length === 0 && (
                <div className="empty-state">No pending reorder suggestions</div>
              )}
              {reorderList.map(s => (
                <ReorderCard
                  key={s.id}
                  s={s}
                  loading={actionLoading}
                  onAccept={(id) => handleReorderResolve(id, 'ACCEPTED')}
                  onReject={(id) => handleReorderResolve(id, 'REJECTED')}
                />
              ))}
            </div>
          </div>
        </section>
      </main>

      {/* ── Toast Notifications ────────────────────── */}
      <div className="toast-area">
        {toasts.map(t => (
          <div key={t.id} className={`toast toast-${t.type}`}>{t.msg}</div>
        ))}
      </div>
    </div>
  );
}
