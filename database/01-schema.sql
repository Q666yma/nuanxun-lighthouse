-- Reconstructed empty demonstration schema, 2026-09-07.
-- This is NOT a database backup; it contains no historical or sample records.
-- Evidence: 11 source Mapper XML files (byte-for-byte identical to the supplied
-- archive's BOOT-INF/classes/mapper files), Java entities, and Python SQL.
-- See schema-notes.md for inferred types, defaults, and limitations.
-- Target: MySQL 8.0.13+ (the application's FamilyMapper uses JSON_TABLE).
-- Select the intended empty database before running this file.
-- No DROP, TRUNCATE, user/grant changes, database creation, or account seed.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `users` (
  `uuid` VARCHAR(36) NOT NULL,
  `username` VARCHAR(191) DEFAULT NULL,
  `password` VARCHAR(255) DEFAULT NULL,
  `phone` VARCHAR(320) DEFAULT NULL,
  `gender` VARCHAR(32) DEFAULT NULL,
  `user_type` VARCHAR(32) DEFAULT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `status` VARCHAR(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uq_users_username` (`username`),
  KEY `ix_users_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `rescuers` (
  `uuid` VARCHAR(36) NOT NULL,
  `name` VARCHAR(191) DEFAULT NULL,
  `status` VARCHAR(32) DEFAULT NULL,
  `location` TEXT,
  `skills_description` TEXT,
  `skill_tags` JSON DEFAULT (JSON_ARRAY()),
  `task_ids` JSON DEFAULT (JSON_ARRAY()),
  PRIMARY KEY (`uuid`),
  KEY `ix_rescuers_status_name` (`status`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `families` (
  `uuid` VARCHAR(36) NOT NULL,
  `name` VARCHAR(191) DEFAULT NULL,
  `task_ids` JSON DEFAULT (JSON_ARRAY()),
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tasks` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `elder_name` VARCHAR(191) DEFAULT NULL,
  `lost_time` DATETIME DEFAULT NULL,
  `photo_url` TEXT,
  `audio_url` TEXT,
  `location` TEXT,
  `status` VARCHAR(32) DEFAULT NULL,
  `extra_info` LONGTEXT,
  PRIMARY KEY (`id`),
  KEY `ix_tasks_status_id` (`status`, `id`),
  KEY `ix_tasks_lost_time` (`lost_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `task_messages` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `task_id` INT DEFAULT NULL,
  `message_data` LONGTEXT,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_task_messages_task_id_id` (`task_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `map_markers` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `task_id` INT DEFAULT NULL,
  `marker_type` VARCHAR(32) DEFAULT NULL,
  `geometry` LONGTEXT,
  `properties` LONGTEXT,
  `created_by` VARCHAR(36) DEFAULT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_map_markers_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `skill_tags` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `tag_name` VARCHAR(191) DEFAULT NULL,
  `tag_category` VARCHAR(191) DEFAULT NULL,
  `description` TEXT,
  PRIMARY KEY (`id`),
  KEY `ix_skill_tags_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Legacy mapper tables are retained so surviving routes can query empty results.
CREATE TABLE IF NOT EXISTS `table_admin` (
  `admin_id` INT NOT NULL AUTO_INCREMENT,
  `admin_name` VARCHAR(191) DEFAULT NULL,
  `admin_password` VARCHAR(255) DEFAULT NULL,
  `admin_phone` VARCHAR(320) DEFAULT NULL,
  PRIMARY KEY (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tb_applyform` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `my_name` VARCHAR(191) DEFAULT NULL,
  `tel` VARCHAR(320) DEFAULT NULL,
  `old_name` VARCHAR(191) DEFAULT NULL,
  `old_gender` VARCHAR(32) DEFAULT NULL,
  `old_age` INT DEFAULT NULL,
  `lost_time` DATETIME DEFAULT NULL,
  `iq` TEXT,
  `health` TEXT,
  `clothing` TEXT,
  `lost_place` TEXT,
  `oldman_img` TEXT,
  `status` INT DEFAULT NULL,
  `open_id` VARCHAR(255) DEFAULT NULL,
  `old_desc` LONGTEXT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tb_message` (
  `message_id` INT NOT NULL AUTO_INCREMENT,
  `sender_id` INT DEFAULT NULL,
  `message_content` LONGTEXT,
  `message_img` TEXT,
  `message_time` DATETIME DEFAULT NULL,
  `message_type` BOOLEAN DEFAULT NULL,
  `oldMan_id` INT DEFAULT NULL,
  PRIMARY KEY (`message_id`),
  KEY `ix_tb_message_oldman_time` (`oldMan_id`, `message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Only these three tb_rescue_experience columns are evidenced by surviving SQL.
-- The complete historical table cannot be reconstructed from the supplied files.
CREATE TABLE IF NOT EXISTS `tb_rescue_experience` (
  `rescue_experience_id` INT NOT NULL AUTO_INCREMENT,
  `rescue_id` INT DEFAULT NULL,
  `confirm` BOOLEAN DEFAULT NULL,
  PRIMARY KEY (`rescue_experience_id`),
  KEY `ix_tb_rescue_experience_rescue_confirm` (`rescue_id`, `confirm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- oldMan_id is explicit SQL; other column spellings follow Clue.java and the
-- enabled MyBatis underscore-to-camel convention. Historical DDL is unavailable.
CREATE TABLE IF NOT EXISTS `tb_clue` (
  `clue_id` INT NOT NULL AUTO_INCREMENT,
  `oldMan_id` INT DEFAULT NULL,
  `user_id` INT DEFAULT NULL,
  `clue_place` TEXT,
  `clue_date` VARCHAR(255) DEFAULT NULL,
  `clue_find_old_man` INT DEFAULT NULL,
  `clue_appear` INT DEFAULT NULL,
  `upload_time` DATETIME DEFAULT NULL,
  `desc` LONGTEXT,
  PRIMARY KEY (`clue_id`),
  KEY `ix_tb_clue_oldman_id` (`oldMan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
