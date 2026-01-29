# Nova Bot 개발 레퍼런스

Nova는 JavaScript 또는 Python으로 메신저 봇을 개발하고 실행할 수 있는 Android 기반 플랫폼입니다.

## 1. 봇 API (Python)

봇 코드는 `bot`과 `Context`라는 두 개의 주요 객체와 상호작용합니다.

### `bot` 객체

`bot` 객체는 명령어, 이벤트 핸들러, 접두사 등을 등록하는 데 사용됩니다.

- **`bot.command(name, handler)`**: 특정 명령어에 대한 핸들러 함수를 등록합니다.
  - `name` (str 또는 list): 명령어 이름. 리스트로 여러 이름을 한 번에 등록할 수 있습니다.
  - `handler` (function): 명령어가 수신될 때 실행될 함수. `Context` 객체를 인자로 받습니다.

- **`bot.on(event, handler)`**: 특정 이벤트에 대한 핸들러를 등록합니다. 현재 `"message"` 이벤트만 지원됩니다.
  - `event` (str): 이벤트 이름. (예: `"message"`)
  - `handler` (function): 메시지가 수신될 때마다 실행될 함수. `Context` 객체를 인자로 받습니다.

- **`bot.prefix(prefixes)`**: 명령어 접두사를 설정합니다. 기본값은 `["!", "/"]`입니다.
  - `prefixes` (str 또는 list): 새로운 접두사. 여러 개를 리스트로 설정할 수 있습니다.

### `Context` 객체

`Context` 객체는 수신된 메시지에 대한 모든 정보를 담고 있으며, 응답을 보내는 데 사용됩니다.

- **`ctx.reply(message)`**: 현재 메시지가 수신된 채팅방으로 응답 메시지를 보냅니다.
  - `message` (str): 보낼 메시지 내용.

- **`ctx.msg`** 또는 **`ctx.content`**: 수신된 메시지의 내용 (문자열).

- **`ctx.room`**: 메시지가 수신된 채팅방의 이름 (문자열).

- **`ctx.sender`**: 메시지를 보낸 사람의 이름 (문자열).

- **`ctx.is_group_chat`**: 그룹 채팅 여부 (불리언).

## 2. 패키지 관리

Python 봇 개발 시 필요한 외부 라이브러리는 `pip`를 사용하여 터미널에서 직접 설치할 수 있습니다. Nova는 자동으로 가상환경(`venv`)을 구성하므로, 별도의 활성화 과정 없이 바로 `pip` 명령을 사용하면 됩니다.

```sh
# requests 라이브러리 설치 예시
pip install requests
```

## 3. 기본 예제 코드

다음은 `!ping` 명령에 `pong!`으로 응답하고, 모든 메시지를 로그로 출력하는 간단한 예제입니다.

```python
# bot 객체는 코드 실행 시 자동으로 생성되어 있습니다.

def handle_ping(ctx):
    ctx.reply("pong!")

# "!ping" 또는 "/ping" 명령이 오면 handle_ping 함수 실행
bot.command("ping", handle_ping)

# 모든 메시지를 로그로 출력하는 이벤트 핸들러
def log_message(ctx):
    print(f"[{ctx.room}] {ctx.sender}: {ctx.msg}")

bot.on("message", log_message)

# 접두사를 '#'으로만 사용하고 싶을 때
# bot.prefix("#")
```
