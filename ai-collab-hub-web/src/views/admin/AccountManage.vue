<template>
  <div class="account-manage-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-head">
          <div>
            <span class="card-title">账户管理</span>
            <span class="card-sub">查看全部已通过审核的用户，可启用/禁用账号</span>
          </div>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </template>

      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索账号 / 昵称 / 邮箱"
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
        <el-select v-model="query.enable" placeholder="账号状态" clearable class="filter-item-sm" @change="handleSearch">
          <el-option label="正常" :value="1" />
          <el-option label="已禁用" :value="0" />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="list" border stripe class="data-table">
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

        <el-table-column label="单位 / 院系" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.department || '—' }}
          </template>
        </el-table-column>

        <el-table-column label="账号状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enable === 1 ? 'success' : 'danger'" size="small">
              {{ row.enable === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="注册时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.enable === 1 ? 'danger' : 'success'"
              @click="handleToggle(row)"
            >
              {{ row.enable === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" text type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无用户" :image-size="90" />
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

    <el-dialog v-model="detailVisible" title="用户详情" width="560px">
      <el-descriptions :column="2" border v-if="detailRow">
        <el-descriptions-item label="账号">{{ detailRow.account }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detailRow.nickname }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detailRow.email }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detailRow.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ resolveRoleName(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="detailRow.enable === 1 ? 'success' : 'danger'" size="small">
            {{ detailRow.enable === 1 ? '正常' : '已禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="单位 / 院系" :span="2">
          {{ detailRow.department || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">
          {{ formatTime(detailRow.createTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 账户管理页。
 * 已通过审核的用户在这里统一管理，可以禁用捣乱的账号。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getUserList, toggleUserEnable } from '@/api/admin'
import { resolveRoleName, resolveRoleTagType } from '@/utils/roleDisplay'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNumber: 1,
  pageSize: 10,
  keyword: '',
  roleId: null,
  enable: null
})

const detailVisible = ref(false)
const detailRow = ref(null)

async function loadList() {
  loading.value = true
  try {
    const res = await getUserList({
      pageNumber: query.pageNumber,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      roleId: query.roleId || undefined,
      enable: query.enable
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

async function handleToggle(row) {
  const action = row.enable === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
        `确定要${action}「${row.nickname}」这个账号吗？`,
        `${action}确认`,
        { type: 'warning' }
    )
    await toggleUserEnable({
      userId: row.userId ?? row.id,
      enable: row.enable === 1 ? 0 : 1
    })
    ElMessage.success(`已${action}`)
    loadList()
  } catch (e) {
    // 取消
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

onMounted(loadList)
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

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.filter-item {
  width: 260px;
}

.filter-item-sm {
  width: 130px;
}

.data-table {
  border-radius: 8px;
}

.pager-wrap {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}
</style>
