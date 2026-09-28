import request from '@/api/request'

/**
 * 答辩意见接口。
 * 意见由管理员上传（答辩现场老师方/专业方给出的问题和意见）。
 *
 * 注意：能看到的范围由后端决定，前端拿到什么就渲染什么。
 * truncated=true 表示正文被截断（你是组外人员），需要提示「加入小组查看全部」。
 */

// ==================== 查询（学生 / 老师都能调） ====================

/** 按小组查答辩意见列表 */
export function getOpinionList(params) {
  return request({ url: '/blog/opinion/list', method: 'get', params })
}

/** 按项目查答辩意见列表 */
export function getOpinionListByArticle(params) {
  return request({ url: '/blog/opinion/list/by-article', method: 'get', params })
}

/** 答辩意见详情 */
export function getOpinionDetail(id) {
  return request({ url: `/blog/opinion/${id}`, method: 'get' })
}

/** 某小组可见的答辩意见条数 */
export function getOpinionCount(groupId) {
  return request({ url: '/blog/opinion/count', method: 'get', params: { groupId } })
}

// ==================== 组长：设置可见性 ====================

/**
 * 设置可见性。
 * 只传 groupId = 改全局默认；同时传 opinionId = 改某一条的覆盖值。
 * scope 传 null 表示单条恢复「跟随全局」。
 */
export function setOpinionScope(data) {
  return request({ url: '/blog/opinion/scope', method: 'put', data })
}

// ==================== 管理员 ====================

/** 管理员上传或修改答辩意见 */
export function saveOpinion(data) {
  return request({ url: '/blog/opinion/admin/save', method: 'post', data })
}

/** 管理员批量导入（前端把表格解析成数组后提交） */
export function importOpinions(groupId, rows) {
  return request({
    url: '/blog/opinion/admin/import',
    method: 'post',
    params: { groupId },
    data: rows
  })
}

/** 管理员删除答辩意见 */
export function deleteOpinion(id) {
  return request({ url: `/blog/opinion/admin/${id}`, method: 'delete' })
}
