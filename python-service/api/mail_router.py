from flask import Blueprint, jsonify
from utils.api_errors import json_object

mail_bp = Blueprint('mail', __name__)


@mail_bp.route('/api/notify-family', methods=['POST'])
def notify_family():
    json_object()
    message = '旧通知入口已停用，请从具体任务中使用“通知家属”，收件人由任务确定'
    return jsonify(success=False, message=message, msg=message, error=message), 410
