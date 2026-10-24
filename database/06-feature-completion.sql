-- Explicit contact sharing and confirmed outcomes; do not infer historical success.
CREATE TABLE IF NOT EXISTS account_contacts (
  uuid VARCHAR(36) PRIMARY KEY,
  contact_phone VARCHAR(40) NOT NULL DEFAULT '',
  contact_email VARCHAR(320) NOT NULL DEFAULT '',
  share_contact TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS task_demographics (
  task_id INT PRIMARY KEY,
  elder_age INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS task_outcomes (
  task_id INT PRIMARY KEY,
  outcome VARCHAR(20) NOT NULL,
  recorded_by VARCHAR(36) NOT NULL,
  recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
