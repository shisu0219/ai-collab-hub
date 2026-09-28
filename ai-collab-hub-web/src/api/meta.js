import request from '@/api/request'

/**
 * 申请表单的固定选项（年级 / 班级 / 擅长技能）。
 * 后端硬编码维护，前端通过接口拿 —— 要改只需改后端一处。
 */

/** 一次拿全：年级 + 班级映射 + 技能列表 */
export function getApplyOptions() {
  return request({ url: '/blog/meta/apply-options', method: 'get' })
}

/** 只要年级 */
export function getGrades() {
  return request({ url: '/blog/meta/grades', method: 'get' })
}

/** 按年级取班级 */
export function getClasses(grade) {
  return request({ url: '/blog/meta/classes', method: 'get', params: { grade } })
}

/** 擅长技能选项 */
export function getSkillOptions() {
  return request({ url: '/blog/meta/skills', method: 'get' })
}
