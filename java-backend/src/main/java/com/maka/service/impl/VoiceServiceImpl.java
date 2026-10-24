package com.maka.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maka.config.XfyunConfig;
import com.maka.service.VoiceService;
import com.maka.service.TaskService;
import com.maka.support.RoleAccess;
import com.maka.support.TaskAccess;
import com.maka.vo.SearchFeatureResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.servlet.http.HttpSession;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** Bounded calls to the configured XFYun voiceprint API version. */
@Service
public class VoiceServiceImpl implements VoiceService {
    public static final long MAX_UPLOAD_BYTES = 20L * 1024 * 1024;
    private final XfyunConfig config;
    @Autowired private RoleAccess roles;
    @Autowired private TaskAccess taskAccess;
    @Autowired private TaskService tasks;
    @Autowired private JdbcTemplate db;
    @Autowired private PlatformTransactionManager transactions;

    @Value("${VOICE_GROUP_ID:guitu_demo_20260908}")
    private String configuredGroupId = "guitu_demo_20260908";

    @Value("${FFMPEG_BIN:ffmpeg}")
    private String ffmpeg = "ffmpeg";

    @Autowired
    public VoiceServiceImpl(XfyunConfig config) {
        this.config = config;
    }

    public String getGroupId() {
        if (configuredGroupId == null || !configuredGroupId.matches("[A-Za-z0-9_]{1,32}")) {
            throw new VoiceOperationException(503, "VOICE_CONFIG", "声纹库配置无效，请联系管理员");
        }
        return configuredGroupId;
    }

    public void authorizeConfiguration(HttpSession session) {
        roles.current(session);
    }

    /** Legacy entry points cannot enroll arbitrary identifiers or search the whole library. */
    @Override
    public String createGroup() {
        throw taskRequired();
    }

    public String createGroup(HttpSession session) {
        roles.requireRole(session, "admin");
        JSONObject options = new JSONObject();
        options.put("groupId", getGroupId());
        options.put("groupName", "暖寻灯塔演示声纹库");
        options.put("groupInfo", "暖寻灯塔当前部署声纹库");
        JSONObject result = call("createGroup", "createGroupRes", options, null);
        if (!getGroupId().equals(result.getString("groupId"))) {
            throw invalidResponse();
        }
        return result.toJSONString();
    }

    @Override
    public String createFeature(String groupId, String featureId, String featureInfo, MultipartFile audioFile) {
        throw taskRequired();
    }

    @Override
    public SearchFeatureResult searchFeature(String groupId, int topK, MultipartFile audioFile) {
        throw taskRequired();
    }

    private static VoiceOperationException taskRequired() {
        return new VoiceOperationException(403, "VOICE_TASK_REQUIRED", "请通过当前任务的声纹入口操作");
    }

    static String taskFeatureId(Integer taskId) {
        if (taskId == null || taskId <= 0) throw new IllegalArgumentException("请选择有效任务");
        return "task_" + taskId;
    }

    private Map<String,Object> enrollmentAccess(Integer taskId, HttpSession session) {
        taskFeatureId(taskId);
        Map<String,Object> user = roles.requireRole(session, "family", "admin");
        if ("family".equals(user.get("userType"))
                && !tasks.isTaskBelongsToFamily(taskId, String.valueOf(user.get("uuid")))) {
            throw new VoiceOperationException(403, "VOICE_TASK_FORBIDDEN", "只能为自己的求助任务录入声纹");
        }
        if (!taskAccess.isActive(taskId)) {
            throw new VoiceOperationException(409, "VOICE_TASK_CLOSED", "任务不存在或已结束，不能录入声纹");
        }
        return user;
    }

    private Map<String,Object> enrollment(Integer taskId) {
        List<Map<String,Object>> rows = db.queryForList(
                "SELECT group_id,feature_id FROM task_voice_features WHERE task_id=?", taskId);
        if (rows.isEmpty()) return null;
        Map<String,Object> row = rows.get(0);
        validateGroup(String.valueOf(row.get("group_id")));
        if (!taskFeatureId(taskId).equals(row.get("feature_id"))) throw invalidResponse();
        return row;
    }

    /** The provider receives only a server-owned task identifier, never browser metadata. */
    public String createFeature(Integer taskId, MultipartFile audioFile, HttpSession session) {
        Map<String,Object> user = enrollmentAccess(taskId, session);
        Map<String,Object> existing = enrollment(taskId);
        String selected = existing == null ? getGroupId() : String.valueOf(existing.get("group_id"));
        String featureId = taskFeatureId(taskId);
        byte[] audio = normalizeAudio(audioFile);
        enrollmentAccess(taskId, session);
        // Persist intent before the external mutation. A crash or a concurrent task close
        // must not strand an untracked voiceprint. The delay covers bounded provider calls.
        db.update("INSERT INTO voice_cleanup_queue(task_id,group_id,next_attempt_at) "
                + "VALUES(?,?,DATE_ADD(CURRENT_TIMESTAMP,INTERVAL 5 MINUTE)) "
                + "ON DUPLICATE KEY UPDATE group_id=VALUES(group_id),next_attempt_at=VALUES(next_attempt_at)",
                taskId, selected);
        JSONObject options = new JSONObject();
        options.put("groupId", selected);
        options.put("featureId", featureId);
        options.put("featureInfo", "task:" + taskId);
        options.put("cover", true);
        if (existing == null) {
            try {
                JSONObject result = call("createFeature", "createFeatureRes", options, audio);
                if (!featureId.equals(result.getString("featureId"))) throw invalidResponse();
            } catch (VoiceProviderException error) {
                // An earlier timed-out create can have succeeded. Only positive evidence
                // for this exact feature permits an update; a truncated list proves nothing.
                if (error.providerCode != 23005 || !listedFeature(selected, featureId)) throw error;
                updateFeature(options, audio);
            }
        } else {
            updateFeature(options, audio);
        }
        Boolean active = new TransactionTemplate(transactions).execute(status -> {
            List<Map<String,Object>> rows = db.queryForList("SELECT status FROM tasks WHERE id=? FOR UPDATE", taskId);
            if (rows.isEmpty() || !Set.of("waiting", "rescuing", "confirming").contains(rows.get(0).get("status"))) {
                db.update("INSERT INTO voice_cleanup_queue(task_id,group_id) VALUES(?,?) ON DUPLICATE KEY UPDATE "
                        + "group_id=VALUES(group_id),next_attempt_at=CURRENT_TIMESTAMP", taskId, selected);
                return false;
            }
            db.update("INSERT INTO task_voice_features(task_id,group_id,feature_id,created_by) VALUES(?,?,?,?) "
                    + "ON DUPLICATE KEY UPDATE group_id=VALUES(group_id),feature_id=VALUES(feature_id),created_by=VALUES(created_by)",
                    taskId, selected, featureId, user.get("uuid"));
            db.update("DELETE FROM voice_cleanup_queue WHERE task_id=? AND group_id=?", taskId, selected);
            return true;
        });
        if (!Boolean.TRUE.equals(active)) {
            throw new VoiceOperationException(409, "VOICE_TASK_CLOSED", "任务已结束，刚录入的声纹已进入清理队列");
        }
        JSONObject result = new JSONObject();
        result.put("featureId", featureId);
        return result.toJSONString();
    }

    private void updateFeature(JSONObject options, byte[] audio) {
        JSONObject result = call("updateFeature", "updateFeatureRes", options, audio);
        if (!"success".equals(result.getString("msg"))) throw invalidResponse();
    }

    public VoiceMatchResult searchFeature(Integer taskId, MultipartFile audioFile, HttpSession session) {
        taskFeatureId(taskId);
        taskAccess.requireRecognition(taskId, session);
        Map<String,Object> existing = enrollment(taskId);
        if (existing == null) return new VoiceMatchResult(false, 0, "", "");
        JSONObject options = new JSONObject();
        options.put("groupId", existing.get("group_id"));
        options.put("dstFeatureId", taskFeatureId(taskId));
        JSONObject result = call("searchScoreFea", "searchScoreFeaRes", options, normalizeAudio(audioFile));
        Object rawScore = result.get("score");
        if (!(rawScore instanceof Number) || !taskFeatureId(taskId).equals(result.getString("featureId"))) throw invalidResponse();
        double score = ((Number) rawScore).doubleValue();
        if (!Double.isFinite(score) || score < -1 || score > 1) throw invalidResponse();
        taskAccess.requireRecognition(taskId, session);
        return new VoiceMatchResult(true, score, "当前任务声纹", taskFeatureId(taskId));
    }

    private static void validateGroup(String groupId) {
        if (groupId == null || !groupId.matches("[A-Za-z0-9_]{1,32}")) throw invalidResponse();
    }

    private boolean listedFeature(String groupId, String featureId) {
        JSONObject query = new JSONObject();
        query.put("groupId", groupId);
        Object result = callProvider("queryFeatureList", "queryFeatureListRes", query, null);
        if (!(result instanceof JSONArray)) throw invalidResponse();
        for (Object item : (JSONArray) result) {
            if (!(item instanceof JSONObject)) throw invalidResponse();
            if (featureId.equals(((JSONObject) item).getString("featureId"))) return true;
        }
        return false;
    }

    /** Worker-only cleanup targets one task feature, never the shared group. */
    void deleteTaskFeature(Integer taskId, String groupId) {
        validateGroup(groupId);
        JSONObject options = new JSONObject();
        options.put("groupId", groupId);
        options.put("featureId", taskFeatureId(taskId));
        try {
            JSONObject result = call("deleteFeature", "deleteFeatureRes", options, null);
            if ("success".equals(result.getString("msg"))) return;
            throw invalidResponse();
        } catch (VoiceOperationException deletionError) {
            JSONObject query = new JSONObject();
            query.put("groupId", groupId);
            try {
                callProvider("queryFeatureList", "queryFeatureListRes", query, null);
            } catch (VoiceProviderException queryError) {
                String prefix = "failed to query feature list detail: failed to query feature list detail:  groupId: " + groupId + ", ";
                if (queryError.providerCode == 23009 && ( (prefix + "this groupId is empty").equals(queryError.providerMessage)
                        || (prefix + "this group does not exist").equals(queryError.providerMessage))) return;
                throw queryError;
            }
            // queryFeatureList can be truncated; absence in a successful list is not proof.
            throw deletionError;
        }
    }

    /** An empty library needs a second, read-only confirmation from the provider. */
    private JSONObject search(JSONObject options, byte[] audio) {
        try {
            return call("searchFea", "searchFeaRes", options, audio);
        } catch (VoiceProviderException searchError) {
            String selected = getGroupId();
            String missingFeatures = "failed to search topk Feature detail: failed to search topK nearest feature detail: "
                    + "the feature db does not have feature with groupId=" + selected + ".";
            if (searchError.providerCode != 23008 || !missingFeatures.equals(searchError.providerMessage)) {
                throw searchError;
            }
            JSONObject query = new JSONObject();
            query.put("groupId", selected);
            try {
                callProvider("queryFeatureList", "queryFeatureListRes", query, null);
            } catch (VoiceProviderException queryError) {
                String emptyGroup = "failed to query feature list detail: failed to query feature list detail:  "
                        + "groupId: " + selected + ", this groupId is empty";
                if (queryError.providerCode == 23009 && emptyGroup.equals(queryError.providerMessage)) {
                    JSONObject empty = new JSONObject();
                    empty.put("scoreList", new JSONArray());
                    return empty;
                }
                throw queryError;
            }
            // A successful query does not prove the specific empty-group condition.
            throw searchError;
        }
    }

    /** New API uses 16 kHz mono PCM WAV; the legacy API requires MP3. */
    protected byte[] normalizeAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择包含清晰人声的音频文件");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("音频文件不能超过 20 MB");
        }
        Path input = null;
        Path output = null;
        Process conversion = null;
        try {
            boolean legacy = "s782b4996".equals(serviceId());
            input = Files.createTempFile("guitu-voice-", ".input");
            output = Files.createTempFile("guitu-voice-", legacy ? ".mp3" : ".wav");
            try (InputStream source = file.getInputStream()) {
                Files.copy(source, input, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            conversion = new ProcessBuilder(ffmpeg, "-nostdin", "-v", "error", "-y",
                    "-protocol_whitelist", "file,pipe", "-i", input.toString(),
                    "-vn", "-ac", "1", "-ar", "16000", "-codec:a", legacy ? "libmp3lame" : "pcm_s16le",
                    "-b:a", "32k", output.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if (!conversion.waitFor(20, TimeUnit.SECONDS)) {
                conversion.destroyForcibly();
                throw new IllegalArgumentException("音频转换超时，请使用较短且清晰的录音");
            }
            if (conversion.exitValue() != 0 || Files.size(output) < (legacy ? 1000 : 16044)) {
                throw new IllegalArgumentException("音频无法解码或内容太短，请使用 3 至 5 秒清晰录音");
            }
            if (Files.size(output) > 3L * 1024 * 1024) {
                throw new IllegalArgumentException("转换后的音频过长，请使用较短录音");
            }
            return Files.readAllBytes(output);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new VoiceOperationException(503, "VOICE_BUSY", "声纹处理被中断，请稍后重试");
        } catch (java.io.IOException error) {
            throw new VoiceOperationException(503, "VOICE_AUDIO", "音频处理服务暂时不可用，请稍后重试");
        } finally {
            if (conversion != null && conversion.isAlive()) {
                conversion.destroyForcibly();
            }
            deleteTemp(input);
            deleteTemp(output);
        }
    }

    private static void deleteTemp(Path path) {
        if (path != null) {
            try { Files.deleteIfExists(path); } catch (java.io.IOException ignored) { }
        }
    }

    /** Separately validate transport status, provider status, and decoded result. */
    private JSONObject call(String operation, String resultKey, JSONObject options, byte[] audio) {
        Object decoded = callProvider(operation, resultKey, options, audio);
        if (!(decoded instanceof JSONObject)) { throw invalidResponse(); }
        return (JSONObject) decoded;
    }

    private Object callProvider(String operation, String resultKey, JSONObject options, byte[] audio) {
        if (blank(config.getAppId()) || blank(config.getApiKey()) || blank(config.getApiSecret())) {
            throw new VoiceOperationException(503, "VOICE_CONFIG", "声纹服务尚未配置，请联系管理员");
        }
        HttpURLConnection connection = null;
        try {
            options = new JSONObject(new java.util.LinkedHashMap<>(options));
            JSONObject format = new JSONObject();
            format.put("encoding", "utf8");
            format.put("compress", "raw");
            format.put("format", "json");
            options.put("func", operation);
            options.put(resultKey, format);
            JSONObject parameter = new JSONObject();
            parameter.put(serviceId(), options);
            JSONObject header = new JSONObject();
            header.put("app_id", config.getAppId());
            header.put("status", 3);
            JSONObject request = new JSONObject();
            request.put("header", header);
            request.put("parameter", parameter);
            if (audio != null) {
                JSONObject resource = new JSONObject();
                resource.put("encoding", "s782b4996".equals(serviceId()) ? "lame" : "raw");
                resource.put("sample_rate", 16000);
                resource.put("channels", 1);
                resource.put("bit_depth", 16);
                resource.put("status", 3);
                resource.put("audio", Base64.getEncoder().encodeToString(audio));
                JSONObject payload = new JSONObject();
                payload.put("resource", resource);
                request.put("payload", payload);
            }
            byte[] body = request.toJSONString().getBytes(StandardCharsets.UTF_8);
            connection = (HttpURLConnection) signedUrl().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream target = connection.getOutputStream()) { target.write(body); }
            int status = connection.getResponseCode();
            JSONObject response;
            InputStream responseStream = status == 200 ? connection.getInputStream() : connection.getErrorStream();
            if (responseStream == null) {
                throw new VoiceOperationException(503, "VOICE_HTTP_" + status,
                        "声纹服务连接失败，请稍后重试");
            }
            try (InputStream source = responseStream) {
                byte[] bytes = source.readNBytes(1024 * 1024 + 1);
                if (bytes.length > 1024 * 1024) { throw invalidResponse(); }
                response = JSON.parseObject(new String(bytes, StandardCharsets.UTF_8));
            }
            JSONObject returnedHeader = response == null ? null : response.getJSONObject("header");
            if (returnedHeader == null || !(returnedHeader.get("code") instanceof Number)) {
                throw invalidResponse();
            }
            int code = returnedHeader.getIntValue("code");
            if (code != 0) {
                String message = code == 23998
                        ? "讯飞声纹服务未开通或授权已失效，请联系管理员开通声纹能力"
                        : code == 23005
                        ? "声纹注册失败，请检查声纹库是否已初始化、编号是否重复以及录音是否清晰"
                        : "声纹服务暂时不可用，请联系管理员检查服务状态或录音内容";
                throw new VoiceProviderException(code, returnedHeader.getString("message"), message);
            }
            if (status != 200) {
                throw new VoiceOperationException(503, "VOICE_HTTP_" + status,
                        "声纹服务连接失败，请稍后重试");
            }
            JSONObject payload = response.getJSONObject("payload");
            JSONObject result = payload == null ? null : payload.getJSONObject(resultKey);
            String encoded = result == null ? null : result.getString("text");
            if (blank(encoded)) { throw invalidResponse(); }
            Object decoded = JSON.parse(new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8));
            if (!(decoded instanceof JSONObject) && !(decoded instanceof JSONArray)) { throw invalidResponse(); }
            return decoded;
        } catch (VoiceOperationException error) {
            throw error;
        } catch (Exception error) {
            // No signed URLs, credentials, audio, or raw provider payloads in logs.
            throw new VoiceOperationException(503, "VOICE_UNAVAILABLE", "声纹服务暂时不可用，请稍后重试");
        } finally {
            if (connection != null) { connection.disconnect(); }
        }
    }

    private URL signedUrl() throws Exception {
        URL endpoint = new URL(config.getRequestUrl());
        serviceId();
        String host = endpoint.getHost();
        // URL.getPort() is -1 for ordinary HTTPS URLs; never sign a fictitious :-1.
        if (endpoint.getPort() > 0 && endpoint.getPort() != 443) {
            host += ":" + endpoint.getPort();
        }
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        String source = "host: " + host + "\ndate: " + date + "\nPOST " + endpoint.getPath() + " HTTP/1.1";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(config.getApiSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getEncoder().encodeToString(mac.doFinal(source.getBytes(StandardCharsets.UTF_8)));
        String authorization = "api_key=\"" + config.getApiKey()
                + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"" + signature + "\"";
        String encoded = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));
        return new URL(endpoint.toString() + "?authorization=" + URLEncoder.encode(encoded, "UTF-8")
                + "&host=" + URLEncoder.encode(host, "UTF-8") + "&date=" + URLEncoder.encode(date, "UTF-8"));
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private String serviceId() {
        try {
            URL endpoint = new URL(config.getRequestUrl());
            if (!"https".equalsIgnoreCase(endpoint.getProtocol())
                    || !"api.xf-yun.com".equalsIgnoreCase(endpoint.getHost())
                    || endpoint.getQuery() != null || endpoint.getRef() != null
                    || endpoint.getUserInfo() != null
                    || (endpoint.getPort() != -1 && endpoint.getPort() != 443)) {
                throw invalidResponse();
            }
            if ("/v1/private/s782b4996".equals(endpoint.getPath())) { return "s782b4996"; }
            if ("/v1/private/s1aa729d0".equals(endpoint.getPath())) { return "s1aa729d0"; }
        } catch (Exception ignored) { }
        throw new VoiceOperationException(503, "VOICE_CONFIG", "声纹服务地址配置无效");
    }

    private static VoiceOperationException invalidResponse() {
        return new VoiceOperationException(502, "VOICE_RESPONSE", "声纹服务返回了无效结果，请稍后重试");
    }

    public static class VoiceMatchResult extends SearchFeatureResult {
        private final boolean hasResult;
        private final String featureId;
        VoiceMatchResult(boolean hasResult, double score, String info, String featureId) {
            super(score, info == null ? "" : info);
            this.hasResult = hasResult;
            this.featureId = featureId;
        }
        public boolean hasResult() { return hasResult; }
        public String getFeatureId() { return featureId; }
    }

    public static class VoiceOperationException extends RuntimeException {
        private final int status;
        private final String code;
        public VoiceOperationException(int status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }
        public int getStatus() { return status; }
        public String getCode() { return code; }
    }

    /** Retain provider details only for internal classification, never in public errors. */
    private static class VoiceProviderException extends VoiceOperationException {
        private final int providerCode;
        private final String providerMessage;
        VoiceProviderException(int providerCode, String providerMessage, String publicMessage) {
            super(503, "VOICE_PROVIDER_" + providerCode, publicMessage);
            this.providerCode = providerCode;
            this.providerMessage = providerMessage;
        }
    }
}
