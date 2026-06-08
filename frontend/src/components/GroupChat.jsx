import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import api from '../api';
import DemocracyDmModal from './DemocracyDmModal';
import DemocracyProposalMessage from './DemocracyProposalMessage';

const WS_URL = 'http://localhost:8080/ws';
const AGENT_EMAIL = 'democracy-agent@fairpay.internal';

export default function GroupChat({ groupId }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState('');
  const [dmPending, setDmPending] = useState(null); // { sessionId, goalText }
  const clientRef = useRef(null);
  const bottomRef = useRef(null);
  const [currentEmail] = useState(() => localStorage.getItem('fairpay_email') || '');

  // Load chat history on mount
  useEffect(() => {
    api.getChatHistory(groupId)
      .then(setMessages)
      .catch(() => setMessages([]));
  }, [groupId]);

  // Check for pending Democracy Agent DM on mount
  useEffect(() => {
    api.getDemocracyDm(groupId)
      .then((dm) => {
        if (dm?.pending) setDmPending({ sessionId: dm.sessionId, goalText: dm.goalText });
      })
      .catch(() => {});
  }, [groupId]);

  // Auto-scroll when messages change
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Handle incoming WebSocket message (regular + democracy types)
  const handleIncomingMessage = useCallback((msg) => {
    if (msg.messageType === 'DEMOCRACY_VOTE_UPDATE') {
      // Patch vote counts into the matching DEMOCRACY_PROPOSAL message
      setMessages((prev) =>
        prev.map((m) => {
          if (m.messageType !== 'DEMOCRACY_PROPOSAL') return m;
          try {
            const meta = typeof m.metadata === 'string' ? JSON.parse(m.metadata) : m.metadata;
            if (!meta || meta.sessionId !== msg.metadata) return m;
            const updatedMeta = { ...meta, votes: JSON.parse(msg.content).votes };
            return { ...m, metadata: JSON.stringify(updatedMeta) };
          } catch (_) { return m; }
        })
      );
      return;
    }

    setMessages((prev) => [...prev, msg]);

    // Show DM modal when democracy trigger happens for current user
    if (msg.messageType === 'DEMOCRACY_TRIGGER' && msg.senderEmail === AGENT_EMAIL) {
      api.getDemocracyDm(groupId)
        .then((dm) => { if (dm?.pending) setDmPending({ sessionId: dm.sessionId, goalText: dm.goalText }); })
        .catch(() => {});
    }
  }, [groupId]);

  // Connect WebSocket
  useEffect(() => {
    const token = api.getToken();
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        setError('');
        client.subscribe(`/topic/chat/${groupId}`, (frame) => {
          const msg = JSON.parse(frame.body);
          handleIncomingMessage(msg);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setError('Connection error. Retrying...'),
    });

    client.activate();
    clientRef.current = client;
    return () => { client.deactivate(); };
  }, [groupId, handleIncomingMessage]);

  const handleSend = useCallback(
    (e) => {
      e.preventDefault();
      const text = input.trim();
      if (!text || !clientRef.current?.connected) return;
      clientRef.current.publish({
        destination: `/app/chat/${groupId}`,
        body: JSON.stringify({ content: text }),
      });
      setInput('');
    },
    [input, groupId]
  );

  const handleVoted = useCallback((sessionId, updatedVoteState) => {
    setMessages((prev) =>
      prev.map((m) => {
        if (m.messageType !== 'DEMOCRACY_PROPOSAL') return m;
        try {
          const meta = typeof m.metadata === 'string' ? JSON.parse(m.metadata) : m.metadata;
          if (!meta || meta.sessionId !== sessionId) return m;
          return { ...m, metadata: JSON.stringify({ ...meta, votes: updatedVoteState.votes }) };
        } catch (_) { return m; }
      })
    );
  }, []);

  const formatTime = (ts) => {
    if (!ts) return '';
    return new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const getInitials = (email) => (email ? email.charAt(0).toUpperCase() : '?');
  const getAvatarColor = (email) => {
    const colors = ['#6366f1', '#ec4899', '#f59e0b', '#10b981', '#3b82f6', '#8b5cf6', '#ef4444', '#14b8a6'];
    let hash = 0;
    for (let i = 0; i < email.length; i++) hash = email.charCodeAt(i) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
  };

  const renderMessage = (msg, i) => {
    const isAgent = msg.senderEmail === AGENT_EMAIL;
    const isMe = !isAgent && msg.senderEmail === currentEmail;
    const showSender = i === 0 || messages[i - 1].senderEmail !== msg.senderEmail;

    // Democracy progress / trigger messages
    if (isAgent && (msg.messageType === 'DEMOCRACY_TRIGGER' || msg.messageType === 'DEMOCRACY_PROGRESS')) {
      return (
        <div key={msg.id || i} style={styles.agentNotice}>
          <span style={styles.agentNoticeIcon}>🤖</span>
          <div>
            <div style={styles.agentNoticeText}>{msg.content}</div>
            <div style={styles.agentNoticeTime}>{formatTime(msg.createdAt)}</div>
          </div>
        </div>
      );
    }

    // Democracy proposals with vote buttons
    if (isAgent && msg.messageType === 'DEMOCRACY_PROPOSAL') {
      const meta = typeof msg.metadata === 'string' ? msg.metadata : JSON.stringify(msg.metadata);
      let sessionId = null;
      try { sessionId = JSON.parse(meta)?.sessionId; } catch (_) {}
      return (
        <div key={msg.id || i} style={styles.proposalWrapper}>
          <div style={styles.agentLabel}>
            <span>🗳️ Democracy Agent</span>
            <span style={styles.agentTime}>{formatTime(msg.createdAt)}</span>
          </div>
          <DemocracyProposalMessage
            groupId={groupId}
            metadata={meta}
            onVoted={(updatedVoteState) => handleVoted(sessionId, updatedVoteState)}
          />
        </div>
      );
    }

    // Regular text messages
    return (
      <div key={msg.id || i} style={{ ...styles.msgRow, flexDirection: isMe ? 'row-reverse' : 'row' }}>
        {!isMe && (
          <div style={{
            ...styles.avatar,
            background: isAgent ? '#6366f1' : getAvatarColor(msg.senderEmail),
            opacity: showSender ? 1 : 0,
          }}>
            {isAgent ? '🤖' : getInitials(msg.senderEmail)}
          </div>
        )}
        <div style={{ maxWidth: '68%' }}>
          {showSender && !isMe && (
            <div style={styles.senderName}>{isAgent ? 'Democracy Agent' : msg.senderEmail}</div>
          )}
          <div style={{
            ...styles.bubble,
            ...(isMe ? styles.bubbleMe : styles.bubbleOther),
            ...(isAgent ? styles.bubbleAgent : {}),
            borderTopLeftRadius: !isMe && !showSender ? 6 : undefined,
            borderTopRightRadius: isMe && !showSender ? 6 : undefined,
          }}>
            <span style={styles.bubbleText}>{msg.content}</span>
            <span style={styles.timestamp}>{formatTime(msg.createdAt)}</span>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div style={styles.container}>
      {/* Header */}
      <div style={styles.header}>
        <div style={styles.headerLeft}>
          <span style={styles.headerTitle}>💬 Group Chat</span>
          <span style={{ ...styles.statusDot, background: connected ? '#10b981' : '#6b7280' }} />
          <span style={styles.statusText}>{connected ? 'Live' : 'Connecting...'}</span>
        </div>
        <div style={styles.headerRight}>
          {dmPending && (
            <button style={styles.dmBadge} onClick={() => {}}>
              🤖 DM pending
            </button>
          )}
          <span style={styles.msgCount}>{messages.length} messages</span>
        </div>
      </div>

      {error && <div style={styles.errorBanner}>{error}</div>}

      {/* Messages */}
      <div style={styles.messageArea}>
        {messages.length === 0 && (
          <div style={styles.empty}>No messages yet. Be the first to say something! 👋</div>
        )}
        {messages.map((msg, i) => renderMessage(msg, i))}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <form onSubmit={handleSend} style={styles.inputRow}>
        <input
          id="chat-input"
          style={styles.input}
          type="text"
          placeholder={connected ? 'Type a message… (try "we need an Airbnb")' : 'Connecting to chat…'}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          disabled={!connected}
          autoComplete="off"
          maxLength={2000}
        />
        <button
          id="chat-send-btn"
          type="submit"
          disabled={!connected || !input.trim()}
          style={{ ...styles.sendBtn, opacity: !connected || !input.trim() ? 0.4 : 1 }}
        >
          ➤
        </button>
      </form>

      {/* Democracy Agent DM Modal */}
      {dmPending && (
        <DemocracyDmModal
          groupId={groupId}
          sessionId={dmPending.sessionId}
          goalText={dmPending.goalText}
          onClose={() => setDmPending(null)}
          onSubmitted={() => setDmPending(null)}
        />
      )}
    </div>
  );
}

const styles = {
  container: {
    display: 'flex', flexDirection: 'column',
    height: 520, borderRadius: 16,
    background: 'var(--surface)',
    border: '1px solid var(--border)',
    overflow: 'hidden',
    boxShadow: '0 4px 24px rgba(0,0,0,0.12)',
  },
  header: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '14px 20px',
    borderBottom: '1px solid var(--border)',
    background: 'var(--surface-2)', flexShrink: 0,
  },
  headerLeft: { display: 'flex', alignItems: 'center', gap: 10 },
  headerRight: { display: 'flex', alignItems: 'center', gap: 12 },
  headerTitle: { fontWeight: 700, fontSize: 15, color: 'var(--text)' },
  statusDot: { width: 8, height: 8, borderRadius: '50%', flexShrink: 0 },
  statusText: { fontSize: 12, color: 'var(--text-2)' },
  msgCount: { fontSize: 12, color: 'var(--text-2)' },
  dmBadge: {
    padding: '4px 10px', borderRadius: 20,
    background: 'rgba(99,102,241,0.15)',
    border: '1px solid rgba(99,102,241,0.4)',
    color: '#6366f1', fontSize: 11, fontWeight: 700, cursor: 'pointer',
  },
  errorBanner: {
    background: 'rgba(220,38,38,0.12)', color: '#ef4444',
    fontSize: 12, padding: '6px 16px',
    borderBottom: '1px solid rgba(220,38,38,0.2)',
  },
  messageArea: {
    flex: 1, overflowY: 'auto', padding: '16px 20px',
    display: 'flex', flexDirection: 'column', gap: 4,
  },
  empty: { margin: 'auto', color: 'var(--text-2)', fontSize: 14, textAlign: 'center', padding: 32 },
  msgRow: { display: 'flex', alignItems: 'flex-end', gap: 8, marginBottom: 2 },
  avatar: {
    width: 32, height: 32, borderRadius: '50%',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontSize: 13, fontWeight: 700, color: '#fff', flexShrink: 0,
  },
  senderName: { fontSize: 11, color: 'var(--text-2)', marginBottom: 3, marginLeft: 2, fontWeight: 600 },
  bubble: {
    padding: '8px 14px', borderRadius: 18,
    fontSize: 14, lineHeight: 1.5,
    display: 'flex', alignItems: 'flex-end', gap: 8, wordBreak: 'break-word',
  },
  bubbleMe: { background: 'var(--blue)', color: '#fff', borderBottomRightRadius: 4 },
  bubbleOther: {
    background: 'var(--surface-2)', color: 'var(--text)',
    borderBottomLeftRadius: 4, border: '1px solid var(--border)',
  },
  bubbleAgent: {
    background: 'rgba(99,102,241,0.08)',
    border: '1px solid rgba(99,102,241,0.3)',
    color: 'var(--text)',
  },
  bubbleText: { flex: 1 },
  timestamp: { fontSize: 10, opacity: 0.6, flexShrink: 0, alignSelf: 'flex-end' },
  agentNotice: {
    display: 'flex', alignItems: 'flex-start', gap: 10,
    margin: '6px 0',
    padding: '10px 14px',
    borderRadius: 12,
    background: 'rgba(99,102,241,0.06)',
    border: '1px solid rgba(99,102,241,0.2)',
  },
  agentNoticeIcon: { fontSize: 18, flexShrink: 0 },
  agentNoticeText: { fontSize: 13, color: 'var(--text)', lineHeight: 1.5, whiteSpace: 'pre-wrap' },
  agentNoticeTime: { fontSize: 10, color: 'var(--text-2)', marginTop: 3 },
  proposalWrapper: { margin: '8px 0' },
  agentLabel: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    marginBottom: 6,
    fontSize: 11, color: 'var(--text-2)', fontWeight: 600,
  },
  agentTime: { fontSize: 10 },
  inputRow: {
    display: 'flex', gap: 10, padding: '12px 16px',
    borderTop: '1px solid var(--border)',
    background: 'var(--surface-2)', flexShrink: 0,
  },
  input: {
    flex: 1, padding: '10px 16px', borderRadius: 24,
    border: '1px solid var(--border)',
    background: 'var(--surface)', color: 'var(--text)',
    fontSize: 14, outline: 'none',
  },
  sendBtn: {
    width: 44, height: 44, borderRadius: '50%',
    border: 'none', background: 'var(--blue)', color: '#fff',
    fontSize: 18, cursor: 'pointer',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    transition: 'opacity 0.2s', flexShrink: 0,
  },
};
