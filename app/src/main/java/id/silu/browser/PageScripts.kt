package id.silu.browser

object PageScripts {
    const val SHIM = "var unsafeWindow=window;var GM_addStyle=function(c){var s=document.createElement('style');s.textContent=c;(document.head||document.documentElement).appendChild(s);return s};var GM_log=function(){console.log.apply(console,arguments)};"

    const val TEMPLATE = """// ==UserScript==
// @name        Example: tint headings
// @match       https://example.com/*
// ==/UserScript==

GM_addStyle("h1 { color: #3E91FF !important; }");
"""

    const val DESKTOP_JS = """(function(){
if(window.__siluDeskOn)return;window.__siluDeskOn=1;
try{Object.defineProperty(navigator,'userAgentData',{get:function(){return undefined}})}catch(e){}
try{Object.defineProperty(navigator,'platform',{get:function(){return 'Linux x86_64'}})}catch(e){}
var W=1280,busy=false;
function scale(){var sw=screen.width||W;return Math.max(.1,Math.min(1,sw/W))}
function content(z){return 'width='+W+', initial-scale='+z.toFixed(4)+', minimum-scale=.1, maximum-scale=10, user-scalable=yes'}
function fix(){if(busy)return;var h=document.head||document.documentElement;if(!h)return;busy=true;
var all=document.querySelectorAll('meta[name=viewport]'),m=all[0];
for(var i=1;i<all.length;i++)all[i].remove();
if(!m){m=document.createElement('meta');m.name='viewport';h.appendChild(m)}
var c=content(scale());if(m.getAttribute('content')!==c)m.setAttribute('content',c);busy=false}
function kick(){fix();var m=document.querySelector('meta[name=viewport]');if(!m)return;
busy=true;var z=scale();m.setAttribute('content',content(Math.min(1,z+.05)));m.setAttribute('content',content(z));busy=false}
window.__siluKick=kick;
fix();
document.addEventListener('DOMContentLoaded',fix);window.addEventListener('load',fix);
try{new MutationObserver(fix).observe(document,{childList:true,subtree:true,attributes:true,attributeFilter:['content','name']})}catch(e){}
})();"""

    const val DESKTOP_KICK_JS = "window.__siluKick&&window.__siluKick();"

    const val SCROLL_JS = """(function(){
if(window.__siluSc)return;window.__siluSc=1;
function post(v){try{siluScroll.postMessage(v?'1':'0')}catch(e){}}
function chk(ev){var p=ev.composedPath?ev.composedPath():[ev.target],v=false;
for(var i=0;i<p.length;i++){var e=p[i];if(e&&e.nodeType===1&&e.scrollTop>0){v=true;break}}
post(v)}
document.addEventListener('touchstart',chk,{capture:true,passive:true});
})();"""

    const val COSMETIC_JS = """(function(){var s=document.createElement('style');
s.textContent='ins.adsbygoogle,.adsbygoogle,[id^="google_ads_"],[id^="div-gpt-ad"],[id^="ad-slot"],.ad-slot,.ad-banner,.ad-container,iframe[src*="doubleclick.net"],iframe[src*="googlesyndication.com"],iframe[src*="adservice"]{display:none!important}';
(document.head||document.documentElement).appendChild(s)})();"""

    const val FORCE_ZOOM_JS = """(function(){var m=document.querySelector('meta[name=viewport]');if(m){var c=(m.getAttribute('content')||'').replace(/(user-scalable|maximum-scale|minimum-scale)\s*=\s*[^,;]+[,;]?\s*/gi,'').replace(/[,;\s]+${'$'}/,'');m.setAttribute('content',c+(c?', ':'')+'user-scalable=yes, maximum-scale=10')}})();"""

    const val PASSKEY_JS = """(function(){
if(window.__siluPk)return;window.__siluPk=1;
var c=navigator.credentials;if(!c)return;
function tell(){try{siluPk.postMessage('pk')}catch(e){}}
function no(){return Promise.reject(new DOMException('Passkeys are not supported in SiLu Browser.','NotAllowedError'))}
['create','get'].forEach(function(k){var f=c[k];if(typeof f!=='function')return;
c[k]=function(o){if(o&&o.publicKey){if(o.mediation!=='conditional')tell();return no()}return f.apply(c,arguments)}});
try{if(window.PublicKeyCredential){PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable=function(){return Promise.resolve(false)};PublicKeyCredential.isConditionalMediationAvailable=function(){return Promise.resolve(false)}}}catch(e){}
})();"""

    const val NOTIF_JS = """(function(){
if(!window.siluBridge||window.__siluN)return;window.__siluN=1;
var perm='default',cb=null;
function post(o){try{siluBridge.postMessage(JSON.stringify(o))}catch(e){}}
siluBridge.onmessage=function(e){try{var d=JSON.parse(e.data);if(d.t==='perm'){perm=d.v;if(cb){var f=cb;cb=null;f(d.v)}}}catch(x){}};
function N(title,o){o=o||{};this.title=String(title);this.body=String(o.body||'');this.tag=String(o.tag||'');this.onclick=null;this.onshow=null;this.onclose=null;this.onerror=null;
post({t:'show',title:this.title,body:this.body,tag:this.tag});var s=this;setTimeout(function(){if(typeof s.onshow==='function')s.onshow({type:'show'})},0)}
N.prototype.close=function(){};N.prototype.addEventListener=function(){};N.prototype.removeEventListener=function(){};
Object.defineProperty(N,'permission',{get:function(){return perm}});
N.requestPermission=function(f){return new Promise(function(res){cb=function(v){if(typeof f==='function')f(v);res(v)};post({t:'perm'})})};
N.maxActions=2;window.Notification=N;
try{if(window.ServiceWorkerRegistration){ServiceWorkerRegistration.prototype.showNotification=function(t,o){post({t:'show',title:String(t),body:String((o&&o.body)||''),tag:String((o&&o.tag)||'')});return Promise.resolve()}}}catch(e){}
post({t:'state'});
})();"""
}
