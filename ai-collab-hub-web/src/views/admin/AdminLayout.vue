<template>
  <el-container class="admin-layout">
    <!-- 左侧菜单 -->
    <el-aside :width="collapsed ? '64px' : '220px'" class="admin-aside">
      <div class="aside-logo">
        <div class="logo-mark">AI</div>
        <span v-show="!collapsed" class="logo-text">双创平台管理后台</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        class="admin-menu"
        router
        background-color="transparent"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>

        <el-menu-item index="/admin/review-account">
          <el-icon><UserFilled /></el-icon>
          <template #title>
            账户审核
            <el-badge v-if="pendingUserCount > 0" :value="pendingUserCount" :max="99" class="menu-badge" />
          </template>
        </el-menu-item>

        <el-menu-item index="/admin/review-content">
          <el-icon><DocumentChecked /></el-icon>
          <template #title>
            内容管理
            <el-badge v-if="pendingArticleCount > 0" :value="pendingArticleCount" :max="99" class="menu-badge" />
          </template>
        </el-menu-item>

        <el-menu-item index="/admin/manage-account">
          <el-icon><User /></el-icon>
          <template #title>账户管理</template>
        </el-menu-item>

        <el-menu-item index="/admin/group">
          <el-icon><OfficeBuilding /></el-icon>
          <template #title>小组管理</template>
        </el-menu-item>

        <el-menu-item index="/admin/tag">
          <el-icon><PriceTag /></el-icon>
          <template #title>标签管理</template>
        </el-menu-item>

        <el-menu-item index="/admin/opinion">
          <el-icon><ChatLineSquare /></el-icon>
          <template #title>答辩意见管理</template>
        </el-menu-item>

        <el-menu-item index="/admin/log">
          <el-icon><Tickets /></el-icon>
          <template #title>操作日志</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header class="admin-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <h3 class="page-title">{{ currentTitle }}</h3>
        </div>

        <div class="header-right">
          <el-button :icon="Compass" text @click="goCooperation">前台首页</el-button>
          <el-dropdown @command="handleCommand">
            <div class="user-block">
              <el-avatar :size="32">{{ (adminName || 'A').charAt(0) }}</el-avatar>
              <span class="user-name">{{ adminName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
/**
 * 管理后台布局。
 * 左侧菜单 + 顶部栏 + 内容区。菜单上带待办数量角标，管理员一眼知道有多少活没干。
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  DataAnalysis,
  UserFilled,
  DocumentChecked,
  User,
  PriceTag,
  Tickets,
  Fold,
  Expand,
  ArrowDown,
  SwitchButton,
  Compass,
  OfficeBuilding,
  ChatLineSquare
} from '@element-plus/icons-vue'
import { getUserInfo, clearLoginState } from '@/utils/authSession'
import { getUserReviewList, getArticleReviewList } from '@/api/admin'

const route = useRoute()
const router = useRouter()

const collapsed = ref(false)
const pendingUserCount = ref(0)
const pendingArticleCount = ref(0)

const adminName = computed(() => getUserInfo()?.nickname || '管理员')

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => {
  const map = {
    '/admin/dashboard': '数据看板',
    '/admin/review-account': '账户审核',
    '/admin/manage-account': '账户管理',
    '/admin/review-content': '内容管理',
    '/admin/tag': '标签管理',
    '/admin/log': '操作日志'
  }
  return map[route.path] || '管理后台'
})

async function loadBadges() {
  try {
    const res = await getUserReviewList({ pageNumber: 1, pageSize: 1, auditStatus: 0 })
    pendingUserCount.value = res?.data?.total || 0
  } catch (e) {
    pendingUserCount.value = 0
  }
  try {
    const res2 = await getArticleReviewList({ pageNumber: 1, pageSize: 1, statusId: 1 })
    pendingArticleCount.value = res2?.data?.total || 0
  } catch (e) {
    pendingArticleCount.value = 0
  }
}

function goCooperation() {
  router.push('/cooperation/browse')
}

async function handleCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      clearLoginState()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch (e) {
      // 取消
    }
  }
}

let timer = null

onMounted(() => {
  loadBadges()
  timer = setInterval(loadBadges, 60000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.admin-aside {
  background: #1d3a6b;
  transition: width 0.2s;
  overflow-x: hidden;
}

.aside-logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  white-space: nowrap;
}

.logo-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #c9a227;
  color: #1d3a6b;
  font-weight: 700;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-text {
  font-size: 14px;
  font-weight: 600;
}

.admin-menu {
  border-right: none;
  padding-top: 8px;
}

.admin-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.75);
  height: 48px;
}

.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: rgba(201, 162, 39, 0.18);
  color: #e0be52;
  border-right: 3px solid #c9a227;
}

.menu-badge {
  margin-left: 6px;
}

.menu-badge :deep(.el-badge__content) {
  position: static;
  transform: none;
}

.admin-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  font-size: 19px;
  cursor: pointer;
  color: #606266;
}

.collapse-btn:hover {
  color: #1d3a6b;
}

.page-title {
  font-size: 17px;
  color: #1d3a6b;
  margin: 0;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-block {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}

.user-block:hover {
  background: #f0f3f8;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.admin-main {
  background: #f5f7fa;
  padding: 20px;
}
</style>
