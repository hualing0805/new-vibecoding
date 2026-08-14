<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Bell, Check, Inbox } from 'lucide-vue-next'

import { apiErrorText, fetchNotifications, markNotificationRead } from '../api/forum'
import type { NotificationItem } from '../api/types'
import { useSessionStore } from '../stores/session'

const notifications = ref<NotificationItem[]>([])
const loading = ref(true)
const error = ref('')
const workingId = ref<number | null>(null)
const session = useSessionStore()

async function loadNotifications() {
  loading.value = true
  error.value = ''
  try {
    notifications.value = await fetchNotifications()
  } catch (err) {
    error.value = apiErrorText(err, '通知加载失败，请确认登录状态。')
  } finally {
    loading.value = false
  }
}

async function markRead(item: NotificationItem) {
  if (item.read) {
    return
  }
  workingId.value = item.id
  error.value = ''
  try {
    const updated = await markNotificationRead(item.id)
    notifications.value = notifications.value.map((notification) => notification.id === item.id ? updated : notification)
    await session.refreshUnreadCount()
  } catch (err) {
    error.value = apiErrorText(err, '标记已读失败，请稍后再试。')
  } finally {
    workingId.value = null
  }
}

function displayTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN')
}

onMounted(loadNotifications)
</script>

<template>
  <div class="single-column-layout">
    <section class="topic-detail">
      <div class="surface-header">
        <div>
          <h1>通知中心</h1>
          <p>回复、点赞、系统消息和治理处理反馈。</p>
        </div>
        <div class="status-pill">
          <Bell :size="16" aria-hidden="true" />
          <span>{{ notifications.filter((item) => !item.read).length }} 未读</span>
        </div>
      </div>

      <p v-if="error" class="form-error">{{ error }}</p>
      <p v-else-if="loading" class="empty-state">正在加载通知。</p>
      <div v-else-if="!notifications.length" class="empty-state">
        <Inbox :size="18" aria-hidden="true" />
        <span>暂无通知</span>
      </div>

      <div v-else class="notification-list">
        <article v-for="item in notifications" :key="item.id" class="notification-row" :class="{ unread: !item.read }">
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.body }}</p>
            <small>{{ item.type }} · {{ displayTime(item.createdAt) }}</small>
          </div>
          <button class="icon-button" type="button" :disabled="item.read || workingId === item.id" :aria-label="item.read ? '已读' : '标记已读'" @click="markRead(item)">
            <Check :size="17" aria-hidden="true" />
          </button>
        </article>
      </div>
    </section>
  </div>
</template>
