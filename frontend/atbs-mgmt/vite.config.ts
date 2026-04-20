import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia']
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    allowedHosts: ['atbs.flower-star-dream.top', '103.115.43.55', 'localhost'],
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 保持/api前缀以匹配网关路由规则
        // 确保所有/api开头的请求都被正确代理
        secure: false, // 允许使用http协议
        // 添加重写规则，确保路径正确匹配
        rewrite: (path) => path // 保持原路径不变
      },
      '/assets': {
        target: 'http://103.115.43.55:9000',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/assets/, '/assets'),
        configure: (proxy, options) => {
          proxy.on('proxyRes', (res) => {
            // 把错误的 text/html 改成 image/png
            if (res.headers['content-type']?.includes('text/html')) {
              res.headers['content-type'] = 'image/png'
            }
          })
        }
      }
    }
  }
})