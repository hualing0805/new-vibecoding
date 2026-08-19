<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Bot, CheckCircle2, Database, EyeOff, FileSearch, Lock, RefreshCw, ShieldCheck, Wrench } from 'lucide-vue-next'

import {
  apiErrorText,
  fetchAdminAiMetrics,
  fetchAdminAiSession,
  fetchAdminAiSessions,
  fetchReports,
  handleReport
} from '../api/forum'
import type { AiAuditMetrics, AiSessionDetail, AiSessionListItem, ReportItem } from '../api/types'

const activeTab = ref<'reports' | 'ai'>('reports')
const reports = ref<ReportItem[]>([])
const selectedReportId = ref<number | null>(null)
const reportStatus = ref('')
const aiSessions = ref<AiSessionListItem[]>([])
const selectedAiSession = ref<AiSessionDetail | null>(null)
const aiMetrics = ref<AiAuditMetrics | null>(null)
const error = ref('')
const working = ref(false)
const pendingCount = computed(() => reports.value.filter((report) => report.status === 'PENDING').length)
const selectedReport = computed(() => reports.value.find((report) => report.id === selectedReportId.value) || null)

async function loadReports() {
  error.value = ''
  try {
    reports.value = await fetchReports(reportStatus.value || undefined)
    selectedReportId.value = reports.value[0]?.id || null
  } catch (err) {
    error.value = apiErrorText(err, '举报列表加载失败，请确认管理员权限。')
  }
}

async function loadAiAudit() {
  error.value = ''
  try {
    const [metrics, page] = await Promise.all([
      fetchAdminAiMetrics(),
      fetchAdminAiSessions({ pageSize: 12 })
    ])
    aiMetrics.value = metrics
    aiSessions.value = page.items
    if (page.items[0]) {
      selectedAiSession.value = await fetchAdminAiSession(page.items[0].sessionId)
    }
  } catch (err) {
    error.value = apiErrorText(err, 'AI 审计加载失败，请确认管理员权限。')
  }
}

async function selectAiSession(session: AiSessionListItem) {
  error.value = ''
  try {
    selectedAiSession.value = await fetchAdminAiSession(session.sessionId)
  } catch (err) {
    error.value = apiErrorText(err, 'AI session 详情加载失败。')
  }
}

async function moderate(action: string) {
  if (!selectedReportId.value) {
    return
  }
  error.value = ''
  working.value = true
  try {
    await handleReport(selectedReportId.value, action, 'M8 管理台处理')
    await loadReports()
  } catch (err) {
    error.value = apiErrorText(err, '处理举报失败，请稍后再试。')
  } finally {
    working.value = false
  }
}

function setReportStatus(status: string) {
  reportStatus.value = status
  void loadReports()
}

function displayTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN')
}

function compact(value: string, limit = 120) {
  if (!value) {
    return '无'
  }
  return value.length > limit ? `${value.slice(0, limit)}...` : value
}

onMounted(() => {
  void loadReports()
  void loadAiAudit()
})
</script>

<template>
  <div class="admin-layout">
    <section class="admin-main">
      <div class="surface-header">
        <div>
          <h1>管理台</h1>
          <p>治理队列、AI session、工具调用和摘要缓存状态。</p>
        </div>
        <div class="status-pill">
          <ShieldCheck :size="16" aria-hidden="true" />
          <span>{{ activeTab === 'reports' ? `${pendingCount} 待处理` : `${aiSessions.length} AI session` }}</span>
        </div>
      </div>

      <div class="admin-tabs" role="tablist" aria-label="管理台视图">
        <button type="button" :class="{ active: activeTab === 'reports' }" @click="activeTab = 'reports'">
          <ShieldCheck :size="17" aria-hidden="true" />治理
        </button>
        <button type="button" :class="{ active: activeTab === 'ai' }" @click="activeTab = 'ai'">
          <Bot :size="17" aria-hidden="true" />AI 审计
        </button>
      </div>

      <p v-if="error" class="form-error">{{ error }}</p>

      <div v-if="activeTab === 'reports'" class="admin-view">
        <div class="admin-toolbar">
          <div class="segmented-control" aria-label="举报状态筛选">
            <button type="button" :class="{ active: reportStatus === '' }" @click="setReportStatus('')">全部</button>
            <button type="button" :class="{ active: reportStatus === 'PENDING' }" @click="setReportStatus('PENDING')">待处理</button>
            <button type="button" :class="{ active: reportStatus === 'ACCEPTED' }" @click="setReportStatus('ACCEPTED')">已处理</button>
            <button type="button" :class="{ active: reportStatus === 'REJECTED' }" @click="setReportStatus('REJECTED')">已驳回</button>
          </div>
          <div class="admin-actions">
            <button type="button" :disabled="working" @click="moderate('IGNORE')"><CheckCircle2 :size="17" aria-hidden="true" />驳回</button>
            <button type="button" :disabled="working" @click="moderate('HIDE')"><EyeOff :size="17" aria-hidden="true" />隐藏</button>
            <button type="button" :disabled="working" @click="moderate('LOCK')"><Lock :size="17" aria-hidden="true" />锁定</button>
          </div>
        </div>

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
      </div>

      <div v-else class="admin-view">
        <div v-if="aiMetrics" class="metric-row audit-metrics" aria-label="AI 审计指标">
          <div class="metric-cell">
            <Bot :size="18" aria-hidden="true" />
            <strong>{{ aiMetrics.sessionsToday }}</strong>
            <span>今日会话</span>
          </div>
          <div class="metric-cell">
            <Wrench :size="18" aria-hidden="true" />
            <strong>{{ aiMetrics.toolCallsToday }}</strong>
            <span>工具调用</span>
          </div>
          <div class="metric-cell">
            <Database :size="18" aria-hidden="true" />
            <strong>{{ aiMetrics.cachedSummaries }}</strong>
            <span>摘要缓存</span>
          </div>
        </div>

        <div class="admin-toolbar">
          <button class="tool-link" type="button" @click="loadAiAudit">
            <RefreshCw :size="17" aria-hidden="true" />
            <span>刷新审计</span>
          </button>
          <span class="muted-line">fallback {{ aiMetrics?.fallbackToday || 0 }}，失败工具 {{ aiMetrics?.failedToolCallsToday || 0 }}</span>
        </div>

        <div class="report-table ai-session-table" role="table" aria-label="AI session 列表">
          <div class="report-row report-row--head ai-session-row" role="row">
            <span>Session</span>
            <span>用户</span>
            <span>场景</span>
            <span>状态</span>
            <span>工具</span>
            <span>时间</span>
          </div>
          <button
            v-for="session in aiSessions"
            :key="session.sessionId"
            class="report-row report-row--selectable ai-session-row"
            :class="{ active: selectedAiSession?.session.sessionId === session.sessionId }"
            role="row"
            type="button"
            @click="selectAiSession(session)"
          >
            <span>#{{ session.sessionId }}</span>
            <span>{{ session.nickname }}</span>
            <span>{{ session.scene }}</span>
            <span>{{ session.status }}</span>
            <span>{{ session.toolCallCount }}</span>
            <span>{{ displayTime(session.updatedAt) }}</span>
          </button>
        </div>

        <section v-if="selectedAiSession" class="audit-detail-panel" aria-label="AI session 详情">
          <div class="panel-title-row">
            <div>
              <h2>Session #{{ selectedAiSession.session.sessionId }}</h2>
              <p class="muted-line">{{ selectedAiSession.session.traceId || '无 traceId' }}</p>
            </div>
            <span class="status-pill">{{ selectedAiSession.session.status }}</span>
          </div>
          <div class="audit-detail-grid">
            <article>
              <div class="dock-head">
                <FileSearch :size="17" aria-hidden="true" />
                <strong>消息</strong>
              </div>
              <div v-for="message in selectedAiSession.messages" :key="message.id" class="audit-row">
                <strong>{{ message.role }}</strong>
                <span>{{ compact(message.content, 180) }}</span>
              </div>
            </article>
            <article>
              <div class="dock-head">
                <Wrench :size="17" aria-hidden="true" />
                <strong>工具调用</strong>
              </div>
              <div v-for="tool in selectedAiSession.toolCalls" :key="tool.id" class="audit-row">
                <strong>{{ tool.toolName }} · {{ tool.status }}</strong>
                <span>{{ compact(tool.outputSummary || tool.inputSummary, 160) }}</span>
                <small v-if="tool.errorSummary">{{ compact(tool.errorSummary, 160) }}</small>
              </div>
            </article>
          </div>
        </section>
      </div>
    </section>

    <aside class="side-panel">
      <template v-if="activeTab === 'reports'">
        <h2>举报详情</h2>
        <div v-if="selectedReport" class="audit-block">
          <div class="scope-row">
            <strong>目标</strong>
            <span>{{ selectedReport.target }}</span>
          </div>
          <div class="scope-row">
            <strong>状态</strong>
            <span>{{ selectedReport.status }}</span>
          </div>
          <div class="audit-row">
            <strong>说明</strong>
            <span>{{ selectedReport.detail || '无补充说明' }}</span>
          </div>
        </div>
        <p v-else class="muted-line">暂无举报。</p>
      </template>

      <template v-else>
        <h2>审计规则</h2>
        <div class="scope-row">
          <strong>用户历史</strong>
          <span>只读本人</span>
        </div>
        <div class="scope-row">
          <strong>工具错误</strong>
          <span>摘要展示</span>
        </div>
        <div class="scope-row">
          <strong>摘要缓存</strong>
          <span>24 小时</span>
        </div>
      </template>
    </aside>
  </div>
</template>
