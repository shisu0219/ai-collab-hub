<template>
  <div class="dashboard-page" v-loading="loading">
    <!-- 顶部数字卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="8" :md="6" :lg="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-inner">
            <div class="stat-icon" :style="{ background: card.color }">
              <el-icon :size="20"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-head">
              <span class="chart-title">注册趋势（近 30 天）</span>
            </div>
          </template>
          <div ref="registerChartRef" class="chart-box"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-head">
              <span class="chart-title">内容填写趋势（近 30 天）</span>
            </div>
          </template>
          <div ref="articleChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-head">
              <span class="chart-title">用户角色分布</span>
            </div>
          </template>
          <div ref="roleChartRef" class="chart-box"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-head">
              <span class="chart-title">对接情况</span>
            </div>
          </template>
          <div ref="collabChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办提醒 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <span class="chart-title">待处理事项</span>
          </template>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="6">
              <div class="todo-card" @click="goPage('/admin/review-account')">
                <div class="todo-num warn">{{ overview.pendingUserCount || 0 }}</div>
                <div class="todo-label">待审核账户</div>
                <el-icon class="todo-arrow"><ArrowRight /></el-icon>
              </div>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <div class="todo-card" @click="goPage('/admin/review-content')">
                <div class="todo-num warn">{{ overview.pendingArticleCount || 0 }}</div>
                <div class="todo-label">待审核内容</div>
                <el-icon class="todo-arrow"><ArrowRight /></el-icon>
              </div>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <div class="todo-card">
                <div class="todo-num">{{ overview.totalUser || 0 }}</div>
                <div class="todo-label">平台总用户</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <div class="todo-card">
                <div class="todo-num">{{ overview.collabRate || 0 }}%</div>
                <div class="todo-label">对接成功率</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * 数据看板（新增功能）。
 *
 * 原平台技术栈里就装了 ECharts，但一个图表都没有，管理员看不到任何数据。
 * 这里把注册、填写、对接、角色分布都可视化出来，团委汇报直接截图就能用。
 */
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  User, UserFilled, Document, ChatDotRound, TrendCharts, ArrowRight
} from '@element-plus/icons-vue'
import {
  getDashboardOverview, getRegisterTrend, getArticleTrend, getCollabStat
} from '@/api/admin'

const router = useRouter()

const loading = ref(false)
const overview = reactive({
  totalUser: 0,
  studentCount: 0,
  teacherCount: 0,
  adminCount: 0,
  totalArticle: 0,
  publishedArticle: 0,
  totalRegistration: 0,
  passedRegistration: 0,
  pendingUserCount: 0,
  pendingArticleCount: 0,
  collabRate: 0
})

const registerChartRef = ref(null)
const articleChartRef = ref(null)
const roleChartRef = ref(null)
const collabChartRef = ref(null)

let charts = []

const statCards = computed(() => [
  { key: 'user', label: '总用户数', value: overview.totalUser, icon: User, color: '#1d3a6b' },
  { key: 'student', label: '学生用户', value: overview.studentCount, icon: UserFilled, color: '#409eff' },
  { key: 'teacher', label: '老师用户', value: overview.teacherCount, icon: UserFilled, color: '#67c23a' },
  { key: 'article', label: '填写内容', value: overview.totalArticle, icon: Document, color: '#e6a23c' },
  { key: 'reg', label: '对接申请', value: overview.totalRegistration, icon: ChatDotRound, color: '#9c27b0' },
  { key: 'rate', label: '对接成功率', value: overview.collabRate + '%', icon: TrendCharts, color: '#c9a227' }
])

async function loadOverview() {
  try {
    const res = await getDashboardOverview()
    Object.assign(overview, res?.data || {})
  } catch (e) {
    // 静默
  }
}

/** 近 30 天的日期标签 */
function buildDateAxis() {
  const arr = []
  for (let i = 29; i >= 0; i--) {
    const d = new Date(Date.now() - i * 86400000)
    arr.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  return arr
}

function initRegisterChart(data) {
  if (!registerChartRef.value) {
    return
  }
  const chart = echarts.init(registerChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: buildDateAxis(), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: '新增注册',
      type: 'line',
      smooth: true,
      data: data || new Array(30).fill(0),
      areaStyle: { opacity: 0.15 },
      itemStyle: { color: '#1d3a6b' },
      lineStyle: { width: 2 }
    }]
  })
  charts.push(chart)
}

function initArticleChart(data) {
  if (!articleChartRef.value) {
    return
  }
  const chart = echarts.init(articleChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: buildDateAxis(), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: '新增填写',
      type: 'bar',
      data: data || new Array(30).fill(0),
      itemStyle: { color: '#c9a227', borderRadius: [4, 4, 0, 0] },
      barMaxWidth: 18
    }]
  })
  charts.push(chart)
}

function initRoleChart() {
  if (!roleChartRef.value) {
    return
  }
  const chart = echarts.init(roleChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '45%'],
      data: [
        { value: overview.studentCount || 0, name: '学生', itemStyle: { color: '#409eff' } },
        { value: overview.teacherCount || 0, name: '老师', itemStyle: { color: '#67c23a' } },
        { value: overview.adminCount || 0, name: '管理员', itemStyle: { color: '#909399' } }
      ],
      label: { formatter: '{b}: {c}' }
    }]
  })
  charts.push(chart)
}

function initCollabChart() {
  if (!collabChartRef.value) {
    return
  }
  const chart = echarts.init(collabChartRef.value)
  const passed = overview.passedRegistration || 0
  const pending = Math.max((overview.totalRegistration || 0) - passed, 0)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { bottom: 0 },
    grid: { left: 40, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: {
      type: 'category',
      data: ['对接申请'],
      axisLabel: { fontSize: 12 }
    },
    series: [
      {
        name: '已通过',
        type: 'bar',
        stack: 'total',
        data: [passed],
        itemStyle: { color: '#67c23a' },
        barWidth: 40
      },
      {
        name: '待处理/其它',
        type: 'bar',
        stack: 'total',
        data: [pending],
        itemStyle: { color: '#e6a23c' },
        barWidth: 40
      }
    ]
  })
  charts.push(chart)
}

async function loadAll() {
  loading.value = true
  try {
    await loadOverview()

    let registerData = null
    let articleData = null

    try {
      const res = await getRegisterTrend({ days: 30 })
      registerData = res?.data?.counts || res?.data
    } catch (e) {
      // 接口没实现时给空数据，图表不报错
    }
    try {
      const res = await getArticleTrend({ days: 30 })
      articleData = res?.data?.counts || res?.data
    } catch (e) {
      // 同上
    }

    await nextTick()
    initRegisterChart(Array.isArray(registerData) ? registerData : null)
    initArticleChart(Array.isArray(articleData) ? articleData : null)
    initRoleChart()
    initCollabChart()
  } finally {
    loading.value = false
  }
}

function handleResize() {
  charts.forEach((c) => c?.resize())
}

function goPage(path) {
  router.push(path)
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach((c) => c?.dispose())
  charts = []
})
</script>

<style scoped>
.dashboard-page {
  min-height: 300px;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 10px;
  margin-bottom: 16px;
}

.stat-card-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-body {
  min-width: 0;
}

.stat-value {
  font-size: 21px;
  font-weight: 700;
  color: #1d3a6b;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 3px;
}

.chart-row {
  margin-bottom: 16px;
}

.chart-card {
  border-radius: 10px;
  margin-bottom: 16px;
}

.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d3a6b;
}

.chart-box {
  height: 280px;
  width: 100%;
}

.todo-card {
  position: relative;
  background: #f8f9fb;
  border-radius: 10px;
  padding: 18px 20px;
  cursor: pointer;
  transition: background 0.15s;
}

.todo-card:hover {
  background: #eef2f9;
}

.todo-num {
  font-size: 26px;
  font-weight: 700;
  color: #1d3a6b;
}

.todo-num.warn {
  color: #e6a23c;
}

.todo-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.todo-arrow {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #c0c4cc;
}
</style>
