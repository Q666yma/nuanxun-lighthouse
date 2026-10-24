package com.maka.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.maka.vo.SearchFeatureResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Base64;

public class SearchFeature {
    private String requestUrl;
    private String APPID;
    private String apiSecret;
    private String apiKey;
    private byte[] audioData;

    // 构造函数，接受音频数据而不是文件路径
    public SearchFeature(String requestUrl, String APPID, String apiSecret, String apiKey, byte[] audioData) {
        this.requestUrl = requestUrl;
        this.APPID = APPID;
        this.apiSecret = apiSecret;
        this.apiKey = apiKey;
        this.audioData = audioData;
    }

    // 请求主方法
    // 请求主方法，返回相似度和featureInfo
    public SearchFeatureResult doRequest(int topK) throws Exception {
        URL realUrl = new URL(buildRequestUrl());
        URLConnection connection = realUrl.openConnection();
        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-type", "application/json");

        OutputStream out = httpURLConnection.getOutputStream();
        String params = buildParam(topK);
        System.out.println("params=>" + params);
        out.write(params.getBytes());
        out.flush();

        InputStream is;
        try {
            is = httpURLConnection.getInputStream();
        } catch (Exception e) {
            is = httpURLConnection.getErrorStream();
            throw new Exception("make request error:" + "code is " + httpURLConnection.getResponseMessage() + readAllBytes(is));
        }

        // 获取返回的响应
        String response = readAllBytes(is);

        // 处理响应并返回最高相似度和featureInfo
        return processResponse(response);
    }


    // 处理响应，解码text并找到最高分数的featureInfo
    // 处理响应，解码text并找到最高分数的featureInfo和相似度
    private SearchFeatureResult processResponse(String response) {
        // 使用Gson解析初始的JSON响应
        Gson gson = new Gson();
        JsonParse jsonParse = gson.fromJson(response, JsonParse.class);

        // 获取Base64编码的text字段
        String encodedText = jsonParse.payload.searchFeaRes.text;

        // 解码Base64编码的text字段
        String decodedText = new String(Base64.getDecoder().decode(encodedText));

        // 解析解码后的内容
        JSONObject decodedJson = JSON.parseObject(decodedText);
        List<ScoreItem> scoreList = JSON.parseArray(decodedJson.getJSONArray("scoreList").toJSONString(), ScoreItem.class);

        // 找到分数最高的条目
        ScoreItem maxScoreItem = scoreList.stream().max(Comparator.comparingDouble(ScoreItem::getScore)).orElse(null);

        if (maxScoreItem != null) {
            return new SearchFeatureResult(maxScoreItem.getScore(), maxScoreItem.getFeatureInfo());  // 返回相似度和featureInfo
        }

        return new SearchFeatureResult(0.0, "No valid score found");  // 如果没有找到有效分数，返回0和提示信息
    }


    // 组装请求URL
    public String buildRequestUrl() {
        try {
            String httpRequestUrl = requestUrl.replace("ws://", "http://").replace("wss://", "https://");
            URL url = new URL(httpRequestUrl);
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());
            String host = url.getHost();

            if (url.getPort() != 80 && url.getPort() != 443) {
                host = host + ":" + url.getPort();
            }

            StringBuilder builder = new StringBuilder("host: ").append(host).append("\n")
                    .append("date: ").append(date).append("\n")
                    .append("POST ").append(url.getPath()).append(" HTTP/1.1");

            Charset charset = Charset.forName("UTF-8");
            Mac mac = Mac.getInstance("hmacsha256");
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
            String sha = Base64.getEncoder().encodeToString(hexDigits);

            String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                    apiKey, "hmac-sha256", "host date request-line", sha);
            String authBase = Base64.getEncoder().encodeToString(authorization.getBytes(charset));
            return String.format("%s?authorization=%s&host=%s&date=%s", requestUrl,
                    URLEncoder.encode(authBase, "UTF-8"),
                    URLEncoder.encode(host, "UTF-8"),
                    URLEncoder.encode(date, "UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("assemble requestUrl error:" + e.getMessage());
        }
    }


    // 构造请求参数
    private String buildParam(int topK) throws IOException {
        String audioBase64 = Base64.getEncoder().encodeToString(audioData);

        String param = "{\n" +
                "    \"header\": {\n" +
                "        \"app_id\": \"" + APPID + "\",\n" +
                "        \"status\": 3\n" +
                "    },\n" +
                "    \"parameter\": {\n" +
                "        \"s782b4996\": {\n" +
                "            \"func\": \"searchFea\",\n" +
                "            \"groupId\": \"iFLYTEK_examples_groupId\",\n" +
                "            \"topK\": " + topK + ",\n" +  // 可根据请求动态传入
                "            \"searchFeaRes\": {\n" +
                "                \"encoding\": \"utf8\",\n" +
                "                \"compress\": \"raw\",\n" +
                "                \"format\": \"json\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"payload\": {\n" +
                "        \"resource\": {\n" +
                "            \"encoding\": \"lame\",\n" +
                "            \"sample_rate\": 16000,\n" +
                "            \"channels\": 1,\n" +
                "            \"bit_depth\": 16,\n" +
                "            \"status\": 3,\n" +
                "            \"audio\": \"" + audioBase64 + "\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        return param;
    }

    // 读取输入流
    private String readAllBytes(InputStream is) throws IOException {
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        int len;
        while ((len = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, len, "utf-8"));
        }
        return sb.toString();
    }

    // Json解析类，声明为静态类
    public static class JsonParse {
        public Header header;
        public Payload payload;
    }

    public static class Header {
        public int code;
        public String message;
        public String sid;
        public int status;
    }

    public static class Payload {
        public SearchFeaRes searchFeaRes;
    }

    public static class SearchFeaRes {
        public String compress;
        public String encoding;
        public String format;
        public String text;
    }

    // ScoreItem类，用于处理scoreList中的条目，声明为静态类
    public static class ScoreItem {
        private double score;
        private String featureInfo;
        private String featureId;

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public String getFeatureInfo() {
            return featureInfo;
        }

        public void setFeatureInfo(String featureInfo) {
            this.featureInfo = featureInfo;
        }

        public String getFeatureId() {
            return featureId;
        }

        public void setFeatureId(String featureId) {
            this.featureId = featureId;
        }
    }
}
