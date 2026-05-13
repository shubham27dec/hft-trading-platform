import { useQuotes } from '../hooks/useQuotes'

export function MarketData({ onSelectSymbol, selectedSymbol }) {
  const quotes = useQuotes()

  return (
    <div className="panel">
      <h2 className="panel-title">Market Data</h2>
      {quotes.length === 0 && (
        <p className="text-sm text-gray-500">Waiting for execution engine feed…</p>
      )}
      {quotes.length > 0 && (
        <div className="overflow-auto flex-1">
          <table className="w-full text-sm">
            <thead className="sticky top-0 bg-gray-900">
              <tr className="text-left text-xs text-gray-400 border-b border-gray-700">
                <Th>Symbol</Th>
                <Th right>Bid</Th>
                <Th right>Ask</Th>
                <Th right>Spread</Th>
                <Th right>Last</Th>
                <Th right>Volume</Th>
              </tr>
            </thead>
            <tbody>
              {quotes.map(q => (
                <tr key={q.symbol} onClick={() => onSelectSymbol(q.symbol)}
                  className={`border-b border-gray-800 cursor-pointer transition-colors ${
                    selectedSymbol === q.symbol ? 'bg-blue-900/25' : 'hover:bg-gray-800/50'
                  }`}>
                  <td className="py-1.5 pr-4 font-semibold text-blue-300">{q.symbol}</td>
                  <td className="py-1.5 pr-4 text-right text-green-400 font-mono">${q.bidPrice.toFixed(2)}</td>
                  <td className="py-1.5 pr-4 text-right text-red-400 font-mono">${q.askPrice.toFixed(2)}</td>
                  <td className="py-1.5 pr-4 text-right text-gray-400 font-mono">
                    {(q.askPrice - q.bidPrice).toFixed(3)}
                  </td>
                  <td className="py-1.5 pr-4 text-right text-gray-200 font-mono">${q.lastPrice.toFixed(2)}</td>
                  <td className="py-1.5 text-right text-gray-400 font-mono">
                    {q.volume ? q.volume.toLocaleString() : '—'}
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
