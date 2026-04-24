import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import api from '../api';

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = localStorage.getItem('fairpay_email') || '';

  const links = [
    { path: '/', label: 'Groups' },
  ];

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">FairPay</div>
      <nav className="sidebar-nav">
        {links.map(l => (
          <button
            key={l.path}
            className={`sidebar-link ${location.pathname === l.path ? 'active' : ''}`}
            onClick={() => navigate(l.path)}
          >
            {l.label}
          </button>
        ))}
      </nav>
      <div className="sidebar-bottom">
        <div className="sidebar-email">{email}</div>
        <button
          className="sidebar-link"
          onClick={() => { api.clearToken(); navigate('/login'); }}
        >
          Log out
        </button>
      </div>
    </aside>
  );
}
