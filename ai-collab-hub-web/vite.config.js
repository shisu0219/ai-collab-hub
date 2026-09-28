import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/**
 * 人工智能学院双创平台 - 前端构建配置
 *
 * 开发时 /api 转发到后端 28848 端口，跟线上 Nginx 的规则保持一致。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3000,
    host: true,
    proxy: {
      // 后端接口统一走 /api 前缀，转发时把 /api 去掉
      '/api': {
        target: 'http://localhost:28848',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      // 上传文件的静态访问
      '/WebFile': {
        target: 'http://localhost:28848',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router'],
          element: ['element-plus', '@element-plus/icons-vue'],
          echarts: ['echarts']
        }
      }
    }
  }
})
