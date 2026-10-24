import json
from flask import Blueprint, jsonify
from services.map_service import MapHeatAIAssessor, semantic_marker_type, apply_marker_rules, SEMANTIC_TYPES, explicitly_searched
from utils.api_errors import json_object, service_unavailable
import math
from utils.authorization import require_task

ai_assessor = MapHeatAIAssessor()
map_bp = Blueprint("map", __name__)

def get_center(geometry):
    """根据GeoJSON geometry类型，返回其中心点(lng,lat)"""
    if not isinstance(geometry, dict):
        raise ValueError("标注缺少有效坐标")
    def position(value):
        if (not isinstance(value, (list, tuple)) or len(value) != 2
                or any(isinstance(n, bool) or not isinstance(n, (int, float)) or not math.isfinite(n) for n in value)
                or not -180 <= value[0] <= 180 or not -90 <= value[1] <= 90):
            raise ValueError('标注坐标必须是有效经纬度')
        return tuple(value)
    coords = geometry['coordinates']
    if geometry["type"] == "Point":
        center = position(coords)
    elif geometry["type"] == "LineString":
        if not isinstance(coords, list) or not 2 <= len(coords) <= 10000:
            raise ValueError('路径坐标数量无效')
        points = [position(point) for point in coords]
        center = points[len(points) // 2]
    elif geometry["type"] == "Polygon":
        if not isinstance(coords, list) or not coords:
            raise ValueError('区域坐标无效')
        rings = []
        for ring in coords:
            if not isinstance(ring, list) or not 4 <= len(ring) <= 10000:
                raise ValueError('区域边界无效')
            points = [position(point) for point in ring]
            if points[0] != points[-1]:
                raise ValueError('区域边界未闭合')
            rings.append(points[:-1])
        # The outer ring locates the region; duplicate closure and holes must
        # not bias its display anchor toward one vertex.
        center = tuple(sum(point[axis] for point in rings[0]) / len(rings[0]) for axis in (0, 1))
    else:
        raise ValueError("Unsupported geometry type: {}".format(geometry["type"]))
    if len(center) != 2 or any(not math.isfinite(float(value)) for value in center):
        raise ValueError("标注坐标必须是有限经纬度")
    if not -180 <= float(center[0]) <= 180 or not -90 <= float(center[1]) <= 90:
        raise ValueError("标注经纬度超出范围")
    return center

@map_bp.route('/mapheat', methods=['POST'])
def mapheat():
    data = json_object()
    require_task(data.get('taskId'))
    markers = data.get("markers", [])
    if not isinstance(markers, list) or len(markers) > 100 or any(not isinstance(m, dict) for m in markers):
        return jsonify(status="error", message="请提供不超过 100 个有效标注", heatmapPoints=[]), 400
    try:
        if not markers:
            return jsonify(status="error", message="没有标注数据", heatmapPoints=[])
        # 1. 先收集所有marker_copy用于AI打分
        marker_copys = []
        centers = []
        for marker in markers:
            geometry = marker.get("geometry")
            if isinstance(geometry, str):
                geometry = json.loads(geometry)
            properties = marker.get("properties")
            if isinstance(properties, str):
                properties = json.loads(properties)
            if properties is not None and not isinstance(properties, dict):
                raise ValueError("标注属性格式错误")
            marker_type = semantic_marker_type({'markerType': marker.get('markerType'), 'properties': properties})
            marker_copy = {
                "geometry": geometry,
                "properties": properties,
                "markerType": marker_type,
                "id": marker.get('id')
            }
            marker_copys.append(marker_copy)
            lng, lat = get_center(geometry)
            centers.append((lng, lat))

        # 2. AI批量赋分（每个marker只打分一次）
        score_source = 'ai_with_marker_rules'
        try:
            scores = ai_assessor.batch_score(marker_copys)
            if len(scores) != len(marker_copys):
                raise ValueError('地图分析返回数量与标注不一致')
            display_scores = [apply_marker_rules(marker, score) for marker, score in zip(marker_copys, scores)]
        except Exception:
            if not all(marker['markerType'] in SEMANTIC_TYPES for marker in marker_copys):
                return service_unavailable("地图智能分析暂时不可用，请稍后重试")
            # Known manual categories can still be shown, explicitly as a rules
            # illustration. No substitute AI score is reported or inferred.
            score_source = 'marker_rules_ai_unavailable'
            scores = [None] * len(marker_copys)
            display_scores = []
            rules = {'searched': (-0.6, 'manual_searched'),
                     'danger': (0.75, 'manual_danger'),
                     'poi': (0.55, 'manual_poi')}
            for marker in marker_copys:
                if marker['markerType'] == 'path':
                    display_scores.append((-0.5, 'manual_path_explicitly_searched') if explicitly_searched(marker)
                                          else (0.0, 'manual_path_without_search_confirmation'))
                else:
                    display_scores.append(rules[marker['markerType']])
        
        result_points = []
        for marker, (lng, lat), raw_score, (value, rule) in zip(marker_copys, centers, scores, display_scores):
            marker_id = marker.get('id')
            result_points.append({
                "lng": float(lng),
                "lat": float(lat),
                "value": float(value),
                "rawScore": None if raw_score is None else float(raw_score),
                "scoreSource": score_source,
                "scoreRule": rule,
                "markerId": marker_id if isinstance(marker_id, int) and not isinstance(marker_id, bool) and marker_id > 0 else None,
                "markerType": marker['markerType'],
                "geometry": marker['geometry'],
                # Display support is local to the recorded point; polygons and paths
                # retain their exact geometry instead of becoming a 3 km circle.
                "radiusMeters": 120
            })

        return jsonify(status="success", heatmapPoints=result_points)
    except (ValueError, KeyError, IndexError, TypeError):
        return jsonify(status="error", message="标注坐标或属性格式错误", heatmapPoints=[]), 400
