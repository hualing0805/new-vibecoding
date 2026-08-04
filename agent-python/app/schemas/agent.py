from pydantic import BaseModel, Field


class Citation(BaseModel):
    topic_id: int | None = Field(default=None, alias="topicId")
    title: str

    model_config = {"populate_by_name": True}


class ToolCall(BaseModel):
    tool_name: str = Field(alias="toolName")
    status: str

    model_config = {"populate_by_name": True}


class AgentChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=4000)
    category_id: int | None = Field(default=None, alias="categoryId")
    session_id: str | None = Field(default=None, alias="sessionId")

    model_config = {"populate_by_name": True}


class AgentChatResponse(BaseModel):
    answer: str
    citations: list[Citation] = Field(default_factory=list)
    tool_calls: list[ToolCall] = Field(default_factory=list, alias="toolCalls")

    model_config = {"populate_by_name": True}


class PolishRequest(BaseModel):
    title: str = Field(min_length=1, max_length=120)
    content: str = Field(min_length=1, max_length=20000)
    tone: str | None = "clear"


class PolishResponse(BaseModel):
    title: str
    content: str
    notes: list[str] = Field(default_factory=list)


class TagSuggestRequest(BaseModel):
    title: str = Field(min_length=1, max_length=120)
    content: str = Field(min_length=1, max_length=20000)
    category_id: int | None = Field(default=None, alias="categoryId")

    model_config = {"populate_by_name": True}


class TagSuggestResponse(BaseModel):
    tags: list[str]


class TopicSummaryRequest(BaseModel):
    topic_id: int = Field(alias="topicId")

    model_config = {"populate_by_name": True}


class TopicSummaryResponse(BaseModel):
    summary: str
    points: list[str] = Field(default_factory=list)
    follow_up_questions: list[str] = Field(default_factory=list, alias="followUpQuestions")

    model_config = {"populate_by_name": True}


class ModerationCheckRequest(BaseModel):
    target_type: str = Field(alias="targetType")
    content: str = Field(min_length=1, max_length=20000)

    model_config = {"populate_by_name": True}


class ModerationCheckResponse(BaseModel):
    risk_level: str = Field(alias="riskLevel")
    reasons: list[str] = Field(default_factory=list)
    suggested_action: str = Field(alias="suggestedAction")

    model_config = {"populate_by_name": True}

