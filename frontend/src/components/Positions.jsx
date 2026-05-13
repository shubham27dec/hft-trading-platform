import { usePositions } from '../hooks/usePositions'

export function Positions({ accountId }) {
  const { positions, error } = usePositions(accountId)

  return (
    <div className="panel">
      <h2 className="panel-title">Positions</h2>
      {!accountId && <p className="text-sm text-gray-500">Enter account ID in header</p>}
      {error && <p className="text-xs text-red-400">{error}</p>}
      {accountId && !error && positions.length === 0 && (
        <p className="text-sm text-gray-500">No open positions</p>
      )}
      {positions.length > 0 && (
        <div className="overflow-auto flex-1">
          <table className="w-full text-sm">
            <thead className="sticky top-0 bg-gray-900">
              <tr className="text-left text-xs text-gray-400 border-b border-gray-700">
                <Th>Symbol</Th>
                <Th right>Net Qty</Th>
                <Th right>Avg Cost</Th>
                <Th right>Unrealized P&amp;L</Th>
                <Th right>Realized P&amp;L</Th>
              </tr>
            </thead>
            <tbody>
              {positions.map(p => (
                <tr key={p.symbol} className="border-b border-gray-800 hover:bg-gray-800/40">
                  <td className="py-2 pr-4 font-semibold text-blue-300">{p.symbol}</td>
                  <td className={`py-2 pr-4 text-right font-mono ${p.netQty > 0 ? 'text-green-400' : 'text-red-400'}`}>
                    {p.netQty > 0 ? '+' : ''}{p.netQty.toLocaleString()}
                  </td>
                  <td className="py-2 pr-4 text-right text-gray-300 font-mono">
                    ${p.avgCostBasis.toFixed(2)}
                  </td>
                  <td className={`py-2 pr-4 text-right font-mono font-medium ${p.unrealizedPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                    {p.unrealizedPnL >= 0 ? '+' : ''}${p.unrealizedPnL.toFixed(2)}
                  </td>
                  <td className={`py-2 text-right font-mono font-medium ${p.realizedPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                    {p.realizedPnL >= 0 ? '+' : ''}${p.realizedPnL.toFixed(2)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function Th({ children, right }) {
  return <th className={`pb-2 pr-4 font-medium ${right ? 'text-right' : ''}`}>{children}</th>
}
