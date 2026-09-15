<template>
<main class="login-scene">
  <DesignArt kind="login-left" class="login-scene-left" />
  <DesignArt kind="login-right" class="login-scene-right" />
  <div class="login-motto">
    <p>安全 · 规范 · 监测 · 智慧管理</p>
    <span></span>
    <small>
      SAFER CONSTRUCTION
      <br />
      A BETTER TOMORROW
    </small>
  </div>
  <div class="login-motto right">
    <p>筑基于安全　守护每一座城市</p>
    <small>
      BUILD A SAFER TOMORROW
      <br />
      FOR BETTER CITIES
    </small>
  </div>
  <section class="login-panel">
    <div class="login-brand">
      <BrandMark />
      <div>
        <h1>基坑安全管理系统</h1>
        <p>Foundation Pit Safety Management System</p>
      </div>
    </div>
    <el-form
      ref="loginFormRef"
      :model="loginForm"
      :rules="rules"
      class="login-form"
      @submit.prevent="handleLogin"
    >
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          aria-label="用户名"
          placeholder="请输入用户名"
          :prefix-icon="User"
          autocomplete="username"
        />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          aria-label="密码"
          placeholder="请输入密码"
          type="password"
          :prefix-icon="Lock"
          show-password
          autocomplete="current-password"
        />
      </el-form-item>
      <el-button type="primary" native-type="submit" class="login-submit" :loading="submitting">
        登录
        <el-icon v-if="!submitting">
          <Right />
        </el-icon>
      </el-button>
    </el-form>
    <div class="login-hint">
      <span></span>
      默认账号：admin / 123456
      <span></span>
    </div>
  </section>
</main>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Right } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'

const router = useRouter()
const loginFormRef = ref()
const submitting = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const handleLogin = () => {
  if (submitting.value) return
  loginFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('请正确填写登录信息')
      return
    }

    submitting.value = true
    try {
      const data = await login(loginForm.username, loginForm.password)
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.userInfo || {}))
      ElMessage.success('登录成功')
      router.push('/')
    } catch (error) {
      ElMessage.error('登录失败，请检查用户名或密码')
    } finally { submitting.value = false }
  })
}
</script>

<style scoped>
.login-scene {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: radial-gradient(ellipse at 17% 12%,#529cff,transparent 70%),linear-gradient(115deg,#447dea,#705ce2);
  padding: 110px 24px 70px;
  isolation: isolate;
}

.login-scene:after {
  content: '';
  position: absolute;
  width: 680px;
  height: 680px;
  border-radius: 50%;
  background: #514ddb22;
  top: 12%;
  right: 8%;
  z-index: -1;
}

.login-scene .login-scene-left {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 38%;
  height: 90%;
  object-fit: cover;
  z-index: -1;
  opacity: .77;
  mask-image: linear-gradient(90deg,#000 55%,transparent);
}

.login-scene .login-scene-right {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 33%;
  height: 90%;
  z-index: -1;
  opacity: .65;
  mask-image: linear-gradient(270deg,#000 60%,transparent);
}

.login-motto {
  position: absolute;
  top: 7%;
  left: 4.3%;
  color: #d8e8ffb8;
  letter-spacing: 6px;
  font-size: 13px;
  line-height: 2;
}

.login-motto span {
  display: block;
  width: 22px;
  height: 2px;
  background: #d8e8ff;
  margin: 16px 0;
}

.login-motto small {
  font-size: 9px;
  letter-spacing: 5px;
}

.login-motto.right {
  left: auto;
  right: 4.3%;
  text-align: right;
}

.login-motto.right small {
  display: block;
  margin-top: 14px;
}

.login-panel {
  position: relative;
  background: linear-gradient(130deg,#ffffff,#fafcff);
  width: 580px;
  max-width: 100%;
  border: 1px solid #fff;
  border-radius: 16px;
  padding: 52px;
  box-shadow: 0 20px 65px #153eaa25;
  z-index: 1;
}

.login-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin: 6px 0 40px;
  white-space: nowrap;
}

.login-brand .brand-mark {
  width: 84px;
  height: 84px;
}

.login-brand h1 {
  font-size: 32px;
  color: #0d2856;
  font-weight: 750;
  margin: 0 0 7px;
}

.login-brand p {
  font-size: 13px;
  color: #8490a9;
  margin: 0;
}

.login-form :deep(.el-input__wrapper) {
  height: 62px;
  border-radius: 8px;
  padding: 0 20px;
  box-shadow: 0 0 0 1px #d8dfed inset;
  background: #fff;
}

.login-form :deep(.el-input__inner) {
  font-size: 18px;
}

.login-form :deep(.el-input__prefix) {
  font-size: 23px;
  margin-right: 16px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #448cff inset;
}

.login-submit {
  width: 100%;
  height: 64px;
  font-size: 24px;
  font-weight: 600;
  margin-top: 2px;
  border-radius: 9px;
  box-shadow: 0 12px 24px #2b7cfb30 !important;
}

.login-submit .el-icon {
  margin-left: 16px;
}

.login-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #8795ae;
  font-size: 14px;
  margin-top: 36px;
  white-space: nowrap;
}

.login-hint span {
  height: 1px;
  width: 36px;
  background: #d9e1ef;
}

@media (min-width:1700px) {
  .login-panel {
    width: 640px;
    padding: 56px;
  }
  .login-brand h1 {
    font-size: 35px;
  }
  .login-brand p {
    font-size: 15px;
  }
}

@media (max-width:700px) {
  .login-scene {
    padding: 90px 20px 40px;
  }
  .login-panel {
    padding: 30px 23px;
  }
  .login-brand {
    gap: 9px;
    margin-bottom: 30px;
  }
  .login-brand .brand-mark {
    width: 58px;
    height: 58px;
  }
  .login-brand h1 {
    font-size: 24px;
  }
  .login-brand p {
    font-size: 9px;
  }
  .login-motto {
    font-size: 10px;
    letter-spacing: 2px;
    top: 4%;
    left: 6%;
  }
  .login-motto small,.login-motto span,.login-motto.right {
    display: none;
  }
  .login-hint {
    font-size: 12px;
    gap: 9px;
  }
  .login-hint span {
    width: 22px;
  }
  .login-form :deep(.el-input__wrapper) {
    height: 54px;
  }
  .login-submit {
    height: 54px;
    font-size: 20px;
  }
}
</style>
