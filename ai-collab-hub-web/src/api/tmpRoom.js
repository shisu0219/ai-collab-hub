import request from '@/api/request'

/**
 * 临时沟通通道接口。
 *
 * 和「站内沟通」（chat.js）是两套东西：
 *   chat.js    长期会话，登录后在站内沟通页聊
 *   本文件      临时房间，凭链接进入，有有效期，到点自动失效
 *
 * 注意 enter / getMessages 是**免登录**的（凭 token），
 * 所以这两个请求即使没登录也能成功 —— 别以为是鉴权漏了。
 */

/** 凭链接进入通道（免登录可查看） */
export function enterRoom(token) {
  return request({ url: '/tmp/room/enter', method: 'get', params: { token } })
}

/** 查通道消息（凭 token） */
export function getRoomMessages(params) {
  return request({ url: '/tmp/room/messages', method: 'get', params })
}

/** 在通道里发言（需登录且须是参与人） */
export function sendRoomMessage(data) {
  return request({ url: '/tmp/room/message', method: 'post', data })
}

/** 关闭通道 */
export function closeRoom(roomId) {
  return request({ url: '/tmp/room/close', method: 'post', params: { roomId } })
}

/** 延长有效期 */
export function extendRoom(roomId, hours) {
  return request({ url: '/tmp/room/extend', method: 'post', params: { roomId, hours } })
}

/** 我参与的通道列表 */
export function getMyRooms() {
  return request({ url: '/tmp/room/my', method: 'get' })
}

/** 按项目查有效通道（判断要不要显示入口） */
export function getRoomByArticle(articleId) {
  return request({ url: `/tmp/room/by-article/${articleId}`, method: 'get' })
}

/** 按申请查有效通道 */
export function getRoomByRegistration(registrationId) {
  return request({ url: `/tmp/room/by-registration/${registrationId}`, method: 'get' })
}

/** 手动补开通道（过期后重开） */
export function createRoom(data) {
  return request({ url: '/tmp/room/create', method: 'post', data })
}
