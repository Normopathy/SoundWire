-- SoundWire migration script (safe-ish)
-- Выполните этот файл, если у вас уже была создана БД soundwire по старой схеме.
-- Скрипт старается быть идемпотентным (добавляет только недостающие колонки/таблицы).

USE soundwire;

DELIMITER $$

CREATE PROCEDURE soundwire_migrate()
BEGIN
  -- users.last_seen
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'last_seen'
  ) THEN
    ALTER TABLE users ADD COLUMN last_seen TIMESTAMP NULL DEFAULT NULL;
  END IF;

  -- chats.avatar_path
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chats' AND COLUMN_NAME = 'avatar_path'
  ) THEN
    ALTER TABLE chats ADD COLUMN avatar_path VARCHAR(255) DEFAULT NULL;
  END IF;

  -- chats.created_by
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chats' AND COLUMN_NAME = 'created_by'
  ) THEN
    ALTER TABLE chats ADD COLUMN created_by INT DEFAULT NULL;
  END IF;

  -- chat_participants.role
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_participants' AND COLUMN_NAME = 'role'
  ) THEN
    ALTER TABLE chat_participants ADD COLUMN role ENUM('member','admin') NOT NULL DEFAULT 'member';
  END IF;

  -- chat_participants.joined_at
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_participants' AND COLUMN_NAME = 'joined_at'
  ) THEN
    ALTER TABLE chat_participants ADD COLUMN joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
  END IF;

  -- messages: расширяем type и добавляем поля для вложений
  -- enum проще всегда модифицировать без проверки
  ALTER TABLE messages MODIFY COLUMN type ENUM('text','image','audio','file') NOT NULL DEFAULT 'text';

  -- messages.text nullable
  -- В старой схеме text был NOT NULL — сделаем NULL
  ALTER TABLE messages MODIFY COLUMN text TEXT NULL;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'messages' AND COLUMN_NAME = 'file_path'
  ) THEN
    ALTER TABLE messages ADD COLUMN file_path VARCHAR(255) DEFAULT NULL;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'messages' AND COLUMN_NAME = 'file_name'
  ) THEN
    ALTER TABLE messages ADD COLUMN file_name VARCHAR(255) DEFAULT NULL;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'messages' AND COLUMN_NAME = 'mime_type'
  ) THEN
    ALTER TABLE messages ADD COLUMN mime_type VARCHAR(120) DEFAULT NULL;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'messages' AND COLUMN_NAME = 'duration_ms'
  ) THEN
    ALTER TABLE messages ADD COLUMN duration_ms INT DEFAULT NULL;
  END IF;

  -- contact_requests
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

  -- contacts
  CREATE TABLE IF NOT EXISTS contacts (
    user_id INT NOT NULL,
    contact_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, contact_id),
    CONSTRAINT fk_contacts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_contacts_contact FOREIGN KEY (contact_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_contacts_user (user_id)
  ) ENGINE=InnoDB;

END$$

DELIMITER ;

CALL soundwire_migrate();
DROP PROCEDURE soundwire_migrate;
