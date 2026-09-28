import request from '@/api/request'

/**
 * 管理后台接口。
 */

// ==================== 账户审核 ====================

/** 待审核用户列表 */
export function getUserReviewList(params) {
  return request({ url: '/user/review/list', method: 'get', params })
}

/** 审核用户：通过 / 拒绝 */
export function reviewUser(data) {
  return request({ url: '/user/review', method: 'post', data })
}

/** 【新增】批量审核用户 */
export function batchReviewUser(data) {
  return request({ url: '/user/review/batch', method: 'post', data })
}

// ==================== 账户管理 ====================

/** 用户列表 */
export function getUserList(params) {
  return request({ url: '/root/user/list', method: 'get', params })
}

/** 启用 / 禁用用户 */
export function toggleUserEnable(data) {
  return request({ url: '/root/user/enable', method: 'post', data })
}

// ==================== 内容审核 ====================

/** 待审内容列表 */
export function getArticleReviewList(params) {
  return request({ url: '/blog/article/review/list', method: 'get', params })
}

/** 审核内容 */
export function reviewArticle(data) {
  return request({ url: '/blog/article/review', method: 'post', data })
}

/** 【新增】批量审核内容 */
export function batchReviewArticle(data) {
  return request({ url: '/blog/article/review/batch', method: 'post', data })
}

// ==================== 标签管理 ====================

/** 新增标签 */
export function addTag(data) {
  return request({ url: '/root/blog/tag', method: 'post', data })
}

/** 删除标签 */
export function deleteTag(id) {
  return request({ url: `/root/blog/tag/${id}`, method: 'delete' })
}

// ==================== 数据看板（新增） ====================

/** 看板总览数据 */
export function getDashboardOverview() {
  return request({ url: '/sys/stat/overview', method: 'get' })
}

/** 注册趋势 */
export function getRegisterTrend(params) {
  return request({ url: '/sys/stat/register-trend', method: 'get', params })
}

/** 内容填写趋势 */
export function getArticleTrend(params) {
  return request({ url: '/sys/stat/article-trend', method: 'get', params })
}

/** 对接成功率 */
export function getCollabStat() {
  return request({ url: '/sys/stat/collab', method: 'get' })
}

// ==================== 拒绝理由模板（新增） ====================

/** 拒绝理由模板列表 */
export function getRejectTemplateList(params) {
  return request({ url: '/sys/tool/reject-template/list', method: 'get', params })
}

// ==================== 操作日志（新增） ====================

/** 操作日志列表 */
export function getOperationLogList(params) {
  return request({ url: '/sys/log/list', method: 'get', params })
}

// ==================== 通用：搜人（邀请成员用） ====================

/** 按账号或昵称搜人，所有登录用户都能调（区别于 /root/user/list） */
export function searchUserList(params) {
  return request({ url: '/user/find/list', method: 'get', params })
}
