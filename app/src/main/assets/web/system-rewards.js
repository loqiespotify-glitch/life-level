/* LIFE LEVEL: offline hunter rewards and equipment.
   Original implementation. Progression concepts adapted from THE SYSTEM (MIT);
   no source code or artwork from the second reference project is used. */
(function () {
  'use strict';
  const $ = id => document.getElementById(id);
  const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const slots = ['weapon','armor','crown','accessory','aura','title','background'];
  const slotNames = {weapon:'Оружие',armor:'Броня',crown:'Корона',accessory:'Аксессуар',aura:'Аура',title:'Титул',background:'Фон',scroll:'Свиток'};
  const tiers = {
    BRONZE:{label:'Бронзовый',color:'#b98b62',chance:[72,23,5]},
    SILVER:{label:'Серебряный',color:'#b8d3e8',chance:[42,43,15]},
    GOLD:{label:'Золотой',color:'#f2cf78',chance:[20,45,35]}
  };
  const loot = {
    common:[
      ['scroll','Свиток пробуждения','Короткое напоминание о выбранном пути'],
      ['title','Железная воля','Титул за последовательность'],
      ['accessory','Печать упорства','Знак завершённых испытаний'],
      ['accessory','Боевой шрам','Косметический знак пройденного пути']
    ],
    rare:[
      ['weapon','Стальной резонатор','Оружие с холодным голубым свечением'],
      ['armor','Панцирь сумрачного стража','Усиленная экипировка охотника'],
      ['background','Зал испытаний','Тема интерфейса охотника'],
      ['title','Покоритель теней','Титул за устойчивый прогресс']
    ],
    epic:[
      ['weapon','Клинок безмолвной звезды','Редкий клинок высшего ранга'],
      ['armor','Доспех глубокой тени','Комплект для тех, кто прошёл долгий путь'],
      ['crown','Корона восходящего','Знак высшего испытания'],
      ['aura','Аура монарха','Редкий визуальный эффект'],
      ['title','Наследник системы','Титул высшего ранга']
    ]
  };
  const lootTables = {
    BRONZE:[['common',60],['rare',30],['epic',10]],
    SILVER:[['common',25],['rare',55],['epic',20]],
    GOLD:[['common',10],['rare',40],['epic',50]]
  };
  function store(){
    state.system=state.system||{};
    const s=state.system;
    s.rewards=s.rewards||{seenLevel:null,chests:[],inventory:[],equipped:{}};
    const r=s.rewards;
    r.chests=Array.isArray(r.chests)?r.chests:[];
    r.inventory=Array.isArray(r.inventory)?r.inventory:[];
    r.equipped=r.equipped||{};
    return r;
  }
  function persist(){if(typeof save==='function')save();else localStorage.setItem('lifeLevel',JSON.stringify(state))}
  function toast(message){if(typeof showToast==='function')showToast(message);else alert(message)}
  function currentTier(lv){return lv>=12?'GOLD':lv>=5?'SILVER':'BRONZE'}
  function syncLevelChest(){
    const r=store(),current=Math.max(1,Number(level())||1);
    if(r.seenLevel===null||!Number.isFinite(Number(r.seenLevel))){r.seenLevel=current;persist();return}
    const previous=Math.max(1,Number(r.seenLevel));
    if(current<=previous)return;
    for(let lv=previous+1;lv<=current;lv++)r.chests.push({id:'chest-'+lv+'-'+Date.now()+'-'+Math.random().toString(36).slice(2,7),level:lv,tier:currentTier(lv),opened:false,createdAt:new Date().toISOString()});
    r.seenLevel=current;persist();toast('Уровень повышен. Получено сундуков: '+(current-previous));
  }
  function roll(tier){
    const table=lootTables[tier]||lootTables.BRONZE;
    const n=Math.random()*100;
    let total=0,rarity='common';
    for(const entry of table){total+=entry[1];if(n<total){rarity=entry[0];break}}
    const list=loot[rarity],chosen=list[Math.floor(Math.random()*list.length)];
    return {type:chosen[0],name:chosen[1],description:chosen[2],rarity,id:'item-'+Date.now()+'-'+Math.random().toString(36).slice(2,9),obtainedAt:new Date().toISOString()};
  }
  function openChest(id){
    const r=store(),chest=r.chests.find(x=>x.id===id);
    if(!chest||chest.opened)return;
    const item=roll(chest.tier);
    const duplicate=r.inventory.find(x=>x.type===item.type&&x.name===item.name);
    if(duplicate){
      item.name += ' · копия';
      item.description += '. Повторная награда сохранена отдельно.';
    }
    chest.opened=true;chest.itemId=item.id;r.inventory.unshift(item);persist();render();toast('Сундук открыт: '+item.name);
  }
  function equip(id){
    const r=store(),item=r.inventory.find(x=>x.id===id);
    if(!item||!slots.includes(item.type))return;
    r.equipped[item.type]=item.id;persist();render();toast('Экипировано: '+item.name);
  }
  function unequip(slot){const r=store();delete r.equipped[slot];persist();render()}
  function render(){
    const r=store(),chestList=$('sr-chests');
    if(chestList)chestList.innerHTML=r.chests.slice().reverse().map(chest=>{
      const tier=tiers[chest.tier]||tiers.BRONZE,item=r.inventory.find(x=>x.id===chest.itemId);
      return '<article class="sr-chest" style="--tier:'+tier.color+'"><div class="sr-chest-icon">'+(chest.opened?'◇':'▣')+'</div><div class="sr-chest-info"><b>'+tier.label+' сундук</b><small>'+(chest.opened?'Награда: '+esc(item?.name||'получена'):'За достижение '+chest.level+' уровня')+'</small></div><button '+(chest.opened?'disabled':'')+' onclick="srOpen(\''+chest.id+'\')">'+(chest.opened?'Открыт':'Открыть')+'</button></article>';
    }).join('')||'<div class="sr-empty">Пока нет сундуков. Они выдаются за повышение уровня.</div>';
    const equipped=$('sr-equipped');
    if(equipped)equipped.innerHTML=slots.map(slot=>{
      const item=r.inventory.find(x=>x.id===r.equipped[slot]);
      return '<div class="sr-slot"><small>'+slotNames[slot]+'</small><b>'+(item?esc(item.name):'Пусто')+'</b>'+(item?'<button onclick="srUnequip(\''+slot+'\')">Снять</button>':'')+'</div>';
    }).join('');
    const inventory=$('sr-inventory');
    if(inventory)inventory.innerHTML=r.inventory.map(item=>{
      const canEquip=slots.includes(item.type),isEquipped=r.equipped[item.type]===item.id;
      const action=canEquip?'<button '+(isEquipped?'disabled':'')+' onclick="srEquip(\\''+item.id+'\\')">'+(isEquipped?'Надето':'Надеть')+'</button>':'<small class="sr-bound">Не экипируется</small>';
      return '<article class="sr-item sr-'+item.rarity+'"><div><b>'+esc(item.name)+'</b><small>'+(slotNames[item.type]||'Предмет')+' · '+({common:'Обычный',rare:'Редкий',epic:'Эпический'}[item.rarity]||'Предмет')+'</small><p>'+esc(item.description)+'</p></div>'+action+'</article>';
    }).join('')||'<div class="sr-empty">Инвентарь пуст. Получай XP, повышай уровень и открывай сундуки.</div>';
    const count=$('sr-count');if(count)count.textContent=r.chests.filter(x=>!x.opened).length+' неоткрытых';
  }
  function mount(){
    const nav=document.querySelector('.nav'),app=document.querySelector('.app');
    if(!nav||!app||$('systemRewards'))return;
    const button=document.createElement('button');button.dataset.page='systemRewards';button.innerHTML='<span class="ico">⚔</span>Арсенал';
    button.onclick=()=>{
      document.querySelectorAll('.page').forEach(p=>p.classList.toggle('active',p.id==='systemRewards'));
      document.querySelectorAll('.nav button').forEach(b=>b.classList.toggle('active',b===button));render();window.scrollTo(0,0);
    };
    nav.appendChild(button);
    const page=document.createElement('section');page.className='page';page.id='systemRewards';
    page.innerHTML='<header class="sr-header"><small>HUNTER REWARD SYSTEM</small><h1>АРСЕНАЛ</h1><p>Награды за реальный прогресс</p></header><div class="sr-summary"><b>НАГРАДНЫЕ СУНДУКИ</b><strong id="sr-count">0</strong></div><h3>СУНДУКИ</h3><div id="sr-chests"></div><h3>ЭКИПИРОВКА</h3><div id="sr-equipped" class="sr-equipped"></div><h3>ИНВЕНТАРЬ</h3><div id="sr-inventory"></div>';
    app.appendChild(page);
    const style=document.createElement('style');
    style.textContent='#systemRewards h3{font:11px ui-monospace,monospace;letter-spacing:2px;color:#83dfff;margin:23px 2px 10px}.sr-header{padding:16px 2px 20px;color:#9be5ff}.sr-header small{letter-spacing:2px;font:10px ui-monospace,monospace}.sr-header h1{font-size:30px;letter-spacing:5px;margin:7px 0}.sr-header p{color:#91a0b1;font-size:12px}.sr-summary{display:flex;justify-content:space-between;align-items:center;padding:15px;background:linear-gradient(120deg,#162b3e,#182332);border:1px solid #315c77;border-radius:13px;color:#9be5ff}.sr-summary strong{font-size:23px}.sr-chest,.sr-item{display:flex;align-items:center;gap:12px;justify-content:space-between;padding:13px;margin:8px 0;background:#142333;border:1px solid #31516a;border-radius:12px}.sr-chest{border-color:var(--tier)}.sr-chest-icon{font-size:27px;color:var(--tier)}.sr-chest-info,.sr-item>div{min-width:0;flex:1}.sr-chest-info small,.sr-item small{display:block;color:#92a8ba;font-size:11px;margin-top:5px}.sr-chest button,.sr-item button,.sr-slot button{border:1px solid #397da0;background:#173c54;color:#e1f6ff;padding:8px 10px;border-radius:8px;font-size:12px}.sr-chest button:disabled,.sr-item button:disabled{opacity:.45}.sr-equipped{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.sr-slot{background:#132333;border:1px solid #2c4a61;border-radius:10px;padding:11px;min-width:0}.sr-slot small{display:block;color:#80cbe9;font-size:10px;text-transform:uppercase}.sr-slot b{display:block;font-size:12px;margin:7px 0;overflow-wrap:anywhere}.sr-item p{font-size:11px;color:#9bafbf;margin:5px 0 0}.sr-bound{font-size:10px;color:#8398aa;white-space:nowrap}.sr-common{border-color:#536577}.sr-rare{border-color:#398bb8}.sr-epic{border-color:#9873c8;box-shadow:inset 0 0 18px #7544aa18}.sr-empty{padding:16px;background:#111e2b;border:1px dashed #36516a;border-radius:10px;color:#8da2b4;font-size:12px}';
    document.head.appendChild(style);render();
  }
  window.srOpen=openChest;window.srEquip=equip;window.srUnequip=unequip;mount();
  const previousRender=window.render;
  if(typeof previousRender==='function')window.render=function(){previousRender.apply(this,arguments);syncLevelChest();render()};
  syncLevelChest();
})();
