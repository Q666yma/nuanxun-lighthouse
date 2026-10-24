import os
import tempfile

import requests
from flask import Blueprint, jsonify, request
from pydub import AudioSegment
from pydub.exceptions import CouldntDecodeError
from utils.authorization import JAVA_BASE, require_roles, require_task, session_headers

voice_proxy_bp = Blueprint('voice_proxy', __name__)


def convert_to_wav(file_storage):
    # Keep paths distinct: replacing a .wav suffix made cleanup delete output.
    suffix = os.path.splitext(file_storage.filename or '')[1].lower()
    input_fd, input_path = tempfile.mkstemp(suffix=suffix)
    output_fd, output_path = tempfile.mkstemp(suffix='.wav')
    os.close(input_fd)
    os.close(output_fd)
    try:
        file_storage.save(input_path)
        with open(input_path, 'rb') as source:
            header = source.read(12)
            source.seek(0)
            audio_format = 'wav' if header[:4] == b'RIFF' and header[8:12] == b'WAVE' else None
            audio = AudioSegment.from_file(source, format=audio_format)
        if not len(audio):
            raise ValueError('音频内容为空')
        with open(output_path, 'wb') as output:
            audio.set_frame_rate(16000).set_channels(1).set_sample_width(2).export(output, format='wav')
        return output_path
    except Exception:
        os.unlink(output_path)
        raise
    finally:
        os.unlink(input_path)


def _proxy(operation, fields):
    require_task(fields.get('taskId'))
    audio_file = request.files.get('audioFile')
    if not audio_file:
        return jsonify(error='缺少音频文件'), 400
    if not fields.get('groupId'):
        return jsonify(error='缺少声纹分组'), 400
    wav_path = None
    try:
        wav_path = convert_to_wav(audio_file)
        with open(wav_path, 'rb') as audio:
            response = requests.post(
                f'{JAVA_BASE}/{operation}', data=fields,
                files={'audioFile': ('audio.wav', audio, 'audio/wav')},
                headers=session_headers(), timeout=(5, 60), allow_redirects=False)
        return response.content, response.status_code, {
            'Content-Type': response.headers.get('Content-Type', 'application/json')}
    except (CouldntDecodeError, ValueError):
        return jsonify(error='无法读取音频，请上传有效的 WAV、MP3 或录音文件'), 400
    except requests.RequestException:
        return jsonify(error='声纹服务暂时不可用，请稍后重试'), 503
    except Exception:
        return jsonify(error='音频处理失败，请稍后重试'), 500
    finally:
        if wav_path and os.path.exists(wav_path):
            os.unlink(wav_path)


@voice_proxy_bp.route('/api/proxy/createFeature', methods=['POST'])
def proxy_create_feature():
    require_roles('family', 'rescuer', 'admin', approved=True)
    fields = {key: request.form.get(key, '') for key in ('taskId', 'groupId', 'featureId', 'featureInfo')}
    if not fields['featureId']:
        return jsonify(error='缺少声纹标识'), 400
    return _proxy('createFeature', fields)


@voice_proxy_bp.route('/api/proxy/searchFeature', methods=['POST'])
def proxy_search_feature():
    require_roles('rescuer', 'admin', approved=True)
    try:
        top_k = int(request.form.get('topK', '1'))
        if not 1 <= top_k <= 10:
            raise ValueError()
    except ValueError:
        return jsonify(error='返回数量必须在 1 到 10 之间'), 400
    return _proxy('searchFeature', {'taskId': request.form.get('taskId', ''),
                                  'groupId': request.form.get('groupId', ''), 'topK': str(top_k)})
