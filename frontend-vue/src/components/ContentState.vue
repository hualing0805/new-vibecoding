<script setup lang="ts">
import { Inbox, LoaderCircle, RefreshCw, TriangleAlert } from 'lucide-vue-next'

const props = defineProps<{
  kind: 'loading' | 'error' | 'empty'
  message: string
  retryLabel?: string
}>()

defineEmits<{
  retry: []
}>()
</script>

<template>
  <div class="content-state" :class="`content-state--${props.kind}`" :role="props.kind === 'error' ? 'alert' : 'status'" aria-live="polite">
    <LoaderCircle v-if="props.kind === 'loading'" class="content-state__spinner" :size="20" aria-hidden="true" />
    <TriangleAlert v-else-if="props.kind === 'error'" :size="20" aria-hidden="true" />
    <Inbox v-else :size="20" aria-hidden="true" />
    <span>{{ props.message }}</span>
    <button v-if="props.kind === 'error'" class="content-state__retry" type="button" @click="$emit('retry')">
      <RefreshCw :size="16" aria-hidden="true" />
      <span>{{ props.retryLabel || '重试' }}</span>
    </button>
  </div>
</template>
