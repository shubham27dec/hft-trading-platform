import { useQuotes } from '../hooks/useQuotes'

export function OrderBook({ symbol }) {
  const quotes = useQuotes()
  const q = quotes.find(x => x.symbol === symbol)

  return (
    <div className="panel">
      <h2 className="panel-title">
        Order Book
        {symbol && <span className="ml-2 text-blue-400 normal-case">{symbol}</span>}
      </h2>

      {!symbol && <p className="text-sm text-gray-500">Click a symbol in Market Data</p>}
      {symbol && !q && <p className="text-sm text-gray-500">No quote cached yet</p>}

      {q && (
        <div className="space-y-2 mt-1">
          <PriceRow label="Ask" price={q.askPrice} size={q.askSize} color="text-red-400" />
          <div className="text-xs text-gray-500 text-center py-0.5">
            spread ${(q.askPrice - q.bidPrice).toFixed(3)}
          </div>
          <PriceRow label="Bid" price={q.bidPrice} size={q.bidSize} color="text-green-400" />

          <div className="pt-3 border-t border-gray-700 space-y-1.5 text-sm">
            <Row label="Last"   value={`$${q.lastPrice.toFixed(2)}`} />
            <Row label="Volume" value={q.volume ? q.volume.toLocaleString() : '—'} />
            <Row label="Mid"    value={`$${((q.bidPrice + q.askPrice) / 2).toFixed(3)}`} />
          </div>
        </div>
      )}
    </div>
  )
}

function PriceRow({ label, price, size, color }) {
  return (
    <div className="flex items-center justify-between px-3 py-2 rounded bg-gray-800">
      <span className="text-xs text-gray-400 w-8">{label}</span>
      <div className="text-right">
        <div className={`font-mono font-bold text-base ${color}`}>${price.toFixed(2)}</div>
        {size > 0 && <div className="text-xs text-gray-500">{size.toLocaleString()} shs</div>}
      </div>
    </div>
  )
}

function Row({ label, value }) {
  return (
    <div className="flex justify-between text-xs">
      <span className="text-gray-400">{label}</span>
      <span className="text-gray-200 font-mono">{value}</span>
    </div>
  )
}
