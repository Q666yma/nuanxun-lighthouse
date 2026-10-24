import logging
from flask import Blueprint, request, jsonify, send_file
from services.tag_extractor import TagExtractor
from config import Config
from services.recommendation_service import RecommendationService
from utils.docx_generator import generate_docx_from_markdown
from utils.api_errors import json_object, service_unavailable
from utils.authorization import require_roles, require_task, recommendation_input
from utils.report_ownership import save_report_owner, authorize_report
from werkzeug.exceptions import HTTPException
import os
import uuid
import tempfile

api_bp = Blueprint("api", __name__)
logger = logging.getLogger(__name__)

tag_extractor = TagExtractor()
recommendation_service = RecommendationService()

# ----------------------------------------------------------------------
# 0. ping — 用于连通性测试
# ----------------------------------------------------------------------
@api_bp.route("/ping", methods=["GET"])
def ping():
    return {"msg": "pong"}, 200


# ----------------------------------------------------------------------
# 1. 提取标签接口（保留，未改动）
# ----------------------------------------------------------------------
@api_bp.route("/extract_tags", methods=["POST"])
def extract_tags():
    require_roles('rescuer', 'admin')
    data = json_object()
    if not isinstance(data.get("user_bio"), str) or not data["user_bio"].strip() or len(data["user_bio"]) > 8000:
        return jsonify(error="请填写技能简介"), 400
    library = data.get("tag_library")
    if library is not None and (not isinstance(library, list) or len(library) > 31 or
                               any(not isinstance(tag, str) for tag in library)):
        return jsonify(error="标签库必须是文字列表"), 400
    try:
        if "user_bio" not in data:
            return jsonify({"error":"user_bio 不能为空"}), 400

        result = tag_extractor.extract_details(
            data["user_bio"].strip(),
            [tag for tag in library if tag in Config.TAG_LIBRARY] if library is not None else None
        )
        return jsonify(result)

    except Exception as e:
        logger.exception("extract_tags error")
        return service_unavailable("技能标签提取服务暂时不可用，请稍后重试")


@api_bp.route("/recommend-rescuers", methods=["POST"])
def recommend_rescuers():
    require_roles('family', 'admin')
    data = json_object()
    if not isinstance(data.get("task"), dict) or not isinstance(data.get("rescuers"), list):
        return jsonify(success=False, message="任务或救援人员数据格式错误"), 400
    if any(not isinstance(item, dict) for item in data["rescuers"]):
        return jsonify(success=False, message="救援人员数据格式错误"), 400
    library = data.get("tagLibrary", [])
    if not isinstance(library, list) or any(not isinstance(tag, str) for tag in library):
        return jsonify(success=False, message="标签库必须是文字列表"), 400
    data = recommendation_input(data['task'].get('id'))
    try:
        logger.info("收到/recommend-rescuers请求")
        
        if not data:
            logger.warning("请求数据为空")
            return jsonify({"success": False, "message": "请求数据为空"}), 400
        
        # 验证必要字段
        if "task" not in data or "rescuers" not in data:
            logger.warning(f"缺少必要字段, data.keys(): {list(data.keys())}")
            return jsonify({"success": False, "message": "缺少任务或救援人员数据"}), 400
        
        # 获取标签库
        tag_library = data.get("tagLibrary", [])
        
        logger.info("调用推荐服务: generate_recommendations")
        result = recommendation_service.generate_recommendations(
            data["task"], 
            data["rescuers"],
            tag_library
        )
        
        if not result.get("success"):
            logger.warning(f"推荐服务失败: {result}")
            return jsonify(result), 400
        
        # 生成唯一文件名
        report_id = str(uuid.uuid4())
        temp_dir = getattr(Config, "TEMP_FILE_DIR", tempfile.gettempdir())
        os.makedirs(temp_dir, exist_ok=True)
        markdown_path = os.path.join(temp_dir, f"report_{report_id}.md")
        docx_path = os.path.join(temp_dir, f"report_{report_id}.docx")
        logger.info(f"生成临时文件: {markdown_path}, {docx_path}")
        
        # 将Markdown内容写入临时文件
        md_content = result.get("report_markdown", "")
        if not md_content:
            logger.error("推荐结果未包含 report_markdown 字段")
            return jsonify({"success": False, "message": "推荐结果无报告内容"}), 500
        with open(markdown_path, "w", encoding="utf-8") as f:
            f.write(md_content)
        logger.info("写入Markdown临时文件完成")
        
        # 本地生成Word文档，失败时保留HTML和Markdown供查看与重试。
        logger.info("调用generate_docx_from_markdown生成docx")
        save_report_owner(report_id, data['task']['id'])
        result['report_id'] = report_id
        result['markdown_url'] = f'/api/download-report/{report_id}?format=markdown'
        result.update(_export_word(report_id))
        
        # 保存文件路径供下载使用
        if not hasattr(Config, "TEMP_FILES"):
            Config.TEMP_FILES = {}
        Config.TEMP_FILES[report_id] = docx_path
        logger.debug(f"已保存docx路径: {Config.TEMP_FILES[report_id]}")
        
        # 返回推荐结果
        logger.info("推荐结果处理完成, 正常返回")
        return jsonify(result), 200
    
    except Exception as e:
        logger.error(f"推荐救援人员时发生错误: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": "推荐报告生成失败，请稍后重试"}), 500


def _export_word(report_id):
    markdown_path = os.path.join(Config.TEMP_FILE_DIR, f'report_{report_id}.md')
    docx_path = os.path.join(Config.TEMP_FILE_DIR, f'report_{report_id}.docx')
    try:
        generate_docx_from_markdown(markdown_path, docx_path, template_path=Config.DOCX_TEMPLATE_PATH)
        return {'export_available': True, 'export_message': 'Word文档已生成',
                'docx_url': f'/api/download-report/{report_id}'}
    except Exception:
        logger.warning('Word导出失败，保留已生成的Markdown供重试')
        return {'export_available': False, 'export_message': 'Word导出暂不可用，可先下载Markdown，或重试Word导出，无需重新生成推荐',
                'docx_url': None}


@api_bp.route('/export-report/<report_id>', methods=['POST'])
def export_report(report_id):
    try:
        if str(uuid.UUID(report_id)) != report_id:
            raise ValueError()
    except ValueError:
        return jsonify(success=False, message='报告不存在'), 404
    authorize_report(report_id)
    if not os.path.isfile(os.path.join(Config.TEMP_FILE_DIR, f'report_{report_id}.md')):
        return jsonify(success=False, message='报告不存在'), 404
    result = _export_word(report_id)
    return jsonify(success=result['export_available'], report_id=report_id, **result), (200 if result['export_available'] else 503)

@api_bp.route("/download-report/<report_id>", methods=["GET"])
def download_report(report_id):
    try:
        # Files remain downloadable after a Gunicorn restart. UUID validation
        # prevents path traversal without relying on process-local dictionaries.
        try:
            if str(uuid.UUID(report_id)) != report_id:
                raise ValueError()
        except ValueError:
            return jsonify({"success": False, "message": "报告不存在或已过期"}), 404
        authorize_report(report_id)

        output_format = request.args.get('format', 'docx')
        if output_format not in ('docx', 'markdown'):
            return jsonify(success=False, message='不支持的报告格式'), 400
        suffix = 'md' if output_format == 'markdown' else 'docx'
        docx_path = os.path.abspath(os.path.join(Config.TEMP_FILE_DIR, f"report_{report_id}.{suffix}"))
        if not os.path.exists(docx_path):
            return jsonify({"success": False, "message": "报告文件已被删除"}), 404
        
        # 提供文件下载
        return send_file(
            docx_path,
            mimetype="text/markdown; charset=utf-8" if suffix == 'md' else "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            as_attachment=True,
            download_name=f"救援人员推荐报告_{report_id}.{suffix}"
        )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"下载报告时发生错误: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": "报告下载失败，请稍后重试"}), 500

# 添加一个定期清理临时文件的路由(仅用于开发环境)
@api_bp.route("/cleanup-temp-files", methods=["POST"])
def cleanup_temp_files():
    require_roles('admin')
    if not Config.DEBUG:
        return jsonify({"success": False, "message": "此功能仅在开发环境可用"}), 403
    
    try:
        if hasattr(Config, "TEMP_FILES"):
            for file_path in Config.TEMP_FILES.values():
                if os.path.exists(file_path):
                    os.remove(file_path)
            Config.TEMP_FILES = {}
        
        return jsonify({"success": True, "message": "临时文件已清理"}), 200
    
    except Exception as e:
        logger.error(f"清理临时文件时发生错误: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": "临时文件清理失败"}), 500
    

@api_bp.route("/generate-summary", methods=["POST"])
def generate_elder_summary():
    require_roles('family', 'admin')
    data = json_object()
    if data.get('taskId') is not None:
        require_task(data['taskId'])
    if not data:
        return jsonify(success=False, message="请先填写走失人员信息"), 400
    try:
        logger.info("收到/generate-summary请求")
        
        if not data:
            logger.warning("请求数据为空")
            return jsonify({"success": False, "message": "请求数据为空"}), 400
        
        logger.debug(f"模板数据字段: {list(data.keys())}")
        
        # 调用服务生成摘要
        summary = recommendation_service.generate_elder_summary(data)
        
        logger.info(f"生成摘要完成，长度: {len(summary)}")
        
        return jsonify({
            "success": True,
            **summary
        }), 200
    
    except Exception as e:
        logger.error(f"生成摘要时发生错误: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": "摘要生成失败，请核对填写的信息后重试"}), 500
    
