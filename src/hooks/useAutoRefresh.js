import { useEffect, useRef } from 'react'

export const useAutoRefresh = (callback, intervalMs) => {
  const timerRef = useRef(null)
  const callbackRef = useRef(callback)

  callbackRef.current = callback

  useEffect(() => {
    timerRef.current = setInterval(() => {
      callbackRef.current()
    }, intervalMs)

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
    }
  }, [intervalMs])
}