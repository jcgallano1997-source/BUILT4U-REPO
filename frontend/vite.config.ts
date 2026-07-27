import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

// Content-Security-Policy for the production build ONLY (apply: 'build'), so the
// dev server (inline scripts + ws:// HMR) is left untouched. When the API is on
// a different origin (hosted), VITE_API_BASE_URL's origin is added to
// connect-src so XHRs aren't blocked; unset (local, same-origin proxy) → 'self'.
function apiOrigin(): string {
  const raw = process.env.VITE_API_BASE_URL
  if (!raw) return ''
  try { return new URL(raw).origin } catch { return '' }
}
const PROD_CSP = [
  "default-src 'self'",
  "script-src 'self'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: blob:",
  "font-src 'self' data:",
  `connect-src 'self'${apiOrigin() ? ' ' + apiOrigin() : ''}`,
  "worker-src 'self' blob:",
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
].join('; ')

const cspPlugin: Plugin = {
  name: 'inject-prod-csp',
  apply: 'build',
  transformIndexHtml(html) {
    return html.replace(
      '</title>',
      `</title>\n    <meta http-equiv="Content-Security-Policy" content="${PROD_CSP}" />` +
        `\n    <meta name="referrer" content="strict-origin-when-cross-origin" />`,
    )
  },
}

export default defineConfig({
  plugins: [react(), tailwindcss(), cspPlugin],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // Single-business POS backend runs on 8083 locally.
      '/api': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
    },
  },
})
