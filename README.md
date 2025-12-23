**SoundWire**<br>
**SoundWire** — это кроссплатформенный мессенджер, в котором музыка является центральным элементом коммуникации. Приложение объединяет функции общения и музыкального стриминга, позволяя пользователям делиться треками, создавать совместные плейлисты, участвовать в голосовых комнатах с синхронным прослушиванием и видеть статусы воспроизведения в реальном времени.

**Инструкция по запуску:**
1. **Создать базу и таблицы в Workbench:** <br>
Подключится к своему серверу MySQL (обычно root@localhost) <br>
Открыть файл: 
SoundWire_Local/soundwire-backend/schema.sql <br>
Выполнить его (кнопка ⚡) <br>
Он создаст БД soundwire и таблицы: <br>
users — профиль, логин <br>
chats, chat_participants — чаты <br>
messages — сообщения <br>
2. **Запуск backend (Node.js + Socket.IO + MySQL)** <br>
Backend лежит тут: 
SoundWire_Local/soundwire-backend/

3. **Настроить .env** <br>
В папке soundwire-backend/: <br>
Скопировать файл .env.example → сделать .env <br>

4. **Установить зависимости и запустить** <br>
cd soundwire-backend <br>
npm install <br>
npm start <br>
Проверка: <br>
Открыть в браузере: http://localhost:3000/health <br>
Должно вернуть { "ok": true }

5. **Чтобы работало в локальной сети** <br>
Если телефон/другие ПК должны подключаться: <br>
Узнать IP компьютера с backend, например 192.168.0.10 <br>
Открыть порт 3000 в файрволле Windows (или разрешить Node.js) <br>
Телефоны должны быть в той же Wi‑Fi сети <br>

6. **Музыка “с компьютера” (серверная папка)** <br>
Положить аудиофайлы на компьютер, где запущен backend, в папку:
SoundWire_Local/soundwire-backend/music/ <br>
Поддерживаются: 
.mp3 .m4a .aac .wav .ogg .flac <br>
Backend отдаёт: <br>
список: GET /music/list <br>
поток: GET /music/stream/:id <br>

7. **Запуск Android-приложения** (Android Studio)<br>
8. **Открыть проект** <br>
Открыть папку: 
SoundWire_Local/ 
в Android Studio (как обычный Gradle проект)

9. **Запустить на эмуляторе** <br>
Если backend запущен на ПК, то в эмуляторе адрес сервера будет: 
http://10.0.2.2:3000/

10. **Запустить на реальном телефоне** <br>
На телефоне “localhost” — это сам телефон, поэтому нужно IP компьютера: 
http://192.168.0.10:3000/

**Список функционала:**

1. Интегрированный Мессенджер
Личные и групповые чаты **(Реализовано не полностью)** <br>
Каналы для публичного общения **(Не реализовано)** <br>
Отправка файлов, аудиосообщений **(Реализовано)** <br>
Статусы доставки и прочтения **(Реализовано)** <br>

2. Музыкальный Стриминг
Каталог лицензированной музыки **(Реализовано)** <br>
Фоновое воспроизведение **(Реализовано)** <br>
Lossless-качество (премиум) **(Не реализовано)** <br>
Адаптивная потоковая передача **(Не реализовано)** <br>

3. Социальные Музыкальные Функции
Статус «Слушает»: Автоматическое отображение текущего трека **(Не реализовано)** <br>
Отправка треков как сообщений: Превью с возможностью воспроизведения в чате **(Частично реализовано)** <br>
Совместные плейлисты: Редактирование несколькими пользователями **(Не реализовано)** <br>
Голосовые комнаты: Синхронное прослушивание + голосовое общение **(Не реализовано)** <br>
«Саундтрек момента»: Автоматические плейлисты на основе активности **(Не реализовано)** <br>

Структура проекта <br>
SoundWire/ <br>
├─ app/                         # Android-приложение (Kotlin) <br>
│  ├─ src/main/java/com/soundwire/ <br>
│  │  ├─ ui/                    # Fragments (Chats/Contacts/Profile/Music/…) <br>
│  │  ├─ player/                # Кэш/плеер (ExoPlayer cache и т.п.) <br>
│  │  ├─ MainActivity.kt         # Навигация по вкладкам + мини-плеер <br>
│  │  ├─ AuthActivity.kt         # Вход/регистрация <br>
│  │  ├─ ChatActivity.kt         # Экран чата <br>
│  │  ├─ PlayerActivity.kt       # Полноэкранный плеер <br>
│  │  ├─ SoundWireApi.kt         # Retrofit API <br>
│  │  ├─ SocketManager.kt        # Socket.IO клиент (онлайн/сообщения) <br>
│  │  ├─ SessionManager.kt       # JWT / хранение сессии <br>
│  │  └─ Models.kt               # DTO/модели <br>
│  ├─ src/main/res/ <br>
│  │  ├─ layout/                 # XML-экраны <br>
│  │  ├─ drawable/               # Иконки/ресурсы <br>
│  │  ├─ menu/                   # Нижнее меню <br>
│  │  └─ values/                 # colors, themes, strings <br>
│  ├─ build.gradle               # Gradle настройки модуля app <br>
│  └─ proguard-rules.pro <br>
│ <br>
├─ soundwire-backend/            # Backend (Node.js + Express + Socket.IO + MySQL) <br>
│  ├─ index.js                   # Точка входа: Express + маршруты + uploads <br>
│  ├─ db.js                      # Подключение к MySQL <br>
│  ├─ socket.js                  # События Socket.IO (чат, presence и т.п.) <br>
│  ├─ routes/                    # REST API <br>
│  │  ├─ auth.js                 # Регистрация/логин (JWT) <br>
│  │  ├─ users.js                # Профили, поиск пользователей <br>
│  │  ├─ contacts.js             # Контакты + заявки <br>
│  │  ├─ chats.js                # Чаты/сообщения/участники <br>
│  │  └─ music.js                # Серверная музыка <br>
│  ├─ middleware/ <br>
│  │  └─ auth.js                 # Проверка JWT <br>
│  ├─ schema.sql                 # Создание БД и таблиц <br>
│  ├─ migrate.sql                # Миграции <br>
│  ├─ uploads/                   # Хранилище файлов (аватары/вложения) <br>
│  │  ├─ avatars/ <br>
│  │  └─ chats/ <br>
│  ├─ music/                     # Пример аудио на сервере <br>
│  ├─ .env.example               # Пример переменных окружения <br>
│  ├─ .env                       # Локальные секреты <br>
│  └─ package.json               # Зависимости backend <br>
│ <br>
├─ gradle/                       # Gradle wrapper <br>
├─ build.gradle                  # Gradle настройки проекта <br>
├─ settings.gradle <br>
├─ gradle.properties <br>
├─ local.properties              # Локальный путь к Android SDK <br>
├─ keystore.properties           # Настройки ключа подписи <br>
└─ README.md <br>


Авторы - Дмитрий Поликарпов, Илья Егоров, Александр Крутицкий.
