import { createRouter, createWebHistory } from 'vue-router'

import AdminDashboardPage from '../pages/AdminDashboardPage.vue'
import AssistantPage from '../pages/AssistantPage.vue'
import HomePage from '../pages/HomePage.vue'
import LoginPage from '../pages/LoginPage.vue'
import TopicDetailPage from '../pages/TopicDetailPage.vue'
import TopicEditorPage from '../pages/TopicEditorPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomePage },
    { path: '/topics/new', name: 'topic-new', component: TopicEditorPage },
    { path: '/topics/:id', name: 'topic-detail', component: TopicDetailPage },
    { path: '/assistant', name: 'assistant', component: AssistantPage },
    { path: '/admin', name: 'admin', component: AdminDashboardPage },
    { path: '/login', name: 'login', component: LoginPage }
  ]
})

export default router

