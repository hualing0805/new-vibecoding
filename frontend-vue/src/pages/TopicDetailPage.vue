<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Bot, Heart, MessageSquare, Send, Star } from 'lucide-vue-next'

import { apiErrorText, createReply, favoriteTopic, fetchReplies, fetchTopic, likeTopic, summarizeTopic } from '../api/forum'
import { topics as mockTopics } from '../app/mockData'
import type { Reply, TopicDetail } from '../api/types'

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
const actionError = ref('')
const replying = ref(false)
const reacting = ref(false)

async function loadTopic() {
  actionError.value = ''
  topic.value = await fetchTopic(topicId.value)
  topicReplies.value = await fetchReplies(topicId.value)
  try {
    const aiSummary = await summarizeTopic(topicId.value)
    if (aiSummary.summary) {
      topic.value = { ...topic.value, aiSummary: aiSummary.summary }
      summaryPoints.value = aiSummary.points
    }
  } catch {
    topic.value = { ...topic.value, aiSummary: '登录后可生成 AI 摘要。' }
    summaryPoints.value = []
  }
}

async function like() {
  actionError.value = ''
  reacting.value = true
  try {
    await likeTopic(topic.value.id)
    topic.value = { ...topic.value, likeCount: topic.value.likeCount + 1 }
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
    await favoriteTopic(topic.value.id)
    topic.value = { ...topic.value, favoriteCount: topic.value.favoriteCount + 1 }
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

onMounted(loadTopic)
watch(topicId, loadTopic)
</script>

<template>
  <div class="detail-layout">
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
        <button type="button" :disabled="reacting" @click="like"><Heart :size="17" aria-hidden="true" />点赞 {{ topic.likeCount }}</button>
        <button type="button" :disabled="reacting" @click="favorite"><Star :size="17" aria-hidden="true" />收藏 {{ topic.favoriteCount }}</button>
        <button type="button"><MessageSquare :size="17" aria-hidden="true" />回复 {{ topic.replyCount }}</button>
      </div>
      <p v-if="actionError" class="form-error">{{ actionError }}</p>
    </article>

    <aside class="summary-panel" aria-label="AI 摘要">
      <div class="dock-head">
        <Bot :size="19" aria-hidden="true" />
        <strong>AI 摘要</strong>
      </div>
      <p>{{ topic.aiSummary || '摘要生成中。' }}</p>
      <ul>
        <li v-for="point in summaryPoints" :key="point">{{ point }}</li>
      </ul>
    </aside>

    <section class="reply-section">
      <h2>回复</h2>
      <article v-for="reply in topicReplies" :key="reply.id" class="reply-card">
        <header>
          <strong>{{ reply.author.nickname }}</strong>
          <span>{{ new Date(reply.createdAt).toLocaleString('zh-CN') }}</span>
        </header>
        <p>{{ reply.content }}</p>
        <button type="button"><Heart :size="15" aria-hidden="true" />{{ reply.likeCount }}</button>
      </article>

      <form class="reply-form" @submit.prevent="submitReply">
        <textarea v-model="replyText" rows="4" placeholder="写下回复"></textarea>
        <button class="primary-action" type="submit" :disabled="replying">
          <Send :size="17" aria-hidden="true" />
          <span>{{ replying ? '提交中' : '提交回复' }}</span>
        </button>
      </form>
    </section>
  </div>
</template>
