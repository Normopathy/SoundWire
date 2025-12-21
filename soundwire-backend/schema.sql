-- SoundWire local schema (MySQL)
-- Кодировка: utf8mb4 чтобы нормально хранить эмодзи/русский
--
-- ⚠️ ВАЖНО:
-- 1) Этот файл рассчитан на "чистую" установку (пустая БД).
-- 2) Если у вас уже была создана БД по старой схеме, выполните soundwire-backend/migrate.sql
--    (или просто удалите БД soundwire и выполните этот файл заново).

CREATE DATABASE IF NOT EXISTS soundwire
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE soundwire;

-- ---------------- USERS ----------------

CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  username VARCHAR(80) NOT NULL,
  status VARCHAR(140) DEFAULT NULL,
  avatar_path VARCHAR(255) DEFAULT NULL,
  last_seen TIMESTAMP NULL DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------- CONTACTS ----------------

CREATE TABLE IF NOT EXISTS contact_requests (
  id INT AUTO_INCREMENT PRIMARY KEY,
  from_user_id INT NOT NULL,
  to_user_id INT NOT NULL,
  status ENUM('pending','accepted','declined') NOT NULL DEFAULT 'pending',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uniq_from_to (from_user_id, to_user_id),
  CONSTRAINT fk_cr_from FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_cr_to FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_cr_to_status (to_user_id, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS contacts (
  user_id INT NOT NULL,
  contact_id INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, contact_id),
  CONSTRAINT fk_contacts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_contacts_contact FOREIGN KEY (contact_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_contacts_user (user_id)
) ENGINE=InnoDB;

-- ---------------- CHATS ----------------

CREATE TABLE IF NOT EXISTS chats (
  id INT AUTO_INCREMENT PRIMARY KEY,
  type ENUM('private','group') NOT NULL DEFAULT 'private',
  title VARCHAR(255) DEFAULT NULL,
  avatar_path VARCHAR(255) DEFAULT NULL,
  created_by INT DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_message TEXT DEFAULT NULL,
  last_message_time TIMESTAMP NULL DEFAULT NULL,
  CONSTRAINT fk_chats_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_chats_last_message_time (last_message_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_participants (
  chat_id INT NOT NULL,
  user_id INT NOT NULL,
  role ENUM('member','admin') NOT NULL DEFAULT 'member',
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (chat_id, user_id),
  CONSTRAINT fk_cp_chat FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
  CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_cp_user (user_id)
) ENGINE=InnoDB;

-- ---------------- MESSAGES ----------------

CREATE TABLE IF NOT EXISTS messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  chat_id INT NOT NULL,
  sender_id INT NOT NULL,
  type ENUM('text','image','audio','file') NOT NULL DEFAULT 'text',
  text TEXT DEFAULT NULL,
  file_path VARCHAR(255) DEFAULT NULL,
  file_name VARCHAR(255) DEFAULT NULL,
  mime_type VARCHAR(120) DEFAULT NULL,
  duration_ms INT DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_msg_chat FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
  CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_messages_chat_id_id (chat_id, id)
) ENGINE=InnoDB;
