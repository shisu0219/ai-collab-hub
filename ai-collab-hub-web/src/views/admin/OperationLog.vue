<template>
  <div class="log-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-head">
          <div>
            <span class="card-title">操作日志</span>
            <span class="card-sub">谁在什么时候做了什么，都记录在这里，出问题能查</span>
          </div>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </template>

      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索操作人 / 动作"
          clearable
          class="filter-item"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />

        <el-select v-model="query.module" placeholder="模块" clearable class="filter-item-sm" @change="handleSearch">
          <el-option label="用户审核" value="用户审核" />
          <el-option label="内容审核" value="内容审核" />
          <el-option label="填写内容" value="发布内容" />
          <el-option label="对接申请" value="对接申请" />
          <el-option label="账户管理" value="账户管理" />
        </el-select>

        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="filter-date"
          @change="handleSearch"
        />
      </div>

      <el-table v-loading="loading" :data="list" border stripe class="data-table">
        <el-table-column prop="createTime" label="时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column prop="userAccount" label="操作人" width="130">
          <template #default="{ row }">{{ row.userAccount || '—' }}</template>
        </el-table-column>

        <el-table-column prop="module" label="模块" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.module || '—' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="action" label="操作" width="180" show-overflow-tooltip />

        <el-table-column prop="detail" label="详情" min-width="240" show-overflow-tooltip />

        <el-table-column prop="ip" label="IP" width="140">
          <template #default="{ row }">{{ row.ip || '—' }}</template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无操作日志" :image-size="90" />
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
  </div>
</template>

<script setup>
/**
 * 操作日志页（新增功能）。
 * 原系统谁改了什么没留痕，出了问题查不到，这里补上。
 */
import { ref, reactive, onMounted } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getOperationLogList } from '@/api/admin'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref([])

const query = reactive({
  pageNumber: 1,
  pageSize: 10,
  keyword: '',
  module: null
})

async function loadList() {
  loading.value = true
  try {
    const res = await getOperationLogList({
      pageNumber: query.pageNumber,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      module: query.module || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined
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

function formatTime(t) {
  if (!t) {
    return ''
  }
  return String(t).replace('T', ' ').slice(0, 19)
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
  width: 240px;
}

.filter-item-sm {
  width: 130px;
}

.filter-date {
  width: 280px;
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
