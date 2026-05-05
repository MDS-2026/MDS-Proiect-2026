import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api';
import Sidebar from '../components/Sidebar';
import Modal from '../components/Modal';
import CheckoutPreview from '../components/CheckoutPreview';

export default function GroupPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [group, setGroup] = useState(null);
  const [assets, setAssets] = useState([]);
  const [wallets, setWallets] = useState([]);
  const [walletTree, setWalletTree] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [virtualCards, setVirtualCards] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('overview');

  // preview state
  const [previewWalletId, setPreviewWalletId] = useState(null);

  // Modal states
  const [showAssetModal, setShowAssetModal] = useState(false);
  const [showWalletModal, setShowWalletModal] = useState(false);
  const [showTxModal, setShowTxModal] = useState(false);
  const [showCardModal, setShowCardModal] = useState(false);
  const [selectedCard, setSelectedCard] = useState(null);

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
  const [walletParentId, setWalletParentId] = useState('');

  // Transaction form
  const [txWalletId, setTxWalletId] = useState('');
  const [txAmount, setTxAmount] = useState('');
  const [txMerchant, setTxMerchant] = useState('');
  const [txCategory, setTxCategory] = useState('');
  const [txAiValidation, setTxAiValidation] = useState(null);

  useEffect(() => { loadAll(); }, [id]);

  const loadAll = async () => {
    try {
      const [g, a, w, tree, t, cards, logs] = await Promise.all([
        api.getGroup(id),
        api.getAssets(id),
        api.getWallets(id),
        api.getWalletTree(id),
        api.getGroupTransactions(id),
        api.getVirtualCards(id),
        api.getAuditLogs(id),
      ]);
      setGroup(g);
      setAssets(a);
      setWallets(w);
      setWalletTree(tree);
      setTransactions(t);
      setVirtualCards(cards);
      setAuditLogs(logs);
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
        parentWalletId: walletParentId || null,
      });
      setShowWalletModal(false);
      setWalletName(''); setWalletPurpose(''); setWalletBudget(''); setWalletThreshold(''); setWalletParentId('');
      loadAll();
    } catch (err) { setError(err.message); }
  };

  const handleCreateTx = async (e) => {
    e.preventDefault();
    try {
      const validation = await api.validateTransaction({
        walletId: txWalletId,
        amount: parseFloat(txAmount),
        merchant: txMerchant,
        category: txCategory,
      });
      setTxAiValidation(validation);

      await api.createTransaction(txWalletId, {
        amount: parseFloat(txAmount),
        merchant: txMerchant,
        category: txCategory,
      });
      setShowTxModal(false);
      setTxAmount(''); setTxMerchant(''); setTxCategory('');
      setTxAiValidation(null);
      loadAll();
    } catch (err) { setError(err.message); }
  };

  const handleChangeRole = async (userId, currentRole) => {
    const newRole = currentRole === 'ADMIN' ? 'MEMBER' : 'ADMIN';
    try {
      await api.changeRole(id, userId, newRole);
      loadAll();
    } catch (err) { setError(err.message); }
  };

  const handleViewCard = (card) => {
    setSelectedCard(card);
    setShowCardModal(true);
  };

  const totalPooled = assets.reduce((sum, a) => sum + a.estimatedEurValue, 0);

  const tabs = [
    { key: 'overview', label: 'Overview' },
    { key: 'tree', label: 'Tree Dashboard' },
    { key: 'cards', label: 'Virtual Cards' },
    { key: 'audit', label: 'Audit Log' },
    { key: 'chat', label: 'Group Chat' },
  ];

  const renderTreeNode = (node, isRoot = false) => {
    const percent = Math.min(100, (node.spentAmount / node.budgetLimit) * 100) || 0;
    const progressColor = percent > 90 ? 'var(--red)' : percent > 75 ? 'var(--yellow)' : 'var(--blue)';

    return (
      <div key={node.id} className={`tree-node ${isRoot ? 'root-node' : ''}`}>
        <div className="tree-card">
          <div className="tree-card-header">
            <span className="tree-card-title">{node.name}</span>
            <span className="tree-card-purpose">{node.purpose || 'General'}</span>
          </div>
          <div className="tree-card-stats">
            <div className="tree-stat">
              <span className="tree-stat-label">BUDGET</span>
              <span className="tree-stat-value">€{node.budgetLimit.toFixed(2)}</span>
            </div>
            <div className="tree-stat">
              <span className="tree-stat-label">SPENT</span>
              <span className="tree-stat-value">€{node.spentAmount.toFixed(2)}</span>
            </div>
          </div>
          <div className="tree-progress">
            <div className="tree-progress-bar" style={{ width: `${percent}%`, backgroundColor: progressColor }}></div>
          </div>
        </div>
        {node.children && node.children.length > 0 && (
          <div className="tree-children">
            {node.children.map(child => renderTreeNode(child))}
          </div>
        )}
      </div>
    );
  };

  if (!group) return <div className="layout"><Sidebar /><main className="main"><div className="empty">Loading...</div></main></div>;

  return (
    <div className="layout">
      <Sidebar />
      <main className="main">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, paddingRight: '64px' }} className="mb-24">
          <button className="btn btn-sm" onClick={() => navigate('/')}>←</button>
          <h1 className="page-title" style={{ marginBottom: 0 }}>{group.name}</h1>
          <span className="invite-code" style={{ marginLeft: 8 }}>{group.inviteCode}</span>
        </div>

        {error && <div className="error mb-12">{error}</div>}

        {/* Tabs */}
        <div className="tab-bar mb-24">
          {tabs.map(t => (
            <button
              key={t.key}
              className={`tab-btn ${activeTab === t.key ? 'tab-active' : ''}`}
              onClick={() => setActiveTab(t.key)}
            >
              {t.label}
            </button>
          ))}
        </div>

        {activeTab === 'overview' && (
          <>
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
              <div className="card">
                <div className="stat-label">Virtual Cards</div>
                <div className="stat-value">{virtualCards.length}</div>
                <div className="stat-sub">{virtualCards.filter(c => c.active).length} active</div>
              </div>
            </div>

            {/* Members */}
            <div className="table-section card">
              <div className="table-header">
                <div className="table-title">Members</div>
              </div>
              <table className="data-table">
                <thead><tr><th>Email</th><th>Role</th><th>Fairness</th><th>Actions</th></tr></thead>
                <tbody>
                  {group.members?.map(m => (
                    <tr key={m.userId}>
                      <td>{m.email}</td>
                      <td><span className={`badge badge-${m.role.toLowerCase()}`}>{m.role}</span></td>
                      <td>{m.fairnessScore}</td>
                      <td>
                        <button
                          className="btn btn-sm"
                          onClick={() => handleChangeRole(m.userId, m.role)}
                        >
                          {m.role === 'ADMIN' ? 'Make Member' : 'Make Admin'}
                        </button>
                      </td>
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
                  <thead><tr><th>Type</th><th>Provider</th><th>Value (EUR)</th><th>Expiry</th><th>Actions</th></tr></thead>
                  <tbody>
                    {assets.map(a => (
                      <tr key={a.id}>
                        <td><span className="badge badge-member">{a.type}</span></td>
                        <td>{a.provider}</td>
                        <td>€{a.estimatedEurValue.toFixed(2)}</td>
                        <td>{a.expiryDate || '—'}</td>
                        <td>
                          <button type="button" className="btn btn-sm" onClick={() => setPreviewWalletId(a.id)}>
                            Preview split
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              {/* Render preview card below assets table when a previewWalletId is selected */}
              {previewWalletId && (
                <div className="card mt-6">
                  <div style={{ display:'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div className="table-title">Checkout split preview (Asset {previewWalletId})</div>
                    <button type="button" className="btn btn-sm" onClick={() => setPreviewWalletId(null)}>Close</button>
                  </div>
                  <div style={{ marginTop: 12 }}>
                    <CheckoutPreview walletId={previewWalletId} />
                  </div>
                </div>
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
                <button className="btn btn-sm" onClick={() => { setTxWalletId(wallets[0]?.id || ''); setTxAiValidation(null); setShowTxModal(true); }}>+ New</button>
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
          </>
        )}

        {activeTab === 'cards' && (
          <div className="table-section card">
            <div className="table-header">
              <div className="table-title">Virtual Cards</div>
            </div>
            {virtualCards.length === 0 ? <div className="empty">No virtual cards. Create a wallet to generate one automatically.</div> : (
              <div className="card-grid">
                {virtualCards.map(card => (
                  <div key={card.id} className="virtual-card" onClick={() => handleViewCard(card)}>
                    <div className="vc-chip"></div>
                    <div className="vc-number">{card.maskedCardNumber}</div>
                    <div className="vc-row">
                      <div>
                        <div className="vc-label">WALLET</div>
                        <div className="vc-value">{card.walletName}</div>
                      </div>
                      <div>
                        <div className="vc-label">EXPIRES</div>
                        <div className="vc-value">{card.expiryDate}</div>
                      </div>
                      <div>
                        <span className={`badge ${card.active ? 'badge-approved' : 'badge-declined'}`}>
                          {card.active ? 'ACTIVE' : 'INACTIVE'}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'tree' && (
          <div className="card" style={{ overflow: 'hidden' }}>
            <div className="table-header" style={{ padding: '0 20px 20px 20px' }}>
              <div className="table-title">Wallet Hierarchy</div>
            </div>
            {walletTree.length === 0 ? <div className="empty">No wallets yet.</div> : (
              <div className="tree-container">
                {walletTree.map(root => renderTreeNode(root, true))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'audit' && (
          <div className="table-section card">
            <div className="table-header">
              <div className="table-title">Audit Log</div>
              <div className="stat-sub">{auditLogs.length} entries</div>
            </div>
            {auditLogs.length === 0 ? <div className="empty">No audit entries yet</div> : (
              <table className="data-table">
                <thead><tr><th>Time</th><th>Action</th><th>By</th><th>Details</th></tr></thead>
                <tbody>
                  {auditLogs.map(log => (
                    <tr key={log.id}>
                      <td style={{ whiteSpace: 'nowrap', color: 'var(--text-2)', fontSize: 12 }}>
                        {new Date(log.createdAt).toLocaleString()}
                      </td>
                      <td>
                        <span className={`badge badge-${getAuditBadgeColor(log.action)}`}>
                          {formatAction(log.action)}
                        </span>
                      </td>
                      <td style={{ fontSize: 12, color: 'var(--text-2)' }}>{log.performedByEmail}</td>
                      <td style={{ fontSize: 12 }}>{log.details}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {activeTab === 'chat' && (
          <GroupChat groupId={id} />
        )}

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
                <label className="form-label">Parent Wallet (optional)</label>
                <select className="form-input" value={walletParentId} onChange={e => setWalletParentId(e.target.value)}>
                  <option value="">None (Root Wallet)</option>
                  {wallets.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
                </select>
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
          <Modal title="New transaction" onClose={() => { setTxAiValidation(null); setShowTxModal(false); }}>
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
              {txAiValidation && (
                <div
                  className="mb-12"
                  style={{
                    padding: '10px 12px',
                    borderRadius: 8,
                    background: txAiValidation.valid ? 'rgba(22, 163, 74, 0.12)' : 'rgba(220, 38, 38, 0.12)',
                    border: `1px solid ${txAiValidation.valid ? 'rgba(22, 163, 74, 0.35)' : 'rgba(220, 38, 38, 0.35)'}`,
                    color: txAiValidation.valid ? '#16a34a' : '#dc2626',
                    fontSize: 13,
                  }}
                >
                  AI validation: {txAiValidation.reason}
                </div>
              )}
              <div className="modal-actions">
                <button type="button" className="btn" onClick={() => { setTxAiValidation(null); setShowTxModal(false); }}>Cancel</button>
                <button type="submit" className="btn btn-primary">Submit</button>
              </div>
            </form>
          </Modal>
        )}

        {/* Virtual Card Detail Modal */}
        {showCardModal && selectedCard && (
          <Modal title="Virtual Card Details" onClose={() => setShowCardModal(false)}>
            <div className="virtual-card virtual-card-lg">
              <div className="vc-chip"></div>
              <div className="vc-number" style={{ fontSize: 20, letterSpacing: 3 }}>
                {selectedCard.cardNumber.replace(/(.{4})/g, '$1 ').trim()}
              </div>
              <div className="vc-row">
                <div>
                  <div className="vc-label">WALLET</div>
                  <div className="vc-value">{selectedCard.walletName}</div>
                </div>
                <div>
                  <div className="vc-label">EXPIRES</div>
                  <div className="vc-value">{selectedCard.expiryDate}</div>
                </div>
                <div>
                  <div className="vc-label">CVV</div>
                  <div className="vc-value">{selectedCard.cvv}</div>
                </div>
              </div>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn" onClick={() => setShowCardModal(false)}>Close</button>
            </div>
          </Modal>
        )}
      </main>
    </div>
  );
}

function getAuditBadgeColor(action) {
  if (action.includes('APPROVED')) return 'approved';
  if (action.includes('DECLINED')) return 'declined';
  if (action.includes('CREATED') || action.includes('JOINED') || action.includes('GENERATED')) return 'admin';
  if (action.includes('CHANGED')) return 'pending';
  return 'member';
}

function formatAction(action) {
  return action.replace(/_/g, ' ');
}