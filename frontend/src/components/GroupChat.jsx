import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import api from '../api';

const WS_URL = 'http://localhost:8080/ws';

export default function GroupChat({ groupId }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState('');
  const clientRef = useRef(null);
  const bottomRef = useRef(null);
  const [currentEmail] = useState(() => localStorage.getItem('fairpay_email') || '');

  // Load history on mount
  useEffect(() => {
    api.getChatHistory(groupId)
      .then(setMessages)
      .catch(() => setMessages([]));
  }, [groupId]);

  // Auto-scroll when messages change
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Connect WebSocket
  useEffect(() => {
    const token = api.getToken();
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        setError('');
        client.subscribe(`/topic/chat/${groupId}`, (frame) => {
          const msg = JSON.parse(frame.body);
          setMessages((prev) => [...prev, msg]);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setError('Connection error. Retrying...'),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [groupId]);

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

  const formatTime = (ts) => {
    if (!ts) return '';
    const d = new Date(ts);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const getInitials = (email) =>
    email ? email.charAt(0).toUpperCase() : '?';

  const getAvatarColor = (email) => {
    const colors = [
      '#6366f1', '#ec4899', '#f59e0b', '#10b981',
      '#3b82f6', '#8b5cf6', '#ef4444', '#14b8a6',
    ];
    let hash = 0;
    for (let i = 0; i < email.length; i++) hash = email.charCodeAt(i) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
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
        <span style={styles.msgCount}>{messages.length} messages</span>
      </div>

      {error && <div style={styles.errorBanner}>{error}</div>}

      {/* Messages */}
      <div style={styles.messageArea}>
        {messages.length === 0 && (
          <div style={styles.empty}>
            No messages yet. Be the first to say something! 👋
          </div>
        )}
        {messages.map((msg, i) => {
          const isMe = msg.senderEmail === currentEmail;
          const showSender = i === 0 || messages[i - 1].senderEmail !== msg.senderEmail;
          return (
            <div key={msg.id || i} style={{ ...styles.msgRow, flexDirection: isMe ? 'row-reverse' : 'row' }}>
              {/* Avatar */}
              {!isMe && (
                <div
                  style={{
                    ...styles.avatar,
                    background: getAvatarColor(msg.senderEmail),
                    opacity: showSender ? 1 : 0,
                  }}
                >
                  {getInitials(msg.senderEmail)}
                </div>
              )}
              <div style={{ maxWidth: '68%' }}>
                {showSender && !isMe && (
                  <div style={styles.senderName}>{msg.senderEmail}</div>
                )}
                <div style={{
                  ...styles.bubble,
                  ...(isMe ? styles.bubbleMe : styles.bubbleOther),
                  borderTopLeftRadius: !isMe && !showSender ? 6 : undefined,
                  borderTopRightRadius: isMe && !showSender ? 6 : undefined,
                }}>
                  <span style={styles.bubbleText}>{msg.content}</span>
                  <span style={styles.timestamp}>{formatTime(msg.createdAt)}</span>
                </div>
              </div>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <form onSubmit={handleSend} style={styles.inputRow}>
        <input
          id="chat-input"
          style={styles.input}
          type="text"
          placeholder={connected ? 'Type a message…' : 'Connecting to chat…'}
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
          style={{
            ...styles.sendBtn,
            opacity: !connected || !input.trim() ? 0.4 : 1,
          }}
        >
          ➤
        </button>
      </form>
    </div>
  );
}

const styles = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    height: 520,
    borderRadius: 16,
    background: 'var(--surface)',
    border: '1px solid var(--border)',
    overflow: 'hidden',
    boxShadow: '0 4px 24px rgba(0,0,0,0.12)',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '14px 20px',
    borderBottom: '1px solid var(--border)',
    background: 'var(--surface-2)',
    flexShrink: 0,
  },
  headerLeft: { display: 'flex', alignItems: 'center', gap: 10 },
  headerTitle: { fontWeight: 700, fontSize: 15, color: 'var(--text)' },
  statusDot: { width: 8, height: 8, borderRadius: '50%', flexShrink: 0 },
  statusText: { fontSize: 12, color: 'var(--text-2)' },
  msgCount: { fontSize: 12, color: 'var(--text-2)' },
  errorBanner: {
    background: 'rgba(220,38,38,0.12)',
    color: '#ef4444',
    fontSize: 12,
    padding: '6px 16px',
    borderBottom: '1px solid rgba(220,38,38,0.2)',
  },
  messageArea: {
    flex: 1,
    overflowY: 'auto',
    padding: '16px 20px',
    display: 'flex',
    flexDirection: 'column',
    gap: 4,
  },
  empty: {
    margin: 'auto',
    color: 'var(--text-2)',
    fontSize: 14,
    textAlign: 'center',
    padding: 32,
  },
  msgRow: {
    display: 'flex',
    alignItems: 'flex-end',
    gap: 8,
    marginBottom: 2,
  },
  avatar: {
    width: 32,
    height: 32,
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 13,
    fontWeight: 700,
    color: '#fff',
    flexShrink: 0,
  },
  senderName: {
    fontSize: 11,
    color: 'var(--text-2)',
    marginBottom: 3,
    marginLeft: 2,
    fontWeight: 600,
  },
  bubble: {
    padding: '8px 14px',
    borderRadius: 18,
    fontSize: 14,
    lineHeight: 1.5,
    display: 'flex',
    alignItems: 'flex-end',
    gap: 8,
    wordBreak: 'break-word',
  },
  bubbleMe: {
    background: 'var(--blue)',
    color: '#fff',
    borderBottomRightRadius: 4,
  },
  bubbleOther: {
    background: 'var(--surface-2)',
    color: 'var(--text)',
    borderBottomLeftRadius: 4,
    border: '1px solid var(--border)',
  },
  bubbleText: { flex: 1 },
  timestamp: {
    fontSize: 10,
    opacity: 0.6,
    flexShrink: 0,
    alignSelf: 'flex-end',
  },
  inputRow: {
    display: 'flex',
    gap: 10,
    padding: '12px 16px',
    borderTop: '1px solid var(--border)',
    background: 'var(--surface-2)',
    flexShrink: 0,
  },
  input: {
    flex: 1,
    padding: '10px 16px',
    borderRadius: 24,
    border: '1px solid var(--border)',
    background: 'var(--surface)',
    color: 'var(--text)',
    fontSize: 14,
    outline: 'none',
  },
  sendBtn: {
    width: 44,
    height: 44,
    borderRadius: '50%',
    border: 'none',
    background: 'var(--blue)',
    color: '#fff',
    fontSize: 18,
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    transition: 'opacity 0.2s',
    flexShrink: 0,
  },
};
