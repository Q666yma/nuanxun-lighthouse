from flask import Blueprint, request, jsonify, render_template
import uuid
from utils.api_errors import json_object, service_unavailable
from utils.authorization import private_session

from services.psych_care_service import PsychCareService

psych_care_bp = Blueprint("psych_care_bp", __name__)
svc = PsychCareService()


@psych_care_bp.route("/psych-care/chat", methods=["POST"])
def psych_chat():
    """
    POST /api/psych-care/chat
    请求体: { "message": "...", "session_id": "可选" }
    返回:   { "session_id": "...", "reply": "..." }
    """
    data = json_object()
    if not isinstance(data.get("message"), str):
        return jsonify(error="请填写消息内容"), 400
    message = data["message"].strip()
    if not message:
        return jsonify(error="message不能为空"), 400

    session_id = data.get("session_id") or str(uuid.uuid4())
    if not isinstance(session_id, str) or len(session_id) > 128 or len(message) > 8000:
        return jsonify(error="会话标识或消息过长"), 400
    try:
        reply = svc.chat(private_session(session_id), message)
    except Exception:
        return service_unavailable("心理关怀对话服务暂时不可用，请稍后重试")
    return jsonify(session_id=session_id, reply=reply)


@psych_care_bp.route("/psych-care", methods=["GET"])
def psych_page():
    """前端页面（纯静态 HTML/JS），也可独立放到 SpringBoot"""
    return render_template("rescuer/psych_care_chat.html")
