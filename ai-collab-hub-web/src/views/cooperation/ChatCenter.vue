<template>
  <div class="chat-page">
    <div class="page-container">
      <h2 class="head-title">站内沟通</h2>
      <p class="head-sub">对接通过后，可以在这里直接聊天，不用再互相加微信</p>

      <div v-loading="loadingSession" class="chat-box">
        <!-- 左侧会话列表 -->
        <aside class="session-side">
          <div class="side-head">
            <span>会话列表</span>
            <el-badge v-if="totalUnread > 0" :value="totalUnread" :max="99" />
          </div>

          <div class="session-list">
            <template v-if="sessionList.length">
              <div
                v-for="s in sessionList"
                :key="s.id"
                class="session-item"
                :class="{ active: String(currentSession?.id) === String(s.id) }"
                @click="openSession(s)"
              >
                <el-avatar :size="40" :src="s.targetAvatar">
                  {{ (s.targetUserName || 'U').charAt(0) }}
                </el-avatar>
                <div class="session-info">
                  <div class="session-top">
                    <span class="session-name">{{ s.targetUserName || '对方' }}</span>
                    <span class="session-time">{{ shortTime(s.lastMsgTime) }}</span>
                  </div>
                  <div class="session-preview">
                    {{ s.lastMessage || '暂无消息，打个招呼吧' }}
                  </div>
                </div>
                <el-badge
                  v-if="s.unreadCount > 0"
                  :value="s.unreadCount"
                  :max="99"
                  class="session-badge"
                />
              </div>
            </template>
            <el-empty v-else-if="!loadingSession" description="还没有会话" :image-size="80">
              <div class="empty-tip">
                当你的对接申请被通过后，这里会自动出现会话
              </div>
            </el-empty>
          </div>
        </aside>

        <!-- 右侧聊天区 -->
        <section class="msg-area">
          <template v-if="currentSession">
            <div class="msg-head">
              <div class="msg-head-left">
                <el-avatar :size="34" :src="currentSession.targetAvatar">
                  {{ (currentSession.targetUserName || 'U').charAt(0) }}
                </el-avatar>
                <div>
                  <div class="msg-name">{{ currentSession.targetUserName || '对方' }}</div>
                  <div class="msg-article">
                    关于「{{ currentSession.articleTitle || '合作内容' }}」
                  </div>
                </div>
              </div>
              <el-tag size="small" type="success" effect="plain">对接已建立</el-tag>
            </div>

            <div ref="msgScrollRef" v-loading="loadingMsg" class="msg-list">
              <template v-if="messageList.length">
                <div
                  v-for="m in messageList"
                  :key="m.id"
                  class="msg-row"
                  :class="{ mine: isMine(m) }"
                >
                  <!-- 对方的消息：[对方头像][气泡]，靠左 -->
                  <el-avatar v-if="!isMine(m)" :size="32" :src="m.senderAvatar">
                    {{ (m.senderName || 'U').charAt(0) }}
                  </el-avatar>
                  <div class="msg-bubble">
                    <div class="bubble-text">{{ m.content }}</div>
                    <div class="bubble-time">{{ shortTime(m.createTime) }}</div>
                  </div>
                  <!-- 自己的消息：[气泡][自己头像]，靠右（头像在文字右侧） -->
                  <el-avatar v-if="isMine(m)" :size="32" :src="myAvatar">
                    {{ (myName || '我').charAt(0) }}
                  </el-avatar>
                </div>
              </template>
              <el-empty v-else-if="!loadingMsg" description="还没有消息，说点什么吧" :image-size="70" />
            </div>

            <div class="msg-input">
              <el-input
                v-model="msgContent"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                resize="none"
                placeholder="输入消息，按 Ctrl + Enter 发送"
                @keydown.ctrl.enter.prevent="handleSend"
              />
              <div class="input-actions">
                <span class="tip">对接已建立，可直接沟通合作细节</span>
                <el-button
                  type="primary"
                  :icon="Promotion"
                  :loading="sending"
                  :disabled="!msgContent.trim()"
                  @click="handleSend"
                >
                  发送
                </el-button>
              </div>
            </div>
          </template>

          <div v-else class="no-session">
            <el-empty description="请从左侧选择一个会话开始沟通" />
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 站内沟通页（新增功能）。
 *
 * 原来平台最大的断点：申请通过后就断了，双方只能靠申请时留的微信线下联系。
 * 这里补上站内会话，对接通过后自动开通，直接在平台上聊。
 */
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import { getSessionList, getChatMessages, sendChatMessage, markSessionRead } from '@/api/chat'
import { getUserInfo } from '@/utils/authSession'
import { refreshUnread } from '@/utils/unreadBus'

const route = useRoute()

const loadingSession = ref(false)
const loadingMsg = ref(false)
const sending = ref(false)

const sessionList = ref([])
const messageList = ref([])
const currentSession = ref(null)
const msgContent = ref('')
const msgScrollRef = ref(null)

const myInfo = getUserInfo()
const myName = computed(() => myInfo?.nickname || '我')
const myAvatar = computed(() => myInfo?.avatar || '')
const myUserId = computed(() => myInfo?.id ?? myInfo?.userId)

const totalUnread = computed(() =>
    sessionList.value.reduce((sum, s) => sum + (s.unreadCount || 0), 0)
)

async function loadSessions() {
  loadingSession.value = true
  try {
    const res = await getSessionList()
    sessionList.value = res?.data || []

    // 从 URL 带 sessionId 进来时直接打开对应会话
    const targetId = route.query.sessionId
    if (targetId) {
      const found = sessionList.value.find((s) => String(s.id) === String(targetId))
      if (found) {
        openSession(found)
        return
      }
    }
    // 否则默认打开第一个
    if (sessionList.value.length && !currentSession.value) {
      openSession(sessionList.value[0])
    }
  } catch (e) {
    sessionList.value = []
  } finally {
    loadingSession.value = false
  }
}

async function openSession(session) {
  currentSession.value = session
  await Promise.all([loadMessages(session.id), markRead(session.id)])
}

async function loadMessages(sessionId) {
  loadingMsg.value = true
  try {
    const res = await getChatMessages({
      sessionId,
      pageNum: 1,
      pageSize: 100
    })
    const data = res?.data || {}
    // 后端按时间倒序给，前端翻过来正序展示
    const records = data.records || []
    messageList.value = [...records].reverse()
    scrollToBottom()
  } catch (e) {
    messageList.value = []
  } finally {
    loadingMsg.value = false
  }
}

async function markRead(sessionId) {
  try {
    await markSessionRead(sessionId)
    const s = sessionList.value.find((it) => String(it.id) === String(sessionId))
    if (s) {
      s.unreadCount = 0
    }
    // 后端已经把消息中心里「关于这个会话」的通知也标已读了，
    // 广播一下让顶部两个红点立刻刷新（不用等 60 秒轮询）
    refreshUnread('chat-read')
  } catch (e) {
    // 静默
  }
}

async function handleSend() {
  if (!msgContent.value.trim() || !currentSession.value) {
    return
  }
  sending.value = true
  try {
    const res = await sendChatMessage({
      sessionId: currentSession.value.id,
      content: msgContent.value.trim(),
      msgKind: 1
    })
    const saved = res?.data
    if (saved) {
      messageList.value.push(saved)
    }
    msgContent.value = ''
    scrollToBottom()
    // 刷新会话列表里的最后一条消息
    const s = sessionList.value.find((it) => String(it.id) === String(currentSession.value.id))
    if (s) {
      s.lastMessage = saved?.content || '新消息'
      s.lastMsgTime = saved?.createTime || new Date().toISOString()
    }
  } catch (e) {
    // 拦截器已提示
  } finally {
    sending.value = false
  }
}

function isMine(m) {
  return String(m.senderId) === String(myUserId.value)
}

function scrollToBottom() {
  nextTick(() => {
    const el = msgScrollRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function shortTime(t) {
  if (!t) {
    return ''
  }
  const s = String(t).replace('T', ' ')
  return s.length > 16 ? s.slice(5, 16) : s
}

let timer = null

onMounted(() => {
  loadSessions()
  // 15 秒轮询一次新消息（想要更实时可以换成 WebSocket，见解释文档）
  timer = setInterval(() => {
    if (currentSession.value) {
      loadMessages(currentSession.value.id)
    }
    loadSessions()
  }, 15000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.chat-page {
  padding: 24px 0;
}

.head-title {
  font-size: 22px;
  color: #1d3a6b;
  margin: 0 0 6px;
}

.head-sub {
  font-size: 13px;
  color: #909399;
  margin: 0 0 18px;
}

.chat-box {
  display: flex;
  height: 620px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  overflow: hidden;
}

/* ---------- 左侧会话列表 ---------- */
.session-side {
  width: 300px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.side-head {
  padding: 16px 18px;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
  font-weight: 600;
  color: #1d3a6b;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f7fa;
  position: relative;
  transition: background 0.15s;
}

.session-item:hover {
  background: #f8f9fb;
}

.session-item.active {
  background: #eef2f9;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.session-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.session-time {
  font-size: 11px;
  color: #c0c4cc;
}

.session-preview {
  font-size: 12px;
  color: #909399;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-badge {
  position: absolute;
  right: 12px;
  bottom: 14px;
}

.empty-tip {
  font-size: 12px;
  color: #c0c4cc;
  line-height: 1.7;
}

/* ---------- 右侧聊天区 ---------- */
.msg-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.msg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #fbfcfe;
}

.msg-head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.msg-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.msg-article {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f8f9fb;
}

/*
 * 消息行布局。
 *
 * 【为什么要改】
 * 原来「自己的消息」用 flex-direction: row-reverse 把整行反转。
 * DOM 顺序是 [对方头像][气泡][自己头像]，反转后变成 [自己头像][气泡][对方头像] ——
 * 结果自己的头像跑到了最左边，右边反而空着，跟微信/QQ 的习惯反了。
 *
 * 改成用 justify-content 控制左右：DOM 顺序不动，靠 flex-end 把「自己的消息」推到右边。
 * 这样对方消息是 [头像][气泡]，自己的消息是 [气泡][头像]，两头都贴边，中间留白。
 */
.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
  justify-content: flex-start;
}

.msg-row.mine {
  justify-content: flex-end;
}

.msg-bubble {
  max-width: 60%;
  background: #fff;
  border-radius: 10px;
  padding: 10px 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.msg-row.mine .msg-bubble {
  background: #1d3a6b;
}

.bubble-text {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  word-break: break-word;
  white-space: pre-wrap;
}

.msg-row.mine .bubble-text {
  color: #fff;
}

.bubble-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 5px;
  text-align: right;
}

.msg-row.mine .bubble-time {
  color: rgba(255, 255, 255, 0.6);
}

.msg-input {
  border-top: 1px solid #e4e7ed;
  padding: 14px 18px;
  background: #fff;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.tip {
  font-size: 12px;
  color: #c0c4cc;
}

.no-session {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 900px) {
  .session-side {
    width: 200px;
  }
}
</style>
