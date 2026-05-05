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
                const token = localStorage.getItem('token') || localStorage.getItem('authToken');

                const url = `/api/checkout/preview?walletId=${encodeURIComponent(walletId)}&amount=${encodeURIComponent(amount)}`;
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
        <div className="p-6 mt-4 border rounded-xl bg-white shadow-sm max-w-md">
            <h2 className="text-xl font-semibold mb-4">Live Payment Preview</h2>
            
            <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">Suma Totală (EUR)</label>
                <input
                    type="number"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="Introdu suma..."
                    className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                />
            </div>

            {loading && <p className="text-blue-500 text-sm">Se calculează cea mai bună ofertă...</p>}
            {error && <p className="text-red-500 text-sm">{error}</p>}

            {preview && !loading && (
                <div className="mt-4 space-y-3 border-t pt-4">
                    <div className="flex justify-between text-gray-600">
                        <span>Acoperit de Vouchere:</span>
                        <span className="font-medium text-green-600">-{(preview.voucherAmount || 0).toFixed(2)} EUR</span>
                    </div>
                    <div className="flex justify-between text-gray-600">
                        <span>Acoperit de Mile:</span>
                        <span className="font-medium text-blue-600">-{(preview.milesAmount || 0).toFixed(2)} EUR</span>
                    </div>
                    <div className="flex justify-between text-lg font-bold mt-2 pt-2 border-t">
                        <span>Total Cash de plată:</span>
                        <span className="text-gray-900">{(preview.cashAmount || 0).toFixed(2)} EUR</span>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CheckoutPreview;