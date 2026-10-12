from __future__ import annotations

import logging
from time import perf_counter

from app.clients.model_client import ModelClient
from app.schemas.agent import (
    AgentChatRequest,
    AgentChatResponse,
    Citation,
    ModerationCheckRequest,
    ModerationCheckResponse,
    PolishRequest,
    PolishResponse,
    TagSuggestRequest,
    TagSuggestResponse,
    ToolCall,
    TopicSummaryRequest,
    TopicSummaryResponse,
)
from app.tools.java_tools import JavaForumTools

logger = logging.getLogger(__name__)


class ForumAgent:
    def __init__(self, model_client: ModelClient, java_tools: JavaForumTools) -> None:
        self._model_client = model_client
        self._java_tools = java_tools

    async def chat(self, request: AgentChatRequest, trace_id: str | None = None) -> AgentChatResponse:
        tool_calls: list[ToolCall] = []
        citations: list[Citation] = []
        topics: list[dict] = []

        search_started_at = perf_counter()
        try:
            topics = await self._java_tools.search_topics(request.message, trace_id=trace_id)
            tool_calls.append(
                ToolCall(
                    toolName="search_topics",
                    status="OK",
                    elapsedMs=self._elapsed_ms(search_started_at),
                    summary=f"found {len(topics)} topics",
                )
            )
        except Exception as ex:
            tool_calls.append(
                ToolCall(
                    toolName="search_topics",
                    status="FAILED",
                    elapsedMs=self._elapsed_ms(search_started_at),
                    error=self._error_summary(ex),
                )
            )

        for topic in topics[:3]:
            citations.append(Citation(topicId=topic.get("id"), title=topic.get("title", "未命名帖子")))

        system = (
            "你是 ThreadMind 论坛的站内助手。回答只基于用户问题、已授权上下文和明确规则。"
            "不要声称已经发布、删除或封禁任何内容。"
        )
        context = "\n".join(f"- {topic.get('title')}: {topic.get('excerpt')}" for topic in topics[:5])
        user = f"用户问题：{request.message}\n\n可见帖子：\n{context or '暂无检索结果'}"
        model_answer = await self._safe_complete(system, user)
        answer = model_answer or self._mock_chat_answer(request, bool(topics))

        return AgentChatResponse(answer=answer, citations=citations, toolCalls=tool_calls)

    async def polish(self, request: PolishRequest) -> PolishResponse:
        system = "你是论坛发帖编辑助手。保留原意，改善标题清晰度和正文表达，不替用户新增事实。"
        user = f"标题：{request.title}\n语气：{request.tone or 'clear'}\n正文：\n{request.content}"
        model_answer = await self._safe_complete(system, user)
        if model_answer:
            return PolishResponse(title=request.title.strip(), content=model_answer.strip(), notes=["已按模型结果生成正文建议"])

        title = request.title.strip()
        if len(title) < 12 and not title.endswith("？"):
            title = f"{title}的设计问题"
        content = request.content.strip()
        polished = content.replace("怎么弄", "如何设计").replace("有没有办法", "是否有合适方案")
        return PolishResponse(
            title=title,
            content=polished,
            notes=["已做轻量润色", "未新增事实", "发布前建议再人工确认一次"],
        )

    async def suggest_tags(self, request: TagSuggestRequest) -> TagSuggestResponse:
        text = f"{request.title} {request.content}".lower()
        candidates = [
            ("Java", ["java", "spring", "后端"]),
            ("Vue3", ["vue", "前端", "页面"]),
            ("Python", ["python", "fastapi"]),
            ("MySQL", ["mysql", "数据库", "sql"]),
            ("Agent", ["agent", "ai", "模型", "工具调用"]),
            ("项目展示", ["展示", "作品", "demo"]),
        ]
        tags = [tag for tag, words in candidates if any(word in text for word in words)]
        return TagSuggestResponse(tags=(tags or ["Agent", "Java", "Vue3"])[:5])

    async def summarize_topic(self, request: TopicSummaryRequest, trace_id: str | None = None) -> TopicSummaryResponse:
        context_started_at = perf_counter()
        try:
            context = await self._java_tools.get_topic_context(request.topic_id, trace_id=trace_id)
            topic = context.get("topic") or {}
            replies = context.get("replies") or []
            title = topic.get("title") or f"帖子 {request.topic_id}"
            return TopicSummaryResponse(
                summary=f"《{title}》主要讨论服务边界、实现顺序和可替换的数据层。当前共有 {len(replies)} 条可见回复参与上下文。",
                points=[
                    "Java 后端保留业务状态和权限",
                    "Python Agent 通过内部工具读取授权上下文",
                    "前端以 API 优先、mock 兜底的方式推进页面联调",
                ],
                followUpQuestions=["是否要把该摘要写入缓存？", "是否需要把高赞回复作为引用来源？"],
                citations=[Citation(topicId=topic.get("id") or request.topic_id, title=title)],
                toolCalls=[
                    ToolCall(
                        toolName="get_topic_context",
                        status="OK",
                        elapsedMs=self._elapsed_ms(context_started_at),
                        summary=f"loaded topic with {len(replies)} replies",
                    )
                ],
            )
        except Exception as ex:
            failed_tool_call = ToolCall(
                toolName="get_topic_context",
                status="FAILED",
                elapsedMs=self._elapsed_ms(context_started_at),
                error=self._error_summary(ex),
            )
            return TopicSummaryResponse(
                summary=f"帖子 {request.topic_id} 的摘要需要结合正文和高赞回复生成。当前骨架返回演示摘要。",
                points=["明确 Java 与 Agent 的职责边界", "AI 输出只作为建议", "后续可加入摘要缓存"],
                followUpQuestions=["是否要把摘要写入缓存表？", "是否需要展示引用的回复？"],
                citations=[],
                toolCalls=[failed_tool_call],
            )

    async def moderation_check(self, request: ModerationCheckRequest) -> ModerationCheckResponse:
        risky_words = ["攻击", "辱骂", "人肉", "泄露"]
        hits = [word for word in risky_words if word in request.content]
        if hits:
            return ModerationCheckResponse(
                riskLevel="MEDIUM",
                reasons=[f"内容包含可能需要复核的词：{word}" for word in hits],
                suggestedAction="REVIEW",
            )
        return ModerationCheckResponse(riskLevel="LOW", reasons=[], suggestedAction="ALLOW")

    async def _safe_complete(self, system: str, user: str) -> str | None:
        try:
            return await self._model_client.complete(system=system, user=user)
        except Exception:
            logger.warning("Model completion failed; using deterministic fallback", exc_info=True)
            return None

    @staticmethod
    def _elapsed_ms(started_at: float) -> int:
        return max(0, int((perf_counter() - started_at) * 1000))

    @staticmethod
    def _error_summary(error: Exception) -> str:
        text = f"{error.__class__.__name__}: {error}"
        return text[:240]

    @staticmethod
    def _mock_chat_answer(request: AgentChatRequest, has_topics: bool) -> str:
        if has_topics:
            return "我查到了相关站内讨论。当前重点是把权限、审计和数据库写入留在 Java 后端，Python Agent 负责生成建议和调用受控工具。"
        return "暂时没有找到足够相关的站内内容。可以先补充板块、关键词或具体帖子链接，我会基于可见内容继续回答。"
