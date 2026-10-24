package com.maka.service.impl;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Retries deletion of one ended task's voiceprint without removing a shared library. */
@Component
public class VoiceCleanupWorker {
    private static final Logger logger = LoggerFactory.getLogger(VoiceCleanupWorker.class);
    @Autowired private JdbcTemplate db;
    @Autowired private VoiceServiceImpl voice;

    @Scheduled(initialDelay = 20000, fixedDelay = 60000)
    public void cleanupPendingVoices() {
        try {
            List<Map<String,Object>> due = db.queryForList("SELECT task_id,group_id,attempts FROM voice_cleanup_queue "
                    + "WHERE next_attempt_at<=CURRENT_TIMESTAMP ORDER BY next_attempt_at,task_id LIMIT 5");
            for (Map<String,Object> row : due.subList(0, Math.min(5, due.size()))) {
                int taskId = ((Number)row.get("task_id")).intValue();
                String groupId = String.valueOf(row.get("group_id"));
                int attempt = ((Number)row.get("attempts")).intValue() + 1;
                if (db.update("UPDATE voice_cleanup_queue SET next_attempt_at=DATE_ADD(CURRENT_TIMESTAMP,INTERVAL 5 MINUTE),"
                        + "attempts=attempts+1 WHERE task_id=? AND group_id=? AND next_attempt_at<=CURRENT_TIMESTAMP", taskId, groupId) != 1) continue;
                try {
                    // Active tasks may have a cleanup intent left by a timed-out enrollment.
                    if (!db.queryForList("SELECT id FROM tasks WHERE id=? AND status NOT IN ('finished','cancelled')", taskId).isEmpty()) {
                        retry(taskId, groupId, 60);
                        continue;
                    }
                    voice.deleteTaskFeature(taskId, groupId);
                    db.update("DELETE FROM task_voice_features WHERE task_id=? AND group_id=? AND feature_id=?",
                            taskId, groupId, VoiceServiceImpl.taskFeatureId(taskId));
                    db.update("DELETE FROM voice_cleanup_queue WHERE task_id=? AND group_id=?", taskId, groupId);
                } catch (Exception error) {
                    retry(taskId, groupId, retrySeconds(attempt));
                    logger.warn("Voice cleanup pending: task {}, attempt {}, error type {}", taskId, attempt, error.getClass().getSimpleName());
                }
            }
        } catch (Exception error) {
            logger.warn("Voice cleanup worker deferred: {}", error.getClass().getSimpleName());
        }
    }

    private void retry(int taskId, String groupId, int seconds) {
        db.update("UPDATE voice_cleanup_queue SET next_attempt_at=TIMESTAMPADD(SECOND,?,CURRENT_TIMESTAMP) WHERE task_id=? AND group_id=?",
                seconds, taskId, groupId);
    }

    public static int retrySeconds(int attempts) {
        return Math.min(3600, 60 * (1 << Math.min(6, Math.max(0, attempts - 1))));
    }
}
