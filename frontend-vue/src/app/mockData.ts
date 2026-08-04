import type { Category, Reply, TopicSummary } from '../api/types'

export const categories: Category[] = [
  {
    id: 1,
    name: '开发讨论',
    slug: 'dev',
    description: 'Java、Vue、Python、数据库和部署经验。',
    topicCount: 128
  },
  {
    id: 2,
    name: 'AI 助手',
    slug: 'agent',
    description: 'Agent 编排、提示词、工具调用和模型接入。',
    topicCount: 42
  },
  {
    id: 3,
    name: '项目展示',
    slug: 'showcase',
    description: '展示正在做的作品，收集反馈。',
    topicCount: 36
  },
  {
    id: 4,
    name: '反馈建议',
    slug: 'feedback',
    description: '产品体验、规则和社区治理建议。',
    topicCount: 19
  }
]

export const topics: TopicSummary[] = [
  {
    id: 101,
    title: 'Spring Boot 和 Agent 服务边界怎么划分',
    excerpt: '讨论 Java 主业务和 Python Agent 之间的权限、审计、工具调用边界。',
    category: categories[0],
    author: { id: 9, nickname: '北桥', avatarUrl: '' },
    tags: ['Java', 'Agent', '架构'],
    replyCount: 18,
    likeCount: 42,
    favoriteCount: 7,
    viewCount: 980,
    status: 'PUBLISHED',
    createdAt: '2026-08-04T12:00:00'
  },
  {
    id: 102,
    title: 'Vue3 发帖编辑器需要哪些 AI 入口',
    excerpt: '润色、标签推荐、摘要预览和风险提示应该放在哪里，才不打断写作。',
    category: categories[0],
    author: { id: 12, nickname: '林同学', avatarUrl: '' },
    tags: ['Vue3', '交互', 'AI'],
    replyCount: 9,
    likeCount: 31,
    favoriteCount: 5,
    viewCount: 622,
    status: 'PUBLISHED',
    createdAt: '2026-08-04T10:38:00'
  },
  {
    id: 103,
    title: '站内问答先用 MySQL 检索还是向量库',
    excerpt: 'MVP 阶段可以先用关键词检索和摘要缓存，后续再加向量召回。',
    category: categories[1],
    author: { id: 9, nickname: '北桥', avatarUrl: '' },
    tags: ['Python', '检索', 'MySQL'],
    replyCount: 12,
    likeCount: 28,
    favoriteCount: 6,
    viewCount: 714,
    status: 'PUBLISHED',
    createdAt: '2026-08-03T22:16:00'
  },
  {
    id: 104,
    title: 'ThreadMind 第一版页面草图',
    excerpt: '首页、帖子详情、发帖编辑器和管理台的初始页面设计记录。',
    category: categories[2],
    author: { id: 12, nickname: '林同学', avatarUrl: '' },
    tags: ['项目展示', 'Vue3'],
    replyCount: 6,
    likeCount: 24,
    favoriteCount: 4,
    viewCount: 406,
    status: 'PUBLISHED',
    createdAt: '2026-08-03T18:20:00'
  }
]

export const replies: Reply[] = [
  {
    id: 201,
    topicId: 101,
    author: { id: 12, nickname: '林同学', avatarUrl: '' },
    content: '建议先让 Agent 只读 Java 工具接口。等权限模型稳定后，再考虑是否加只读视图。',
    likeCount: 18,
    status: 'PUBLISHED',
    createdAt: '2026-08-04T13:00:00'
  },
  {
    id: 202,
    topicId: 101,
    author: { id: 9, nickname: '北桥', avatarUrl: '' },
    content: 'AI 的失败也要降级成普通论坛可用，不能让发帖流程依赖模型服务。',
    likeCount: 11,
    status: 'PUBLISHED',
    createdAt: '2026-08-04T13:22:00'
  }
]

export const reports = [
  {
    id: 301,
    target: '回复 #202',
    reason: '疑似偏题',
    status: 'PENDING',
    risk: 'LOW',
    createdAt: '12 分钟前'
  },
  {
    id: 302,
    target: '帖子 #103',
    reason: '标题不清晰',
    status: 'PENDING',
    risk: 'LOW',
    createdAt: '45 分钟前'
  }
]

