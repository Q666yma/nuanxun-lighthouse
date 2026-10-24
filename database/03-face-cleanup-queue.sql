-- Durable external face cleanup outbox. Apply before deploying the new Java classes.
-- No foreign key: the queue must survive deletion of its associated local task.
-- This stores only task identifiers and retry metadata, never biometric data or secrets.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS face_cleanup_queue (
    task_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INT UNSIGNED NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP NULL DEFAULT NULL,
    last_error VARCHAR(64) DEFAULT NULL,
    PRIMARY KEY (task_id),
    KEY ix_face_cleanup_due (next_attempt_at, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
