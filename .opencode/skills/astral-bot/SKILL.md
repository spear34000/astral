---
name: astral-bot
description: Use when creating, editing, debugging, or explaining Astral KakaoTalk bots. Covers bot API, script syntax, flow compilation, REST API usage, and plugin development. Use ONLY for Astral bot framework questions, not for general programming help.
---

# Astral Bot — 봇 만들기 도우미

Astral은 카카오톡 봇을 만드는 안드로이드 프레임워크입니다.

## 봇 API 요약

```javascript
// JavaScript / Node.js
bot.reply('메시지')       // 답장 보내기
bot.toast('텍스트')       // 토스트 띄우기
bot.vibrate(200)         // 진동
bot.log('로그')           // 로그 기록
bot.getName()            // 봇 이름 반환
bot.onMessage(fn)        // 모든 메시지 처리
bot.onCommand('name',fn) // !명령어 처리
bot.setPrefix(['!','/']) // 접두사 설정
```

```python
# Python
bot.reply('메시지')
bot.toast('텍스트')
bot.log('로그')
bot.get_name()
bot.on_message(fn)
bot.on_command('name', fn)
bot.set_prefix(['!', '/'])
```

## 자주 하는 실수

- `bot.onMessage`를 함수 호출하지 말고 전달만: `bot.onMessage(fn)` (O), `bot.onMessage(fn())` (X)
- Python은 `bot.on_message` (snake_case) 사용
- `bot.reply`를 호출하기 전에 메시지가 와야 함 (onMessage 콜백 안에서 호출)

## REST API (외부 제어)

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/v1/bots` | 봇 목록 |
| POST | `/api/v1/bots` | 봇 생성 `{name, language}` |
| POST | `/api/v1/message` | 메시지 전송 `{room, message}` |
| POST | `/api/v1/flows/compile` | 플로우 → 코드 변환 |

## 기본 템플릿

새 봇을 만들면 아래 코드가 자동으로 들어갑니다. onMessage로 시작하고 onCommand로 명령어를 추가하는 패턴을 추천합니다.

## 플러그인 (.atlp)

ZIP 구조: `plugin.json` + `main.js`. manifest의 `hooks` 필드로 이벤트 구독.

## 플로우 → 코드

비주얼 플로우는 FlowCompiler를 통해 JavaScript로 변환됩니다. 각 노드는 순차적으로 실행되며, 조건 분기는 if문으로 변환됩니다.
