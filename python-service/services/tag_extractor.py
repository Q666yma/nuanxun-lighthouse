from utils.ai_client import create_zhipu_client
from config import Config
import logging
import re
import json

def split_tags(raw_response):
    # 支持中英文逗号、顿号、分号、换行、空格等
    return [x.strip() for x in re.split(r"[，,、;\n\r\s]+", raw_response) if x.strip()]

logger = logging.getLogger(__name__)

class TagExtractor:
    """标签提取服务类"""
    
    def __init__(self, api_key=None, model=None):
        """初始化标签提取器

        Args:
            api_key (str, optional): 智谱API密钥. 默认使用配置文件中的密钥.
            model (str, optional): 模型名称. 默认使用配置文件中的模型.
        """
        self.api_key = api_key or Config.ZHIPU_API_KEY
        self.model = model or Config.MODEL_NAME
        self.client = create_zhipu_client(api_key=self.api_key)
        logger.info(f"标签提取服务初始化完成，使用模型: {self.model}")
    
    def _get_system_prompt(self, tag_library):
        """根据标签库生成系统提示词

        Args:
            tag_library (list): 标签库列表

        Returns:
            str: 系统提示词
        """
        tags_str = "、".join(tag_library)
        return (
            f"你是一位志愿者任务匹配助手。我会提供志愿者个人简介，你必须仅从固定标签库中挑选最合适的标签，若个人简介不够清晰则可以不返回任何标签。"
            f"输出请直接给出一个中文逗号分隔的标签列表，必须保证和接下来的标签库中的标签完全一样，如果相似度不够则可以不选择，我们的标签匹配比较严格，勿附加多余解释。标签库：{tags_str}。"
        )
    
    def _chat(self, user_bio, tag_library):
        """调用大模型API进行对话

        Args:
            user_bio (str): 用户技能描述
            tag_library (list): 标签库列表

        Returns:
            str: 模型回复原始文本
        """
        system_prompt = self._get_system_prompt(tag_library)
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"个人简介：{user_bio}"}
        ]
        
        try:
            logger.debug(f"发送请求到智谱AI，个人简介: {user_bio[:100]}{'...' if len(user_bio) > 100 else ''}")
            response = self.client.chat.completions.create(
                model=self.model, 
                messages=messages
            )
            result = response.choices[0].message.content.strip()
            logger.debug(f"智谱AI响应: {result}")
            return result
        except Exception as e:
            logger.error(f"调用智谱AI时发生错误: {str(e)}")
            raise
    
    def extract_tags(self, user_bio, tag_library=None):
        """从用户简介中提取相关技能标签

        Args:
            user_bio (str): 用户技能描述
            tag_library (list, optional): 标签库. 默认使用配置文件中的标签库.

        Returns:
            list: 提取出的标签列表
        """
        return self.extract_details(user_bio, tag_library)['tags']

    def extract_details(self, user_bio, tag_library=None):
        if not user_bio or not user_bio.strip():
            logger.warning("用户描述为空")
            return {'tags': [], 'source': 'local_keywords', 'message': '请填写技能简介'}
        
        # 如果未提供标签库，使用默认标签库
        if tag_library is None:
            tag_library = Config.TAG_LIBRARY
        
        try:
            # 调用模型获取原始文本
            raw_response = self._chat(user_bio, tag_library)
            
            # 处理模型输出，允许中英文逗号
            try:
                decoded = json.loads(re.sub(r'^```(?:json)?\s*|\s*```$', '', raw_response.strip(), flags=re.I))
                candidates = decoded.get('tags', []) if isinstance(decoded, dict) else decoded
                if not isinstance(candidates, list):
                    candidates = split_tags(raw_response)
            except (ValueError, TypeError):
                candidates = split_tags(raw_response)
            logger.info(f"模型提取的候选标签: {candidates}")
            
            # 与标签库取交集，确保标签有效
            valid_tags = list(dict.fromkeys(tag for tag in candidates if isinstance(tag, str) and tag in tag_library))
            logger.info(f"有效标签: {valid_tags}")
            
            return {'tags': valid_tags, 'source': 'ai', 'message': '请核对并确认技能标签'}
        except Exception as e:
            logger.warning("技能标签AI不可用，使用明确关键词匹配: %s", type(e).__name__)
            tags = [tag for tag in dict.fromkeys(tag_library) if tag.lower() in user_bio.lower()]
            return {'tags': tags, 'source': 'local_keywords',
                    'message': 'AI暂不可用，已按简介中的明确技能词匹配，请核对后保存；可手动补充标签'}
