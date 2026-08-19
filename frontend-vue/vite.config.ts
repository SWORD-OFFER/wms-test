/// <reference types="vitest" />
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // Spring Boot 默认端口
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'happy-dom',
    // e2e 目录由 Playwright 运行，排除出 Vitest
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
  },
})
