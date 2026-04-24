import React, { useState } from 'react';
import './index.css';

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');

  const transactions = [
    { id: 1, merchant: 'Airbnb', category: 'Travel', amount: -450.00, date: '2026-04-20', status: 'APPROVED', wallet: 'Paris Trip' },
    { id: 2, merchant: 'Carrefour', category: 'Groceries', amount: -85.50, date: '2026-04-21', status: 'APPROVED', wallet: 'Shared Groceries' },
    { id: 3, merchant: 'Group Deposit', category: 'Deposit', amount: 500.00, date: '2026-04-22', status: 'APPROVED', wallet: 'Main Wallet' },
    { id: 4, merchant: 'Uber', category: 'Transport', amount: -24.00, date: '2026-04-23', status: 'PENDING', wallet: 'Paris Trip' },
  ];

  const assets = [
    { type: 'CASH', provider: 'EUR Account', value: '€1,250.00' },
    { type: 'MILES', provider: 'FlyingBlue', value: '45,000 Miles' },
    { type: 'VOUCHER', provider: 'Amazon', value: '€150.00' }
  ];

  return (
    <div className="app-container">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="logo">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 12V7H5a2 2 0 0 1 0-4h14v4" />
            <path d="M3 5v14a2 2 0 0 0 2 2h16v-5" />
            <path d="M18 12a2 2 0 0 0 0 4h4v-4Z" />
          </svg>
          FairPay
        </div>
        <nav>
          <div className={`nav-item ${activeTab === 'dashboard' ? 'active' : ''}`} onClick={() => setActiveTab('dashboard')}>Dashboard</div>
          <div className={`nav-item ${activeTab === 'wallets' ? 'active' : ''}`} onClick={() => setActiveTab('wallets')}>Wallets</div>
          <div className={`nav-item ${activeTab === 'transactions' ? 'active' : ''}`} onClick={() => setActiveTab('transactions')}>Transactions</div>
          <div className={`nav-item ${activeTab === 'group' ? 'active' : ''}`} onClick={() => setActiveTab('group')}>Group Settings</div>
        </nav>
      </aside>

      {/* Main Content */}
      <main className="main-content">
        <header className="header">
          <h1 className="title">Group Overview</h1>
          <div className="user-profile">
            <button className="btn btn-primary">+ Add Funds</button>
            <div className="avatar">AL</div>
          </div>
        </header>

        {/* Dashboard Grid */}
        <div className="dashboard-grid">
          {assets.map((asset, idx) => (
            <div className="card" key={idx}>
              <h3 className="card-title">Pooled {asset.type}</h3>
              <div className="card-value">{asset.value}</div>
              <div className="card-subtitle">{asset.provider}</div>
            </div>
          ))}
        </div>

        <div className="dashboard-grid" style={{ gridTemplateColumns: '1fr 1fr' }}>
          {/* Wallets Overview */}
          <div className="transactions-section">
            <div className="section-header">
              <h3>Active Wallets</h3>
              <button className="btn btn-primary" style={{ padding: '0.25rem 0.75rem', fontSize: '0.85rem' }}>+ New</button>
            </div>
            <div className="transaction-list">
              <div className="transaction-item">
                <div className="tx-info">
                  <div className="tx-icon">✈️</div>
                  <div className="tx-details">
                    <h4>Paris Trip</h4>
                    <p>Budget: €1000.00</p>
                  </div>
                </div>
                <div className="tx-amount negative">€526.00 Left</div>
              </div>
              <div className="transaction-item">
                <div className="tx-info">
                  <div className="tx-icon">🛒</div>
                  <div className="tx-details">
                    <h4>Shared Groceries</h4>
                    <p>Budget: €300.00</p>
                  </div>
                </div>
                <div className="tx-amount negative">€214.50 Left</div>
              </div>
            </div>
          </div>

          {/* Recent Transactions */}
          <div className="transactions-section">
            <div className="section-header">
              <h3>Recent Activity</h3>
              <span style={{ fontSize: '0.85rem', color: 'var(--primary)', cursor: 'pointer' }}>View All</span>
            </div>
            <div className="transaction-list">
              {transactions.map(tx => (
                <div className="transaction-item" key={tx.id}>
                  <div className="tx-info">
                    <div className="tx-icon">{tx.amount > 0 ? '↓' : '↑'}</div>
                    <div className="tx-details">
                      <h4>{tx.merchant}</h4>
                      <p>{tx.wallet} • {tx.date}</p>
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div className={`tx-amount ${tx.amount > 0 ? 'positive' : 'negative'}`}>
                      {tx.amount > 0 ? '+' : ''}€{Math.abs(tx.amount).toFixed(2)}
                    </div>
                    <span className={`badge badge-${tx.status.toLowerCase()}`}>{tx.status}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;
