import { useState, useEffect } from 'react'
import { fetchQuotes } from '../api'

export function useQuotes() {
  const [quotes, setQuotes] = useState([])

  useEffect(() => {
    const load = async () => {
      try { setQuotes(await fetchQuotes()) } catch (_) {}
    }
    load()
    const id = setInterval(load, 1500)
    return () => clearInterval(id)
  }, [])

  return quotes
}
