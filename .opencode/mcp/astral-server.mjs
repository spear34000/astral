#!/usr/bin/env node

/**
 * Astral MCP Server — Vibe Coding for KakaoTalk Bots
 *
 * Connects opencode / Claude to a running Astral REST API.
 * Set env: ASTRAL_HOST (default 127.0.0.1), ASTRAL_PORT (default 9345), ASTRAL_TOKEN (optional)
 *
 * Protocol: MCP stdio (JSON-RPC over stdin/stdout)
 */

const host = process.env.ASTRAL_HOST || '127.0.0.1';
const port = process.env.ASTRAL_PORT || '9345';
const token = process.env.ASTRAL_TOKEN || '';
const BASE = `http://${host}:${port}/api/v1`;

// ── Helpers ────────────────────────────────────────────

async function api(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${BASE}${path}`, {
    method, headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  return res.json();
}

function json(msg) {
  process.stdout.write(JSON.stringify(msg) + '\n');
}

// ── MCP handler ────────────────────────────────────────

const TOOLS = {
  ping: {
    description: 'Check if Astral API server is running',
    inputSchema: { type: 'object', properties: {} },
    handler: async () => {
      try {
        const res = await api('GET', '/ping');
        return { content: [{ type: 'text', text: JSON.stringify(res, null, 2) }] };
      } catch (e) {
        return { content: [{ type: 'text', text: `❌ Astral API에 연결할 수 없습니다 (${BASE})` }], isError: true };
      }
    },
  },

  list_bots: {
    description: 'List all bots registered in Astral',
    inputSchema: {
      type: 'object',
      properties: {},
    },
    handler: async () => {
      const res = await api('GET', '/bots');
      const bots = res.bots || [];
      if (bots.length === 0) {
        return { content: [{ type: 'text', text: '🤖 등록된 봇이 없습니다. 봇을 만들어보세요!' }] };
      }
      const lines = bots.map(b =>
        `• ${b.name} (${b.language}) ${b.enabled ? '✅ 켜짐' : '⛔ 꺼짐'}  ${b.running ? '⚡ 실행 중' : '💤'}  id: ${b.id}`
      );
      return { content: [{ type: 'text', text: `📋 봇 목록 (${bots.length}개)\n${lines.join('\n')}` }] };
    },
  },

  create_bot: {
    description: 'Create a new bot',
    inputSchema: {
      type: 'object',
      properties: {
        name: { type: 'string', description: 'Bot name' },
        language: { type: 'string', enum: ['javascript', 'python'], description: 'Script language (default: javascript)' },
      },
      required: ['name'],
    },
    handler: async (args) => {
      const res = await api('POST', '/bots', { name: args.name, language: args.language || 'javascript' });
      if (res.error) return { content: [{ type: 'text', text: `❌ ${res.error}` }], isError: true };
      return { content: [{ type: 'text', text: `✅ 봇 "${args.name}" 생성 완료! id: ${res.bot}\n\n봇을 켜고 카톡에서 테스트해보세요.` }] };
    },
  },

  toggle_bot: {
    description: 'Enable or disable a bot by ID',
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'string', description: 'Bot ID (from list_bots)' },
      },
      required: ['id'],
    },
    handler: async (args) => {
      const res = await api('POST', `/bots/${args.id}/toggle`);
      return {
        content: [{ type: 'text', text: `🔄 봇 상태 변경됨: ${res.enabled ? '✅ 켜짐' : '⛔ 꺼짐'}` }],
      };
    },
  },

  delete_bot: {
    description: 'Delete a bot by ID',
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'string', description: 'Bot ID (from list_bots)' },
      },
      required: ['id'],
    },
    handler: async (args) => {
      const res = await api('DELETE', `/bots/${args.id}`);
      return { content: [{ type: 'text', text: res.success ? '🗑️ 봇 삭제됨' : `❌ ${res.error}` }] };
    },
  },

  compile_flow: {
    description: 'Compile a visual flow JSON to JavaScript code',
    inputSchema: {
      type: 'object',
      properties: {
        flow: {
          type: 'object',
          description: 'Flow object with nodes and connections',
        },
      },
      required: ['flow'],
    },
    handler: async (args) => {
      const res = await api('POST', '/flows/compile', args.flow);
      if (res.error) return { content: [{ type: 'text', text: `❌ ${res.error}` }], isError: true };
      return {
        content: [
          { type: 'text', text: '✅ 플로우 → 코드 변환 완료!' },
          { type: 'text', text: '```javascript\n' + (res.code || '') + '\n```' },
        ],
      };
    },
  },

  list_plugins: {
    description: 'List registered .atlp plugins',
    inputSchema: { type: 'object', properties: {} },
    handler: async () => {
      const res = await api('GET', '/plugins');
      const plugins = res.plugins || [];
      if (plugins.length === 0) return { content: [{ type: 'text', text: '📦 설치된 플러그인이 없습니다.' }] };
      const lines = plugins.map(p => `• ${p.name || p.id} v${p.version || '?'}`);
      return { content: [{ type: 'text', text: `📦 플러그인 (${plugins.length}개)\n${lines.join('\n')}` }] };
    },
  },

  send_message: {
    description: 'Send a reply to a KakaoTalk chat room',
    inputSchema: {
      type: 'object',
      properties: {
        room: { type: 'string', description: 'Chat room identifier' },
        message: { type: 'string', description: 'Message text to send' },
      },
      required: ['room', 'message'],
    },
    handler: async (args) => {
      const res = await api('POST', '/message', { room: args.room, message: args.message });
      return { content: [{ type: 'text', text: res.success ? '📤 메시지 전송됨' : `❌ ${res.error}` }] };
    },
  },
};

const TOOL_NAMES = Object.keys(TOOLS);

// ── JSON-RPC dispatch ─────────────────────────────────
async function handleRequest(msg) {
  const id = msg.id;
  const respond = (result, error) => json({ jsonrpc: '2.0', id, ...(result ? { result } : {}), ...(error ? { error } : {}) });

  switch (msg.method) {
    case 'initialize':
      respond({
        protocolVersion: '2024-11-05',
        capabilities: { tools: {} },
        serverInfo: { name: 'astral-mcp', version: '1.0.0' },
      });
      break;

    case 'notifications/initialized':
      // no response needed
      break;

    case 'tools/list':
      respond({
        tools: TOOL_NAMES.map(name => ({
          name,
          description: TOOLS[name].description,
          inputSchema: TOOLS[name].inputSchema,
        })),
      });
      break;

    case 'tools/call': {
      const tool = TOOLS[msg.params.name];
      if (!tool) {
        respond(null, { code: -32601, message: `Unknown tool: ${msg.params.name}` });
        return;
      }
      try {
        const result = await tool.handler(msg.params.arguments || {});
        respond(result);
      } catch (e) {
        respond(null, { code: -32603, message: e.message });
      }
      break;
    }

    case 'ping':
      respond({});
      break;

    default:
      respond(null, { code: -32601, message: `Unknown method: ${msg.method}` });
  }
}

// ── Main loop ──────────────────────────────────────────
let buffer = '';
process.stdin.on('data', (chunk) => {
  buffer += chunk.toString();
  const lines = buffer.split('\n');
  buffer = lines.pop() || '';
  for (const line of lines) {
    if (line.trim()) {
      try {
        const msg = JSON.parse(line);
        handleRequest(msg);
      } catch (e) {
        // ignore malformed lines
      }
    }
  }
});

process.stdin.on('end', () => process.exit(0));
