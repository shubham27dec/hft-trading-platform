const BASE = {
  orders:    '/api/orders',
  positions: '/api/positions',
  risk:      '/api/risk',
  market:    '/api/market',
}

export async function submitOrder(order, apiKey) {
  const res = await fetch(BASE.orders, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-API-Key': apiKey },
    body: JSON.stringify(order),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.error || `HTTP ${res.status}`)
  }
  return res.json()
}

export async function fetchPositions(accountId) {
  const res = await fetch(`${BASE.positions}/${accountId}`)
  if (!res.ok) throw new Error(`Positions HTTP ${res.status}`)
  return res.json()
}

export async function fetchRisk(accountId) {
  const res = await fetch(`${BASE.risk}/${accountId}`)
  if (!res.ok) throw new Error(`Risk HTTP ${res.status}`)
  return res.json()
}

export async function fetchQuotes() {
  const res = await fetch(`${BASE.market}/quotes`)
  if (!res.ok) throw new Error(`Quotes HTTP ${res.status}`)
  return res.json()
}

export async function simulatePartition() {
  await fetch(`${BASE.risk}/simulate-partition`, { method: 'POST' })
}

export async function restorePartition() {
  await fetch(`${BASE.risk}/restore`, { method: 'POST' })
}
