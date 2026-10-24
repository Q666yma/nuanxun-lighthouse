-- Task-specific family contact snapshots. Existing tasks deliberately remain unfilled.
-- This table is not joined into task lists, AI inputs, posters or public maps.
CREATE TABLE IF NOT EXISTS task_family_contacts (
  task_id INT PRIMARY KEY,
  contact_name VARCHAR(100) NOT NULL,
  relationship VARCHAR(60) NOT NULL,
  contact_phone VARCHAR(40) NOT NULL DEFAULT '',
  bound_email VARCHAR(320) NOT NULL DEFAULT '',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
