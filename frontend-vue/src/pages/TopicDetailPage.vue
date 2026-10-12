<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Bot, FileSearch, Heart, MessageSquare, RefreshCw, Send, Star, Wrench } from 'lucide-vue-next'

import {
  apiErrorText,
  createReply,
  favoriteTopic,
  fetchReplies,
  fetchTopic,
  fetchTopicInteractionStatus,
  likeTopic,
  summarizeTopic,
  unfavoriteTopic,
  unlikeTopic
} from '../api/forum'
import { topics as mockTopics } from '../app/mockData'
import ContentState from '../components/ContentState.vue'
import type { AgentCitation, AgentToolCall, InteractionStatus, Reply, TopicDetail } from '../api/types'

const route = useRoute()
const replyText = ref('')
const topicId = computed(() => Number(route.params.id))
const topic = ref<TopicDetail>({
  ...mockTopics[0],
  content: '',
  aiSummary: ''
})
const topicReplies = ref<Reply[]>([])
const summaryPoints = ref<string[]>([])
const summaryCitations = ref<AgentCitation[]>([])
const summaryToolCalls = ref<AgentToolCall[]>([])
const summarySessionId = ref<number | null>(null)
const summaryLoading = ref(false)
const actionError = ref('')
const loadError = ref('')
const loading = ref(true)
const replying = ref(false)
const reacting = ref(false)
const interactionStatus = ref<InteractionStatus>({
  targetType: 'TOPIC',
  targetId: 0,
  liked: false,
  favorited: false,
  likeCount: 0,
  favoriteCount: 0
})
let loadRequestId = 0
let summaryRequestId = 0

async function loadTopic() {
  const currentRequest = ++loadRequestId
  const id = topicId.value
  loading.value = true
  loadError.value = ''
  actionError.value = ''
  if (!Number.isSafeInteger(id) || id <= 0) {
    loadError.value = '主题地址无效，请返回首页重新选择。'
    loading.value = false
    return
  }
  try {
    const [loadedTopic, loadedReplies] = await Promise.all([fetchTopic(id), fetchReplies(id)])
    if (currentRequest !== loadRequestId) {
      return
    }
    topic.value = loadedTopic
    topicReplies.value = loadedReplies
    summaryPoints.value = []
    summaryCitations.value = []
    summaryToolCalls.value = []
    summarySessionId.value = null

    try {
      interactionStatus.value = await fetchTopicInteractionStatus(id)
      if (currentRequest !== loadRequestId) {
        return
      }
      topic.value = {
        ...topic.value,
        likeCount: interactionStatus.value.likeCount,
        favoriteCount: interactionStatus.value.favoriteCount
      }
    } catch {
      interactionStatus.value = {
        targetType: 'TOPIC',
        targetId: id,
        liked: false,
        favorited: false,
        likeCount: topic.value.likeCount,
        favoriteCount: topic.value.favoriteCount
      }
    }
    void loadSummary(false, currentRequest)
  } catch (err) {
    if (currentRequest === loadRequestId) {
      loadError.value = apiErrorText(err, '主题加载失败，请检查服务状态后重试。')
    }
  } finally {
    if (currentRequest === loadRequestId) {
      loading.value = false
    }
  }
}

async function like() {
  actionError.value = ''
  reacting.value = true
  try {
    const response = interactionStatus.value.liked
      ? await unlikeTopic(topic.value.id)
      : await likeTopic(topic.value.id)
    interactionStatus.value = response
    topic.value = { ...topic.value, likeCount: response.likeCount, favoriteCount: response.favoriteCount }
  } catch (err) {
    actionError.value = apiErrorText(err, '点赞失败，请确认已登录。')
  } finally {
    reacting.value = false
  }
}

async function favorite() {
  actionError.value = ''
  reacting.value = true
  try {
    const response = interactionStatus.value.favorited
      ? await unfavoriteTopic(topic.value.id)
      : await favoriteTopic(topic.value.id)
    interactionStatus.value = response
    topic.value = { ...topic.value, likeCount: response.likeCount, favoriteCount: response.favoriteCount }
  } catch (err) {
    actionError.value = apiErrorText(err, '收藏失败，请确认已登录。')
  } finally {
    reacting.value = false
  }
}

async function submitReply() {
  if (!replyText.value.trim()) {
    return
  }
  actionError.value = ''
  replying.value = true
  try {
    await createReply(topic.value.id, replyText.value)
    replyText.value = ''
    topicReplies.value = await fetchReplies(topic.value.id)
    topic.value = { ...topic.value, replyCount: topic.value.replyCount + 1 }
  } catch (err) {
    actionError.value = apiErrorText(err, '回复失败，请确认已登录。')
  } finally {
    replying.value = false
  }
}

async function loadSummary(refresh: boolean, parentRequestId = loadRequestId) {
  const currentSummaryRequest = ++summaryRequestId
  summaryLoading.value = true
  try {
    const aiSummary = await summarizeTopic(topicId.value, refresh)
    if (parentRequestId !== loadRequestId || currentSummaryRequest !== summaryRequestId) {
      return
    }
    applySummary(aiSummary)
  } catch (err) {
    if (parentRequestId !== loadRequestId || currentSummaryRequest !== summaryRequestId) {
      return
    }
    if (refresh) {
      actionError.value = apiErrorText(err, '摘要刷新失败，已保留当前内容。')
    }
    if (!topic.value.aiSummary) {
      topic.value = { ...topic.value, aiSummary: '登录后可生成 AI 摘要。' }
      summaryPoints.value = []
      summaryCitations.value = []
      summaryToolCalls.value = []
      summarySessionId.value = null
    }
  } finally {
    if (currentSummaryRequest === summaryRequestId) {
      summaryLoading.value = false
    }
  }
}

function applySummary(aiSummary: Awaited<ReturnType<typeof summarizeTopic>>) {
  if (!aiSummary.summary) {
    return
  }
  topic.value = { ...topic.value, aiSummary: aiSummary.summary }
  summaryPoints.value = aiSummary.points
  summaryCitations.value = aiSummary.citations || []
  summaryToolCalls.value = aiSummary.toolCalls || []
  summarySessionId.value = aiSummary.sessionId ?? null
}

onMounted(loadTopic)
watch(topicId, loadTopic)
</script>

<template>
  <div class="detail-layout">
    <ContentState v-if="loadError" class="detail-state" kind="error" :message="loadError" @retry="loadTopic" />
    <ContentState v-else-if="loading" class="detail-state" kind="loading" message="正在加载主题" />
    <template v-else>
    <article class="topic-detail">
      <div class="topic-card__top">
        <span class="category-chip">{{ topic.category.name }}</span>
        <span class="topic-time">{{ topic.author.nickname }}</span>
      </div>

      <h1>{{ topic.title }}</h1>
      <div class="tag-row">
        <span v-for="tag in topic.tags" :key="tag" class="tag-chip">{{ tag }}</span>
      </div>

      <div class="prose-block">
        <p v-for="paragraph in topic.content.split(/\n{2,}/).filter(Boolean)" :key="paragraph">
          {{ paragraph }}
        </p>
      </div>

      <div class="action-row">
        <button type="button" :class="{ active: interactionStatus.liked }" :disabled="reacting" @click="like">
          <Heart :size="17" aria-hidden="true" />{{ interactionStatus.liked ? '已点赞' : '点赞' }} {{ topic.likeCount }}
        </button>
        <button type="button" :class="{ active: interactionStatus.favorited }" :disabled="reacting" @click="favorite">
          <Star :size="17" aria-hidden="true" />{{ interactionStatus.favorited ? '已收藏' : '收藏' }} {{ topic.favoriteCount }}
        </button>
        <a class="secondary-action" href="#reply-form"><MessageSquare :size="17" aria-hidden="true" />回复 {{ topic.replyCount }}</a>
      </div>
      <p v-if="actionError" class="form-error">{{ actionError }}</p>
    </article>

    <aside class="summary-panel" aria-label="AI 摘要">
      <div class="panel-title-row">
        <div class="dock-head">
          <Bot :size="19" aria-hidden="true" />
          <strong>AI 摘要</strong>
        </div>
        <button class="icon-button" type="button" :disabled="summaryLoading" aria-label="刷新摘要" @click="loadSummary(true)">
          <RefreshCw :size="17" aria-hidden="true" />
        </button>
      </div>
      <p>{{ summaryLoading && !topic.aiSummary ? '摘要生成中。' : topic.aiSummary || '摘要生成中。' }}</p>
      <ul>
        <li v-for="point in summaryPoints" :key="point">{{ point }}</li>
      </ul>
      <div class="summary-audit">
        <small>Session {{ summarySessionId || '未生成' }}</small>
        <div class="dock-head">
          <FileSearch :size="16" aria-hidden="true" />
          <strong>引用</strong>
        </div>
        <RouterLink v-for="item in summaryCitations" :key="item.topicId" class="audit-row" :to="`/topics/${item.topicId}`">
          {{ item.title }}
        </RouterLink>
        <p v-if="!summaryCitations.length" class="muted-line">暂无引用</p>

        <div class="dock-head">
          <Wrench :size="16" aria-hidden="true" />
          <strong>工具</strong>
        </div>
        <div v-for="tool in summaryToolCalls" :key="`${tool.toolName}-${tool.status}-${tool.elapsedMs}`" class="audit-row">
          <strong>{{ tool.toolName }}</strong>
          <span>{{ tool.status }}{{ tool.elapsedMs !== undefined && tool.elapsedMs !== null ? ` · ${tool.elapsedMs}ms` : '' }}</span>
        </div>
        <p v-if="!summaryToolCalls.length" class="muted-line">暂无工具调用</p>
      </div>
    </aside>

    <section class="reply-section">
      <h2>回复</h2>
      <article v-for="reply in topicReplies" :key="reply.id" class="reply-card">
        <header>
          <strong>{{ reply.author.nickname }}</strong>
          <span>{{ new Date(reply.createdAt).toLocaleString('zh-CN') }}</span>
        </header>
        <p>{{ reply.content }}</p>
        <span class="reply-like-count"><Heart :size="15" aria-hidden="true" />{{ reply.likeCount }}</span>
      </article>
      <ContentState v-if="!topicReplies.length" kind="empty" message="暂无回复，来写第一条回复" />

      <form id="reply-form" class="reply-form" @submit.prevent="submitReply">
        <label class="sr-only" for="reply-content">回复内容</label>
        <textarea id="reply-content" v-model="replyText" rows="4" placeholder="写下回复"></textarea>
        <button class="primary-action" type="submit" :disabled="replying">
          <Send :size="17" aria-hidden="true" />
          <span>{{ replying ? '提交中' : '提交回复' }}</span>
        </button>
      </form>
    </section>
    </template>
  </div>
</template>
