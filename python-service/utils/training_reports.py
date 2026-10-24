"""Store completed training scores under their verified session owner's UUID."""
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import tempfile
import uuid

from werkzeug.exceptions import NotFound
from config import Config

REPORT_FIELDS = ('emotion', 'info', 'safe', 'rounds', 'strengths', 'improvements',
                 'dimension_advice', 'scoreLog', 'advice')


def _directory(owner):
    return Path(Config.TEMP_FILE_DIR, 'training', str(uuid.UUID(owner)))


def save_training_report(owner, scene, report):
    if scene not in ('fear', 'refuse', 'night') or report.get('rounds') != 5 or len(report.get('scoreLog', [])) != 5:
        raise ValueError('Only completed five-round training can be saved')
    # Explicitly omit the actor/volunteer conversation and all request data.
    scores = {key: report[key] for key in REPORT_FIELDS if key in report}
    scores['scoreLog'] = [{key: row[key] for key in ('emotion', 'info', 'safe', 'advice')}
                          for row in report['scoreLog']]
    identifier = str(uuid.uuid4())
    record = {'id': identifier, 'scene': scene,
              'createdAt': datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z'), 'report': scores}
    directory = _directory(owner)
    directory.mkdir(parents=True, exist_ok=True, mode=0o700)
    fd, temporary = tempfile.mkstemp(prefix='pending-', suffix='.tmp', dir=directory)
    try:
        with os.fdopen(fd, 'w', encoding='utf-8') as stream:
            json.dump(record, stream, ensure_ascii=False, allow_nan=False)
        os.replace(temporary, directory / (identifier + '.json'))
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)
    return identifier


def read_training_report(owner, identifier):
    try:
        if str(uuid.UUID(identifier)) != identifier:
            raise ValueError()
        record = json.loads((_directory(owner) / (identifier + '.json')).read_text(encoding='utf-8'))
        if not isinstance(record, dict) or record.get('id') != identifier or not isinstance(record.get('report'), dict):
            raise ValueError()
        return record
    except (ValueError, FileNotFoundError):
        raise NotFound('训练报告不存在或不属于当前账号') from None


def list_training_reports(owner):
    directory = _directory(owner)
    if not directory.exists():
        return []
    records = []
    for path in directory.glob('*.json'):
        record = read_training_report(owner, path.stem)
        records.append({'id': record['id'], 'scene': record['scene'], 'createdAt': record['createdAt'],
                        'rounds': record['report']['rounds']})
    return sorted(records, key=lambda record: record['createdAt'], reverse=True)[:50]
