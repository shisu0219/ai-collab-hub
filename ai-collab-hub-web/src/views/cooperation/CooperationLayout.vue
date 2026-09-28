<template>
  <div class="coop-layout">
    <!-- 顶部导航 -->
    <header class="coop-header">
      <div class="header-inner">
        <div class="logo-block" @click="goBrowse">
          <div class="logo-mark">AI</div>
          <div class="logo-text">
            <div class="logo-title">人工智能学院双创平台</div>
            <div class="logo-sub">校企协同 · 产学研对接</div>
          </div>
        </div>

        <nav class="nav-menu">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.name }}</span>
            <el-badge
              v-if="item.path === '/cooperation/notifications' && unreadCount > 0"
              :value="unreadCount"
              :max="99"
              class="nav-badge"
            />
            <el-badge
              v-if="item.path === '/cooperation/chat' && chatUnread > 0"
              :value="chatUnread"
              :max="99"
              class="nav-badge"
            />
          </router-link>
        </nav>

        <div class="header-right">
          <el-button type="primary" :icon="Plus" @click="goPublish">填写信息</el-button>

          <el-dropdown @command="handleUserCommand">
            <div class="user-block">
              <el-avatar :size="34" :src="userAvatar">
                {{ (userInfo?.nickname || 'U').charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ userInfo?.nickname || '未登录' }}</span>
              <el-tag size="small" :type="roleTagType" effect="plain">{{ roleLabel }}</el-tag>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
                <el-dropdown-item command="favorites" :icon="Star">我的收藏</el-dropdown-item>
                <el-dropdown-item
                  v-if="isAdminUser"
                  command="admin"
                  :icon="Setting"
                  divided
                >
                  管理后台
                </el-dropdown-item>
                <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- 内容区 -->
    <main class="coop-main">
      <router-view />
    </main>

    <!-- 页脚 -->
    <footer class="coop-footer">
      <div class="footer-inner">
        <span>人工智能学院双创平台</span>
        <span class="footer-sep">|</span>
        <span>校企产学研对接服务</span>
        <span class="footer-sep">|</span>
        <span>技术支持：人工智能学院创新创业中心</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
/**
 * 合作平台布局。
 * 顶部导航 + 内容区 + 页脚，学生和老师共用这套壳子，
 * 页面里再根据角色（roleCode）显示不同的文案。
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  UserFilled,
  Plus,
  ArrowDown,
  User,
  Star,
  Setting,
  SwitchButton,
  Compass,
  Document,
  Position,
  Bell,
  ChatDotRound
} from '@element-plus/icons-vue'
import { getUserInfo, clearLoginState, isAdmin, getRoleCode } from '@/utils/authSession'
import { watch } from 'vue'
import { getUnreadCount } from '@/api/notice'
import { unreadBus } from '@/utils/unreadBus'
import { getSessionList } from '@/api/chat'

const route = useRoute()
const router = useRouter()

const userInfo = ref(getUserInfo())
const unreadCount = ref(0)
const chatUnread = ref(0)

const isAdminUser = computed(() => isAdmin())
const roleCode = computed(() => getRoleCode())

const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', STUDENT: '学生', TEACHER: '老师' }
  return map[roleCode.value] || '用户'
})

const roleTagType = computed(() => {
  const map = { ADMIN: 'danger', STUDENT: 'primary', TEACHER: 'warning' }
  return map[roleCode.value] || 'info'
})

const userAvatar = computed(() => userInfo.value?.avatar || '')

const navItems = [
  { path: '/cooperation/browse', name: '浏览合作', icon: Compass },
  { path: '/cooperation/my-published', name: '我填写的', icon: Document },
  { path: '/cooperation/my-group', name: '我的小组', icon: UserFilled },
  { path: '/cooperation/applications', name: '我的申请', icon: Position },
  { path: '/cooperation/chat', name: '站内沟通', icon: ChatDotRound },
  { path: '/cooperation/notifications', name: '消息中心', icon: Bell }
]

function isActive(path) {
  return route.path.startsWith(path)
}

function goBrowse() {
  router.push('/cooperation/browse')
}

function goPublish() {
  router.push('/cooperation/publish')
}

async function loadUnread() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res?.data?.total ?? 0
  } catch (e) {
    // 静默失败，不打扰用户
  }
  try {
    const res2 = await getSessionList()
    const list = res2?.data || []
    chatUnread.value = list.reduce((sum, it) => sum + (it.unreadCount || 0), 0)
  } catch (e) {
    // 静默失败
  }
}

async function handleUserCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'favorites') {
    router.push('/cooperation/favorites')
  } else if (cmd === 'admin') {
    router.push('/admin/dashboard')
  } else if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      clearLoginState()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch (e) {
      // 用户取消
    }
  }
}

let timer = null

/*
 * 监听全局未读信号，任何地方读完消息立刻刷新两个红点。
 *
 * 不加这个的话，用户点完「全部已读」或进会话读完消息，
 * 红点要等最多 60 秒（轮询间隔）才消失，看起来像没生效。
 */
watch(
    () => unreadBus.version,
    () => {
      loadUnread()
    }
)

onMounted(() => {
  loadUnread()
  // 每 60 秒刷新一次未读数
  timer = setInterval(loadUnread, 60000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.coop-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.coop-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1320px;
  margin: 0 auto;
  padding: 0 20px;
  height: 68px;
  display: flex;
  align-items: center;
  gap: 30px;
}

.logo-block {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo-mark {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1d3a6b, #2c5490);
  color: #c9a227;
  font-weight: 700;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d3a6b;
  line-height: 1.2;
}

.logo-sub {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  color: #606266;
  font-size: 14px;
  transition: all 0.2s;
  position: relative;
}

.nav-item:hover {
  background: #f0f3f8;
  color: #1d3a6b;
}

.nav-item.active {
  background: #eef2f9;
  color: #1d3a6b;
  font-weight: 600;
}

.nav-badge {
  position: absolute;
  top: 2px;
  right: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.user-block {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-block:hover {
  background: #f0f3f8;
}

.user-name {
  font-size: 14px;
  color: #303133;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coop-main {
  flex: 1;
  width: 100%;
}

.coop-footer {
  background: #fff;
  border-top: 1px solid #e4e7ed;
  padding: 20px 0;
  margin-top: 40px;
}

.footer-inner {
  max-width: 1320px;
  margin: 0 auto;
  padding: 0 20px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.footer-sep {
  margin: 0 10px;
  color: #dcdfe6;
}

@media (max-width: 1100px) {
  .nav-item span {
    display: none;
  }
  .logo-sub {
    display: none;
  }
}
</style>
