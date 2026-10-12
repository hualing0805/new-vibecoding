<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Activity, Flame, MessageSquareText, RadioTower } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'

import AiDock from '../components/AiDock.vue'
import ContentState from '../components/ContentState.vue'
import TopicList from '../components/TopicList.vue'
import { apiErrorText, apiModeLabel, fetchCategories, fetchTopics } from '../api/forum'
import type { Category, TopicSummary } from '../api/types'

const activeSort = ref<'latest' | 'hot' | 'unanswered'>('latest')
const activeCategory = ref<number | null>(null)
const categories = ref<Category[]>([])
const topics = ref<TopicSummary[]>([])
const loading = ref(true)
const error = ref('')
let requestId = 0
const route = useRoute()
const router = useRouter()

const totalHeat = computed(() => topics.value.reduce((sum, topic) => sum + topic.likeCount, 0))
const totalAiCalls = computed(() => Math.max(5, categories.value.find((category) => category.slug === 'agent')?.topicCount || 0))
const activeKeyword = computed(() => typeof route.query.keyword === 'string' ? route.query.keyword.trim() : '')

async function loadTopics() {
  const currentRequest = ++requestId
  loading.value = true
  error.value = ''
  try {
    const response = await fetchTopics({
      categoryId: activeCategory.value,
      sort: activeSort.value,
      keyword: activeKeyword.value
    })
    if (currentRequest === requestId) {
      topics.value = response.items
    }
  } catch (err) {
    if (currentRequest === requestId) {
      error.value = apiErrorText(err, '主题加载失败，请检查服务状态后重试。')
    }
  } finally {
    if (currentRequest === requestId) {
      loading.value = false
    }
  }
}

onMounted(async () => {
  const [categoryResult] = await Promise.allSettled([fetchCategories(), loadTopics()])
  if (categoryResult.status === 'fulfilled') {
    categories.value = categoryResult.value
  }
})

watch([activeSort, activeCategory, activeKeyword], () => {
  void loadTopics()
})

function clearKeyword() {
  router.push({ path: '/' })
}
</script>

<template>
  <div class="home-layout">
    <section class="feed-column">
      <div class="surface-header">
        <div>
          <h1>社区脉冲</h1>
          <p>最新讨论、活跃板块和 AI 辅助状态。</p>
        </div>
        <div class="status-pill">
          <Activity :size="16" aria-hidden="true" />
          <span>{{ loading ? '同步中' : apiModeLabel }}</span>
        </div>
      </div>

      <div class="metric-row" aria-label="论坛状态">
        <div class="metric-cell">
          <MessageSquareText :size="18" aria-hidden="true" />
          <strong>{{ topics.length }}</strong>
          <span>主题</span>
        </div>
        <div class="metric-cell">
          <Flame :size="18" aria-hidden="true" />
          <strong>{{ totalHeat }}</strong>
          <span>热度</span>
        </div>
        <div class="metric-cell">
          <RadioTower :size="18" aria-hidden="true" />
          <strong>{{ totalAiCalls }}</strong>
          <span>AI 调用</span>
        </div>
      </div>

      <div class="control-band">
        <div class="segmented-control" aria-label="排序方式">
          <button :class="{ active: activeSort === 'latest' }" :aria-pressed="activeSort === 'latest'" type="button" @click="activeSort = 'latest'">最新</button>
          <button :class="{ active: activeSort === 'hot' }" :aria-pressed="activeSort === 'hot'" type="button" @click="activeSort = 'hot'">热门</button>
          <button :class="{ active: activeSort === 'unanswered' }" :aria-pressed="activeSort === 'unanswered'" type="button" @click="activeSort = 'unanswered'">待回复</button>
        </div>
        <button v-if="activeKeyword" class="filter-chip" type="button" @click="clearKeyword">
          搜索：{{ activeKeyword }}
        </button>
      </div>

      <ContentState v-if="error" kind="error" :message="error" @retry="loadTopics" />
      <ContentState v-else-if="loading" kind="loading" message="正在同步主题" />
      <ContentState v-else-if="!topics.length" kind="empty" message="当前筛选下没有主题" />
      <TopicList v-else :topics="topics" />
    </section>

    <section class="side-column" aria-label="板块与助手">
      <AiDock />

      <div class="side-panel">
        <h2>板块</h2>
        <button
          v-for="category in categories"
          :key="category.id"
          class="category-row"
          :class="{ active: activeCategory === category.id }"
          :aria-pressed="activeCategory === category.id"
          type="button"
          @click="activeCategory = activeCategory === category.id ? null : category.id"
        >
          <span>
            <strong>{{ category.name }}</strong>
            <small>{{ category.description }}</small>
          </span>
          <em>{{ category.topicCount }}</em>
        </button>
      </div>
    </section>
  </div>
</template>
