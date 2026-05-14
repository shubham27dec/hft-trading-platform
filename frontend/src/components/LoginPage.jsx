import { useState } from 'react'
import keycloak from '../keycloak'

const KEYCLOAK_TOKEN_URL = 'http://localhost:8180/realms/hft/protocol/openid-connect/token'
const REGISTER_URL = '/api/auth/register'

function parseJwt(token) {
  try {
    return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
  } catch {
    return null
  }
}

async function ropcLogin(username, password) {
  const res = await fetch(KEYCLOAK_TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: 'hft-frontend',
      username,
      password,
    }),
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error_description ?? 'Login failed')
  keycloak.token = data.access_token
  keycloak.refreshToken = data.refresh_token
  keycloak.tokenParsed = parseJwt(data.access_token)
  keycloak.authenticated = true
  keycloak.subject = keycloak.tokenParsed?.sub
}

export default function LoginPage({ onAuthenticated }) {
  const [tab, setTab] = useState('signin')

  const [signInForm, setSignInForm] = useState({ username: '', password: '' })
  const [signUpForm, setSignUpForm] = useState({ username: '', email: '', password: '', confirmPassword: '' })

  const [error, setError]   = useState('')
  const [loading, setLoading] = useState(false)

  const handleSignIn = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await ropcLogin(signInForm.username, signInForm.password)
      onAuthenticated()
    } catch (err) {
      setError(err.message || 'Invalid credentials')
    } finally {
      setLoading(false)
    }
  }

  const handleSignUp = async (e) => {
    e.preventDefault()
    setError('')
    if (signUpForm.password !== signUpForm.confirmPassword) {
      setError('Passwords do not match')
      return
    }
    if (signUpForm.password.length < 8) {
      setError('Password must be at least 8 characters')
      return
    }
    setLoading(true)
    try {
      const res = await fetch(REGISTER_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: signUpForm.username,
          email: signUpForm.email,
          password: signUpForm.password,
        }),
      })
      const data = await res.json()
      if (!res.ok) {
        setError(data.error ?? 'Registration failed')
        return
      }
      await ropcLogin(signUpForm.username, signUpForm.password)
      onAuthenticated()
    } catch (err) {
      setError(err.message || 'Registration failed — please try again')
    } finally {
      setLoading(false)
    }
  }

  const handleGoogle = () => {
    keycloak.login({ idpHint: 'google', redirectUri: window.location.origin })
  }

  const switchTab = (t) => { setTab(t); setError('') }

  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4">
      <div className="w-full max-w-md">

        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2 mb-3">
            <span className="text-3xl">⚡</span>
            <span className="text-2xl font-bold text-white tracking-tight">HFT Platform</span>
          </div>
          <p className="text-gray-400 text-sm">Algorithmic trading infrastructure</p>
        </div>

        {/* Card */}
        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-8 shadow-2xl">

          {/* Tabs */}
          <div className="flex mb-6 bg-gray-800 rounded-lg p-1">
            <button onClick={() => switchTab('signin')}
              className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${
                tab === 'signin' ? 'bg-blue-600 text-white shadow' : 'text-gray-400 hover:text-gray-200'
              }`}>
              Sign In
            </button>
            <button onClick={() => switchTab('signup')}
              className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${
                tab === 'signup' ? 'bg-blue-600 text-white shadow' : 'text-gray-400 hover:text-gray-200'
              }`}>
              Sign Up
            </button>
          </div>

          {error && (
            <div className="mb-4 bg-red-950 border border-red-800 text-red-400 text-xs rounded-lg px-3 py-2">
              {error}
            </div>
          )}

          {tab === 'signin' ? (
            <form onSubmit={handleSignIn} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-400 mb-1.5">Username</label>
                <input type="text" value={signInForm.username} autoFocus required
                  onChange={e => setSignInForm(f => ({ ...f, username: e.target.value }))}
                  placeholder="Enter your username"
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2.5
                             text-gray-100 text-sm placeholder-gray-600
                             focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors" />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-400 mb-1.5">Password</label>
                <input type="password" value={signInForm.password} required
                  onChange={e => setSignInForm(f => ({ ...f, password: e.target.value }))}
                  placeholder="••••••••"
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2.5
                             text-gray-100 text-sm placeholder-gray-600
                             focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors" />
              </div>
              <button type="submit" disabled={loading}
                className="w-full bg-blue-600 hover:bg-blue-500 disabled:opacity-50
                           text-white font-medium py-2.5 rounded-lg text-sm transition-colors">
                {loading ? 'Signing in…' : 'Sign In'}
              </button>

              <div className="flex items-center gap-3">
                <div className="flex-1 h-px bg-gray-700" />
                <span className="text-xs text-gray-500">or</span>
                <div className="flex-1 h-px bg-gray-700" />
              </div>

              <button type="button" onClick={handleGoogle}
                className="w-full flex items-center justify-center gap-2.5
                           bg-gray-800 hover:bg-gray-700 border border-gray-700 hover:border-gray-600
                           text-gray-200 font-medium py-2.5 rounded-lg text-sm transition-colors">
                <svg viewBox="0 0 24 24" className="w-4 h-4">
                  <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                  <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                  <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#FBBC05"/>
                  <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
                </svg>
                Continue with Google
              </button>

              <p className="text-center text-xs text-gray-500 pt-1">
                Don't have an account?{' '}
                <button type="button" onClick={() => switchTab('signup')}
                  className="text-blue-400 hover:text-blue-300 transition-colors">
                  Sign up
                </button>
              </p>
            </form>
          ) : (
            <form onSubmit={handleSignUp} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-400 mb-1.5">Username</label>
                <input type="text" value={signUpForm.username} autoFocus required
                  onChange={e => setSignUpForm(f => ({ ...f, username: e.target.value }))}
                  placeholder="Choose a username"
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2.5
                             text-gray-100 text-sm placeholder-gray-600
                             focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors" />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-400 mb-1.5">Email</label>
                <input type="email" value={signUpForm.email} required
                  onChange={e => setSignUpForm(f => ({ ...f, email: e.target.value }))}
                  placeholder="you@example.com"
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2.5
                             text-gray-100 text-sm placeholder-gray-600
                             focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors" />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-400 mb-1.5">Password</label>
                <input type="password" value={signUpForm.password} required
                  onChange={e => setSignUpForm(f => ({ ...f, password: e.target.value }))}
                  placeholder="Min 8 characters"
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2.5
                             text-gray-100 text-sm placeholder-gray-600
                             focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors" />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-400 mb-1.5">Confirm Password</label>
                <input type="password" value={signUpForm.confirmPassword} required
                  onChange={e => setSignUpForm(f => ({ ...f, confirmPassword: e.target.value }))}
                  placeholder="Repeat your password"
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2.5
                             text-gray-100 text-sm placeholder-gray-600
                             focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors" />
              </div>
              <button type="submit" disabled={loading}
                className="w-full bg-blue-600 hover:bg-blue-500 disabled:opacity-50
                           text-white font-medium py-2.5 rounded-lg text-sm transition-colors">
                {loading ? 'Creating account…' : 'Create Account'}
              </button>

              <p className="text-center text-xs text-gray-500 pt-1">
                Already have an account?{' '}
                <button type="button" onClick={() => switchTab('signin')}
                  className="text-blue-400 hover:text-blue-300 transition-colors">
                  Sign in
                </button>
              </p>
            </form>
          )}
        </div>

        <p className="text-center text-xs text-gray-600 mt-6">
          High-frequency trading platform · Paper trading only
        </p>
      </div>
    </div>
  )
}
