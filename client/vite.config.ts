import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: true,
    // Фронт и API на одном origin: refresh cookie сохраняется и отправляется без CORS
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        // Для браузера запрос same-origin. Убираем Origin, иначе Spring проверит CORS и вернет 403,
        // если фронт открыт не с разрешенного адреса (например, по IP из сети при host: true)
        configure: (proxy) => {
          proxy.on("proxyReq", (proxyReq) => proxyReq.removeHeader("origin"));
        },
      },
    }
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
