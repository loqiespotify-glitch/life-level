/* LIFE LEVEL: crash-safe focus sessions with timestamp recovery */
(function(){
'use strict';
const $=id=>document.getElementById(id);
let ticker=null;
function S(){state.system=state.system||{};state.system.focusSession=state.system.focusSession||null;return state.system}
function fmt(sec){return Math.floor(sec/60)+':'+String(sec%60).padStart(2,'0')}
function paint(){let s=S(),f=s.focusSession,clock=$('sx-clock'),btn=$('sx-start');if(!clock||!btn)return;if(!f||f.status!=='running'){clock.textContent=f?.status==='done'?'Завершено':'25:00';btn.disabled=false;return}let left=Math.max(0,Math.ceil((f.endsAt-Date.now())/1000));clock.textContent=fmt(left);btn.disabled=true;if(left<=0)finish(f.id)}
function finish(id){let s=S(),f=s.focusSession;if(!f||f.id!==id||f.status!=='running')return;f.status='done';f.completedAt=Date.now();s.focusMinutes=(s.focusMinutes||0)+f.minutes;state.xp+=Math.max(5,Math.round(f.minutes/5));save();if(ticker){clearInterval(ticker);ticker=null}paint();showToast('Фокус завершён · +'+Math.max(5,Math.round(f.minutes/5))+' XP')}
function start(){let s=S(),f=s.focusSession;if(f?.status==='running'){showToast('Фокус уже идёт');return}let mins=Math.max(5,Math.min(120,Number($('sx-mins')?.value)||25));f={id:String(Date.now())+'-'+Math.random().toString(36).slice(2),status:'running',minutes:mins,startedAt:Date.now(),endsAt:Date.now()+mins*60000};s.focusSession=f;save();paint();if(ticker)clearInterval(ticker);ticker=setInterval(paint,500);showToast('Фокус начат · '+mins+' минут')}
function stop(){let s=S(),f=s.focusSession;if(!f||f.status!=='running'){showToast('Активного сеанса нет');return}f.status='stopped';f.stoppedAt=Date.now();save();if(ticker){clearInterval(ticker);ticker=null}paint();$('sx-clock').textContent='Остановлен';showToast('Сеанс остановлен без награды')}
function init(){let s=S(),f=s.focusSession;if(f?.status==='running'){if(f.endsAt<=Date.now())finish(f.id);else{paint();ticker=setInterval(paint,500)}}}
window.sxFocusStart=start;window.sxFocusStop=stop;
const old=window.render;if(typeof old==='function'&&!old.__sxFocusPersist){window.render=function(){old();if($('systemExpansion'))paint()};window.render.__sxFocusPersist=true}
init();
})();