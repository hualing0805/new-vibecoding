from __future__ import annotations

import httpx

from app.core.config import Settings


class JavaForumTools:
    def __init__(self, settings: Settings, http_client: httpx.AsyncClient) -> None:
        self._settings = settings
        self._http_client = http_client

    async def search_topics(self, keyword: str, trace_id: str | None = None) -> list[dict]:
        headers = self._headers(trace_id)
        params = {"keyword": keyword, "limit": 5}
        url = self._settings.java_api_base_url.rstrip("/") + "/api/internal/agent/topics/search"

        response = await self._http_client.get(url, params=params, headers=headers, timeout=10)
        response.raise_for_status()
        payload = response.json()

        data = payload.get("data") or {}
        return list(data.get("topics") or [])

    async def get_topic_context(self, topic_id: int, trace_id: str | None = None) -> dict:
        headers = self._headers(trace_id)
        url = self._settings.java_api_base_url.rstrip("/") + f"/api/internal/agent/topics/{topic_id}/context"

        response = await self._http_client.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        payload = response.json()

        return dict(payload.get("data") or {})

    def _headers(self, trace_id: str | None) -> dict[str, str]:
        return {
            "X-Trace-Id": trace_id or "agent-local",
            "X-Internal-Token": self._settings.agent_internal_token,
        }
