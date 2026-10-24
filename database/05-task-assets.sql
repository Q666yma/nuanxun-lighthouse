CREATE TABLE IF NOT EXISTS task_uploads (
    path VARCHAR(255) NOT NULL PRIMARY KEY,
    owner_uuid VARCHAR(36) NOT NULL,
    task_id INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX ix_upload_owner (owner_uuid), INDEX ix_upload_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS task_voice_features (
    task_id INT NOT NULL PRIMARY KEY,
    group_id VARCHAR(32) NOT NULL,
    feature_id VARCHAR(32) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS voice_cleanup_queue (
    task_id INT NOT NULL PRIMARY KEY,
    group_id VARCHAR(32) NOT NULL,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bind surviving task attachments to their existing owner; do not infer ownership for orphan files.
INSERT IGNORE INTO task_uploads(path,owner_uuid,task_id)
SELECT t.photo_url,f.uuid,t.id FROM tasks t JOIN families f
ON JSON_CONTAINS(COALESCE(f.task_ids,JSON_ARRAY()),CAST(t.id AS JSON))
WHERE t.photo_url LIKE '/uploads/%';
INSERT IGNORE INTO task_uploads(path,owner_uuid,task_id)
SELECT t.audio_url,f.uuid,t.id FROM tasks t JOIN families f
ON JSON_CONTAINS(COALESCE(f.task_ids,JSON_ARRAY()),CAST(t.id AS JSON))
WHERE t.audio_url LIKE '/uploads/%';
