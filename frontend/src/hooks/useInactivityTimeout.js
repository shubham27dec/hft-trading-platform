import { useEffect, useRef, useState, useCallback } from 'react'

const INACTIVE_MS = 2 * 60 * 1000  // 2 minutes
const WARN_MS     = 30              // 30 second countdown

export function useInactivityTimeout(onLogout) {
  const [countdown, setCountdown] = useState(null) // null = no warning shown
  const inactiveTimer  = useRef(null)
  const countdownTimer = useRef(null)

  const clearAll = useCallback(() => {
    clearTimeout(inactiveTimer.current)
    clearInterval(countdownTimer.current)
  }, [])

  const startCountdown = useCallback(() => {
    let secs = WARN_MS
    setCountdown(secs)
    countdownTimer.current = setInterval(() => {
      secs -= 1
      if (secs <= 0) {
        clearAll()
        setCountdown(null)
        onLogout()
      } else {
        setCountdown(secs)
      }
    }, 1000)
  }, [clearAll, onLogout])

  const resetTimer = useCallback(() => {
    clearAll()
    setCountdown(null)
    inactiveTimer.current = setTimeout(startCountdown, INACTIVE_MS)
  }, [clearAll, startCountdown])

  useEffect(() => {
    const events = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart']
    const handler = () => resetTimer()
    events.forEach(e => window.addEventListener(e, handler, { passive: true }))
    resetTimer()
    return () => {
      clearAll()
      events.forEach(e => window.removeEventListener(e, handler))
    }
  }, [resetTimer, clearAll])

  return countdown
}
