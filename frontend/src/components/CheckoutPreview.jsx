import React, { useState, useEffect } from 'react';

const CheckoutPreview = ({ walletId }) => {
    const [amount, setAmount] = useState('');
    const [preview, setPreview] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        const delayDebounceFn = setTimeout(async () => {
            if (!amount || isNaN(amount) || amount <= 0) {
                setPreview(null);
                return;
            }

            setLoading(true);
            setError(null);
            try {
                const token = localStorage.getItem('fairpay_token');

                const url = `http://localhost:8080/api/checkout/preview?walletId=${encodeURIComponent(walletId)}&amount=${encodeURIComponent(amount)}`;
                const response = await fetch(url, {
                    headers: {
                        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
                        'Accept': 'application/json'
                    }
                });

                if (!response.ok) {
                    // try to read error message from body if present
                    let msg = 'Eroare la calcularea split-ului';
                    try { const errBody = await response.json(); if (errBody && errBody.message) msg = errBody.message; } catch (_) {}
                    throw new Error(msg);
                }

                const data = await response.json();
                setPreview(data);
            } catch (err) {
                setError(err.message);
                setPreview(null);
            } finally {
                setLoading(false);
            }
        }, 500);

        return () => clearTimeout(delayDebounceFn);
    }, [amount, walletId]);

    return (
        <div className="card" style={{ marginTop: '16px', maxWidth: '440px' }}>
            <h2 style={{ fontSize: '18px', fontWeight: 600, marginBottom: '16px' }}>Live Payment Preview</h2>
            
            <div className="form-group">
                <label className="form-label">Suma Totală (EUR)</label>
                <input
                    type="number"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="Introdu suma..."
                    className="form-input"
                />
            </div>

            {loading && <p style={{ color: 'var(--blue)', fontSize: '13px' }}>Se calculează cea mai bună ofertă...</p>}
            {error && <p className="error">{error}</p>}

            {preview && !loading && (
                <div style={{ marginTop: '16px', borderTop: '1px solid var(--border)', paddingTop: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-2)', fontSize: '13px' }}>
                        <span>Acoperit de Vouchere:</span>
                        <span style={{ fontWeight: 500, color: 'var(--green)' }}>-{(preview.voucherAmount || 0).toFixed(2)} EUR</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-2)', fontSize: '13px' }}>
                        <span>Acoperit de Mile:</span>
                        <span style={{ fontWeight: 500, color: 'var(--blue)' }}>-{(preview.milesAmount || 0).toFixed(2)} EUR</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '15px', fontWeight: 700, marginTop: '8px', paddingTop: '8px', borderTop: '1px solid var(--border)', color: 'var(--text)' }}>
                        <span>Total Cash de plată:</span>
                        <span>{(preview.cashAmount || 0).toFixed(2)} EUR</span>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CheckoutPreview;