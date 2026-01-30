// Node.js runtime initialization for nodejs-mobile
console.log("NODE: Astral Bot Engine started (nodejs-mobile)");

// Load nodejs-mobile bridge
var nodejsMobile = require('nodejs-mobile');

// Modules
var vm = require('vm');
var fs = require('fs');

// Global Bot Context
var botContext = {
    bot: {
        _plugins: [],
        _prefixes: ['!', '/'],
        plugin: function (match, fn) {
            if (typeof match === 'function') {
                fn = match;
                match = null;
            }
            this._plugins.push({ match: match, fn: fn });
        },
        command: function (cmd, fn) {
            var self = this;
            this.plugin(function(msg) {
                if (!msg.content.startsWith(cmd)) return;
                msg.command = cmd;
                msg.args = msg.content.substring(cmd.length).trim().split(/\s+/);
                fn(msg);
            });
        }
    },
    native: {
        call: function(module) {
            var args = Array.prototype.slice.call(arguments, 1);
            return { ok: true, value: null };
        }
    },
    console: {
        log: function(msg) { sendToAndroid({ type: 'log', level: 'info', message: String(msg) }) },
        error: function(msg) { sendToAndroid({ type: 'log', level: 'error', message: String(msg) }) },
        debug: function(msg) { sendToAndroid({ type: 'log', level: 'debug', message: String(msg) }) },
    },
    require: require,
    process: process,
    fs: fs
};

function sendToAndroid(obj) {
    try {
        nodejsMobile.channel.send(JSON.stringify(obj));
    } catch (err) {
        console.log("NODE: Failed to send message: " + err.message);
    }
}

function handleEval(payload) {
    var replies = [];
    var replier = {
        reply: function(text) { replies.push(String(text)) }
    };

    botContext.replier = replier;
    botContext.msg = payload.data;

    try {
        var script = new vm.Script(payload.code);
        var contextObj = {};
        for (var key in botContext) { 
            contextObj[key] = botContext[key]; 
        }
        
        var context = vm.createContext(contextObj);
        script.runInContext(context);
        sendToAndroid({ type: 'response', id: payload.id, result: replies });
    } catch (e) {
        sendToAndroid({ type: 'log', level: 'error', message: "Script Error: " + e.message });
        sendToAndroid({ type: 'response', id: payload.id, result: [] });
    }
}

// Listen for messages from Android
nodejsMobile.channel.on('message', function(msg) {
    try {
        var payload = JSON.parse(msg);
        
        if (payload.type === 'eval') {
            handleEval(payload);
        } else if (payload.type === 'npm_install') {
            sendToAndroid({ type: 'log', message: 'NPM install not implemented', level: 'warning' });
        } else if (payload.type === 'npm_list') {
            sendToAndroid({ type: 'response', id: payload.id, result: [] });
        }
    } catch (e) {
        sendToAndroid({ type: 'log', level: 'error', message: "Payload Error: " + e.message });
    }
});

// Notify Android that runtime is ready
sendToAndroid({ type: 'ready' });
console.log("NODE: Runtime ready, waiting for commands");
