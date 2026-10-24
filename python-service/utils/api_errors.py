"""Consistent, non-sensitive API failures for the browser clients."""
from flask import jsonify, request
from werkzeug.exceptions import BadRequest


class ExternalServiceError(RuntimeError):
    pass


def json_object():
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        raise BadRequest("请求内容必须是 JSON 对象")
    return data


def service_unavailable(message="AI 服务暂时不可用，请稍后重试"):
    return jsonify(success=False, status="error", code="AI_UNAVAILABLE",
                   message=message, error=message), 503
