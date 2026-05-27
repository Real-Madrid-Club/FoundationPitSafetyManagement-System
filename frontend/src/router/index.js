import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../views/Layout.vue'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import DeviceList from '../views/DeviceList.vue'
import WorkOrder from '../views/WorkOrder.vue'
import MonitorData from '../views/MonitorData.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '首页大盘', requiresAuth: true }
      },
      {
        path: 'device',
        name: 'DeviceList',
        component: DeviceList,
        meta: { title: '设备管理', requiresAuth: true, roles: ['ROLE_ADMIN'] }
      },
      {
        path: 'workorder',
        name: 'WorkOrder',
        component: WorkOrder,
        meta: { title: '工单调度', requiresAuth: true }
      },
      {
        path: 'ai-chat',
        name: 'AiChat',
        component: () => import('../views/AiChat.vue'),
        meta: { title: 'AI助手', requiresAuth: true }
      },
      {
        path: 'monitor',
        name: 'MonitorData',
        component: MonitorData,
        meta: { title: '监测数据', requiresAuth: true }
      },
      {
        path: 'foundation-pit',
        name: 'FoundationPit3D',
        component: () => import('../views/FoundationPit3D.vue'),
        meta: { title: '基坑模型', requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const isAuthenticated = localStorage.getItem('token')
  
  // 如果访问登录页，直接放行
  if (to.path === '/login') {
    next()
    return
  }
  
  // 如果没有token且不是登录页，重定向到登录页
  if (!isAuthenticated && to.meta.requiresAuth) {
    next('/login')
    return
  }

  // 角色白名单校验
  if (to.meta.roles) {
    let userRoles = []
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      userRoles = user?.roles || user?.userInfo?.roles || []
    } catch { /* ignore */ }
    const allowed = to.meta.roles.some(r => userRoles.includes(r))
    if (!allowed) {
      next('/dashboard')
      return
    }
  }
  
  // 其他情况正常放行
  next()
})

export default router
