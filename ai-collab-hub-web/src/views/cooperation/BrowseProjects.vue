<template>
  <div class="browse-page">
    <div class="page-container">
      <!-- 页面标题 + 搜索区 -->
      <div class="browse-head">
        <div class="head-left">
          <h2 class="head-title">浏览合作</h2>
          <p class="head-sub">{{ headSub }}</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="goPublish">填写信息</el-button>
      </div>

      <!-- 搜索筛选条（新增功能） -->
      <el-card class="filter-card" shadow="never">
        <div class="filter-row">
          <el-input
            v-model="query.title"
            placeholder="搜索标题关键词"
            clearable
            class="filter-title"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />

          <el-select v-model="query.typeId" placeholder="内容类型" clearable class="filter-item">
            <el-option
              v-for="it in typeList"
              :key="it.id"
              :label="it.name"
              :value="it.id"
            />
          </el-select>

          <el-select v-model="query.tagId" placeholder="标签" clearable class="filter-item">
            <el-option
              v-for="it in tagList"
              :key="it.id"
              :label="it.tagName"
              :value="it.id"
            />
          </el-select>

          <el-input
            v-model="query.location"
            placeholder="地区/地点"
            clearable
            class="filter-item"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />

          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </div>

        <!-- 快捷标签 -->
        <div class="quick-filter">
          <span class="quick-label">热门标签：</span>
          <el-tag
            v-for="it in tagList.slice(0, 8)"
            :key="it.id"
            :type="query.tagId === it.id ? 'primary' : 'info'"
            effect="plain"
            class="quick-tag"
            @click="toggleTag(it.id)"
          >
            {{ it.tagName }}
          </el-tag>
        </div>
      </el-card>

      <!-- 列表 -->
      <div v-loading="loading" class="list-wrap">
        <template v-if="articleList.length">
          <div
            v-for="item in articleList"
            :key="item.id"
            class="item-card"
            @click="goDetail(item.id)"
          >
            <div class="card-top">
              <div class="card-title-line">
                <el-tag size="small" :type="typeTagType(item.typeId)" effect="plain">
                  {{ item.typeName || '内容' }}
                </el-tag>
                <!-- 老师发布标识：学生要能一眼看出这是老师的课题还是同学的项目 -->
                <el-tag v-if="item.publisherRoleCode === 'TEACHER'" size="small" type="warning" effect="dark">
                  老师发布
                </el-tag>
                <span class="item-card__title">{{ item.title }}</span>
              </div>
              <div class="card-actions">
                <el-button
                  v-if="canApply(item)"
                  type="primary"
                  size="small"
                  @click.stop="goApply(item)"
                >
                  {{ applyText }}
                </el-button>
                <el-tag v-else size="small" type="info" effect="plain">仅可查看</el-tag>

                <!-- 收藏按钮（新增功能） -->
                <el-button
                  size="small"
                  :type="favoriteMap[item.id] ? 'warning' : 'default'"
                  :icon="favoriteMap[item.id] ? StarFilled : Star"
                  circle
                  @click.stop="handleFavorite(item)"
                />
              </div>
            </div>

            <p class="item-card__desc">{{ stripHtml(item.content) }}</p>

            <div class="item-card__meta">
              <span><el-icon><Location /></el-icon> {{ item.location || '地点待定' }}</span>
              <span><el-icon><User /></el-icon> {{ item.publisherName || '未知组长' }}</span>
              <span><el-icon><CollectionTag /></el-icon> {{ item.tagName || '无标签' }}</span>
              <span><el-icon><Clock /></el-icon> {{ formatTime(item.createTime) }}</span>
              <span><el-icon><View /></el-icon> {{ item.viewCount || 0 }} 次浏览</span>
            </div>
          </div>
        </template>

        <el-empty v-else-if="!loading" description="没有找到符合条件的内容" />
      </div>

      <!-- 分页 -->
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

    <!-- 申请对接弹窗 -->
    <el-dialog v-model="applyVisible" :title="applyDialogTitle" width="560px" destroy-on-close>
      <el-alert
        :title="applyAlertTitle"
        type="info"
        :closable="false"
        class="mb-16"
      />
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-position="top">
        <el-form-item label="申请原因" prop="reason">
          <el-input
            v-model="applyForm.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            :placeholder="applyReasonPlaceholder"
          />
        </el-form-item>
        <el-form-item label="联系方式" prop="contactWayValue">
          <el-input v-model="applyForm.contactWayValue" placeholder="微信 / QQ / 电话 / 邮箱，至少留一个" />
        </el-form-item>
        <el-form-item label="联系方式类型" prop="contactWay">
          <el-select v-model="applyForm.contactWay" placeholder="请选择" style="width: 100%">
            <el-option label="微信" value="微信" />
            <el-option label="QQ" value="QQ" />
            <el-option label="电话" value="电话" />
            <el-option label="邮箱" value="邮箱" />
          </el-select>
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
 * 浏览合作页。
 *
 * 相比原系统，这里补上了：
 *  1. 搜索与多条件筛选（标题/类型/标签/地区）—— 原来只能一页页翻
 *  2. 热门标签快捷筛选
 *  3. 收藏按钮
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Plus, Search, RefreshLeft, Star, StarFilled,
  Location, User, CollectionTag, Clock, View
} from '@element-plus/icons-vue'
import {
  getArticleList, getArticleTypeList, getArticleTagList,
  submitRegistration, addFavorite, cancelFavorite, checkFavorite
} from '@/api/blog'
import { getRoleCode, getRoleId } from '@/utils/authSession'

const router = useRouter()

const loading = ref(false)
const articleList = ref([])
const total = ref(0)
const typeList = ref([])
const tagList = ref([])
const favoriteMap = ref({})

const currentRoleCode = computed(() => getRoleCode())
const currentRoleId = computed(() => getRoleId())

const headSub = computed(() => '浏览大家填写的内容，找到合适的点「感兴趣」')

const applyText = computed(() => '感兴趣')

const query = reactive({
  pageNumber: 1,
  pageSize: 10,
  title: '',
  typeId: null,
  tagId: null,
  location: '',
  statusId: 2
})

// ---------- 申请弹窗 ----------
const applyVisible = ref(false)
const applyLoading = ref(false)
const applyFormRef = ref(null)
const applyForm = reactive({
  articleId: null,
  typeId: null,
  reason: '',
  contactWay: '微信',
  contactWayValue: ''
})

const applyDialogTitle = computed(() => '表达兴趣')

const applyAlertTitle = computed(() =>
    '提交后由发布者决定是否通过，通过后系统会自动开通临时沟通通道'
)

const applyReasonPlaceholder = computed(() =>
    '说明你的合作意向、学习目标、能承担的工作'
)

const applyRules = {
  reason: [
    { required: true, message: '请填写申请原因', trigger: 'blur' },
    { min: 10, message: '至少写 10 个字，让对方了解你的来意', trigger: 'blur' }
  ],
  contactWay: [{ required: true, message: '请选择联系方式类型', trigger: 'change' }],
  contactWayValue: [{ required: true, message: '请填写联系方式', trigger: 'blur' }]
}

// ---------- 数据加载 ----------

async function loadDict() {
  try {
    const [typeRes, tagRes] = await Promise.all([
      getArticleTypeList(),
      getArticleTagList()
    ])
    typeList.value = typeRes?.data || []
    tagList.value = tagRes?.data || []
  } catch (e) {
    // 静默
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await getArticleList({
      pageNumber: query.pageNumber,
      pageSize: query.pageSize,
      title: query.title || undefined,
      typeId: query.typeId || undefined,
      tagId: query.tagId || undefined,
      location: query.location || undefined,
      statusId: 2
    })
    const data = res?.data || {}
    articleList.value = data.records || []
    total.value = data.total || 0
    await loadFavoriteState()
  } catch (e) {
    articleList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 批量回显收藏状态 */
async function loadFavoriteState() {
  const map = {}
  const ids = articleList.value.map((it) => it.id)
  for (const id of ids) {
    try {
      const res = await checkFavorite({ targetType: 1, targetId: id })
      map[id] = !!res?.data
    } catch (e) {
      map[id] = false
    }
  }
  favoriteMap.value = map
}

function handleSearch() {
  query.pageNumber = 1
  loadList()
}

function handleReset() {
  query.title = ''
  query.typeId = null
  query.tagId = null
  query.location = ''
  query.pageNumber = 1
  loadList()
}

function toggleTag(tagId) {
  query.tagId = query.tagId === tagId ? null : tagId
  handleSearch()
}

// ---------- 交互 ----------

/**
 * 判断当前用户能不能对这个内容发起意向。
 *
 * 【本次修正】原来是从老平台抄来的规则：
 *   学生只能申请 typeId=2（需求），老师压根没有分支 —— 直接掉到最后的 return false。
 * 结果列表上的按钮，学生对项目看不到、老师全都看不到。
 *
 * 现在改成：只要是别人填的内容，学生和老师都能表达意向。
 */
function canApply(item) {
  const myRole = currentRoleCode.value
  if (!myRole || myRole === 'ADMIN') {
    return false
  }
  // 自己填的不给申请
  if (String(item.userId) === String(getUserIdFromStore())) {
    return false
  }
  return myRole === 'STUDENT' || myRole === 'TEACHER'
}

function getUserIdFromStore() {
  try {
    const raw = localStorage.getItem('ai_collab_user')
    if (!raw) {
      return ''
    }
    const u = JSON.parse(raw)
    return u?.id ?? u?.userId ?? ''
  } catch (e) {
    return ''
  }
}

function goApply(item) {
  applyForm.articleId = item.id
  applyForm.typeId = item.typeId
  applyForm.reason = ''
  applyForm.contactWay = '微信'
  applyForm.contactWayValue = ''
  applyVisible.value = true
}

async function submitApply() {
  const valid = await applyFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  applyLoading.value = true
  try {
    await submitRegistration({
      articleId: applyForm.articleId,
      typeId: applyForm.typeId,
      reason: applyForm.reason,
      contactWay: applyForm.contactWay,
      contactWayValue: applyForm.contactWayValue
    })
    ElMessage.success('申请已提交，等待对方处理')
    applyVisible.value = false
    loadList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    applyLoading.value = false
  }
}

async function handleFavorite(item) {
  const isFav = !!favoriteMap.value[item.id]
  try {
    if (isFav) {
      await cancelFavorite({ targetType: 1, targetId: item.id })
      favoriteMap.value[item.id] = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite({ targetType: 1, targetId: item.id })
      favoriteMap.value[item.id] = true
      ElMessage.success('已收藏，可在「我的收藏」中查看')
    }
  } catch (e) {
    // 拦截器已提示
  }
}

function goDetail(id) {
  router.push(`/cooperation/detail/${id}`)
}

function goPublish() {
  router.push('/cooperation/publish')
}

// ---------- 展示工具 ----------

function typeTagType(typeId) {
  const map = { 1: 'success', 2: 'warning', 3: 'info' }
  return map[typeId] || 'info'
}

function stripHtml(html) {
  if (!html) {
    return '暂无描述'
  }
  const text = String(html).replace(/<[^>]+>/g, '')
  return text.length > 120 ? text.slice(0, 120) + '...' : text
}

function formatTime(t) {
  if (!t) {
    return ''
  }
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  loadDict()
  loadList()
})
</script>

<style scoped>
.browse-page {
  padding: 24px 0 0;
}

.browse-head {
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
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-title {
  flex: 1;
  min-width: 220px;
}

.filter-item {
  width: 160px;
}

.quick-filter {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #e4e7ed;
}

.quick-label {
  font-size: 13px;
  color: #909399;
}

.quick-tag {
  cursor: pointer;
}

.list-wrap {
  min-height: 300px;
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

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.item-card__meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pager-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
