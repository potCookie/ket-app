import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/HomeView.vue'),
    meta: { tabIndex: 0, title: '首页', requiresAuth: true }
  },
  {
    path: '/learn',
    name: 'Learn',
    component: () => import('../views/LearnView.vue'),
    meta: { tabIndex: 1, title: '学习', requiresAuth: true }
  },
  {
    path: '/achieve',
    name: 'Achieve',
    component: () => import('../views/AchieveView.vue'),
    meta: { tabIndex: 2, title: '成就', requiresAuth: true }
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('../views/HistoryView.vue'),
    meta: { tabIndex: 3, title: '学习记录', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/ProfileView.vue'),
    meta: { tabIndex: 4, title: '我的', requiresAuth: true }
  },
  {
    path: '/parent',
    name: 'Parent',
    component: () => import('../views/ParentView.vue'),
    meta: { title: '家长入口', requiresAuth: true }
  },
  {
    path: '/plan-config',
    name: 'PlanConfig',
    component: () => import('../views/PlanConfigView.vue'),
    meta: { title: '学习计划', requiresAuth: true }
  },
  {
    path: '/onboarding',
    name: 'Onboarding',
    component: () => import('../views/OnboardingView.vue'),
    meta: { title: '初始设置', requiresAuth: true }
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

// --- Navigation guard ---
router.beforeEach(async (to, _from, next) => {
  // Init auth store (must be inside guard since Pinia needs app context)
  const authStore = useAuthStore()

  // Pages that don't require auth
  if (to.meta.noAuth) {
    // If already logged in and going to login, redirect home
    if (authStore.isLoggedIn && to.path === '/login') {
      return next('/home')
    }
    return next()
  }

  // Check auth for protected pages
  if (to.meta.requiresAuth) {
    if (!authStore.isLoggedIn) {
      return next('/login')
    }
    
    // Validate token on navigation (in case it expired)
    if (!authStore.user) {
      try {
        await authStore.fetchMe()
      } catch {
        // Token invalid, redirect to login
        authStore.clearAuth()
        return next('/login')
      }
    }
  }

  next()
})

export default router
