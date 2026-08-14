<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bot, Check, Eye, Sparkles, Tags } from 'lucide-vue-next'

import { apiErrorText, createTopic, fetchCategories, moderationCheck, polishDraft as polishDraftApi, suggestTags } from '../api/forum'
import { categories as mockCategories } from '../app/mockData'
import type { Category } from '../api/types'

const title = ref('agent 怎么和 java 配合')
const content = ref('我想问一下服务边界怎么设计，哪些东西放 Java，哪些东西放 Python Agent。')
const categories = ref<Category[]>(mockCategories)
const categoryId = ref(mockCategories[0].id)
const selectedTags = ref<string[]>(['Java', 'Agent'])
const tone = ref('clear')
const riskText = ref('未发现明显风险，发布前仍保留人工确认。')
const working = ref(false)
const error = ref('')
const router = useRouter()

const suggestedTags = computed(() => {
  const text = `${title.value} ${content.value}`.toLowerCase()
  const tags = ['Java', 'Vue3', 'Python', 'MySQL', 'Agent'].filter((tag) => text.includes(tag.toLowerCase()) || text.includes(tag))
  return tags.length ? tags : ['Java', 'Agent', '架构']
})

const previewHtml = computed(() => {
  const escaped = content.value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
  return escaped
    .split(/\n{2,}/)
    .map((paragraph) => `<p>${paragraph}</p>`)
    .join('')
})

async function polishDraft() {
  error.value = ''
  working.value = true
  try {
    const response = await polishDraftApi({
      title: title.value,
      content: content.value,
      tone: tone.value
    })
    title.value = response.title
    content.value = response.content
  } catch (err) {
    error.value = apiErrorText(err, '润色失败，请确认已登录且 Agent 服务可用。')
  } finally {
    working.value = false
  }
}

function toggleTag(tag: string) {
  selectedTags.value = selectedTags.value.includes(tag)
    ? selectedTags.value.filter((item) => item !== tag)
    : [...selectedTags.value, tag]
}

async function refreshTags() {
  error.value = ''
  try {
    const response = await suggestTags({
      title: title.value,
      content: content.value,
      categoryId: categoryId.value
    })
    selectedTags.value = response.tags
  } catch (err) {
    error.value = apiErrorText(err, '标签推荐失败，请稍后再试。')
  }
}

async function checkRisk() {
  error.value = ''
  try {
    const response = await moderationCheck(content.value)
    riskText.value = response.riskLevel === 'LOW'
      ? '未发现明显风险，发布前仍保留人工确认。'
      : `${response.riskLevel}: ${response.reasons.join('；') || '建议人工复核'}`
  } catch (err) {
    error.value = apiErrorText(err, '风险检查失败，请稍后再试。')
  }
}

async function publishTopic() {
  error.value = ''
  working.value = true
  try {
    const created = await createTopic({
      categoryId: categoryId.value,
      title: title.value,
      content: content.value,
      tagNames: selectedTags.value
    })
    await router.push(`/topics/${created.id}`)
  } catch (err) {
    error.value = apiErrorText(err, '发布失败，请确认登录状态和后端服务。')
  } finally {
    working.value = false
  }
}

onMounted(async () => {
  categories.value = await fetchCategories()
  if (!categories.value.some((category) => category.id === categoryId.value)) {
    categoryId.value = categories.value[0]?.id || categoryId.value
  }
})
</script>

<template>
  <div class="editor-layout">
    <section class="editor-main">
      <div class="surface-header">
        <div>
          <h1>发布主题</h1>
          <p>草稿、预览和 AI 建议保持在同一工作面。</p>
        </div>
        <button class="primary-action" type="button" :disabled="working" @click="publishTopic">
          <Check :size="18" aria-hidden="true" />
          <span>发布</span>
        </button>
      </div>

      <div class="editor-grid">
        <form class="draft-form">
          <label>
            <span>标题</span>
            <input v-model="title" maxlength="120" />
          </label>

          <label>
            <span>板块</span>
            <select v-model="categoryId">
              <option v-for="category in categories" :key="category.id" :value="category.id">
                {{ category.name }}
              </option>
            </select>
          </label>

          <label>
            <span>正文</span>
            <textarea v-model="content" rows="14"></textarea>
          </label>

          <div class="tag-row">
            <button
              v-for="tag in suggestedTags"
              :key="tag"
              :class="{ active: selectedTags.includes(tag) }"
              class="tag-button"
              type="button"
              @click="toggleTag(tag)"
            >
              {{ tag }}
            </button>
          </div>
        </form>

        <aside class="preview-pane">
          <div class="dock-head">
            <Eye :size="18" aria-hidden="true" />
            <strong>预览</strong>
          </div>
          <h2>{{ title }}</h2>
          <div class="preview-content" v-html="previewHtml"></div>
        </aside>
      </div>
    </section>

    <aside class="assistant-panel" aria-label="发帖 AI">
      <div class="dock-head">
        <Bot :size="19" aria-hidden="true" />
        <strong>写作辅助</strong>
      </div>

      <p v-if="error" class="form-error">{{ error }}</p>

      <label>
        <span>语气</span>
        <select v-model="tone">
          <option value="clear">清晰</option>
          <option value="friendly">友好</option>
          <option value="technical">技术讨论</option>
        </select>
      </label>

      <button class="tool-link" type="button" :disabled="working" @click="polishDraft">
        <Sparkles :size="17" aria-hidden="true" />
        <span>润色草稿</span>
      </button>
      <button class="tool-link" type="button" @click="refreshTags">
        <Tags :size="17" aria-hidden="true" />
        <span>刷新标签</span>
      </button>
      <button class="tool-link" type="button" @click="checkRisk">
        <Check :size="17" aria-hidden="true" />
        <span>检查风险</span>
      </button>

      <div class="risk-note">
        <strong>风险提示</strong>
        <p>{{ riskText }}</p>
      </div>
    </aside>
  </div>
</template>
