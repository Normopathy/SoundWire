const express = require('express');
const pool = require('../db');
const { auth } = require('../middleware/auth');
const path = require('path');
const fs = require('fs');
const multer = require('multer');

const router = express.Router();

function makeUrl(req, relPath) {
  return relPath ? `${req.protocol}://${req.get('host')}${relPath}` : null;
}

function makeAvatarUrl(req, avatar_path) {
  return makeUrl(req, avatar_path);
}

function makeFileUrl(req, file_path) {
  return makeUrl(req, file_path);
}

async function ensureParticipant(chatId, userId) {
  const [rows] = await pool.query(
    'SELECT 1 FROM chat_participants WHERE chat_id = ? AND user_id = ? LIMIT 1',
    [chatId, userId]
  );
  return rows.length > 0;
}

async function ensureChatIsGroup(chatId) {
  const [rows] = await pool.query('SELECT type FROM chats WHERE id = ? LIMIT 1', [chatId]);
  return rows.length > 0 && rows[0].type === 'group';
}

async function getUserById(userId) {
  const [rows] = await pool.query(
    'SELECT id, email, username, status, avatar_path, UNIX_TIMESTAMP(last_seen)*1000 AS lastSeen FROM users WHERE id = ? LIMIT 1',
    [userId]
  );
  return rows.length ? rows[0] : null;
}

// ----------- FILE UPLOAD (chat messages) -----------
const chatsUploadRoot = path.join(__dirname, '..', 'uploads', 'chats');
if (!fs.existsSync(chatsUploadRoot)) {
  fs.mkdirSync(chatsUploadRoot, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const chatId = String(req.params.chatId || 'common');
    const dir = path.join(chatsUploadRoot, chatId);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    cb(null, dir);
  },
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname || '');
    const type = (req.body.type || 'file').toString();
    cb(null, `${type}_${req.userId}_${Date.now()}${ext}`);
  }
});

const upload = multer({
  storage,
  limits: { fileSize: 50 * 1024 * 1024 } // 50MB
});

// Отдельная загрузка аватарки группы
const groupAvatarDir = path.join(chatsUploadRoot, 'avatars');
if (!fs.existsSync(groupAvatarDir)) fs.mkdirSync(groupAvatarDir, { recursive: true });

const avatarStorage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, groupAvatarDir),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname || '');
    cb(null, `group_${req.params.chatId}_${Date.now()}${ext}`);
  }
});

const uploadGroupAvatar = multer({ storage: avatarStorage, limits: { fileSize: 10 * 1024 * 1024 } });

// ----------- PRIVATE CHAT (existing) -----------
router.post('/private', auth, async (req, res) => {
  try {
    const { otherUserId } = req.body || {};
    if (!otherUserId) return res.status(400).json({ error: 'otherUserId is required' });
    if (Number(otherUserId) === Number(req.userId)) {
      return res.status(400).json({ error: 'Cannot create chat with yourself' });
    }

    // Проверим, что такой пользователь существует
    const other = await getUserById(otherUserId);
    if (!other) return res.status(404).json({ error: 'User not found' });

    // Ищем существующий private chat между двумя юзерами
    const [cRows] = await pool.query(
      `SELECT c.id
       FROM chats c
       JOIN chat_participants p1 ON p1.chat_id = c.id AND p1.user_id = ?
       JOIN chat_participants p2 ON p2.chat_id = c.id AND p2.user_id = ?
       WHERE c.type = 'private'
       LIMIT 1`,
      [req.userId, otherUserId]
    );

    let chatId;
    if (cRows.length > 0) {
      chatId = cRows[0].id;
    } else {
      const [insertRes] = await pool.query('INSERT INTO chats (type) VALUES (\'private\')');
      chatId = insertRes.insertId;
      await pool.query(
        'INSERT INTO chat_participants (chat_id, user_id, role) VALUES (?, ?, ?), (?, ?, ?)',
        [chatId, req.userId, 'admin', chatId, otherUserId, 'member']
      );
    }

    return res.json({
      id: chatId,
      type: 'private',
      title: null,
      avatarUrl: null,
      membersCount: 2,
      otherUser: {
        id: other.id,
        email: other.email,
        username: other.username,
        status: other.status,
        avatarUrl: makeAvatarUrl(req, other.avatar_path),
        lastSeen: other.lastSeen || null,
      },
      lastMessage: null,
      lastMessageTime: null,
    });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

// ----------- GROUP CHAT CREATE -----------
router.post('/group', auth, async (req, res) => {
  const conn = await pool.getConnection();
  try {
    const { title, participantIds } = req.body || {};
    const t = (title || '').toString().trim();
    const list = Array.isArray(participantIds) ? participantIds.map(Number).filter(n => !!n) : [];

    if (!t) return res.status(400).json({ error: 'title is required' });
    if (list.length === 0) return res.status(400).json({ error: 'participantIds is required' });

    // Уникальные участники
    const uniq = Array.from(new Set([Number(req.userId), ...list])).filter(n => !!n);

    await conn.beginTransaction();

    const [insertRes] = await conn.query(
      'INSERT INTO chats (type, title, created_by) VALUES (\'group\', ?, ?)',
      [t, req.userId]
    );

    const chatId = insertRes.insertId;

    // вставляем участников
    const values = [];
    const placeholders = [];
    uniq.forEach(uid => {
      const role = uid === Number(req.userId) ? 'admin' : 'member';
      placeholders.push('(?, ?, ?)');
      values.push(chatId, uid, role);
    });

    await conn.query(
      `INSERT INTO chat_participants (chat_id, user_id, role) VALUES ${placeholders.join(',')}`,
      values
    );

    await conn.commit();

    return res.json({
      id: chatId,
      type: 'group',
      title: t,
      avatarUrl: null,
      membersCount: uniq.length,
      otherUser: null,
      lastMessage: null,
      lastMessageTime: null,
    });
  } catch (e) {
    try { await conn.rollback(); } catch (_) {}
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  } finally {
    conn.release();
  }
});

// ----------- LIST CHATS (private + group) -----------
router.get('/', auth, async (req, res) => {
  try {
    // PRIVATE
    const [privateRows] = await pool.query(
      `SELECT c.id, c.type, c.title, c.avatar_path AS chatAvatar,
              c.last_message AS lastMessage,
              UNIX_TIMESTAMP(c.last_message_time) * 1000 AS lastMessageTime,
              UNIX_TIMESTAMP(c.created_at) * 1000 AS createdAt,
              u.id AS otherId, u.email AS otherEmail, u.username AS otherUsername,
              u.status AS otherStatus, u.avatar_path AS otherAvatar,
              UNIX_TIMESTAMP(u.last_seen)*1000 AS otherLastSeen
       FROM chats c
       JOIN chat_participants me ON me.chat_id = c.id AND me.user_id = ?
       JOIN chat_participants otherP ON otherP.chat_id = c.id AND otherP.user_id <> ?
       JOIN users u ON u.id = otherP.user_id
       WHERE c.type = 'private'`,
      [req.userId, req.userId]
    );

    const privateOut = privateRows.map(r => ({
      id: r.id,
      type: r.type,
      title: null,
      avatarUrl: null,
      membersCount: 2,
      otherUser: {
        id: r.otherId,
        email: r.otherEmail,
        username: r.otherUsername,
        status: r.otherStatus,
        avatarUrl: makeAvatarUrl(req, r.otherAvatar),
        lastSeen: r.otherLastSeen || null,
      },
      lastMessage: r.lastMessage,
      lastMessageTime: r.lastMessageTime || null,
      createdAt: r.createdAt || null,
    }));

    // GROUP
    const [groupRows] = await pool.query(
      `SELECT c.id, c.type, c.title, c.avatar_path AS chatAvatar,
              c.last_message AS lastMessage,
              UNIX_TIMESTAMP(c.last_message_time) * 1000 AS lastMessageTime,
              UNIX_TIMESTAMP(c.created_at) * 1000 AS createdAt,
              (SELECT COUNT(*) FROM chat_participants cp WHERE cp.chat_id = c.id) AS membersCount
       FROM chats c
       JOIN chat_participants me ON me.chat_id = c.id AND me.user_id = ?
       WHERE c.type = 'group'`,
      [req.userId]
    );

    const groupOut = groupRows.map(r => ({
      id: r.id,
      type: r.type,
      title: r.title,
      avatarUrl: makeAvatarUrl(req, r.chatAvatar),
      membersCount: Number(r.membersCount) || 0,
      otherUser: null,
      lastMessage: r.lastMessage,
      lastMessageTime: r.lastMessageTime || null,
      createdAt: r.createdAt || null,
    }));

    const all = [...privateOut, ...groupOut];
    all.sort((a, b) => {
      const at = a.lastMessageTime || a.createdAt || 0;
      const bt = b.lastMessageTime || b.createdAt || 0;
      return bt - at;
    });

    return res.json(all);
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

// ----------- GET CHAT PARTICIPANTS -----------
router.get('/:chatId/participants', auth, async (req, res) => {
  try {
    const chatId = Number(req.params.chatId);
    if (!chatId) return res.status(400).json({ error: 'chatId is required' });

    const ok = await ensureParticipant(chatId, req.userId);
    if (!ok) return res.status(403).json({ error: 'Forbidden' });

    const [rows] = await pool.query(
      `SELECT u.id AS uId, u.email AS uEmail, u.username AS uUsername, u.status AS uStatus,
              u.avatar_path AS uAvatar, UNIX_TIMESTAMP(u.last_seen)*1000 AS uLastSeen,
              cp.role AS role
       FROM chat_participants cp
       JOIN users u ON u.id = cp.user_id
       WHERE cp.chat_id = ?
       ORDER BY (cp.role = 'admin') DESC, u.username ASC`,
      [chatId]
    );

    const out = rows.map(r => ({
      user: {
        id: r.uId,
        email: r.uEmail,
        username: r.uUsername,
        status: r.uStatus,
        avatarUrl: makeAvatarUrl(req, r.uAvatar),
        lastSeen: r.uLastSeen || null,
      },
      role: r.role,
    }));

    return res.json(out);
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

// ----------- ADD PARTICIPANTS (GROUP) -----------
router.post('/:chatId/participants', auth, async (req, res) => {
  try {
    const chatId = Number(req.params.chatId);
    if (!chatId) return res.status(400).json({ error: 'chatId is required' });

    const ok = await ensureParticipant(chatId, req.userId);
    if (!ok) return res.status(403).json({ error: 'Forbidden' });

    const isGroup = await ensureChatIsGroup(chatId);
    if (!isGroup) return res.status(400).json({ error: 'Not a group chat' });

    const { userIds } = req.body || {};
    const ids = Array.isArray(userIds) ? userIds.map(Number).filter(n => !!n) : [];
    if (ids.length === 0) return res.status(400).json({ error: 'userIds is required' });

    const uniq = Array.from(new Set(ids));

    const placeholders = uniq.map(() => '(?, ?, ?)').join(',');
    const params = [];
    uniq.forEach(uid => {
      params.push(chatId, uid, 'member');
    });

    await pool.query(
      `INSERT IGNORE INTO chat_participants (chat_id, user_id, role) VALUES ${placeholders}`,
      params
    );

    return res.json({ ok: true });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

// ----------- GROUP AVATAR UPLOAD -----------
router.post('/:chatId/avatar', auth, uploadGroupAvatar.single('avatar'), async (req, res) => {
  try {
    const chatId = Number(req.params.chatId);
    if (!chatId) return res.status(400).json({ error: 'chatId is required' });

    const ok = await ensureParticipant(chatId, req.userId);
    if (!ok) return res.status(403).json({ error: 'Forbidden' });

    const isGroup = await ensureChatIsGroup(chatId);
    if (!isGroup) return res.status(400).json({ error: 'Not a group chat' });

    if (!req.file) return res.status(400).json({ error: 'avatar file is required' });

    const relPath = `/uploads/chats/avatars/${req.file.filename}`;
    await pool.query('UPDATE chats SET avatar_path = ? WHERE id = ?', [relPath, chatId]);

    return res.json({ avatarUrl: makeUrl(req, relPath) });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

// ----------- MESSAGES LIST -----------
router.get('/:chatId/messages', auth, async (req, res) => {
  try {
    const chatId = Number(req.params.chatId);
    const limit = Math.min(Number(req.query.limit) || 50, 200);
    const beforeId = req.query.beforeId ? Number(req.query.beforeId) : null;

    if (!chatId) return res.status(400).json({ error: 'chatId is required' });

    const ok = await ensureParticipant(chatId, req.userId);
    if (!ok) return res.status(403).json({ error: 'Forbidden' });

    let sql =
      `SELECT m.id, m.chat_id AS chatId, m.sender_id AS senderId, m.type, m.text,
              m.file_path AS filePath, m.file_name AS fileName, m.mime_type AS mimeType,
              m.duration_ms AS durationMs,
              UNIX_TIMESTAMP(m.created_at) * 1000 AS createdAt,
              u.id AS uId, u.email AS uEmail, u.username AS uUsername, u.status AS uStatus,
              u.avatar_path AS uAvatar, UNIX_TIMESTAMP(u.last_seen)*1000 AS uLastSeen
       FROM messages m
       JOIN users u ON u.id = m.sender_id
       WHERE m.chat_id = ?`;

    const params = [chatId];
    if (beforeId) {
      sql += ' AND m.id < ?';
      params.push(beforeId);
    }
    sql += ' ORDER BY m.id DESC LIMIT ?';
    params.push(limit);

    const [rows] = await pool.query(sql, params);

    const out = rows.reverse().map(r => ({
      id: r.id,
      chatId: r.chatId,
      senderId: r.senderId,
      type: r.type,
      text: r.text || '',
      fileUrl: makeFileUrl(req, r.filePath),
      fileName: r.fileName || null,
      mimeType: r.mimeType || null,
      durationMs: r.durationMs || null,
      createdAt: r.createdAt,
      sender: {
        id: r.uId,
        email: r.uEmail,
        username: r.uUsername,
        status: r.uStatus,
        avatarUrl: makeAvatarUrl(req, r.uAvatar),
        lastSeen: r.uLastSeen || null,
      }
    }));

    return res.json(out);
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

// ----------- SEND MESSAGE VIA HTTP (text / image / audio / file) -----------
router.post('/:chatId/messages', auth, upload.single('file'), async (req, res) => {
  try {
    const chatId = Number(req.params.chatId);
    if (!chatId) return res.status(400).json({ error: 'chatId is required' });

    const ok = await ensureParticipant(chatId, req.userId);
    if (!ok) return res.status(403).json({ error: 'Forbidden' });

    const type = (req.body.type || 'text').toString();
    const text = (req.body.text || '').toString();
    const durationMs = req.body.durationMs ? Number(req.body.durationMs) : null;

    const allowed = new Set(['text', 'image', 'audio', 'file']);
    if (!allowed.has(type)) return res.status(400).json({ error: 'Invalid type' });

    let filePath = null;
    let fileName = null;
    let mimeType = null;

    if (type !== 'text') {
      if (!req.file) return res.status(400).json({ error: 'file is required for this type' });
      fileName = (req.file.originalname || '').toString();
      mimeType = (req.file.mimetype || '').toString();
      // multer destination already created as uploads/chats/<chatId>
      filePath = `/uploads/chats/${chatId}/${req.file.filename}`;
    }

    // Для text: пустые сообщения запрещаем
    if (type === 'text' && !text.trim()) {
      return res.status(400).json({ error: 'text is required' });
    }

    const [insertRes] = await pool.query(
      'INSERT INTO messages (chat_id, sender_id, type, text, file_path, file_name, mime_type, duration_ms) VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
      [chatId, req.userId, type, type === 'text' ? text : (text || null), filePath, fileName, mimeType, durationMs]
    );

    const messageId = insertRes.insertId;

    // last_message preview
    let preview = text;
    if (type === 'image') preview = text?.trim() ? `🖼️ ${text.trim()}` : '🖼️ Фото';
    if (type === 'audio') preview = '🎤 Аудио';
    if (type === 'file') preview = fileName ? `📎 ${fileName}` : '📎 Файл';

    await pool.query('UPDATE chats SET last_message = ?, last_message_time = NOW() WHERE id = ?', [preview, chatId]);

    const [mRows] = await pool.query(
      `SELECT m.id, m.chat_id AS chatId, m.sender_id AS senderId, m.type, m.text,
              m.file_path AS filePath, m.file_name AS fileName, m.mime_type AS mimeType,
              m.duration_ms AS durationMs,
              UNIX_TIMESTAMP(m.created_at)*1000 AS createdAt
       FROM messages m WHERE m.id = ? LIMIT 1`,
      [messageId]
    );

    const u = await getUserById(req.userId);

    const m = mRows[0];

    const event = {
      id: m.id,
      chatId: m.chatId,
      senderId: m.senderId,
      type: m.type,
      text: m.text || '',
      fileUrl: makeFileUrl(req, m.filePath),
      fileName: m.fileName || null,
      mimeType: m.mimeType || null,
      durationMs: m.durationMs || null,
      createdAt: m.createdAt || Date.now(),
      sender: u ? {
        id: u.id,
        email: u.email,
        username: u.username,
        status: u.status,
        avatarUrl: makeAvatarUrl(req, u.avatar_path),
        lastSeen: u.lastSeen || null,
      } : null
    };

    // broadcast to socket room
    const io = req.app.get('io');
    if (io) {
      io.to(`chat_${chatId}`).emit('new_message', event);
    }

    return res.json(event);
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

module.exports = { router, ensureParticipant };
