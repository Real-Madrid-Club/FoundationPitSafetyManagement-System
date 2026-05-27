<template>
  <div class="layout-container">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <div class="header-left">
        <h2>🏗️ 基坑安全管理系统</h2>
      </div>
      <div class="header-right">
        <el-dropdown>
          <span class="user-info">
            <el-avatar :size="32" icon="User" />
            <span>{{ user?.username || '管理员' }}</span>
            <el-tag size="small" :type="roleTagType" style="margin-left:4px">{{ roleLabel }}</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="showPwd = true">修改密码</el-dropdown-item>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="main-container">
      <!-- 左侧菜单 -->
      <el-aside width="200px" class="sidebar">
        <el-menu
          :default-active="$route.path"
          router
          class="menu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/dashboard">
            <el-icon><House /></el-icon>
            <span>首页大盘</span>
          </el-menu-item>
          <el-menu-item v-if="isAdmin" index="/device">
            <el-icon><Monitor /></el-icon>
            <span>设备管理</span>
          </el-menu-item>
          <el-menu-item index="/workorder">
            <el-icon><Tickets /></el-icon>
            <span>工单调度</span>
          </el-menu-item>
          <el-menu-item index="/ai-chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI助手</span>
          </el-menu-item>
          <el-menu-item index="/monitor">
            <el-icon><TrendCharts /></el-icon>
            <span>监测数据</span>
          </el-menu-item>
          <el-menu-item index="/foundation-pit">
            <el-icon><Box /></el-icon>
            <span>基坑模型</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 右侧内容区域 -->
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="showPwd" title="修改密码" width="380px" :close-on-click-modal="false">
      <el-form :model="pwdForm" ref="pwdFormRef" label-width="80px">
        <el-form-item label="旧密码" prop="oldPwd"
          :rules="[{ required: true, message: '请输入旧密码', trigger: 'blur' }]">
          <el-input v-model="pwdForm.oldPwd" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPwd"
          :rules="[{ required: true, min: 6, message: '至少6位', trigger: 'blur' }]">
          <el-input v-model="pwdForm.newPwd" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd"
          :rules="[{ required: true, validator: validateConfirm, trigger: 'blur' }]">
          <el-input v-model="pwdForm.confirmPwd" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPwd = false">取消</el-button>
        <el-button type="primary" @click="handleChangePwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { House, Monitor, Tickets, ChatDotRound, TrendCharts, Box } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changePassword } from '@/api/auth'

const router = useRouter()
const user = ref(null)
const showPwd = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPwd: '', newPwd: '', confirmPwd: '' })

const validateConfirm = (_rule, value, callback) => {
  if (value !== pwdForm.newPwd) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

onMounted(() => {
  // 获取用户信息
  const userInfo = localStorage.getItem('user')
  if (userInfo) {
    user.value = JSON.parse(userInfo)
  }
})

const isAdmin = computed(() => {
  const roles = user.value?.roles || user.value?.userInfo?.roles || []
  return roles.includes('ROLE_ADMIN')
})

const roleLabel = computed(() => {
  const roles = user.value?.roles || user.value?.userInfo?.roles || []
  if (roles.includes('ROLE_ADMIN')) return '监控中心'
  if (roles.includes('ROLE_REPAIRER')) return '维修工程师'
  if (roles.includes('ROLE_BUYER')) return '施工方'
  return '用户'
})

const roleTagType = computed(() => {
  const roles = user.value?.roles || user.value?.userInfo?.roles || []
  if (roles.includes('ROLE_ADMIN')) return 'danger'
  if (roles.includes('ROLE_REPAIRER')) return 'warning'
  if (roles.includes('ROLE_BUYER')) return 'success'
  return 'info'
})

const handleChangePwd = async () => {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    const uid = user.value?.id || user.value?.userInfo?.id
    await changePassword(uid, pwdForm.oldPwd, pwdForm.newPwd)
    ElMessage.success('密码修改成功')
    showPwd.value = false
    pwdForm.oldPwd = ''
    pwdForm.newPwd = ''
    pwdForm.confirmPwd = ''
  } catch {
    ElMessage.error('修改失败，请检查旧密码')
  }
}

const handleLogout = () => {
  ElMessageBox.confirm(
    '确定要退出登录吗？',
    '退出确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 清除登录信息
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {
    // 用户取消退出
  })
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background-color: #409EFF;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-left h2 {
  margin: 0;
  font-size: 18px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: white;
}

.main-container {
  flex: 1;
  overflow: hidden;
}

.sidebar {
  background-color: #304156;
  height: 100%;
}

.menu {
  border: none;
  height: 100%;
}

.content {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
