<template>
  <div class="profile-page">
    <div class="page-container">
      <div class="back-row">
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
      </div>

      <h2 class="head-title">个人中心</h2>
      <p class="head-sub">完善资料，让对方更了解你</p>

      <el-row :gutter="20">
        <!-- 左侧：头像 + 账号概览 -->
        <el-col :xs="24" :md="8">
          <el-card class="avatar-card" shadow="never">
            <div class="avatar-wrap">
              <el-avatar :size="100" :src="form.avatar">
                {{ (form.nickname || 'U').charAt(0) }}
              </el-avatar>
              <el-upload
                class="avatar-upload"
                :action="avatarUrl"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :on-error="handleUploadError"
                :before-upload="beforeAvatarUpload"
                accept="image/*"
              >
                <el-button size="small" :icon="Camera" class="mt-16">更换头像</el-button>
              </el-upload>
            </div>

            <div class="avatar-info">
              <div class="nickname">{{ form.nickname || '未设置昵称' }}</div>
              <el-tag :type="roleTagType" effect="plain">{{ roleLabel }}</el-tag>
            </div>

            <el-divider />

            <div class="quick-info">
              <div class="qi-item">
                <span class="qi-label">账号</span>
                <span class="qi-value">{{ form.account || '—' }}</span>
              </div>
              <div class="qi-item">
                <span class="qi-label">审核状态</span>
                <el-tag :type="auditStatusTagType(accountInfo.auditStatus)" size="small">
                  {{ auditStatusName(accountInfo.auditStatus) }}
                </el-tag>
              </div>
              <div class="qi-item">
                <span class="qi-label">注册时间</span>
                <span class="qi-value">{{ formatTime(accountInfo.createTime) || '—' }}</span>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧：表单 -->
        <el-col :xs="24" :md="16">
          <el-card class="form-card" shadow="never">
            <el-tabs v-model="activeTab">
              <!-- 基本资料 -->
              <el-tab-pane label="基本资料" name="basic">
                <el-form
                  ref="basicFormRef"
                  :model="form"
                  :rules="basicRules"
                  label-width="100px"
                  class="mt-16"
                >
                  <el-form-item label="昵称" prop="nickname">
                    <el-input v-model="form.nickname" placeholder="怎么称呼你" maxlength="20" show-word-limit />
                  </el-form-item>

                  <el-form-item label="邮箱" prop="email">
                    <el-input v-model="form.email" placeholder="用于接收通知" />
                  </el-form-item>

                  <el-form-item label="手机号" prop="phone">
                    <el-input v-model="form.phone" placeholder="用于找回密码" maxlength="11" />
                  </el-form-item>

                  <el-form-item>
                    <el-button type="primary" :loading="saving" @click="handleSave">保存资料</el-button>
                  </el-form-item>
                </el-form>
              </el-tab-pane>

              <!-- 修改密码 -->
              <el-tab-pane label="修改密码" name="password">
                <el-form
                  ref="pwdFormRef"
                  :model="pwdForm"
                  :rules="pwdRules"
                  label-width="100px"
                  class="mt-16"
                >
                  <el-form-item label="原密码" prop="oldPassword">
                    <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
                  </el-form-item>
                  <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 8 位，含大小写字母、数字、特殊字符" />
                  </el-form-item>
                  <el-form-item label="确认新密码" prop="confirmPassword">
                    <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再输一次" />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" :loading="pwdSaving" @click="handleChangePwd">确认修改</el-button>
                  </el-form-item>
                </el-form>
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
/**
 * 个人中心页。
 * 学生 / 老师能改昵称、邮箱、手机号。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Camera } from '@element-plus/icons-vue'
import { getMyInfo, updateMyInfo, uploadAvatar } from '@/api/user'
import {} from '@/api/user'
import { getRoleCode, getToken, setUserInfo, getUserInfo } from '@/utils/authSession'
import { auditStatusName, auditStatusTagType } from '@/utils/roleDisplay'

const router = useRouter()

const activeTab = ref('basic')
const saving = ref(false)
const pwdSaving = ref(false)
const basicFormRef = ref(null)
const pwdFormRef = ref(null)

const accountInfo = ref({})

const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', STUDENT: '学生', TEACHER: '老师' }
  return map[getRoleCode()] || '用户'
})
const roleTagType = computed(() => {
  const map = { ADMIN: 'danger', STUDENT: 'primary', TEACHER: 'warning' }
  return map[getRoleCode()] || 'info'
})

const form = reactive({
  account: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const basicRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  // 邮箱选填：和注册页保持一致，不写 required，只校验格式
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  // 手机号必填：找回密码靠它验证身份
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

function validateNewPwd(rule, value, callback) {
  if (!value || value.length < 8) {
    callback(new Error('新密码至少 8 位'))
    return
  }
  const ok = /[A-Z]/.test(value) && /[a-z]/.test(value) && /\d/.test(value) && /[^A-Za-z0-9]/.test(value)
  if (!ok) {
    callback(new Error('密码须同时包含大写字母、小写字母、数字和特殊字符'))
    return
  }
  callback()
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, validator: validateNewPwd, trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) =>
          value === pwdForm.newPassword ? callback() : callback(new Error('两次输入的密码不一致')),
      trigger: 'blur'
    }
  ]
}

const avatarUrl = '/api/user/info/avatar'
const uploadHeaders = computed(() => {
  const headerName = 'Authorization'
  return { [headerName]: 'Bearer ' + getToken() }
})

function beforeAvatarUpload(file) {
  const isImage = /^image\//.test(file.type)
  if (!isImage) {
    ElMessage.error('头像只能是图片')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('头像不能超过 5MB')
    return false
  }
  return true
}

function handleAvatarSuccess(res) {
  if (res?.code === 200) {
    form.avatar = res.data?.url || res.data
    ElMessage.success('头像已更新')
    // 同步本地缓存
    const cached = getUserInfo() || {}
    cached.avatar = form.avatar
    setUserInfo(cached)
  } else {
    ElMessage.error(res?.msg || '上传失败')
  }
}

function handleUploadError() {
  ElMessage.error('头像上传失败，请重试')
}

async function loadInfo() {
  try {
    const res = await getMyInfo()
    const d = res?.data || {}
    accountInfo.value = d
    form.account = d.account || ''
    form.nickname = d.nickname || ''
    form.email = d.email || ''
    form.phone = d.phone || ''
    form.avatar = d.avatar || ''
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleSave() {
  const valid = await basicFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    await updateMyInfo({
      nickname: form.nickname,
      email: form.email,
      phone: form.phone || null
    })
    ElMessage.success('资料已保存')

    // 同步本地缓存，顶部导航能立刻更新
    const cached = getUserInfo() || {}
    cached.nickname = form.nickname
    cached.email = form.email
    setUserInfo(cached)
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function handleChangePwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  pwdSaving.value = true
  try {
    // 复用找回密码接口做修改（后端如提供独立改密接口，替换这里的调用即可）
    const { findPasswordByPhone } = await import('@/api/auth')
    await findPasswordByPhone({
      account: form.account,
      phone: form.phone,
      // ⚠️ 字段名必须是 newPassword —— 后端 ResetByPhoneDTO 就是这么定义的。
      // 写成 password 后端收不到，会被 @NotBlank 拦下报「新密码不能为空」。
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (e) {
    // 拦截器已提示
  } finally {
    pwdSaving.value = false
  }
}

function goBack() {
  router.back()
}

function formatTime(t) {
  if (!t) {
    return ''
  }
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(loadInfo)
</script>

<style scoped>
.profile-page {
  padding: 24px 0;
}

.back-row {
  margin-bottom: 14px;
}

.head-title {
  font-size: 22px;
  color: #1d3a6b;
  margin: 0 0 6px;
}

.head-sub {
  font-size: 13px;
  color: #909399;
  margin: 0 0 18px;
}

.avatar-card,
.form-card {
  border-radius: 10px;
}

.avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 0 6px;
}

.avatar-upload {
  display: flex;
  justify-content: center;
}

.avatar-info {
  text-align: center;
  margin-top: 14px;
}

.nickname {
  font-size: 18px;
  font-weight: 600;
  color: #1d3a6b;
  margin-bottom: 8px;
}

.quick-info {
  padding: 0 6px;
}

.qi-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 0;
  font-size: 13px;
  border-bottom: 1px dashed #f0f2f5;
}

.qi-item:last-child {
  border-bottom: none;
}

.qi-label {
  color: #909399;
}

.qi-value {
  color: #303133;
}
</style>
