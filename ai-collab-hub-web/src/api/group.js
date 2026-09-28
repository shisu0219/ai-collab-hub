import request from '@/api/request'

/**
 * 项目小组接口。一个小组只能有一个项目，组长是小组的负责人。
 */

// ==================== 建组 / 改组 ====================

/** 创建小组（组长自己建） */
export function createGroup(data) {
  return request({ url: '/blog/group', method: 'post', data })
}

/** 修改小组信息（组长） */
export function updateGroup(data) {
  return request({ url: '/blog/group', method: 'put', data })
}

/** 解散小组（组长） */
export function dismissGroup(groupId) {
  return request({ url: `/blog/group/${groupId}`, method: 'delete' })
}

// ==================== 查询 ====================

/** 我的小组列表 */
export function getMyGroups() {
  return request({ url: '/blog/group/my', method: 'get' })
}

/** 小组详情 */
export function getGroupDetail(groupId) {
  return request({ url: `/blog/group/${groupId}`, method: 'get' })
}

/** 按项目ID查小组 */
export function getGroupByArticle(articleId) {
  return request({ url: `/blog/group/by-article/${articleId}`, method: 'get' })
}

/** 查某人担任组长的小组 */
export function getGroupByLeader(leaderId) {
  return request({ url: `/blog/group/by-leader/${leaderId}`, method: 'get' })
}

// ==================== 成员管理 ====================

/** 邀请成员进组（需要对方同意） */
export function inviteMembers(data) {
  return request({ url: '/blog/group/invite', method: 'post', data })
}

/** 同意 / 拒绝进组邀请 */
export function handleInvite(data) {
  return request({ url: '/blog/group/invite/handle', method: 'post', data })
}

/** 我的待处理邀请 */
export function getMyInvites() {
  return request({ url: '/blog/group/invite/my', method: 'get' })
}

/** 移除成员（组长） */
export function removeMember(groupId, userId) {
  return request({ url: '/blog/group/member', method: 'delete', params: { groupId, userId } })
}

/** 转让组长 */
export function transferLeader(groupId, newLeaderId) {
  return request({ url: '/blog/group/transfer-leader', method: 'post', params: { groupId, newLeaderId } })
}

// ==================== 管理员 ====================

/** 管理员代建小组 */
export function adminCreateGroup(data) {
  return request({ url: '/blog/group/admin/create', method: 'post', data })
}

/** 管理员直接拉人进组（无需同意） */
export function adminAddMembers(groupId, userIds, memberRole = 2) {
  return request({
    url: '/blog/group/admin/add-members',
    method: 'post',
    params: { groupId, memberRole },
    data: userIds
  })
}

/** 管理员查看全部小组 */
export function adminListGroups() {
  return request({ url: '/blog/group/admin/list', method: 'get' })
}

/** 把项目绑定到小组（一个小组只能有一个项目） */
export function bindArticleToGroup(groupId, articleId) {
  return request({
    url: '/blog/group/bind-article',
    method: 'post',
    params: { groupId, articleId }
  })
}
