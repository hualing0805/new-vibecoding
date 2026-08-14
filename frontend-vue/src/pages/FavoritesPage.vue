<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Bookmark, Inbox } from 'lucide-vue-next'

import TopicList from '../components/TopicList.vue'
import { apiErrorText, fetchFavoriteTopics } from '../api/forum'
import type { TopicSummary } from '../api/types'

const topics = ref<TopicSummary[]>([])
const loading = ref(true)
const error = ref('')

async function loadFavorites() {
  loading.value = true
  error.value = ''
  try {
    const response = await fetchFavoriteTopics()
    topics.value = response.items
  } catch (err) {
    error.value = apiErrorText(err, '收藏列表加载失败，请确认登录状态。')
  } finally {
    loading.value = false
  }
}

onMounted(loadFavorites)
</script>

<template>
  <div class="single-column-layout">
    <section class="feed-column">
      <div class="surface-header">
        <div>
          <h1>我的收藏</h1>
          <p>回到你保存过的主题。</p>
        </div>
        <div class="status-pill">
          <Bookmark :size="16" aria-hidden="true" />
          <span>{{ topics.length }} 条</span>
        </div>
      </div>

      <p v-if="error" class="form-error">{{ error }}</p>
      <p v-else-if="loading" class="empty-state">正在加载收藏。</p>
      <div v-else-if="!topics.length" class="empty-state">
        <Inbox :size="18" aria-hidden="true" />
        <span>暂无收藏主题</span>
      </div>
      <TopicList v-else :topics="topics" />
    </section>
  </div>
</template>
