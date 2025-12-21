const express = require('express');
const fs = require('fs');
const path = require('path');

const router = express.Router();

const MUSIC_DIR = process.env.MUSIC_DIR || 'music';

function getMusicFolder() {
  return path.join(__dirname, '..', MUSIC_DIR);
}

function isAudioFile(name) {
  const ext = path.extname(name).toLowerCase();
  return ['.mp3', '.m4a', '.aac', '.wav', '.ogg', '.flac'].includes(ext);
}

function encodeId(relPath) {
  return Buffer.from(relPath).toString('base64url');
}

function decodeId(id) {
  return Buffer.from(id, 'base64url').toString('utf8');
}

function guessMime(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  switch (ext) {
    case '.mp3': return 'audio/mpeg';
    case '.m4a': return 'audio/mp4';
    case '.aac': return 'audio/aac';
    case '.wav': return 'audio/wav';
    case '.ogg': return 'audio/ogg';
    case '.flac': return 'audio/flac';
    default: return 'application/octet-stream';
  }
}

router.get('/list', async (req, res) => {
  try {
    const folder = getMusicFolder();
    if (!fs.existsSync(folder)) {
      return res.json([]);
    }

    const files = fs.readdirSync(folder)
      .filter(isAudioFile)
      .sort((a, b) => a.localeCompare(b));

    const out = files.map(fileName => {
      const id = encodeId(fileName);
      const title = path.parse(fileName).name;
      return {
        id,
        title,
        artist: null,
        url: `/music/stream/${id}`
      };
    });

    return res.json(out);
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

router.get('/stream/:id', async (req, res) => {
  try {
    const rel = decodeId(req.params.id);
    const folder = getMusicFolder();
    const fullPath = path.join(folder, rel);

    // защита от path traversal
    if (!fullPath.startsWith(folder)) {
      return res.status(400).end();
    }

    if (!fs.existsSync(fullPath)) {
      return res.status(404).end();
    }

    const stat = fs.statSync(fullPath);
    const fileSize = stat.size;
    const range = req.headers.range;
    const mime = guessMime(fullPath);

    if (range) {
      const parts = range.replace(/bytes=/, '').split('-');
      const start = parseInt(parts[0], 10);
      const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1;
      const chunkSize = (end - start) + 1;

      const file = fs.createReadStream(fullPath, { start, end });
      res.writeHead(206, {
        'Content-Range': `bytes ${start}-${end}/${fileSize}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunkSize,
        'Content-Type': mime,
      });
      file.pipe(res);
    } else {
      res.writeHead(200, {
        'Content-Length': fileSize,
        'Content-Type': mime,
      });
      fs.createReadStream(fullPath).pipe(res);
    }
  } catch (e) {
    console.error(e);
    return res.status(500).end();
  }
});

module.exports = router;
