"""
改版说明
--------

1. **彻底去掉经纬度依赖**  
   - rescuer 表只要 uuid / name / location / skill_tags（JSON）即可  
   - lat / lon 字段不存在也没关系，程序里一律设为 None

2. **打分仅保留两个维度**  
   - 标签匹配  (weight = 0.6)
   - 历史成功数( weight = 0.4 )   ← 表里如果根本没有 success_count，就全当 0

3. **报告里的距离描述** 统一写成 “距离信息暂缺”。

后续接入地图 API：  
   把 Volunteer.lat / lon 赋值、把 WEIGHT_DISTANCE > 0，注释里有 TODO
"""
import logging
import json
from typing import List, Tuple, Any
from utils.ai_client import create_zhipu_client
from sqlalchemy import text, inspect
from utils.db import SessionLocal
from config import Config

logger = logging.getLogger(__name__)

# ---------- 可自调权重 ----------
WEIGHT_TAG      = 0.6   # 标签匹配
WEIGHT_SUCCESS  = 0.4   # 历史成功次数
WEIGHT_DISTANCE = 0.0   # === 暂时不用距离 === 以后接入地图再调到 0.2~0.4

class Volunteer:
    def __init__(self,
                 uuid: str,
                 name: str,
                 lat: Any,          # 目前恒为 None
                 lon: Any,
                 tags: List[str],
                 success_cnt: int):
        self.uuid = uuid
        self.name = name
        self.lat  = lat
        self.lon  = lon
        if isinstance(tags, str):
            try:
                tags = json.loads(tags)
            except ValueError:
                tags = []
        self.tags = [t for t in tags if isinstance(t, str)] if isinstance(tags, list) else []
        self.success_cnt = success_cnt or 0

class VolunteerRecommender:
    def __init__(self):
        self.client = create_zhipu_client(api_key=Config.ZHIPU_API_KEY)

    # ---------- 1. 拉候选志愿者 ----------
    def _fetch_volunteers(self) -> List[Volunteer]:
        """
        只取 uuid / name / location / skill_tags / success_count
        success_count 没有就默认 0
        """
        with SessionLocal() as db:
            # 动态检查列清单，防止缺列报错
            columns = {c["name"] for c in inspect(db.get_bind()).get_columns("rescuers")}

            has_success = "success_count" in columns

            sql = text(f"""
                SELECT uuid,
                       name,
                       skill_tags
                       {', success_count' if has_success else ''}
                FROM rescuers
                WHERE status = 'available'
            """)
            rows = db.execute(sql).mappings().all()

        volunteers = []
        for r in rows:
            volunteers.append(
                Volunteer(
                    uuid = r["uuid"],
                    name = r["name"],
                    lat  = None,          # 没有经纬度，先占位
                    lon  = None,
                    tags = r["skill_tags"] or [],
                    success_cnt = int(r.get("success_count") or 0)
                )
            )
        return volunteers

    # ---------- 2. 打分 ----------
    def _score(self,
               v: Volunteer,
               required_tags: List[str],
               max_success: int) -> float:

        # ---- 标签分 ----
        tag_overlap = len(set(required_tags) & set(v.tags))
        tag_score   = tag_overlap / max(1, len(required_tags))

        # ---- 成功分 ----
        succ_score = v.success_cnt / max(1, max_success)

        # ---- 距离分 固定 0.5，但权重本来就是 0 ----
        dist_score = 0.5

        total = (
            WEIGHT_TAG     * tag_score   +
            WEIGHT_SUCCESS * succ_score  +
            WEIGHT_DISTANCE* dist_score
        ) / (WEIGHT_TAG + WEIGHT_SUCCESS + WEIGHT_DISTANCE)

        logger.debug(
            f"{v.name} tag={tag_score:.2f}, succ={succ_score:.2f}, "
            f"TOTAL={total:.3f}"
        )
        return total

    # ---------- 3. 选 Top3 ----------
    def pick_top3(self,
                  required_tags: List[str]) -> List[Volunteer]:

        volunteers = self._fetch_volunteers()
        if not volunteers:
            return []

        max_success = max(v.success_cnt for v in volunteers) or 1

        scored: List[Tuple[Volunteer, float]] = [
            (v, self._score(v, required_tags, max_success)) for v in volunteers
        ]
        scored.sort(key=lambda x: x[1], reverse=True)
        return [v for v, _ in scored[:3]]

    # ---------- 4. 生成报告 ----------
    def gen_report(self,
                   family_name: str,
                   lost_address: str,
                   tags: List[str]) -> str:

        top3 = self.pick_top3(tags)
        if len(top3) < 3:
            return "当前可用志愿者不足，无法生成推荐报告。"

        def fmt_one(idx, v: Volunteer):
            return (
                f"志愿者{idx}：{v.name}\n"
                f"当前位置：距离信息暂缺\n"
                f"兴趣标签：{' '.join('#'+t for t in v.tags[:5])}\n"
                f"个人简介：待补充\n"
            )
        body = "\n".join(fmt_one(i+1, v) for i, v in enumerate(top3))

        prompt = f"""
请把下面的要点生成正式中文公文格式的“寻人志愿者推荐报告”，语气参考示例。

要点：
家庭称呼：{family_name}
失踪地点：{lost_address}
引用志愿者列表：
{body}

要求：
1. 标题“寻人志愿者推荐报告”居中
2. 署名“寻人平台志愿者协调组”，末尾保留“日期：{{date}}”占位符
3. 其他格式与示例保持一致
"""

        resp = self.client.chat.completions.create(
            model=Config.MODEL_NAME,
            messages=[
                {"role": "system", "content": "你是一名公文写作专家。"},
                {"role": "user",   "content": prompt}
            ]
        )
        return resp.choices[0].message.content.strip()
