import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import type { ServerResponse } from 'node:http'
import type { ProxyOptions } from 'vite'

/**
 * Backend chưa bật hoặc đang restart: trả 503 kèm JSON gọn thay vì để Vite in cả stack
 * "AggregateError [ECONNREFUSED]" mỗi lần frontend gọi API. Frontend nhận được lỗi có
 * status nên biết là "server tạm thời không sẵn sàng", không nhầm thành phiên hỏng.
 */
const onProxyError: ProxyOptions['configure'] = (proxy) => {
  proxy.on('error', (err: NodeJS.ErrnoException, req, res) => {
    const target = res as ServerResponse
    if (typeof target?.writeHead !== 'function' || target.headersSent) {
      return
    }
    console.warn(`[proxy] backend chưa sẵn sàng (${err.code ?? err.message}) — ${req.url}`)
    target.writeHead(503, { 'Content-Type': 'application/json; charset=utf-8' })
    target.end(
      JSON.stringify({ message: 'Backend chưa chạy hoặc đang khởi động lại. Thử lại sau ít giây.' }),
    )
  })
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        configure: onProxyError,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: onProxyError,
      },
    },
  },
})
