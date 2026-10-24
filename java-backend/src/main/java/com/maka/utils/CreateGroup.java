package com.maka.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

public class CreateGroup {

    private static final Logger logger = LoggerFactory.getLogger(CreateGroup.class);

    private String requestUrl;
    private String APPID;
    private String apiSecret;
    private String apiKey;

    private static Gson json = new Gson();

    public CreateGroup(String requestUrl, String APPID, String apiSecret, String apiKey) {
        this.requestUrl = requestUrl;
        this.APPID = APPID;
        this.apiSecret = apiSecret;
        this.apiKey = apiKey;
    }

    public String doCreateGroup() {
        try {
            String resp = doRequest();
            logger.info("Response: {}", resp);
            JsonParse myJsonParse = json.fromJson(resp, JsonParse.class);
            String textBase64Decode = new String(Base64.getDecoder().decode(myJsonParse.payload.createGroupRes.text), "UTF-8");
            JSONObject jsonObject = JSON.parseObject(textBase64Decode);
            logger.info("Decoded Text: {}", jsonObject);
            return jsonObject.toJSONString();
        } catch (Exception e) {
            logger.error("Error creating group", e);
            return null;
        }
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
        logger.info("Params: {}", params);
        out.write(params.getBytes());
        out.flush();

        InputStream is;
        try {
            is = httpURLConnection.getInputStream();
        } catch (Exception e) {
            is = httpURLConnection.getErrorStream();
            throw new Exception("Request Error: " + httpURLConnection.getResponseMessage() + readAllBytes(is));
        }
        return readAllBytes(is);
    }

    public String buildRequetUrl() {
        try {
            URL url = new URL(requestUrl.replace("ws://", "http://").replace("wss://", "https://"));
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());
            String host = url.getHost();

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
            throw new RuntimeException("Error building request URL: " + e.getMessage());
        }
    }

    private String buildParam() {
        return "{\n" +
                "    \"header\": {\n" +
                "        \"app_id\": \"" + APPID + "\",\n" +
                "        \"status\": 3\n" +
                "    },\n" +
                "    \"parameter\": {\n" +
                "        \"s782b4996\": {\n" +
                "            \"func\": \"createGroup\",\n" +
                "            \"groupId\": \"iFLYTEK_examples_groupId\",\n" +
                "            \"groupName\": \"iFLYTEK_examples_groupName\",\n" +
                "            \"groupInfo\": \"iFLYTEK_examples_groupInfo\",\n" +
                "            \"createGroupRes\": {\n" +
                "                \"encoding\": \"utf8\",\n" +
                "                \"compress\": \"raw\",\n" +
                "                \"format\": \"json\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
    }

    private String readAllBytes(InputStream is) throws IOException {
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        int len;
        while ((len = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, len, "UTF-8"));
        }
        return sb.toString();
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
        public CreateGroupRes createGroupRes;
    }

    class CreateGroupRes {
        public String compress;
        public String encoding;
        public String format;
        public String text;
    }
}
