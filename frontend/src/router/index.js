import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../views/Layout.vue'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import DeviceList from '../views/DeviceList.vue'
import WorkOrder from '../views/WorkOrder.vue'

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
        meta: { title: '设备管理', requiresAuth: true }
      },
      {
        path: 'workorder',
        name: 'WorkOrder',
        component: WorkOrder,
        meta: { title: '工单调度', requiresAuth: true }
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
  
  // 其他情况正常放行
  next()
})

export default router
