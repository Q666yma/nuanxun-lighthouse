CREATE TABLE IF NOT EXISTS task_location_sharing (
 task_id INT NOT NULL, user_uuid VARCHAR(36) NOT NULL,
 sharing TINYINT(1) NOT NULL DEFAULT 0,
 updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(task_id,user_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS task_locations (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 task_id INT NOT NULL, user_uuid VARCHAR(36) NOT NULL,
 latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL, accuracy DOUBLE NOT NULL,
 recorded_at DATETIME(3) NOT NULL, received_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE KEY uq_location_sample(task_id,user_uuid,recorded_at),
 KEY ix_location_task_user(task_id,user_uuid,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
