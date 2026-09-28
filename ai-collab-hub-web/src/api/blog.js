import request from '@/api/request'

/**
 * 文章 / 合作内容相关接口。
 * 学生发「项目」，老师发「需求」，走的是同一套接口，靠 typeId 区分。
 */

/** 文章类型字典 */
export function getArticleTypeList() {
  return request({ url: '/blog/article/static/type', method: 'get' })
}

/** 文章状态字典 */
export function getArticleStatusList() {
  return request({ url: '/blog/article/static/status', method: 'get' })
}

/** 进度字典 */
export function getArticleProgressList(params) {
  return request({ url: '/blog/article/static/progress', method: 'get', params })
}

/** 标签字典，支持按名字模糊查 */
export function getArticleTagList(params) {
  return request({ url: '/blog/article/static/tag', method: 'get', params })
}

/** 一次性拿全部字典 */
export function getAllDict(params) {
  return request({ url: '/blog/article/static/all', method: 'get', params })
}

/**
 * 浏览内容列表（简要信息）
 * @param {Object} params { pageNumber, pageSize, title, typeId, location, tagId, statusId }
 */
export function getArticleList(params) {
  return request({ url: '/blog/article/list/brief', method: 'get', params })
}

/** 我填写的信息列表 */
export function getMyArticleList(params) {
  return request({ url: '/blog/article/list', method: 'get', params })
}

/** 某个用户填写的内容列表（个人主页聚合） */
export function getUserArticleList(params) {
  return request({ url: '/blog/article/list/by-user', method: 'get', params })
}

/** 文章详情 */
export function getArticleDetail(id) {
  return request({ url: `/blog/article/list/brief/${id}`, method: 'get' })
}

/** 填写内容 */
export function publishArticle(data) {
  return request({ url: '/blog/article', method: 'post', data })
}

/** 修改内容 */
export function updateArticle(data) {
  return request({ url: '/blog/article', method: 'put', data })
}

/** 删除内容 */
export function deleteArticle(id) {
  return request({ url: `/blog/article/${id}`, method: 'delete' })
}

/** 上传文章附件 */
export function uploadArticleFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/blog/article/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 删除附件 */
export function deleteAttachment(params) {
  return request({ url: '/blog/article/attachment', method: 'delete', params })
}

// ==================== 对接申请 ====================

/** 提交对接申请（学生 / 老师「感兴趣」） */
export function submitRegistration(data) {
  return request({ url: '/blog/registration', method: 'post', data })
}

/** 申请列表：我发出的 / 我收到的 */
export function getRegistrationList(params) {
  return request({ url: '/blog/registration/list', method: 'get', params })
}

/** 申请详情 */
export function getRegistrationDetail(id) {
  return request({ url: `/blog/registration/${id}`, method: 'get' })
}

/** 处理申请：通过 / 拒绝 */
export function handleRegistration(data) {
  return request({ url: '/blog/registration/review', method: 'post', data })
}

/** 更新对接进度 */
export function updateCollabProgress(data) {
  return request({ url: '/blog/registration/progress', method: 'put', data })
}

/** 撤回申请 */
export function cancelRegistration(id) {
  return request({ url: `/blog/registration/${id}`, method: 'delete' })
}

// ==================== 收藏 ====================

/** 收藏 */
export function addFavorite(data) {
  return request({ url: '/blog/favorite', method: 'post', data })
}

/** 取消收藏 */
export function cancelFavorite(data) {
  return request({ url: '/blog/favorite', method: 'delete', data })
}

/** 我的收藏列表 */
export function getFavoriteList(params) {
  return request({ url: '/blog/favorite/list', method: 'get', params })
}

/** 判断是否已收藏 */
export function checkFavorite(params) {
  return request({ url: '/blog/favorite/check', method: 'get', params })
}
