import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: true,
    allowedHosts: ['localdev.com', '.localdev.com'],
    proxy: {
      '/api': {
        target: 'http://localdev.com:8080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
        },
      },
    },
  },
  resolve: {
    alias: {
      '@app': path.resolve(import.meta.dirname, './src/app'),
      '@pages': path.resolve(import.meta.dirname, './src/pages'),
      '@widgets': path.resolve(import.meta.dirname, './src/widgets'),
      '@features': path.resolve(import.meta.dirname, './src/features'),
      '@shared': path.resolve(import.meta.dirname, './src/shared'),
      '@lib': path.resolve(import.meta.dirname, './src/lib'),
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
})