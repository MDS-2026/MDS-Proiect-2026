import React, { useState } from 'react';
import api from '../api';

export default function DemocracyDmModal({ groupId, sessionId, goalText, onClose, onSubmitted }) {
  const [form, setForm] = useState({
    startDate: '',
    endDate: '',
    dealBreakers: '',
    maxContribution: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await api.submitDemocracyConstraints(groupId, {
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        dealBreakers: form.dealBreakers || null,
        maxContribution: form.maxContribution ? parseFloat(form.maxContribution) : null,
      });
      onSubmitted?.();
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to submit preferences');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div style={styles.modal}>
        <div style={styles.header}>
          <div style={styles.agentBadge}>🤖 Democracy Agent</div>
          <button style={styles.closeBtn} onClick={onClose}>✕</button>
        </div>

        <div style={styles.body}>
          <div style={styles.goalBox}>
            <span style={styles.goalLabel}>Group goal:</span>
            <span style={styles.goalText}>"{goalText}"</span>
          </div>
          <p style={styles.intro}>
            Share your preferences privately. Your answers are only used to generate fair group options.
          </p>

          <form onSubmit={handleSubmit}>
            <div style={styles.row}>
              <div style={styles.field}>
                <label style={styles.label}>Preferred start date</label>
                <input
                  style={styles.input}
                  type="date"
                  name="startDate"
                  value={form.startDate}
                  onChange={handleChange}
                />
              </div>
              <div style={styles.field}>
                <label style={styles.label}>Preferred end date</label>
                <input
                  style={styles.input}
                  type="date"
                  name="endDate"
                  value={form.endDate}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div style={styles.field}>
              <label style={styles.label}>Deal-breakers (optional)</label>
              <textarea
                style={{ ...styles.input, resize: 'vertical', minHeight: 72 }}
                name="dealBreakers"
                placeholder="e.g. no smoking rooms, must have parking, not further than 30 min from city center..."
                value={form.dealBreakers}
                onChange={handleChange}
                maxLength={500}
              />
            </div>

            <div style={styles.field}>
              <label style={styles.label}>Maximum individual contribution (€)</label>
              <input
                style={styles.input}
                type="number"
                name="maxContribution"
                placeholder="e.g. 150"
                min="1"
                step="1"
                value={form.maxContribution}
                onChange={handleChange}
              />
            </div>

            {error && <div style={styles.error}>{error}</div>}

            <div style={styles.actions}>
              <button type="button" style={styles.cancelBtn} onClick={onClose} disabled={submitting}>
                Skip for now
              </button>
              <button type="submit" style={styles.submitBtn} disabled={submitting}>
                {submitting ? 'Submitting…' : 'Submit preferences'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

const styles = {
  overlay: {
    position: 'fixed', inset: 0,
    background: 'rgba(0,0,0,0.55)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    zIndex: 1000,
  },
  modal: {
    background: 'var(--surface)',
    border: '1px solid var(--border)',
    borderRadius: 16,
    width: '100%', maxWidth: 500,
    boxShadow: '0 8px 40px rgba(0,0,0,0.28)',
    overflow: 'hidden',
  },
  header: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '16px 20px',
    background: 'var(--surface-2)',
    borderBottom: '1px solid var(--border)',
  },
  agentBadge: {
    fontWeight: 700, fontSize: 15, color: 'var(--text)',
    display: 'flex', alignItems: 'center', gap: 8,
  },
  closeBtn: {
    background: 'none', border: 'none',
    color: 'var(--text-2)', cursor: 'pointer',
    fontSize: 16, padding: 4, lineHeight: 1,
  },
  body: { padding: '20px 24px' },
  goalBox: {
    background: 'var(--surface-2)',
    border: '1px solid var(--border)',
    borderRadius: 10, padding: '10px 14px',
    marginBottom: 12, display: 'flex', flexDirection: 'column', gap: 2,
  },
  goalLabel: { fontSize: 11, color: 'var(--text-2)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' },
  goalText: { fontSize: 14, color: 'var(--text)', fontStyle: 'italic' },
  intro: { fontSize: 13, color: 'var(--text-2)', marginBottom: 18, lineHeight: 1.5 },
  row: { display: 'flex', gap: 12, marginBottom: 14 },
  field: { display: 'flex', flexDirection: 'column', gap: 5, flex: 1, marginBottom: 14 },
  label: { fontSize: 12, fontWeight: 600, color: 'var(--text-2)' },
  input: {
    padding: '9px 12px',
    borderRadius: 8,
    border: '1px solid var(--border)',
    background: 'var(--surface)',
    color: 'var(--text)',
    fontSize: 14,
    outline: 'none',
    width: '100%',
    boxSizing: 'border-box',
  },
  error: {
    background: 'rgba(220,38,38,0.1)',
    color: '#ef4444',
    borderRadius: 8,
    padding: '8px 12px',
    fontSize: 13,
    marginBottom: 14,
  },
  actions: { display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 6 },
  cancelBtn: {
    padding: '9px 18px', borderRadius: 8,
    border: '1px solid var(--border)',
    background: 'transparent', color: 'var(--text-2)',
    cursor: 'pointer', fontSize: 14,
  },
  submitBtn: {
    padding: '9px 20px', borderRadius: 8,
    border: 'none',
    background: 'var(--blue)', color: '#fff',
    cursor: 'pointer', fontSize: 14, fontWeight: 600,
  },
};
