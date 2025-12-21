const express = require('express');
const path = require('path');
const fs = require('fs');
const multer = require('multer');
const pool = require('../db');
const { auth } = require('../middleware/auth');

const router = express.Router();

function makeAvatarUrl(req, avatar_path) {
  return avatar_path ? `${req.protocol}://${req.get('host')}${avatar_path}` : null;
}

function mapUser(req, u) {
  return {
    id: u.id,
    email: u.email,
    username: u.username,
    status: u.status,
    avatarUrl: makeAvatarUrl(req, u.avatar_path),
    lastSeen: u.lastSeen || null,
  };
}

const uploadDir = path.join(__dirname, '..', 'uploads', 'avatars');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname || '');
    cb(null, `avatar_${req.userId}_${Date.now()}${ext}`);
  }
});

const upload = multer({ storage });

router.get('/me', auth, async (req, res) => {
  try {
    const [rows] = await pool.query(
      'SELECT id, email, username, status, avatar_path, UNIX_TIMESTAMP(last_seen)*1000 AS lastSeen FROM users WHERE id = ? LIMIT 1',
      [req.userId]
    );

    if (rows.length === 0) return res.status(404).json({ error: 'Not found' });

    return res.json(mapUser(req, rows[0]));
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

router.put('/me', auth, async (req, res) => {
  try {
    const { username, status } = req.body || {};
    if (!username) return res.status(400).json({ error: 'username is required' });

    await pool.query(
      'UPDATE users SET username = ?, status = ? WHERE id = ?',
      [username, status || null, req.userId]
    );

    const [rows] = await pool.query(
      'SELECT id, email, username, status, avatar_path, UNIX_TIMESTAMP(last_seen)*1000 AS lastSeen FROM users WHERE id = ? LIMIT 1',
      [req.userId]
    );

    return res.json(mapUser(req, rows[0]));
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

router.post('/me/avatar', auth, upload.single('avatar'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ error: 'avatar file is required' });

    const relPath = `/uploads/avatars/${req.file.filename}`;
    await pool.query('UPDATE users SET avatar_path = ? WHERE id = ?', [relPath, req.userId]);

    return res.json({ avatarUrl: makeAvatarUrl(req, relPath) });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

router.get('/list', auth, async (req, res) => {
  try {
    const [rows] = await pool.query(
      'SELECT id, email, username, status, avatar_path, UNIX_TIMESTAMP(last_seen)*1000 AS lastSeen FROM users WHERE id <> ? ORDER BY username ASC',
      [req.userId]
    );

    return res.json(rows.map(u => mapUser(req, u)));
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

router.get('/search', auth, async (req, res) => {
  try {
    const q = (req.query.q || '').toString().trim();
    if (!q) return res.json([]);

    const like = `%${q}%`;
    const [rows] = await pool.query(
      'SELECT id, email, username, status, avatar_path, UNIX_TIMESTAMP(last_seen)*1000 AS lastSeen FROM users WHERE id <> ? AND (email LIKE ? OR username LIKE ?) ORDER BY username ASC LIMIT 50',
      [req.userId, like, like]
    );

    return res.json(rows.map(u => mapUser(req, u)));
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

module.exports = router;
