package com.maka.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;

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
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 添加音频特征
 */
public class CreateFeature {
    private String requestUrl;
    private String APPID;
    private String apiSecret;
    private String apiKey;
    private byte[] audioData;
    private String groupId;
    private String featureId;
    private String featureInfo;

    // 修改后的构造函数，接收动态参数
    public CreateFeature(String requestUrl, String APPID, String apiSecret, String apiKey, byte[] audioData, String groupId, String featureId, String featureInfo) {
        this.requestUrl = requestUrl;
        this.APPID = APPID;
        this.apiSecret = apiSecret;
        this.apiKey = apiKey;
        this.audioData = audioData;
        this.groupId = groupId;          // 从前端传入
        this.featureId = featureId;      // 从前端传入
        this.featureInfo = featureInfo;  // 从前端传入
    }

    public String doRequest() throws Exception {
        URL realUrl = new URL(buildRequetUrl());
        URLConnection connection = realUrl.openConnection();
        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-type", "application/json");

        OutputStream out = httpURLConnection.getOutputStream();
        String params = buildParam();
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
        return readAllBytes(is);
    }

    public String buildRequetUrl() {
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
            String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
            String authBase = Base64.getEncoder().encodeToString(authorization.getBytes(charset));
            return String.format("%s?authorization=%s&host=%s&date=%s", requestUrl,
                    URLEncoder.encode(authBase, "UTF-8"),
                    URLEncoder.encode(host, "UTF-8"),
                    URLEncoder.encode(date, "UTF-8"));

        } catch (Exception e) {
            throw new RuntimeException("assemble requestUrl error:" + e.getMessage());
        }
    }

    // 构造请求参数，使用从前端传入的参数
    private String buildParam() throws IOException {
        // 将音频数据转换为 Base64 字符串
        String audioBase64 = Base64.getEncoder().encodeToString(audioData);

        String param = "{\n" +
                "    \"header\": {\n" +
                "        \"app_id\": \"" + APPID + "\",\n" +
                "        \"status\": 3\n" +
                "    },\n" +
                "    \"parameter\": {\n" +
                "        \"s782b4996\": {\n" +
                "            \"func\": \"createFeature\",\n" +
                // 这里使用从前端传入的 groupId、featureId 和 featureInfo
                "            \"groupId\": \"" + groupId + "\",\n" +
                "            \"featureId\": \"" + featureId + "\",\n" +
                "            \"featureInfo\": \"" + featureInfo + "\",\n" +
                "            \"createFeatureRes\": {\n" +
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

    private String readAllBytes(InputStream is) throws IOException {
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        int len;
        while ((len = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, len, "utf-8"));
        }
        return sb.toString();
    }

    public static byte[] read(String filePath) throws IOException {
        InputStream in = new FileInputStream(filePath);
        byte[] data = inputStream2ByteArray(in);
        in.close();
        return data;
    }

    private static byte[] inputStream2ByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    class JsonParse {
        public Header header;
        public Payload payload;
    }

    class Header {
        public int code;
        public String message;
        public String sid;
        public int status;
    }

    class Payload {
        public CreateFeatureRes createFeatureRes;
    }

    class CreateFeatureRes {
        public String compress;
        public String encoding;
        public String format;
        public String text;
    }
}
