import React, { useState } from 'react';

export default function Modal({ title, onClose, children }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-content"
        onClick={e => e.stopPropagation()}
        style={{
          borderRadius: 10,
          boxShadow: '0 8px 20px rgba(2,6,23,0.06)',
          transition: 'transform 160ms ease, box-shadow 160ms ease, opacity 160ms ease'
        }}
        role="dialog"
        aria-modal="true"
        aria-label={title}
      >
        <div className="modal-title">{title}</div>
        {children}
      </div>
    </div>
  );
}