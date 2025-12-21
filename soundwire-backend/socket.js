const jwt = require('jsonwebtoken');
const pool = require('./db');
const { JWT_SECRET } = require('./middleware/auth');

function extractToken(socket) {
  // 1) query ?token=...
  if (socket.handshake && socket.handshake.query && socket.handshake.query.token) {
    return socket.handshake.query.token;
  }

  // 2) auth payload (socket.io v4)
  if (socket.handshake && socket.handshake.auth && socket.handshake.auth.token) {
    return socket.handshake.auth.token;
  }

  // 3) header Authorization: Bearer ...
  const auth = (socket.handshake.headers && socket.handshake.headers.authorization) || '';
  if (auth.startsWith('Bearer ')) {
    return auth.slice(7);
  }

  return null;
}

async function isParticipant(chatId, userId) {
  const [rows] = await pool.query(
    'SELECT 1 FROM chat_participants WHERE chat_id = ? AND user_id = ? LIMIT 1',
    [chatId, userId]
  );
  return rows.length > 0;
}

function makeAvatarUrlForSocket(socket, avatarPath) {
  if (!avatarPath) return null;
  const host = (socket.handshake.headers && socket.handshake.headers.host) || '';
  if (!host) return avatarPath;
  return `http://${host}${avatarPath}`;
}

// userId -> connections count
const onlineCounts = new Map();

function setOnline(userId) {
  const c = onlineCounts.get(userId) || 0;
  onlineCounts.set(userId, c + 1);
}

function setOffline(userId) {
  const c = onlineCounts.get(userId) || 0;
  if (c <= 1) {
    onlineCounts.delete(userId);
    return true; // became offline
  }
  onlineCounts.set(userId, c - 1);
  return false;
}

function onlineUserIds() {
  return Array.from(onlineCounts.keys());
}

module.exports = function attachSocket(io) {
  io.use((socket, next) => {
    try {
      const token = extractToken(socket);
      if (!token) return next(new Error('unauthorized'));
      const payload = jwt.verify(token, JWT_SECRET);
      socket.userId = payload.id;
      return next();
    } catch (e) {
      return next(new Error('unauthorized'));
    }
  });

  io.on('connection', (socket) => {
    console.log('Socket connected. userId=', socket.userId);

    // Presence: online
    setOnline(socket.userId);
    io.emit('presence_update', { userId: socket.userId, online: true });

    // Snapshot to newly connected socket
    socket.emit('presence_snapshot', { onlineUserIds: onlineUserIds() });

    socket.on('presence_get', () => {
      socket.emit('presence_snapshot', { onlineUserIds: onlineUserIds() });
    });

    socket.on('join_chat', async (chatId) => {
      try {
        const id = Number(chatId);
        if (!id) return;
        const ok = await isParticipant(id, socket.userId);
        if (!ok) return;
        socket.join(`chat_${id}`);
      } catch (e) {
        console.error(e);
      }
    });

    socket.on('leave_chat', (chatId) => {
      const id = Number(chatId);
      if (!id) return;
      socket.leave(`chat_${id}`);
    });

    // Text messages via socket (attachments are sent via HTTP route)
    socket.on('send_message', async (payload) => {
      try {
        const chatId = Number(payload.chatId);
        const text = (payload.text || '').toString().trim();
        if (!chatId || !text) return;

        const ok = await isParticipant(chatId, socket.userId);
        if (!ok) return;

        const [insertRes] = await pool.query(
          'INSERT INTO messages (chat_id, sender_id, type, text) VALUES (?, ?, \'text\', ?)',
          [chatId, socket.userId, text]
        );

        await pool.query(
          'UPDATE chats SET last_message = ?, last_message_time = NOW() WHERE id = ?',
          [text, chatId]
        );

        const messageId = insertRes.insertId;

        const [mRows] = await pool.query(
          `SELECT id, chat_id AS chatId, sender_id AS senderId, type, text,
                  UNIX_TIMESTAMP(created_at)*1000 AS createdAt
           FROM messages WHERE id = ? LIMIT 1`,
          [messageId]
        );

        const [uRows] = await pool.query(
          'SELECT id, email, username, status, avatar_path, UNIX_TIMESTAMP(last_seen)*1000 AS lastSeen FROM users WHERE id = ? LIMIT 1',
          [socket.userId]
        );

        const m = mRows[0];
        const u = uRows[0];

        const event = {
          id: m.id,
          chatId: m.chatId,
          senderId: m.senderId,
          type: m.type,
          text: m.text || '',
          fileUrl: null,
          fileName: null,
          mimeType: null,
          durationMs: null,
          createdAt: m.createdAt || Date.now(),
          sender: {
            id: u.id,
            email: u.email,
            username: u.username,
            status: u.status,
            lastSeen: u.lastSeen || null,
            avatarUrl: makeAvatarUrlForSocket(socket, u.avatar_path),
          }
        };

        io.to(`chat_${chatId}`).emit('new_message', event);
      } catch (e) {
        console.error(e);
      }
    });

    socket.on('disconnect', async () => {
      try {
        const becameOffline = setOffline(socket.userId);
        if (becameOffline) {
          // Save last seen
          try {
            await pool.query('UPDATE users SET last_seen = NOW() WHERE id = ?', [socket.userId]);
          } catch (_) {}

          io.emit('presence_update', { userId: socket.userId, online: false, lastSeen: Date.now() });
        }
      } catch (e) {
        console.error(e);
      }
    });
  });
};
