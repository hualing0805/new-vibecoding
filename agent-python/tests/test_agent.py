from fastapi.testclient import TestClient

from app.main import create_app


client = TestClient(create_app())
headers = {"X-Internal-Token": "dev-internal-token"}


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_agent_requires_internal_token() -> None:
    response = client.post("/agent/chat", json={"message": "hello"})
    assert response.status_code == 401


def test_suggest_tags() -> None:
    response = client.post(
        "/agent/suggest-tags",
        headers=headers,
        json={
            "title": "Vue3 和 Agent 页面怎么设计",
            "content": "前端页面需要调用 Python Agent 和 Java 后端。",
        },
    )
    assert response.status_code == 200
    assert "Vue3" in response.json()["tags"]


def test_chat_returns_tool_trace() -> None:
    response = client.post("/agent/chat", headers=headers, json={"message": "Agent 和 Java 怎么配合"})

    assert response.status_code == 200
    payload = response.json()
    assert payload["toolCalls"]
    assert payload["toolCalls"][0]["toolName"] == "search_topics"
    assert "elapsedMs" in payload["toolCalls"][0]


def test_summarize_topic_returns_tool_trace() -> None:
    response = client.post("/agent/summarize-topic", headers=headers, json={"topicId": 101})

    assert response.status_code == 200
    payload = response.json()
    assert payload["toolCalls"]
    assert payload["toolCalls"][0]["toolName"] == "get_topic_context"
    assert "elapsedMs" in payload["toolCalls"][0]
