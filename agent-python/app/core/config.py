from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    agent_internal_token: str = Field(default="dev-internal-token", alias="AGENT_INTERNAL_TOKEN")
    java_api_base_url: str = Field(default="http://localhost:8080", alias="JAVA_API_BASE_URL")
    model_base_url: str = Field(default="", alias="MODEL_BASE_URL")
    model_api_key: str = Field(default="", alias="MODEL_API_KEY")
    model_name: str = Field(default="gpt-4.1-mini", alias="MODEL_NAME")
    model_timeout_seconds: int = Field(default=30, alias="MODEL_TIMEOUT_SECONDS")

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


@lru_cache
def get_settings() -> Settings:
    return Settings()

