import logging
import math
import re
import json
from utils.ai_client import create_zhipu_client
from config import Config
from utils.api_errors import ExternalServiceError

logger = logging.getLogger(__name__)

SEMANTIC_TYPES = frozenset(('searched', 'danger', 'poi', 'path'))


def semantic_marker_type(marker):
    """The outer markerType is usually a shape; the saved property is its meaning."""
    properties = marker.get('properties') or {}
    selected = properties.get('markerType')
    if isinstance(selected, str) and selected in SEMANTIC_TYPES:
        return selected
    selected = marker.get('markerType')
    return selected if isinstance(selected, str) and selected in SEMANTIC_TYPES else 'unknown'


def explicitly_searched(marker):
    properties = marker.get('properties') or {}
    description = properties.get('description', '')
    if not isinstance(description, str):
        return False
    # A partial or merely planned search must not colour an entire route as covered.
    if re.search(r'未(?:经|曾|能|完成)?(?:排查|搜索|搜查)|待(?:排查|搜索|搜查)|'
                 r'尚未|未完成|没有(?:排查|搜索|搜查)|计划|规划|拟|准备|部分|一半|局部|剩余', description):
        return False
    return bool(re.search(r'已(?:经)?(?:完成)?(?:排查|搜索|搜查)|'
                          r'(?:排查|搜索|搜查)(?:已)?完成', description))


def apply_marker_rules(marker, score):
    """Preserve recorded meaning with minimum visible intensity, not a new AI score."""
    if isinstance(score, bool) or not isinstance(score, (int, float)) or not math.isfinite(score) or not -1 <= score <= 1:
        raise ExternalServiceError('地图分析返回了无效分值')
    marker_type = semantic_marker_type(marker)
    if marker_type == 'searched':
        return -max(abs(score), 0.25), 'searched_minimum_negative' if abs(score) < 0.25 else 'searched_negative'
    if marker_type in ('danger', 'poi'):
        return max(abs(score), 0.25), 'attention_minimum_positive' if abs(score) < 0.25 else 'attention_nonnegative'
    if explicitly_searched(marker):
        if marker_type == 'path':
            return -max(abs(score), 0.25), 'explicitly_searched_minimum_negative' if abs(score) < 0.25 else 'explicitly_searched_negative'
        return -abs(score), 'explicitly_searched_negative'
    if marker_type == 'path':
        return 0.0, 'path_without_search_confirmation'
    return max(0.0, score), 'unconfirmed_nonnegative'


class MapHeatAIAssessor:
    """
    调用智谱清言/GLM模型为每个地图标注赋分（波峰/波谷/热力分值）
    """

    def __init__(self):
        self.client = create_zhipu_client(api_key=Config.ZHIPU_API_KEY)
        self.model = getattr(Config, "MAP_SCORE_MODEL", "glm-4-flash")

    def score_marker(self, marker: dict) -> float:
        """
        单个标注AI评分，返回区间[-1.0, 1.0]，负为波谷，正为波峰
        """
        geometry = marker.get("geometry")
        properties = marker.get("properties")
        marker_type = semantic_marker_type(marker)
        description = (properties or {}).get("description", "")

        # 构造prompt
        prompt = (
            "你是一名专业地图分析AI，为走失人员搜救提供地图辅助分析。仅依据提供的标记信息判断，不默认搜救对象的年龄、病史或行动能力。\n"
            "请根据以下地图标记的类型与描述判断关注度；负值只表示明确记录的已排查，绝不表示安全或不可能找到目标。"
            "searched应为负值，danger/poi应为非负关注值；规划路径path默认0，只有明确已完成排查的路径才能为负值。"
            "这不是失踪概率或找到概率。输出一个[-1.0,1.0]分值，0表示信息不足或中性。\n"
            f"类型: {marker_type}\n"
            f"描述: {description}\n"
            "请只返回分值(float)，不要其它文字。"

        )
        logger.info(f"收到海报生成请求: {prompt}")
        try:
            resp = self.client.chat.completions.create(
                model=self.model,
                messages=[{"role": "user", "content": prompt}],
                temperature=0.2,
                max_tokens=30
            )
            text = resp.choices[0].message.content.strip()
            logger.info(f"收到海报生成请求: {text}")
            # 只提取第一个浮点数
            import re
            m = re.fullmatch(r'[+-]?(?:\d+(?:\.\d*)?|\.\d+)', text)
            if m:
                value = float(m.group())
                if not math.isfinite(value) or not -1 <= value <= 1:
                    raise ExternalServiceError("地图分析返回了超出范围的分值")
                return value
            else:
                logger.warning(f"AI无有效分值返回: {text}")
                raise ExternalServiceError("地图分析服务未返回有效分值")
        except Exception as e:
            logger.error(f"智谱GLM评分失败: {e}", exc_info=True)
            raise

    def batch_score(self, marker_list: list) -> list:
        """
        批量评分，marker_list为marker字典列表。
        返回分值列表（顺序与输入一致）。
        """
        if not marker_list:
            return []

        # 构建批量 prompt
        prompt = (
            "你是一名专业地图分析AI。我们正在执行一项搜救走失人员的任务，搜救员会将搜索过的区域在地图上进行标记。仅依据提供的标记信息判断，不默认搜救对象的年龄、病史或行动能力。每个标记有类型和描述，请根据规则为每个标记评分：\n"
            "【标记类型说明】\n"
            "- searched：人员明确记录的已搜索区域，用负值表示这条排查记录；不代表安全、排除目标或可以停止复查。\n"
            "- danger：危险或需要关注的区域，用非负值表示关注程度，不推断目标出现概率。\n"
            "- poi：需要关注的线索或地点，用非负值表示关注程度，不推断存在被困人员。\n"
            "- path：路径本身不证明已排查。规划、待执行、部分排查或未说明完成情况的路径给0；仅明确已完成排查的整条路径可给负值。\n"
            "【评分规则】\n"
            "- 分值区间为[-1.0, 1.0]。\n"
            "- 负值仅用于明确的已排查记录；禁止把危险、未搜索或未知区域当作已排查。\n"
            "- searched不得改成正值；danger/poi不得改成负值。\n"
            "- path默认0；仅明确已完成排查、且不存在规划或未完成信息的整条路径可给负值。\n"
            "- 普通、不明确或证据不足可以给0；不得虚构搜索结果。\n"
            "- 数值是标记语义辅助展示，不是失踪概率、找到概率或安全等级。\n"
            "【输出格式】\n"
            f"本次共有 {len(marker_list)} 个标记，必须恰好返回 {len(marker_list)} 个得分，顺序与输入编号一致。\n"
            "只返回纯 JSON 数字数组，例如[-0.6,0,0.8]，不要 Markdown、编号、解释、对象或字符串。\n"
            "每个输入标记必须占一个数组位置；中性或信息不足也必须写0，不得合并或省略相似标记。\n"
            "请根据以上类型说明和规则，对以下逐项编号的标记进行评分：\n"
        )

        for index, marker in enumerate(marker_list, 1):
            marker_type = semantic_marker_type(marker)
            description = (marker.get("properties") or {}).get("description", "")
            prompt += f"{index}. 类型: {marker_type}，描述: {description}\n"

        try:
            resp = self.client.chat.completions.create(
                model=self.model,
                messages=[{"role": "user", "content": prompt}],
                temperature=0.1,
                max_tokens=70 * len(marker_list)
            )
            text = resp.choices[0].message.content.strip()
            if text.startswith('['):
                try:
                    scores = json.loads(text)
                except (json.JSONDecodeError, ValueError):
                    raise ExternalServiceError("地图分析返回了无效数字数组") from None
                if not isinstance(scores, list) or any(isinstance(value, bool) or not isinstance(value, (int, float)) for value in scores):
                    raise ExternalServiceError("地图分析返回了无效分值")
            else:
                # Retain the previous strict one-number-per-line format, without
                # extracting numbers from prose, Markdown or numbered answers.
                values = [line.strip() for line in text.splitlines() if line.strip()]
                if any(not re.fullmatch(r'[+-]?(?:\d+(?:\.\d*)?|\.\d+)', value) for value in values):
                    raise ExternalServiceError("地图分析返回了无效分值")
                scores = [float(value) for value in values]
            if any(not math.isfinite(value) or not -1 <= value <= 1 for value in scores):
                raise ExternalServiceError("地图分析返回了超出范围的分值")
            if len(scores) != len(marker_list):
                raise ExternalServiceError("地图分析返回数量与标注不一致")
            return scores
        except Exception as e:
            logger.error(f"智谱GLM批量评分失败: {e}", exc_info=True)
            raise
