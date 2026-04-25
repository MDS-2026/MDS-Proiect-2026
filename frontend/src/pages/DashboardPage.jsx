import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import Sidebar from '../components/Sidebar';
import Modal from '../components/Modal';

export default function DashboardPage() {
  const [groups, setGroups] = useState([]);
  const [showCreate, setShowCreate] = useState(false);
  const [showJoin, setShowJoin] = useState(false);
  const [name, setName] = useState('');
  const [inviteCode, setInviteCode] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => { loadGroups(); }, []);

  const loadGroups = async () => {
    try {
      const data = await api.getGroups();
      setGroups(data);
    } catch (err) {
      setError(err.message);
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      await api.createGroup(name);
      setShowCreate(false);
      setName('');
      loadGroups();
    } catch (err) { setError(err.message); }
  };

  const handleJoin = async (e) => {
    e.preventDefault();
    try {
      await api.joinGroup(inviteCode);
      setShowJoin(false);
      setInviteCode('');
      loadGroups();
    } catch (err) { setError(err.message); }
  };

  return (
    <div className="layout">
      <Sidebar />
      <main className="main">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingRight: '64px' }} className="mb-24">
          <h1 className="page-title" style={{ marginBottom: 0 }}>Groups</h1>
          <div className="flex-gap">
            <button className="btn" onClick={() => setShowJoin(true)}>Join group</button>
            <button className="btn btn-primary" onClick={() => setShowCreate(true)}>New group</button>
          </div>
        </div>

        {error && <div className="error mb-12">{error}</div>}

        {groups.length === 0 ? (
          <div className="card"><div className="empty">No groups yet. Create one or join with an invite code.</div></div>
        ) : (
          <div className="card-grid">
            {groups.map(g => (
              <div
                className="card"
                key={g.id}
                style={{ cursor: 'pointer' }}
                onClick={() => navigate(`/groups/${g.id}`)}
              >
                <div style={{ fontWeight: 600, fontSize: 15, marginBottom: 8 }}>{g.name}</div>
                <div style={{ fontSize: 12, color: 'var(--text-2)', marginBottom: 4 }}>
                  {g.members?.length || 0} member{g.members?.length !== 1 ? 's' : ''}
                </div>
                <div>
                  <span style={{ fontSize: 11, color: 'var(--text-2)' }}>Invite: </span>
                  <span className="invite-code">{g.inviteCode}</span>
                </div>
              </div>
            ))}
          </div>
        )}

        {showCreate && (
          <Modal title="Create group" onClose={() => setShowCreate(false)}>
            <form onSubmit={handleCreate}>
              <div className="form-group">
                <label className="form-label">Group name</label>
                <input className="form-input" value={name} onChange={e => setName(e.target.value)} required />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn" onClick={() => setShowCreate(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Create</button>
              </div>
            </form>
          </Modal>
        )}

        {showJoin && (
          <Modal title="Join group" onClose={() => setShowJoin(false)}>
            <form onSubmit={handleJoin}>
              <div className="form-group">
                <label className="form-label">Invite code</label>
                <input className="form-input" value={inviteCode} onChange={e => setInviteCode(e.target.value)} required />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn" onClick={() => setShowJoin(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Join</button>
              </div>
            </form>
          </Modal>
        )}
      </main>
    </div>
  );
}
