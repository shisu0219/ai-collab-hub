<template>
  <div class="my-published">
    <div class="page-container">
      <div class="head-row">
        <div>
          <h2 class="head-title">我填写的</h2>
          <p class="head-sub">这里能看到你填写的全部信息和审核状态</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="goPublish">填写信息</el-button>
      </div>

      <!-- 状态筛选 -->
      <el-card class="filter-card" shadow="never">
        <div class="filter-row">
          <el-radio-group v-model="query.statusId" @change="handleSearch">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button :value="1">待审核</el-radio-button>
            <el-radio-button :value="2">已发布</el-radio-button>
            <el-radio-button :value="3">已拒绝</el-radio-button>
            <el-radio-button :value="4">已下架</el-radio-button>
          </el-radio-group>

          <el-input
            v-model="query.title"
            placeholder="搜索标题"
            clearable
            class="filter-title"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </div>
      </el-card>

      <!-- 列表 -->
      <div v-loading="loading" class="list-wrap">
        <template v-if="list.length">
          <div v-for="item in list" :key="item.id" class="item-card">
            <div class="card-top">
              <div class="card-title-line">
                <el-tag size="small" :type="typeTagType(item.typeId)" effect="plain">
                  {{ item.typeName || '内容' }}
                </el-tag>
                <span class="item-card__title" @click="goDetail(item.id)">{{ item.title }}</span>
              </div>
              <div class="card-actions">
                <el-tag :type="articleStatusTagType(item.statusId)" size="small">
                  {{ articleStatusName(item.statusId) }}
                </el-tag>
              </div>
            </div>

            <p class="item-card__desc">{{ stripHtml(item.content) }}</p>

            <!-- 被拒原因 -->
            <el-alert
              v-if="item.statusId === 3 && item.rejectReason"
              :title="`未通过原因：${item.rejectReason}`"
              type="error"
              :closable="false"
              class="reject-alert"
            />

            <div class="item-card__meta">
              <span><el-icon><Location /></el-icon> {{ item.location || '地点待定' }}</span>
              <span><el-icon><CollectionTag /></el-icon> {{ item.tagName || '无标签' }}</span>
              <span><el-icon><Clock /></el-icon> {{ formatTime(item.createTime) }}</span>
              <span><el-icon><View /></el-icon> {{ item.viewCount || 0 }} 次浏览</span>
              <span
                class="apply-count"
                :class="{ 'apply-count--active': item.registrationCount > 0 }"
                @click="openApplyDialog(item)"
              >
                <el-icon><ChatDotRound /></el-icon>
                {{ item.registrationCount || 0 }} 条申请
                <em v-if="item.registrationCount > 0">查看 / 沟通</em>
              </span>
            </div>

            <div class="card-footer">
              <el-button
                v-if="item.registrationCount > 0"
                size="small"
                type="primary"
                plain
                :icon="ChatDotRound"
                @click="openApplyDialog(item)"
              >
                申请（{{ item.registrationCount }}）
              </el-button>
              <el-button
                v-if="item.statusId === 2"
                size="small"
                :icon="View"
                @click="goDetail(item.id)"
              >
                查看
              </el-button>
              <el-button
                size="small"
                :icon="Edit"
                @click="goEdit(item.id)"
              >
                编辑
              </el-button>
              <el-button
                v-if="item.statusId !== 4"
                size="small"
                :icon="Download"
                @click="handleOffline(item)"
              >
                下架
              </el-button>
              <el-button
                size="small"
                type="danger"
                :icon="Delete"
                @click="handleDelete(item)"
              >
                删除
              </el-button>
            </div>
          </div>
        </template>

        <el-empty v-else-if="!loading" description="还没有填写过信息">
          <el-button type="primary" @click="goPublish">去填写第一条</el-button>
        </el-empty>
      </div>

      <!-- 申请管理弹窗：组长在这里看谁申请了、审核、直接开聊 -->
      <el-dialog
        v-model="applyVisible"
        :title="`「${currentArticle?.title || ''}」的申请`"
        width="720px"
        class="apply-dialog"
      >
        <div v-loading="applyLoading">
          <template v-if="applyList.length">
            <div v-for="row in applyList" :key="row.id" class="apply-item">
              <div class="apply-item__head">
                <div class="apply-item__who">
                  <span class="apply-item__name">{{ row.applicantName || '申请人' }}</span>
                  <el-tag
                    size="small"
                    :type="row.pass === 1 ? 'success' : (row.pass === 0 ? 'danger' : 'warning')"
                    effect="plain"
                  >
                    {{ row.pass === 1 ? '已通过' : (row.pass === 0 ? '已拒绝' : '待处理') }}
                  </el-tag>
                  <el-tag v-if="row.collabProgress" size="small" type="info" effect="plain">
                    {{ row.collabProgressName || progressName(row.collabProgress) }}
                  </el-tag>
                </div>
                <span class="apply-item__time">{{ formatTime(row.createTime) }}</span>
              </div>

              <p class="apply-item__reason">{{ row.reason || '（未填写申请说明）' }}</p>

              <div class="apply-item__contact">
                联系方式：{{ row.contactWay || '未填' }}
                <span v-if="row.contactWayValue">：{{ row.contactWayValue }}</span>
              </div>

              <div v-if="row.reviewMessage" class="apply-item__reply">
                我的回复：{{ row.reviewMessage }}
              </div>

              <div class="apply-item__actions">
                <template v-if="row.pass == null">
                  <el-button size="small" type="success" :icon="Check" @click="openReview(row, true)">
                    通过
                  </el-button>
                  <el-button size="small" type="danger" :icon="Close" @click="openReview(row, false)">
                    拒绝
                  </el-button>
                </template>
                <el-button
                  size="small"
                  type="primary"
                  :icon="ChatDotRound"
                  @click="goChat(row)"
                >
                  进入沟通
                </el-button>
              </div>
            </div>
          </template>
          <el-empty v-else-if="!applyLoading" description="还没有人申请" />
        </div>

        <div class="apply-pager">
          <el-pagination
            v-model:current-page="applyQuery.pageNum"
            :page-size="applyQuery.pageSize"
            :total="applyTotal"
            layout="prev, pager, next"
            @current-change="loadApplications"
          />
        </div>
      </el-dialog>

      <!-- 处理申请 -->
      <el-dialog v-model="reviewVisible" :title="reviewPass ? '通过申请' : '拒绝申请'" width="440px">
        <el-input
          v-model="reviewForm.reason"
          type="textarea"
          :rows="3"
          :placeholder="reviewPass ? '给对方留句话（可选）' : '请说明拒绝原因，对方能看到'"
        />
        <template #footer>
          <el-button @click="reviewVisible = false">取消</el-button>
          <el-button
            :type="reviewPass ? 'success' : 'danger'"
            :loading="reviewLoading"
            @click="submitReview"
          >
            确定
          </el-button>
        </template>
      </el-dialog>

      <div class="pager-wrap">
        <el-pagination
          v-model:current-page="query.pageNumber"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
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
 * 我填写的信息页。
 * 相比原系统补上了：状态筛选、搜索、被拒原因展示、下架按钮。
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, Location, CollectionTag, Clock, View,
  Edit, Delete, Download, ChatDotRound, Check, Close
} from '@element-plus/icons-vue'
import { getMyArticleList, deleteArticle, updateArticle } from '@/api/blog'
import {
  getRegistrationList, handleRegistration, updateCollabProgress
} from '@/api/blog'
import { createSession } from '@/api/chat'
import { articleStatusName, articleStatusTagType, collabProgressName } from '@/utils/roleDisplay'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNumber: 1,
  pageSize: 10,
  statusId: null,
  title: ''
})

// ---------- 申请管理弹窗 ----------
const applyVisible = ref(false)
const applyLoading = ref(false)
const applyList = ref([])
const applyTotal = ref(0)
const currentArticle = ref(null)   // 当前在看哪篇内容的申请
const applyQuery = reactive({
  pageNum: 1,
  pageSize: 10
})

// 处理申请（通过/拒绝）
const reviewVisible = ref(false)
const reviewPass = ref(true)
const reviewLoading = ref(false)
const reviewForm = reactive({ id: null, reason: '' })

function openApplyDialog(item) {
  currentArticle.value = item
  applyQuery.pageNum = 1
  applyVisible.value = true
  loadApplications()
}

async function loadApplications() {
  if (!currentArticle.value) {
    return
  }
  applyLoading.value = true
  try {
    const res = await getRegistrationList({
      direction: 'received',
      articleId: currentArticle.value.id,
      pageNum: applyQuery.pageNum,
      pageSize: applyQuery.pageSize
    })
    const data = res?.data || {}
    applyList.value = data.records || []
    applyTotal.value = data.total || 0
  } catch (e) {
    applyList.value = []
    applyTotal.value = 0
  } finally {
    applyLoading.value = false
  }
}

function openReview(row, pass) {
  reviewForm.id = row.id
  reviewForm.reason = ''
  reviewPass.value = pass
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewPass.value && !reviewForm.reason.trim()) {
    ElMessage.warning('拒绝时请说明原因，对方能看到')
    return
  }
  reviewLoading.value = true
  try {
    await handleRegistration({
      id: reviewForm.id,
      pass: reviewPass.value ? 1 : 0,
      reviewMessage: reviewForm.reason
    })
    ElMessage.success(reviewPass.value ? '已通过申请' : '已拒绝申请')
    reviewVisible.value = false
    loadApplications()
    loadList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    reviewLoading.value = false
  }
}

/**
 * 进入沟通。
 * 这就是原来缺的那条路：组长在「我填写的」里看到申请，
 * 直接点这里就能建会话开聊，不用再翻到「我的申请」去找。
 */
async function goChat(row) {
  try {
    const res = await createSession({
      registrationId: row.id,
      articleId: currentArticle.value?.id
    })
    const sessionId = res?.data?.id ?? res?.data
    if (!sessionId) {
      ElMessage.error('会话创建失败，请稍后再试')
      return
    }
    applyVisible.value = false
    router.push({ path: '/cooperation/chat', query: { sessionId: String(sessionId) } })
  } catch (e) {
    // 拦截器已提示
  }
}

function progressName(v) {
  return collabProgressName ? collabProgressName(v) : '对接中'
}

async function loadList() {
  loading.value = true
  try {
    const res = await getMyArticleList({
      pageNumber: query.pageNumber,
      pageSize: query.pageSize,
      statusId: query.statusId || undefined,
      title: query.title || undefined
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
}

function handleSearch() {
  query.pageNumber = 1
  loadList()
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm(
        `确定要删除「${item.title}」吗？删除后无法恢复。`,
        '删除确认',
        { type: 'warning' }
    )
    await deleteArticle(item.id)
    ElMessage.success('已删除')
    loadList()
  } catch (e) {
    // 用户取消或接口报错
  }
}

async function handleOffline(item) {
  try {
    await ElMessageBox.confirm(
        '下架后内容将不再展示在浏览页，可以重新编辑后再提交审核。确定下架吗？',
        '下架确认',
        { type: 'warning' }
    )
    // 下架 = 把状态改成 4
    await updateArticle({ id: item.id, statusId: 4, title: item.title, typeId: item.typeId })
    ElMessage.success('已下架')
    loadList()
  } catch (e) {
    // 取消
  }
}

function goDetail(id) {
  router.push(`/cooperation/detail/${id}`)
}

function goEdit(id) {
  router.push({ path: '/cooperation/publish', query: { id } })
}

function goPublish() {
  router.push('/cooperation/publish')
}

function typeTagType(typeId) {
  const map = { 1: 'success', 2: 'warning', 3: 'info' }
  return map[typeId] || 'info'
}

function stripHtml(html) {
  if (!html) {
    return '暂无描述'
  }
  const text = String(html).replace(/<[^>]+>/g, '')
  return text.length > 110 ? text.slice(0, 110) + '...' : text
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
.my-published {
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
  width: 260px;
}

.list-wrap {
  min-height: 260px;
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.card-title-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.card-title-line .item-card__title {
  margin-bottom: 0;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-title-line .item-card__title:hover {
  color: #1d3a6b;
  text-decoration: underline;
}

.item-card__meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.reject-alert {
  margin-top: 10px;
}

.card-footer {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #e4e7ed;
  display: flex;
  gap: 8px;
}

.pager-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* ---------- 申请数（可点击） ---------- */
.apply-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.apply-count--active {
  color: #1d3a6b;
  cursor: pointer;
  font-weight: 600;
}

.apply-count--active em {
  font-style: normal;
  font-size: 12px;
  color: #409eff;
  text-decoration: underline;
}

/* ---------- 申请弹窗 ---------- */
.apply-item {
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fafbfc;
}

.apply-item__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.apply-item__who {
  display: flex;
  align-items: center;
  gap: 8px;
}

.apply-item__name {
  font-weight: 600;
  color: #1d3a6b;
}

.apply-item__time {
  font-size: 12px;
  color: #909399;
}

.apply-item__reason {
  margin: 6px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.apply-item__contact {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.apply-item__reply {
  font-size: 13px;
  color: #67c23a;
  background: #f0f9eb;
  padding: 6px 10px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.apply-item__actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.apply-pager {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

</style>
