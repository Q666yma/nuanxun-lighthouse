package com.maka.service.impl;

import com.baidu.aip.face.AipFace;
import com.maka.mapper.TaskMapper;
import com.maka.pojo.Task;
import com.maka.service.FaceComparisonService;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;

@Service
public class FaceComparisonServiceImpl implements FaceComparisonService {
    @Value("${BAIDU_FACE_APP_ID:}") private String appId;
    @Value("${BAIDU_FACE_API_KEY:}") private String apiKey;
    @Value("${BAIDU_FACE_SECRET_KEY:}") private String secretKey;
    @Value("${BAIDU_FACE_GROUP_ID:guitu_demo_20260908}") private String groupId;
    @Autowired public TaskMapper taskMapper;
    private AipFace client;
    private boolean groupReady;

    private synchronized void ensureGroup() {
        if (groupReady) return;
        JSONObject response = client().groupAdd(groupId, new HashMap<>());
        if (response.optInt("error_code", -1) != 223101) requireSuccess(response);
        groupReady = true;
    }

    private synchronized AipFace client() {
        if (client == null) {
            if (appId.isEmpty() || apiKey.isEmpty() || secretKey.isEmpty())
                throw new IllegalStateException("人脸服务尚未配置");
            client = new AipFace(appId, apiKey, secretKey);
            client.setConnectionTimeoutInMillis(5000);
            client.setSocketTimeoutInMillis(15000);
        }
        return client;
    }

    private String image(MultipartFile file) throws Exception {
        if (file.isEmpty() || file.getSize() > 1500000)
            throw new IllegalArgumentException("请选择不超过 1.5 MB 的清晰人脸照片");
        return Base64.getEncoder().encodeToString(file.getBytes());
    }

    private void requireSuccess(JSONObject result) {
        if (result.optInt("error_code", -1) != 0) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Baidu face request failed: provider_code={}", result.optInt("error_code", -1));
            throw new IllegalStateException("人脸服务暂时不可用（代码 " + result.optInt("error_code", -1) + "）");
        }
    }

    @Override public boolean checkQuality(MultipartFile file) throws Exception {
        HashMap<String,Object> options = new HashMap<>();
        options.put("face_field", "quality");
        options.put("max_face_num", 1);
        JSONObject result = client().detect(image(file), "BASE64", options);
        int code = result.optInt("error_code", -1);
        if (code == 222202 || code == 222203 || code == 222210) return false;
        requireSuccess(result);
        JSONArray faces = result.getJSONObject("result").getJSONArray("face_list");
        return faces.length() > 0 && passesQuality(faces.getJSONObject(0));
    }

    // Baidu's documented detect/quality recommendations; detection does not enroll a face.
    // https://cloud.baidu.com/doc/FACE/s/yk37c1u4t
    public static boolean passesQuality(JSONObject face) {
        JSONObject quality = face.optJSONObject("quality");
        if (quality == null) return false;
        double probability = face.optDouble("face_probability", Double.NaN);
        double blur = quality.optDouble("blur", Double.NaN);
        double illumination = quality.optDouble("illumination", Double.NaN);
        if (!Double.isFinite(probability) || probability < .8 || probability > 1 ||
            !Double.isFinite(blur) || blur < 0 || blur >= .7 ||
            !Double.isFinite(illumination) || illumination <= 40 || illumination > 255 ||
            quality.optInt("completeness", 0) != 1) return false;
        JSONObject occlusion = quality.optJSONObject("occlusion");
        if (occlusion == null) return false;
        String[] parts = {"left_eye","right_eye","nose","mouth","left_cheek","right_cheek","chin_contour"};
        double[] limits = {.6,.6,.7,.7,.8,.8,.6};
        for (int i=0; i<parts.length; i++) {
            double value = occlusion.optDouble(parts[i], Double.NaN);
            if (!Double.isFinite(value) || value < 0 || value >= limits[i]) return false;
        }
        return true;
    }

    @Override public void addFace(MultipartFile file, String userId, String userName) throws Exception {
        String encoded = image(file);
        ensureGroup();
        HashMap<String,String> options = new HashMap<>();
        options.put("action_type", "APPEND");
        options.put("quality_control", "NORMAL");
        options.put("user_info", userName);
        JSONObject response = client().addUser(encoded, "BASE64", groupId, userId, options);
        if (response.optInt("error_code", -1) == 223105)
            requireSuccess(client().getUser(userId, groupId, new HashMap<>()));
        else requireSuccess(response);
    }

    @Override public void deleteUser(String userId) throws Exception {
        // The SDK delete method takes group first, unlike getUser(user, group).
        JSONObject response = client().deleteUser(groupId, userId, new HashMap<>());
        int code = response.optInt("error_code", -1);
        if (code != 223100 && code != 223103) requireSuccess(response);
    }

    private JSONObject search(String encoded, Integer selectedTaskId) {
        return search(encoded, "BASE64", selectedTaskId);
    }

    private JSONObject search(String image, String imageType, Integer selectedTaskId) {
        HashMap<String,Object> options = new HashMap<>();
        if (selectedTaskId == null || selectedTaskId < 1) throw new IllegalArgumentException("请选择参与的任务");
        // Upstream scope: https://cloud.baidu.com/doc/FACE/s/Gk37c1uzc
        options.put("user_id",selectedTaskId.toString());
        options.put("match_threshold", "80");
        options.put("max_user_num", 5);
        JSONObject response = client().search(image, imageType, groupId, options);
        int code = response.optInt("error_code", -1);
        if (code == 222207 || code == 222202 || code == 223100 || code == 223103)
            return new JSONObject().put("lostPerson", "无");
        requireSuccess(response);
        JSONArray matches = response.getJSONObject("result").getJSONArray("user_list");
        for (int i=0; i<matches.length(); i++) {
            JSONObject match = matches.getJSONObject(i);
            int taskId;
            try { taskId = Integer.parseInt(match.optString("user_id", "")); }
            catch (NumberFormatException ignored) { continue; }
            if (taskId != selectedTaskId) continue;
            Object rawScore = match.opt("score");
            if (!(rawScore instanceof Number)) throw new IllegalStateException("人脸服务返回了无效分数");
            double score = ((Number) rawScore).doubleValue();
            if (!Double.isFinite(score) || score < 0 || score > 100)
                throw new IllegalStateException("人脸服务返回了无效分数");
            Task task = taskMapper.selectById(taskId);
            if (task == null || !java.util.Set.of("waiting","rescuing","confirming").contains(task.getStatus()) || score < 80) continue;
            return new JSONObject().put("lostPerson", task.getElderName())
                .put("similarity", score).put("img", task.getPhotoUrl())
                .put("taskId", taskId).put("requiresConfirmation", true);
        }
        return new JSONObject().put("lostPerson", "无");
    }

    @Override public String compareFace(MultipartFile file) throws Exception {
        throw new IllegalArgumentException("必须指定获授权的任务");
    }
    public String compareFace(MultipartFile file,Integer taskId) throws Exception {
        return search(image(file),taskId).toString();
    }

    @Override public String compareFaceFromVideo(MultipartFile file) throws Exception {
        throw new IllegalArgumentException("必须指定获授权的任务");
    }

    protected interface VideoFrames extends AutoCloseable {
        long durationMicros();
        long timestampMicros();
        BufferedImage frameAt(long timestamp) throws Exception;
    }

    protected VideoFrames openVideo(MultipartFile file) throws Exception {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file.getInputStream());
        Java2DFrameConverter converter = new Java2DFrameConverter();
        grabber.setTimeout(15000);
        try { grabber.start(); }
        catch (Exception failure) { try { grabber.close(); } finally { converter.close(); } throw failure; }
        return new VideoFrames() {
            public long durationMicros() { return grabber.getLengthInTime(); }
            public long timestampMicros() { return grabber.getTimestamp(); }
            public BufferedImage frameAt(long timestamp) throws Exception {
                grabber.setTimestamp(timestamp);
                Frame frame = grabber.grabImage();
                return frame == null || frame.image == null ? null : converter.convert(frame);
            }
            public void close() throws Exception { try { grabber.close(); } finally { converter.close(); } }
        };
    }

    private static void checkVideoDeadline(long deadline) {
        if (System.nanoTime() >= deadline)
            throw new IllegalStateException("视频分析超时，请截取更短的视频后重试");
    }

    private static String videoImage(BufferedImage picture) throws Exception {
        // Keep aspect ratio and fit the detect API's image and Base64 size limits.
        double scale = Math.min(1, Math.min(1280.0 / picture.getWidth(), 960.0 / picture.getHeight()));
        BufferedImage image = new BufferedImage(Math.max(1, (int)(picture.getWidth() * scale)),
            Math.max(1, (int)(picture.getHeight() * scale)), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(picture, 0, 0, image.getWidth(), image.getHeight(), null);
        } finally { graphics.dispose(); }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "jpg", buffer) || buffer.size() > 1500000) return null;
        return Base64.getEncoder().encodeToString(buffer.toByteArray());
    }

    public String compareFaceFromVideo(MultipartFile file,Integer taskId) throws Exception {
        if (file.isEmpty() || file.getSize() > 30L*1024*1024)
            throw new IllegalArgumentException("请选择不超过 30 MB 的视频");
        if (taskId == null || taskId < 1) throw new IllegalArgumentException("请选择参与的任务");
        client();
        long deadline = System.nanoTime() + 60_000_000_000L;
        String bestToken = null, bestImage = null;
        double bestBlur = Double.POSITIVE_INFINITY, bestSecond = 0;
        int checked = 0, qualified = 0;
        JSONArray sampledSeconds = new JSONArray();
        JSONObject analysis = new JSONObject().put("method", "quality_blur").put("maxFacesPerSample", 10);
        HashMap<String,Object> options = new HashMap<>();
        options.put("face_field", "quality");
        options.put("max_face_num", 10);
        try (VideoFrames video = openVideo(file)) {
            long length = video.durationMicros();
            if (length <= 0) throw new IllegalArgumentException("无法读取视频时长，请转换为常见 MP4 格式后重试");
            int planned = (int)Math.min(6L, 1 + length / 1_000_000L);
            long last = Math.max(0, length - Math.min(100_000L, length / 2));
            analysis.put("videoDurationSeconds", length / 1_000_000.0).put("plannedSamples", planned);
            for (int sample = 0; sample < planned; sample++) {
                checkVideoDeadline(deadline);
                long stamp = planned == 1 ? 0 : (long)(last * (sample / (double)(planned - 1)));
                BufferedImage picture = video.frameAt(stamp);
                checkVideoDeadline(deadline);
                if (picture == null) continue;
                double second = video.timestampMicros() / 1_000_000.0;
                sampledSeconds.put(second);
                String encoded = videoImage(picture);
                if (encoded == null) continue;
                checkVideoDeadline(deadline);
                JSONObject detected = client().detect(encoded, "BASE64", options);
                checked++;
                checkVideoDeadline(deadline);
                int code = detected.optInt("error_code", -1);
                if (code == 222202 || code == 222203 || code == 222210) continue;
                requireSuccess(detected);
                JSONArray faces = detected.getJSONObject("result").getJSONArray("face_list");
                for (int i=0; i<Math.min(10, faces.length()); i++) {
                    JSONObject face = faces.getJSONObject(i);
                    if (!passesQuality(face)) continue;
                    double blur = face.getJSONObject("quality").optDouble("blur", Double.NaN);
                    if (!Double.isFinite(blur) || blur < 0 || blur >= .7) continue;
                    String token = face.optString("face_token", "");
                    if (token.isEmpty()) throw new IllegalStateException("人脸服务未返回有效的人脸标识");
                    qualified++;
                    if (blur < bestBlur) {
                        bestBlur = blur; bestToken = token; bestImage = encoded; bestSecond = second;
                    }
                }
            }
        }
        checkVideoDeadline(deadline);
        analysis.put("sampledSeconds", sampledSeconds).put("decodedSamples", sampledSeconds.length())
            .put("qualityCheckedSamples", checked).put("qualifiedFaces", qualified);
        if (bestToken == null)
            return new JSONObject().put("lostPerson", "无").put("code", "NO_QUALIFIED_FACE")
                .put("message", "抽样画面中未检测到质量合格的人脸，请使用正面、光线充足的视频或清晰照片重试")
                .put("videoAnalysis", analysis).toString();
        // Detect face_token is valid for 60 minutes; use it immediately to search exactly
        // the selected face, including when another face occupies more of the same frame.
        // https://cloud.baidu.com/doc/FACE/s/yk37c1u4t and /FACE/s/Gk37c1uzc
        JSONObject result = search(bestToken, "FACE_TOKEN", taskId);
        checkVideoDeadline(deadline);
        return result.put("bestFrame", bestImage).put("bestFrameMime", "image/jpeg")
            .put("videoAnalysis", analysis.put("selectedSecond", bestSecond).put("selectedBlur", bestBlur)).toString();
    }
}
