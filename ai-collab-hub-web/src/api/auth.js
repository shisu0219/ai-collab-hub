import request from '@/api/request'

/**
 * 认证相关接口：登录、注册、找回密码。
 */

/** 学生端登录 */
export function loginStudent(data) {
  return request({ url: '/user/login/student', method: 'post', data })
}

/** 老师登录 */
export function loginTeacher(data) {
  return request({ url: '/user/login/teacher', method: 'post', data })
}

/** 管理员登录 */
export function loginAdmin(data) {
  return request({ url: '/user/login/admin', method: 'post', data })
}

/** 老师注册 */
export function registerTeacher(data) {
  return request({ url: '/user/register/teacher', method: 'post', data })
}

/** 学生注册 */
export function registerStudent(data) {
  return request({ url: '/user/register/common', method: 'post', data })
}

/**
 * 找回密码（手机号版）。
 *
 * 凭「账号 + 手机号」双重校验重置密码。
 * 手机号在注册时是必填的，所以每个账号都能用这个方式找回。
 */
export function findPasswordByPhone(data) {
  return request({ url: '/user/find/pwd/phone', method: 'post', data })
}

/**
 * 找回密码（邮箱版）。
 *
 * 凭「账号 + 邮箱」双重校验重置密码，和手机号版**并行**，任选其一都能重置。
 * 邮箱是注册时选填的，没填邮箱的账号用不了这个方式。
 */
export function findPassword(data) {
  return request({ url: '/user/find/pwd/email', method: 'post', data })
}

/** 退出登录（后端清 Redis 里的登录态） */
export function logout() {
  return request({ url: '/user/logout', method: 'post' })
}
