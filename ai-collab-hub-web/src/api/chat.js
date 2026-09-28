import request from '@/api/request'

/**
 * 站内会话接口（新增功能）。
 * 对接申请通过后，双方可以在这里直接聊，不用再互相加微信。
 */

/** 我的会话列表 */
export function getSessionList() {
  return request({ url: '/chat/session/list', method: 'get' })
}

/** 按对接申请ID取会话 */
export function getSessionByRegistration(registrationId) {
  return request({ url: `/chat/session/by-registration/${registrationId}`, method: 'get' })
}

/** 创建会话 */
export function createSession(data) {
  return request({ url: '/chat/session', method: 'post', data })
}

/** 会话内消息分页 */
export function getChatMessages(params) {
  return request({ url: '/chat/message/list', method: 'get', params })
}

/** 发消息 */
export function sendChatMessage(data) {
  return request({ url: '/chat/message', method: 'post', data })
}

/** 会话消息已读 */
export function markSessionRead(sessionId) {
  // 后端这个接口用的是 URL 参数（@RequestParam），不是请求体
  return request({ url: '/chat/message/read', method: 'post', params: { sessionId } })
}
