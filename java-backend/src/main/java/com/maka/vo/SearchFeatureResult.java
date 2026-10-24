package com.maka.vo;

public class SearchFeatureResult {
    private double similarity;  // 相似度
    private String featureInfo; // 对应的特征信息

    // 构造函数
    public SearchFeatureResult(double similarity, String featureInfo) {
        this.similarity = similarity;
        this.featureInfo = featureInfo;
    }

    // 获取相似度
    public double getSimilarity() {
        return similarity;
    }

    // 获取特征信息
    public String getFeatureInfo() {
        return featureInfo;
    }

    // 打印类内容
    @Override
    public String toString() {
        return "SearchFeatureResult{" +
                "similarity=" + similarity +
                ", featureInfo='" + featureInfo + '\'' +
                '}';
    }
}
