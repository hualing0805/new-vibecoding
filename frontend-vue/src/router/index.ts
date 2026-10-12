import { createRouter, createWebHistory } from 'vue-router'

import { useSessionStore } from '../stores/session'

const AdminDashboardPage = () => import('../pages/AdminDashboardPage.vue')
const AssistantPage = () => import('../pages/AssistantPage.vue')
const FavoritesPage = () => import('../pages/FavoritesPage.vue')
const HomePage = () => import('../pages/HomePage.vue')
const LoginPage = () => import('../pages/LoginPage.vue')
const NotificationsPage = () => import('../pages/NotificationsPage.vue')
const TopicDetailPage = () => import('../pages/TopicDetailPage.vue')
const TopicEditorPage = () => import('../pages/TopicEditorPage.vue')

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
