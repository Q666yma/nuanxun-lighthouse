import logging
import uuid
from datetime import datetime
from html import escape
from urllib.parse import urlsplit


def _escape_values(value):
    if isinstance(value, dict):
        return {key: _escape_values(item) for key, item in value.items()}
    if isinstance(value, list):
        return [_escape_values(item) for item in value]
    return escape(str(value), quote=True) if value is not None else ''

logger = logging.getLogger(__name__)

class PosterGenerator:
    """海报生成器类 - 生成带内联样式的HTML海报"""
    
    def __init__(self):
        pass
    
    def generate(self, data):
        """生成HTML海报"""
        try:
            photo_url = str(data.get('photoUrl', ''))
            parsed = urlsplit(photo_url)
            if parsed.scheme not in ('', 'http', 'https') or photo_url.startswith('//'):
                return {'success': False, 'message': '照片地址无效'}
            # All inserted values are text/attributes, never trusted HTML.
            data = _escape_values(data)
            # 提取基本信息
            elder_name = data.get('elderName', '')
            lost_time = data.get('lostTime', '')
            location = data.get('location', '')
            photo_url = data.get('photoUrl', '')
            contact_phone = data.get('contactPhone', '110')
            current_time = data.get('currentTime', datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
            poster_id = data.get('posterId', str(uuid.uuid4()))
            
            # 生成特征描述
            features_html = self._generate_features_html(data)
            
            # 获取完整的自包含HTML（包含内联CSS）
            complete_html = self._get_complete_poster_html(
                elder_name, lost_time, location, photo_url, 
                contact_phone, current_time, features_html
            )
            
            # 提取CSS用于预览
            css = self._get_preview_css()
            
            logger.info(f"海报生成成功，海报ID: {poster_id}")
            
            return {
                'success': True,
                'html': complete_html,  # 完整的自包含HTML
                'css': css,             # 预览用的CSS
                'posterId': poster_id
            }
            
        except Exception as e:
            logger.error(f"生成海报时出错: {str(e)}")
            return {
                'success': False,
                'message': '海报生成失败，请核对填写的信息后重试'
            }

    def _get_complete_poster_html(self, elder_name, lost_time, location, photo_url, 
                                 contact_phone, current_time, features_html):
        """返回完整的自包含HTML，所有样式都内联"""
        
        complete_html = f"""
<div class="poster-container" style="
    width: 100%;
    max-width: 760px;
    margin: 0 auto;
    font-family: 'Microsoft YaHei', 'SimSun', Arial, sans-serif;
    color: #333;
    background-color: #fff;
    padding: 20px;
    box-shadow: 0 0 10px rgba(0,0,0,0.1);
    border: 3px solid #e74c3c;
    box-sizing: border-box;
">
    <div class="poster-header" style="
        text-align: center;
        padding: 15px 0;
        border-bottom: 3px solid #e74c3c;
        margin-bottom: 20px;
    ">
        <h1 style="
            font-size: 36px;
            color: #e74c3c;
            margin: 0;
            font-weight: bold;
            letter-spacing: 4px;
            font-family: 'Microsoft YaHei', 'SimSun', serif;
        ">寻 人 启 事</h1>
        <p style="
            font-size: 18px;
            color: #555;
            margin: 10px 0 0;
            font-weight: 500;
        ">请帮助寻找走失人员</p>
    </div>
    
    <div class="poster-content" style="
        display: table;
        width: 100%;
        margin-bottom: 20px;
        table-layout: fixed;
    ">
        <div class="poster-left" style="
            display: table-cell;
            width: 50%;
            vertical-align: top;
            padding-right: 15px;
        ">
            <div class="poster-image" style="
                text-align: center;
                margin-bottom: 15px;
                border: 3px solid #ddd;
                padding: 10px;
                background: #f9f9f9;
                border-radius: 8px;
            ">
                <img src="{photo_url}" alt="{elder_name}的照片" style="
                    max-width: 100%;
                    max-height: 280px;
                    object-fit: contain;
                    border: 2px solid #ccc;
                    border-radius: 5px;
                ">
            </div>
            
            <div class="poster-basic-info" style="
                background-color: #f8f9fa;
                padding: 18px;
                border-radius: 8px;
                border: 2px solid #e9ecef;
            ">
                <div style="
                    margin-bottom: 12px; 
                    line-height: 1.8;
                    font-size: 15px;
                ">
                    <span style="
                        font-weight: bold; 
                        color: #e74c3c; 
                        display: inline-block; 
                        width: 80px;
                        font-size: 16px;
                    ">姓名：</span><span style="font-size: 16px; font-weight: 500;">{elder_name}</span>
                </div>
                <div style="
                    margin-bottom: 12px; 
                    line-height: 1.8;
                    font-size: 15px;
                ">
                    <span style="
                        font-weight: bold; 
                        color: #e74c3c; 
                        display: inline-block; 
                        width: 80px;
                        font-size: 16px;
                    ">走失时间：</span><span style="font-size: 14px;">{lost_time}</span>
                </div>
                <div style="
                    margin-bottom: 0; 
                    line-height: 1.8;
                    font-size: 15px;
                ">
                    <span style="
                        font-weight: bold; 
                        color: #e74c3c; 
                        display: inline-block; 
                        width: 80px;
                        font-size: 16px;
                    ">走失地点：</span><span style="font-size: 14px;">{location}</span>
                </div>
            </div>
        </div>
        
        <div class="poster-right" style="
            display: table-cell;
            width: 50%;
            vertical-align: top;
            padding-left: 15px;
        ">
            <div class="poster-features" style="
                background-color: #f8f9fa;
                padding: 18px;
                border-radius: 8px;
                border: 2px solid #e9ecef;
                min-height: 380px;
            ">
                {features_html}
            </div>
        </div>
    </div>
    
    <div class="poster-footer" style="
        text-align: center;
        margin-top: 25px;
        padding-top: 20px;
        border-top: 3px solid #e74c3c;
    ">
        <div style="
            font-size: 24px;
            font-weight: bold;
            color: #e74c3c;
            margin-bottom: 15px;
            line-height: 1.5;
        ">有线索请联系：<span style="
            font-size: 28px;
            background-color: #f8d7da;
            padding: 10px 20px;
            border-radius: 8px;
            border: 3px solid #e74c3c;
            display: inline-block;
            margin-left: 10px;
            color: #000;
            font-weight: bold;
        ">{contact_phone}</span></div>
        <p style="
            color: #666; 
            font-size: 13px; 
            margin: 10px 0;
            line-height: 1.4;
        ">发布时间：{current_time}</p>
        <p style="
            color: #666; 
            font-size: 12px; 
            margin: 8px 0;
            line-height: 1.4;
        ">本海报仅供寻人用，请勿用于其他用途。如有发现请及时联系，谢谢！</p>
    </div>
</div>

<style>
@media print {{
    * {{
        -webkit-print-color-adjust: exact !important;
        color-adjust: exact !important;
    }}
    
    body {{
        margin: 0 !important;
        padding: 0 !important;
        font-family: 'Microsoft YaHei', 'SimSun', Arial, sans-serif !important;
        background: white !important;
    }}
    
    .poster-container {{
        box-shadow: none !important;
        margin: 0 !important;
        max-width: none !important;
        border: 3px solid #000 !important;
        width: 100% !important;
        page-break-inside: avoid !important;
    }}
    
    .poster-content {{
        display: table !important;
        width: 100% !important;
    }}
    
    .poster-left, .poster-right {{
        display: table-cell !important;
        vertical-align: top !important;
    }}
    
    .poster-image img {{
        max-height: 250px !important;
        width: auto !important;
    }}
    
    @page {{
        size: A4;
        margin: 1cm;
    }}
}}

@media screen {{
    .poster-container {{
        box-shadow: 0 4px 12px rgba(0,0,0,0.15) !important;
    }}
}}
</style>
"""
        
        return complete_html

    def _generate_features_html(self, data):
        """生成特征描述HTML - 使用内联样式"""
        template_data = data.get('templateData') or {}
        if not isinstance(template_data, dict):
            template_data = {}
        extra_info = data.get('extraInfo', '')
        
        features_parts = []
        
        # 基本信息
        basic_info = []
        if template_data.get('age'):
            basic_info.append(f"年龄：{template_data['age']}岁")
        if template_data.get('gender'):
            basic_info.append(f"性别：{template_data['gender']}")
        if template_data.get('height'):
            basic_info.append(f"身高：约{template_data['height']}cm")
        if template_data.get('weight'):
            basic_info.append(f"体重：约{template_data['weight']}kg")
        
        if basic_info:
            features_parts.append(f'''
            <div style="margin-bottom: 18px;">
                <h3 style="
                    font-size: 16px; 
                    color: #e74c3c; 
                    margin: 0 0 10px 0; 
                    font-weight: bold; 
                    border-bottom: 2px solid #e74c3c; 
                    padding-bottom: 5px;
                    font-family: 'Microsoft YaHei', 'SimSun', sans-serif;
                ">基本特征</h3>
                <div style="
                    line-height: 1.8; 
                    font-size: 14px;
                    color: #333;
                    padding-left: 8px;
                ">{"、".join(basic_info)}</div>
            </div>
            ''')
        
        # 身体状况
        health_info = []
        if template_data.get('health') and isinstance(template_data['health'], list):
            health_info.extend(template_data['health'])
        if template_data.get('mobility') and isinstance(template_data['mobility'], list):
            health_info.extend(template_data['mobility'])
        
        if health_info:
            features_parts.append(f'''
            <div style="margin-bottom: 18px;">
                <h3 style="
                    font-size: 16px; 
                    color: #e74c3c; 
                    margin: 0 0 10px 0; 
                    font-weight: bold; 
                    border-bottom: 2px solid #e74c3c; 
                    padding-bottom: 5px;
                    font-family: 'Microsoft YaHei', 'SimSun', sans-serif;
                ">身体状况</h3>
                <div style="
                    line-height: 1.8; 
                    font-size: 14px;
                    color: #333;
                    padding-left: 8px;
                ">{"、".join(health_info)}</div>
            </div>
            ''')
        
        # 外貌特征
        appearance_info = []
        if template_data.get('bodyType') and isinstance(template_data['bodyType'], list):
            appearance_info.extend(template_data['bodyType'])
        if template_data.get('hair') and isinstance(template_data['hair'], list):
            appearance_info.extend(template_data['hair'])
        if template_data.get('marks') and isinstance(template_data['marks'], list):
            appearance_info.extend(template_data['marks'])
        
        if appearance_info:
            features_parts.append(f'''
            <div style="margin-bottom: 18px;">
                <h3 style="
                    font-size: 16px; 
                    color: #e74c3c; 
                    margin: 0 0 10px 0; 
                    font-weight: bold; 
                    border-bottom: 2px solid #e74c3c; 
                    padding-bottom: 5px;
                    font-family: 'Microsoft YaHei', 'SimSun', sans-serif;
                ">外貌特征</h3>
                <div style="
                    line-height: 1.8; 
                    font-size: 14px;
                    color: #333;
                    padding-left: 8px;
                ">{"、".join(appearance_info)}</div>
            </div>
            ''')
        
        # 着装信息
        clothing_info = []
        if template_data.get('topClothing'):
            clothing_info.append(f"上衣：{template_data['topClothing']}")
        if template_data.get('bottomClothing'):
            clothing_info.append(f"下装：{template_data['bottomClothing']}")
        if template_data.get('shoes'):
            clothing_info.append(f"鞋子：{template_data['shoes']}")
        if template_data.get('belongings') and isinstance(template_data['belongings'], list):
            clothing_info.append(f"随身物品：{', '.join(template_data['belongings'])}")
        
        if clothing_info:
            features_parts.append(f'''
            <div style="margin-bottom: 18px;">
                <h3 style="
                    font-size: 16px; 
                    color: #e74c3c; 
                    margin: 0 0 10px 0; 
                    font-weight: bold; 
                    border-bottom: 2px solid #e74c3c; 
                    padding-bottom: 5px;
                    font-family: 'Microsoft YaHei', 'SimSun', sans-serif;
                ">着装信息</h3>
                <div style="
                    line-height: 1.8; 
                    font-size: 14px;
                    color: #333;
                    padding-left: 8px;
                ">{"；".join(clothing_info)}</div>
            </div>
            ''')
        
        # 行为习惯
        behavior_info = []
        if template_data.get('hobbies') and isinstance(template_data['hobbies'], list):
            behavior_info.append(f"爱好：{', '.join(template_data['hobbies'])}")
        if template_data.get('activities') and isinstance(template_data['activities'], list):
            behavior_info.append(f"日常活动：{', '.join(template_data['activities'])}")
        if template_data.get('behavior') and isinstance(template_data['behavior'], list):
            behavior_info.append(f"行为特点：{', '.join(template_data['behavior'])}")
        if template_data.get('frequentPlaces') and isinstance(template_data['frequentPlaces'], list):
            behavior_info.append(f"可能去往：{', '.join(template_data['frequentPlaces'])}")
        
        if behavior_info:
            features_parts.append(f'''
            <div style="margin-bottom: 18px;">
                <h3 style="
                    font-size: 16px; 
                    color: #e74c3c; 
                    margin: 0 0 10px 0; 
                    font-weight: bold; 
                    border-bottom: 2px solid #e74c3c; 
                    padding-bottom: 5px;
                    font-family: 'Microsoft YaHei', 'SimSun', sans-serif;
                ">行为习惯</h3>
                <div style="
                    line-height: 1.8; 
                    font-size: 14px;
                    color: #333;
                    padding-left: 8px;
                ">{"；".join(behavior_info)}</div>
            </div>
            ''')
        
        # 紧急注意事项
        if template_data.get('emergency') and isinstance(template_data['emergency'], list):
            features_parts.append(f'''
            <div style="margin-bottom: 18px;">
                <h3 style="
                    font-size: 16px; 
                    color: #dc3545; 
                    margin: 0 0 10px 0; 
                    font-weight: bold; 
                    border-bottom: 2px solid #dc3545; 
                    padding-bottom: 5px;
                    font-family: 'Microsoft YaHei', 'SimSun', sans-serif;
                ">紧急注意</h3>
                <div style="
                    line-height: 1.8; 
                    font-size: 14px;
                    color: #dc3545;
                    font-weight: 500;
                    padding-left: 8px;
                ">{", ".join(template_data['emergency'])}</div>
            </div>
            ''')
        
        # 额外信息
        # if extra_info:
        #     features_parts.append(f'''
        #     <div style="margin-bottom: 18px;">
        #         <h3 style="
        #             font-size: 16px; 
        #             color: #e74c3c; 
        #             margin: 0 0 10px 0; 
        #             font-weight: bold; 
        #             border-bottom: 2px solid #e74c3c; 
        #             padding-bottom: 5px;
        #             font-family: 'Microsoft YaHei', 'SimSun', sans-serif;
        #         ">其他信息</h3>
        #         <div style="
        #             line-height: 1.8; 
        #             font-size: 14px;
        #             color: #333;
        #             padding-left: 8px;
        #         ">{extra_info}</div>
        #     </div>
        #     ''')
        
        if extra_info:
            features_parts.append('<div style="margin-bottom:18px;white-space:pre-wrap">'
                                  '<h3>其他信息</h3>' + str(extra_info) + '</div>')

        # 如果没有任何特征信息
        if not features_parts:
            features_parts.append('''
            <div style="
                line-height: 1.8; 
                font-size: 14px; 
                color: #666;
                text-align: center;
                padding: 20px 0;
                font-style: italic;
            ">暂无详细特征信息</div>
            ''')
        
        return ''.join(features_parts)

    def _get_preview_css(self):
        """返回预览用的CSS（用于弹窗显示）"""
        return """
        .poster-preview-wrapper { 
            overflow: hidden; 
            background: #f5f5f5;
            padding: 10px;
        }
        .poster-container { 
            margin: 0 auto; 
            background: white;
        }
        """
