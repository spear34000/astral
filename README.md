# ✦ Astral

**vibe coding framework for KakaoTalk bots**

Astral is a lightweight, no-code Android framework for building KakaoTalk bots.  
Write scripts in **JavaScript (Node.js)** or **Python**, or build visually with the **Flow editor**.

> "let the code flow through you" — astral

---

## ✨ Features

- **Zero heavy dependencies** — uses system WebView V8 for JS, Jython for Python
- **Dual runtime** — Node.js API (require/module/Buffer/process) + Python 2.7
- **Visual Flow Builder** — drag-and-drop node editor, compiles to JS
- **REST API** — built-in HTTP server (configurable port, auth token, CORS)
- **Plugin system** — `.atlp` format plugins with hook lifecycle
- **KakaoTalk integration** — NotificationListenerService + auto-reply
- **Premium cosmic UI** — glassmorphism, particle animations, Material 3
- **Vibe Coding** — creative prompts, inspirational quotes, pulsing UI

---

## 📦 Architecture

```
app/
├── engine/
│   ├── ScriptEngine.kt      # Multi-runtime dispatcher
│   ├── NodeRuntime.kt       # WebView V8 + Node.js polyfill
│   ├── NodePolyfill.kt      # require/module/Buffer/process/timers
│   ├── PythonRuntime.kt     # Jython-based Python 2.7
│   ├── Runtime.kt           # Runtime interface + enum
│   ├── FlowCompiler.kt      # Visual flow → JavaScript
│   ├── ApiServer.kt         # REST API (com.sun.net.httpserver)
│   ├── PluginManager.kt     # .atlp plugin loader + hook system
│   └── BotApi.kt            # Event bus + device actions
├── model/                   # Bot, Flow, ChatMessage, LogEntry
├── service/                 # NotificationListener + reply
├── store/                   # JSON file persistence
└── ui/                      # Compose screens + components
```

---

## 🚀 Getting Started

### Requirements
- Android 8.0+ (API 26)
- KakaoTalk installed
- Notification access enabled

### Build
```bash
git clone https://github.com/spear34000/astral.git
cd astral
./gradlew assembleDebug
```

### First Bot
```javascript
// JavaScript / Node.js
bot.onMessage(function(room, sender, msg, isGroup) {
    if (msg.includes('hello')) {
        bot.reply('hi there! ✦');
    }
});
```

```python
# Python
def on_message(room, sender, msg, is_group):
    if 'hello' in msg:
        bot.reply('hi there! ✦')

bot.on_message(on_message)
```

---

## 🌐 REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/ping` | Health check |
| GET | `/api/v1/bots` | List bots |
| POST | `/api/v1/bots` | Create bot `{name, language}` |
| GET | `/api/v1/bots/:id` | Get bot |
| POST | `/api/v1/bots/:id/toggle` | Toggle bot |
| DELETE | `/api/v1/bots/:id` | Delete bot |
| POST | `/api/v1/flows/compile` | Compile flow → code |
| POST | `/api/v1/message` | Send reply `{room, message}` |
| GET | `/api/v1/plugins` | List plugins |
| POST | `/api/v1/server/restart` | Restart API server |

Default port: `9345` · Auth: optional `Authorization: Bearer <token>`

---

## 🔌 Plugin Format (.atlp)

`.atlp` is a ZIP package:

```
example.atlp
├── plugin.json      # manifest
└── main.js          # plugin code
```

### plugin.json
```json
{
  "id": "example",
  "name": "Example Plugin",
  "version": "1.0.0",
  "hooks": ["APP_START", "KAKAO_MESSAGE_RECEIVED"],
  "main": "main.js"
}
```

### Available Hooks
`APP_START`, `APP_STOP`, `BOT_CREATED`, `BOT_DELETED`,  
`BOT_ENABLED`, `BOT_DISABLED`, `KAKAO_MESSAGE_RECEIVED`, `FLOW_COMPILED`

---

## 🎨 Bot API

| Method | Description |
|--------|-------------|
| `bot.reply(text)` | Send message to current chat |
| `bot.toast(text)` | Show Android toast |
| `bot.vibrate(ms)` | Vibrate device |
| `bot.log(text)` | Write to Astral logs |
| `bot.getName()` | Get bot name |
| `bot.onMessage(fn)` | Handle all messages |
| `bot.onCommand(name, fn)` | Handle prefixed commands |
| `bot.setPrefix([p])` | Set command prefixes (default: `!`, `/`) |

**Node.js only:**
- `require('events')`, `require('util')`, `require('path')`
- `Buffer`, `process`, `setTimeout`, `setInterval`

---

## 🧠 Visual Flow Nodes

| Type | Color | Purpose |
|------|-------|---------|
| On Message | Blue | Trigger on keyword |
| On Command | Blue | Trigger on `!command` |
| Reply | Green | Send message |
| Toast | Yellow | Show toast |
| Delay | Red | Wait (ms) |
| Log | Purple | Log output |
| Condition | Pink | If/else logic |

---

## 📄 License

GNU General Public License v3.0 — see [LICENSE](LICENSE)

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
