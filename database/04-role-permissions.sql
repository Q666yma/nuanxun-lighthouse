-- Three fixed roles and server-side notifications/content. Existing approved
-- rescuers remain available; no pending/visitor rescuer is approved here.
SET NAMES utf8mb4;
UPDATE users SET status='active' WHERE status IS NULL OR status='';
ALTER TABLE users ALTER COLUMN status SET DEFAULT 'active';
CREATE TABLE IF NOT EXISTS account_profiles (
  uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(191) NOT NULL DEFAULT '', location TEXT,
  auth_version INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS admin_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, actor_uuid VARCHAR(36) NOT NULL,
  action VARCHAR(64) NOT NULL, target_type VARCHAR(32) NOT NULL,
  target_id VARCHAR(100) NOT NULL, reason TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY ix_audit_actor_created(actor_uuid,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, task_id INT NULL,
  sender_uuid VARCHAR(36) NULL, recipient_uuid VARCHAR(36) NOT NULL,
  title VARCHAR(191) NOT NULL, message TEXT NOT NULL, kind VARCHAR(40) NOT NULL,
  request_key VARCHAR(191) NULL, email_status VARCHAR(32) NOT NULL DEFAULT 'not_requested',
  read_at TIMESTAMP NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_notification_request(recipient_uuid,request_key),
  KEY ix_notifications_recipient(recipient_uuid,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS urgent_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, task_id INT NOT NULL,
  sender_uuid VARCHAR(36) NOT NULL, message TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'submitted', request_key VARCHAR(100) NOT NULL,
  handled_by VARCHAR(36) NULL, reply TEXT, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_urgent_request(sender_uuid,request_key), KEY ix_urgent_status(status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS site_content (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, kind VARCHAR(32) NOT NULL,
  title VARCHAR(191) NOT NULL, body TEXT NOT NULL, url VARCHAR(1500) NOT NULL DEFAULT '',
  category VARCHAR(100) NOT NULL DEFAULT '', duration VARCHAR(32) NOT NULL DEFAULT '',
  audience VARCHAR(32) NOT NULL DEFAULT 'both', status VARCHAR(32) NOT NULL DEFAULT 'pending',
  author_uuid VARCHAR(36) NOT NULL, reviewed_by VARCHAR(36) NULL, review_reason TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY ix_content_public(kind,status,audience,created_at), KEY ix_content_author(author_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Preserve the ten existing real course files; do not seed demo thanks.
INSERT INTO site_content(kind,title,body,url,category,duration,audience,status,author_uuid) SELECT 'course','急救三角巾包扎','原有课程资料','/course-files/bandage.mp4','急救','6:41','rescuer','published','system-course-import' WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE kind='course' AND url='/course-files/bandage.mp4');
INSERT INTO site_content(kind,title,body,url,category,duration,audience,status,author_uuid) SELECT 'course','和老人沟通技巧','原有课程资料','/course-files/laorengoutong.mp4','沟通','3:26','both','published','system-course-import' WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE kind='course' AND url='/course-files/laorengoutong.mp4');
INSERT INTO site_content(kind,title,body,url,category,duration,audience,status,author_uuid) SELECT 'course','“黄金四分钟”心肺复苏','原有课程资料','/course-files/xinfeifusu.mp4','急救','4:30','rescuer','published','system-course-import' WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE kind='course' AND url='/course-files/xinfeifusu.mp4');
INSERT INTO site_content(kind,title,body,url,category,duration,audience,status,author_uuid) SELECT 'course','与失智老人沟通法','原有课程资料','/course-files/shizhilaoren.mp4','沟通','2:43','both','published','system-course-import' WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE kind='course' AND url='/course-files/shizhilaoren.mp4');
INSERT INTO site_content(kind,title,body,url,category,duration,audience,status,author_uuid) SELECT 'course','对讲机使用教学','原有课程资料','/course-files/duijiangji.mp4','设备','3:48','rescuer','published','system-course-import' WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE kind='course' AND url='/course-files/duijiangji.mp4');
INSERT INTO site_content(kind,title,body,url,category,duration,audience,status,author_uuid) SELECT 'course','徒步搜救省力走姿','原有课程资料','/course-files/tubu.mp4','体能','1:22','rescuer','published','system-course-import' WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE kind='course' AND url='/course-files/tubu.mp4');
INSERT INTO site_content(kind,title,body,url,category,duration,audience,status,author_uuid) SELECT 'course','山地绳索基础打结','原有课程资料','/course-files/moutain.mp4','技术','8:01','rescuer','published','system-course-import' WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE kind='course' AND url='/course-files/moutain.mp4');
INSERT INTO site_content(kind,title,body,url,category,duration,audience,status,author_uuid) SELECT 'course','急救担架搬运','原有课程资料','/course-files/danjia.mp4','急救','1:17','rescuer','published','system-course-import' WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE kind='course' AND url='/course-files/danjia.mp4');
INSERT INTO site_content(kind,title,body,url,category,duration,audience,status,author_uuid) SELECT 'course','创口止血压迫包扎','原有课程资料','/course-files/baoza.mp4','急救','18:18','rescuer','published','system-course-import' WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE kind='course' AND url='/course-files/baoza.mp4');
INSERT INTO site_content(kind,title,body,url,category,duration,audience,status,author_uuid) SELECT 'course','雨季救援触电防范','原有课程资料','/course-files/yujichudian.mp4','安全','1:59','rescuer','published','system-course-import' WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE kind='course' AND url='/course-files/yujichudian.mp4');
