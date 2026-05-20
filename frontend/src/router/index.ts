import { createRouter, createWebHistory } from 'vue-router'
import studentRoutes from './modules/student'
import publicRoutes from './modules/public'
import adminRoutes from './modules/admin'
import teacherRoutes from './modules/teacher'

const routes = [
  ...publicRoutes,
  ...studentRoutes,
  ...adminRoutes,
  ...teacherRoutes,
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('../views/ChangePassword.vue'),
    meta: { title: '修改密码' },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { title: '个人信息' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = sessionStorage.getItem('token') || localStorage.getItem('token')

  // 公开页面（无需登录）
  if (to.meta.noAuth) {
    next()
    return
  }

  // 无 token → 跳登录
  if (!token) {
    next('/login')
    return
  }

  // RBAC 角色校验
  const matchedRoles = to.matched.flatMap(r => (r.meta?.roles as string[]) || [])
  if (matchedRoles.length > 0) {
    let userInfo: Record<string, any> | null = null
    try {
      const raw = localStorage.getItem('userInfo')
      if (raw) userInfo = JSON.parse(raw)
    } catch { /* ignore */ }

    if (!userInfo) {
      next('/login')
      return
    }

    const roleCode = (userInfo.roleCode || '').replace('ROLE_', '').toLowerCase()
    if (!matchedRoles.includes(roleCode)) {
      next('/login')
      return
    }
  }

  next()
})

export default router
