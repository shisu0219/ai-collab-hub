<template>
  <div class="auth-page">
    <div class="auth-box">
      <!-- 左侧品牌区 -->
      <div class="brand-side">
        <div class="brand-logo">AI</div>
        <h1 class="brand-title">人工智能学院双创平台</h1>
        <p class="brand-sub">校企协同 · 产学研对接 · 项目孵化</p>
        <ul class="brand-points">
          <li>学生填写项目，老师填写需求</li>
          <li>管理员审核，信息真实可靠</li>
          <li>线上对接，沟通不再断线</li>
        </ul>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-side">
        <h2 class="form-title">登录</h2>
        <p class="form-sub">欢迎回来，请登录你的账号</p>

        <!-- 身份选择：三个方形卡片，点击区域大，不容易点偏 -->
        <div class="role-cards">
          <div
            v-for="r in roleOptions"
            :key="r.value"
            class="role-card"
            :class="{ active: form.role === r.value }"
            @click="handleSwitchRole(r.value)"
          >
            <el-icon :size="24"><component :is="r.icon" /></el-icon>
            <span class="role-card__name">{{ r.label }}</span>
          </div>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item label="账号" prop="account">
            <el-input
              v-model="form.account"
              placeholder="请输入账号"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <div class="form-actions">
            <el-checkbox v-model="form.remember">记住账号</el-checkbox>
            <el-link type="primary" :underline="false" @click="goForgot">忘记密码？</el-link>
          </div>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>

          <div class="register-tip">
            还没有账号？
            <el-link type="primary" :underline="false" @click="goRegister">注册账号</el-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 登录页。
 * 支持学生 / 老师 / 管理员三种身份登录，后端接口分开。
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, School, OfficeBuilding, Setting } from '@element-plus/icons-vue'
import { loginStudent, loginAdmin, loginTeacher } from '@/api/auth'
import { saveLoginState } from '@/utils/authSession'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const loading = ref(false)

/** 身份选项：三个卡片 */
const roleOptions = [
  { value: 'STUDENT', label: '学生', icon: School },
  { value: 'TEACHER', label: '老师', icon: OfficeBuilding },
  { value: 'ADMIN', label: '管理员', icon: Setting }
]

const form = reactive({
  role: 'STUDENT',
  account: '',
  password: '',
  remember: true
})

const rules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, message: '账号至少 4 位字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位', trigger: 'blur' }
  ]
}

onMounted(() => {
  // 记住上次登录的账号，省得每次都手打
  const saved = localStorage.getItem('ai_collab_last_account')
  if (saved) {
    form.account = saved
  }
  // 注册完跳回来带 role 参数，自动切换身份
  if (route.query.role) {
    form.role = String(route.query.role)
  }
})

async function handleLogin() {
  if (!formRef.value) {
    return
  }

  // 校验不通过必须给出提示。
  // 原来这里直接 return 什么都不做，用户看到的就是"点了没反应"，
  // 根本不知道是哪里填错了。
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请检查账号和密码是否填写正确（账号至少4位，密码至少8位）')
    return
  }

  // 顺手去一下首尾空格，避免复制粘贴带进来的空格把登录搞失败
  form.account = (form.account || '').trim()

  loading.value = true
  try {
    const payload = { account: form.account, password: form.password }
    // 三种身份走各自的入口：管理员单独一个接口，学生/老师分开
    let res
    if (form.role === 'ADMIN') {
      res = await loginAdmin(payload)
    } else if (form.role === 'TEACHER') {
      res = await loginTeacher(payload)
    } else {
      res = await loginStudent(payload)
    }

    const data = res?.data || {}
    // 后端返回的是 roleCodes 数组，这里统一取一个字符串出来
    const codes = data.roleCodes || data.roleCode || data?.roleInfo?.code
    const roleCode = Array.isArray(codes) ? (codes[0] || '') : (codes || '')

    // 后端把 token 放在 tokenInfo 里（data.tokenInfo.token），不在顶层的 data.token。
    // 之前直接取 data.token 拿到的是 undefined，等于没存登录态，
    // 结果路由守卫以为没登录，一进后台就被踢回登录页，来回打转。
    const token = data.tokenInfo?.token || data.token || ''
    if (!token) {
      ElMessage.error('登录失败：服务端没有返回凭证')
      return
    }

    // 登录态存的是整个 data，但要把 token 和 roleCode 提到顶层，别的地方要用
    saveLoginState(token, { ...data, roleCode })

    if (form.remember) {
      localStorage.setItem('ai_collab_last_account', form.account)
    } else {
      localStorage.removeItem('ai_collab_last_account')
    }

    ElMessage.success('登录成功')

    // 跳转完全以后端返回的真实角色为准，不看身份按钮选了什么。
    // 原因：身份按钮只是告诉用户"我要登哪种账号"，
    // 真正有权限的是库里存的角色，按钮点错不该影响结果。
    if (roleCode === 'ADMIN') {
      router.replace('/admin/dashboard')
      return
    }

    // 学生 / 老师：能回原页面就回，回不去就进合作平台
    const redirect = route.query.redirect
    const fallback = '/cooperation/browse'
    if (redirect && !String(redirect).startsWith('/admin')) {
      router.replace(String(redirect))
    } else {
      router.replace(fallback)
    }
  } catch (e) {
    // 错误提示已经在 request 拦截器里统一弹了，这里不重复
  } finally {
    loading.value = false
  }
}

/**
 * 切换身份。
 * 换身份时把已填的账号密码清掉——不同身份的账号不是一套，
 * 留着上一个人的输入容易登错，也容易误点。
 */
function handleSwitchRole(role) {
  if (form.role === role) {
    return
  }
  form.role = role
  form.account = ''
  form.password = ''
  // 清掉校验状态，避免刚切过来就飘红
  formRef.value?.clearValidate()
}

function goRegister() {
  router.push('/register')
}

function goForgot() {
  router.push('/forgot-password')
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d3a6b 0%, #2c5490 50%, #c9a227 100%);
  padding: 20px;
}

.auth-box {
  display: flex;
  width: 920px;
  max-width: 100%;
  min-height: 560px;
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.25);
}

.brand-side {
  width: 42%;
  background: linear-gradient(160deg, #1d3a6b 0%, #2c5490 100%);
  color: #fff;
  padding: 48px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-logo {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: #c9a227;
  color: #1d3a6b;
  font-size: 26px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.brand-title {
  font-size: 24px;
  margin: 0 0 10px;
  font-weight: 600;
  line-height: 1.4;
}

.brand-sub {
  font-size: 13px;
  opacity: 0.8;
  margin: 0 0 32px;
}

.brand-points {
  list-style: none;
  padding: 0;
  margin: 0;
}

.brand-points li {
  font-size: 13px;
  opacity: 0.9;
  padding-left: 18px;
  position: relative;
  margin-bottom: 12px;
}

.brand-points li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c9a227;
}

.form-side {
  flex: 1;
  padding: 48px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-title {
  font-size: 24px;
  margin: 0 0 6px;
  color: #1d3a6b;
}

.form-sub {
  font-size: 13px;
  color: #909399;
  margin: 0 0 24px;
}

.role-cards {
  display: flex;
  gap: 10px;
  margin-bottom: 22px;
}

.role-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 14px 8px;
  border: 2px solid #e4e7ed;
  border-radius: 10px;
  cursor: pointer;
  color: #909399;
  transition: all 0.18s;
  user-select: none;
}

.role-card:hover {
  border-color: #a8b8d0;
  color: #1d3a6b;
  background: #f8fafd;
}

.role-card.active {
  border-color: #1d3a6b;
  background: #eef2f9;
  color: #1d3a6b;
  font-weight: 600;
}

.role-card__name {
  font-size: 14px;
}

.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  padding-bottom: 4px;
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 2px;
}

.register-tip {
  text-align: center;
  margin-top: 18px;
  font-size: 13px;
  color: #909399;
}

@media (max-width: 768px) {
  .brand-side {
    display: none;
  }
  .auth-box {
    min-height: auto;
  }
}
</style>
