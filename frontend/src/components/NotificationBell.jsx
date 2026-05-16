import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';

export default function NotificationBell() {
  const [notifications, setNotifications] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const ref = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 5000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setIsOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const fetchNotifications = async () => {
    try {
      if (!api.getToken()) return;
      const data = await api.getNotifications();
      if (Array.isArray(data)) setNotifications(data);
    } catch (_) {}
  };

  const markAsRead = async (id) => {
    try {
      await api.markNotificationRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
    } catch (_) {}
  };

   const markAllRead = () => notifications.filter(n => !n.read).forEach(n => markAsRead(n.id));

   const handleNotifClick = async (n) => {
     if (!n.read) await markAsRead(n.id);
     if (n.targetUrl) {
       setIsOpen(false);
       navigate(n.targetUrl);
     }
   };

  const unreadCount = notifications.filter(n => !n.read).length;

  const fmt = (dt) => {
    try {
      const d = new Date(dt);
      return `${d.toLocaleDateString()} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
    } catch (_) { return ''; }
  };

  return (
    <div ref={ref} style={{ position: 'relative' }}>
      <button className="btn notif-btn" onClick={() => setIsOpen(o => !o)}>
        <div className="notif-icon-container">
          <span className="notif-bell-icon">🔔</span>
          {unreadCount > 0 && (
            <span className="notif-badge-bubble">{unreadCount}</span>
          )}
        </div>
        <span className="notif-label">Notifications</span>
      </button>

      {isOpen && (
        <div className="notif-panel">
          <div className="notif-panel-header">
            <span>🔔 Notifications</span>
            {unreadCount > 0 && (
              <button onClick={markAllRead} style={{ fontSize: '11px', color: 'var(--text-2)', background: 'none', border: 'none', cursor: 'pointer' }}>
                Mark all read
              </button>
            )}
          </div>
          <div className="notif-panel-body">
            {notifications.length === 0 ? (
              <div style={{ padding: '32px 16px', textAlign: 'center', color: 'var(--text-2)', fontSize: '13px' }}>
                No notifications yet.
              </div>
            ) : notifications.map(n => (
              <div
                key={n.id}
                className={`notif-item ${n.read ? 'read' : 'unread'}`}
                onClick={() => handleNotifClick(n)}
                style={{ cursor: n.targetUrl ? 'pointer' : 'default' }}
              >
                <div className="notif-item-meta">
                  <span className="notif-item-group">{n.groupName || 'SYSTEM'}</span>
                  <span className="notif-item-time">{fmt(n.createdAt)}</span>
                </div>
                <div className="notif-item-msg">{n.message}</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
