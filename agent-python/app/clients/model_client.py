from __future__ import annotations

import httpx

from app.core.config import Settings


class ModelClient:
    def __init__(self, settings: Settings, http_client: httpx.AsyncClient) -> None:
        self._settings = settings
        self._http_client = http_client

    async def complete(self, system: str, user: str) -> str | None:
        if not self._settings.model_base_url or not self._settings.model_api_key:
            return None

        payload = {
            "model": self._settings.model_name,
            "messages": [
                {"role": "system", "content": system},
                {"role": "user", "content": user},
            ],
            "temperature": 0.3,
        }
        headers = {"Authorization": f"Bearer {self._settings.model_api_key}"}
        url = self._settings.model_base_url.rstrip("/") + "/chat/completions"

        response = await self._http_client.post(
            url,
            headers=headers,
            json=payload,
            timeout=self._settings.model_timeout_seconds,
        )
        response.raise_for_status()
        data = response.json()

        choices = data.get("choices") or []
        if not choices:
            return None
        message = choices[0].get("message") or {}
        content = message.get("content")
        return content if isinstance(content, str) else None

