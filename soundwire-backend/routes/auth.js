const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const pool = require('../db');
const { JWT_SECRET } = require('../middleware/auth');

const router = express.Router();

router.post('/register', async (req, res) => {
  try {
    const { email, password, username } = req.body || {};
    if (!email || !password || !username) {
      return res.status(400).json({ error: 'email, password, username are required' });
    }

    // check existing
    const [rows] = await pool.query('SELECT id FROM users WHERE email = ? LIMIT 1', [email]);
    if (rows.length > 0) {
      return res.status(409).json({ error: 'User already exists' });
    }

    const hash = await bcrypt.hash(password, 10);
    await pool.query(
      'INSERT INTO users (email, password_hash, username, status) VALUES (?, ?, ?, ?) ',
      [email, hash, username, '']
    );

    return res.json({ ok: true });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body || {};
    if (!email || !password) {
      return res.status(400).json({ error: 'email and password are required' });
    }

    const [rows] = await pool.query(
      'SELECT id, email, password_hash, username, status, avatar_path, UNIX_TIMESTAMP(last_seen)*1000 AS lastSeen FROM users WHERE email = ? LIMIT 1',
      [email]
    );

    if (rows.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const user = rows[0];
    const ok = await bcrypt.compare(password, user.password_hash);
    if (!ok) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const token = jwt.sign({ id: user.id }, JWT_SECRET, { expiresIn: '30d' });
    const avatarUrl = user.avatar_path ? `${req.protocol}://${req.get('host')}${user.avatar_path}` : null;

    return res.json({
      token,
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
        status: user.status,
        avatarUrl,
        lastSeen: user.lastSeen || null,
      }
    });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

module.exports = router;
