<template>
  <div class="publish-page">
    <div class="page-container">
      <div class="publish-head">
        <div>
          <h2 class="head-title">{{ pageTitle }}</h2>
          <p class="head-sub">{{ pageSub }}</p>
        </div>
        <el-button :icon="Back" @click="goBack">返回</el-button>
      </div>

      <el-card class="form-card" shadow="never">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="110px"
          label-position="right"
        >
          <!-- 所属小组：学生必填（项目要挂在组下面）；老师发的是项目想法，不强制建组 -->
          <template v-if="!isTeacher">
          <el-divider content-position="left">所属小组</el-divider>

          <el-form-item label="小组">
            <template v-if="groupLoading">
              <span class="field-tip">正在加载小组信息…</span>
            </template>
            <template v-else-if="myGroup">
              <el-tag type="success" effect="plain">{{ myGroup.name }}</el-tag>
              <span class="group-hint">
                组长：{{ myGroup.leaderName || '—' }} ·
                成员 {{ (myGroup.members || []).length }} 人
              </span>
            </template>
            <template v-else>
              <el-alert type="warning" :closable="false" class="no-group-alert">
                <template #title>
                  你还没有小组，项目要挂在小组下面
                </template>
                <el-button size="small" type="primary" @click="goCreateGroup">
                  去创建小组
                </el-button>
              </el-alert>
            </template>
          </el-form-item>

          </template>

          <el-divider content-position="left">基础信息</el-divider>

          <el-form-item label="标题" prop="title">
            <el-input
              v-model="form.title"
              :placeholder="titlePlaceholder"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="类型" prop="typeId">
            <el-radio-group v-model="form.typeId" :disabled="!canChooseType">
              <el-radio-button v-for="it in typeList" :key="it.id" :value="it.id">
                {{ it.name }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="地区/地点" prop="location">
            <el-input v-model="form.location" placeholder="例如：上海、金山区、线上远程" />
          </el-form-item>

          <el-form-item label="标签" prop="tagId">
            <el-select
              v-model="form.tagId"
              placeholder="选择标签（来源后台标签库）"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="it in tagList"
                :key="it.id"
                :label="it.tagName"
                :value="it.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="详细描述" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="10"
              maxlength="3000"
              show-word-limit
              :placeholder="contentPlaceholder"
            />
            <div class="field-tip">
              {{ contentTip }}
            </div>
          </el-form-item>

          <el-form-item label="附件">
            <el-upload
              v-model:file-list="fileList"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :limit="10"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :on-remove="handleUploadRemove"
              :before-upload="beforeUpload"
              multiple
            >
              <el-button :icon="Upload">选择文件</el-button>
              <template #tip>
                <div class="field-tip">
                  支持 doc/docx/xls/xlsx/ppt/pptx/pdf/zip/rar/图片等，单个文件最大 100MB，最多 10 个。
                </div>
              </template>
            </el-upload>
          </el-form-item>

          <!-- 项目类字段 -->
          <template v-if="form.typeId === 1">
            <el-divider content-position="left">项目信息</el-divider>
            <el-form-item label="项目预算" prop="budget">
              <el-input v-model="form.budget" placeholder="例如：5000-10000 元，或「面议」" />
            </el-form-item>
            <el-form-item label="起止日期" prop="dateRange">
              <el-date-picker
                v-model="form.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </template>

          <!-- 需求类字段 -->
          <template v-if="form.typeId === 2">
            <el-divider content-position="left">需求信息</el-divider>
            <el-form-item label="需求预算" prop="budget">
              <el-input v-model="form.budget" placeholder="例如：3000 元，或「面议」" />
            </el-form-item>
            <el-form-item label="紧急程度" prop="urgencyLevel">
              <el-radio-group v-model="form.urgencyLevel">
                <el-radio-button value="普通">普通</el-radio-button>
                <el-radio-button value="较急">较急</el-radio-button>
                <el-radio-button value="紧急">紧急</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="期望完成日期" prop="expectedDeadline">
              <el-date-picker
                v-model="form.expectedDeadline"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </template>

          <el-form-item class="submit-row">
            <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
              {{ isEdit ? '保存修改' : '立即提交' }}
            </el-button>
            <el-button size="large" @click="handleSaveDraft">存草稿</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
/**
 * 填写 / 编辑信息页。
 *
 * 学生发「项目」，老师发「需求」，字段不一样，靠 typeId 切换。
 * 描述框加了字数提示，引导按模板写，减少管理员退回。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Upload } from '@element-plus/icons-vue'
import {
  getArticleTypeList, getArticleTagList,
  publishArticle, updateArticle, getArticleDetail
} from '@/api/blog'
import { getRoleCode, getToken } from '@/utils/authSession'
import { getMyGroups, getGroupDetail, bindArticleToGroup } from '@/api/group'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const submitting = ref(false)
const typeList = ref([])
const tagList = ref([])
const fileList = ref([])

const currentRoleCode = computed(() => getRoleCode())
const isEdit = computed(() => !!route.query.id)

const canChooseType = ref(true)

/** 我的小组。一个小组只能有一个项目，所以这里最多一条。 */
const myGroup = ref(null)
const groupLoading = ref(false)

const isTeacher = computed(() => currentRoleCode.value === 'TEACHER')

const pageTitle = computed(() => {
  if (isEdit.value) {
    return '编辑内容'
  }
  return isTeacher.value ? '填写需求' : '填写项目'
})

const pageSub = computed(() => {
  if (isEdit.value) {
    return '修改后需要重新提交审核'
  }
  if (isTeacher.value) {
    return '把你的课题或项目想法写出来，有兴趣的学生会来探讨和申请'
  }
  return '把你的项目写清楚，同学和老师才好判断怎么参与'
})

const titlePlaceholder = computed(() =>
    isTeacher.value
        ? '例如：招聘小程序开发兼职（3人）'
        : '例如：校园二手交易平台开发（前后端各1人）'
)

const contentPlaceholder = computed(() => {
  if (isTeacher.value) {
    return '按以下三点写清楚：\n① 需要什么技能或背景\n② 工作内容是什么\n③ 希望什么时候完成'
  }
  return '按以下四点写清楚：\n① 项目背景和目标\n② 技术栈或要求\n③ 分工方式\n④ 预期成果'
})

const contentTip = computed(() =>
    isTeacher.value
        ? '建议包含：① 需要什么技能或背景 ② 工作内容是什么 ③ 希望什么时候完成'
        : '建议包含：① 项目背景和目标 ② 技术栈或要求 ③ 分工方式 ④ 预期成果'
)

const uploadHeaders = computed(() => {
  const headerName = 'Authorization'
  return { [headerName]: 'Bearer ' + getToken() }
})

const form = reactive({
  id: null,
  groupId: null,
  title: '',
  typeId: 1,
  location: '',
  tagId: null,
  content: '',
  attachments: '',
  budget: '',
  dateRange: [],
  urgencyLevel: '普通',
  expectedDeadline: null
})

const rules = computed(() => ({
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, message: '标题至少 5 个字', trigger: 'blur' }
  ],
  typeId: [{ required: true, message: '请选择类型', trigger: 'change' }],
  location: [{ required: true, message: '请填写地区/地点', trigger: 'blur' }],
  content: [
    { required: true, message: '请填写详细描述', trigger: 'blur' },
    { min: 30, message: '描述至少 30 个字，写清楚对方才好判断', trigger: 'blur' }
  ],
  urgencyLevel: form.typeId === 2
      ? [{ required: true, message: '请选择紧急程度', trigger: 'change' }]
      : []
}))

function beforeUpload(file) {
  const maxSize = 100 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('单个文件不能超过 100MB')
    return false
  }
  const ext = file.name.split('.').pop().toLowerCase()
  const allow = [
    'bmp', 'gif', 'jpg', 'jpeg', 'png', 'svg', 'tif', 'webp',
    'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',
    'csv', 'htm', 'html', 'xml', 'mdb', 'zip', 'rar'
  ]
  if (!allow.includes(ext)) {
    ElMessage.error(`不支持的文件格式：.${ext}`)
    return false
  }
  return true
}

function handleUploadSuccess(res, file) {
  if (res?.code === 200) {
    file.url = res.data?.url || res.data
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(res?.msg || '上传失败')
  }
}

function handleUploadError() {
  ElMessage.error('上传失败，请重试')
}

function handleUploadRemove(file) {
  // 前端移除即可，提交时不带这个地址
}

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

async function loadDetail() {
  if (!route.query.id) {
    return
  }
  try {
    const res = await getArticleDetail(route.query.id)
    const d = res?.data || {}
    form.id = d.id
    form.title = d.title || ''
    form.typeId = d.typeId || 1
    form.groupId = d.groupId || null
    form.location = d.location || ''
    form.tagId = d.tagId || null
    form.content = d.content || ''
    form.budget = d.budget || ''
    form.urgencyLevel = d.urgencyLevel || '普通'
    form.expectedDeadline = d.expectedDeadline || null
    if (d.startDate && d.endDate) {
      form.dateRange = [d.startDate, d.endDate]
    }
    // 已提交过的内容不允许改类型
    canChooseType.value = false
  } catch (e) {
    // 拦截器已提示
  }
}

/**
 * 加载我的小组，并自动带出要绑定的 groupId。
 *
 * 一个小组只能有一个项目，所以：
 *   - 已有项目的小组，进来是「编辑」模式（后端会按 id 查出来）
 *   - 没有项目的小组，提交后由后端绑定
 */
async function loadMyGroup() {
  groupLoading.value = true
  try {
    const res = await getMyGroups()
    const list = res?.data || []
    if (!list.length) {
      myGroup.value = null
      return
    }
    const detail = await getGroupDetail(list[0].id)
    myGroup.value = detail?.data || null
    // 新建时自动带上小组；编辑时以详情里的为准，别覆盖
    if (!isEdit.value) {
      form.groupId = myGroup.value.id
    }
  } catch (e) {
    myGroup.value = null
  } finally {
    groupLoading.value = false
  }
}

function goCreateGroup() {
  router.push('/cooperation/my-group')
}

function buildPayload() {
  const attachmentUrls = fileList.value
      .map((f) => f.url || f.response?.data?.url)
      .filter(Boolean)
      .join(',')

  return {
    id: form.id,
    groupId: form.groupId,
    title: form.title,
    typeId: form.typeId,
    location: form.location,
    tagId: form.tagId,
    content: form.content,
    attachments: attachmentUrls,
    budget: form.budget,
    startDate: form.dateRange?.[0] || null,
    endDate: form.dateRange?.[1] || null,
    urgencyLevel: form.urgencyLevel,
    expectedDeadline: form.expectedDeadline
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  // 学生必须挂小组；老师发的是项目想法，不强制
  if (!isEdit.value && !isTeacher.value && !form.groupId) {
    ElMessage.warning('你需要先加入或创建一个小组，项目要挂在小组下面')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateArticle(buildPayload())
      ElMessage.success('修改已提交，需重新审核')
    } else {
      const res = await publishArticle(buildPayload())
      // 新建的内容要把项目绑到这个小组上（一个组只能有一个项目）
      const newArticleId = res?.data?.id || res?.data
      if (newArticleId && form.groupId) {
        try {
          await bindArticleToGroup(form.groupId, newArticleId)
        } catch (e) {
          // 绑定失败不阻断主流程，后端在查详情时还能按 groupId 找到
        }
      }
      await ElMessageBox.alert(
          '内容已提交，正在等待管理员审核。审核结果会通过消息中心通知你。',
          '提交成功',
          { confirmButtonText: '查看我填写的', type: 'success' }
      )
    }
    router.push('/cooperation/my-published')
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function handleSaveDraft() {
  // 草稿先存本地，不落库（后端没有草稿状态，避免脏数据）
  localStorage.setItem('ai_collab_draft', JSON.stringify(buildPayload()))
  ElMessage.success('已暂存到本地（不会上传），下次进入可继续编辑')
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadDict()
  loadMyGroup()
  loadDetail()
  // 恢复本地草稿
  if (!isEdit.value) {
    const draft = localStorage.getItem('ai_collab_draft')
    if (draft) {
      try {
        const d = JSON.parse(draft)
        Object.assign(form, d)
      } catch (e) {
        // 忽略
      }
    }
  }
})
</script>

<style scoped>
.publish-page {
  padding: 24px 0;
}

.publish-head {
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

.form-card {
  border-radius: 10px;
}

.group-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.no-group-alert {
  width: 100%;
}

.no-group-alert .el-button {
  margin-top: 8px;
}

.field-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.7;
  margin-top: 6px;
  white-space: pre-line;
}

.submit-row {
  margin-top: 10px;
}
</style>
