<template>
  <div class="applications-page">
    <div class="page-container">
      <div class="head-row">
        <div>
          <h2 class="head-title">我的申请</h2>
          <p class="head-sub">{{ headSub }}</p>
        </div>
      </div>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 我发出的 -->
        <el-tab-pane label="我发出的" name="sent">
          <div v-loading="loadingSent" class="tab-body">
            <template v-if="sentList.length">
              <div v-for="item in sentList" :key="item.id" class="item-card">
                <div class="card-top">
                  <div class="card-title-line">
                    <span class="item-card__title" @click="goDetail(item.articleId)">
                      {{ item.articleTitle || '内容已删除' }}
                    </span>
                  </div>
                  <el-tag :type="collabProgressTagType(item.collabProgress)" size="small">
                    {{ collabProgressName(item.collabProgress) }}
                  </el-tag>
                </div>

                <div class="apply-info">
                  <div class="info-row">
                    <span class="label">对方：</span>
                    <span>{{ item.publisherName || '未知' }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">我的申请原因：</span>
                    <span class="reason-text">{{ item.reason }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">联系方式：</span>
                    <span>{{ item.contactWay }}：{{ item.contactWayValue }}</span>
                  </div>
                  <div v-if="item.reviewMessage" class="info-row">
                    <span class="label">对方回复：</span>
                    <span class="reply-text">{{ item.reviewMessage }}</span>
                  </div>
                </div>

                <div class="item-card__meta">
                  <span><el-icon><Clock /></el-icon> 申请于 {{ formatTime(item.createTime) }}</span>
                </div>

                <div class="card-footer">
                  <el-button
                    v-if="item.collabProgress === 1 || item.collabProgress === 2"
                    type="primary"
                    size="small"
                    :icon="ChatDotRound"
                    @click="goChat(item)"
                  >
                    进入沟通
                  </el-button>
                  <el-button
                    v-if="item.collabProgress === 0"
                    size="small"
                    :icon="Delete"
                    @click="handleCancel(item)"
                  >
                    撤回申请
                  </el-button>
                  <el-button
                    v-if="item.collabProgress >= 1 && item.collabProgress <= 3"
                    size="small"
                    :icon="Refresh"
                    @click="openProgressDialog(item)"
                  >
                    更新进度
                  </el-button>
                </div>
              </div>
            </template>
            <el-empty v-else-if="!loadingSent" description="还没有发出过申请" />
          </div>
        </el-tab-pane>

        <!-- 我收到的 -->
        <el-tab-pane :label="receivedLabel" name="received">
          <div v-loading="loadingReceived" class="tab-body">
            <template v-if="receivedList.length">
              <div v-for="item in receivedList" :key="item.id" class="item-card">
                <div class="card-top">
                  <div class="card-title-line">
                    <span class="item-card__title" @click="goDetail(item.articleId)">
                      {{ item.articleTitle || '内容已删除' }}
                    </span>
                  </div>
                  <el-tag :type="collabProgressTagType(item.collabProgress)" size="small">
                    {{ collabProgressName(item.collabProgress) }}
                  </el-tag>
                </div>

                <div class="apply-info">
                  <div class="info-row">
                    <span class="label">申请人：</span>
                    <span>{{ item.applicantName || '未知' }}</span>
                  </div>
                  <!-- 学生申请：显示年级班级 + 擅长（组长看申请最关心的信息） -->
                  <template v-if="!isTeacherApply(item)">
                    <div class="info-row">
                      <span class="label">年级班级：</span>
                      <span class="grade-class">
                        <el-tag size="small" effect="plain">{{ item.grade || '未填' }}</el-tag>
                        <el-tag size="small" effect="plain" type="info">{{ item.className || '未填' }}</el-tag>
                      </span>
                    </div>
                    <div class="info-row">
                      <span class="label">擅长：</span>
                      <span class="skill-tags">
                        <template v-if="(item.skillList || []).length">
                          <el-tag
                            v-for="(s, si) in item.skillList"
                            :key="si"
                            size="small"
                            type="success"
                            effect="plain"
                          >
                            {{ skillName(s) }}
                          </el-tag>
                        </template>
                        <span v-else class="text-sub">未填写</span>
                      </span>
                    </div>
                  </template>
                  <!-- 老师申请：没有年级班级，标个「老师申请」让人一眼分辨 -->
                  <div v-else class="info-row">
                    <span class="label">申请类型：</span>
                    <el-tag size="small" type="warning" effect="plain">老师申请</el-tag>
                  </div>

                  <div v-if="item.reason" class="info-row">
                    <span class="label">{{ isTeacherApply(item) ? '为什么感兴趣：' : '补充说明：' }}</span>
                    <span class="reason-text">{{ item.reason }}</span>
                  </div>
                  <div v-if="item.contactWayValue" class="info-row">
                    <span class="label">联系方式：</span>
                    <span class="contact-highlight">{{ item.contactWay }}：{{ item.contactWayValue }}</span>
                  </div>
                  <div v-else class="info-row">
                    <span class="label">联系方式：</span>
                    <span class="text-sub">未留（可通过临时通道沟通）</span>
                  </div>
                </div>

                <div class="item-card__meta">
                  <span><el-icon><Clock /></el-icon> 申请于 {{ formatTime(item.createTime) }}</span>
                </div>

                <!-- 待处理：通过 / 拒绝 -->
                <div v-if="item.collabProgress === 0" class="card-footer">
                  <el-button type="success" size="small" :icon="Check" @click="handleReview(item, true)">
                    通过
                  </el-button>
                  <el-button type="danger" size="small" :icon="Close" @click="handleReview(item, false)">
                    拒绝
                  </el-button>
                </div>
                <div v-else class="card-footer">
                  <el-button
                    v-if="item.collabProgress >= 1 && item.collabProgress <= 3"
                    type="primary"
                    size="small"
                    :icon="ChatDotRound"
                    @click="goChat(item)"
                  >
                    进入沟通
                  </el-button>
                  <!-- 临时通道：申请通过后系统自动开通，有有效期，到期自动失效 -->
                  <el-button
                    v-if="item.collabProgress >= 1 && item.collabProgress <= 3"
                    size="small"
                    :type="roomState(item).active ? 'primary' : 'default'"
                    :icon="Timer"
                    @click="goTmpRoom(item)"
                  >
                    {{ roomState(item).label }}
                  </el-button>
                  <el-button
                    v-if="item.collabProgress >= 1 && item.collabProgress <= 3"
                    size="small"
                    :icon="Refresh"
                    @click="openProgressDialog(item)"
                  >
                    更新进度
                  </el-button>
                </div>

                <div v-if="item.reviewMessage" class="my-reply">
                  <span class="label">我的回复：</span>{{ item.reviewMessage }}
                </div>
              </div>
            </template>
            <el-empty v-else-if="!loadingReceived" description="还没有收到过申请" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 处理申请弹窗 -->
    <el-dialog v-model="reviewVisible" :title="reviewTitle" width="520px" destroy-on-close>
      <el-alert :title="reviewAlert" :type="reviewPass ? 'success' : 'warning'" :closable="false" class="mb-16" />
      <el-form label-position="top">
        <el-form-item :label="reviewPass ? '回复内容（选填）' : '拒绝原因'">
          <el-input
            v-model="reviewMessage"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            :placeholder="reviewPass ? '例如：你的方案很契合，我们约个时间细聊' : '说明为什么不合适，让对方心里有数'"
          />
        </el-form-item>
        <!-- 拒绝理由模板（新增） -->
        <el-form-item v-if="!reviewPass && rejectTemplates.length" label="常用理由（点一下填入）">
          <el-tag
            v-for="t in rejectTemplates"
            :key="t.code"
            class="reject-template-tag"
            effect="plain"
            @click="reviewMessage = t.content"
          >
            {{ t.content }}
          </el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button :type="reviewPass ? 'success' : 'danger'" :loading="reviewLoading" @click="submitReview">
          确定{{ reviewPass ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 更新进度弹窗 -->
    <el-dialog v-model="progressVisible" title="更新对接进度" width="480px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="当前进度">
          <el-radio-group v-model="progressForm.collabProgress">
            <el-radio-button :value="2">洽谈中</el-radio-button>
            <el-radio-button :value="3">已合作</el-radio-button>
            <el-radio-button :value="4">已结束</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注（选填）">
          <el-input v-model="progressForm.remark" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="progressVisible = false">取消</el-button>
        <el-button type="primary" :loading="progressLoading" @click="submitProgress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 我的申请页。
 * 两个标签页：我发出的、我收到的。
 *
 * 相比原系统补上了：
 *  1. 对接进度状态展示（原来只有通过/拒绝两态）
 *  2. 进度更新入口
 *  3. 「进入沟通」直达站内会话
 *  4. 拒绝理由模板一键填入
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Clock, Delete, Refresh, ChatDotRound, Check, Close, Timer
} from '@element-plus/icons-vue'
import {
  getRegistrationList, handleRegistration, cancelRegistration, updateCollabProgress
} from '@/api/blog'
import { createSession } from '@/api/chat'
import { getRoomByRegistration, getRoomByArticle, createRoom } from '@/api/tmpRoom'
import { getSkillOptions } from '@/api/meta'
import { getRejectTemplateList } from '@/api/admin'
import { collabProgressName, collabProgressTagType } from '@/utils/roleDisplay'
import { getRoleCode } from '@/utils/authSession'

const router = useRouter()

const activeTab = ref('sent')

const loadingSent = ref(false)
const loadingReceived = ref(false)
const sentList = ref([])

/**
 * registrationId -> 临时通道信息。
 *
 * 【为什么要单独存一份】
 * 之前按钮的显示条件只看 collabProgress（申请进度），完全不管通道实际状态 ——
 * 结果是通道过期了或关了，按钮还在那儿，点进去才报错；反过来通道是好的，
 * 界面上却看不出「已经开通了」。
 * 这里把通道状态拉出来，让列表直接显示「剩余 X 小时 / 已关闭 / 待开通」。
 */
const roomMap = ref({})
const receivedList = ref([])
const rejectTemplates = ref([])

const currentRoleCode = computed(() => getRoleCode())

const headSub = computed(() =>
    '记录你发出的意向申请，以及别人对你填写内容的申请'
)

const receivedLabel = computed(() => '我收到的')

// ---------- 擅长技能（预设，后端给） ----------
const skillMap = ref({})

async function loadSkills() {
  try {
    const res = await getSkillOptions()
    const list = res?.data || []
    const m = {}
    list.forEach((s) => {
      m[s.code] = s.name
    })
    skillMap.value = m
  } catch (e) {
    // 拿不到就退回显示 code，不影响主流程
  }
}

/**
 * 这条申请是不是老师发的。
 *
 * 判断依据：老师申请不填年级班级和擅长（那是学生的口径），
 * 所以这三项都空的就是老师申请。
 * 不用后端加字段 —— 数据本身就能区分，加字段反而要改 VO 和回填逻辑。
 */
function isTeacherApply(item) {
  return !item.grade && !item.className && !(item.skillList || []).length
}

/**
 * code → 中文名。
 * 后端存的是 code（如 backend），显示要中文（后端开发）。
 * 拿不到映射就原样显示，至少不是空白。
 */
function skillName(code) {
  if (!code) {
    return ''
  }
  return skillMap.value[code] || code
}

// ---------- 临时沟通通道 ----------
/**
 * 进临时通道。
 *
 * 通道在申请通过时已由后端自动开好，这里先查有没有有效的：
 *   有 → 直接跳 /tmp-room/{token}
 *   没有（过期了）→ 问用户要不要续开，重开后再跳
 */
/**
 * 一条申请的临时通道状态，直接给按钮用。
 *
 *   已开通（未过期） -> 按钮显示「进入 · 剩 X 小时」，蓝色可点
 *   已关闭 / 已过期   -> 按钮显示「重新开通」，默认色
 *   还没开通         -> 按钮显示「开通临时通道」
 *
 * 这样用户不点进去也知道通道是什么情况 —— 之前按钮只写「临时通道」三个字，
 * 有没有开通、过没过期完全看不出来。
 */
function roomState(item) {
  const room = roomMap.value[item.id]
  if (room && room.roomToken && !room.expired) {
    const h = Number(room.remainingHours || 0)
    return {
      active: true,
      label: h > 0 ? `进入 · 剩 ${h} 小时` : '进入临时通道',
      room
    }
  }
  if (room && (room.expired || room.status === 0)) {
    return { active: false, label: '重新开通通道', room: null }
  }
  return { active: false, label: '开通临时通道', room: null }
}

async function goTmpRoom(item) {
  try {
    let room = null
    const res = await getRoomByRegistration(item.id)
    room = res?.data
    if (!room?.roomToken) {
      // 按项目再找一次（老数据可能没关联 registrationId）
      const res2 = await getRoomByArticle(item.articleId)
      room = res2?.data
    }
    if (!room?.roomToken) {
      // 确实没有有效通道 → 重开一个
      const created = await createRoom({ registrationId: item.id, articleId: item.articleId })
      room = created?.data
    }
    if (room?.roomToken) {
      // 记下来，回来时按钮状态也能跟上
      roomMap.value = { ...roomMap.value, [item.id]: room }
      window.open(`/tmp-room/${room.roomToken}`, '_blank')
    } else {
      ElMessage.warning('通道开通失败，请稍后重试')
    }
  } catch (e) {
    // 拦截器已提示
  }
}

// ---------- 处理申请 ----------
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewPass = ref(true)
const reviewMessage = ref('')
const reviewTarget = ref(null)

const reviewTitle = computed(() => (reviewPass.value ? '通过申请' : '拒绝申请'))
const reviewAlert = computed(() =>
    reviewPass.value
        ? '通过后系统会自动为你们开通站内沟通，可以直接在平台上聊，不用再加微信。'
        : '请说明拒绝原因，方便对方了解情况。'
)

// ---------- 更新进度 ----------
const progressVisible = ref(false)
const progressLoading = ref(false)
const progressForm = reactive({
  id: null,
  collabProgress: 2,
  remark: ''
})

// ---------- 加载 ----------

async function loadSent() {
  loadingSent.value = true
  try {
    const res = await getRegistrationList({
      direction: 'sent',
      pageNumber: 1,
      pageSize: 50
    })
    sentList.value = res?.data?.records || res?.data || []
    await loadRoomStates(sentList.value)
  } catch (e) {
    sentList.value = []
  } finally {
    loadingSent.value = false
  }
}

async function loadReceived() {
  loadingReceived.value = true
  try {
    const res = await getRegistrationList({
      direction: 'received',
      pageNumber: 1,
      pageSize: 50
    })
    receivedList.value = res?.data?.records || res?.data || []
    await loadRoomStates(receivedList.value)
  } catch (e) {
    receivedList.value = []
  } finally {
    loadingReceived.value = false
  }
}

/**
 * 批量查各申请的临时通道状态。
 * 只查「已通过」的申请 —— 没通过的本来也不该有通道。
 * 单条失败不影响其它条，某条查不到就当「未开通」。
 */
async function loadRoomStates(list) {
  const map = { ...roomMap.value }
  const targets = (list || []).filter(
    (it) => it.collabProgress >= 1 && it.collabProgress <= 3
  )
  await Promise.all(
    targets.map(async (it) => {
      try {
        const res = await getRoomByRegistration(it.id)
        map[it.id] = res?.data || null
      } catch (e) {
        map[it.id] = null
      }
    })
  )
  roomMap.value = map
}

async function loadRejectTemplates() {
  try {
    const res = await getRejectTemplateList({ scene: 'article' })
    rejectTemplates.value = res?.data || []
  } catch (e) {
    rejectTemplates.value = []
  }
}

function handleTabChange(name) {
  if (name === 'sent') {
    loadSent()
  } else {
    loadReceived()
  }
}

// ---------- 操作 ----------

function handleReview(item, pass) {
  reviewTarget.value = item
  reviewPass.value = pass
  reviewMessage.value = ''
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewPass.value && !reviewMessage.value.trim()) {
    ElMessage.warning('拒绝时请填写原因')
    return
  }
  reviewLoading.value = true
  try {
    await handleRegistration({
      id: reviewTarget.value.id,
      pass: reviewPass.value ? 1 : 0,
      reviewMessage: reviewMessage.value
    })
    ElMessage.success(reviewPass.value ? '已通过，可以开始沟通了' : '已拒绝')
    reviewVisible.value = false
    loadReceived()
  } catch (e) {
    // 拦截器已提示
  } finally {
    reviewLoading.value = false
  }
}

async function handleCancel(item) {
  try {
    await cancelRegistration(item.id)
    ElMessage.success('已撤回')
    loadSent()
  } catch (e) {
    // 拦截器已提示
  }
}

async function goChat(item) {
  try {
    const res = await createSession({
      registrationId: item.id,
      articleId: item.articleId
    })
    const sessionId = res?.data?.id ?? res?.data
    router.push({ path: '/cooperation/chat', query: { sessionId } })
  } catch (e) {
    // 拦截器已提示
  }
}

function openProgressDialog(item) {
  progressForm.id = item.id
  progressForm.collabProgress = Math.min(Math.max(item.collabProgress + 1, 2), 4)
  progressForm.remark = ''
  progressVisible.value = true
}

async function submitProgress() {
  progressLoading.value = true
  try {
    await updateCollabProgress({
      id: progressForm.id,
      collabProgress: progressForm.collabProgress,
      remark: progressForm.remark
    })
    ElMessage.success('进度已更新')
    progressVisible.value = false
    loadSent()
    loadReceived()
  } catch (e) {
    // 拦截器已提示
  } finally {
    progressLoading.value = false
  }
}

function goDetail(articleId) {
  if (articleId) {
    router.push(`/cooperation/detail/${articleId}`)
  }
}

function formatTime(t) {
  if (!t) {
    return ''
  }
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  loadSkills()
  loadSent()
  loadRejectTemplates()
})
</script>

<style scoped>
.applications-page {
  padding: 24px 0;
}

.head-row {
  margin-bottom: 10px;
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

.tab-body {
  min-height: 260px;
  padding-top: 8px;
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.card-title-line {
  flex: 1;
  min-width: 0;
}

.card-title-line .item-card__title {
  margin-bottom: 0;
  cursor: pointer;
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-title-line .item-card__title:hover {
  color: #1d3a6b;
  text-decoration: underline;
}

.apply-info {
  margin-top: 10px;
  background: #f8f9fb;
  border-radius: 6px;
  padding: 12px 14px;
}

.info-row {
  font-size: 13px;
  line-height: 1.9;
  color: #606266;
}

.info-row .label {
  color: #909399;
  margin-right: 4px;
}

.grade-class {
  display: inline-flex;
  gap: 6px;
}

.skill-tags {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
}

.text-sub {
  color: #a0a4ab;
  font-size: 12px;
}

.reason-text {
  color: #303133;
}

.reply-text {
  color: #1d3a6b;
}

.contact-highlight {
  color: #e65100;
  font-weight: 500;
}

.item-card__meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-footer {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #e4e7ed;
  display: flex;
  gap: 8px;
}

.my-reply {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
  background: #f0f9eb;
  border-radius: 6px;
  padding: 10px 12px;
}

.my-reply .label {
  color: #909399;
}

.reject-template-tag {
  margin: 0 8px 8px 0;
  cursor: pointer;
}

.reject-template-tag:hover {
  background: #ecf5ff;
}
</style>
