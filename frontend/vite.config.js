import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/admin': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/technician': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
        '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    }
  }
})