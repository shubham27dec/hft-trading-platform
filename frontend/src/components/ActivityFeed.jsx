import { useNotifications } from '../hooks/useNotifications'

export function ActivityFeed({ accountId }) {
  const { notifications, connected } = useNotifications(accountId)

  return (
    <div className="panel">
      <div className="flex items-center justify-between shrink-0 mb-3">
        <h2 className="panel-title mb-0">Activity Feed</h2>
        <span className={`text-xs px-2 py-0.5 rounded-full ${
          connected ? 'bg-green-900/50 text-green-400' : 'bg-gray-700 text-gray-500'
        }`}>
          {connected ? '● live' : '○ off'}
        </span>
      </div>

      {!accountId && <p className="text-sm text-gray-500">Enter account ID in header</p>}
      {accountId && notifications.length === 0 && (
        <p className="text-sm text-gray-500">Waiting for activity…</p>
      )}

      <div className="space-y-1.5 overflow-auto flex-1">
        {notifications.map((n, i) => (
          <div key={i} className={`flex gap-2 items-start px-2 py-1.5 rounded text-sm ${
            n.type === 'FILL'
              ? 'bg-green-900/20 border border-green-900/40'
              : 'bg-red-900/20 border border-red-900/40'
          }`}>
            <span className={`text-xs font-bold mt-0.5 shrink-0 w-12 ${
              n.type === 'FILL' ? 'text-green-400' : 'text-red-400'
            }`}>
              {n.type}
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-gray-200 break-words leading-snug">{n.message}</p>
              <p className="text-xs text-gray-500 mt-0.5">{relativeTime(n.timestamp)}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function relativeTime(ts) {
  const diff = Date.now() - ts
  if (diff < 60_000) return 'just now'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}m ago`
  return new Date(ts).toLocaleTimeString()
}
