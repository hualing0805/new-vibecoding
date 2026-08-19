<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Bot, Clock3, CornerDownLeft, FileSearch, History, RefreshCw, SearchCheck, UserRound, Wrench } from 'lucide-vue-next'

import { apiErrorText, askAgent, fetchMyAiSession, fetchMyAiSessions } from '../api/forum'
import type { AgentCitation, AgentToolCall, AiSessionListItem } from '../api/types'

type ChatMessage = {
  role: 'assistant' | 'user'
  text: string
}

const prompt = ref('最近大家怎么讨论 Agent 和 Java 的边界？')
const messages = ref<ChatMessage[]>([
  {
    role: 'assistant',
    text: '可以问站内内容、发帖草稿、标签和摘要。'
  }
])
const toolStatus = ref('等待提问')
const citations = ref<AgentCitation[]>([])
const toolCalls = ref<AgentToolCall[]>([])
const historyItems = ref<AiSessionListItem[]>([])
const sessionId = ref<number | null>(null)
const asking = ref(false)
const loadingHistory = ref(false)
const error = ref('')

async function ask() {
  if (!prompt.value.trim()) {
    return
  }
  const message = prompt.value
  messages.value.push({ role: 'user', text: message })
  asking.value = true
  error.value = ''
  try {
    const response = await askAgent(message, sessionId.value)
    const calls = response.toolCalls || []
    const refs = response.citations || []
    messages.value.push({
      role: 'assistant',
      text: response.answer
    })
    toolStatus.value = calls.length
      ? calls.map((tool) => `${tool.toolName}:${tool.status}`).join(' / ')
      : '无工具调用'
    citations.value = refs
    toolCalls.value = calls
    sessionId.value = response.sessionId ?? sessionId.value
    prompt.value = ''
    await loadHistory()
  } catch (err) {
    error.value = apiErrorText(err, '助手请求失败，请确认登录状态和服务可用性。')
    toolStatus.value = '请求失败'
    toolCalls.value = []
    citations.value = []
  } finally {
    asking.value = false
  }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const page = await fetchMyAiSessions({ pageSize: 8 })
    historyItems.value = page.items
  } catch {
    historyItems.value = []
  } finally {
    loadingHistory.value = false
  }
}

async function openHistory(item: AiSessionListItem) {
  error.value = ''
  try {
    const detail = await fetchMyAiSession(item.sessionId)
    sessionId.value = detail.session.sessionId
    messages.value = detail.messages.map((message) => ({
      role: message.role === 'USER' ? 'user' : 'assistant',
      text: message.content
    }))
    if (!messages.value.length) {
      messages.value = [{ role: 'assistant', text: '这个 session 暂无可展示消息。' }]
    }
    toolCalls.value = detail.toolCalls.map((tool) => ({
      toolName: tool.toolName,
      status: tool.status,
      elapsedMs: tool.elapsedMs,
      summary: tool.outputSummary || tool.inputSummary,
      error: tool.errorSummary
    }))
    citations.value = []
    toolStatus.value = `${detail.session.scene}:${detail.session.status}`
  } catch (err) {
    error.value = apiErrorText(err, '历史会话加载失败。')
  }
}

function displayTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(loadHistory)
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
        <p v-if="error" class="form-error">{{ error }}</p>
        <textarea v-model="prompt" rows="3" placeholder="输入问题"></textarea>
        <button class="primary-action" type="submit" :disabled="asking">
          <CornerDownLeft :size="17" aria-hidden="true" />
          <span>{{ asking ? '发送中' : '发送' }}</span>
        </button>
      </form>
    </section>

    <aside class="side-panel assistant-side">
      <div class="panel-title-row">
        <h2>AI 审计</h2>
        <button class="icon-button" type="button" :disabled="loadingHistory" aria-label="刷新历史" @click="loadHistory">
          <RefreshCw :size="17" aria-hidden="true" />
        </button>
      </div>
      <div class="scope-row">
        <strong>Session</strong>
        <span>{{ sessionId || '未生成' }}</span>
      </div>

      <div class="audit-block">
        <div class="dock-head">
          <History :size="17" aria-hidden="true" />
          <strong>历史</strong>
        </div>
        <button
          v-for="item in historyItems"
          :key="item.sessionId"
          class="history-row"
          :class="{ active: sessionId === item.sessionId }"
          type="button"
          @click="openHistory(item)"
        >
          <span>
            <strong>#{{ item.sessionId }} · {{ item.scene }}</strong>
            <small>{{ displayTime(item.updatedAt) }}</small>
          </span>
          <em>{{ item.status }}</em>
        </button>
        <p v-if="!historyItems.length" class="muted-line">暂无历史</p>
      </div>

      <div class="audit-block">
        <div class="dock-head">
          <FileSearch :size="17" aria-hidden="true" />
          <strong>引用</strong>
        </div>
        <RouterLink v-for="item in citations" :key="item.topicId" class="audit-row" :to="`/topics/${item.topicId}`">
          {{ item.title }}
        </RouterLink>
        <p v-if="!citations.length" class="muted-line">暂无引用</p>
      </div>

      <div class="audit-block">
        <div class="dock-head">
          <Wrench :size="17" aria-hidden="true" />
          <strong>工具调用</strong>
        </div>
        <div v-for="tool in toolCalls" :key="`${tool.toolName}-${tool.status}-${tool.elapsedMs}`" class="audit-row">
          <strong>{{ tool.toolName }}</strong>
          <span>{{ tool.status }}{{ tool.elapsedMs !== undefined && tool.elapsedMs !== null ? ` · ${tool.elapsedMs}ms` : '' }}</span>
          <small v-if="tool.summary">{{ tool.summary }}</small>
          <small v-if="tool.error">{{ tool.error }}</small>
        </div>
        <p v-if="!toolCalls.length" class="muted-line">暂无工具调用</p>
      </div>

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
