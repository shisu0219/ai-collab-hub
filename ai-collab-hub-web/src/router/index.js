import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken, getRoleId, getRoleCode, clearLoginState } from '@/utils/authSession'
import { ROLE_ID } from '@/utils/roleDisplay'

/**
 * 人工智能学院双创平台 - 路由配置
 *
 * 路由分三块：
 *   1. 认证页（登录/注册/找回密码）—— 免登录
 *   2. 合作平台（学生/老师共用）—— 需登录
 *   3. 管理后台 —— 需登录 + 仅管理员
 */

const routes = [
  // ---------- 默认进登录页 ----------
  { path: '/', redirect: '/login' },

  // ---------- 认证页 ----------
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/auth/ForgotPassword.vue'),
    meta: { title: '找回密码' }
  },

  // ---------- 临时沟通通道 ----------
  // 【注意】这个页面故意不放在 requiresAuth 下面：
  // 临时通道是「凭链接进入」的，有链接就能查看消息（像共享文档那样）。
  // 发言才需要登录，由页面内部判断，后端也会再校验一次参与人身份。
  {
    path: '/tmp-room/:token',
    name: 'TmpRoom',
    component: () => import('@/views/cooperation/TmpRoom.vue'),
    meta: { title: '临时沟通通道' }
  },

  // ---------- 合作平台 ----------
  {
    path: '/cooperation',
    component: () => import('@/views/cooperation/CooperationLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/cooperation/browse' },
      {
        path: 'browse',
        name: 'Browse',
        component: () => import('@/views/cooperation/BrowseProjects.vue'),
        meta: { title: '浏览合作' }
      },
      {
        path: 'my-published',
        name: 'MyPublished',
        component: () => import('@/views/cooperation/MyPublished.vue'),
        meta: { title: '我填写的' }
      },
      {
        path: 'publish',
        name: 'Publish',
        component: () => import('@/views/cooperation/PublishProject.vue'),
        meta: { title: '填写信息' }
      },
      {
        path: 'applications',
        name: 'MyApplications',
        component: () => import('@/views/cooperation/MyApplications.vue'),
        meta: { title: '我的申请' }
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/cooperation/Notifications.vue'),
        meta: { title: '消息中心' }
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/cooperation/ChatCenter.vue'),
        meta: { title: '站内沟通' }
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: () => import('@/views/cooperation/MyFavorites.vue'),
        meta: { title: '我的收藏' }
      },
      {
        path: 'my-group',
        name: 'MyGroup',
        component: () => import('@/views/cooperation/MyGroup.vue'),
        meta: { title: '我的小组' }
      },
      {
        path: 'detail/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/cooperation/ProjectDetail.vue'),
        meta: { title: '详情' }
      },
      {
        path: 'user/:id',
        name: 'UserHome',
        component: () => import('@/views/cooperation/UserHome.vue'),
        meta: { title: '个人主页' }
      }
    ]
  },

  // ---------- 个人中心 ----------
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/Profile.vue'),
    meta: { requiresAuth: true, title: '个人中心' }
  },

  // ---------- 管理后台 ----------
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据看板' }
      },
      {
        path: 'review-account',
        name: 'ReviewAccount',
        component: () => import('@/views/admin/AccountReview.vue'),
        meta: { title: '账户审核' }
      },
      {
        path: 'manage-account',
        name: 'ManageAccount',
        component: () => import('@/views/admin/AccountManage.vue'),
        meta: { title: '账户管理' }
      },
      {
        path: 'review-content',
        name: 'ReviewContent',
        component: () => import('@/views/admin/ContentReview.vue'),
        meta: { title: '内容管理' }
      },
      {
        path: 'tag',
        name: 'TagManage',
        component: () => import('@/views/admin/TagManage.vue'),
        meta: { title: '标签管理' }
      },
      {
        path: 'group',
        name: 'GroupManage',
        component: () => import('@/views/admin/GroupManage.vue'),
        meta: { title: '小组管理' }
      },
      {
        path: 'opinion',
        name: 'OpinionManage',
        component: () => import('@/views/admin/OpinionManage.vue'),
        meta: { title: '答辩意见管理' }
      },
      {
        path: 'log',
        name: 'OperationLog',
        component: () => import('@/views/admin/OperationLog.vue'),
        meta: { title: '操作日志' }
      }
    ]
  },

  // ---------- 404 ----------
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  document.title = to.meta?.title
      ? `${to.meta.title} - 人工智能学院双创平台`
      : '人工智能学院双创平台'

  const logged = !!getToken()
  const roleId = getRoleId()
  const roleCode = getRoleCode()

  // 自愈：token 没了但用户信息还在 —— 这是旧版本留下的脏数据。
  // 之前旧代码把 token 存成了 undefined，导致每次刷新都被守卫当成"没登录"踢回登录页，
  // 用户反复登录也进不去。这里主动清掉，并明确提示，省得用户一脸懵。
  const hasUserInfo = !!(roleId || roleCode)
  if (!logged && hasUserInfo) {
    clearLoginState()
    ElMessage.warning('登录状态已失效，请重新登录')
    if (to.path !== '/login') {
      next({ path: '/login', replace: true })
      return
    }
  }

  // 管理后台：必须登录 + 管理员角色
  if (to.matched.some((r) => r.meta.requiresAdmin)) {
    if (!logged) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }

    // 读不到角色说明本地缓存是坏的（比如旧版本存进去的数据），
    // 直接把登录态清掉回登录页，别让用户卡在一个"点了没反应"的状态里。
    if (!roleId && !roleCode) {
      clearLoginState()
      ElMessage.warning('登录状态异常，请重新登录')
      next({ path: '/login', replace: true })
      return
    }

    if (roleId !== String(ROLE_ID.ADMIN) && roleCode !== 'ADMIN') {
      ElMessage.warning('无权限访问管理后台')
      next({ path: '/cooperation', replace: true })
      return
    }
    next()
    return
  }

  // 需要登录的普通页面
  if (to.matched.some((r) => r.meta.requiresAuth) && !logged) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  next()
})

export default router
