export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId: string
}

export interface PageResponse<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

export interface Category {
  id: number
  name: string
  slug: string
  description: string
  topicCount: number
}

export interface Author {
  id: number
  nickname: string
  avatarUrl: string
}

export interface TopicSummary {
  id: number
  title: string
  excerpt: string
  category: Category
  author: Author
  tags: string[]
  replyCount: number
  likeCount: number
  favoriteCount: number
  viewCount: number
  status: string
  createdAt: string
}

export interface TopicDetail extends TopicSummary {
  content: string
  aiSummary: string
}

export interface Reply {
  id: number
  topicId: number
  author: Author
  content: string
  likeCount: number
  status: string
  createdAt: string
}

export interface CreatedResource {
  id: number
}

export interface AuthUser {
  id: number
  username: string
  nickname: string
  avatarUrl: string
  roles: string[]
}

export interface AuthResponse {
  token: string
  user: AuthUser
}

export interface AgentChatResponse {
  answer: string
  citations: Array<{ topicId: number; title: string }>
  toolCalls: Array<{ toolName: string; status: string }>
}

export interface PolishResponse {
  title: string
  content: string
  notes: string[]
}

export interface TagSuggestResponse {
  tags: string[]
}

export interface TopicSummaryResponse {
  summary: string
  points: string[]
  followUpQuestions: string[]
}

export interface ModerationCheckResponse {
  riskLevel: string
  reasons: string[]
  suggestedAction: string
}

export interface NotificationItem {
  id: number
  type: string
  title: string
  body: string
  read: boolean
  createdAt: string
}

export interface ReportItem {
  id: number
  targetType: string
  targetId: number
  target: string
  reason: string
  detail: string
  status: string
  risk: string
  createdAt: string
}
