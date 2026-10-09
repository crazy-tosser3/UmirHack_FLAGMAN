import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: true,
    // proxy: {
    //   "/api": "http://localhost:3001",
    //   "/socket.io": {target: "http://localhost:3001", ws: true}
    // }
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
    }
  }
})
