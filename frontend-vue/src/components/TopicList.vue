<script setup lang="ts">
import { Eye, Heart, MessageSquare, Star } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'

import type { TopicSummary } from '../api/types'

defineProps<{
  topics: TopicSummary[]
}>()
</script>

<template>
  <div class="topic-list">
    <article v-for="topic in topics" :key="topic.id" class="topic-card">
      <div class="topic-card__top">
        <span class="category-chip">{{ topic.category.name }}</span>
        <span class="topic-time">{{ new Date(topic.createdAt).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) }}</span>
      </div>

      <RouterLink class="topic-title" :to="`/topics/${topic.id}`">
        {{ topic.title }}
      </RouterLink>
      <p class="topic-excerpt">{{ topic.excerpt }}</p>

      <div class="tag-row">
        <span v-for="tag in topic.tags" :key="tag" class="tag-chip">{{ tag }}</span>
      </div>

      <footer class="topic-card__footer">
        <span>{{ topic.author.nickname }}</span>
        <span><MessageSquare :size="15" aria-hidden="true" />{{ topic.replyCount }}</span>
        <span><Heart :size="15" aria-hidden="true" />{{ topic.likeCount }}</span>
        <span><Star :size="15" aria-hidden="true" />{{ topic.favoriteCount }}</span>
        <span><Eye :size="15" aria-hidden="true" />{{ topic.viewCount }}</span>
      </footer>
    </article>
  </div>
</template>

