import { useState } from 'react'
import { submitOrder } from '../api'
import { useQuotes } from '../hooks/useQuotes'

export function OrderEntry() {
  const quotes = useQuotes()
  const [form, setForm] = useState({
    symbol: '', side: 'BUY', type: 'MARKET', quantity: '', limitPrice: '',
  })
  const [status, setStatus] = useState(null)
  const [loading, setLoading] = useState(false)

  const set = (field, value) => setForm(prev => ({ ...prev, [field]: value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setStatus(null)
    try {
      const res = await submitOrder({
        clientOrderId: crypto.randomUUID(),
        symbol: form.symbol.toUpperCase(),
        side: form.side,
        type: form.type,
        quantity: parseInt(form.quantity, 10),
        limitPrice: form.type === 'LIMIT' ? parseFloat(form.limitPrice) : 0,
      })
      setStatus({ ok: true, msg: `${res.status} — ${res.orderId.slice(0, 8)}…` })
      setForm(prev => ({ ...prev, quantity: '', limitPrice: '' }))
    } catch (err) {
      setStatus({ ok: false, msg: err.message })
    } finally {
      setLoading(false)
    }
  }

  const selectedQuote = quotes.find(q => q.symbol === form.symbol)

  return (
    <div className="panel">
      <h2 className="panel-title">Order Entry</h2>
      <form onSubmit={handleSubmit} className="space-y-3 flex-1">
        <Field label="Symbol">
          {quotes.length > 0 ? (
            <select className="input" value={form.symbol} required
              onChange={e => set('symbol', e.target.value)}>
              <option value="">Select symbol…</option>
              {quotes.map(q => (
                <option key={q.symbol} value={q.symbol}>
                  {q.symbol} — ${q.lastPrice.toFixed(2)}
                </option>
              ))}
            </select>
          ) : (
            <input className="input" value={form.symbol}
              onChange={e => set('symbol', e.target.value.toUpperCase())}
              placeholder="AAPL" required maxLength={6} />
          )}
          {selectedQuote && (
            <p className="text-xs text-gray-500 mt-1">
              Bid <span className="text-green-400">${selectedQuote.bidPrice.toFixed(2)}</span>
              {' · '}
              Ask <span className="text-red-400">${selectedQuote.askPrice.toFixed(2)}</span>
            </p>
          )}
        </Field>

        <Field label="Side">
          <div className="flex gap-2">
            {['BUY', 'SELL'].map(s => (
              <button key={s} type="button" onClick={() => set('side', s)}
                className={`flex-1 py-1.5 rounded text-sm font-semibold transition-colors ${
                  form.side === s
                    ? s === 'BUY' ? 'bg-green-600 text-white' : 'bg-red-600 text-white'
                    : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                }`}>
                {s}
              </button>
            ))}
          </div>
        </Field>

        <Field label="Type">
          <div className="flex gap-2">
            {['MARKET', 'LIMIT'].map(t => (
              <button key={t} type="button" onClick={() => set('type', t)}
                className={`flex-1 py-1.5 rounded text-sm transition-colors ${
                  form.type === t ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                }`}>
                {t}
              </button>
            ))}
          </div>
        </Field>

        <Field label="Quantity">
          <input className="input" type="number" min="1" value={form.quantity}
            onChange={e => set('quantity', e.target.value)} placeholder="100" required />
        </Field>

        {form.type === 'LIMIT' && (
          <Field label="Limit Price">
            <input className="input" type="number" step="0.01" min="0.01"
              value={form.limitPrice} onChange={e => set('limitPrice', e.target.value)}
              placeholder="150.00" required />
          </Field>
        )}

        <button type="submit" disabled={loading}
          className={`w-full py-2 rounded font-semibold text-white transition-colors
            disabled:opacity-40 disabled:cursor-not-allowed ${
            form.side === 'BUY' ? 'bg-green-600 hover:bg-green-500' : 'bg-red-600 hover:bg-red-500'
          }`}>
          {loading ? 'Submitting…' : `${form.side} ${form.symbol || '—'}`}
        </button>

        {status && (
          <div className={`text-xs px-2 py-1.5 rounded ${
            status.ok ? 'bg-green-900/50 text-green-400' : 'bg-red-900/50 text-red-400'
          }`}>
            {status.msg}
          </div>
        )}
      </form>
    </div>
  )
}

function Field({ label, children }) {
  return (
    <div>
      <label className="block text-xs text-gray-400 mb-1">{label}</label>
      {children}
    </div>
  )
}
