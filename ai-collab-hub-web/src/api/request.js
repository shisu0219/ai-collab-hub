import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { clearLoginState, getToken } from '@/utils/authSession'

/**
 * axios 统一封装。
 * 请求自动带 token，响应统一剥壳，401 自动跳登录页。
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// ---------- 请求拦截 ----------
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---------- 响应拦截 ----------
request.interceptors.response.use(
  (response) => {
    // 文件流（下载、预览）直接返回，不做剥壳
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response
    }

    const res = response.data
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 200) {
        return res
      }
      // 401 登录失效，清空本地状态回登录页
      if (res.code === 401) {
        clearLoginState()
        ElMessage.error(res.msg || '登录已失效，请重新登录')
        router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
        return Promise.reject(new Error(res.msg || '登录已失效'))
      }
      ElMessage.error(res.msg || '操作失败')
      return Promise.reject(new Error(res.msg || '操作失败'))
    }
    return response
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      clearLoginState()
      ElMessage.error('登录已失效，请重新登录')
      router.push({ path: '/login' })
    } else if (status === 403) {
      ElMessage.error('没有权限执行该操作')
    } else if (status === 404) {
      ElMessage.error('接口不存在，请检查后端服务')
    } else if (status >= 500) {
      ElMessage.error('服务器开小差了，请稍后再试')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后再试')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
