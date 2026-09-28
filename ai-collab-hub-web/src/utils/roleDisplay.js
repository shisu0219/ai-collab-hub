/**
 * 角色展示相关的小工具。
 * 页面上经常要根据角色显示不同文案，集中放这里。
 */

/** 角色ID 常量，和后端 CommonConst 保持一致 */
export const ROLE_ID = {
  ADMIN: 1,
  STUDENT: 2,
  TEACHER: 4
}

/** 角色标识 */
export const ROLE_CODE = {
  ADMIN: 'ADMIN',
  STUDENT: 'STUDENT',
  TEACHER: 'TEACHER'
}

/**
 * 角色ID 转中文名
 */
export function roleName(roleId) {
  const map = {
    1: '系统管理员',
    2: '学生',
    4: '老师'
  }
  return map[String(roleId)] || '未知角色'
}

/**
 * 从后端返回的一行数据里解析出角色中文名。
 *
 * 后端不同接口给的字段不一样：
 *   - 审核列表：roleId + roleName
 *   - 账户管理：roleCodes + roleNames（字符串，可能是 "TEACHER" / "老师"）
 *   - 有的接口只给 roleCode
 * 所以按「roleName → roleNames → roleId → roleCode」的顺序兜底，
 * 不要再用二元判断谁是谁，
 * 那样老师会被显示成学生，管理员也会。
 */
export function resolveRoleName(row) {
  if (!row) {
    return '未知角色'
  }
  // 1. 后端直接给了中文名
  if (row.roleName) {
    return row.roleName
  }
  if (row.roleNames) {
    const s = Array.isArray(row.roleNames) ? row.roleNames.join('、') : String(row.roleNames)
    if (s) {
      return s
    }
  }
  // 2. 有 roleId 就查表
  if (row.roleId != null && row.roleId !== '') {
    const byId = { 1: '系统管理员', 2: '学生', 4: '老师' }
    if (byId[String(row.roleId)]) {
      return byId[String(row.roleId)]
    }
  }
  // 3. 用角色码转
  const code = Array.isArray(row.roleCodes) ? row.roleCodes[0] : row.roleCode
  const byCode = {
    ADMIN: '系统管理员',
    STUDENT: '学生',
    TEACHER: '老师'
  }
  return byCode[code] || '未知角色'
}

/**
 * 角色对应的标签颜色
 */
export function resolveRoleTagType(row) {
  if (!row) {
    return 'info'
  }
  const code = Array.isArray(row.roleCodes) ? row.roleCodes[0] : row.roleCode
  const map = { ADMIN: 'danger', STUDENT: 'primary', TEACHER: 'warning' }
  if (map[code]) {
    return map[code]
  }
  const byId = { 1: 'danger', 2: 'primary', 4: 'warning' }
  return byId[String(row.roleId)] || 'info'
}

/**
 * 审核状态转中文
 */
export function auditStatusName(status) {
  const map = {
    0: '待审核',
    1: '已通过',
    2: '已拒绝'
  }
  return map[String(status)] ?? '未知'
}

/**
 * 审核状态对应的标签颜色（Element Plus tag type）
 */
export function auditStatusTagType(status) {
  const map = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return map[String(status)] || 'info'
}

/**
 * 内容状态转中文
 */
export function articleStatusName(statusId) {
  const map = {
    1: '待审核',
    2: '已发布',
    3: '已拒绝',
    4: '已下架'
  }
  return map[String(statusId)] ?? '未知'
}

/**
 * 内容状态对应的标签颜色
 */
export function articleStatusTagType(statusId) {
  const map = {
    1: 'warning',
    2: 'success',
    3: 'danger',
    4: 'info'
  }
  return map[String(statusId)] || 'info'
}

/**
 * 对接进度转中文
 */
export function collabProgressName(code) {
  const map = {
    0: '待处理',
    1: '已通过',
    2: '洽谈中',
    3: '已合作',
    4: '已结束',
    5: '已拒绝'
  }
  return map[String(code)] ?? '未知'
}

/**
 * 对接进度对应颜色
 */
export function collabProgressTagType(code) {
  const map = {
    0: 'warning',
    1: 'success',
    2: 'primary',
    3: 'success',
    4: 'info',
    5: 'danger'
  }
  return map[String(code)] || 'info'
}

/**
 * 消息分类转中文
 */
export function noticeTypeName(type) {
  const map = {
    1: '审核结果',
    2: '收到申请',
    3: '系统通知',
    4: '会话消息'
  }
  return map[String(type)] ?? '通知'
}

/**
 * 根据当前角色，给出「对方」的称呼。
 * 平台只有学生 / 老师两端注册。
 */
export function roleOfOther(roleCode) {
  if (roleCode === ROLE_CODE.STUDENT) {
    return '老师'
  }
  return '对方'
}
