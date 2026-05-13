import { useState, useEffect } from 'react'
import { OrderEntry }    from './components/OrderEntry'
import { Positions }    from './components/Positions'
import { MarketData }   from './components/MarketData'
import { OrderBook }    from './components/OrderBook'
import { ActivityFeed } from './components/ActivityFeed'
import { fetchRisk, simulatePartition, restorePartition } from './api'

export default function App() {
  const [accountId, setAccountId] = useState('')
  const [apiKey, setApiKey]       = useState('')
  const [selectedSymbol, setSelectedSymbol] = useState(null)
  const [risk, setRisk] = useState(null)

  useEffect(() => {
    if (!accountId) return
    const load = async () => {
      try { setRisk(await fetchRisk(accountId)) } catch (_) {}
    }
    load()
    const id = setInterval(load, 5000)
    return () => clearInterval(id)
  }, [accountId])

  const toggleHalt = async () => {
    try {
      risk?.haltActive ? await restorePartition() : await simulatePartition()
    } catch (_) {}
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex flex-col">

      {/* ── Header ──────────────────────────────────────────────────── */}
      <header className="border-b border-gray-800 px-4 py-2 flex items-center gap-4 flex-wrap shrink-0">
        <span className="text-blue-400 font-bold text-base shrink-0">⚡ HFT Platform</span>

        <div className="flex items-center gap-2">
          <label className="text-xs text-gray-400 shrink-0">Account</label>
          <input className="input w-44" value={accountId}
            onChange={e => setAccountId(e.target.value)} placeholder="account-id" />
        </div>

        <div className="flex items-center gap-2">
          <label className="text-xs text-gray-400 shrink-0">API Key</label>
          <input className="input w-52" type="password" value={apiKey}
            onChange={e => setApiKey(e.target.value)} placeholder="x-api-key" />
        </div>

        <div className="ml-auto flex items-center gap-4 flex-wrap">
          {risk && (
            <div className="flex gap-4 text-xs text-gray-400">
              <span>Fills <span className="text-green-400 font-mono">{risk.fillCount}</span></span>
              <span>Rejects <span className="text-red-400 font-mono">{risk.rejectCount}</span></span>
              <span>Exposure <span className="text-gray-200 font-mono">
                ${(risk.grossExposure ?? 0).toLocaleString(undefined, { maximumFractionDigits: 0 })}
              </span></span>
            </div>
          )}

          {risk != null && (
            <span className={`text-xs font-bold px-2 py-0.5 rounded ${
              risk.haltActive
                ? 'bg-red-900 text-red-300 animate-pulse'
                : 'bg-gray-800 text-gray-500'
            }`}>
              HALT {risk.haltActive ? 'ON' : 'OFF'}
            </span>
          )}

          {accountId && (
            <button onClick={toggleHalt}
              className="text-xs px-2.5 py-1 rounded border border-gray-600 hover:border-gray-400
                         text-gray-300 hover:text-gray-100 transition-colors">
              {risk?.haltActive ? 'Restore' : 'Simulate Partition'}
            </button>
          )}
        </div>
      </header>

      {/* ── Main Grid ───────────────────────────────────────────────── */}
      <main className="flex-1 p-3 grid gap-3 overflow-hidden"
        style={{ gridTemplateColumns: '3fr 6fr 3fr', gridTemplateRows: '1fr 1fr' }}>

        <OrderEntry apiKey={apiKey} />
        <MarketData onSelectSymbol={setSelectedSymbol} selectedSymbol={selectedSymbol} />
        <OrderBook symbol={selectedSymbol} />

        <div style={{ gridColumn: '1 / 3' }}>
          <Positions accountId={accountId} />
        </div>
        <ActivityFeed accountId={accountId} />
      </main>
    </div>
  )
}
