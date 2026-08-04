<script setup lang="ts">
import { ref } from 'vue'
import { LogIn } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

import { useSessionStore } from '../stores/session'

const username = ref('demo')
const password = ref('ThreadMind@123')
const session = useSessionStore()
const router = useRouter()

function submit() {
  session.login(username.value)
  router.push('/')
}
</script>

<template>
  <div class="login-layout">
    <section class="login-panel">
      <div class="surface-header">
        <div>
          <h1>登录</h1>
          <p>进入论坛工作台。</p>
        </div>
      </div>

      <form class="draft-form" @submit.prevent="submit">
        <label>
          <span>用户名</span>
          <input v-model="username" autocomplete="username" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="password" type="password" autocomplete="current-password" />
        </label>
        <button class="primary-action" type="submit">
          <LogIn :size="17" aria-hidden="true" />
          <span>登录</span>
        </button>
      </form>
    </section>
  </div>
</template>

