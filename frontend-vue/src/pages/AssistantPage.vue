<script setup lang="ts">
import { ref } from 'vue'
import { Bot, CornerDownLeft, SearchCheck, UserRound } from 'lucide-vue-next'

import { askAgent } from '../api/forum'

const prompt = ref('最近大家怎么讨论 Agent 和 Java 的边界？')
const messages = ref([
  {
    role: 'assistant',
    text: '可以问站内内容、发帖草稿、标签和摘要。'
  }
])
const toolStatus = ref('等待提问')
const asking = ref(false)

async function ask() {
  if (!prompt.value.trim()) {
    return
  }
  const message = prompt.value
  messages.value.push({ role: 'user', text: message })
  asking.value = true
  const response = await askAgent(message)
  messages.value.push({
    role: 'assistant',
    text: response.answer
  })
  toolStatus.value = response.toolCalls.length
    ? response.toolCalls.map((tool) => `${tool.toolName}:${tool.status}`).join(' / ')
    : '无工具调用'
  asking.value = false
  prompt.value = ''
}
</script>

<template>
  <div class="assistant-layout">
    <section class="chat-surface">
      <div class="surface-header">
        <div>
          <h1>站内助手</h1>
          <p>基于可见帖子和项目规则回答。</p>
        </div>
        <div class="status-pill">
          <SearchCheck :size="16" aria-hidden="true" />
          <span>{{ toolStatus }}</span>
        </div>
      </div>

      <div class="message-list">
        <article v-for="(message, index) in messages" :key="index" class="message-row" :class="message.role">
          <span class="message-avatar">
            <Bot v-if="message.role === 'assistant'" :size="17" aria-hidden="true" />
            <UserRound v-else :size="17" aria-hidden="true" />
          </span>
          <p>{{ message.text }}</p>
        </article>
      </div>

      <form class="chat-input" @submit.prevent="ask">
        <textarea v-model="prompt" rows="3" placeholder="输入问题"></textarea>
        <button class="primary-action" type="submit" :disabled="asking">
          <CornerDownLeft :size="17" aria-hidden="true" />
          <span>{{ asking ? '发送中' : '发送' }}</span>
        </button>
      </form>
    </section>

    <aside class="side-panel assistant-side">
      <h2>引用范围</h2>
      <div class="scope-row">
        <strong>公开帖子</strong>
        <span>允许</span>
      </div>
      <div class="scope-row">
        <strong>隐藏内容</strong>
        <span>拒绝</span>
      </div>
      <div class="scope-row">
        <strong>管理备注</strong>
        <span>拒绝</span>
      </div>
    </aside>
  </div>
</template>
