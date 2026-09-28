<template>
  <div class="auth-page">
    <div class="auth-box">
      <h2 class="form-title">找回密码</h2>
      <p class="form-sub">用注册时填的手机号或邮箱验证身份，验证通过后重置密码</p>

      <el-steps :active="step" align-center class="steps">
        <el-step title="验证身份" />
        <el-step title="设置新密码" />
        <el-step title="完成" />
      </el-steps>

      <!-- 第一步：验证身份（手机号 / 邮箱 两种方式并行，任选其一） -->
      <el-form v-if="step === 0" ref="step1Ref" :model="form" :rules="rules1" label-position="top">
        <!-- 验证方式切换 -->
        <el-form-item label="验证方式">
          <el-radio-group v-model="mode" class="mode-group">
            <el-radio-button value="phone">用手机号</el-radio-button>
            <el-radio-button value="email">用邮箱</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="账号" prop="account">
          <el-input v-model="form.account" placeholder="请输入注册时的账号" :prefix-icon="User" />
        </el-form-item>

        <!-- 手机号方式 -->
        <el-form-item v-if="mode === 'phone'" label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入注册时的手机号" :prefix-icon="Phone" maxlength="11" />
        </el-form-item>

        <!-- 邮箱方式 -->
        <el-form-item v-else label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入注册时的邮箱" :prefix-icon="Message" />
        </el-form-item>

        <p v-if="mode === 'email'" class="mode-tip">
          注：邮箱是注册时选填的，没填邮箱的话请改用「用手机号」
        </p>

        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="nextStep">
          下一步
        </el-button>
      </el-form>

      <!-- 第二步：新密码 -->
      <el-form v-else-if="step === 1" ref="step2Ref" :model="form" :rules="rules2" label-position="top">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="至少 8 位，含大小写字母、数字、特殊字符" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再输一次" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleReset">
          提交重置
        </el-button>
      </el-form>

      <!-- 第三步：完成 -->
      <div v-else class="done-block">
        <el-icon class="done-icon" :size="56" color="#67c23a"><CircleCheckFilled /></el-icon>
        <p class="done-text">密码重置成功</p>
        <el-button type="primary" size="large" @click="goLogin">返回登录</el-button>
      </div>

      <div class="register-tip">
        <el-link type="primary" :underline="false" @click="goLogin">返回登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 找回密码页。
 * 三步走：验证身份 → 设置新密码 → 完成。
 *
 * 【两种验证方式并行】用户可以用手机号或邮箱任一方式验证身份：
 *   手机号 —— 注册必填，人人都有，前端默认选这个
 *   邮箱   —— 注册选填，记得邮箱的人可以用
 * 两种各自独立，后端也是两个接口（/find/pwd/phone、/find/pwd/email）。
 */
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Phone, Message, CircleCheckFilled } from '@element-plus/icons-vue'
import { findPasswordByPhone, findPassword } from '@/api/auth'

const router = useRouter()
const step = ref(0)
const loading = ref(false)
const step1Ref = ref(null)
const step2Ref = ref(null)

/** 验证方式：phone（默认）或 email */
const mode = ref('phone')

const form = reactive({
  account: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: ''
})

/**
 * 第一步校验规则。
 * 手机号和邮箱两块规则都定义着，但只校验当前选中的那个
 * （没选中的那个字段是空的，如果一起校验会一直报「请输入」）。
 */
const rules1 = computed(() => {
  const base = {
    account: [{ required: true, message: '请输入账号', trigger: 'blur' }]
  }
  if (mode.value === 'phone') {
    base.phone = [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
    ]
  } else {
    base.email = [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
    ]
  }
  return base
})

function validatePassword(rule, value, callback) {
  if (!value || value.length < 8) {
    callback(new Error('密码至少 8 位'))
    return
  }
  const ok = /[A-Z]/.test(value) && /[a-z]/.test(value) && /\d/.test(value) && /[^A-Za-z0-9]/.test(value)
  if (!ok) {
    callback(new Error('密码须同时包含大写字母、小写字母、数字和特殊字符'))
    return
  }
  callback()
}

const rules2 = {
  password: [{ required: true, validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) =>
          value === form.password ? callback() : callback(new Error('两次输入的密码不一致')),
      trigger: 'blur'
    }
  ]
}

async function nextStep() {
  const valid = await step1Ref.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    // 真正的校验在后端重置时才做，这里直接进第二步
    step.value = 1
  } finally {
    loading.value = false
  }
}

async function handleReset() {
  const valid = await step2Ref.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    // ⚠️ 字段名必须是 newPassword（后端 DTO 就这么定的），
    // 写成 password 后端收不到，会被拦下报「新密码不能为空」。
    if (mode.value === 'phone') {
      await findPasswordByPhone({
        account: form.account,
        phone: form.phone,
        newPassword: form.password
      })
    } else {
      await findPassword({
        account: form.account,
        email: form.email,
        newPassword: form.password
      })
    }
    step.value = 2
  } catch (e) {
    // 提示已由拦截器处理
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
  width: 520px;
  max-width: 100%;
  background: #fff;
  border-radius: 14px;
  padding: 44px 44px 34px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.25);
}

.form-title {
  font-size: 22px;
  margin: 0 0 6px;
  color: #1d3a6b;
}

.form-sub {
  font-size: 13px;
  color: #909399;
  margin: 0 0 24px;
}

.steps {
  margin-bottom: 28px;
}

/* 验证方式切换（用手机号 / 用邮箱），两个按钮均分宽度 */
.mode-group {
  width: 100%;
  display: flex;
}

.mode-group :deep(.el-radio-button) {
  flex: 1;
}

.mode-group :deep(.el-radio-button__inner) {
  width: 100%;
}

/* 邮箱方式的提示：邮箱是选填的，没填的人要改用手机号 */
.mode-tip {
  font-size: 12px;
  color: #e6a23c;
  margin: -8px 0 14px;
  line-height: 1.6;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 2px;
}

.done-block {
  text-align: center;
  padding: 20px 0 10px;
}

.done-icon {
  margin-bottom: 14px;
}

.done-text {
  font-size: 16px;
  color: #303133;
  margin: 0 0 22px;
}

.register-tip {
  text-align: center;
  margin-top: 18px;
}
</style>
