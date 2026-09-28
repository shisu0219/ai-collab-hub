<template>
  <div class="opinion-admin">
    <div class="head">
      <div>
        <h2 class="head-title">答辩意见管理</h2>
        <p class="head-sub">上传答辩时老师方/专业方给出的问题和意见，也可以把整理好的表格批量导入</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Download" @click="downloadTemplate">下载导入模板</el-button>
        <el-button type="primary" :icon="Upload" @click="openImport">批量导入</el-button>
        <el-button type="success" :icon="Plus" @click="openForm()">单条添加</el-button>
      </div>
    </div>

    <!-- 筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true">
        <el-form-item label="小组">
          <el-select
            v-model="query.groupId"
            placeholder="选择小组"
            clearable
            filterable
            style="width: 240px"
            @change="load(1)"
          >
            <el-option
              v-for="g in groups"
              :key="g.id"
              :label="g.name + (g.leaderName ? '（' + g.leaderName + '）' : '')"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never" class="list-card">
      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.opinionType)" effect="plain">
              {{ row.opinionTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="groupName" label="小组" width="150" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="120" show-overflow-tooltip />
        <el-table-column label="正文" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="content-preview">{{ row.content || '（仅附件）' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="资料" width="80">
          <template #default="{ row }">
            <span v-if="(row.attachments || []).length">{{ row.attachments.length }} 个</span>
            <span v-else class="text-sub">—</span>
          </template>
        </el-table-column>
        <el-table-column label="可见范围" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="scopeTag(row.effectiveScope)">
              {{ scopeName(row.effectiveScope) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDetail(row)">查看</el-button>
            <el-button size="small" type="primary" link @click="openForm(row)">编辑</el-button>
            <el-button size="small" type="warning" link @click="openScope(row)">可见性</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="还没有答辩意见" />

      <div v-if="total > 0" class="pager">
        <el-pagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </el-card>

    <!-- 单条 添加/编辑 -->
    <el-dialog
      v-model="formVisible"
      :title="form.id ? '编辑答辩意见' : '添加答辩意见'"
      width="640px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="96px">
        <el-form-item label="所属小组" prop="groupId">
          <el-select v-model="form.groupId" placeholder="选择小组" filterable style="width: 100%">
            <el-option
              v-for="g in groups"
              :key="g.id"
              :label="g.name + (g.leaderName ? '（' + g.leaderName + '）' : '')"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.opinionType">
            <el-radio :value="1">答辩问题</el-radio>
            <el-radio :value="2">答辩意见</el-radio>
            <el-radio :value="3">修改建议</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="200" placeholder="如：系统架构合理性存疑" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="答辩老师的原话或整理后的表述"
          />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" maxlength="200" placeholder="如：张老师、校外专家，可留空" />
        </el-form-item>
        <el-form-item label="答辩资料">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="true"
            :limit="5"
            :file-list="fileList"
            :on-success="handleUploadSuccess"
            :on-remove="handleUploadRemove"
          >
            <el-button :icon="Upload">上传文档 / 表格</el-button>
            <template #tip>
              <div class="form-tip">可以只传文件不写正文，也可以都不传只记一条问题</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="可见范围">
          <el-select v-model="form.scopeOverride" clearable placeholder="跟随小组默认" style="width: 100%">
            <el-option label="跟随小组默认设置" :value="null" />
            <el-option v-for="s in scopeOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 -->
    <el-dialog v-model="importVisible" title="批量导入答辩意见" width="640px">
      <el-steps :active="importStep" simple class="import-steps">
        <el-step title="下载模板并整理好数据" />
        <el-step title="上传文件" />
        <el-step title="确认导入" />
      </el-steps>

      <el-form label-width="96px" class="import-form">
        <el-form-item label="导入到">
          <el-select v-model="importGroupId" placeholder="选择小组" filterable style="width: 100%">
            <el-option
              v-for="g in groups"
              :key="g.id"
              :label="g.name + (g.leaderName ? '（' + g.leaderName + '）' : '')"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据文件">
          <input
            ref="fileInputRef"
            type="file"
            accept=".csv,.txt"
            class="file-input"
            @change="handleFilePick"
          />
          <div class="form-tip">
            把表格另存为 <b>CSV（逗号分隔）</b> 再上传。
            列顺序：类型 / 标题 / 正文 / 来源 —— 第一行表头会被自动跳过。
          </div>
        </el-form-item>
      </el-form>

      <template v-if="parsedRows.length">
        <div class="preview-head">解析到 {{ parsedRows.length }} 条，确认无误后导入：</div>
        <el-table :data="parsedRows.slice(0, 8)" size="small" max-height="260">
          <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
          <el-table-column prop="content" label="正文" min-width="180" show-overflow-tooltip />
          <el-table-column prop="source" label="来源" width="100" />
        </el-table>
        <div v-if="parsedRows.length > 8" class="preview-more">
          仅预览前 8 条，其余 {{ parsedRows.length - 8 }} 条也会一并导入
        </div>
      </template>

      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!parsedRows.length || !importGroupId"
          :loading="saving"
          @click="submitImport"
        >
          确认导入 {{ parsedRows.length }} 条
        </el-button>
      </template>
    </el-dialog>

    <!-- 可见性设置 -->
    <el-dialog v-model="scopeVisible" title="设置可见范围" width="520px">
      <p class="scope-desc">
        组员和指导老师永远能看全部；这里设置的是<b>组外人员</b>能看到多少。
        单条设置会覆盖小组的默认值。
      </p>
      <el-radio-group v-model="scopeForm.scope" class="scope-group">
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

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="答辩意见详情" width="640px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="小组">{{ detail.groupName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.opinionTypeName }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ detail.title || '—' }}</el-descriptions-item>
        <el-descriptions-item label="正文">
          <div class="detail-content">{{ detail.content || '（无正文，仅附件）' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="来源">{{ detail.source || '—' }}</el-descriptions-item>
        <el-descriptions-item label="答辩资料">
          <template v-if="(detail.attachments || []).length">
            <el-link
              v-for="(f, i) in detail.attachments"
              :key="i"
              type="primary"
              :href="f"
              target="_blank"
              class="file-link"
            >
              {{ fileName(f) }}
            </el-link>
          </template>
          <span v-else class="text-sub">无</span>
        </el-descriptions-item>
        <el-descriptions-item label="可见范围">
          {{ scopeName(detail.effectiveScope) }}
        </el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 答辩意见管理页（管理员）。
 *
 * 两种录入方式：
 *   1. 单条添加 —— 顺手记一条问题
 *   2. 批量导入 —— 把答辩记录整理成表格，另存为 CSV 上传，一次导入一批
 *
 * 导入用 CSV 而不是 xlsx：前端不用引第三方解析库（xlsx 包体积很大），
 * 而 Excel / WPS 都能「另存为 CSV」，管理员操作成本几乎一样。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search } from '@element-plus/icons-vue'
import {
  getOpinionList,
  saveOpinion,
  importOpinions,
  deleteOpinion,
  setOpinionScope
} from '@/api/opinion'
import { adminListGroups } from '@/api/group'
import { getToken } from '@/utils/authSession'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const groups = ref([])

const query = reactive({ groupId: null, pageNum: 1, pageSize: 10 })

const formVisible = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  groupId: null,
  opinionType: 1,
  title: '',
  content: '',
  source: '',
  scopeOverride: null,
  attachments: []
})
const fileList = ref([])

const importVisible = ref(false)
const importGroupId = ref(null)
const parsedRows = ref([])
const fileInputRef = ref()
const importStep = ref(1)

const scopeVisible = ref(false)
const scopeForm = reactive({ opinionId: null, groupId: null, scope: 1 })

const detailVisible = ref(false)
const detail = ref(null)

const formRules = {
  groupId: [{ required: true, message: '请选择所属小组', trigger: 'change' }]
}

const scopeOptions = [
  { value: 0, label: '仅组内可见', desc: '组外人员看不到任何答辩意见' },
  { value: 1, label: '组外可见摘要', desc: '组外人员能看到标题、来源和正文前 50 字' },
  { value: 2, label: '组外可见全部', desc: '组外人员能看到完整内容和附件' }
]

const uploadUrl = '/api/blog/article/upload'
const headKey = 'Authorization'
const uploadHeaders = { [headKey]: 'Bearer ' + getToken() }

async function loadGroups() {
  try {
    const res = await adminListGroups()
    groups.value = res?.data || []
  } catch (e) {
    groups.value = []
  }
}

async function load(page) {
  if (page) {
    query.pageNum = page
  }
  if (!query.groupId) {
    // 没选小组就按全部小组分别查太费劲，直接提示先选
    list.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await getOpinionList({
      groupId: query.groupId,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    const data = res?.data || {}
    list.value = data.records || []
    total.value = Number(data.total || list.value.length)
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  if (row) {
    form.id = row.id
    form.groupId = row.groupId
    form.opinionType = row.opinionType
    form.title = row.title
    form.content = row.content
    form.source = row.source
    form.scopeOverride = row.effectiveScope
    form.attachments = row.attachments || []
    fileList.value = (row.attachments || []).map((u, i) => ({
      name: fileName(u),
      url: u,
      uid: -(i + 1)
    }))
  } else {
    form.id = null
    form.groupId = query.groupId
    form.opinionType = 1
    form.title = ''
    form.content = ''
    form.source = ''
    form.scopeOverride = null
    form.attachments = []
    fileList.value = []
  }
  formVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!form.content && !form.attachments.length) {
    ElMessage.warning('请填写正文或上传答辩资料，至少要有一样')
    return
  }
  saving.value = true
  try {
    await saveOpinion({
      id: form.id,
      groupId: Number(form.groupId),
      articleId: null,
      opinionType: form.opinionType,
      title: form.title,
      content: form.content,
      source: form.source,
      scopeOverride: form.scopeOverride,
      attachments: form.attachments
    })
    ElMessage.success('保存成功，组内成员会收到通知')
    formVisible.value = false
    load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

function handleUploadSuccess(res) {
  const url = res?.data?.url || res?.data?.fileUrl || res?.data?.path
  if (url) {
    form.attachments.push(url)
    ElMessage.success('上传成功')
  } else {
    ElMessage.warning('上传成功但没拿到文件地址，请检查后端返回结构')
  }
}

function handleUploadRemove(file) {
  const url = file.url || file.response?.data?.url
  form.attachments = form.attachments.filter((u) => u !== url)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除这条答辩意见吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteOpinion(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function openScope(row) {
  scopeForm.opinionId = row.id
  scopeForm.groupId = row.groupId
  scopeForm.scope = row.effectiveScope ?? 1
  scopeVisible.value = true
}

async function submitScope() {
  try {
    await setOpinionScope({
      groupId: Number(scopeForm.groupId),
      opinionId: scopeForm.opinionId,
      scope: scopeForm.scope
    })
    ElMessage.success('设置成功')
    scopeVisible.value = false
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

// ==================== 批量导入 ====================

function openImport() {
  parsedRows.value = []
  importGroupId.value = query.groupId
  importStep.value = 1
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
  importVisible.value = true
}

function downloadTemplate() {
  const header = '类型,标题,正文,来源'
  const sample = '答辩问题,系统架构合理性存疑,请说明为什么选择当前的技术架构，以及高并发场景下的应对方案,张老师'
  const sample2 = '修改建议,补充测试数据,建议补充系统在真实数据量下的性能测试结果,校外专家'
  // 加 BOM，否则 Excel 打开中文会乱码
  const content = '\ufeff' + [header, sample, sample2].join('\r\n')
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '答辩意见导入模板.csv'
  a.click()
  URL.revokeObjectURL(a.href)
}

function handleFilePick(e) {
  const file = e.target.files?.[0]
  if (!file) {
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    const text = String(reader.result || '').replace(/^\ufeff/, '')
    parsedRows.value = parseCsv(text)
    importStep.value = parsedRows.value.length ? 2 : 1
    if (!parsedRows.value.length) {
      ElMessage.warning('没解析到数据，请检查文件格式')
    }
  }
  reader.readAsText(file, 'UTF-8')
}

/**
 * 解析 CSV。用最朴素的方式处理引号包裹的字段 ——
 * 管理员的表格里一般不会出现复杂嵌套，够用。第一行表头跳过。
 */
function parseCsv(text) {
  const lines = String(text).split(/\r?\n/).filter((l) => l.trim())
  if (lines.length <= 1) {
    return []
  }
  const typeMap = { 答辩问题: 1, 答辩意见: 2, 修改建议: 3 }
  const rows = []
  for (let i = 1; i < lines.length; i++) {
    const cells = splitCsvLine(lines[i])
    const [typeRaw, title, content, source] = cells
    if (!String(title || '').trim() && !String(content || '').trim()) {
      continue
    }
    rows.push({
      opinionType: typeMap[String(typeRaw || '').trim()] || 1,
      title: String(title || '').trim(),
      content: String(content || '').trim(),
      source: String(source || '').trim()
    })
  }
  return rows
}

function splitCsvLine(line) {
  const out = []
  let cur = ''
  let inQuote = false
  for (let i = 0; i < line.length; i++) {
    const c = line[i]
    if (c === '"') {
      if (inQuote && line[i + 1] === '"') {
        cur += '"'
        i++
      } else {
        inQuote = !inQuote
      }
    } else if (c === ',' && !inQuote) {
      out.push(cur)
      cur = ''
    } else {
      cur += c
    }
  }
  out.push(cur)
  return out
}

async function submitImport() {
  if (!importGroupId.value) {
    ElMessage.warning('请选择导入到哪个小组')
    return
  }
  saving.value = true
  try {
    const res = await importOpinions(Number(importGroupId.value), parsedRows.value)
    const ok = res?.data?.count ?? parsedRows.value.length
    ElMessage.success(`成功导入 ${ok} 条，组内成员会收到通知`)
    importVisible.value = false
    parsedRows.value = []
    query.groupId = importGroupId.value
    load(1)
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

// ==================== 显示工具 ====================

function typeTag(t) {
  const map = { 1: 'danger', 2: 'warning', 3: 'primary' }
  return map[t] || 'info'
}

function scopeTag(s) {
  const map = { 0: 'info', 1: 'warning', 2: 'success' }
  return map[s] || 'info'
}

function scopeName(s) {
  const map = { 0: '仅组内', 1: '可见摘要', 2: '可见全部' }
  return map[s] || '—'
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

onMounted(async () => {
  await loadGroups()
  if (groups.value.length) {
    query.groupId = groups.value[0].id
    load(1)
  }
})
</script>

<style scoped>
.opinion-admin {
  padding: 4px;
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.head-title {
  font-size: 20px;
  color: #1d3a6b;
  margin: 0 0 4px;
}

.head-sub {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.filter-card {
  margin-bottom: 14px;
  border-radius: 10px;
}

.list-card {
  border-radius: 10px;
}

.content-preview {
  color: #606266;
  font-size: 13px;
}

.text-sub {
  color: #a0a4ab;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

.form-tip {
  font-size: 12px;
  color: #a0a4ab;
  line-height: 1.6;
}

.import-steps {
  margin-bottom: 18px;
}

.import-form {
  margin-bottom: 8px;
}

.file-input {
  font-size: 13px;
}

.preview-head {
  font-size: 13px;
  color: #606266;
  margin: 8px 0 10px;
}

.preview-more {
  font-size: 12px;
  color: #a0a4ab;
  margin-top: 8px;
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

.detail-content {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.file-link {
  margin-right: 12px;
}
</style>
