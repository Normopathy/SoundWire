# SoundWire
SoundWire — это кроссплатформенный мессенджер, в котором музыка является центральным элементом коммуникации. Приложение объединяет функции общения и музыкального стриминга, позволяя пользователям делиться треками, создавать совместные плейлисты, участвовать в голосовых комнатах с синхронным прослушиванием и видеть статусы воспроизведения в реальном времени.

Инструкция по запуску:
1) Создать базу и таблицы в Workbench:
Подключится к своему серверу MySQL (обычно root@localhost)
Открыть файл:
SoundWire_Local/soundwire-backend/schema.sql
Выполнить его (кнопка ⚡)
Он создаст БД soundwire и таблицы:

users — профиль, логин

chats, chat_participants — чаты

messages — сообщения

2) Запуск backend (Node.js + Socket.IO + MySQL)
Backend лежит тут:
SoundWire_Local/soundwire-backend/

2.1) Настроить .env
В папке soundwire-backend/:
Скопировать файл .env.example → сделать .env
Открыть .env и поставить свои данные MySQL:

Пример:

PORT=3000
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=YOUR_PASSWORD
DB_NAME=soundwire
JWT_SECRET=soundwire_local_secret_change_me
MUSIC_DIR=music

2.2) Установить зависимости и запустить
В терминале:
cd soundwire-backend
npm install
npm start
Проверка:
Открыть в браузере: http://localhost:3000/health
Должно вернуть { "ok": true }

2.3) Чтобы работало в локальной сети
Если телефон/другие ПК должны подключаться:
Узнать IP компьютера с backend, например 192.168.0.10
Открыть порт 3000 в файрволле Windows (или разрешить Node.js)
Телефоны должны быть в той же Wi‑Fi сети

3) Музыка “с компьютера” (серверная папка)
Положить аудиофайлы на компьютер, где запущен backend, в папку:
SoundWire_Local/soundwire-backend/music/
Поддерживаются:
.mp3 .m4a .aac .wav .ogg .flac
Backend отдаёт:
список: GET /music/list
поток: GET /music/stream/:id

4) Запуск Android-приложения (Android Studio)

4.1) Открыть проект
Открывай папку:
SoundWire_Local/
в Android Studio (как обычный Gradle проект)

4.2) Запусти на эмуляторе
Если backend запущен на твоём ПК, то в эмуляторе адрес сервера будет:
http://10.0.2.2:3000/

4.3) Запусти на реальном телефоне
На телефоне “localhost” — это сам телефон, поэтому нужно IP компьютера:
http://192.168.0.10:3000/

Авторы - Дмитрий Поликарпов, Илья Егоров, Александр Крутицкий.
