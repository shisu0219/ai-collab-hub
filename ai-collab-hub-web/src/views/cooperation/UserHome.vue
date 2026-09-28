<template>
  <div class="user-home-page">
    <div class="page-container">
      <div class="back-row">
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
      </div>

      <div v-loading="loading" class="profile-wrap">
        <template v-if="userInfo">
          <!-- 用户信息卡 -->
          <el-card class="info-card" shadow="never">
            <div class="info-main">
              <el-avatar :size="76" :src="userInfo.avatar">
                {{ (userInfo.nickname || 'U').charAt(0) }}
              </el-avatar>

              <div class="info-detail">
                <div class="name-row">
                  <h2 class="user-name">{{ userInfo.nickname || '未知用户' }}</h2>
                  <el-tag :type="roleTagType" effect="plain">{{ roleLabel }}</el-tag>
                  <el-tag v-if="userInfo.auditStatus === 1" type="success" effect="plain" size="small">
                    已认证
                  </el-tag>
                </div>

                <div v-if="userInfo.location" class="sub-line">
                  <el-icon><Location /></el-icon>
                  <span>{{ userInfo.location }}</span>
                </div>

                <p v-if="userInfo.description" class="desc-text">{{ userInfo.description }}</p>
              </div>

              <div class="info-actions">
                <el-button
                  :type="favorited ? 'warning' : 'default'"
                  :icon="favorited ? StarFilled : Star"
                  @click="handleFavorite"
                >
                  {{ favorited ? '已关注' : '关注' }}
                </el-button>
              </div>
            </div>

            <!-- 数据统计 -->
            <div class="stat-row">
              <div class="stat-item">
                <div class="stat-num">{{ articleTotal }}</div>
                <div class="stat-label">填写内容</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ joinDays }}</div>
                <div class="stat-label">入驻天数</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ userInfo.email ? '已填' : '未填' }}</div>
                <div class="stat-label">联系方式</div>
              </div>
            </div>
          </el-card>

          <!-- TA 填写的内容（新增功能：原来只能看到零散信息） -->
          <el-card class="article-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">TA 填写的内容</span>
                <el-radio-group v-model="query.typeId" size="small" @change="handleFilter">
                  <el-radio-button :value="null">全部</el-radio-button>
                  <el-radio-button :value="1">项目</el-radio-button>
                  <el-radio-button :value="2">需求</el-radio-button>
                </el-radio-group>
              </div>
            </template>

            <div v-loading="articleLoading" class="article-list">
              <template v-if="articleList.length">
                <div
                  v-for="item in articleList"
                  :key="item.id"
                  class="item-card"
                  @click="goDetail(item.id)"
                >
                  <div class="item-title-line">
                    <el-tag size="small" :type="typeTagType(item.typeId)" effect="plain">
                      {{ item.typeName || '内容' }}
                    </el-tag>
                    <span class="item-card__title">{{ item.title }}</span>
                  </div>
                  <p class="item-card__desc">{{ stripHtml(item.content) }}</p>
                  <div class="item-card__meta">
                    <span><el-icon><Location /></el-icon> {{ item.location || '待定' }}</span>
                    <span><el-icon><Clock /></el-icon> {{ formatTime(item.createTime) }}</span>
                  </div>
                </div>
              </template>
              <el-empty v-else-if="!articleLoading" description="TA 还没有填写内容" />
            </div>

            <div v-if="articleTotal > articleList.length" class="more-row">
              <el-button text type="primary" @click="loadMoreArticles">
                加载更多（共 {{ articleTotal }} 条）
              </el-button>
            </div>
          </el-card>
        </template>

        <el-empty v-else-if="!loading" description="用户不存在" />
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 个人主页（新增功能）。
 *
 * 原来平台点开用户只能看到基础信息，看不到这个人发过什么。
 * 这里把 TA 填写的内容聚合起来，另外能关注。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Star, StarFilled, Location, Clock
} from '@element-plus/icons-vue'
import { getUserInfoById } from '@/api/user'
import { getUserArticleList, addFavorite, cancelFavorite, checkFavorite } from '@/api/blog'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const articleLoading = ref(false)
const userInfo = ref(null)
const articleList = ref([])
const articleTotal = ref(0)
const favorited = ref(false)

const query = reactive({
  pageNumber: 1,
  pageSize: 10,
  typeId: null
})

const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', STUDENT: '学生', TEACHER: '老师' }
  return map[userInfo.value?.roleCode] || '用户'
})

const roleTagType = computed(() => {
  const map = { ADMIN: 'danger', STUDENT: 'primary', TEACHER: 'warning' }
  return map[userInfo.value?.roleCode] || 'info'
})

const joinDays = computed(() => {
  const t = userInfo.value?.createTime
  if (!t) {
    return '—'
  }
  const start = new Date(String(t).replace('T', ' ').replace(/-/g, '/'))
  const diff = Date.now() - start.getTime()
  const days = Math.floor(diff / 86400000)
  return days > 0 ? days : 1
})

async function loadUser() {
  loading.value = true
  try {
    const res = await getUserInfoById(route.params.id)
    userInfo.value = res?.data || null
    if (userInfo.value) {
      await Promise.all([loadFavoriteState(), loadArticles()])
    }
  } catch (e) {
    userInfo.value = null
  } finally {
    loading.value = false
  }
}

async function loadFavoriteState() {
  try {
    const res = await checkFavorite({ targetType: 2, targetId: route.params.id })
    favorited.value = !!res?.data
  } catch (e) {
    favorited.value = false
  }
}

async function loadArticles() {
  articleLoading.value = true
  try {
    const res = await getUserArticleList({
      userId: route.params.id,
      pageNumber: query.pageNumber,
      pageSize: query.pageSize,
      typeId: query.typeId || undefined
    })
    const data = res?.data || {}
    const records = data.records || []
    if (query.pageNumber === 1) {
      articleList.value = records
    } else {
      articleList.value = articleList.value.concat(records)
    }
    articleTotal.value = data.total || 0
  } catch (e) {
    articleList.value = []
    articleTotal.value = 0
  } finally {
    articleLoading.value = false
  }
}

function handleFilter() {
  query.pageNumber = 1
  loadArticles()
}

function loadMoreArticles() {
  query.pageNumber += 1
  loadArticles()
}

async function handleFavorite() {
  try {
    if (favorited.value) {
      await cancelFavorite({ targetType: 2, targetId: route.params.id })
      favorited.value = false
      ElMessage.success('已取消关注')
    } else {
      await addFavorite({ targetType: 2, targetId: route.params.id })
      favorited.value = true
      ElMessage.success('已关注')
    }
  } catch (e) {
    // 拦截器已提示
  }
}

function goDetail(id) {
  router.push(`/cooperation/detail/${id}`)
}

function goBack() {
  router.back()
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
  return text.length > 100 ? text.slice(0, 100) + '...' : text
}

function formatTime(t) {
  if (!t) {
    return ''
  }
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(loadUser)
</script>

<style scoped>
.user-home-page {
  padding: 24px 0;
}

.back-row {
  margin-bottom: 14px;
}

.profile-wrap {
  min-height: 300px;
}

.info-card {
  border-radius: 10px;
  margin-bottom: 18px;
}

.info-main {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.info-detail {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.user-name {
  font-size: 21px;
  color: #1d3a6b;
  margin: 0;
}

.sub-line {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  margin-top: 8px;
}

.sub-line .sep {
  color: #dcdfe6;
}

.desc-text {
  font-size: 13px;
  color: #909399;
  line-height: 1.8;
  margin: 12px 0 0;
}

.info-actions {
  flex-shrink: 0;
}

.stat-row {
  display: flex;
  justify-content: space-around;
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px dashed #e4e7ed;
}

.stat-item {
  text-align: center;
}

.stat-num {
  font-size: 22px;
  font-weight: 600;
  color: #1d3a6b;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.article-card {
  border-radius: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d3a6b;
}

.article-list {
  min-height: 200px;
}

.item-title-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-title-line .item-card__title {
  margin-bottom: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-card__meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.more-row {
  text-align: center;
  margin-top: 14px;
}

@media (max-width: 768px) {
  .info-main {
    flex-direction: column;
  }
}
</style>
