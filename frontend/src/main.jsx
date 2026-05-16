import React from 'react'
import ReactDOM from 'react-dom/client'
import './index.css'
import App from './App'
import keycloak from './keycloak'

function parseJwt(token) {
  try {
    return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
  } catch {
    return null
  }
}

const token = localStorage.getItem('hft_token')
const refreshToken = localStorage.getItem('hft_refresh_token')
let preAuthenticated = false

if (token) {
  const parsed = parseJwt(token)
  if (parsed && parsed.exp * 1000 > Date.now()) {
    keycloak.token = token
    keycloak.refreshToken = refreshToken
    keycloak.tokenParsed = parsed
    keycloak.authenticated = true
    keycloak.subject = parsed.sub
    preAuthenticated = true
  } else {
    localStorage.removeItem('hft_token')
    localStorage.removeItem('hft_refresh_token')
  }
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App authenticated={preAuthenticated} />
  </React.StrictMode>
)
