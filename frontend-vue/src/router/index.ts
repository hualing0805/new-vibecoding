import { createRouter, createWebHistory } from 'vue-router'

import AdminDashboardPage from '../pages/AdminDashboardPage.vue'
import AssistantPage from '../pages/AssistantPage.vue'
import FavoritesPage from '../pages/FavoritesPage.vue'
import HomePage from '../pages/HomePage.vue'
import LoginPage from '../pages/LoginPage.vue'
import NotificationsPage from '../pages/NotificationsPage.vue'
import TopicDetailPage from '../pages/TopicDetailPage.vue'
import TopicEditorPage from '../pages/TopicEditorPage.vue'
import { useSessionStore } from '../stores/session'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomePage },
    { path: '/topics/new', name: 'topic-new', component: TopicEditorPage, meta: { requiresAuth: true } },
    { path: '/topics/:id', name: 'topic-detail', component: TopicDetailPage },
    { path: '/favorites', name: 'favorites', component: FavoritesPage, meta: { requiresAuth: true } },
    { path: '/notifications', name: 'notifications', component: NotificationsPage, meta: { requiresAuth: true } },
    { path: '/assistant', name: 'assistant', component: AssistantPage, meta: { requiresAuth: true } },
    { path: '/admin', name: 'admin', component: AdminDashboardPage, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/login', name: 'login', component: LoginPage }
  ]
})

router.beforeEach((to) => {
  const session = useSessionStore()
  if (to.meta.requiresAuth && !session.isSignedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !session.isAdmin) {
    return { path: '/' }
  }
})

export default router
