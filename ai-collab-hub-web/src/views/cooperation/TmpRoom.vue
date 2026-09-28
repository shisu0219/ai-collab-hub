<template>
  <div class="tmp-room-page">
    <div class="page-container">
      <!-- 通道状态条 -->
      <div v-if="room" class="room-bar" :class="{ 'room-bar--expired': room.expired }">
        <div class="bar-left">
          <el-icon class="bar-icon"><Timer /></el-icon>
          <div>
            <div class="bar-title">{{ room.title || '临时沟通通道' }}</div>
            <div class="bar-sub">
              <template v-if="room.expired">
                该通道已失效
              </template>
              <template v-else>
                临时通道 · 剩余约 {{ room.remainingHours }} 小时
              </template>
            </div>
          </div>
        </div>
        <div class="bar-right">
          <span v-if="room.userAName && room.userBName" class="bar-users">
            {{ room.userAName }} ↔ {{ room.userBName }}
          </span>
          <el-button v-if="!room.expired" size="small" @click="handleExtend">续期</el-button>
          <el-button v-if="!room.expired" size="small" @click="handleCopy">复制链接</el-button>
        </div>
      </div>

      <!-- 加载中 / 打开失败 -->
      <el-card v-if="loading" shadow="never" class="state-card">
        <div class="state-text">正在进入通道…</div>
      </el-card>
      <el-card v-else-if="errorMsg" shadow="never" class="state-card">
        <el-result icon="warning" title="无法进入该通道" :sub-title="errorMsg">
          <template #extra>
            <el-button type="primary" @click="goHome">返回首页</el-button>
          </template>
        </el-result>
      </el-card>

      <template v-else-if="room">
        <!-- 已失效提示 -->
        <el-alert
          v-if="room.expired"
          class="expired-alert"
          type="warning"
          :closable="false"
          title="这个临时通道已经失效"
        >
          <div class="expired-body">
            临时通道是给双方先沟通用的，到期会自动关闭。
            如需继续，请回到「我的申请」里重新开通。
          </div>
        </el-alert>

        <!-- 聊天区 -->
        <el-card shadow="never" class="chat-card">
          <div ref="listRef" v-loading="msgLoading" class="msg-list">
            <template v-if="messages.length">
              <div
                v-for="m in messages"
                :key="m.id"
                class="msg-row"
                :class="{ 'msg-row--mine': m.mine }"
              >
                <!-- 对方的头像：靠左 -->
                <el-avatar v-if="!m.mine" :size="34" class="msg-avatar">
                  {{ (m.senderName || '对方').charAt(0) }}
                </el-avatar>
                <div class="msg-bubble">
                  <div class="msg-head">
                    <span class="msg-name">{{ m.mine ? '我' : (m.senderName || '对方') }}</span>
                    <span class="msg-time">{{ formatTime(m.createTime) }}</span>
                  </div>
                  <div class="msg-content">{{ m.content }}</div>
                </div>
                <!-- 自己的头像：靠右（在文字右侧） -->
                <el-avatar v-if="m.mine" :size="34" class="msg-avatar msg-avatar--mine">
                  {{ (myName || '我').charAt(0) }}
                </el-avatar>
              </div>
            </template>
            <el-empty v-else-if="!msgLoading" description="还没有消息，打个招呼吧" />
          </div>

          <!-- 发言区 -->
          <div class="send-area">
            <template v-if="canSpeak">
              <el-input
                v-model="draft"
                type="textarea"
                :rows="3"
                maxlength="1000"
                show-word-limit
                placeholder="说点什么…（Ctrl + Enter 发送）"
                @keydown.ctrl.enter="handleSend"
              />
              <div class="send-actions">
                <span class="send-tip">
                  这是临时通道，到期自动关闭；正式合作请用「站内沟通」
                </span>
                <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
              </div>
            </template>

            <el-alert
              v-else
              type="info"
              :closable="false"
              :title="speakBlockReason"
            />
          </div>
        </el-card>
      </template>
    </div>
  </div>
</template>

<script setup>
/**
 * 临时沟通通道页。
 *
 * 访问路径：/tmp-room/{token}
 *
 * 【和「站内沟通」的区别（页面文案里也要让用户看得懂）】
 *   站内沟通  长期会话，登录后在我的会话列表里，能一直聊
 *   本页面    临时通道，凭链接进入、有有效期、到期自动失效
 *
 * 【免登录的设计】
 * 「凭 token 查看房间和消息」是免登录的（像共享文档那样，有链接就能看），
 * 所以这个页面不放在 requiresAuth 的路由下面。
 * 但发言必须登录 —— 未登录点发送会被引导去登录页。
 */
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Timer } from '@element-plus/icons-vue'
import { enterRoom, getRoomMessages, sendRoomMessage, extendRoom } from '@/api/tmpRoom'
import { getToken, getUserInfo } from '@/utils/authSession'

const route = useRoute()
const router = useRouter()

const token = computed(() => String(route.params.token || ''))

const loading = ref(true)
const msgLoading = ref(false)
const sending = ref(false)
const errorMsg = ref('')
const room = ref(null)
const messages = ref([])
const draft = ref('')
const listRef = ref(null)

let timer = null

/**
 * 自己的昵称，用来显示自己那条消息的头像。
 * 未登录时拿不到，返回空串，模板里会退回显示「我」。
 */
const myName = computed(() => {
  const u = getUserInfo()
  return u?.nickname || u?.account || ''
})

/** 登录了才可能发言（是否参与人由后端再判一次） */
const canSpeak = computed(() => !!getToken() && room.value && !room.value.expired)

const speakBlockReason = computed(() => {
  if (!room.value) {
    return ''
  }
  if (room.value.expired) {
    return '通道已失效，无法发言'
  }
  if (!getToken()) {
    return '登录后才能发言（有链接即可查看消息）'
  }
  return ''
})

async function loadRoom() {
  if (!token.value) {
    errorMsg.value = '链接不完整'
    loading.value = false
    return
  }
  try {
    const res = await enterRoom(token.value)
    room.value = res?.data || null
    if (room.value) {
      await loadMessages()
      // 通道有效就轮询，让双方能看到对方说的话
      if (!room.value.expired) {
        timer = setInterval(loadMessages, 10000)
      }
    }
  } catch (e) {
    errorMsg.value = e?.response?.data?.msg || e?.message || '通道不存在或已失效'
  } finally {
    loading.value = false
  }
}

async function loadMessages() {
  msgLoading.value = true
  try {
    const res = await getRoomMessages({ token: token.value, pageNum: 1, pageSize: 200 })
    const data = res?.data || {}
    messages.value = data.records || []
    await scrollToBottom()
  } catch (e) {
    // 通道失效时这里会报错，不用反复提示 —— 上面的状态条已经在说了
  } finally {
    msgLoading.value = false
  }
}

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

async function handleSend() {
  if (!getToken()) {
    ElMessage.warning('请先登录再发言')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!draft.value.trim()) {
    ElMessage.warning('消息不能为空')
    return
  }
  if (!room.value || !room.value.id) {
    return
  }
  sending.value = true
  try {
    await sendRoomMessage({ roomId: room.value.id, content: draft.value.trim() })
    draft.value = ''
    await loadMessages()
  } catch (e) {
    // 拦截器已提示
  } finally {
    sending.value = false
  }
}

async function handleExtend() {
  if (!getToken()) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await extendRoom(room.value.id, 72)
    room.value = res?.data || room.value
    ElMessage.success('已续期')
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleCopy() {
  const url = window.location.href
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('链接已复制，发给对方即可')
  } catch (e) {
    // 有些浏览器不允许自动复制，给用户看到链接让他自己复制
    ElMessage.info(url)
  }
}

function goHome() {
  router.push('/')
}

function formatTime(t) {
  if (!t) {
    return ''
  }
  return String(t).replace('T', ' ').slice(5, 16)
}

onMounted(loadRoom)
onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.tmp-room-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px 0;
}

.page-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 0 16px;
}

.room-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #1d3a6b, #2c5aa0);
  color: #fff;
  border-radius: 10px;
  margin-bottom: 16px;
}

.room-bar--expired {
  background: linear-gradient(135deg, #8a8f99, #a8adb5);
}

.bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-icon {
  font-size: 24px;
}

.bar-title {
  font-size: 15px;
  font-weight: 500;
}

.bar-sub {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 2px;
}

.bar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.bar-users {
  font-size: 12px;
  opacity: 0.9;
}

.state-card {
  border-radius: 10px;
}

.state-text {
  text-align: center;
  color: #909399;
  padding: 40px 0;
}

.expired-alert {
  margin-bottom: 16px;
}

.expired-body {
  font-size: 13px;
  line-height: 1.6;
  margin-top: 4px;
}

.chat-card {
  border-radius: 10px;
}

.msg-list {
  height: 460px;
  overflow-y: auto;
  padding: 8px 4px;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
}

/* 自己的消息整行靠右 —— 头像因此在气泡右侧 */
.msg-row--mine {
  justify-content: flex-end;
}

.msg-avatar {
  flex-shrink: 0;
  background: #1d3a6b;
  color: #fff;
  font-size: 14px;
}

.msg-avatar--mine {
  background: #409eff;
}

.msg-bubble {
  max-width: 70%;
  padding: 10px 14px;
  background: #f0f2f5;
  border-radius: 10px;
}

.msg-row--mine .msg-bubble {
  background: #e6f0ff;
}

.msg-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.msg-name {
  font-size: 12px;
  color: #1d3a6b;
  font-weight: 500;
}

.msg-time {
  font-size: 11px;
  color: #a0a4ab;
}

.msg-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.send-area {
  border-top: 1px solid #ebeef5;
  padding-top: 14px;
  margin-top: 8px;
}

.send-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.send-tip {
  font-size: 12px;
  color: #a0a4ab;
}
</style>
