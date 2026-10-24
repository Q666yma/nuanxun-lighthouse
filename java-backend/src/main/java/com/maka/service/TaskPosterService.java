package com.maka.service;

import java.util.Map;

/**
 * 任务海报服务接口
 */
public interface TaskPosterService {
    
    /**
     * 生成HTML海报
     * 
     * @param elderName 老人姓名
     * @param lostTime 走失时间
     * @param location 走失地点
     * @param photoUrl 照片URL
     * @param extraInfo 额外信息
     * @param templateData 模板数据
     * @param userId 用户ID
     * @return 包含海报HTML和CSS的结果
     */
    Map<String, Object> generateHtmlPoster(
            String elderName, 
            String lostTime, 
            String location, 
            String photoUrl, 
            String extraInfo, 
            Map<String, Object> templateData,
            String contactPhone,   // 联系电话
            String currentTime,    // 当前时间
            String userId);
}