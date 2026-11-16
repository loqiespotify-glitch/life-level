/* LIFE LEVEL: adaptive training planner and recovery intelligence */
(function(){
'use strict';
const $=id=>document.getElementById(id),pad=n=>String(n).padStart(2,'0'),date=d=>d.getFullYear()+'-'+pad(d.getMonth()+1)+'-'+pad(d.getDate());
const plans=[
{name:'Фундамент',desc:'Мягкий вход, техника и привычка',days:[['Пн','Лёгкая силовая'],['Вт','Ходьба и мобильность'],['Ср','Силовая'],['Чт','Восстановление'],['Пт','Силовая'],['Сб','Прогулка'],['Вс','Отдых']]},
{name:'Сбалансированный',desc:'Три силовых дня и восстановление',days:[['Пн','Силовая A'],['Вт','Ходьба'],['Ср','Силовая B'],['Чт','Мобильность'],['Пт','Силовая A'],['Сб','Лёгкая активность'],['Вс','Отдых']]},
{name:'Минимум оборудования',desc:'Резинка, собственный вес и ходьба',days:[['Пн','Верх тела'],['Вт','Ходьба'],['Ср','Низ тела и корпус'],['Чт','Отдых'],['Пт','Всё тело'],['Сб','Мобильность'],['Вс','Отдых']]}
];
function data(){state.system=state.system||{};let s=state.system;s.training=s.training||{plan:0,notes:{},sessions:{}};return s.training}
function weekStart(){let d=new Date();d.setDate(d.getDate()-((d.getDay()+6)%7));return date(d)}
function volume(k){let log=state.workouts?.logs?.[k]||{};return Object.values(log).filter(x=>x.done).reduce((a,x)=>a+(x.values||[]).reduce((b,n)=>b+Number(n||0),0),0)}
function workoutCount(k){return Object.values(state.workouts?.logs?.[k]||{}).filter(x=>x.done).length}
function render(){
 let t=data(),p=plans[t.plan]||plans[0],today=new Date(),idx=(today.getDay()+6)%7;
 $('sx-plan-select').value=String(t.plan);$('sx-plan-desc').textContent=p.desc;
 $('sx-plan-days').innerHTML=p.days.map((x,i)=>'<div class="sx-plan-day '+(i===idx?'today':'')+'"><b>'+x[0]+'</b><span>'+x[1]+'</span>'+(i===idx?'<small>СЕГОДНЯ</small>':'')+'</div>').join('');
 let recent=Array.from({length:7},(_,i)=>{let d=new Date();d.setDate(d.getDate()-6+i);let k=date(d);return {k,n:workoutCount(k),v:volume(k)}});
 $('sx-training-week').innerHTML=recent.map(x=>'<div><b>'+x.n+'</b><i><em style="height:'+Math.min(100,x.n/Math.max(1,state.workouts?.exercises?.length||4)*100)+'%"></em></i><small>'+x.k.slice(5)+'</small></div>').join('');
 let sessions=recent.reduce((a,x)=>a+x.n,0),v=recent.reduce((a,x)=>a+x.v,0),last=recent.slice(0,6).reduce((a,x)=>a+x.n,0);
 $('sx-training-stats').innerHTML=[['Упражнения за 7 дней',sessions],['Объём повторений/секунд',v],['Активных дней',recent.filter(x=>x.n>0).length]].map(x=>'<div><b>'+x[1]+'</b><small>'+x[0]+'</small></div>').join('');
 let hint=sessions>=18?'Нагрузка высокая: запланируй восстановление и не увеличивай объём на этой неделе.':sessions>=8?'Регулярная нагрузка: сохраняй текущий объём, повышай только один параметр за раз.':sessions>0?'Начало положено. Сначала закрепи регулярность, затем добавляй повторения.':'Записей тренировок за неделю пока нет. Начни с короткой лёгкой сессии.';
 let logs=Object.entries(state.workouts?.logs||{}).filter(([k,v])=>k>=weekStart()&&Object.values(v).some(x=>x.done)).length;
 $('sx-recovery-advice').textContent=hint+(logs>=4?' Есть несколько тренировочных дней: чередуй нагрузку на разные группы мышц.':'');
 let completed=Object.keys(t.sessions).filter(k=>t.sessions[k]).length;$('sx-training-note').value=t.notes[date(today)]||'';
}
window.sxPlanChange=v=>{data().plan=Math.max(0,Math.min(plans.length-1,Number(v)||0));save();render();};
window.sxTrainingNote=()=>{data().notes[date(new Date())]=$('sx-training-note').value.slice(0,400);save();showToast('Заметка сохранена')};
window.sxSessionDone=()=>{let t=data(),k=date(new Date());if(t.sessions[k]){showToast('Сегодняшняя сессия уже отмечена');return}t.sessions[k]=true;state.xp+=15;save();render();showToast('Тренировочная сессия отмечена · +15 XP')};
function mount(){let p=$('systemExpansion');if(!p)return;let h='<h3>ПЛАНИРОВЩИК ТРЕНИРОВОК</h3><section class="sx-panel"><label>Программа<select id="sx-plan-select" onchange="sxPlanChange(this.value)"><option value="0">Фундамент</option><option value="1">Сбалансированный</option><option value="2">Минимум оборудования</option></select></label><small id="sx-plan-desc"></small><div id="sx-plan-days" class="sx-plan-days"></div><div id="sx-recovery-advice" class="sx-recovery-advice"></div><div id="sx-training-week" class="sx-water-week"></div><div id="sx-training-stats" class="sx-metrics"></div><label>Заметка о тренировке<textarea id="sx-training-note" rows="2" placeholder="Самочувствие, сложность, что изменить..."></textarea></label><button onclick="sxTrainingNote()">Сохранить заметку</button><button onclick="sxSessionDone()">Отметить тренировочную сессию +15 XP</button><small>Отметка сессии не заменяет запись подходов во вкладке «Тренировка».</small></section>';let anchor=p.querySelector('#sx-library');if(anchor)anchor.parentElement.insertAdjacentHTML('afterend',h);else p.insertAdjacentHTML('beforeend',h);let st=document.createElement('style');st.textContent='.sx-plan-days{display:grid;grid-template-columns:repeat(2,1fr);gap:6px;margin:12px 0}.sx-plan-day{padding:8px;background:#102234;border:1px solid #2b465c;border-radius:9px;display:grid;grid-template-columns:30px 1fr;gap:5px;align-items:center}.sx-plan-day.today{border-color:#85d9ff;background:#17334a}.sx-plan-day small{grid-column:2;color:#9ceaff;font-size:9px}.sx-recovery-advice{margin:10px 0;padding:10px;background:#102234;border-left:3px solid #6cbfe6;border-radius:6px;font-size:12px;color:#c5d7e5}';document.head.appendChild(st);render()}
const old=window.render;if(typeof old==='function'&&!old.__sxTraining){window.render=function(){old();if($('systemExpansion'))render()};window.render.__sxTraining=true}
mount();
})();