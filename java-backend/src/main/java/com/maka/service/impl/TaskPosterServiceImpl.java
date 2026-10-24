package com.maka.service.impl;

import com.maka.config.AppConfig;
import com.maka.service.TaskPosterService;
//import com.maka.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Service
public class TaskPosterServiceImpl implements TaskPosterService {
    
    @Value("${ai.poster.url:http://localhost:5000/api/generate-html-poster}")
    private String posterApiUrl;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Override
    public Map<String, Object> generateHtmlPoster(
            String elderName, 
            String lostTime, 
            String location, 
            String photoUrl, 
            String extraInfo, 
            Map<String, Object> templateData,
            String contactPhone,   // 联系电话
            String currentTime,    // 当前时间
            String userId) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            
            // 构建请求数据
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("elderName", elderName);
            requestData.put("lostTime", lostTime);
            requestData.put("location", location);
            requestData.put("photoUrl", photoUrl);
            requestData.put("contactPhone", contactPhone);  // 添加联系电话
            requestData.put("currentTime", currentTime);    // 添加当前时间
            
            // 如果有额外信息，添加到请求数据中
            if (extraInfo != null && !extraInfo.isEmpty()) {
                requestData.put("extraInfo", extraInfo);
            }
            
            // 如果有模板数据，添加到请求数据中
            if (templateData != null && !templateData.isEmpty()) {
                requestData.put("templateData", templateData);
            }
            
            // 生成唯一的海报ID
            String posterId = UUID.randomUUID().toString();
            requestData.put("posterId", posterId);
            
            // 调用API生成HTML海报
            Map<String, Object> posterData = callPosterApi(requestData);
            
            if (posterData != null && (Boolean) posterData.get("success")) {
                result.put("success", true);
                result.put("posterHtml", posterData.get("html"));
                result.put("posterCss", posterData.get("css"));
                result.put("posterId", posterId);
            } else {
                result.put("success", false);
                result.put("message", posterData != null ? posterData.get("message") : "生成海报失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "服务器错误: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 调用API生成HTML海报
     */
    private Map<String, Object> callPosterApi(Map<String, Object> requestData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            com.maka.support.RequestCookies.forward(headers);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    posterApiUrl,
                    entity,
                    Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = response.getBody();
                return responseBody;
            } else {
                return Map.of(
                    "success", false,
                    "message", "API调用失败: " + response.getStatusCode()
                );
            }
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "调用API错误: " + e.getMessage()
            );
        }
    }
    
    
}
