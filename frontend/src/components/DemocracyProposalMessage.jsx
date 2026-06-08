import React, { useState } from 'react';
import api from '../api';

const OPTION_COLORS = ['#6366f1', '#10b981', '#f59e0b'];
const OPTION_LABELS = ['A', 'B', 'C'];

export default function DemocracyProposalMessage({ groupId, metadata, onVoted }) {
  const [voting, setVoting] = useState(false);
  const [error, setError] = useState('');

  let parsed = null;
  try {
    parsed = typeof metadata === 'string' ? JSON.parse(metadata) : metadata;
  } catch (_) {
    return null;
  }

  if (!parsed || !parsed.options) return null;

  const { sessionId, goal, options, votes = {} } = parsed;
  const totalVotes = Object.values(votes).reduce((s, v) => s + (Number(v) || 0), 0);

  const handleVote = async (optionIndex) => {
    if (voting) return;
    setError('');
    setVoting(true);
    try {
      const updatedVotes = await api.castDemocracyVote(groupId, sessionId, optionIndex);
      onVoted?.(updatedVotes);
    } catch (err) {
      setError(err.message || 'Vote failed');
    } finally {
      setVoting(false);
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.agentHeader}>
        <span style={styles.agentIcon}>🗳️</span>
        <div>
          <div style={styles.agentTitle}>Democracy Agent — 3 Proposals</div>
          <div style={styles.agentGoal}>Goal: "{goal}"</div>
        </div>
      </div>

      <div style={styles.options}>
        {(Array.isArray(options) ? options : []).map((opt) => {
          const idx = opt.index ?? opt.index;
          const voteCount = Number(votes[String(idx)] ?? 0);
          const pct = totalVotes > 0 ? Math.round((voteCount / totalVotes) * 100) : 0;
          const color = OPTION_COLORS[idx] || '#6366f1';
          const label = OPTION_LABELS[idx] || String(idx + 1);

          return (
            <div key={idx} style={{ ...styles.optionCard, borderColor: color + '44' }}>
              <div style={styles.optionTop}>
                <div style={{ ...styles.optionBadge, background: color }}>
                  {label}
                </div>
                <div style={styles.optionInfo}>
                  <div style={styles.optionTitle}>{opt.title}</div>
                  <div style={styles.optionDesc}>{opt.description}</div>
                  {opt.highlights && (
                    <div style={styles.highlights}>
                      {opt.highlights.map((h, i) => (
                        <span key={i} style={{ ...styles.highlight, borderColor: color + '66', color }}>
                          {h}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
                <div style={styles.priceCol}>
                  <div style={styles.price}>
                    €{typeof opt.estimatedPricePerPerson === 'number'
                      ? opt.estimatedPricePerPerson.toFixed(0)
                      : '?'}
                  </div>
                  <div style={styles.priceSub}>per person</div>
                  {opt.fairnessScore != null && (
                    <div style={{ ...styles.fairness, color }}>
                      {opt.fairnessScore}% fair
                    </div>
                  )}
                </div>
              </div>

              <div style={styles.voteRow}>
                <div style={styles.voteBarWrap}>
                  <div
                    style={{ ...styles.voteBar, width: pct + '%', background: color }}
                  />
                </div>
                <span style={styles.voteCount}>{voteCount} vote{voteCount !== 1 ? 's' : ''}</span>
                <button
                  style={{ ...styles.voteBtn, background: color }}
                  onClick={() => handleVote(idx)}
                  disabled={voting}
                >
                  {voting ? '…' : 'Vote'}
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {error && <div style={styles.error}>{error}</div>}
      <div style={styles.footer}>
        {totalVotes} vote{totalVotes !== 1 ? 's' : ''} cast · Weighted by fairness score
      </div>
    </div>
  );
}

const styles = {
  container: {
    background: 'var(--surface-2)',
    border: '1px solid var(--border)',
    borderRadius: 14,
    overflow: 'hidden',
    marginBottom: 4,
    maxWidth: 520,
  },
  agentHeader: {
    display: 'flex', alignItems: 'center', gap: 12,
    padding: '12px 16px',
    borderBottom: '1px solid var(--border)',
    background: 'rgba(99,102,241,0.06)',
  },
  agentIcon: { fontSize: 22, flexShrink: 0 },
  agentTitle: { fontWeight: 700, fontSize: 14, color: 'var(--text)' },
  agentGoal: { fontSize: 12, color: 'var(--text-2)', fontStyle: 'italic', marginTop: 2 },
  options: { display: 'flex', flexDirection: 'column', gap: 0 },
  optionCard: {
    padding: '12px 14px',
    borderBottom: '1px solid var(--border)',
    borderLeft: '3px solid transparent',
  },
  optionTop: { display: 'flex', gap: 12, alignItems: 'flex-start', marginBottom: 10 },
  optionBadge: {
    width: 28, height: 28, borderRadius: '50%',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontWeight: 800, color: '#fff', fontSize: 13, flexShrink: 0, marginTop: 2,
  },
  optionInfo: { flex: 1 },
  optionTitle: { fontWeight: 700, fontSize: 13, color: 'var(--text)', marginBottom: 3 },
  optionDesc: { fontSize: 12, color: 'var(--text-2)', lineHeight: 1.5 },
  highlights: { display: 'flex', flexWrap: 'wrap', gap: 5, marginTop: 6 },
  highlight: {
    fontSize: 10, fontWeight: 600,
    border: '1px solid',
    borderRadius: 20, padding: '2px 8px',
  },
  priceCol: { textAlign: 'right', flexShrink: 0 },
  price: { fontWeight: 800, fontSize: 18, color: 'var(--text)' },
  priceSub: { fontSize: 10, color: 'var(--text-2)' },
  fairness: { fontSize: 11, fontWeight: 700, marginTop: 4 },
  voteRow: { display: 'flex', alignItems: 'center', gap: 10 },
  voteBarWrap: {
    flex: 1, height: 5, background: 'var(--border)',
    borderRadius: 4, overflow: 'hidden',
  },
  voteBar: { height: '100%', borderRadius: 4, transition: 'width 0.4s ease' },
  voteCount: { fontSize: 11, color: 'var(--text-2)', flexShrink: 0, minWidth: 50, textAlign: 'right' },
  voteBtn: {
    padding: '5px 14px', borderRadius: 20,
    border: 'none', color: '#fff',
    cursor: 'pointer', fontSize: 12, fontWeight: 600,
    flexShrink: 0, transition: 'opacity 0.2s',
  },
  error: {
    margin: '0 14px 10px',
    background: 'rgba(220,38,38,0.1)',
    color: '#ef4444',
    borderRadius: 8, padding: '6px 10px', fontSize: 12,
  },
  footer: {
    padding: '8px 14px',
    fontSize: 11, color: 'var(--text-2)',
    textAlign: 'center',
  },
};
