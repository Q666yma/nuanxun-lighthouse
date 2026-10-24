"""Lazy SDK construction keeps the web service available without AI credentials."""
from utils.api_errors import ExternalServiceError


class LazyZhipuClient:
    def __init__(self, api_key):
        self.api_key = api_key
        self._client = None

    def _get_client(self):
        if not self.api_key:
            raise ExternalServiceError("AI 服务尚未配置")
        if self._client is None:
            from zhipuai import ZhipuAI
            self._client = ZhipuAI(api_key=self.api_key, timeout=30, max_retries=0)
        return self._client

    @property
    def chat(self):
        return self._get_client().chat

    def chat_completion(self, **kwargs):
        # The archived recommendation service expects plain text.
        response = self._get_client().chat.completions.create(**kwargs)
        return response.choices[0].message.content or ""


def create_zhipu_client(api_key):
    return LazyZhipuClient(api_key)
