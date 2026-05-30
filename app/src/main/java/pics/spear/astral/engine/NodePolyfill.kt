package pics.spear.astral.engine

/** Node.js API polyfill injected into every WebView runtime. */
object NodePolyfill {
    val HTML: String
        get() = """
<!DOCTYPE html><html><body>
<script>
"use strict";

// ── Global bot object ──────────────────────────────────
var bot = {
    reply: function(m) { __bridge.reply(bot._room, String(m)); },
    toast: function(t) { __bridge.toast(String(t)); },
    vibrate: function(m) { __bridge.vibrate(Number(m)||200); },
    log: function(t) { __bridge.log(bot._name, String(t)); },
    getName: function() { return bot._name; },
    onMessage: function(f) { bot._onMessage = f; },
    onCommand: function(c,f) { bot._commands = bot._commands||{}; bot._commands[String(c)]=f; },
    setPrefix: function(p) { bot._prefix = typeof p=='string'?[String(p)]:p.map(String); },
    _room: '',
    _name: '',
    _prefix: ['!','/'],
    _onMessage: null,
    _commands: {}
};

// ── Node.js module system ──────────────────────────────
var __modules = {};
function require(id) {
    if (__modules[id]) return __modules[id].exports;
    // built-in shims
    if (id === 'events') {
        return { EventEmitter: function(){this._l={};this.on=function(e,f){(this._l[e]=this._l[e]||[]).push(f);return this;};this.emit=function(e){var a=Array.prototype.slice.call(arguments,1);(this._l[e]||[]).forEach(function(f){f.apply(null,a)});return this;}} };
    }
    if (id === 'util') {
        return { inherits: function(c,s){c.prototype=Object.create(s.prototype);c.prototype.constructor=c;} };
    }
    if (id === 'path') {
        return { join: function(){return Array.prototype.join.call(arguments,'/')}, dirname: function(p){return p.substring(0,p.lastIndexOf('/'))||'.'}, basename: function(p){return p.substring(p.lastIndexOf('/')+1)} };
    }
    var err = new Error('Cannot find module "'+id+'"');
    err.code = 'MODULE_NOT_FOUND';
    throw err;
}
var module = { exports: {} };
var exports = module.exports;

// ── process ────────────────────────────────────────────
var process = {
    env: {},
    argv: [],
    platform: 'android',
    arch: 'arm64',
    version: 'v20.0.0',
    versions: { node: '20.0.0', v8: '12.0' },
    nextTick: function(f) { setTimeout(f,0); },
    hrtime: function(t){var n=Date.now();return t?[n-t[0],0]:[n,0];},
    cwd: function(){return '/';},
    exit: function(){}
};

// ── Buffer ─────────────────────────────────────────────
function Buffer(arg, enc) {
    if (typeof arg === 'number') { this.length=arg; for(var i=0;i<arg;i++)this[i]=0; return; }
    if (typeof arg === 'string') {
        var d = enc==='base64' ? atob(arg) : arg;
        this.length = d.length;
        for(var i=0;i<d.length;i++) this[i]=d.charCodeAt(i);
        return;
    }
    if (Array.isArray(arg)) { this.length=arg.length; for(var i=0;i<arg.length;i++)this[i]=arg[i]; }
}
Buffer.from = function(v,e){return new Buffer(v,e)};
Buffer.alloc = function(s){return new Buffer(s)};
Buffer.concat = function(l){var t=new Buffer(l.reduce(function(a,b){return a+b.length},0)),o=0;l.forEach(function(b){b.copy(t,o);o+=b.length});return t};
Buffer.prototype.toString = function(e){var r='';for(var i=0;i<this.length;i++)r+=String.fromCharCode(this[i]);return e==='base64'?btoa(r):r};
Buffer.prototype.copy = function(t,s,d,e){d=d||0;e=e||this.length;for(var i=d;i<e;i++)t[i]=this[s?i-s:i];};
Buffer.prototype.slice = function(s,e){return Buffer.from(Array.prototype.slice.call(this,s,e))};

// ── Timers ──────────────────────────────────────────────
var __tid=0;
function setTimeout(fn, ms) { __tid++; var id=__tid; var _fn=typeof fn==='string'?new Function(fn):fn; __timeouts[id]=setTimeout(function(){delete __timeouts[id];_fn();},ms||0); return id; }
function clearTimeout(id) { if(__timeouts[id]){clearTimeout(__timeouts[id]);delete __timeouts[id]} }
function setInterval(fn, ms) { __tid++; var id=__tid; var _fn=typeof fn==='string'?new Function(fn):fn; __intervals[id]=setInterval(_fn,ms||0); return id; }
function clearInterval(id) { if(__intervals[id]){clearInterval(__intervals[id]);delete __intervals[id]} }
var __timeouts={},__intervals={};

// ── console ────────────────────────────────────────────
var console = {
    log: function() { var a=Array.prototype.slice.call(arguments).map(function(x){return typeof x==='object'?JSON.stringify(x):String(x)}); __bridge.log(bot._name, a.join(' ')); },
    warn: function(m) { __bridge.log(bot._name, 'WARN: '+m); },
    error: function(m) { __bridge.log(bot._name, 'ERROR: '+m); },
};

// ── Global constructors ────────────────────────────────
var global = this;
var globalThis = this;
var URL = function(u,b){this.href=u;this.hostname='';this.pathname=u;};
var TextEncoder = function(){this.encode=function(s){var a=[];for(var i=0;i<s.length;i++)a.push(s.charCodeAt(i));return Buffer.from(a)};};
var TextDecoder = function(){this.decode=function(b){var r='';for(var i=0;i<b.length;i++)r+=String.fromCharCode(b[i]);return r};};

<\/script>
</body></html>
        """.trimIndent()
}
