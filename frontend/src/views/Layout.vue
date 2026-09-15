<template>
<div class="app-shell" :class="{ 'nav-collapsed': collapsed }">
  <aside class="app-sidebar">
    <router-link class="app-brand" to="/dashboard" aria-label="基坑安全管理系统首页">
      <BrandMark />
      <div>
        <strong>基坑安全管理系统</strong>
        <small>FOUNDATION PIT SAFETY MANAGEMENT</small>
      </div>
    </router-link>
    <nav class="app-nav" aria-label="主导航">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        :class="{ active: route.path === item.path }"
        :title="item.label"
      >
        <el-icon>
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
    <DesignArt kind="sidebar" class="sidebar-art" />
  </aside>
  <div class="app-workspace">
    <header class="app-topbar">
      <el-button
        text
        class="nav-toggle"
        :icon="collapsed ? Expand : Fold"
        :aria-label="collapsed ? '展开导航' : '收起导航'"
        @click="collapsed = !collapsed"
      />
      <div class="topbar-user">
        <el-dropdown trigger="click">
          <span class="user-info" tabindex="0">
            <el-avatar :size="34" :icon="UserFilled" />
            <span>{{ user?.username || user?.userInfo?.username || '用户' }}</span>
            <el-tag :type="roleTagType">{{ roleLabel }}</el-tag>
            <el-icon>
              <ArrowDown />
            </el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="showPwd = true">修改密码</el-dropdown-item>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <main class="app-content" :class="{ 'model-content': route.path === '/foundation-pit' }">
      <router-view />
    </main>
  </div>
  <!-- 修改密码弹窗 -->
  <el-dialog v-model="showPwd" title="修改密码" width="380px" :close-on-click-modal="false">
    <el-form :model="pwdForm" ref="pwdFormRef" label-width="80px">
      <el-form-item
        label="旧密码"
        prop="oldPwd"
        :rules="[{ required: true, message: '请输入旧密码', trigger: 'blur' }]"
      >
        <el-input v-model="pwdForm.oldPwd" type="password" show-password />
      </el-form-item>
      <el-form-item
        label="新密码"
        prop="newPwd"
        :rules="[{ required: true, min: 6, message: '至少6位', trigger: 'blur' }]"
      >
        <el-input v-model="pwdForm.newPwd" type="password" show-password />
      </el-form-item>
      <el-form-item
        label="确认密码"
        prop="confirmPwd"
        :rules="[{ required: true, validator: validateConfirm, trigger: 'blur' }]"
      >
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
import { useRouter, useRoute } from 'vue-router'
import { House, Monitor, Tickets, ChatDotRound, TrendCharts, Box, ArrowDown, Fold, Expand, UserFilled } from '@element-plus/icons-vue'
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

const route = useRoute()
const collapsed = ref(false)
const navItems = computed(() => [
{path:'/dashboard',label:'首页大盘',icon:House},
...(isAdmin.value ? [{path:'/device',label:'设备管理',icon:Monitor}] : []),
{path:'/workorder',label:'工单调度',icon:Tickets},
{path:'/ai-chat',label:'AI助手',icon:ChatDotRound},
{path:'/monitor',label:'监测数据',icon:TrendCharts},
{path:'/foundation-pit',label:'基坑模型',icon:Box}
])
</script>

<style scoped>
.app-shell {
  --sidebar-width: 250px;
  display: flex;
  height: 100dvh;
  overflow: hidden;
  background: linear-gradient(115deg,#d3e4fd,#f4f9ff);
}

.app-sidebar {
  width: var(--sidebar-width);
  flex-shrink: 0;
  position: relative;
  background: linear-gradient(140deg,#203f75,#132c55 55%,#214b86);
  overflow: hidden;
  border-right: 1px solid #638ac850;
  transition: width .2s;
}

.app-brand {
  display: flex;
  gap: 7px;
  align-items: center;
  height: 90px;
  padding: 15px 20px;
  color: white;
  position: relative;
  z-index: 2;
  white-space: nowrap;
}

.app-brand .brand-mark {
  width: 49px;
  height: 56px;
}

.app-brand strong {
  font-size: 19px;
  letter-spacing: .3px;
}

.app-brand small {
  display: block;
  font-size: 7px;
  letter-spacing: .3px;
  margin-top: 5px;
  color: #cadcf8;
}

.app-nav {
  position: relative;
  z-index: 2;
  padding: 8px 16px;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.app-nav a {
  height: 57px;
  display: flex;
  align-items: center;
  gap: 17px;
  color: #e5edff;
  border: 1px solid transparent;
  border-radius: 10px;
  padding: 0 19px;
  font-size: 16px;
  white-space: nowrap;
  transition: background .18s;
}

.app-nav .el-icon {
  font-size: 24px;
  color: #c0d4f9;
}

.app-nav a:hover {
  background: #ffffff0d;
}

.app-nav a.active {
  background: linear-gradient(100deg,#185cff,#254c9970);
  border-color: #4a8aff30;
  box-shadow: 0 4px 16px #087dff24,inset 2px 0 0 #71d6ff;
  color: white;
  font-weight: 600;
}

.app-nav a.active .el-icon {
  color: white;
  filter: drop-shadow(0 0 6px #5db3ff);
}

.app-sidebar .sidebar-art {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  z-index: 0;
  mask-image: linear-gradient(transparent,#000 20%);
  opacity: .88;
}

.app-workspace {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.app-topbar {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 27px;
  background: linear-gradient(100deg,#c7ddfc88,#f5faffaa);
  border-bottom: 1px solid #ffffffbd;
}

.nav-toggle {
  color: #5473a5;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 13px;
  cursor: pointer;
  font-size: 14px;
  color: #173466;
}

.user-info .el-avatar {
  background: linear-gradient(140deg,#87bcff,#5265ed);
  box-shadow: 0 2px 7px #688ce124;
}

.user-info:focus-visible {
  outline: 2px solid #3885ff;
  outline-offset: 5px;
}

.app-content {
  flex: 1;
  min-height: 0;
  padding: 0 24px 24px;
  overflow: auto;
  background: radial-gradient(ellipse at 80% 0,#d8eaff80,transparent 42%),#eff7ff;
  border-radius: 20px 0 0 0;
}

.model-content {
  padding: 16px;
}

.nav-collapsed {
  --sidebar-width: 84px;
}

.nav-collapsed .app-brand {
  padding: 15px;
}

.nav-collapsed .app-brand div,.nav-collapsed .app-nav span {
  display: none;
}

.nav-collapsed .app-nav {
  padding: 8px 12px;
}

.nav-collapsed .app-nav a {
  justify-content: center;
  padding: 0;
}

.nav-collapsed .sidebar-art {
  display: none;
}

@media (min-width:1600px) {
  .app-shell {
    --sidebar-width: 280px;
  }
  .nav-collapsed {
    --sidebar-width: 84px;
  }
  .app-brand strong {
    font-size: 21px;
  }
  .app-brand small {
    font-size: 8px;
  }
  .app-brand {
    height: 96px;
  }
  .app-nav {
    gap: 12px;
  }
  .app-nav a {
    height: 61px;
    font-size: 18px;
  }
  .app-content {
    padding: 0 28px 26px;
  }
  .app-topbar {
    height: 60px;
  }
}

@media (max-width:1000px) {
  .app-shell {
    --sidebar-width: 210px;
  }
  .app-brand {
    padding: 12px;
  }
  .app-brand strong {
    font-size: 16px;
  }
  .app-brand small {
    font-size: 6px;
  }
  .app-brand .brand-mark {
    width: 40px;
  }
  .app-nav {
    padding: 8px 12px;
  }
  .app-nav a {
    padding: 0 13px;
    gap: 12px;
    font-size: 15px;
  }
  .app-content {
    padding: 0 16px 20px;
  }
  .nav-collapsed {
    --sidebar-width: 74px;
  }
}

@media (max-width:700px) {
  .app-shell {
    --sidebar-width: 64px;
  }
  .app-brand {
    padding: 10px;
  }
  .app-brand div,.app-nav span,.app-sidebar .sidebar-art {
    display: none;
  }
  .app-nav {
    padding: 4px 7px;
  }
  .app-nav a {
    justify-content: center;
    padding: 0;
    height: 48px;
  }
  .app-nav .el-icon {
    font-size: 22px;
  }
  .app-content {
    padding: 0 10px 16px;
    border-radius: 12px 0 0 0;
  }
  .app-topbar {
    padding: 0 12px;
  }
  .nav-toggle {
    visibility: hidden;
  }
  .user-info {
    gap: 8px;
  }
}
</style>
