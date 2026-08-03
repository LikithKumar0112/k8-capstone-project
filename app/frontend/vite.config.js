import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// During `npm run dev`, proxy /api to the local backend (port 8082, since 8080
// is Jenkins). In the container, nginx handles this proxying instead.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8082',
    },
  },
})
