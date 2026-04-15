import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '雅苑 · 墨韵' }
  },
  {
    path: '/news',
    name: 'News',
    component: () => import('@/views/NewsView.vue'),
    meta: { title: 'AI 新闻早间报 · 雅苑' }
  },
  {
    // 兜底重定向
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0, behavior: 'smooth' })
})

// 动态修改页面标题
router.afterEach((to) => {
  document.title = to.meta?.title || '雅苑'
})

export default router
