<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <div class="logo">药</div>
        <div>
          <h1>内勤查询台</h1>
          <p>财务 / 销售内勤 · 查发票、未回款、库存与红冲</p>
        </div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item v-if="showServer" prop="apiBase">
          <el-input
            v-model="form.apiBase"
            placeholder="后端地址，例如 http://192.168.1.10:8899"
            size="large"
            prefix-icon="Link"
            @change="applyServer"
          />
        </el-form-item>
        <el-form-item prop="loginname">
          <el-input v-model="form.loginname" placeholder="工号 / 用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="pwd">
          <el-input v-model="form.pwd" type="password" placeholder="密码" size="large" show-password prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="code">
          <div class="captcha-row">
            <el-input v-model="form.code" placeholder="验证码" size="large" prefix-icon="Key" />
            <img :src="captchaImage" class="captcha-img" title="点击刷新" @click="refreshCaptcha" />
          </div>
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
        <el-button text class="back-btn" @click="emit('back')">返回介绍</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { login, getCaptchaBase64, currentUser } from '../api/login'
import { getApiBase, saveApiBase, TOKEN_KEY, USER_KEY } from '../utils/request'
import { savePermissions } from '../utils/perm'

const emit = defineEmits<{
  success: [payload: { token: string; name: string; permissions: string[] }]
  back: []
}>()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaId = ref('')
const captchaImage = ref('')
const showServer = Boolean(window.employeeApp?.isElectron) && !import.meta.env.DEV
const form = reactive({
  apiBase: getApiBase(),
  loginname: 'admin',
  pwd: '123456',
  code: ''
})
const rules = {
  loginname: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  pwd: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const refreshCaptcha = async () => {
  const res: any = await getCaptchaBase64()
  captchaId.value = res.captchaId
  captchaImage.value = res.image
}

const applyServer = async () => {
  form.apiBase = await saveApiBase(form.apiBase)
  form.code = ''
  await refreshCaptcha()
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      if (showServer) {
        form.apiBase = await saveApiBase(form.apiBase)
      }
      const res: any = await login({
        loginname: form.loginname,
        pwd: form.pwd,
        code: form.code,
        captchaId: captchaId.value
      })
      if (res.code !== 200) {
        form.code = ''
        await refreshCaptcha()
        return
      }
      const token = res.token
      if (!token) {
        ElMessage.error('未拿到登录凭证，请检查后端 satoken')
        return
      }
      localStorage.setItem(TOKEN_KEY, token)
      const me: any = await currentUser()
      const name = me.user?.name || form.loginname
      const permissions = me.permissions || []
      localStorage.setItem(USER_KEY, name)
      savePermissions(permissions)
      ElMessage.success('登录成功')
      emit('success', { token, name, permissions })
    } catch {
      form.code = ''
      await refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}

onMounted(refreshCaptcha)
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(1200px 500px at 10% -10%, #1d4f7a 0%, transparent 55%),
    linear-gradient(160deg, #0f2740 0%, #16324f 45%, #0c1d30 100%);
}
.login-card {
  width: 420px;
  padding: 36px 32px 28px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
}
.brand {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 28px;
}
.logo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1b7fbf, #0d5a8a);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
}
.brand h1 {
  margin: 0;
  font-size: 22px;
  color: #123;
}
.brand p {
  margin: 4px 0 0;
  color: #6b7c8d;
  font-size: 13px;
}
.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.captcha-img {
  width: 116px;
  height: 40px;
  border-radius: 6px;
  cursor: pointer;
  background: #f3f6f9;
}
.login-btn {
  width: 100%;
  margin-top: 4px;
}
.back-btn {
  width: 100%;
  margin-top: 4px;
}
</style>
