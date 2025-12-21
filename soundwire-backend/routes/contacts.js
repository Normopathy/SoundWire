const express = require('express');
const pool = require('../db');
const { auth } = require('../middleware/auth');

const router = express.Router();

function makeAvatarUrl(req, avatar_path) {
  return avatar_path ? `${req.protocol}://${req.get('host')}${avatar_path}` : null;
}

function mapUserFromRow(req, row, prefix = '') {
  // Ожидаемые поля: `${prefix}Id`, `${prefix}Email`, `${prefix}Username`, `${prefix}Status`, `${prefix}Avatar`, `${prefix}LastSeen`
  return {
    id: row[`${prefix}Id`],
    email: row[`${prefix}Email`],
    username: row[`${prefix}Username`],
    status: row[`${prefix}Status`],
    avatarUrl: makeAvatarUrl(req, row[`${prefix}Avatar`]),
    lastSeen: row[`${prefix}LastSeen`] || null,
  };
}

async function ensureUserExists(userId) {
  const [rows] = await pool.query('SELECT id FROM users WHERE id = ? LIMIT 1', [userId]);
  return rows.length > 0;
}

async function areContacts(a, b) {
  const [rows] = await pool.query('SELECT 1 FROM contacts WHERE user_id = ? AND contact_id = ? LIMIT 1', [a, b]);
  return rows.length > 0;
}

async function addContactsPair(a, b) {
  // Две записи (двунаправленно)
  await pool.query(
    'INSERT IGNORE INTO contacts (user_id, contact_id) VALUES (?, ?), (?, ?)',
    [a, b, b, a]
  );
}

// ----------- LIST CONTACTS -----------
router.get('/', auth, async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT u.id AS uId, u.email AS uEmail, u.username AS uUsername, u.status AS uStatus,
              u.avatar_path AS uAvatar,
              UNIX_TIMESTAMP(u.last_seen) * 1000 AS uLastSeen
       FROM contacts c
       JOIN users u ON u.id = c.contact_id
       WHERE c.user_id = ?
       ORDER BY u.username ASC`,
      [req.userId]
    );

    return res.json(rows.map(r => mapUserFromRow(req, r, 'u')));
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

// ----------- SEND REQUEST -----------
router.post('/request', auth, async (req, res) => {
  try {
    const { toUserId } = req.body || {};
    const toId = Number(toUserId);

    if (!toId) return res.status(400).json({ error: 'toUserId is required' });
    if (toId === Number(req.userId)) return res.status(400).json({ error: 'Cannot add yourself' });

    const exists = await ensureUserExists(toId);
    if (!exists) return res.status(404).json({ error: 'User not found' });

    // Уже в контактах?
    if (await areContacts(req.userId, toId)) {
      return res.json({ ok: true, already: true });
    }

    // Если есть входящий pending-запрос от этого юзера → сразу принимаем
    const [incoming] = await pool.query(
      'SELECT id FROM contact_requests WHERE from_user_id = ? AND to_user_id = ? AND status = \'pending\' LIMIT 1',
      [toId, req.userId]
    );

    if (incoming.length > 0) {
      const reqId = incoming[0].id;
      await pool.query('UPDATE contact_requests SET status = \'accepted\' WHERE id = ?', [reqId]);
      await addContactsPair(req.userId, toId);
      return res.json({ ok: true, accepted: true, requestId: reqId });
    }

    // Создаём/обновляем исходящий запрос
    // Если запись уже была (accepted/declined) — переводим снова в pending
    await pool.query(
      `INSERT INTO contact_requests (from_user_id, to_user_id, status)
       VALUES (?, ?, 'pending')
       ON DUPLICATE KEY UPDATE status = 'pending', updated_at = CURRENT_TIMESTAMP`,
      [req.userId, toId]
    );

    const [rows] = await pool.query(
      'SELECT id FROM contact_requests WHERE from_user_id = ? AND to_user_id = ? LIMIT 1',
      [req.userId, toId]
    );

    return res.json({ ok: true, requestId: rows[0]?.id || null });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

// ----------- INCOMING REQUESTS -----------
router.get('/requests/incoming', auth, async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT r.id AS requestId, r.status, UNIX_TIMESTAMP(r.created_at) * 1000 AS createdAt,
              u.id AS fromId, u.email AS fromEmail, u.username AS fromUsername,
              u.status AS fromStatus, u.avatar_path AS fromAvatar,
              UNIX_TIMESTAMP(u.last_seen) * 1000 AS fromLastSeen
       FROM contact_requests r
       JOIN users u ON u.id = r.from_user_id
       WHERE r.to_user_id = ? AND r.status = 'pending'
       ORDER BY r.created_at DESC`,
      [req.userId]
    );

    return res.json(rows.map(r => ({
      id: r.requestId,
      status: r.status,
      createdAt: r.createdAt || null,
      fromUser: mapUserFromRow(req, r, 'from'),
    })));
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

// ----------- OUTGOING REQUESTS -----------
router.get('/requests/outgoing', auth, async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT r.id AS requestId, r.status, UNIX_TIMESTAMP(r.created_at) * 1000 AS createdAt,
              u.id AS toId, u.email AS toEmail, u.username AS toUsername,
              u.status AS toStatus, u.avatar_path AS toAvatar,
              UNIX_TIMESTAMP(u.last_seen) * 1000 AS toLastSeen
       FROM contact_requests r
       JOIN users u ON u.id = r.to_user_id
       WHERE r.from_user_id = ? AND r.status = 'pending'
       ORDER BY r.created_at DESC`,
      [req.userId]
    );

    return res.json(rows.map(r => ({
      id: r.requestId,
      status: r.status,
      createdAt: r.createdAt || null,
      toUser: mapUserFromRow(req, r, 'to'),
    })));
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

// ----------- ACCEPT REQUEST -----------
router.post('/requests/:id/accept', auth, async (req, res) => {
  try {
    const id = Number(req.params.id);
    if (!id) return res.status(400).json({ error: 'Invalid id' });

    const [rows] = await pool.query(
      'SELECT id, from_user_id AS fromId, to_user_id AS toId, status FROM contact_requests WHERE id = ? LIMIT 1',
      [id]
    );
    if (rows.length === 0) return res.status(404).json({ error: 'Not found' });

    const r = rows[0];
    if (Number(r.toId) !== Number(req.userId)) return res.status(403).json({ error: 'Forbidden' });
    if (r.status !== 'pending') return res.json({ ok: true, already: true });

    await pool.query('UPDATE contact_requests SET status = \'accepted\' WHERE id = ?', [id]);
    await addContactsPair(r.fromId, r.toId);

    return res.json({ ok: true });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

// ----------- DECLINE REQUEST -----------
router.post('/requests/:id/decline', auth, async (req, res) => {
  try {
    const id = Number(req.params.id);
    if (!id) return res.status(400).json({ error: 'Invalid id' });

    const [rows] = await pool.query(
      'SELECT id, to_user_id AS toId, status FROM contact_requests WHERE id = ? LIMIT 1',
      [id]
    );
    if (rows.length === 0) return res.status(404).json({ error: 'Not found' });

    const r = rows[0];
    if (Number(r.toId) !== Number(req.userId)) return res.status(403).json({ error: 'Forbidden' });
    if (r.status !== 'pending') return res.json({ ok: true, already: true });

    await pool.query('UPDATE contact_requests SET status = \'declined\' WHERE id = ?', [id]);

    return res.json({ ok: true });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

// ----------- REMOVE CONTACT -----------
router.delete('/:contactId', auth, async (req, res) => {
  try {
    const contactId = Number(req.params.contactId);
    if (!contactId) return res.status(400).json({ error: 'Invalid contactId' });

    await pool.query('DELETE FROM contacts WHERE user_id = ? AND contact_id = ?', [req.userId, contactId]);
    await pool.query('DELETE FROM contacts WHERE user_id = ? AND contact_id = ?', [contactId, req.userId]);

    return res.json({ ok: true });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

module.exports = router;
