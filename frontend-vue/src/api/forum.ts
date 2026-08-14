import { apiClient } from './client'
import type {
  AgentChatResponse,
  ApiResponse,
  AuthResponse,
  AuthUser,
  Category,
  CreatedResource,
  InteractionStatus,
  ModerationCheckResponse,
  NotificationItem,
  NotificationUnreadCount,
  PageResponse,
  PolishResponse,
  Reply,
  ReportItem,
  TagSuggestResponse,
  TopicDetail,
  TopicSummary,
  TopicSummaryResponse
} from './types'
import { categories as mockCategories, replies as mockReplies, reports as mockReports, topics as mockTopics } from '../app/mockData'

export const liveApiEnabled = Boolean(import.meta.env.VITE_API_BASE_URL)
export const apiModeLabel = liveApiEnabled ? 'API 优先' : 'Mock 模式'

async function requestLive<T>(request: () => Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await request()
  return response.data.data
}

async function requestRequired<T>(request: () => Promise<{ data: ApiResponse<T> }>, fallback: T): Promise<T> {
  if (!liveApiEnabled) {
    return fallback
  }
  return requestLive(request)
}

async function requestOr<T>(request: () => Promise<{ data: ApiResponse<T> }>, fallback: T): Promise<T> {
  if (!liveApiEnabled) {
    return fallback
  }
  try {
    const response = await request()
    return response.data.data
  } catch {
    return fallback
  }
}

export function loginUser(payload: { username: string; password: string }): Promise<AuthResponse> {
  if (!liveApiEnabled) {
    return Promise.resolve(mockAuthResponse(payload.username))
  }
  return requestLive(() => apiClient.post<ApiResponse<AuthResponse>>('/auth/login', payload))
}

export function registerUser(payload: { username: string; email: string; password: string; nickname: string }): Promise<AuthResponse> {
  if (!liveApiEnabled) {
    return Promise.resolve(mockAuthResponse(payload.username, payload.nickname))
  }
  return requestLive(() => apiClient.post<ApiResponse<AuthResponse>>('/auth/register', payload))
}

export function fetchCurrentUser(): Promise<AuthUser> {
  return requestLive(() => apiClient.get<ApiResponse<AuthUser>>('/users/me'))
}

export function fetchCategories(): Promise<Category[]> {
  return requestOr(() => apiClient.get<ApiResponse<Category[]>>('/categories'), mockCategories)
}

export function fetchTopics(params: { categoryId?: number | null; sort?: string; keyword?: string } = {}): Promise<PageResponse<TopicSummary>> {
  const fallback = {
    items: filterMockTopics(params),
    page: 1,
    pageSize: 20,
    total: filterMockTopics(params).length
  }
  return requestOr(() => apiClient.get<ApiResponse<PageResponse<TopicSummary>>>('/topics', { params }), fallback)
}

export function fetchFavoriteTopics(params: { page?: number; pageSize?: number } = {}): Promise<PageResponse<TopicSummary>> {
  const favoriteItems = mockTopics.filter((topic) => topic.favoriteCount > 0)
  return requestRequired(() => apiClient.get<ApiResponse<PageResponse<TopicSummary>>>('/users/me/favorites', { params }), {
    items: favoriteItems,
    page: params.page || 1,
    pageSize: params.pageSize || 20,
    total: favoriteItems.length
  })
}

export function fetchTopic(id: number): Promise<TopicDetail> {
  const topic = mockTopics.find((item) => item.id === id) || mockTopics[0]
  const fallback: TopicDetail = {
    ...topic,
    content: '我们准备把 Java 后端作为权限、事务和审计中心，Python Agent 只处理模型调用和工具编排。\n\n当前争议点在于 Agent 做站内问答时，应该直接查 MySQL，还是通过 Java 提供的内部工具接口。',
    aiSummary: '这条讨论集中在 Java 与 Python Agent 的职责边界。'
  }
  return requestOr(() => apiClient.get<ApiResponse<TopicDetail>>(`/topics/${id}`), fallback)
}

export function createTopic(payload: { categoryId: number; title: string; content: string; tagNames: string[] }): Promise<CreatedResource> {
  return requestRequired(() => apiClient.post<ApiResponse<CreatedResource>>('/topics', payload), { id: Date.now() })
}

export function fetchReplies(topicId: number): Promise<Reply[]> {
  return requestOr(() => apiClient.get<ApiResponse<Reply[]>>(`/topics/${topicId}/replies`), mockReplies.filter((reply) => reply.topicId === topicId))
}

export function createReply(topicId: number, content: string): Promise<CreatedResource> {
  return requestRequired(() => apiClient.post<ApiResponse<CreatedResource>>(`/topics/${topicId}/replies`, { content }), { id: Date.now() })
}

export function fetchTopicInteractionStatus(topicId: number): Promise<InteractionStatus> {
  const topic = mockTopics.find((item) => item.id === topicId)
  return requestRequired(() => apiClient.get<ApiResponse<InteractionStatus>>(`/topics/${topicId}/interaction-status`), {
    targetType: 'TOPIC',
    targetId: topicId,
    liked: false,
    favorited: false,
    likeCount: topic?.likeCount || 0,
    favoriteCount: topic?.favoriteCount || 0
  })
}

export function likeTopic(topicId: number): Promise<InteractionStatus> {
  return requestRequired(() => apiClient.post<ApiResponse<InteractionStatus>>(`/topics/${topicId}/like`), {
    targetType: 'TOPIC',
    targetId: topicId,
    liked: true,
    favorited: false,
    likeCount: 1,
    favoriteCount: 0
  })
}

export function unlikeTopic(topicId: number): Promise<InteractionStatus> {
  return requestRequired(() => apiClient.delete<ApiResponse<InteractionStatus>>(`/topics/${topicId}/like`), {
    targetType: 'TOPIC',
    targetId: topicId,
    liked: false,
    favorited: false,
    likeCount: 0,
    favoriteCount: 0
  })
}

export function favoriteTopic(topicId: number): Promise<InteractionStatus> {
  return requestRequired(() => apiClient.post<ApiResponse<InteractionStatus>>(`/topics/${topicId}/favorite`), {
    targetType: 'TOPIC',
    targetId: topicId,
    liked: false,
    favorited: true,
    likeCount: 0,
    favoriteCount: 1
  })
}

export function unfavoriteTopic(topicId: number): Promise<InteractionStatus> {
  return requestRequired(() => apiClient.delete<ApiResponse<InteractionStatus>>(`/topics/${topicId}/favorite`), {
    targetType: 'TOPIC',
    targetId: topicId,
    liked: false,
    favorited: false,
    likeCount: 0,
    favoriteCount: 0
  })
}

export function fetchNotifications(): Promise<NotificationItem[]> {
  const fallback = [
    {
      id: 901,
      type: 'REPLY_CREATED',
      title: '收到新回复',
      body: '林同学 回复了你的帖子。',
      read: false,
      createdAt: new Date().toISOString()
    }
  ]
  return requestRequired(() => apiClient.get<ApiResponse<NotificationItem[]>>('/notifications'), fallback)
}

export function fetchNotificationUnreadCount(): Promise<NotificationUnreadCount> {
  return requestRequired(() => apiClient.get<ApiResponse<NotificationUnreadCount>>('/notifications/unread-count'), {
    unreadCount: 0
  })
}

export function markNotificationRead(id: number): Promise<NotificationItem> {
  return requestRequired(() => apiClient.post<ApiResponse<NotificationItem>>(`/notifications/${id}/read`), {
    id,
    type: 'SYSTEM',
    title: '已读',
    body: '',
    read: true,
    createdAt: new Date().toISOString()
  })
}

export function fetchReports(): Promise<ReportItem[]> {
  return requestRequired(() => apiClient.get<ApiResponse<ReportItem[]>>('/admin/reports'), mockReports as ReportItem[])
}

export function handleReport(id: number, action: string, note = '') {
  return requestRequired(() => apiClient.post<ApiResponse<unknown>>(`/admin/reports/${id}/handle`, { action, note }), {})
}

export function askAgent(message: string): Promise<AgentChatResponse> {
  return requestRequired(() => apiClient.post<ApiResponse<AgentChatResponse>>('/agent/chat', { message }), {
    answer: '后端暂未连接，当前显示前端兜底回答：Java 保留业务状态和审计，Python Agent 提供建议。',
    citations: mockTopics.slice(0, 2).map((topic) => ({ topicId: topic.id, title: topic.title })),
    toolCalls: [{ toolName: 'frontend-fallback', status: 'OK', elapsedMs: 0, summary: 'mock response' }],
    sessionId: null
  })
}

export function polishDraft(payload: { title: string; content: string; tone: string }): Promise<PolishResponse> {
  return requestRequired(() => apiClient.post<ApiResponse<PolishResponse>>('/agent/polish', payload), {
    title: 'Agent 与 Java 后端如何配合',
    content: '我想确认论坛项目中的服务边界：哪些业务应该留在 Java 后端，哪些任务适合交给 Python Agent。',
    notes: ['当前为前端兜底润色']
  })
}

export function suggestTags(payload: { title: string; content: string; categoryId: number }): Promise<TagSuggestResponse> {
  return requestRequired(() => apiClient.post<ApiResponse<TagSuggestResponse>>('/agent/suggest-tags', payload), {
    tags: ['Java', 'Agent', '架构']
  })
}

export function summarizeTopic(topicId: number): Promise<TopicSummaryResponse> {
  const topic = mockTopics.find((item) => item.id === topicId)
  return requestRequired(() => apiClient.post<ApiResponse<TopicSummaryResponse>>('/agent/summarize-topic', { topicId }), {
    summary: '当前为前端兜底摘要。',
    points: ['Java 负责业务边界', 'Agent 负责建议生成'],
    followUpQuestions: ['是否需要生成引用？'],
    citations: topic ? [{ topicId: topic.id, title: topic.title }] : [],
    toolCalls: [{ toolName: 'frontend-fallback', status: 'OK', elapsedMs: 0, summary: 'mock summary' }],
    sessionId: null
  })
}

export function moderationCheck(content: string): Promise<ModerationCheckResponse> {
  return requestRequired(() => apiClient.post<ApiResponse<ModerationCheckResponse>>('/agent/moderation-check', { targetType: 'TOPIC', content }), {
    riskLevel: 'LOW',
    reasons: [],
    suggestedAction: 'ALLOW'
  })
}

export function apiErrorText(error: unknown, fallback = '请求失败，请稍后再试。') {
  if (typeof error === 'object' && error && 'response' in error) {
    const response = (error as { response?: { status?: number; data?: { message?: string } } }).response
    if (response?.status === 401) {
      return '登录已失效，请重新登录。'
    }
    if (response?.status === 403) {
      return '当前账号没有权限执行该操作。'
    }
    if (response?.data?.message) {
      return response.data.message
    }
  }
  return fallback
}

function filterMockTopics(params: { categoryId?: number | null; sort?: string; keyword?: string }) {
  let result = mockTopics.filter((topic) => !params.categoryId || topic.category.id === params.categoryId)
  if (params.keyword) {
    result = result.filter((topic) => `${topic.title} ${topic.excerpt} ${topic.tags.join(' ')}`.includes(params.keyword || ''))
  }
  if (params.sort === 'hot') {
    result = [...result].sort((a, b) => b.likeCount + b.replyCount - (a.likeCount + a.replyCount))
  }
  if (params.sort === 'unanswered') {
    result = result.filter((topic) => topic.replyCount === 0)
  }
  return result
}

function mockAuthResponse(username: string, nickname = username || '演示用户'): AuthResponse {
  return {
    token: `demo.${Date.now()}`,
    user: {
      id: 1,
      username: username || 'demo',
      nickname: nickname || '演示用户',
      avatarUrl: '',
      roles: ['USER', 'ADMIN']
    }
  }
}
