from typing import Annotated

from fastapi import APIRouter, Depends, Header

from app.agents.forum_agent import ForumAgent
from app.clients.model_client import ModelClient
from app.core.config import Settings, get_settings
from app.core.security import verify_internal_token
from app.schemas.agent import (
    AgentChatRequest,
    AgentChatResponse,
    ModerationCheckRequest,
    ModerationCheckResponse,
    PolishRequest,
    PolishResponse,
    TagSuggestRequest,
    TagSuggestResponse,
    TopicSummaryRequest,
    TopicSummaryResponse,
)
from app.tools.java_tools import JavaForumTools

router = APIRouter(prefix="/agent", dependencies=[Depends(verify_internal_token)])


def get_agent(settings: Settings = Depends(get_settings)) -> ForumAgent:
    return ForumAgent(ModelClient(settings), JavaForumTools(settings))


@router.post("/chat", response_model=AgentChatResponse)
async def chat(
    request: AgentChatRequest,
    agent: ForumAgent = Depends(get_agent),
    x_trace_id: Annotated[str | None, Header(alias="X-Trace-Id")] = None,
) -> AgentChatResponse:
    return await agent.chat(request, trace_id=x_trace_id)


@router.post("/polish", response_model=PolishResponse)
async def polish(request: PolishRequest, agent: ForumAgent = Depends(get_agent)) -> PolishResponse:
    return await agent.polish(request)


@router.post("/suggest-tags", response_model=TagSuggestResponse)
async def suggest_tags(request: TagSuggestRequest, agent: ForumAgent = Depends(get_agent)) -> TagSuggestResponse:
    return await agent.suggest_tags(request)


@router.post("/summarize-topic", response_model=TopicSummaryResponse)
async def summarize_topic(
    request: TopicSummaryRequest,
    agent: ForumAgent = Depends(get_agent),
    x_trace_id: Annotated[str | None, Header(alias="X-Trace-Id")] = None,
) -> TopicSummaryResponse:
    return await agent.summarize_topic(request, trace_id=x_trace_id)


@router.post("/moderation-check", response_model=ModerationCheckResponse)
async def moderation_check(
    request: ModerationCheckRequest,
    agent: ForumAgent = Depends(get_agent),
) -> ModerationCheckResponse:
    return await agent.moderation_check(request)
