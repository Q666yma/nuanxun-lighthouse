package com.maka.service.impl;

import com.maka.service.FaceComparisonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/** Retries external cleanup after the local task-cancellation transaction commits. */
@Component
public class FaceCleanupWorker {
    private static final Logger logger = LoggerFactory.getLogger(FaceCleanupWorker.class);
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private FaceComparisonService faceService;

    /** No @Transactional: the external provider must never hold a database transaction open. */
    @Scheduled(initialDelay = 15000, fixedDelay = 60000)
    public void cleanupPendingFaces() {
        try {
            List<Map<String,Object>> due = jdbcTemplate.queryForList(
                "SELECT task_id, attempts FROM face_cleanup_queue " +
                "WHERE next_attempt_at <= CURRENT_TIMESTAMP " +
                "ORDER BY next_attempt_at, task_id LIMIT 5");
            for (Map<String,Object> entry : due.subList(0, Math.min(5, due.size()))) {
                int taskId = ((Number) entry.get("task_id")).intValue();
                int attempts = ((Number) entry.get("attempts")).intValue() + 1;
                // A conditional lease supports restart/crash recovery and concurrent workers.
                int claimed = jdbcTemplate.update(
                    "UPDATE face_cleanup_queue SET next_attempt_at = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 5 MINUTE), " +
                    "last_attempt_at = CURRENT_TIMESTAMP, attempts = attempts + 1 " +
                    "WHERE task_id = ? AND next_attempt_at <= CURRENT_TIMESTAMP", taskId);
                if (claimed != 1) continue;
                try {
                    // Do not delete an active task's face if identifiers were restored/reused.
                    List<Map<String,Object>> active = jdbcTemplate.queryForList(
                        "SELECT id FROM tasks WHERE id = ? AND status NOT IN ('finished','cancelled')", taskId);
                    if (!active.isEmpty()) {
                        retry(taskId, "ActiveTask", 3600);
                        continue;
                    }
                    faceService.deleteUser(Integer.toString(taskId));
                    jdbcTemplate.update("DELETE FROM face_cleanup_queue WHERE task_id = ?", taskId);
                } catch (Exception error) {
                    String kind = error.getClass().getSimpleName();
                    if (kind.length() > 64) kind = kind.substring(0, 64);
                    retry(taskId, kind, retrySeconds(attempts));
                    logger.warn("Face cleanup pending: task {}, attempt {}, error type {}", taskId, attempts, kind);
                }
            }
        } catch (Exception error) {
            // Provider/JDBC messages may contain request or connection data; never log them.
            logger.warn("Face cleanup worker deferred: {}", error.getClass().getSimpleName());
        }
    }

    private void retry(int taskId, String errorType, int seconds) {
        jdbcTemplate.update("UPDATE face_cleanup_queue SET last_error = ?, " +
                "next_attempt_at = TIMESTAMPADD(SECOND, ?, CURRENT_TIMESTAMP) WHERE task_id = ?",
                errorType, seconds, taskId);
    }

    public static int retrySeconds(int attempts) {
        return Math.min(3600, 60 * (1 << Math.min(6, Math.max(0, attempts - 1))));
    }
}
