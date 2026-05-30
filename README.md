# ✦ Astral

**vibe coding framework for KakaoTalk bots**

Astral은 카카오톡 봇을 만들기 위한 안드로이드 경량 프레임워크입니다.  
**JavaScript (Node.js)** 또는 **Python**으로 스크립트를 작성하거나, **Flow 편집기**로 시각적으로 제작할 수 있습니다.

> "let the code flow through you" — astral

---

## ✨ 특징

- **제로 헤비 의존성** — WebView V8 (시스템 내장) + Jython (순수 Java Python)
- **듀얼 런타임** — Node.js API (require/module/Buffer/process) + Python 2.7
- **비주얼 플로우 빌더** — 드래그 앤 드롭 노드 편집기, JS로 컴파일
- **REST API** — 내장 HTTP 서버 (포트 설정 가능, 인증 토큰, CORS)
- **플러그인 시스템** — `.atlp` 포맷, 훅 라이프사이클 지원
- **카카오톡 연동** — NotificationListenerService + 자동 응답
- **프리미엄 UI** — 유리주의, 파티클 애니메이션, Material 3
- **바이브 코딩** — 창의적 프롬프트, 영감 인용구, 펄스 UI

---

## 📦 아키텍처

```
app/
├── engine/
│   ├── ScriptEngine.kt      # 멀티-랭귀지 디스패처
│   ├── NodeRuntime.kt       # WebView V8 + Node.js 폴리필
│   ├── NodePolyfill.kt      # require/module/Buffer/process/timers
│   ├── PythonRuntime.kt     # Jython 기반 Python 2.7
│   ├── Runtime.kt           # Runtime 인터페이스 + enum
│   ├── FlowCompiler.kt      # 비주얼 플로우 → JavaScript
│   ├── ApiServer.kt         # REST API (com.sun.net.httpserver)
│   ├── PluginManager.kt     # .atlp 플러그인 로더 + 훅 시스템
│   └── BotApi.kt            # 이벤트 버스 + 디바이스 액션
├── model/                   # Bot, Flow, ChatMessage, LogEntry
├── service/                 # NotificationListener + 응답
├── store/                   # JSON 파일 영속성
└── ui/                      # Compose 화면 + 컴포넌트
```

---

## 🚀 시작하기

### 요구사항
- Android 8.0+ (API 26)
- 카카오톡 설치
- 알림 접근 권한 활성화

### 빌드
```bash
git clone https://github.com/spear34000/astral.git
cd astral
./gradlew assembleDebug
```

### 첫 번째 봇
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

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/v1/ping` | 상태 확인 |
| GET | `/api/v1/bots` | 봇 목록 |
| POST | `/api/v1/bots` | 봇 생성 `{name, language}` |
| GET | `/api/v1/bots/:id` | 봇 상세 |
| POST | `/api/v1/bots/:id/toggle` | 봇 켜기/끄기 |
| DELETE | `/api/v1/bots/:id` | 봇 삭제 |
| POST | `/api/v1/flows/compile` | 플로우 → 코드 컴파일 |
| POST | `/api/v1/message` | 메시지 전송 `{room, message}` |
| GET | `/api/v1/plugins` | 플러그인 목록 |
| POST | `/api/v1/server/restart` | API 서버 재시작 |

기본 포트: `9345` · 인증: `Authorization: Bearer <token>` (선택)

---

## 🔌 플러그인 포맷 (.atlp)

`.atlp`는 ZIP 패키지입니다:

```
example.atlp
├── plugin.json      # 매니페스트
└── main.js          # 플러그인 코드
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

### 지원 훅
`APP_START`, `APP_STOP`, `BOT_CREATED`, `BOT_DELETED`,  
`BOT_ENABLED`, `BOT_DISABLED`, `KAKAO_MESSAGE_RECEIVED`, `FLOW_COMPILED`

---

## 🎨 Bot API

| 메서드 | 설명 |
|--------|------|
| `bot.reply(text)` | 현재 채팅방에 메시지 전송 |
| `bot.toast(text)` | Android 토스트 표시 |
| `bot.vibrate(ms)` | 디바이스 진동 |
| `bot.log(text)` | Astral 로그에 기록 |
| `bot.getName()` | 봇 이름 반환 |
| `bot.onMessage(fn)` | 모든 메시지 처리 |
| `bot.onCommand(name, fn)` | 접두사 명령어 처리 |
| `bot.setPrefix([p])` | 명령어 접두사 설정 (기본: `!`, `/`) |

**Node.js 전용:**
- `require('events')`, `require('util')`, `require('path')`
- `Buffer`, `process`, `setTimeout`, `setInterval`

---

## 🧠 비주얼 플로우 노드

| 타입 | 색상 | 설명 |
|------|------|------|
| On Message | 파랑 | 키워드 감지 |
| On Command | 파랑 | `!명령어` 감지 |
| Reply | 초록 | 메시지 전송 |
| Toast | 노랑 | 토스트 표시 |
| Delay | 빨강 | 대기 (ms) |
| Log | 보라 | 로그 출력 |
| Condition | 분홍 | 조건 분기 |

---

## 📄 라이선스

GNU General Public License v3.0 — [LICENSE](LICENSE)

이 프로그램은 자유 소프트웨어입니다. Free Software Foundation이 발표한 GNU General Public License v3 (또는 이후 버전) 하에 재배포 및 수정할 수 있습니다.
