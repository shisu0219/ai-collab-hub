import request from '@/api/request'

/**
 * 消息通知接口。
 */

/** 我的消息列表，支持 msgType 分类、title 搜索 */
export function getNoticeList(params) {
  return request({ url: '/notice/info/list', method: 'get', params })
}

/** 未读数量统计 */
export function getUnreadCount() {
  return request({ url: '/notice/info/unread', method: 'get' })
}

/** 标记单条已读 */
export function markNoticeRead(id) {
  return request({ url: '/notice/info/read-status', method: 'post', data: { id } })
}

/** 全部已读 */
export function markAllRead(data) {
  return request({ url: '/notice/info/read-all', method: 'post', data })
}

/** 删除消息 */
export function deleteNotice(id) {
  return request({ url: `/notice/info/${id}`, method: 'delete' })
}
