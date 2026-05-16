import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  define: {
    global: 'globalThis',
  },
  server: {
    port: 5173,
    proxy: {
      '/api/orders':    { target: 'http://localhost:8085', changeOrigin: true },
      '/api/market':    { target: 'http://localhost:8085', changeOrigin: true },
      '/api/positions': { target: 'http://localhost:8081', changeOrigin: true },
      '/api/risk':      { target: 'http://localhost:8082', changeOrigin: true },
    },
  },
})
