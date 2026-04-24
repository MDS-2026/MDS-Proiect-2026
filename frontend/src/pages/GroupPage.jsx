import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api';
import Sidebar from '../components/Sidebar';
import Modal from '../components/Modal';

export default function GroupPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [group, setGroup] = useState(null);
  const [assets, setAssets] = useState([]);
  const [wallets, setWallets] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [error, setError] = useState('');

  // Modal states
  const [showAssetModal, setShowAssetModal] = useState(false);
  const [showWalletModal, setShowWalletModal] = useState(false);
  const [showTxModal, setShowTxModal] = useState(false);

  // Asset form
  const [assetType, setAssetType] = useState('CASH');
  const [assetProvider, setAssetProvider] = useState('');
  const [assetValue, setAssetValue] = useState('');
  const [assetExpiry, setAssetExpiry] = useState('');

  // Wallet form
  const [walletName, setWalletName] = useState('');
  const [walletPurpose, setWalletPurpose] = useState('');
  const [walletBudget, setWalletBudget] = useState('');
  const [walletThreshold, setWalletThreshold] = useState('');

  // Transaction form
  const [txWalletId, setTxWalletId] = useState('');
  const [txAmount, setTxAmount] = useState('');
  const [txMerchant, setTxMerchant] = useState('');
  const [txCategory, setTxCategory] = useState('');

  useEffect(() => { loadAll(); }, [id]);

  const loadAll = async () => {
    try {
      const [g, a, w, t] = await Promise.all([
        api.getGroup(id),
        api.getAssets(id),
        api.getWallets(id),
        api.getGroupTransactions(id),
      ]);
      setGroup(g);
      setAssets(a);
      setWallets(w);
      setTransactions(t);
    } catch (err) { setError(err.message); }
  };

  const handleAddAsset = async (e) => {
    e.preventDefault();
    try {
      await api.addAsset(id, {
        type: assetType,
        provider: assetProvider,
        estimatedEurValue: parseFloat(assetValue),
        expiryDate: assetExpiry || null,
      });
      setShowAssetModal(false);
      setAssetProvider(''); setAssetValue(''); setAssetExpiry('');
      loadAll();
    } catch (err) { setError(err.message); }
  };

  const handleCreateWallet = async (e) => {
    e.preventDefault();
    try {
      await api.createWallet(id, {
        name: walletName,
        purpose: walletPurpose,
        budgetLimit: parseFloat(walletBudget),
        autoApproveThreshold: parseFloat(walletThreshold),
      });
      setShowWalletModal(false);
      setWalletName(''); setWalletPurpose(''); setWalletBudget(''); setWalletThreshold('');
      loadAll();
    } catch (err) { setError(err.message); }
  };

  const handleCreateTx = async (e) => {
    e.preventDefault();
    try {
      await api.createTransaction(txWalletId, {
        amount: parseFloat(txAmount),
        merchant: txMerchant,
        category: txCategory,
      });
      setShowTxModal(false);
      setTxAmount(''); setTxMerchant(''); setTxCategory('');
      loadAll();
    } catch (err) { setError(err.message); }
  };

  const totalPooled = assets.reduce((sum, a) => sum + a.estimatedEurValue, 0);

  if (!group) return <div className="layout"><Sidebar /><main className="main"><div className="empty">Loading...</div></main></div>;

  return (
    <div className="layout">
      <Sidebar />
      <main className="main">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }} className="mb-24">
          <button className="btn btn-sm" onClick={() => navigate('/')}>←</button>
          <h1 className="page-title" style={{ marginBottom: 0 }}>{group.name}</h1>
          <span className="invite-code" style={{ marginLeft: 8 }}>{group.inviteCode}</span>
        </div>

        {error && <div className="error mb-12">{error}</div>}

        {/* Stats */}
        <div className="card-grid">
          <div className="card">
            <div className="stat-label">Total Pooled</div>
            <div className="stat-value">€{totalPooled.toFixed(2)}</div>
            <div className="stat-sub">{assets.length} asset{assets.length !== 1 ? 's' : ''}</div>
          </div>
          <div className="card">
            <div className="stat-label">Wallets</div>
            <div className="stat-value">{wallets.length}</div>
            <div className="stat-sub">€{wallets.reduce((s, w) => s + w.budgetLimit, 0).toFixed(2)} total budget</div>
          </div>
          <div className="card">
            <div className="stat-label">Members</div>
            <div className="stat-value">{group.members?.length || 0}</div>
          </div>
        </div>

        {/* Members */}
        <div className="table-section card">
          <div className="table-header">
            <div className="table-title">Members</div>
          </div>
          <table className="data-table">
            <thead><tr><th>Email</th><th>Role</th><th>Fairness</th></tr></thead>
            <tbody>
              {group.members?.map(m => (
                <tr key={m.userId}>
                  <td>{m.email}</td>
                  <td><span className={`badge badge-${m.role.toLowerCase()}`}>{m.role}</span></td>
                  <td>{m.fairnessScore}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Assets */}
        <div className="table-section card">
          <div className="table-header">
            <div className="table-title">Assets</div>
            <button className="btn btn-sm" onClick={() => setShowAssetModal(true)}>+ Add</button>
          </div>
          {assets.length === 0 ? <div className="empty">No assets</div> : (
            <table className="data-table">
              <thead><tr><th>Type</th><th>Provider</th><th>Value (EUR)</th><th>Expiry</th></tr></thead>
              <tbody>
                {assets.map(a => (
                  <tr key={a.id}>
                    <td><span className="badge badge-member">{a.type}</span></td>
                    <td>{a.provider}</td>
                    <td>€{a.estimatedEurValue.toFixed(2)}</td>
                    <td>{a.expiryDate || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Wallets */}
        <div className="table-section card">
          <div className="table-header">
            <div className="table-title">Wallets</div>
            <button className="btn btn-sm" onClick={() => setShowWalletModal(true)}>+ New</button>
          </div>
          {wallets.length === 0 ? <div className="empty">No wallets</div> : (
            <table className="data-table">
              <thead><tr><th>Name</th><th>Purpose</th><th>Budget</th><th>Auto-approve</th></tr></thead>
              <tbody>
                {wallets.map(w => (
                  <tr key={w.id}>
                    <td style={{ fontWeight: 500 }}>{w.name}</td>
                    <td style={{ color: 'var(--text-2)' }}>{w.purpose || '—'}</td>
                    <td>€{w.budgetLimit.toFixed(2)}</td>
                    <td>€{w.autoApproveThreshold.toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Transactions */}
        <div className="table-section card">
          <div className="table-header">
            <div className="table-title">Transactions</div>
            <button className="btn btn-sm" onClick={() => { setTxWalletId(wallets[0]?.id || ''); setShowTxModal(true); }}>+ New</button>
          </div>
          {transactions.length === 0 ? <div className="empty">No transactions</div> : (
            <table className="data-table">
              <thead><tr><th>Merchant</th><th>Category</th><th>Amount</th><th>Wallet</th><th>Status</th><th>Actions</th></tr></thead>
              <tbody>
                {transactions.map(tx => (
                  <tr key={tx.id}>
                    <td>{tx.merchant}</td>
                    <td style={{ color: 'var(--text-2)' }}>{tx.category || '—'}</td>
                    <td>€{tx.amount.toFixed(2)}</td>
                    <td style={{ color: 'var(--text-2)' }}>{tx.walletName}</td>
                    <td><span className={`badge badge-${tx.status.toLowerCase()}`}>{tx.status}</span></td>
                    <td>
                      {tx.status === 'PENDING' && (
                        <div className="flex-gap">
                          <button className="btn btn-sm" onClick={async () => { await api.approveTransaction(tx.id); loadAll(); }}>Approve</button>
                          <button className="btn btn-sm btn-danger" onClick={async () => { await api.declineTransaction(tx.id); loadAll(); }}>Decline</button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Add Asset Modal */}
        {showAssetModal && (
          <Modal title="Add asset" onClose={() => setShowAssetModal(false)}>
            <form onSubmit={handleAddAsset}>
              <div className="form-group">
                <label className="form-label">Type</label>
                <select className="form-input" value={assetType} onChange={e => setAssetType(e.target.value)}>
                  <option value="CASH">Cash</option>
                  <option value="MILES">Miles</option>
                  <option value="VOUCHER">Voucher</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Provider</label>
                <input className="form-input" value={assetProvider} onChange={e => setAssetProvider(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Estimated value (EUR)</label>
                <input className="form-input" type="number" step="0.01" value={assetValue} onChange={e => setAssetValue(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Expiry date (optional)</label>
                <input className="form-input" type="date" value={assetExpiry} onChange={e => setAssetExpiry(e.target.value)} />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn" onClick={() => setShowAssetModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Add</button>
              </div>
            </form>
          </Modal>
        )}

        {/* Create Wallet Modal */}
        {showWalletModal && (
          <Modal title="Create wallet" onClose={() => setShowWalletModal(false)}>
            <form onSubmit={handleCreateWallet}>
              <div className="form-group">
                <label className="form-label">Name</label>
                <input className="form-input" value={walletName} onChange={e => setWalletName(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Purpose</label>
                <input className="form-input" value={walletPurpose} onChange={e => setWalletPurpose(e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Budget limit (EUR)</label>
                <input className="form-input" type="number" step="0.01" value={walletBudget} onChange={e => setWalletBudget(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Auto-approve threshold (EUR)</label>
                <input className="form-input" type="number" step="0.01" value={walletThreshold} onChange={e => setWalletThreshold(e.target.value)} required />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn" onClick={() => setShowWalletModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Create</button>
              </div>
            </form>
          </Modal>
        )}

        {/* Create Transaction Modal */}
        {showTxModal && (
          <Modal title="New transaction" onClose={() => setShowTxModal(false)}>
            <form onSubmit={handleCreateTx}>
              <div className="form-group">
                <label className="form-label">Wallet</label>
                <select className="form-input" value={txWalletId} onChange={e => setTxWalletId(e.target.value)} required>
                  {wallets.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Amount (EUR)</label>
                <input className="form-input" type="number" step="0.01" value={txAmount} onChange={e => setTxAmount(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Merchant</label>
                <input className="form-input" value={txMerchant} onChange={e => setTxMerchant(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Category</label>
                <input className="form-input" value={txCategory} onChange={e => setTxCategory(e.target.value)} />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn" onClick={() => setShowTxModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Submit</button>
              </div>
            </form>
          </Modal>
        )}
      </main>
    </div>
  );
}
