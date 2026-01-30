
const id = document.getElementById("drawflow");
const editor = new Drawflow(id);
editor.start();

// Custom Node Templates - Natural Language
const templates = {
    trigger: {
        html: `<div class="node-trigger">
                <div class="title"><i class="fas fa-bolt"></i> 메시지 받기</div>
                <div class="content">
                    <span class="helper-text">채팅방에 메시지가 도착하면 이 로직이 시작됩니다.</span>
                </div>
              </div>`,
        inputs: 0,
        outputs: 1
    },
    if: {
        html: `<div class="node-if">
                <div class="title"><i class="fas fa-question-circle"></i> 만약 ~라면</div>
                <div class="content">
                    <span class="helper-text">다음 단어가 포함되어 있나요?</span>
                    <select df-matchType class="node-select-field">
                        <option value="includes">단어를 포함할 때</option>
                        <option value="equals">정확히 일치할 때</option>
                        <option value="startsWith">이 단어로 시작할 때</option>
                    </select>
                    <input type="text" df-value class="node-input-field" placeholder="검색할 단어 입력...">
                    <div style="display:flex; justify-content:space-between; margin-top:12px; font-size:11px; font-weight:600">
                        <span style="color:var(--accent-action)">맞으면 (YES)</span>
                        <span style="color:var(--accent-logic)">틀리면 (NO)</span>
                    </div>
                </div>
              </div>`,
        inputs: 1,
        outputs: 2 // 1: True, 2: False
    },
    reply: {
        html: `<div class="node-reply">
                <div class="title"><i class="fas fa-paper-plane"></i> 답장 보내기</div>
                <div class="content">
                    <span class="helper-text">상대방에게 보낼 메시지 내용을 입력하세요.</span>
                    <textarea df-text class="node-input-field" style="height:60px; resize:none;" placeholder="답장 내용..."></textarea>
                </div>
              </div>`,
        inputs: 1,
        outputs: 0
    },
    http: {
        html: `<div class="node-http">
                <div class="title"><i class="fas fa-cloud-download-alt"></i> 정보 가져오기</div>
                <div class="content">
                   <span class="helper-text">외부 사이트(API)에서 정보를 가져옵니다.</span>
                   <input type="text" df-url class="node-input-field" placeholder="https://api.example.com">
                </div>
              </div>`,
        inputs: 1,
        outputs: 1
    }
};

// Drag and Drop support
function drag(ev) {
    ev.dataTransfer.setData("node", ev.target.getAttribute('data-node'));
}

id.ondrop = function(ev) {
    ev.preventDefault();
    const type = ev.dataTransfer.getData("node");
    const x = ev.clientX - id.getBoundingClientRect().left;
    const y = ev.clientY - id.getBoundingClientRect().top;
    addNodeToDrawflow(type, x, y);
};

id.ondragover = function(ev) {
    ev.preventDefault();
};

function addNodeToDrawflow(type, x, y) {
    const temp = templates[type];
    if (temp) {
        editor.addNode(type, temp.inputs, temp.outputs, x, y, type, { matchType: 'includes' }, temp.html);
    }
}

// Initial Template
function applyTemplate(name) {
    editor.clear();
    if (name === 'hello') {
        const id1 = editor.addNode('trigger', 0, 1, 50, 100, 'trigger', {}, templates.trigger.html);
        const id2 = editor.addNode('if', 1, 2, 300, 100, 'if', { matchType: 'includes', value: '안녕' }, templates.if.html);
        const id3 = editor.addNode('reply', 1, 0, 600, 50, 'reply', { text: '안녕하세요! 반갑습니다.' }, templates.reply.html);
        const id4 = editor.addNode('reply', 1, 0, 600, 250, 'reply', { text: '무슨 말씀인지 잘 모르겠어요.' }, templates.reply.html);
        
        editor.addConnection(id1, id2, 'output_1', 'input_1');
        editor.addConnection(id2, id3, 'output_1', 'input_1');
        editor.addConnection(id2, id4, 'output_2', 'input_1');
    }
}

// Interop with Android
function saveFlow() {
    const data = editor.export();
    if (window.WebviewCallback) {
        WebviewCallback.onSaveFlow(JSON.stringify(data));
    } else {
        console.log("Exported Data:", data);
    }
}

function loadFlow(dataStr) {
    try {
        const data = JSON.parse(dataStr);
        if (data && data.drawflow) {
            editor.import(data);
        } else {
            addNodeToDrawflow('trigger', 50, 150);
        }
    } catch(e) {
        addNodeToDrawflow('trigger', 50, 150);
    }
}
