const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'soundwire_local_secret_change_me';

function auth(req, res, next) {
  const header = req.headers['authorization'] || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;

  if (!token) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  try {
    const payload = jwt.verify(token, JWT_SECRET);
    req.userId = payload.id;
    next();
  } catch (e) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
}

module.exports = { auth, JWT_SECRET };
