import { useState, useEffect } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const WS_URL = 'http://localhost:8083/ws'
const MAX_ITEMS = 50

export function useNotifications(accountId) {
  const [notifications, setNotifications] = useState([])
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    if (!accountId) return

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)
        client.subscribe(`/topic/account/${accountId}`, (msg) => {
          const notification = JSON.parse(msg.body)
          setNotifications(prev => [notification, ...prev].slice(0, MAX_ITEMS))
        })
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
    })

    client.activate()
    return () => client.deactivate()
  }, [accountId])

  return { notifications, connected }
}
