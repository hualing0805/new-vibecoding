<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CheckCircle2, EyeOff, Lock, ShieldCheck } from 'lucide-vue-next'

import { apiErrorText, fetchReports, handleReport } from '../api/forum'
import type { ReportItem } from '../api/types'

const reports = ref<ReportItem[]>([])
const selectedReportId = ref<number | null>(null)
const error = ref('')
const working = ref(false)
const pendingCount = computed(() => reports.value.filter((report) => report.status === 'PENDING').length)

async function loadReports() {
  error.value = ''
  try {
    reports.value = await fetchReports()
    selectedReportId.value = reports.value[0]?.id || null
  } catch (err) {
    error.value = apiErrorText(err, '举报列表加载失败，请确认管理员权限。')
  }
}

async function moderate(action: string) {
  if (!selectedReportId.value) {
    return
  }
  error.value = ''
  working.value = true
  try {
    await handleReport(selectedReportId.value, action, 'M2.5 管理台验收处理')
    reports.value = reports.value.map((report) => report.id === selectedReportId.value
      ? { ...report, status: action === 'IGNORE' ? 'REJECTED' : 'ACCEPTED' }
      : report
    )
  } catch (err) {
    error.value = apiErrorText(err, '处理举报失败，请稍后再试。')
  } finally {
    working.value = false
  }
}

function displayTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN')
}

onMounted(loadReports)
</script>

<template>
  <div class="admin-layout">
    <section class="admin-main">
      <div class="surface-header">
        <div>
          <h1>治理队列</h1>
          <p>举报、风险提示和处理记录。</p>
        </div>
        <div class="status-pill">
          <ShieldCheck :size="16" aria-hidden="true" />
          <span>{{ pendingCount }} 待处理</span>
        </div>
      </div>

      <div class="admin-actions">
        <button type="button" :disabled="working" @click="moderate('IGNORE')"><CheckCircle2 :size="17" aria-hidden="true" />驳回</button>
        <button type="button" :disabled="working" @click="moderate('HIDE')"><EyeOff :size="17" aria-hidden="true" />隐藏</button>
        <button type="button" :disabled="working" @click="moderate('LOCK')"><Lock :size="17" aria-hidden="true" />锁定</button>
      </div>
      <p v-if="error" class="form-error">{{ error }}</p>

      <div class="report-table" role="table" aria-label="举报列表">
        <div class="report-row report-row--head" role="row">
          <span>目标</span>
          <span>原因</span>
          <span>AI 风险</span>
          <span>状态</span>
          <span>时间</span>
        </div>
        <button
          v-for="report in reports"
          :key="report.id"
          class="report-row report-row--selectable"
          :class="{ active: selectedReportId === report.id }"
          role="row"
          type="button"
          @click="selectedReportId = report.id"
        >
          <span>{{ report.target }}</span>
          <span>{{ report.reason }}</span>
          <span>{{ report.risk }}</span>
          <span>{{ report.status }}</span>
          <span>{{ displayTime(report.createdAt) }}</span>
        </button>
      </div>
    </section>

    <aside class="side-panel">
      <h2>审计</h2>
      <div class="scope-row">
        <strong>traceId</strong>
        <span>开启</span>
      </div>
      <div class="scope-row">
        <strong>AI 工具调用</strong>
        <span>记录</span>
      </div>
      <div class="scope-row">
        <strong>人工确认</strong>
        <span>必需</span>
      </div>
    </aside>
  </div>
</template>
