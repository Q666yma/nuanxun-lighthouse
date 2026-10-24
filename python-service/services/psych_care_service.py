import uuid
import logging
from typing import Dict, List

from utils.ai_client import create_zhipu_client
from config import Config

logger = logging.getLogger(__name__)


class PsychCareService:
    """
    心理关怀 GPT —— 维护多会话聊天记录，调用智谱轻言/GLM 模型
    """

    def __init__(self):
        self.client = create_zhipu_client(api_key=Config.ZHIPU_API_KEY)
        self.model = getattr(Config, "PSYCH_MODEL_NAME", Config.MODEL_NAME)
        self.sessions: Dict[str, List[dict]] = {}   # session_id -> 对话历史

    def _init_session(self, session_id: str) -> None:
        """首次对话时创建带系统提示的上下文"""
        self.sessions[session_id] = [{
            "role": "system",
            "content": (
                "你是一名经过专业训练、温暖且值得信赖的心理关怀助手。"
                "请用耐心、共情的语气与用户聊天，帮助他们舒缓情绪、获得积极力量。"
                "在任何情况下都禁止给出医疗诊断或处方，必要时建议寻求线下专业心理咨询。"
            )
        }]

    def chat(self, session_id: str, user_message: str) -> str:
        """核心对话方法：追加消息 → 调 API → 追加回复 → 截断上下文"""
        if session_id not in self.sessions:
            self._init_session(session_id)

        history = self.sessions[session_id]
        pending = history + [{"role": "user", "content": user_message}]

        try:
            resp = self.client.chat.completions.create(
                model=self.model,
                messages=pending,
                temperature=0.7,
                max_tokens=1024
            )
            assistant_reply = resp.choices[0].message.content.strip()
        except Exception as e:
            logger.error(f"PsychCare GPT 调用失败: {e}", exc_info=True)
            raise

        if not assistant_reply:
            raise ValueError("对话服务未返回内容")
        self.sessions[session_id] = [pending[0]] + pending[1:][-17:] + [
            {"role": "assistant", "content": assistant_reply}]
        while len(self.sessions) > 200:
            self.sessions.pop(next(iter(self.sessions)))

        return assistant_reply
