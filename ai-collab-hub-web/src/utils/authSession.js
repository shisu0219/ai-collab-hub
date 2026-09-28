/**
 * 登录态存取。
 * 统一从这里读写 localStorage，别在页面里到处写 localStorage.getItem('token')。
 */

const STORAGE_FOR_LOGIN = 'ai_collab_login'
const USER_STORE_NAME = 'ai_collab_user'

/** 取 token */
export function getToken() {
  return localStorage.getItem(STORAGE_FOR_LOGIN) || ''
}

/** 存 token */
export function setToken(token) {
  localStorage.setItem(STORAGE_FOR_LOGIN, token || '')
}

/** 取用户信息对象 */
export function getUserInfo() {
  const raw = localStorage.getItem(USER_STORE_NAME)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

/** 存用户信息 */
export function setUserInfo(user) {
  localStorage.setItem(USER_STORE_NAME, JSON.stringify(user || {}))
}

/** 取角色ID。后端不直接返回 roleId 时，按 roleCode 反推 */
export function getRoleId() {
  const user = getUserInfo()
  if (!user) {
    return ''
  }
  const rid = user?.roleInfo?.id ?? user?.roleId
  if (rid != null && rid !== '') {
    return String(rid)
  }
  // 后端只给了 roleCodes，按标识反推 ID（和后端 SysConstants 一致）
  const code = getRoleCode()
  const map = { ADMIN: '1', STUDENT: '2' }
  return map[code] || ''
}

/** 取角色标识（后端返回的是 roleCodes 数组，取第一个） */
export function getRoleCode() {
  const user = getUserInfo()
  if (!user) {
    return ''
  }
  // 后端字段是 roleCodes: ['ADMIN']，兼容一下其它可能的写法
  const codes = user.roleCodes || user.roleCode || user.roleInfo?.code
  if (Array.isArray(codes)) {
    return codes[0] || ''
  }
  return codes || ''
}

/** 是否已登录 */
export function isLogin() {
  return !!getToken()
}

/** 是否管理员 */
export function isAdmin() {
  return getRoleId() === '1' || getRoleCode() === 'ADMIN'
}

/** 是否学生 */
export function isStudent() {
  return getRoleCode() === 'STUDENT'
}

/** 清空登录态 */
export function clearLoginState() {
  localStorage.removeItem(STORAGE_FOR_LOGIN)
  localStorage.removeItem(USER_STORE_NAME)
}

/** 登录成功后一次性写入 */
export function saveLoginState(token, user) {
  setToken(token)
  setUserInfo(user)
}
