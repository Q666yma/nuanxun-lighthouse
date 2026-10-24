from flask import Blueprint, jsonify, g, request, current_app
import uuid
import json
from werkzeug.exceptions import HTTPException
from services.scene_train_service import SceneTrainService
from utils.api_errors import json_object, service_unavailable
from utils.authorization import private_session, require_roles
from utils.training_reports import save_training_report, list_training_reports, read_training_report

scene_bp = Blueprint('scene_train_bp', __name__)
svc = SceneTrainService()


@scene_bp.route('/scene-train/chat', methods=['POST'])
def chat():
    require_roles('rescuer', 'admin')
    data = json_object()
    sid = data.get('session_id') or str(uuid.uuid4())
    if not isinstance(sid, str) or len(sid) > 128:
        return jsonify(error='会话标识格式错误'), 400
    room_key = private_session(sid)
    try:
        if data.get('cmd') == 'start':
            scene = data.get('scene', 'fear')
            if scene not in ('fear', 'refuse', 'night'):
                return jsonify(error='请选择有效训练场景'), 400
            result = svc.start(room_key, scene)
        elif data.get('cmd') == 'talk':
            content = data.get('content')
            if not isinstance(content, str) or not content.strip() or len(content) > 8000:
                return jsonify(error='请填写有效的对话内容'), 400
            if room_key not in svc.rooms:
                return jsonify(error='训练已结束或会话已过期，请重新选择场景'), 409
            scene = svc.rooms[room_key]['scene']
            result = svc.talk(room_key, content.strip())
            if result.get('done'):
                try:
                    report_id = save_training_report(g.current_user['uuid'], scene, result['report'])
                    result.update(reportSaved=True, reportId=report_id, reportMessage='训练报告已保存，可在历史报告中查看')
                except Exception:
                    current_app.logger.warning('训练报告保存失败，保留当次评分报告')
                    result.update(reportSaved=False, reportId=None,
                                  reportMessage='历史保存暂不可用，本次报告仍可查看和下载；请在离开页面前保存')
        else:
            return jsonify(error='无效操作'), 400
        result['session_id'] = sid
        return jsonify(result)
    except Exception:
        return service_unavailable('情景训练服务暂时不可用，请稍后重试')


@scene_bp.route('/scene-train/reports', methods=['GET'])
def report_list():
    user = require_roles('rescuer', 'admin')
    try:
        response = jsonify(success=True, data=list_training_reports(user['uuid']), limit=50)
        response.headers['Cache-Control'] = 'no-store'
        return response
    except Exception:
        return service_unavailable('训练报告历史暂时无法读取，请稍后重试')


@scene_bp.route('/scene-train/reports/<report_id>', methods=['GET'])
def report_detail(report_id):
    user = require_roles('rescuer', 'admin')
    try:
        record = read_training_report(user['uuid'], report_id)
    except HTTPException:
        raise
    except OSError:
        return service_unavailable('训练报告暂时无法读取，请稍后重试')
    response = jsonify(success=True, data=record)
    if request.args.get('download') == '1':
        response = current_app.response_class(json.dumps(record, ensure_ascii=False), mimetype='application/json')
        response.headers['Content-Disposition'] = 'attachment; filename="training-report-' + record['id'] + '.json"'
    response.headers['Cache-Control'] = 'no-store'
    return response
