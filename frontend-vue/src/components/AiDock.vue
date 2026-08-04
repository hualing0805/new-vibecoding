<script setup lang="ts">
import { Bot, CheckCircle2, Sparkles, Tags } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
</script>

<template>
  <aside class="ai-dock" aria-label="AI 辅助">
    <div class="dock-head">
      <Bot :size="19" aria-hidden="true" />
      <strong>AI Dock</strong>
    </div>

    <div class="ai-brief">
      <p>热议集中在服务边界、发帖编辑器和站内检索。</p>
      <RouterLink to="/assistant">打开助手</RouterLink>
    </div>

    <div class="tool-strip">
      <RouterLink class="tool-link" to="/topics/new">
        <Sparkles :size="17" aria-hidden="true" />
        <span>润色草稿</span>
      </RouterLink>
      <RouterLink class="tool-link" to="/topics/new">
        <Tags :size="17" aria-hidden="true" />
        <span>推荐标签</span>
      </RouterLink>
      <RouterLink class="tool-link" to="/admin">
        <CheckCircle2 :size="17" aria-hidden="true" />
        <span>风险复核</span>
      </RouterLink>
    </div>
  </aside>
</template>

