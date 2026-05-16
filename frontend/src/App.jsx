import { useState, useEffect, useCallback } from 'react'
import { OrderEntry }    from './components/OrderEntry'
import { Positions }    from './components/Positions'
import { MarketData }   from './components/MarketData'
import { OrderBook }    from './components/OrderBook'
import { ActivityFeed } from './components/ActivityFeed'
import LoginPage        from './components/LoginPage'
import { fetchRisk, simulatePartition, restorePartition } from './api'
import keycloak from './keycloak'
import { useInactivityTimeout } from './hooks/useInactivityTimeout'

export default function App({ authenticated: initialAuth }) {
  const [authenticated, setAuthenticated] = useState(initialAuth)
  const [selectedSymbol, setSelectedSymbol] = useState(null)
  const [risk, setRisk] = useState(null)

  const accountId = keycloak.tokenParsed?.sub ?? ''
  const username  = keycloak.tokenParsed?.preferred_username ?? ''

  const handleLogout = useCallback(() => {
    localStorage.removeItem('hft_token')
    localStorage.removeItem('hft_refresh_token')
    keycloak.authenticated = false
    keycloak.token = null
    setAuthenticated(false)
  }, [])

  const countdown = useInactivityTimeout(authenticated ? handleLogout : () => {})

  useEffect(() => {
    if (!accountId) return
    const load = async () => {
      try { setRisk(await fetchRisk(accountId)) } catch (_) {}
    }
    load()
    const id = setInterval(load, 5000)
    return () => clearInterval(id)
  }, [accountId])

  if (!authenticated) {
    return <LoginPage onAuthenticated={() => setAuthenticated(true)} />
  }

  const toggleHalt = async () => {
    try {
      risk?.haltActive ? await restorePartition() : await simulatePartition()
    } catch (_) {}
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex flex-col">

      {/* ── Inactivity Warning ──────────────────────────────────────── */}
      {countdown !== null && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-gray-900 border border-yellow-500 rounded-xl p-8 text-center shadow-2xl max-w-sm w-full mx-4">
            <p className="text-yellow-400 text-lg font-semibold mb-2">Session expiring</p>
            <p className="text-gray-300 text-sm">You'll be logged out in</p>
            <p className="text-white font-bold text-5xl my-3">{countdown}</p>
            <p className="text-gray-300 text-sm">seconds due to inactivity</p>
            <p className="text-gray-500 text-xs mt-4">Move your mouse or press any key to stay logged in</p>
          </div>
        </div>
      )}

      {/* ── Header ──────────────────────────────────────────────────── */}
      <header className="border-b border-gray-800 px-4 py-2 flex items-center gap-4 flex-wrap shrink-0">
        <span className="text-blue-400 font-bold text-base shrink-0">⚡ HFT Platform</span>

        <span className="text-xs text-gray-400">
          Logged in as <span className="text-gray-200 font-medium">{username}</span>
        </span>

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
                : 'hidden'
            }`}>
              TRADING HALTED
            </span>
          )}

          <button onClick={() => {
            localStorage.removeItem('hft_token')
            localStorage.removeItem('hft_refresh_token')
            keycloak.authenticated = false
            keycloak.token = null
            window.location.reload()
          }}
            className="text-xs px-2.5 py-1 rounded border border-gray-700 hover:border-red-500
                       text-gray-500 hover:text-red-400 transition-colors">
            Logout
          </button>
        </div>
      </header>

      {/* ── Main Grid ───────────────────────────────────────────────── */}
      <main className="flex-1 p-3 grid gap-3 overflow-hidden"
        style={{ gridTemplateColumns: '3fr 6fr 3fr', gridTemplateRows: '1fr 1fr' }}>

        <OrderEntry />
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
