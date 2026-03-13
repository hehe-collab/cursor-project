<template>
  <div class="login-page">
    <div class="login-box">
      <h1>短剧出海</h1>
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password @keyup.enter="onSubmit" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="onSubmit" style="width: 100%">登录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const res = await request.post('/auth/login', form)
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data.user))
    sessionStorage.setItem('loginSuccess', '1')
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    /* 错误已由 request 拦截器提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  position: relative;
  overflow: hidden;
}
.login-page::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 30% 30%, rgba(64,158,255,0.08) 0%, transparent 50%);
  pointer-events: none;
}
.login-box {
  width: 400px;
  padding: 48px 44px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255,255,255,0.1) inset;
  position: relative;
  backdrop-filter: blur(10px);
}
.login-box h1 {
  text-align: center;
  margin-bottom: 36px;
  color: #1a1a2e;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 3px;
}
.login-form {
  margin-top: 24px;
}
.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 12px 16px;
}
.login-form :deep(.el-button) {
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
}
</style>
