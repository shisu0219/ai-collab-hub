import request from '@/api/request'

/**
 * 用户信息接口。
 */

/** 我的信息 */
export function getMyInfo() {
  return request({ url: '/user/info', method: 'get' })
}

/** 修改我的信息 */
export function updateMyInfo(data) {
  return request({ url: '/user/info', method: 'put', data })
}

/** 上传头像 */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/user/info/avatar',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 查看他人主页信息 */
export function getUserInfoById(id) {
  return request({ url: `/user/info/${id}`, method: 'get' })
}
