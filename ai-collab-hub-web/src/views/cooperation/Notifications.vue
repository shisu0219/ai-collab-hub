<template>
  <div class="notifications-page">
    <div class="page-container">
      <div class="head-row">
        <div>
          <h2 class="head-title">消息中心</h2>
          <p class="head-sub">
            审核结果、收到申请、系统通知都在这里
            <span v-if="unreadTotal > 0" class="unread-hint">（{{ unreadTotal }} 条未读）</span>
          </p>
        </div>
        <div class="head-actions">
          <el-button :icon="Select" :disabled="!unreadTotal" @click="handleMarkAllRead">全部已读</el-button>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </div>

      <!-- 分类 + 搜索（新增功能） -->
      <el-card class="filter-card" shadow="never">
        <div class="filter-row">
          <el-radio-group v-model="query.msgType" @change="handleSearch">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button :value="1">审核结果</el-radio-button>
            <el-radio-button :value="2">收到申请</el-radio-button>
            <el-radio-button :value="3">系统通知</el-radio-button>
            <el-radio-button :value="4">会话消息</el-radio-button>
          </el-radio-group>

          <el-input
            v-model="query.title"
            placeholder="搜索消息标题"
            clearable
            class="filter-title"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />

          <el-checkbox v-model="onlyUnread" @change="handleSearch">只看未读</el-checkbox>
        </div>
      </el-card>

      <!-- 消息列表 -->
      <div v-loading="loading" class="notice-list">
        <template v-if="list.length">
          <div
            v-for="item in list"
            :key="item.id"
            class="notice-item"
            :class="{ unread: !item.readStatus }"
            @click="handleOpen(item)"
          >
            <div class="notice-icon" :class="`icon-type-${item.msgType}`">
              <el-icon :size="18"><component :is="typeIcon(item.msgType)" /></el-icon>
            </div>

            <div class="notice-body">
              <div class="notice-head">
                <span class="notice-title">{{ item.title }}</span>
                <el-tag size="small" effect="plain" class="notice-tag">
                  {{ noticeTypeName(item.msgType) }}
                </el-tag>
                <span v-if="!item.readStatus" class="dot"></span>
              </div>
              <p class="notice-content">{{ item.content }}</p>
              <div class="notice-foot">
                <span class="time">{{ formatTime(item.createTime) }}</span>
              </div>
            </div>

            <div class="notice-actions">
              <el-button
                v-if="!item.readStatus"
                size="small"
                text
                type="primary"
                @click.stop="handleMarkRead(item)"
              >
                标为已读
              </el-button>
              <el-button
                size="small"
                text
                type="danger"
                @click.stop="handleDelete(item)"
              >
                删除
              </el-button>
            </div>
          </div>
        </template>
        <el-empty v-else-if="!loading" description="暂时没有消息" />
      </div>

      <div class="pager-wrap">
        <el-pagination
          v-model:current-page="query.pageNumber"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 消息中心页。
 *
 * 相比原系统补上了：
 *  1. 按类型分组筛选（审核结果 / 收到申请 / 系统通知 / 会话消息）
 *  2. 标题搜索
 *  3. 只看未读
 *  4. 分类图标，一眼能看出是什么消息
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search, Select, Refresh, CircleCheckFilled, BellFilled,
  ChatDotRound, Document
} from '@element-plus/icons-vue'
import {
  getNoticeList, getUnreadCount, markNoticeRead, markAllRead, deleteNotice
} from '@/api/notice'
import { noticeTypeName } from '@/utils/roleDisplay'
import { refreshUnread } from '@/utils/unreadBus'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const unreadTotal = ref(0)
const onlyUnread = ref(false)

const query = reactive({
  pageNumber: 1,
  pageSize: 10,
  msgType: null,
  title: ''
})

async function loadList() {
  loading.value = true
  try {
    const res = await getNoticeList({
      pageNumber: query.pageNumber,
      pageSize: query.pageSize,
      msgType: query.msgType || undefined,
      title: query.title || undefined,
      readStatus: onlyUnread.value ? 0 : undefined
    })
    const data = res?.data || {}
    list.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
  loadUnread()
}

async function loadUnread() {
  try {
    const res = await getUnreadCount()
    unreadTotal.value = res?.data?.total ?? 0
  } catch (e) {
    unreadTotal.value = 0
  }
}

function handleSearch() {
  query.pageNumber = 1
  loadList()
}

async function handleMarkRead(item) {
  try {
    await markNoticeRead(item.id)
    item.readStatus = 1
    loadUnread()
    // 广播：顶部两个红点立刻刷新，不用等 60 秒轮询
    refreshUnread('notice-read')
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleMarkAllRead() {
  try {
    await markAllRead({ msgType: query.msgType || null })
    ElMessage.success('已全部标为已读')
    loadList()
    // 后端在这接口里连带把聊天未读也清了，所以本页未读数要重拉
    loadUnread()
    refreshUnread('notice-read-all')
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleDelete(item) {
  try {
    await deleteNotice(item.id)
    ElMessage.success('已删除')
    loadList()
  } catch (e) {
    // 拦截器已提示
  }
}

/** 点击消息：标已读 + 按类型跳转 */
function handleOpen(item) {
  if (!item.readStatus) {
    handleMarkRead(item)
  }
  // 会话消息跳到站内沟通
  if (item.msgType === 4) {
    router.push('/cooperation/chat')
    return
  }
  // 审核结果：待审核的内容跳我填写的，申请类的跳我的申请
  if (item.msgType === 1) {
    router.push('/cooperation/my-published')
  } else if (item.msgType === 2) {
    router.push('/cooperation/applications')
  }
}

function typeIcon(type) {
  const map = {
    1: CircleCheckFilled,
    2: Document,
    3: BellFilled,
    4: ChatDotRound
  }
  return map[type] || BellFilled
}

function formatTime(t) {
  if (!t) {
    return ''
  }
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(loadList)
</script>

<style scoped>
.notifications-page {
  padding: 24px 0;
}

.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.head-title {
  font-size: 22px;
  color: #1d3a6b;
  margin: 0 0 6px;
}

.head-sub {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.unread-hint {
  color: #e65100;
  font-weight: 500;
}

.head-actions {
  display: flex;
  gap: 8px;
}

.filter-card {
  margin-bottom: 18px;
  border-radius: 10px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-title {
  width: 240px;
}

.notice-list {
  min-height: 260px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px 18px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.notice-item:hover {
  box-shadow: 0 3px 12px rgba(29, 58, 107, 0.08);
}

.notice-item.unread {
  border-left: 3px solid #1d3a6b;
  background: #fbfcfe;
}

.notice-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}

.icon-type-1 { background: #67c23a; }
.icon-type-2 { background: #e6a23c; }
.icon-type-3 { background: #909399; }
.icon-type-4 { background: #1d3a6b; }

.notice-body {
  flex: 1;
  min-width: 0;
}

.notice-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.notice-tag {
  transform: scale(0.9);
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f56c6c;
}

.notice-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
  margin: 6px 0 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-foot {
  margin-top: 6px;
}

.time {
  font-size: 12px;
  color: #c0c4cc;
}

.notice-actions {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex-shrink: 0;
}

.pager-wrap {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}
</style>
