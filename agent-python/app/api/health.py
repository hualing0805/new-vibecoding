from datetime import datetime, timezone

from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
async def health() -> dict:
    return {
        "service": "threadmind-agent",
        "status": "UP",
        "time": datetime.now(timezone.utc).isoformat(),
    }

