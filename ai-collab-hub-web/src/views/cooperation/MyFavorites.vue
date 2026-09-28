<template>
  <div class="favorites-page">
    <div class="page-container">
      <h2 class="head-title">我的收藏</h2>
      <p class="head-sub">收藏的内容都在这里，方便随时回看</p>

      <el-tabs v-model="activeType" @tab-change="loadList">
        <el-tab-pane label="收藏的内容" name="article" />
        <el-tab-pane label="关注的用户" name="user" />
      </el-tabs>

      <div v-loading="loading" class="fav-list">
        <template v-if="activeType === 'article' && articleList.length">
          <div
            v-for="item in articleList"
            :key="item.id"
            class="item-card"
            @click="goDetail(item.targetId)"
          >
            <div class="card-top">
              <div class="card-title-line">
                <el-tag size="small" :type="typeTagType(item.typeId)" effect="plain">
                  {{ item.typeName || '内容' }}
                </el-tag>
                <span class="item-card__title">{{ item.title }}</span>
              </div>
              <el-button
                size="small"
                type="warning"
                :icon="StarFilled"
                @click.stop="handleCancel(item)"
              >
                取消收藏
              </el-button>
            </div>
            <div class="item-card__meta">
              <span><el-icon><Clock /></el-icon> 收藏于 {{ formatTime(item.createTime) }}</span>
            </div>
          </div>
        </template>

        <template v-else-if="activeType === 'user' && userList.length">
          <div
            v-for="item in userList"
            :key="item.id"
            class="item-card"
            @click="goUserHome(item.targetId)"
          >
            <div class="card-top">
              <div class="user-line">
                <el-avatar :size="40">{{ (item.nickname || 'U').charAt(0) }}</el-avatar>
                <div>
                  <div class="item-card__title">{{ item.nickname || '未知用户' }}</div>
                  <div class="text-sub">{{ item.roleName || '' }}</div>
                </div>
              </div>
              <el-button
                size="small"
                type="warning"
                :icon="StarFilled"
                @click.stop="handleCancel(item)"
              >
                取消关注
              </el-button>
            </div>
          </div>
        </template>

        <el-empty v-else-if="!loading" description="还没有收藏任何内容" />
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 我的收藏页（新增功能）。
 * 学生看到感兴趣的同学/老师可以关注，不用截图存微信了。
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { StarFilled, Clock } from '@element-plus/icons-vue'
import { getFavoriteList, cancelFavorite } from '@/api/blog'

const router = useRouter()

const activeType = ref('article')
const loading = ref(false)
const articleList = ref([])
const userList = ref([])

async function loadList() {
  loading.value = true
  try {
    const targetType = activeType.value === 'article' ? 1 : 2
    const res = await getFavoriteList({
      targetType,
      pageNum: 1,
      pageSize: 100
    })
    const records = res?.data?.records || res?.data || []
    if (activeType.value === 'article') {
      articleList.value = records
    } else {
      userList.value = records
    }
  } catch (e) {
    articleList.value = []
    userList.value = []
  } finally {
    loading.value = false
  }
}

async function handleCancel(item) {
  try {
    const targetType = activeType.value === 'article' ? 1 : 2
    await cancelFavorite({ targetType, targetId: item.targetId })
    ElMessage.success('已取消')
    loadList()
  } catch (e) {
    // 拦截器已提示
  }
}

function goDetail(id) {
  router.push(`/cooperation/detail/${id}`)
}

function goUserHome(id) {
  router.push(`/cooperation/user/${id}`)
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

onMounted(loadList)
</script>

<style scoped>
.favorites-page {
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
  margin: 0 0 14px;
}

.fav-list {
  min-height: 240px;
  padding-top: 10px;
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-card__meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
