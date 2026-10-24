import json
import re
from concurrent.futures import ThreadPoolExecutor
from typing import Dict
from utils.ai_client import create_zhipu_client
from utils.api_errors import ExternalServiceError
from config import Config

_PROMPTS = {
    'fear': '你扮演一位走失人员，情绪激动，反复说想找到家人。说话口语化，根据情境回应，不自行添加年龄或病史。',
    'refuse': '你扮演一位走失人员，对陌生人高度警惕，拒绝回答问题。说话口语化，根据情境回应，不自行添加年龄或病史。',
    'night': '你扮演深夜寒冷中的走失人员，情绪低落并颤抖。说话口语化，根据情境回应，不自行添加年龄或病史。',
}
_SCORE_SYSTEM = ('你是搜救对话训练教练，请针对志愿者的话给出 0-100 分的训练反馈。'
                 'emotion 为安抚情绪、info 为收集信息、safe 为关注安全；advice 为中文建议。'
                 '仅输出 JSON：{"emotion":数值,"info":数值,"safe":数值,"advice":"建议"}')


class SceneTrainService:
    def __init__(self):
        self.client = create_zhipu_client(api_key=Config.ZHIPU_API_KEY)
        self.model = Config.MODEL_NAME
        self.rooms: Dict[str, Dict] = {}

    def start(self, sid, scene):
        history = [{'role': 'system', 'content': _PROMPTS[scene]},
                   {'role': 'user', 'content': '请以场景中走失人员的身份开始说第一句话。'}]
        reply = self._chat(history)
        self.rooms[sid] = {'scene': scene, 'history': history + [{'role': 'assistant', 'content': reply}], 'scoreLog': []}
        while len(self.rooms) > 200:
            self.rooms.pop(next(iter(self.rooms)))
        return {'reply': reply, 'score': None, 'done': False}

    def talk(self, sid, user):
        room = self.rooms[sid]
        pending = room['history'] + [{'role': 'user', 'content': user}]
        trimmed = [pending[0]] + pending[1:][-28:]
        # The actor and coach have different prompts and independent model calls.
        # Neither commits a partial turn if the other call fails.
        with ThreadPoolExecutor(max_workers=2) as executor:
            actor = executor.submit(self._chat, trimmed)
            coach = executor.submit(self._score, user, room)
            reply, score = actor.result(), coach.result()
        # Commit this turn only when both real responses are valid; retries do
        # not append duplicate history and provider failures never invent scores.
        room['history'] = trimmed + [{'role': 'assistant', 'content': reply}]
        room['scoreLog'].append(score)
        done = len(room['scoreLog']) >= 5
        report = self._report(room) if done else {}
        if done:
            self.rooms.pop(sid, None)
        return {'reply': reply, 'score': score, 'done': done, 'report': report}

    def _chat(self, messages):
        response = self.client.chat.completions.create(
            model=self.model, messages=messages, temperature=0.7, max_tokens=256)
        reply = response.choices[0].message.content
        if not isinstance(reply, str) or not reply.strip():
            raise ExternalServiceError('训练服务未返回内容')
        return reply.strip()

    def _score(self, user_msg, room=None):
        context = {'scene': (room or {}).get('scene'),
                   'history': (room or {}).get('history', [])[1:][-12:],
                   'volunteer_message': user_msg}
        response = self.client.chat.completions.create(
            model=self.model,
            messages=[{'role': 'system', 'content': _SCORE_SYSTEM},
                      {'role': 'user', 'content': json.dumps(context, ensure_ascii=False)}],
            temperature=0.3, max_tokens=300)
        raw = response.choices[0].message.content or ''
        raw = re.sub(r'^```(?:json)?\s*|\s*```$', '', raw.strip(), flags=re.I)
        try:
            result = json.loads(raw)
            for field in ('emotion', 'info', 'safe'):
                value = result[field]
                if isinstance(value, bool) or not isinstance(value, (int, float)) or not 0 <= value <= 100:
                    raise ValueError()
            if not isinstance(result.get('advice'), str) or not result['advice'].strip():
                raise ValueError()
            return {key: result[key] for key in ('emotion', 'info', 'safe', 'advice')}
        except (ValueError, TypeError, KeyError):
            raise ExternalServiceError('训练评分服务返回无效内容') from None

    def _report(self, room):
        scores = room['scoreLog']
        report = {field: round(sum(s[field] for s in scores) / len(scores), 1)
                  for field in ('emotion', 'info', 'safe')}
        labels = {'emotion': '情绪安抚', 'info': '信息获取', 'safe': '安全关注'}
        practices = {'emotion': '先表明身份，认可对方情绪，使用简短平和的表达。',
                     'info': '一次询问一个问题，核对姓名、联系人及熟悉地点，避免连续追问。',
                     'safe': '先确认眼前环境、保暖与身体不适，遇紧急风险及时联系专业救援。'}
        best = max(report.values())
        weakest = min(report.values())
        report['rounds'] = len(scores)
        report['strengths'] = [labels[key] for key in labels if report[key] == best]
        report['improvements'] = [labels[key] for key in labels if report[key] == weakest]
        report['dimension_advice'] = {key: f'五轮平均 {report[key]:.1f} 分。{practices[key]}' for key in labels}
        report['scoreLog'] = [dict(score) for score in scores]
        report['advice'] = ('本次相对优势：' + '、'.join(report['strengths']) + '；优先练习：'
                            + '、'.join(report['improvements']) + '。教练最后一轮建议：' + scores[-1]['advice'])
        return report
