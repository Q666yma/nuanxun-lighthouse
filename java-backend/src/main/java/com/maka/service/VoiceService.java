package com.maka.service;

import com.maka.vo.SearchFeatureResult;
import org.springframework.web.multipart.MultipartFile;

public interface VoiceService {

    /**
     * 创建声纹库
     * @return 服务响应结果
     */
    String createGroup();

    /**
     * 添加音频特征
     * @param groupId 组ID
     * @param featureId 特征ID
     * @param featureInfo 特征信息
     * @param audioFile 音频文件
     * @return 服务响应结果
     */
    String createFeature(String groupId, String featureId, String featureInfo, MultipartFile audioFile);

    /**
     * 声纹特征比对 1:N
     * @param groupId 组ID
     * @param topK 返回的匹配数量
     * @param audioFile 音频文件
     * @return 服务响应结果
     */
    SearchFeatureResult searchFeature(String groupId, int topK, MultipartFile audioFile);
}
