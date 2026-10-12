from contextlib import asynccontextmanager

import httpx
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.agents.forum_agent import ForumAgent
from app.api.agent import router as agent_router
from app.api.health import router as health_router
from app.clients.model_client import ModelClient
from app.core.config import get_settings
from app.tools.java_tools import JavaForumTools


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    async with httpx.AsyncClient() as http_client:
        app.state.forum_agent = ForumAgent(
            ModelClient(settings, http_client),
            JavaForumTools(settings, http_client),
        )
        yield


def create_app() -> FastAPI:
    app = FastAPI(title="ThreadMind Agent", version="0.1.0", lifespan=lifespan)

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["http://localhost:8080"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(health_router)
    app.include_router(agent_router)
    return app


app = create_app()

