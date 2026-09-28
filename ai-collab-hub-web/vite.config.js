import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/**
 * 人工智能学院双创平台 - 前端构建配置
 *
 * 【两种运行方式】
 *   1. 本地开发（npm run dev）
 *      /api 转发到 localhost:28848，靠 Vite 的 proxy（同源，不需要后端开跨域）
 *   2. 部署到 GitHub Pages（npm run build）
 *      静态托管没有代理服务，所以接口地址要写成**后端的绝对地址**，
 *      由 .env.production 里的 VITE_API_BASE 指定。
 *
 * base 也要改：GitHub Pages 的地址是 https://用户名.github.io/仓库名/，
 * 不是根路径，不改的话所有 js/css 都会 404。
 */
export default defineConfig(({ mode }) => {
  // 读 .env.production 里的 VITE_API_BASE
  const env = loadEnv(mode, process.cwd(), '')
  // GitHub Pages 仓库名（部署用）；本地开发保持 '/' 
  const base = env.VITE_BASE || '/'

  return {
    base,
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
  }
})
