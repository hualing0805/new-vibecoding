<script setup lang="ts">
import { ref } from 'vue'
import { LogIn, UserPlus } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'

import { useSessionStore } from '../stores/session'

const username = ref('demo')
const password = ref('ThreadMind@123')
const email = ref('demo@threadmind.local')
const nickname = ref('演示用户')
const mode = ref<'login' | 'register'>('login')
const error = ref('')
const working = ref(false)
const session = useSessionStore()
const router = useRouter()
const route = useRoute()

async function submit() {
  error.value = ''
  working.value = true
  try {
    if (mode.value === 'register') {
      await session.register({
        username: username.value,
        email: email.value,
        password: password.value,
        nickname: nickname.value
      })
    } else {
      await session.login(username.value, password.value)
    }
    await router.push(String(route.query.redirect || '/'))
  } catch {
    error.value = mode.value === 'register'
      ? '注册失败，请检查用户名、邮箱或密码。'
      : '登录失败，请检查用户名和密码。'
  } finally {
    working.value = false
  }
}

function switchMode(nextMode: 'login' | 'register') {
  mode.value = nextMode
  error.value = ''
}
</script>

<template>
  <div class="login-layout">
    <section class="login-panel">
      <div class="surface-header">
        <div>
          <h1>{{ mode === 'login' ? '登录' : '注册' }}</h1>
          <p>进入论坛工作台。</p>
        </div>
      </div>

      <form class="draft-form" @submit.prevent="submit">
        <div class="segmented-control" aria-label="账号操作">
          <button :class="{ active: mode === 'login' }" type="button" @click="switchMode('login')">登录</button>
          <button :class="{ active: mode === 'register' }" type="button" @click="switchMode('register')">注册</button>
        </div>

        <label>
          <span>用户名</span>
          <input v-model="username" autocomplete="username" />
        </label>
        <label v-if="mode === 'register'">
          <span>邮箱</span>
          <input v-model="email" autocomplete="email" />
        </label>
        <label v-if="mode === 'register'">
          <span>昵称</span>
          <input v-model="nickname" autocomplete="nickname" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="password" type="password" :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" />
        </label>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="primary-action" type="submit" :disabled="working">
          <LogIn v-if="mode === 'login'" :size="17" aria-hidden="true" />
          <UserPlus v-else :size="17" aria-hidden="true" />
          <span>{{ mode === 'login' ? '登录' : '创建账号' }}</span>
        </button>
      </form>
    </section>
  </div>
</template>
