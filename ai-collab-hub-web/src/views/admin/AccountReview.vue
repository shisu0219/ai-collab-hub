<template>
  <div class="review-account-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-head">
          <div>
            <span class="card-title">账户审核</span>
            <span class="card-sub">学生和老师注册后需要在这里审核，通过后才能登录</span>
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
          v-model="query.keyword"
          placeholder="搜索账号 / 昵称"
          clearable
          class="filter-item"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.roleId" placeholder="角色" clearable class="filter-item-sm" @change="handleSearch">
          <el-option label="学生" :value="2" />
            <el-option label="老师" :value="4" />
        </el-select>
        <el-radio-group v-model="query.auditStatus" @change="handleSearch">
          <el-radio-button :value="0">待审核</el-radio-button>
          <el-radio-button :value="1">已通过</el-radio-button>
          <el-radio-button :value="2">已拒绝</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        class="data-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" :selectable="rowSelectable" />

        <el-table-column prop="account" label="账号" width="130" />
        <el-table-column prop="nickname" label="昵称/姓名" width="130" />

        <el-table-column label="角色" width="90">
          <template #default="{ row }">
            <el-tag :type="resolveRoleTagType(row)" effect="plain" size="small">
              {{ resolveRoleName(row) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="email" label="邮箱" width="190" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="120">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>

        <!-- 单位 / 院系 -->
        <el-table-column label="单位 / 院系" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.department || '—' }}
          </template>
        </el-table-column>

        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="auditStatusTagType(row.auditStatus)" size="small">
              {{ auditStatusName(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="注册时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.auditStatus === 0">
              <el-button size="small" type="success" @click="handleReview(row, true)">通过</el-button>
              <el-button size="small" type="danger" @click="handleReview(row, false)">拒绝</el-button>
            </template>
            <el-button v-else size="small" text type="primary" @click="showDetail(row)">查看</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有待审核的账户" :image-size="90" />
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
    <el-dialog v-model="reviewVisible" :title="reviewPass ? '通过审核' : '拒绝审核'" width="520px" destroy-on-close>
      <el-alert
        :title="reviewPass ? '通过后该用户即可登录平台' : '拒绝后请说明原因，用户可以在注册页重新提交'"
        :type="reviewPass ? 'success' : 'warning'"
        :closable="false"
        class="mb-16"
      />

      <div class="review-user-info">
        <span class="label">账号：</span>{{ reviewTarget?.account }}
        <span class="label ml-16">昵称：</span>{{ reviewTarget?.nickname }}
      </div>

      <!-- 拒绝理由模板（新增） -->
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
          <el-input
            v-model="reason"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            :placeholder="reviewPass ? '可以写点欢迎语' : '请说明拒绝原因'"
          />
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
    <el-dialog v-model="detailVisible" title="账户详情" width="560px">
      <el-descriptions :column="2" border v-if="detailRow">
        <el-descriptions-item label="账号">{{ detailRow.account }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detailRow.nickname }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detailRow.email }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detailRow.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
            {{ resolveRoleName(detailRow) }}
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="auditStatusTagType(detailRow.auditStatus)" size="small">
            {{ auditStatusName(detailRow.auditStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">
          {{ formatTime(detailRow.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">
          {{ detailRow.reason || '—' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 账户审核页。
 *
 * 相比原系统补上了：
 *  1. 多选 + 批量通过/批量拒绝（开学季几十个注册，一个个点会疯）
 *  2. 按账号/昵称搜索
 *  3. 按角色、状态筛选
 *  4. 拒绝理由模板一键填入
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Select, CloseBold, Refresh, Search } from '@element-plus/icons-vue'
import { getUserReviewList, reviewUser, batchReviewUser, getRejectTemplateList } from '@/api/admin'
import { auditStatusName, auditStatusTagType, resolveRoleName, resolveRoleTagType } from '@/utils/roleDisplay'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const selectedIds = ref([])
const rejectTemplates = ref([])

const query = reactive({
  pageNumber: 1,
  pageSize: 10,
  keyword: '',
  roleId: null,
  auditStatus: 0
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
    const res = await getUserReviewList({
      pageNumber: query.pageNumber,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      roleId: query.roleId || undefined,
      auditStatus: query.auditStatus
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
    const res = await getRejectTemplateList({ scene: 'user' })
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
  selectedIds.value = rows.map((r) => r.id)
}

/** 已处理过的不能再选 */
function rowSelectable(row) {
  return row.auditStatus === 0
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
    await reviewUser({
      userId: reviewTarget.value.userId ?? reviewTarget.value.id,
      pass: reviewPass.value ? 1 : 0,
      reason: reason.value
    })
    ElMessage.success(reviewPass.value ? '已通过' : '已拒绝')
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
        `确定要批量${action}选中的 ${selectedIds.value.length} 个账户吗？`,
        `批量${action}`,
        { type: 'warning' }
    )

    if (!pass) {
      // 批量拒绝时统一填一个理由
      const { value } = await ElMessageBox.prompt(
          '请填写批量拒绝的原因（会应用到所有选中账户）',
          '拒绝原因',
          { inputPlaceholder: '例如：材料不全，请补充后重新提交', inputValue: '' }
      )
      if (!value) {
        ElMessage.warning('请填写拒绝原因')
        return
      }
      await batchReviewUser({ userIds: selectedIds.value, pass: 0, reason: value })
    } else {
      await batchReviewUser({ userIds: selectedIds.value, pass: 1, reason: '批量通过' })
    }

    ElMessage.success(`已批量${action} ${selectedIds.value.length} 个账户`)
    loadList()
  } catch (e) {
    // 用户取消
  }
}

function showDetail(row) {
  detailRow.value = row
  detailVisible.value = true
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

.review-user-info {
  font-size: 14px;
  color: #303133;
  padding: 10px 14px;
  background: #f8f9fb;
  border-radius: 8px;
}

.review-user-info .label {
  color: #909399;
}

.ml-16 {
  margin-left: 16px;
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
