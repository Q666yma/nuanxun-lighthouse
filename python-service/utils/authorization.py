"""Use the Java session and task checks for every Python business request."""
import requests
import os
import ipaddress
from urllib.parse import urlsplit
from flask import g, request
from werkzeug.exceptions import BadRequest, Conflict, Forbidden, ServiceUnavailable, Unauthorized

def _java_origin(value):
    try:
        parsed = urlsplit(value)
        loopback = parsed.hostname == 'localhost' or ipaddress.ip_address(parsed.hostname).is_loopback
        if (parsed.scheme == 'http' and loopback and parsed.username is None and parsed.password is None
                and not parsed.query and not parsed.fragment and parsed.path in ('', '/')
                and parsed.port is not None and 1 <= parsed.port <= 65535):
            return 'http://' + parsed.netloc
    except (ValueError, TypeError):
        pass
    raise ValueError('JAVA_INTERNAL_ORIGIN must be a loopback HTTP origin with an explicit port')


JAVA_BASE = _java_origin(os.getenv('JAVA_INTERNAL_ORIGIN', 'http://127.0.0.1:8080'))


def session_headers():
    session_id = request.cookies.get('JSESSIONID')
    if not session_id:
        raise Unauthorized('请先登录')
    return {'Cookie': 'JSESSIONID=' + session_id}


def _java_json(path):
    try:
        response = requests.get(JAVA_BASE + path, headers=session_headers(),
                                timeout=(3, 10), allow_redirects=False)
        if response.status_code == 401:
            raise Unauthorized('登录已失效，请重新登录')
        if response.status_code == 403:
            raise Forbidden('没有访问此功能或任务的权限')
        if response.status_code == 404:
            raise Forbidden('任务不存在或无权访问')
        if response.status_code == 409:
            raise Conflict('任务状态已变化，当前操作不再适用，请刷新任务')
        if response.status_code != 200:
            raise ServiceUnavailable('暂时无法核验访问权限，请稍后重试')
        data = response.json()
        if not isinstance(data, dict):
            raise ValueError('Invalid authorization response')
        return data
    except (requests.RequestException, ValueError):
        raise ServiceUnavailable('暂时无法核验访问权限，请稍后重试') from None


def authenticate_request():
    if request.endpoint is None or request.path == '/api/ping' or request.method == 'OPTIONS':
        return
    user = _java_json('/api/users/me')
    if not isinstance(user.get('uuid'), str) or not user['uuid']:
        raise Unauthorized('请重新登录')
    if user.get('userType') not in ('family', 'rescuer', 'admin') or user.get('status') != 'active':
        raise Forbidden('账号未启用或身份无效，请联系管理员')
    g.current_user = user


def require_roles(*roles, approved=False):
    user = g.current_user
    if user['userType'] not in roles:
        raise Forbidden('当前身份不能使用此功能')
    if approved and user['userType'] == 'rescuer' and user.get('rescuerStatus') != 'available':
        raise Forbidden('请先通过队员审核后再执行救援任务')
    return user


def task_id(value):
    if isinstance(value, bool) or not isinstance(value, (str, int)) or not str(value).isdigit():
        raise BadRequest('请从具体任务中使用此功能，并提供有效的 taskId')
    value = int(value)
    if not 1 <= value <= 2147483647:
        raise BadRequest('任务编号无效')
    return value


def require_task(value):
    identifier = task_id(value)
    require_roles('family', 'rescuer', 'admin', approved=True)
    result = _java_json('/task/' + str(identifier))
    if result.get('code') in (401, 403, 404):
        raise Forbidden('任务不存在或无权访问')
    task = result.get('data')
    if result.get('code') != 0 or not isinstance(task, dict) or task.get('id') != identifier:
        raise ServiceUnavailable('暂时无法核验任务权限，请稍后重试')
    return task


def private_session(identifier):
    # A caller-chosen session ID is only a label inside that authenticated user's namespace.
    return (g.current_user['uuid'], identifier)


def recommendation_input(value):
    """Java derives candidates and confirmed-success counts from its database."""
    require_roles('family', 'admin')
    identifier = task_id(value)
    result = _java_json('/task/recommendation-input/' + str(identifier))
    if result.get('code') in (401, 403, 404):
        raise Forbidden('任务不存在或无权推荐')
    if result.get('code') == 409:
        raise Conflict('任务已结束或撤回，不能重新生成推荐')
    data = result.get('data')
    if (result.get('code') != 0 or not isinstance(data, dict)
            or not isinstance(data.get('task'), dict) or data['task'].get('id') != identifier
            or not isinstance(data.get('rescuers'), list)
            or any(not isinstance(row, dict) for row in data['rescuers'])
            or not isinstance(data.get('tagLibrary'), list)
            or any(not isinstance(tag, str) for tag in data['tagLibrary'])):
        raise ServiceUnavailable('暂时无法获取真实推荐资料，请稍后重试')
    return data
