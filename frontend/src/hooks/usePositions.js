import { useState, useEffect, useCallback } from 'react'
import { fetchPositions } from '../api'

export function usePositions(accountId) {
  const [positions, setPositions] = useState([])
  const [error, setError] = useState(null)

  const refresh = useCallback(async () => {
    if (!accountId) return
    try {
      setPositions(await fetchPositions(accountId))
      setError(null)
    } catch (e) {
      setError(e.message)
    }
  }, [accountId])

  useEffect(() => {
    refresh()
    const id = setInterval(refresh, 2000)
    return () => clearInterval(id)
  }, [refresh])

  return { positions, error }
}
