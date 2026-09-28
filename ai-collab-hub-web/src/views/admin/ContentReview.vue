<template>
  <div class="content-review-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-head">
          <div>
            <span class="card-title">内容管理</span>
            <span class="card-sub">学生项目、老师课题填写提交后需要在这里审核</span>
          </div>
          <div class="head-actions">
            <el-button
              type="success"
              :icon="Select"
              :disabled="!selectedIds.length"
              @click="handleBatch(true)"
            >
              批量通过（{{ selectedIds.length }}）
            </el-button>
            <el-button
              type="danger"
              :icon="CloseBold"
              :disabled="!selectedIds.length"
              @click="handleBatch(false)"
            >
              批量拒绝（{{ selectedIds.length }}）
            </el-button>
            <el-button :icon="Refresh" @click="loadList">刷新</el-button>
          </div>
        </div>
      </template>

      <!-- 筛选 -->
      <div class="filter-row">
        <el-input
          v-model="query.title"
          placeholder="搜索标题"
          clearable
          class="filter-item"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.typeId" placeholder="类型" clearable class="filter-item-sm" @change="handleSearch">
          <el-option label="项目" :value="1" />
          <el-option label="需求" :value="2" />
          <el-option label="课程" :value="3" />
        </el-select>
        <el-radio-group v-model="query.statusId" @change="handleSearch">
          <el-radio-button :value="1">待审核</el-radio-button>
          <el-radio-button :value="2">已发布</el-radio-button>
          <el-radio-button :value="3">已拒绝</el-radio-button>
          <el-radio-button :value="4">已下架</el-radio-button>
        </el-radio-group>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        class="data-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" :selectable="rowSelectable" />

        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />

        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.typeId)" effect="plain" size="small">
              {{ row.typeName || (row.typeId === 1 ? '项目' : row.typeId === 2 ? '需求' : '课程') }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="publisherName" label="组长" width="120" show-overflow-tooltip />

        <el-table-column label="组长角色" width="100">
          <template #default="{ row }">
            {{ resolveRoleName({ roleCode: row.publisherRoleCode, roleName: row.publisherRoleName }) }}
          </template>
        </el-table-column>

        <el-table-column prop="location" label="地区" width="110" show-overflow-tooltip />

        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="articleStatusTagType(row.statusId)" size="small">
              {{ articleStatusName(row.statusId) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="提交时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="showDetail(row)">详情</el-button>
            <template v-if="row.statusId === 1">
              <el-button size="small" type="success" @click="handleReview(row, true)">通过</el-button>
              <el-button size="small" type="danger" @click="handleReview(row, false)">拒绝</el-button>
            </template>
            <el-button
              v-else-if="row.statusId === 2"
              size="small"
              type="warning"
              @click="handleOffline(row)"
            >
              下架
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有符合条件的内容" :image-size="90" />
        </template>
      </el-table>

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
    </el-card>

    <!-- 审核弹窗 -->
    <el-dialog v-model="reviewVisible" :title="reviewPass ? '通过内容' : '拒绝内容'" width="620px" destroy-on-close>
      <el-alert
        :title="reviewPass ? '通过后内容将展示在浏览页' : '拒绝后请说明原因，组长可以在「我填写的」看到'"
        :type="reviewPass ? 'success' : 'warning'"
        :closable="false"
        class="mb-16"
      />

      <div v-if="reviewTarget" class="content-preview">
        <div class="preview-title">{{ reviewTarget.title }}</div>
        <div class="preview-meta">
          {{ reviewTarget.publisherName }} · {{ reviewTarget.location || '地点待定' }}
        </div>
        <div class="preview-content">{{ reviewTarget.content }}</div>
      </div>

      <div v-if="!reviewPass" class="template-block">
        <div class="template-label">常用拒绝理由（点击填入）：</div>
        <el-tag
          v-for="t in rejectTemplates"
          :key="t.code"
          class="template-tag"
          effect="plain"
          @click="reason = t.content"
        >
          {{ t.content }}
        </el-tag>
      </div>

      <el-form label-position="top" class="mt-16">
        <el-form-item :label="reviewPass ? '备注（选填）' : '拒绝原因'">
          <el-input v-model="reason" type="textarea" :rows="3" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button
          :type="reviewPass ? 'success' : 'danger'"
          :loading="reviewLoading"
          @click="submitReview"
        >
          确定{{ reviewPass ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="内容详情" width="680px">
      <el-descriptions :column="2" border v-if="detailRow">
        <el-descriptions-item label="标题" :span="2">{{ detailRow.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          {{ detailRow.typeId === 1 ? '项目' : detailRow.typeId === 2 ? '需求' : '课程' }}
        </el-descriptions-item>
        <el-descriptions-item label="地区">{{ detailRow.location || '—' }}</el-descriptions-item>
        <el-descriptions-item label="组长">{{ detailRow.publisherName }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatTime(detailRow.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="标签">{{ detailRow.tagName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="预算">{{ detailRow.budget || '—' }}</el-descriptions-item>
        <el-descriptions-item label="详细内容" :span="2">
          <div class="detail-content">{{ detailRow.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailRow.rejectReason" label="拒绝原因" :span="2">
          {{ detailRow.rejectReason }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 内容管理页。
 *
 * 相比原系统补上了：
 *  1. 批量审核
 *  2. 标题搜索 + 类型/状态筛选
 *  3. 审核前能直接看完整内容，不用跳页
 *  4. 拒绝理由模板
 *  5. 已发布内容支持下架
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Select, CloseBold, Refresh, Search } from '@element-plus/icons-vue'
import { getArticleReviewList, reviewArticle, batchReviewArticle, getRejectTemplateList } from '@/api/admin'
import { updateArticle } from '@/api/blog'
import { articleStatusName, articleStatusTagType, resolveRoleName } from '@/utils/roleDisplay'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const selectedIds = ref([])
const rejectTemplates = ref([])

const query = reactive({
  pageNumber: 1,
  pageSize: 10,
  title: '',
  typeId: null,
  statusId: 1
})

const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewPass = ref(true)
const reviewTarget = ref(null)
const reason = ref('')

const detailVisible = ref(false)
const detailRow = ref(null)

async function loadList() {
  loading.value = true
  try {
    const res = await getArticleReviewList({
      pageNumber: query.pageNumber,
      pageSize: query.pageSize,
      title: query.title || undefined,
      typeId: query.typeId || undefined,
      statusId: query.statusId
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

async function loadTemplates() {
  try {
    const res = await getRejectTemplateList({ scene: 'article' })
    rejectTemplates.value = res?.data || []
  } catch (e) {
    rejectTemplates.value = []
  }
}

function handleSearch() {
  query.pageNumber = 1
  loadList()
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.articleId || r.id)
}

function rowSelectable(row) {
  return row.statusId === 1
}

function handleReview(row, pass) {
  reviewTarget.value = row
  reviewPass.value = pass
  reason.value = ''
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewPass.value && !reason.value.trim()) {
    ElMessage.warning('拒绝时请填写原因')
    return
  }
  reviewLoading.value = true
  try {
    await reviewArticle({
      articleId: reviewTarget.value.articleId || reviewTarget.value.id,
      pass: reviewPass.value ? 1 : 0,
      reason: reason.value
    })
    ElMessage.success(reviewPass.value ? '已通过，内容已上线' : '已拒绝')
    reviewVisible.value = false
    loadList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    reviewLoading.value = false
  }
}

async function handleBatch(pass) {
  if (!selectedIds.value.length) {
    return
  }
  const action = pass ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(
        `确定要批量${action}选中的 ${selectedIds.value.length} 条内容吗？`,
        `批量${action}`,
        { type: 'warning' }
    )

    let batchReason = '批量通过'
    if (!pass) {
      const { value } = await ElMessageBox.prompt(
          '请填写批量拒绝的原因（会应用到所有选中内容）',
          '拒绝原因',
          { inputPlaceholder: '例如：内容描述过于简略，请补充完整信息' }
      )
      if (!value) {
        ElMessage.warning('请填写拒绝原因')
        return
      }
      batchReason = value
    }

    await batchReviewArticle({ ids: selectedIds.value, pass: pass ? 1 : 0, reason: batchReason })
    ElMessage.success(`已批量${action} ${selectedIds.value.length} 条内容`)
    loadList()
  } catch (e) {
    // 用户取消
  }
}

async function handleOffline(row) {
  try {
    await ElMessageBox.confirm(
        `确定要下架「${row.title}」吗？下架后浏览页不再展示。`,
        '下架确认',
        { type: 'warning' }
    )
    await updateArticle({ id: row.articleId || row.id, statusId: 4, title: row.title, typeId: row.typeId })
    ElMessage.success('已下架')
    loadList()
  } catch (e) {
    // 取消
  }
}

function showDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

function typeTagType(typeId) {
  const map = { 1: 'success', 2: 'warning', 3: 'info' }
  return map[typeId] || 'info'
}

function formatTime(t) {
  if (!t) {
    return ''
  }
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  loadList()
  loadTemplates()
})
</script>

<style scoped>
.main-card {
  border-radius: 10px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d3a6b;
  margin-right: 12px;
}

.card-sub {
  font-size: 12px;
  color: #909399;
}

.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.filter-item {
  width: 220px;
}

.filter-item-sm {
  width: 120px;
}

.data-table {
  border-radius: 8px;
}

.content-preview {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 14px 16px;
  max-height: 220px;
  overflow-y: auto;
}

.preview-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d3a6b;
}

.preview-meta {
  font-size: 12px;
  color: #909399;
  margin: 6px 0 10px;
}

.preview-content {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

.detail-content {
  white-space: pre-wrap;
  line-height: 1.8;
  max-height: 260px;
  overflow-y: auto;
}

.template-block {
  margin-top: 14px;
  padding: 12px 14px;
  background: #fdf6ec;
  border-radius: 8px;
}

.template-label {
  font-size: 12px;
  color: #b88230;
  margin-bottom: 8px;
}

.template-tag {
  margin: 0 8px 8px 0;
  cursor: pointer;
}

.template-tag:hover {
  background: #fff;
}

.pager-wrap {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}
</style>
