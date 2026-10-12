<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Bot, Bell, Bookmark, Home, LogIn, LogOut, PencilLine, Plus, Search, ShieldCheck } from 'lucide-vue-next'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'

import { useSessionStore } from '../stores/session'

const session = useSessionStore()
const router = useRouter()
const route = useRoute()
const searchKeyword = ref('')

const navItems = computed(() => [
  { to: '/', label: '首页', icon: Home, visible: true },
  { to: '/topics/new', label: '发帖', icon: PencilLine, visible: session.isSignedIn },
  { to: '/favorites', label: '收藏', icon: Bookmark, visible: session.isSignedIn },
  { to: '/assistant', label: '助手', icon: Bot, visible: session.isSignedIn },
  { to: '/admin', label: '管理', icon: ShieldCheck, visible: session.isAdmin },
  { to: '/login', label: '登录', icon: LogIn, visible: !session.isSignedIn }
].filter((item) => item.visible))

function logout() {
  session.logout()
  router.push('/login')
}

function submitSearch() {
  const keyword = searchKeyword.value.trim()
  router.push({ path: '/', query: keyword ? { keyword } : {} })
}

onMounted(() => void session.refreshUnreadCount())
watch(() => route.fullPath, () => void session.refreshUnreadCount())
watch(() => route.query.keyword, (keyword) => {
  searchKeyword.value = typeof keyword === 'string' ? keyword : ''
}, { immediate: true })
</script>

<template>
  <div class="app-shell">
    <aside class="side-nav" aria-label="主导航">
      <RouterLink class="brand-mark" to="/" aria-label="ThreadMind 首页">
        <span class="brand-fold" aria-hidden="true"></span>
        <span>
          <strong>ThreadMind</strong>
          <small>论坛工作台</small>
        </span>
      </RouterLink>

      <nav class="nav-stack">
        <RouterLink v-for="item in navItems" :key="item.to" class="nav-item" :to="item.to">
          <component :is="item.icon" :size="18" aria-hidden="true" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <div class="workspace">
      <header class="top-bar">
        <form class="search-box" role="search" @submit.prevent="submitSearch">
          <Search :size="18" aria-hidden="true" />
          <label class="sr-only" for="global-search">搜索帖子、标签、作者</label>
          <input id="global-search" v-model="searchKeyword" type="search" placeholder="搜索帖子、标签、作者" />
        </form>
        <div class="top-actions">
          <span v-if="session.isSignedIn" class="session-chip">{{ session.nickname }}</span>
          <RouterLink class="icon-button" to="/notifications" aria-label="通知">
            <Bell :size="18" aria-hidden="true" />
            <span v-if="session.unreadCount" class="notification-badge">{{ session.unreadCount > 99 ? '99+' : session.unreadCount }}</span>
          </RouterLink>
          <button v-if="session.isSignedIn" class="icon-button" type="button" aria-label="退出登录" @click="logout">
            <LogOut :size="18" aria-hidden="true" />
          </button>
          <RouterLink v-if="session.isSignedIn" class="primary-action header-publish" to="/topics/new">
            <Plus :size="18" aria-hidden="true" />
            <span>发布</span>
          </RouterLink>
        </div>
      </header>

      <main class="page-host">
        <RouterView />
      </main>
    </div>
  </div>
</template>
