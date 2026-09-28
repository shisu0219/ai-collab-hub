<template>
  <div class="auth-page">
    <div class="auth-box">
      <div class="brand-side">
        <div class="brand-logo">AI</div>
        <h1 class="brand-title">人工智能学院双创平台</h1>
        <p class="brand-sub">注册账号，加入校企对接网络</p>
        <div class="brand-notice">
          <p class="notice-title">注册须知</p>
          <ul class="brand-points">
            <li>账号至少 4 位字符</li>
            <li>密码至少 8 位，须含大小写字母、数字、特殊字符</li>
            <li>提交后需等待管理员审核，通过后才能登录</li>
            <li>老师注册需填写所属学院与职称</li>
            <li>手机号务必填写正确 —— 忘记密码时要用它来验证身份</li>
          </ul>
        </div>
      </div>

      <div class="form-side">
        <h2 class="form-title">注册账号</h2>
        <p class="form-sub">选择身份后填写信息，提交等待审核</p>

        <el-radio-group v-model="form.role" class="role-switch" size="large">
          <el-radio-button value="STUDENT">学生</el-radio-button>
          <el-radio-button value="TEACHER">老师</el-radio-button>
        </el-radio-group>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="reg-form"
        >
          <el-form-item label="账号" prop="account">
            <el-input v-model="form.account" placeholder="至少 4 位字符" :prefix-icon="User" clearable />
          </el-form-item>

          <el-form-item label="昵称 / 姓名" prop="nickname">
            <el-input v-model="form.nickname" placeholder="怎么称呼你" :prefix-icon="Avatar" clearable />
          </el-form-item>

          <!-- 学生专属字段 -->
          <template v-if="form.role === 'STUDENT'">
            <el-form-item label="所在院系 / 班级" prop="department">
              <el-input v-model="form.department" placeholder="例如：人工智能学院 大数据4班" clearable />
            </el-form-item>
          </template>

          <!-- 老师专属字段 -->
          <template v-else>
            <el-form-item label="所属学院 / 部门" prop="department">
              <el-input v-model="form.department" placeholder="例如：人工智能学院" clearable />
            </el-form-item>
            <el-form-item label="职称" prop="title">
              <el-select v-model="form.title" placeholder="请选择职称" style="width: 100%" clearable>
                <el-option v-for="it in titleOptions" :key="it" :label="it" :value="it" />
              </el-select>
            </el-form-item>
          </template>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="选填，用于接收通知" :prefix-icon="Message" clearable />
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="用于找回密码，请如实填写" :prefix-icon="Phone" clearable maxlength="11" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="至少 8 位" :prefix-icon="Lock" show-password />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="再输一次" :prefix-icon="Lock" show-password />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            @click="handleRegister"
          >
            提交注册
          </el-button>

          <div class="register-tip">
            已有账号？
            <el-link type="primary" :underline="false" @click="goLogin">返回登录</el-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 注册页。
 * 平台对外只有学生 / 老师两种身份可注册（管理员由系统预设），
 * 两种身份的扩展字段不一样，用 role 切换动态渲染。
 *
 * 【本次修正】
 *  1. 平台只有学生 / 老师两种身份可注册，老师填「所属学院 + 职称」
 *  2. 邮箱保持必填（找回密码需要）
 */
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock, Message, Phone, Avatar } from '@element-plus/icons-vue'
import { registerStudent, registerTeacher } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

/** 职称选项：老师注册时选 */
const titleOptions = ['助教', '讲师', '副教授', '教授', '研究员', '工程师', '其它']

const form = reactive({
  role: 'STUDENT',
  account: '',
  nickname: '',
  department: '',
  title: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

/** 密码强度校验：至少8位，含大小写字母、数字、特殊字符 */
function validatePassword(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (value.length < 8) {
    callback(new Error('密码至少 8 位'))
    return
  }
  const hasUpper = /[A-Z]/.test(value)
  const hasLower = /[a-z]/.test(value)
  const hasDigit = /\d/.test(value)
  const hasSpecial = /[^A-Za-z0-9]/.test(value)
  if (!(hasUpper && hasLower && hasDigit && hasSpecial)) {
    callback(new Error('密码须同时包含大写字母、小写字母、数字和特殊字符'))
    return
  }
  callback()
}

function validateConfirm(rule, value, callback) {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = computed(() => ({
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, message: '账号至少 4 位字符', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  // 邮箱选填（找回密码走手机号，不用邮箱了）
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  // 手机号必填：找回密码要用「账号 + 手机号」双重校验，不填就没法找回
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ],
  // 学生和老师的「院系」都必填，但提示语不同
  department: [
    {
      required: true,
      message: form.role === 'STUDENT' ? '请填写所在院系班级' : '请填写所属学院',
      trigger: 'blur'
    }
  ],
  // 职称只在老师身份下要求
  title: form.role === 'TEACHER'
      ? [{ required: true, message: '请选择职称', trigger: 'change' }]
      : []
}))

async function handleRegister() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    const payload = {
      account: form.account,
      nickname: form.nickname,
      email: form.email,
      phone: form.phone || null,
      password: form.password
    }

    if (form.role === 'TEACHER') {
      await registerTeacher({
        ...payload,
        department: form.department,
        title: form.title
      })
    } else {
      await registerStudent({
        ...payload,
        department: form.department
      })
    }

    await ElMessageBox.alert(
        '注册信息已提交，需要管理员审核通过后才能登录。请耐心等待，管理员审核后会通过站内消息通知你。',
        '提交成功',
        { confirmButtonText: '返回登录', type: 'success' }
    )
    router.push('/login')
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d3a6b 0%, #2c5490 50%, #c9a227 100%);
  padding: 24px;
}

.auth-box {
  display: flex;
  width: 960px;
  max-width: 100%;
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.25);
}

.brand-side {
  width: 38%;
  background: linear-gradient(160deg, #1d3a6b 0%, #2c5490 100%);
  color: #fff;
  padding: 48px 34px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-logo {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  background: #c9a227;
  color: #1d3a6b;
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 22px;
}

.brand-title {
  font-size: 22px;
  margin: 0 0 10px;
  font-weight: 600;
  line-height: 1.4;
}

.brand-sub {
  font-size: 13px;
  opacity: 0.8;
  margin: 0 0 28px;
}

.notice-title {
  font-size: 14px;
  font-weight: 600;
  color: #e0be52;
  margin: 0 0 12px;
}

.brand-points {
  list-style: none;
  padding: 0;
  margin: 0;
}

.brand-points li {
  font-size: 13px;
  opacity: 0.9;
  padding-left: 16px;
  position: relative;
  margin-bottom: 10px;
  line-height: 1.6;
}

.brand-points li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #c9a227;
}

.form-side {
  flex: 1;
  padding: 40px 44px;
  max-height: 90vh;
  overflow-y: auto;
}

.form-title {
  font-size: 22px;
  margin: 0 0 6px;
  color: #1d3a6b;
}

.form-sub {
  font-size: 13px;
  color: #909399;
  margin: 0 0 20px;
}

.role-switch {
  margin-bottom: 20px;
  width: 100%;
}

.role-switch :deep(.el-radio-button) {
  flex: 1;
}

.role-switch :deep(.el-radio-button__inner) {
  width: 100%;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 2px;
  margin-top: 6px;
}

.register-tip {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: #909399;
}

@media (max-width: 768px) {
  .brand-side {
    display: none;
  }
  .form-side {
    padding: 28px 22px;
  }
}
</style>
