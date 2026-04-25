import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import api from '../api';

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = localStorage.getItem('fairpay_email') || '';
  const [groups, setGroups] = useState([]);
  const [isGroupsOpen, setIsGroupsOpen] = useState(true);

  useEffect(() => {
    if (api.getToken()) {
      api.getGroups()
        .then(data => setGroups(data))
        .catch(err => console.error("Failed to load groups for sidebar", err));
    }
  }, []);

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">FairPay</div>
      <nav className="sidebar-nav">
        <button
          className={`sidebar-link ${location.pathname === '/' ? 'active' : ''}`}
          onClick={() => navigate('/')}
        >
          Home
        </button>

        {groups.length > 0 && (
          <div style={{ marginTop: '24px' }}>
            <div 
              style={{ 
                fontSize: '11px', 
                color: 'var(--text-2)', 
                letterSpacing: '0.05em', 
                textTransform: 'uppercase', 
                marginBottom: '8px', 
                paddingLeft: '12px',
                fontWeight: 600,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                paddingRight: '12px'
              }}
              onClick={() => setIsGroupsOpen(!isGroupsOpen)}
            >
              <span>My Groups</span>
              <span style={{ transform: isGroupsOpen ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s', fontSize: '10px' }}>▼</span>
            </div>
            
            {isGroupsOpen && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', animation: 'modalIn 0.2s ease-out' }}>
                {groups.map(g => (
                  <button
                    key={g.id}
                    className={`sidebar-link ${location.pathname === `/groups/${g.id}` ? 'active' : ''}`}
                    onClick={() => navigate(`/groups/${g.id}`)}
                    style={{ paddingLeft: '24px' }}
                  >
                    <span style={{ color: 'var(--accent)', marginRight: '6px', opacity: 0.5 }}>#</span>
                    {g.name}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}
      </nav>
      <div className="sidebar-bottom">
        <div className="sidebar-email">{email}</div>
        <button
          className="sidebar-link"
          onClick={() => { api.clearToken(); window.location.href = '/login'; }}
        >
          Log out
        </button>
      </div>
    </aside>
  );
}
