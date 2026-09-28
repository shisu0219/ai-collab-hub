<template>
  <el-card class="opinion-card" shadow="never">
    <div class="opinion-head">
      <div>
        <h3 class="opinion-title">
          答辩意见
          <el-tag v-if="count" size="small" type="info" effect="plain">{{ count }} 条</el-tag>
        </h3>
        <p class="opinion-sub">
          <template v-if="isInsider">
            答辩老师给出的问题和修改意见，组内可见全部内容
          </template>
          <template v-else>
            组外只能看到部分内容，加入小组后可查看完整意见
          </template>
        </p>
      </div>
      <el-button
        v-if="isLeader"
        size="small"
        type="primary"
        link
        @click="openScopeDialog"
      >
        设置可见范围
      </el-button>
    </div>

    <div v-loading="loading" class="opinion-list">
      <template v-if="list.length">
        <div v-for="op in list" :key="op.id" class="opinion-item">
          <div class="item-top">
            <el-tag size="small" :type="typeTag(op.opinionType)" effect="plain">
              {{ op.opinionTypeName }}
            </el-tag>
            <span class="item-title">{{ op.title || '（无标题）' }}</span>
          </div>
          <div class="item-content">{{ op.content }}</div>

          <!-- 组外被截断时的提示 -->
          <div v-if="op.truncated" class="truncate-tip">
            <el-icon><Lock /></el-icon>
            以上为摘要，加入本小组可查看完整内容
          </div>

          <!-- 附件只在有权限时才有地址 -->
          <div v-if="(op.attachments || []).length" class="item-files">
            <span class="files-label">答辩资料：</span>
            <el-link
              v-for="(f, i) in op.attachments"
              :key="i"
              type="primary"
              :href="f"
              target="_blank"
              class="file-link"
            >
              {{ fileName(f) }}
            </el-link>
          </div>

          <div class="item-meta">
            <span v-if="op.source"><el-icon><User /></el-icon> {{ op.source }}</span>
            <span><el-icon><Clock /></el-icon> {{ formatTime(op.createTime) }}</span>
            <span v-if="op.uploaderName">上传人：{{ op.uploaderName }}</span>
            <!-- 当前可见状态，让组长一眼看出这条是不是单独设过 -->
            <el-tag size="small" effect="plain" :type="scopeTagType(op)">
              {{ scopeLabel(op) }}
            </el-tag>
            <!-- 组长：给这一条单独设可见范围 -->
            <el-button
              v-if="isLeader"
              size="small"
              link
              type="primary"
              @click="openScopeDialog(op)"
            >
              单独设置
            </el-button>
          </div>
        </div>
      </template>

      <el-empty v-else-if="!loading" :description="emptyText" />
    </div>

    <div v-if="total > list.length" class="more-bar">
      <el-button size="small" link type="primary" @click="loadMore">加载更多</el-button>
    </div>

    <!-- 组长：可见性设置 -->
    <el-dialog v-model="scopeVisible" :title="scopeDialogTitle" width="520px">
      <p class="scope-desc">
        组员和指导老师永远能看全部；这里设置的是<b>组外人员</b>能看到多少。
        <template v-if="currentOpinion">
          <br />
          正在设置：<b>{{ currentOpinion.title || '（无标题）' }}</b> —— 只影响这一条。
        </template>
      </p>
      <el-radio-group v-model="scopeValue" class="scope-group">
        <el-radio v-for="s in scopeOptions" :key="s.value" :value="s.value" class="scope-radio">
          <div class="scope-label">{{ s.label }}</div>
          <div class="scope-sub">{{ s.desc }}</div>
        </el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="scopeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitScope">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
/**
 * 答辩意见列表组件。
 *
 * 放到小组页和项目详情页都用它。
 *
 * 【重要：不要在前端做可见性判断】
 * 后端已经按登录人身份把内容裁剪过了：
 *   - 不该看的记录根本不返回
 *   - 只能看摘要的，content 已被截断且 truncated=true
 * 这个组件只按 truncated 决定要不要显示那句提示，不做「能不能看」的决策。
 * 前端藏起来的东西，别人直接调接口还是能拿到 —— 过滤必须在后端。
 */
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, User, Clock } from '@element-plus/icons-vue'
import { getOpinionList, getOpinionCount, setOpinionScope } from '@/api/opinion'

const props = defineProps({
  groupId: { type: [Number, String], required: true },
  isLeader: { type: Boolean, default: false },
  members: { type: Array, default: () => [] },
  // 当前查看人是不是组内成员（由父页面根据 group.isMember 传进来，只影响文案）
  isMember: { type: Boolean, default: true },
  // 全组默认可见范围（来自小组的 opinionScope），单条没单独设过时就按这个显示
  defaultScope: { type: [Number, String], default: 1 }
})

const loading = ref(false)
const list = ref([])
const total = ref(0)
const count = ref(0)
const pageNum = ref(1)
const pageSize = 10

const scopeVisible = ref(false)
/** 正在设置的那条意见；null 表示在设全组默认 */
const currentOpinion = ref(null)

/** 可见范围取值 → 名称，和后端约定一致：0仅组内 / 1可见摘要 / 2可见全部 */
const SCOPE_NAMES = { 0: '仅组内可见', 1: '组外可见摘要', 2: '组外可见全部' }
const scopeValue = ref(1)

const scopeOptions = [
  { value: 0, label: '仅组内可见', desc: '组外人员看不到任何答辩意见' },
  { value: 1, label: '组外可见摘要', desc: '组外人员能看到标题、来源和正文前 50 字' },
  { value: 2, label: '组外可见全部', desc: '组外人员能看到完整内容和附件' }
]

const isInsider = computed(() => !!props.isMember)
const emptyText = computed(() =>
  props.isMember ? '还没有答辩意见' : '暂无对外公开的答辩意见'
)

async function load(reset = true) {
  if (reset) {
    pageNum.value = 1
  }
  loading.value = true
  try {
    const res = await getOpinionList({
      groupId: props.groupId,
      pageNum: pageNum.value,
      pageSize
    })
    const data = res?.data || {}
    const records = data.records || []
    list.value = reset ? records : [...list.value, ...records]
    total.value = Number(data.total || records.length)
  } catch (e) {
    if (reset) {
      list.value = []
    }
  } finally {
    loading.value = false
  }
}

async function loadCount() {
  try {
    const res = await getOpinionCount(props.groupId)
    count.value = Number(res?.data || 0)
  } catch (e) {
    count.value = 0
  }
}

function loadMore() {
  pageNum.value += 1
  load(false)
}

/**
 * 打开可见范围设置。
 *
 * 不传参数 = 设置全组的默认可见范围（影响所有没单独设过的意见）
 * 传 op    = 只给这一条单独设置（会覆盖全局默认）
 *
 * 原来只能设全局，单条覆盖后端早就支持了（scope_override），
 * 但前端没入口，等于这个能力用不上。
 */
function openScopeDialog(op = null) {
  currentOpinion.value = op
  // 单条时先回显它现在的值：有自己的覆盖值就用它，没有就用全组默认
  if (op) {
    scopeValue.value = op.scopeOverride != null
        ? Number(op.scopeOverride)
        : Number(props.defaultScope ?? 1)
  } else {
    scopeValue.value = Number(props.defaultScope ?? 1)
  }
  scopeVisible.value = true
}

/** 当前这条意见实际生效的可见范围 */
function effectiveScope(op) {
  return op.scopeOverride != null ? Number(op.scopeOverride) : Number(props.defaultScope ?? 1)
}

function scopeLabel(op) {
  const s = effectiveScope(op)
  const name = SCOPE_NAMES[s] || '未知'
  return op.scopeOverride != null ? `${name}（单独设）` : name
}

function scopeTagType(op) {
  const s = effectiveScope(op)
  return s === 0 ? 'danger' : s === 1 ? 'warning' : 'success'
}

async function submitScope() {
  try {
    await setOpinionScope({
      groupId: Number(props.groupId),
      // 单条设置时带上 id；不传 id = 设全组默认
      opinionId: currentOpinion.value ? Number(currentOpinion.value.id) : null,
      scope: scopeValue.value
    })
    ElMessage.success(currentOpinion.value ? '这条意见的可见范围已更新' : '默认可见范围已更新')
    scopeVisible.value = false
    load()
    loadCount()
  } catch (e) {
    // 拦截器已提示
  }
}

function typeTag(type) {
  const map = { 1: 'danger', 2: 'warning', 3: 'primary' }
  return map[type] || 'info'
}

function fileName(url) {
  if (!url) {
    return '附件'
  }
  const parts = String(url).split('/')
  return decodeURIComponent(parts[parts.length - 1] || '附件')
}

function formatTime(t) {
  if (!t) {
    return ''
  }
  return String(t).replace('T', ' ').slice(0, 16)
}

watch(() => props.groupId, () => {
  if (props.groupId) {
    load()
    loadCount()
  }
})

onMounted(() => {
  if (props.groupId) {
    load()
    loadCount()
  }
})
</script>

<style scoped>
.opinion-card {
  border-radius: 10px;
}

.opinion-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.opinion-title {
  font-size: 16px;
  color: #1d3a6b;
  margin: 0 0 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.opinion-sub {
  font-size: 12px;
  color: #909399;
  margin: 0;
}

.opinion-list {
  min-height: 100px;
}

.opinion-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0f2f5;
}

.opinion-item:last-child {
  border-bottom: none;
}

.item-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.item-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.truncate-tip {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 8px;
  padding: 6px 10px;
  background: #fdf6ec;
  border-radius: 6px;
  font-size: 12px;
  color: #e6a23c;
}

.item-files {
  margin-top: 10px;
  font-size: 13px;
}

.files-label {
  color: #909399;
  font-size: 12px;
}

.file-link {
  margin-right: 12px;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 10px;
  font-size: 12px;
  color: #a0a4ab;
}

.item-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.more-bar {
  text-align: center;
  padding-top: 10px;
}

.scope-desc {
  font-size: 13px;
  color: #606266;
  margin: 0 0 14px;
}

.scope-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
}

.scope-radio {
  height: auto;
  align-items: flex-start;
  padding: 8px 0;
}

.scope-label {
  font-size: 14px;
  color: #303133;
}

.scope-sub {
  font-size: 12px;
  color: #a0a4ab;
  margin-top: 2px;
}
</style>
