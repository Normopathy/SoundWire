require('dotenv').config();

const express = require('express');
const cors = require('cors');
const http = require('http');
const { Server } = require('socket.io');
const path = require('path');

const authRoutes = require('./routes/auth');
const usersRoutes = require('./routes/users');
const contactsRoutes = require('./routes/contacts');
const musicRoutes = require('./routes/music');
const { router: chatsRoutes } = require('./routes/chats');

const attachSocket = require('./socket');

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

// Статическая раздача загруженных файлов (/uploads/...)
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

app.get('/health', (req, res) => res.json({ ok: true }));

app.use('/auth', authRoutes);
app.use('/users', usersRoutes);
app.use('/contacts', contactsRoutes);
app.use('/chats', chatsRoutes);
app.use('/music', musicRoutes);

const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: '*' }
});

// чтобы роуты могли делать io.to(...).emit(...)
app.set('io', io);

attachSocket(io);

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`SoundWire backend listening on http://localhost:${PORT}`);
  console.log('Music folder:', path.join(__dirname, process.env.MUSIC_DIR || 'music'));
});
