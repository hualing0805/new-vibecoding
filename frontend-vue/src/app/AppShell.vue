<script setup lang="ts">
import { Bot, Bell, Home, LogIn, PencilLine, Plus, Search, ShieldCheck } from 'lucide-vue-next'
import { RouterLink, RouterView } from 'vue-router'

const navItems = [
  { to: '/', label: '首页', icon: Home },
  { to: '/topics/new', label: '发帖', icon: PencilLine },
  { to: '/assistant', label: '助手', icon: Bot },
  { to: '/admin', label: '管理', icon: ShieldCheck },
  { to: '/login', label: '登录', icon: LogIn }
]
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
        <label class="search-box">
          <Search :size="18" aria-hidden="true" />
          <input placeholder="搜索帖子、标签、作者" />
        </label>
        <div class="top-actions">
          <button class="icon-button" type="button" aria-label="通知">
            <Bell :size="18" aria-hidden="true" />
          </button>
          <RouterLink class="primary-action" to="/topics/new">
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

