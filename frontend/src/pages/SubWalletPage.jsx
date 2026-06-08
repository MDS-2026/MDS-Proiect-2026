import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api';
import Sidebar from '../components/Sidebar';
import Modal from '../components/Modal';
import GroupChat from '../components/GroupChat';
import NotificationBell from '../components/NotificationBell';

// Mirrors GroupPage but scoped to a single sub-wallet: its own members, virtual card,
// transactions, subtree and isolated private chat.
export default function SubWalletPage() {
  const { id, walletId } = useParams();
  const navigate = useNavigate();

  const [group, setGroup] = useState(null);
  const [wallets, setWallets] = useState([]);
  const [walletTree, setWalletTree] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [card, setCard] = useState(null);
  const [access, setAccess] = useState(null);
  const [members, setMembers] = useState([]);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('overview');

  // Virtual card modal
  const [showCardModal, setShowCardModal] = useState(false);

  // Member assignment
  const [assignUserId, setAssignUserId] = useState('');

  // Transaction form
  const [showTxModal, setShowTxModal] = useState(false);
  const [txAmount, setTxAmount] = useState('');
  const [txMerchant, setTxMerchant] = useState('');
  const [txCategory, setTxCategory] = useState('');
  const [txAiValidation, setTxAiValidation] = useState(null);
  const [txSubmitting, setTxSubmitting] = useState(false);
  const [txError, setTxError] = useState('');

  useEffect(() => { loadAll(); }, [id, walletId]);

  const loadAll = async () => {
    try {
      const [g, w, tree, txs, acc, mem, c] = await Promise.all([
        api.getGroup(id),
        api.getWallets(id),
        api.getWalletTree(id),
        api.getWalletTransactions(walletId),
        api.getSubWalletAccess(walletId),
        api.getSubWalletMembers(walletId).catch(() => []),
        api.getVirtualCard(id, walletId).catch(() => null),
      ]);
      setGroup(g);
      setWallets(w);
      setWalletTree(tree);
      setTransactions(txs);
      setAccess(acc);
      setMembers(mem || []);
      setCard(c);
    } catch (err) { setError(err.message); }
  };

  const wallet = wallets.find(w => w.id === walletId);
  const node = findNode(walletTree, walletId);
  const isAdmin = access?.admin;
  const canParticipate = access?.canParticipate;

  const handleAssignMember = async (e) => {
    e.preventDefault();
    if (!assignUserId) return;
    try {
      await api.assignSubWalletMembers(walletId, [assignUserId]);
      setAssignUserId('');
      loadAll();
    } catch (err) { setError(err.message); }
  };

  const handleRemoveMember = async (userId) => {
    try {
      await api.removeSubWalletMember(walletId, userId);
      loadAll();
    } catch (err) { setError(err.message); }
  };

  const handleCreateTx = async (e) => {
    e.preventDefault();
    if (txSubmitting) return;
    setTxSubmitting(true);
    setTxError('');
    try {
      const validation = await api.validateTransaction({
        walletId, amount: parseFloat(txAmount), merchant: txMerchant, category: txCategory,
      });
      setTxAiValidation(validation);
      await api.createTransaction(walletId, {
        amount: parseFloat(txAmount), merchant: txMerchant, category: txCategory,
      });
      setTimeout(() => {
        setShowTxModal(false);
        setTxAmount(''); setTxMerchant(''); setTxCategory('');
        setTxAiValidation(null); setTxError('');
        loadAll();
      }, 1500);
    } catch (err) {
      setTxError(err.message);
    } finally {
      setTxSubmitting(false);
    }
  };

  const assignable = (group?.members || []).filter(
    m => m.role !== 'ADMIN' && !members.some(s => s.userId === m.userId)
  );

  const tabs = [
    { key: 'overview', label: 'Overview' },
    { key: 'tree', label: 'Tree Dashboard' },
    { key: 'cards', label: 'Virtual Card' },
    { key: 'chat', label: 'Sub-wallet Chat' },
  ];

  if (!wallet || !access) {
    return <div className="layout"><Sidebar /><main className="main"><div className="empty">Loading...</div></main></div>;
  }

  const budget = wallet.budgetLimit || 0;
  const spent = node?.spentAmount || 0;

  return (
    <div className="layout">
      <Sidebar />
      <main className="main">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }} className="mb-24">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <button className="btn btn-sm" onClick={() => navigate(`/groups/${id}`)}>←</button>
            <h1 className="page-title" style={{ marginBottom: 0 }}>
              {getIconForPurpose(wallet.purpose)} {wallet.name}
            </h1>
            <span className="badge badge-member" style={{ marginLeft: 4 }}>Sub-wallet</span>
            <span style={{ color: 'var(--text-2)', fontSize: 13 }}>of {group?.name}</span>
          </div>
          <NotificationBell />
        </div>

        {error && <div className="error mb-12">{error}</div>}

        {!canParticipate && (
          <div className="card mb-24" style={{ borderLeft: '3px solid var(--yellow)' }}>
            <div style={{ fontWeight: 600 }}>Read-only view</div>
            <div className="stat-sub" style={{ marginTop: 4 }}>
              You are not a member of this sub-wallet. You can view its ledger, but cannot
              chat, approve transactions or use its virtual card.
            </div>
          </div>
        )}

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
            <div className="card-grid">
              <div className="card">
                <div className="stat-label">Budget</div>
                <div className="stat-value">€{budget.toFixed(2)}</div>
              </div>
              <div className="card">
                <div className="stat-label">Spent</div>
                <div className="stat-value">€{spent.toFixed(2)}</div>
              </div>
              <div className="card">
                <div className="stat-label">Members</div>
                <div className="stat-value">{members.length}</div>
              </div>
              <div className="card">
                <div className="stat-label">Auto-approve</div>
                <div className="stat-value">€{(wallet.autoApproveThreshold || 0).toFixed(2)}</div>
              </div>
            </div>

            {/* Members */}
            <div className="table-section card">
              <div className="table-header">
                <div className="table-title">Sub-wallet Members</div>
              </div>
              {isAdmin && (
                <form onSubmit={handleAssignMember} className="flex-gap mb-12" style={{ alignItems: 'center' }}>
                  <select className="form-input" style={{ maxWidth: 280 }} value={assignUserId} onChange={e => setAssignUserId(e.target.value)}>
                    <option value="">Assign a root-wallet member…</option>
                    {assignable.map(m => <option key={m.userId} value={m.userId}>{m.email}</option>)}
                  </select>
                  <button type="submit" className="btn btn-sm btn-primary" disabled={!assignUserId}>Assign</button>
                </form>
              )}
              {members.length === 0 ? <div className="empty">No members assigned yet.</div> : (
                <table className="data-table">
                  <thead><tr><th>Email</th><th>Access</th>{isAdmin && <th>Actions</th>}</tr></thead>
                  <tbody>
                    {members.map(m => (
                      <tr key={m.userId}>
                        <td>{m.email}</td>
                        <td>
                          <span className={`badge ${m.admin ? 'badge-admin' : 'badge-member'}`}>
                            {m.admin ? 'ADMIN (all sub-wallets)' : 'MEMBER'}
                          </span>
                        </td>
                        {isAdmin && (
                          <td>
                            {m.admin ? (
                              <span style={{ fontSize: 11, color: 'var(--text-2)' }}>—</span>
                            ) : (
                              <button className="btn btn-sm btn-danger" onClick={() => handleRemoveMember(m.userId)}>Remove</button>
                            )}
                          </td>
                        )}
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
                {canParticipate && (
                  <button className="btn btn-sm" onClick={() => { setTxAiValidation(null); setTxError(''); setShowTxModal(true); }}>+ New</button>
                )}
              </div>
              {transactions.length === 0 ? <div className="empty">No transactions</div> : (
                <table className="data-table">
                  <thead><tr><th>Merchant</th><th>Category</th><th>Amount</th><th>Status</th>{canParticipate && <th>Actions</th>}</tr></thead>
                  <tbody>
                    {transactions.map(tx => (
                      <tr key={tx.id}>
                        <td>{tx.merchant}</td>
                        <td style={{ color: 'var(--text-2)' }}>{tx.category || '—'}</td>
                        <td>€{tx.amount.toFixed(2)}</td>
                        <td>
                          <span className={`badge badge-${txStatusBadgeClass(tx.status)}`}>{tx.status}</span>
                          {(tx.status === 'PENDING_GROUP_APPROVAL' || tx.status === 'PENDING_MANUAL_APPROVAL') && tx.groupConsensusRequired != null && (
                            <div className="stat-sub" style={{ marginTop: 6 }}>
                              Approvals: {tx.groupConsensusApproved ?? 0}/{tx.groupConsensusRequired} (sub-wallet majority)
                            </div>
                          )}
                        </td>
                        {canParticipate && (
                          <td>
                            {(tx.status === 'PENDING' || tx.status === 'PENDING_GROUP_APPROVAL' || tx.status === 'PENDING_MANUAL_APPROVAL') && (
                              <div className="flex-gap">
                                <button className="btn btn-sm" onClick={async () => { await api.approveTransaction(tx.id); loadAll(); }}>Approve</button>
                                <button className="btn btn-sm btn-danger" onClick={async () => { await api.declineTransaction(tx.id); loadAll(); }}>Decline</button>
                              </div>
                            )}
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        )}

        {activeTab === 'tree' && (
          <div className="card" style={{ overflow: 'hidden' }}>
            <div className="table-header" style={{ padding: '0 20px 20px 20px' }}>
              <div className="table-title">Sub-wallet Hierarchy</div>
            </div>
            {!node ? <div className="empty">No data.</div> : (
              <div className="tree-container">
                {renderTreeNode(node, true, 0)}
              </div>
            )}
          </div>
        )}

        {activeTab === 'cards' && (
          <div className="table-section card">
            <div className="table-header">
              <div className="table-title">Virtual Card</div>
            </div>
            {!card ? <div className="empty">No virtual card for this sub-wallet.</div> : !canParticipate ? (
              <div className="empty">The virtual card is private to this sub-wallet's members.</div>
            ) : (
              <div className="card-grid">
                <div className="virtual-card" onClick={() => setShowCardModal(true)}>
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
              </div>
            )}
          </div>
        )}

        {activeTab === 'chat' && (
          canParticipate ? (
            <GroupChat groupId={id} channelId={walletId} variant="subwallet" />
          ) : (
            <div className="card"><div className="empty">
              This sub-wallet's chat is private to its members.
            </div></div>
          )
        )}

        {/* New transaction modal */}
        {showTxModal && (
          <Modal title={`New transaction — ${wallet.name}`} onClose={() => { setTxAiValidation(null); setShowTxModal(false); }}>
            <form onSubmit={handleCreateTx}>
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
                <div className="mb-12" style={{
                  padding: '10px 12px', borderRadius: 8,
                  background: txAiValidation.valid ? 'rgba(22, 163, 74, 0.12)' : 'rgba(220, 38, 38, 0.12)',
                  border: `1px solid ${txAiValidation.valid ? 'rgba(22, 163, 74, 0.35)' : 'rgba(220, 38, 38, 0.35)'}`,
                  color: txAiValidation.valid ? '#16a34a' : '#dc2626', fontSize: 13,
                }}>
                  AI validation: {txAiValidation.reason}
                </div>
              )}
              {txError && (
                <div className="mb-12" style={{
                  padding: '10px 12px', borderRadius: 8,
                  background: 'rgba(220, 38, 38, 0.12)', border: '1px solid rgba(220, 38, 38, 0.35)',
                  color: '#dc2626', fontSize: 13,
                }}>{txError}</div>
              )}
              <div className="modal-actions">
                <button type="button" className="btn" onClick={() => { setTxAiValidation(null); setTxError(''); setShowTxModal(false); }}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={txSubmitting}>
                  {txSubmitting ? 'Submitting…' : 'Submit'}
                </button>
              </div>
            </form>
          </Modal>
        )}

        {/* Virtual card detail modal */}
        {showCardModal && card && (
          <Modal title="Virtual Card Details" onClose={() => setShowCardModal(false)}>
            <div className="virtual-card virtual-card-lg">
              <div className="vc-chip"></div>
              <div className="vc-number" style={{ fontSize: 20, letterSpacing: 3 }}>
                {card.cardNumber.replace(/(.{4})/g, '$1 ').trim()}
              </div>
              <div className="vc-row">
                <div><div className="vc-label">WALLET</div><div className="vc-value">{card.walletName}</div></div>
                <div><div className="vc-label">EXPIRES</div><div className="vc-value">{card.expiryDate}</div></div>
                <div><div className="vc-label">CVV</div><div className="vc-value">{card.cvv}</div></div>
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

function findNode(nodes, targetId) {
  for (const n of nodes || []) {
    if (n.id === targetId) return n;
    const f = findNode(n.children, targetId);
    if (f) return f;
  }
  return null;
}

function getIconForPurpose(purpose) {
  const p = (purpose || '').toLowerCase();
  if (p.includes('travel') || p.includes('flight')) return '✈️';
  if (p.includes('food') || p.includes('grocer') || p.includes('restaurant')) return '🍔';
  if (p.includes('entertain') || p.includes('party') || p.includes('fun')) return '🎉';
  if (p.includes('transport') || p.includes('uber') || p.includes('taxi')) return '🚕';
  if (p.includes('house') || p.includes('rent') || p.includes('util')) return '🏠';
  return '💼';
}

function renderTreeNode(node, isRoot = false, depth = 0) {
  const percent = Math.min(100, (node.spentAmount / node.budgetLimit) * 100) || 0;
  const progressColor = percent > 90 ? 'var(--red)' : percent > 75 ? 'var(--yellow)' : 'var(--blue)';
  const icon = getIconForPurpose(node.purpose);
  const delay = depth * 0.15;

  return (
    <div key={node.id} className={`tree-node ${isRoot ? 'root-node' : ''}`} style={{ animationDelay: `${delay}s` }}>
      <div className="tree-card">
        <div className="tree-card-header">
          <div className="tree-card-title-wrap">
            <div className="tree-card-icon">{icon}</div>
            <div>
              <div className="tree-card-title">{node.name}</div>
              <div className="tree-card-purpose">{node.purpose || 'General'}</div>
            </div>
          </div>
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
          <div className="tree-progress-bar" style={{ width: `${percent}%`, backgroundColor: progressColor, boxShadow: `0 0 10px ${progressColor}80` }}></div>
        </div>
      </div>
      {node.children && node.children.length > 0 && (
        <div className="tree-children">
          {node.children.map(child => renderTreeNode(child, false, depth + 1))}
        </div>
      )}
    </div>
  );
}

function txStatusBadgeClass(status) {
  if (status === 'PENDING_GROUP_APPROVAL' || status === 'PENDING_MANUAL_APPROVAL') return 'pending';
  return status.toLowerCase();
}
