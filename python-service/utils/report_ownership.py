"""Persist report ownership beside its generated files so restarts keep it private."""
import json
from pathlib import Path

from flask import g
from werkzeug.exceptions import Forbidden, NotFound
from config import Config
from utils.authorization import require_roles, require_task


def save_report_owner(report_id, task_id):
    path = Path(Config.TEMP_FILE_DIR, 'report_' + report_id + '.owner.json')
    with path.open('x', encoding='utf-8') as stream:
        json.dump({'owner': g.current_user['uuid'], 'taskId': task_id}, stream)


def authorize_report(report_id):
    require_roles('family', 'admin')
    path = Path(Config.TEMP_FILE_DIR, 'report_' + report_id + '.owner.json')
    try:
        owner = json.loads(path.read_text(encoding='utf-8'))
    except (FileNotFoundError, ValueError):
        # Historic reports without an accountable owner are not public downloads.
        raise NotFound('报告不存在或无法确认归属，请从自己的任务重新生成') from None
    if not isinstance(owner, dict) or owner.get('owner') != g.current_user['uuid']:
        raise Forbidden('只能下载本人生成的报告')
    require_task(owner.get('taskId'))
