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

