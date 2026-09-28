<template>
  <div class="detail-page">
    <div class="page-container">
      <div class="back-row">
        <el-button :icon="ArrowLeft" @click="goBack">返回列表</el-button>
      </div>

      <div v-loading="loading" class="detail-wrap">
        <template v-if="detail">
          <!-- 主信息卡片 -->
          <el-card class="main-card" shadow="never">
            <div class="title-row">
              <div class="title-left">
                <el-tag :type="typeTagType(detail.typeId)" effect="plain">
                  {{ detail.typeName || '内容' }}
                </el-tag>
                <h1 class="detail-title">{{ detail.title }}</h1>
              </div>
              <div class="title-right">
                <el-button
                  :type="favorited ? 'warning' : 'default'"
                  :icon="favorited ? StarFilled : Star"
                  @click="handleFavorite"
                >
                  {{ favorited ? '已收藏' : '收藏' }}
                </el-button>
                <el-button
                  v-if="canApply"
                  type="primary"
                  @click="applyVisible = true"
                >
                  {{ applyText }}
                </el-button>
              </div>
            </div>

            <div class="meta-row">
              <span><el-icon><Location /></el-icon> {{ detail.location || '地点待定' }}</span>
              <span><el-icon><CollectionTag /></el-icon> {{ detail.tagName || '无标签' }}</span>
              <span><el-icon><Clock /></el-icon> 填写于 {{ formatTime(detail.createTime) }}</span>
              <span><el-icon><View /></el-icon> {{ detail.viewCount || 0 }} 次浏览</span>
            </div>

            <el-divider />

            <!-- 描述 -->
            <div class="section">
              <h3 class="section-title">详细说明</h3>
              <div class="content-text">{{ detail.content || '暂无描述' }}</div>
            </div>

            <!-- 项目 / 需求 专属信息 -->
            <div v-if="detail.typeId === 1" class="section">
              <h3 class="section-title">项目信息</h3>
              <div class="info-grid">
                <div class="info-item">
                  <span class="label">项目预算</span>
                  <span class="value">{{ detail.budget || '面议' }}</span>
                </div>
                <div class="info-item">
                  <span class="label">开始日期</span>
                  <span class="value">{{ detail.startDate || '待定' }}</span>
                </div>
                <div class="info-item">
                  <span class="label">结束日期</span>
                  <span class="value">{{ detail.endDate || '待定' }}</span>
                </div>
              </div>
            </div>

            <div v-if="detail.typeId === 2" class="section">
              <h3 class="section-title">需求信息</h3>
              <div class="info-grid">
                <div class="info-item">
                  <span class="label">需求预算</span>
                  <span class="value">{{ detail.budget || '面议' }}</span>
                </div>
                <div class="info-item">
                  <span class="label">紧急程度</span>
                  <span class="value">
                    <el-tag
                      size="small"
                      :type="urgencyType(detail.urgencyLevel)"
                      effect="plain"
                    >
                      {{ detail.urgencyLevel || '普通' }}
                    </el-tag>
                  </span>
                </div>
                <div class="info-item">
                  <span class="label">期望完成日期</span>
                  <span class="value">{{ detail.expectedDeadline || '待定' }}</span>
                </div>
              </div>
            </div>

            <!-- 附件 -->
            <div v-if="attachmentList.length" class="section">
              <h3 class="section-title">附件（{{ attachmentList.length }}）</h3>
              <div class="attach-list">
                <div
                  v-for="(f, idx) in attachmentList"
                  :key="idx"
                  class="attach-item"
                  @click="handleDownload(f)"
                >
                  <el-icon><Document /></el-icon>
                  <span class="attach-name">{{ fileName(f) }}</span>
                  <el-icon class="attach-dl"><Download /></el-icon>
                </div>
              </div>
            </div>
          </el-card>

          <!-- 组长卡片 -->
          <el-card class="side-card" shadow="never">
            <h3 class="section-title">组长</h3>
            <div class="publisher">
              <el-avatar :size="52" :src="detail.publisherAvatar">
                {{ (detail.publisherName || 'U').charAt(0) }}
              </el-avatar>
              <div class="pub-info">
                <div class="pub-name">{{ detail.publisherName || '未知组长' }}</div>
                <el-tag size="small" :type="publisherRoleType" effect="plain">
                  {{ publisherRoleName }}
                </el-tag>
              </div>
            </div>
            <el-button
              class="view-home-btn"
              :icon="User"
              @click="goUserHome(detail.userId)"
            >
              查看 TA 的主页
            </el-button>

            <el-divider />

            <div class="stat-row">
              <div class="stat-item">
                <div class="stat-num">{{ detail.registrationCount || 0 }}</div>
                <div class="stat-label">已收到申请</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ detail.favoriteCount || 0 }}</div>
                <div class="stat-label">被收藏</div>
              </div>
            </div>

            <el-alert
              v-if="isMyOwn"
              title="这是你自己填写的内容，不能对自己的内容发起对接。"
              type="info"
              :closable="false"
              class="own-tip"
            />
          </el-card>

          <!-- 所属小组 -->
          <el-card v-if="group" class="group-block" shadow="never">
            <div class="group-block-head">
              <h3 class="section-title">所属小组</h3>
            </div>
            <div class="group-block-body">
              <el-tag type="success" effect="plain">{{ group.name }}</el-tag>
              <span class="group-leader">组长：{{ group.leaderName || '—' }}</span>
              <span class="group-members">成员 {{ (group.members || []).length }} 人</span>
            </div>
            <div class="group-member-list">
              <el-tag
                v-for="m in (group.members || [])"
                :key="m.id"
                size="small"
                effect="plain"
                :type="m.memberRole === 1 ? 'danger' : (m.memberRole === 3 ? 'warning' : 'primary')"
              >
                {{ m.nickName || m.account }} · {{ m.memberRoleName }}
              </el-tag>
            </div>
          </el-card>

          <!-- 答辩意见：管理员上传的答辩问题与意见 -->
          <OpinionList
            v-if="group"
            class="opinion-block"
            :group-id="group.id"
            :is-leader="!!group.isLeader"
            :default-scope="group.opinionScope"
            :is-member="!!group.isMember"
          />
        </template>

        <el-empty v-else-if="!loading" description="内容不存在或已被删除" />
      </div>
    </div>

    <!-- 申请弹窗 -->

    <el-dialog v-model="applyVisible" :title="applyText" width="560px" destroy-on-close>
      <el-alert :title="applyAlert" type="info" :closable="false" class="mb-16" />
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-position="top">
        <!-- 学生：年级 / 班级 / 擅长 -->
        <template v-if="!isTeacher">
          <el-form-item label="年级" prop="grade">
            <el-select v-model="applyForm.grade" placeholder="选择你的年级" style="width: 100%">
              <el-option v-for="g in gradeOptions" :key="g" :label="g" :value="g" />
            </el-select>
          </el-form-item>
          <el-form-item label="班级" prop="className">
            <el-select v-model="applyForm.className" placeholder="选择你的班级" style="width: 100%">
              <el-option v-for="c in classOptions" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="擅长什么部分" prop="skills">
            <el-select
              v-model="applyForm.skills"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="可多选，让人一眼看出你能干什么"
              style="width: 100%"
            >
              <el-option
                v-for="s in skillOptions"
                :key="s.code"
                :label="s.name"
                :value="s.code"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="补充说明（选填）">
            <el-input
              v-model="applyForm.reason"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="还想补充什么？比如具体做过什么、希望怎么参与"
            />
          </el-form-item>
        </template>

        <!-- 老师：只填为什么感兴趣 + 联系方式 -->
        <template v-else>
          <el-form-item label="为什么感兴趣" prop="reason">
            <el-input
              v-model="applyForm.reason"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="说说你对这个项目的看法、能提供什么指导或资源"
            />
          </el-form-item>
        </template>

        <el-form-item :label="isTeacher ? '联系方式' : '联系方式（选填）'" prop="contactWayValue">
          <div class="contact-row">
            <el-select v-model="applyForm.contactWay" placeholder="方式" clearable class="contact-type">
              <el-option label="微信" value="微信" />
              <el-option label="QQ" value="QQ" />
              <el-option label="电话" value="电话" />
              <el-option label="邮箱" value="邮箱" />
            </el-select>
            <el-input v-model="applyForm.contactWayValue" placeholder="留了通过后方便联系" class="contact-value" />
          </div>
          <div class="contact-tip">
            {{ isTeacher
              ? '申请通过后系统还会自动开一条临时沟通通道，双方可以先进去聊'
              : '不填也行 —— 申请通过后系统会开一条临时沟通通道，双方可以先进去聊' }}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applyLoading" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 内容详情页。
 * 展示完整信息 + 组长信息 + 附件下载，并提供申请对接入口。
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Location, CollectionTag, Clock, View, Star, StarFilled,
  Document, Download, User
} from '@element-plus/icons-vue'
import { getArticleDetail, submitRegistration, addFavorite, cancelFavorite, checkFavorite } from '@/api/blog'
import { getApplyOptions } from '@/api/meta'
import { getGroupByArticle } from '@/api/group'
import { getRoleCode, getUserInfo } from '@/utils/authSession'
import OpinionList from '@/components/OpinionList.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref(null)
const favorited = ref(false)

/** 这个项目所属的小组。答辩意见挂在小组下面，所以详情页要拿到它。 */
const group = ref(null)

const currentRoleCode = computed(() => getRoleCode())
const currentUserId = computed(() => {
  const u = getUserInfo()
  return u?.id ?? u?.userId
})

const isMyOwn = computed(() =>
    detail.value && String(detail.value.userId) === String(currentUserId.value)
)

/**
 * 能不能对这条内容表达意向。
 *
 * 【本轮修正】原来这里是从老平台抄来的规则：
 *   学生只能申请 typeId=2（需求），老师压根没有分支 —— 直接 return false。
 * 结果就是「老师和学生对项目都无法表示感兴趣」，等于这个环节废了。
 *
 * 现在的业务是「小组做项目，学生和老师都可以参与」，
 * 所以规则改成：**只要是别人填的内容，学生和老师都能表达意向**。
 * 唯一不放行的还是「自己填的内容」。
 */
const canApply = computed(() => {
  if (!detail.value || isMyOwn.value) {
    return false
  }
  const role = currentRoleCode.value
  // 学生、老师都能对别人的内容表达意向；管理员不用参与对接
  return role === 'STUDENT' || role === 'TEACHER'
})

/** 按钮文案：统一用「感兴趣」—— 这就是我们想要的语义 */
const applyText = computed(() => '感兴趣')

const applyAlert = computed(() => {
  const owner = detail.value?.publisherRoleCode === 'TEACHER' ? '老师' : '组长'
  return `提交后由${owner}决定是否通过，通过后系统会自动开通一条临时沟通通道`
})

/**
 * 当前登录人是不是老师。
 * 老师点「感兴趣」时不用填年级/班级/擅长（那是给学生的口径），
 * 只填「为什么感兴趣 + 联系方式」。
 */
const isTeacher = computed(() => currentRoleCode.value === 'TEACHER')

const attachmentList = computed(() => {
  const a = detail.value?.attachments
  if (!a) {
    return []
  }
  return String(a).split(',').filter(Boolean)
})

// 组长角色：用后端返回的 publisherRoleCode 判断。
// 原来这里拿的是 typeId（内容类型：1=项目 2=需求），完全不是角色，
// 所以谁发的都显示成「学生」，老师也是错的。
const publisherRoleName = computed(() => {
  const d = detail.value || {}
  if (d.publisherRoleName) {
    return d.publisherRoleName
  }
  const map = {
    ADMIN: '系统管理员',
    STUDENT: '学生',
    TEACHER: '老师'
  }
  return map[d.publisherRoleCode] || '用户'
})

const publisherRoleType = computed(() => {
  const map = {
    ADMIN: 'danger',
    STUDENT: 'primary',
    TEACHER: 'warning'
  }
  return map[detail.value?.publisherRoleCode] || 'info'
})

// ---------- 申请弹窗 ----------
const applyVisible = ref(false)
const applyLoading = ref(false)
const applyFormRef = ref(null)
const applyForm = reactive({
  grade: '',
  className: '',
  skills: [],
  reason: '',
  contactWay: '',
  contactWayValue: ''
})

// 申请表单的固定选项（年级 / 班级 / 技能），从后端拿
const gradeOptions = ref([])
const classOptions = ref([])
const skillOptions = ref([])

/** 选完年级再筛班级 —— 不同年级的班级不是一套 */
watch(() => applyForm.grade, (g) => {
  applyForm.className = ''
  classOptions.value = g ? (classMap.value[g] || []) : allClasses.value
})

const classMap = ref({})
const allClasses = ref([])

async function loadApplyOptions() {
  try {
    const res = await getApplyOptions()
    const d = res?.data || {}
    gradeOptions.value = d.grades || []
    classMap.value = d.classMap || {}
    skillOptions.value = d.skills || []
    // 没选年级时先展示全部班级
    allClasses.value = [...new Set(Object.values(classMap.value).flat())]
    classOptions.value = allClasses.value
  } catch (e) {
    // 拿不到就留空，用户至少还能填其他字段
  }
}

/**
 * 申请表单校验。**按角色分两套**：
 *
 * 学生 —— 必填「年级 + 班级 + 擅长部分」。
 *   组长（或被申请方）看申请时，最关心的是「这人是哪个班、会干什么」，
 *   而不是一段客套的理由。
 *
 * 老师 —— 必填「为什么感兴趣 + 联系方式」。
 *   老师不适用「年级班级擅长」那套（本来就是教课的），
 *   关心的是他对这个项目想干什么、怎么联系。
 *
 * 学生的补充说明和老师的联系方式按各自口径处理：
 *   学生：补充说明选填（通过后有临时通道，不必先交微信）
 *   老师：联系方式必填（老师习惯直接联系）
 */
const applyRules = computed(() => {
  const base = {
    contactWayValue: isTeacher.value
        ? [{ required: true, message: '请留下联系方式，方便对方联系你', trigger: 'blur' }]
        : []
  }
  if (isTeacher.value) {
    return {
      ...base,
      reason: [{ required: true, message: '请说说你为什么感兴趣', trigger: 'blur' }]
    }
  }
  return {
    ...base,
    grade: [{ required: true, message: '请选择你的年级', trigger: 'change' }],
    className: [{ required: true, message: '请选择你的班级', trigger: 'change' }],
    skills: [
      { required: true, type: 'array', min: 1, message: '请至少选一项擅长的部分', trigger: 'change' }
    ]
  }
})

async function loadDetail() {
  loading.value = true
  try {
    const res = await getArticleDetail(route.params.id)
    detail.value = res?.data || null
    if (detail.value) {
      await loadFavoriteState()
    }
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function loadFavoriteState() {
  try {
    const res = await checkFavorite({ targetType: 1, targetId: route.params.id })
    favorited.value = !!res?.data
  } catch (e) {
    favorited.value = false
  }
}

async function handleFavorite() {
  try {
    if (favorited.value) {
      await cancelFavorite({ targetType: 1, targetId: detail.value.id })
      favorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite({ targetType: 1, targetId: detail.value.id })
      favorited.value = true
      ElMessage.success('已收藏')
    }
  } catch (e) {
    // 拦截器已提示
  }
}

async function submitApply() {
  const valid = await applyFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  applyLoading.value = true
  try {
    await submitRegistration({
      articleId: detail.value.id,
      typeId: detail.value.typeId,
      // 老师不填年级班级擅长，这三项传空字符串，后端按「未填」处理
      grade: isTeacher.value ? '' : applyForm.grade,
      className: isTeacher.value ? '' : applyForm.className,
      // 后端用逗号分隔的字符串存，这里转一下
      skills: isTeacher.value ? '' : (applyForm.skills || []).join(','),
      reason: applyForm.reason,
      contactWay: applyForm.contactWay,
      contactWayValue: applyForm.contactWayValue
    })
    ElMessage.success('申请已提交，等待对方处理')
    applyVisible.value = false
  } catch (e) {
    // 拦截器已提示
  } finally {
    applyLoading.value = false
  }
}

function fileName(url) {
  if (!url) {
    return '附件'
  }
  const parts = String(url).split('/')
  const last = parts[parts.length - 1]
  // 去掉时间戳前缀，取原始文件名
  const idx = last.indexOf('_')
  return idx > 0 ? last.slice(idx + 1) : last
}

function handleDownload(url) {
  window.open(url, '_blank')
}

function goUserHome(userId) {
  if (userId) {
    router.push(`/cooperation/user/${userId}`)
  }
}

function goBack() {
  router.back()
}

function typeTagType(typeId) {
  const map = { 1: 'success', 2: 'warning', 3: 'info' }
  return map[typeId] || 'info'
}

function urgencyType(level) {
  const map = { 普通: 'info', 较急: 'warning', 紧急: 'danger' }
  return map[level] || 'info'
}

function formatTime(t) {
  if (!t) {
    return ''
  }
  return String(t).replace('T', ' ').slice(0, 16)
}

async function loadGroup() {
  const articleId = route.params.id
  if (!articleId) {
    return
  }
  try {
    const res = await getGroupByArticle(articleId)
    group.value = res?.data || null
  } catch (e) {
    group.value = null
  }
}

onMounted(() => {
  loadDetail()
  loadGroup()
  loadApplyOptions()
})
</script>

<style scoped>
.detail-page {
  padding: 24px 0;
}

.group-block {
  margin-top: 18px;
  border-radius: 10px;
}

.group-block-body {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
}

.group-leader,
.group-members {
  font-size: 13px;
  color: #606266;
}

.group-member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.opinion-block {
  margin-top: 18px;
}

.contact-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.contact-type {
  width: 120px;
  flex-shrink: 0;
}

.contact-value {
  flex: 1;
}

.contact-tip {
  font-size: 12px;
  color: #a0a4ab;
  margin-top: 6px;
  line-height: 1.5;
}

.back-row {
  margin-bottom: 14px;
}

.detail-wrap {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.main-card {
  flex: 1;
  border-radius: 10px;
  min-width: 0;
}

.side-card {
  width: 300px;
  border-radius: 10px;
  flex-shrink: 0;
}

.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.title-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.detail-title {
  font-size: 21px;
  color: #1d3a6b;
  margin: 0;
  line-height: 1.4;
  word-break: break-word;
}

.title-right {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  color: #909399;
  font-size: 13px;
  margin-top: 14px;
}

.meta-row span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.section {
  margin-bottom: 22px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d3a6b;
  margin: 0 0 12px;
}

.content-text {
  font-size: 14px;
  line-height: 1.9;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
}

.info-item {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 12px 14px;
}

.info-item .label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.info-item .value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.attach-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attach-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #f8f9fb;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  font-size: 13px;
}

.attach-item:hover {
  background: #eef2f9;
}

.attach-name {
  flex: 1;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attach-dl {
  color: #1d3a6b;
}

.publisher {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.pub-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.view-home-btn {
  width: 100%;
}

.stat-row {
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.stat-num {
  font-size: 20px;
  font-weight: 600;
  color: #1d3a6b;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 3px;
}

.own-tip {
  margin-top: 14px;
}

@media (max-width: 1000px) {
  .detail-wrap {
    flex-direction: column;
  }
  .side-card {
    width: 100%;
  }
}
</style>
