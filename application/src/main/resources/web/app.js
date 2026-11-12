'use strict';
/* HallSync browser client. Authentication and booking rules live on the Java server. */
const S = {user:null,csrf:'',connected:false,hasConfig:false,problem:null,page:'overview',refs:null,bookings:[],schedule:[],filter:'All',catalog:null,care:'equipment',report:'BookingDetails',roomQuery:'',building:'',search:null,wizard:null};
const app = document.querySelector('#app');
const modal = document.querySelector('#modal');
const paths = {
 grid:'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
 search:'m21 21-4.4-4.4 M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
 calendar:'M8 2v4 M16 2v4 M3 10h18 M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2',
 book:'M4 3h13a2 2 0 0 1 2 2v16H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2 M3 17h16 M8 7h7 M8 11h5',
 check:'m5 12 4 4L19 6', arrow:'M4 12h16 M14 6l6 6-6 6', plus:'M12 5v14 M5 12h14',
 back:'M20 12H4 M10 6l-6 6 6 6', close:'m6 6 12 12 M6 18 18 6',
 clock:'M12 8v4l3 2 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
 users:'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M16 3a4 4 0 0 1 0 8 M22 21v-2a4 4 0 0 0-3-3.87 M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
 building:'M3 21h18 M5 21V7l7-4 7 4v14 M9 9v2 M15 9v2 M9 14v2 M15 14v2 M10 21v-2h4v2',
 pin:'M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0 M15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
 screen:'M4 3h16v12H4z M12 15v6 M8 21h8',
 logout:'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9',
 menu:'M4 6h16 M4 12h16 M4 18h16',
 shield:'M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11 M8 12l3 3 5-6',
 eye:'M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12 M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
 tool:'M14.7 6.3a5 5 0 0 0-6.4 6.4L3 18a2 2 0 0 0 3 3l5.3-5.3a5 5 0 0 0 6.4-6.4l-3 3-3-3z',
 chart:'M4 20V4 M4 20h17 M8 16v-4 M13 16V8 M18 16V5',
 download:'M12 3v12 M7 10l5 5 5-5 M5 16v5h14v-5',
 refresh:'M20 7v5h-5 M4 17v-5h5 M6.1 6a8 8 0 0 1 13.1 3 M4.8 15a8 8 0 0 0 13.1 3',
 info:'M12 11v6 M12 7h.01 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
 mail:'M3 5h18v14H3z m0 0 9 8 9-8', leaf:'M20 3C7 2 2 8 5 15s16 5 15-12 M5 20l10-10',
 settings:'M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8 M12 2v3 M12 19v3 M2 12h3 M19 12h3 M5 5l2 2 M17 17l2 2 M5 19l2-2 M17 7l2-2',
 cup:'M5 3h12v8a6 6 0 0 1-12 0z M17 5h2a3 3 0 0 1 0 6h-2 M8 21h6 M11 17v4'
};
const icon = name => `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="${paths[name]||paths.grid}"/></svg>`;
const esc = value => String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const idOf = d => d.id ?? d.departmentId;
const deptName = id => S.refs?.departments.find(d=>idOf(d)===id)?.departmentName || '—';
const initials = name => String(name||'U').split(/[ _.]+/).map(p=>p[0]).slice(0,2).join('').toUpperCase();
const today = () => new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Colombo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
const dateObj = d => new Date(`${d}T12:00:00`);
const plusDays = (d,n) => { const x=dateObj(d);x.setDate(x.getDate()+n);return `${x.getFullYear()}-${String(x.getMonth()+1).padStart(2,'0')}-${String(x.getDate()).padStart(2,'0')}`; };
const fmtDate = (d,opts={day:'numeric',month:'short',year:'numeric'}) => new Intl.DateTimeFormat('en-GB',opts).format(dateObj(d));
const time = t => String(t||'').slice(0,5);
const longTime = t => { const [h,m]=time(t).split(':').map(Number);return `${h%12||12}:${String(m).padStart(2,'0')} ${h>=12?'PM':'AM'}`; };
const timeNow = () => new Intl.DateTimeFormat('en-GB',{timeZone:'Asia/Colombo',hour:'2-digit',minute:'2-digit',hour12:false}).format(new Date());
const future = b => b.date>today() || (b.date===today()&&time(b.start)>timeNow());
const ended = b => b.date<today() || (b.date===today()&&time(b.end)<=timeNow());
const roomImage = id => `/assets/space-${(Math.abs(Number(id)||1)-1)%3+1}.svg`;
const roomLabel = h => `${h.buildingCode} / ${h.number}`;
const roleLabel = role => ({AdminStaff:'Administrator',Lecturer:'Lecturer',Student:'Student'}[role]||role);
const tag = status => `<span class="tag ${({Approved:'green',Pending:'gold',Rejected:'red',Cancelled:'gray',Completed:'blue',Active:'green',Inactive:'gray'})[status]||'gray'}"><span class="dot"></span>${esc(status)}</span>`;
const button = (label,action,cls='btn',attrs='') => `<button type="button" class="${cls}" data-action="${action}" ${attrs}>${label}</button>`;
const empty = (title,text,action='') => `<div class="empty">${icon('calendar')}<h3>${esc(title)}</h3><p>${esc(text)}</p>${action}</div>`;
const field = (label,name,value='',type='text',attrs='') => `<label class="field">${label}<input name="${name}" type="${type}" value="${esc(value)}" ${attrs}></label>`;
const opts = (rows,label,selected,optional=false) => `${optional?`<option value="">${typeof optional==='string'?esc(optional):'Not selected'}</option>`:''}${rows.map(d=>`<option value="${idOf(d)}" ${Number(selected)===idOf(d)?'selected':''}>${esc(label(d))}</option>`).join('')}`;
const select = (label,name,options,attrs='') => `<label class="field">${label}<select name="${name}" ${attrs}>${options}</select></label>`;
const passwordField = (label,name='password',newPassword=true) => `<label class="field">${label}<span class="password-wrap"><input name="${name}" type="password" ${newPassword?'minlength="8" maxlength="256" autocomplete="new-password"':'autocomplete="current-password" maxlength="256"'} required placeholder="${newPassword?'At least 8 characters':'Enter your password'}"><button type="button" class="password-toggle" data-action="toggle-password" aria-label="Show password">${icon('eye')}</button></span></label>`;
function toast(message,error=false) { const div=document.createElement('div');div.className=`toast${error?' error':''}`;div.innerHTML=icon(error?'info':'check')+`<span>${esc(message)}</span>`;document.querySelector('#toasts').append(div);setTimeout(()=>div.remove(),5500); }
async function api(path,data) {
 let response;
 try { response=await fetch('/api'+path,{method:data===undefined?'GET':'POST',credentials:'same-origin',headers:data===undefined?{}:{'Content-Type':'application/json','X-CSRF-Token':S.csrf},...(data===undefined?{}:{body:JSON.stringify(data)})}); }
 catch { throw new Error('HallSync is not reachable. Keep the launcher open and try again.'); }
 const result=await response.json().catch(()=>({ok:false,message:'The server could not read this request.'}));
 if(!response.ok||!result.ok) { if(response.status===401&&S.user&&path!=='/auth/signin'){S.user=null;renderAuth('login');}throw new Error(result.message||'Please try again.'); }
 return result.data;
}
function busy(form,working) { const b=form.querySelector('[type=submit]');if(!b)return;if(working){b.dataset.label=b.innerHTML;b.disabled=true;b.innerHTML='<span class="spinner"></span> Please wait';}else{b.disabled=false;b.innerHTML=b.dataset.label||'Continue';} }
function formError(form,message) { let e=form.querySelector('.error-box');if(!e){e=document.createElement('div');e.className='error-box';e.setAttribute('role','alert');form.prepend(e);}e.textContent=message;e.scrollIntoView({block:'nearest',behavior:'smooth'}); }
function showModal(title,subtitle,body,footer='',wide=false) {
 if(modal.open)modal.close();
 modal.innerHTML=`<div class="modal-head"><div><div class="eyebrow">HALLSYNC</div><h2 id="modal-title">${esc(title)}</h2>${subtitle?`<p>${esc(subtitle)}</p>`:''}</div>${button(icon('close'),'close-modal','icon-btn','aria-label="Close dialog"')}</div><div class="modal-body">${body}</div>${footer?`<div class="modal-footer">${footer}</div>`:''}`;
 modal.classList.toggle('wide',wide);modal.showModal();
}
function closeModal(){if(modal.open)modal.close();S.wizard=null;}
async function refreshData() {
 const [refs,bookings,schedule]=await Promise.all([api('/reference'),S.user.canRequest?api('/bookings?scope='+(S.user.admin?'all':'mine')):Promise.resolve([]),api(`/schedule?from=${today()}&to=${plusDays(today(),6)}`)]);
 S.refs=refs;S.bookings=bookings;S.schedule=schedule;
}
async function start(){try{Object.assign(S,await api('/state'));if(S.user){await refreshData();navigate(location.hash.slice(1)||'overview',true);}else renderAuth(S.connected?'login':'connection');}catch(e){app.innerHTML=`<div class="boot">${icon('info')}<h2>Let’s reconnect</h2><p>${esc(e.message)}</p>${button('Try again','restart','btn btn-primary')}</div>`;}}
function authFrame(body,wide=false){app.innerHTML=`<div class="auth-page"><aside class="auth-brand"><div class="brand"><img src="/assets/brand.svg" alt="">HallSync</div><div class="auth-story"><div class="eyebrow">THOUGHTFUL SPACES. SEAMLESS DAYS.</div><h1 class="serif">Great spaces.<br><em>Better possibilities.</em></h1><p>A little less coordination.<br>A lot more room for what matters.</p></div><div class="auth-visual"><img src="/assets/space-1.svg" alt="Illustration of a welcoming lecture hall"></div><div class="auth-caption"><span>Faculty of Science · University of Kelaniya</span><span class="pill">${icon('leaf')} Made for campus life</span></div></aside><main class="auth-main" id="main"><div class="auth-card ${wide?'wide':''}">${body}</div></main></div>`;}
async function renderAuth(view){
 S.authView=view;
 if(view==='connection') {
  authFrame(`<span class="setup-title-icon">${icon('settings')}</span><div class="eyebrow">YOUR WORKSPACE</div><h2>One connection.<br>Everything in sync.</h2><p>Connect HallSync to your local MySQL database.</p>${S.problem?`<div class="error-box">${esc(S.problem)}</div>`:''}${S.hasConfig?`<div class="notice warning">${icon('info')}<span>Your settings file is already saved. Start MySQL, then restart HallSync. Existing settings are kept safe.</span></div>${button('Check again','restart','btn btn-primary full')}`:`<form id="connection-form" class="auth-form"><div class="error-box" role="alert"></div>${field('Database address','url','jdbc:mysql://127.0.0.1:3306/hallsync_db','text','required autocomplete="off"')}${field('Connection username','username','hallsync_app','text','required autocomplete="username"')}${passwordField('Connection password','password',false)}<button type="submit" class="btn btn-primary full">Test & connect ${icon('arrow')}</button></form><p class="setup-note">First installation? Import the supplied SQL files in MySQL Workbench first. Your password stays in the local settings file.</p>`}`,false);return;
 }
 if(view==='register'||view==='admin') {
  let departments=[];try{departments=await api('/public/departments');}catch(e){toast(e.message,true);return;}
  const admin=view==='admin';
  authFrame(`${button(icon('back')+' Back to sign in','show-login','text-btn auth-back')}<div class="eyebrow">${admin?'WORKSPACE INITIALIZATION':'JOIN YOUR CAMPUS'}</div><h2>${admin?'Set up your administrator':'Make yourself at home.'}</h2><p>${admin?'Create the first usable administrator account.':'Create a student account to explore halls and view the timetable.'}</p><form id="${admin?'admin-setup':'register'}-form" class="auth-form"><div class="error-box" role="alert"></div>${admin?`<div class="notice">${icon('shield')}<span>MySQL administrator access is required. These database credentials are used once and are not saved.</span></div><div class="form-grid">${field('MySQL admin username','databaseUser','root','text','required autocomplete="off"')}${passwordField('MySQL admin password','databasePassword',false)}</div><div class="auth-divider"></div>`:''}<div class="form-grid">${field('Full name','fullName',admin?'Project Administrator':'','text','required maxlength="150" autocomplete="name"')}${field('Email address','email','','email','required maxlength="254" autocomplete="email"')}${field('Username','username',admin?'project_admin':'','text','required maxlength="50" autocomplete="username"')}${select('Department','departmentId',opts(departments,d=>d.departmentName,'','No department'))}${passwordField('Choose a password')}${passwordField('Confirm password','confirmation')}</div><button type="submit" class="btn btn-primary full">${admin?'Create administrator':'Create student account'} ${icon('arrow')}</button><p class="setup-note">${admin?'Existing accounts and reservations are preserved.':'Staff and lecturer accounts are created by an administrator.'}</p></form>`,true);return;
 }
 authFrame(`<div class="eyebrow">YOUR CAMPUS, IN SYNC</div><h2>Welcome back.</h2><p>Sign in to find a space and make your day happen.</p><form id="login-form" class="auth-form"><div class="error-box" role="alert"></div>${field('Username','username','','text','required autocomplete="username" placeholder="Your HallSync username"')}${passwordField('Password','password',false)}<button type="submit" class="btn btn-primary full">Sign in to HallSync ${icon('arrow')}</button></form><p class="auth-foot">New to HallSync? ${button('Create a student account','show-register','text-btn')}</p><div class="auth-divider"></div><div class="row between"><span class="muted"><small>Setting up for the first time?</small></span>${button('Create administrator','show-admin','text-btn')}</div><p class="micro-copy">${icon('shield')} Connected locally. Designed around your campus.</p>`);
}
const navs=[['overview','grid','Overview'],['spaces','search','Find a hall'],['bookings','book','My bookings'],['schedule','calendar','Schedule']];
const adminNavs=[['reviews','check','Reservations'],['catalog','building','Campus manager'],['care','tool','Hall care'],['people','users','People'],['reports','chart','Reports']];
const names=Object.fromEntries([...navs,...adminNavs].map(([k,i,n])=>[k,n]));
function shell(){
 const nav=items=>items.filter(([id])=>id!=='bookings'||S.user.canRequest).map(([id,i,n])=>`<button class="nav-item ${S.page===id?'active':''}" data-nav="${id}" ${S.page===id?'aria-current="page"':''}>${icon(i)}<span>${n}</span>${id==='reviews'&&S.bookings.some(b=>b.status==='Pending')?`<span class="nav-count">${S.bookings.filter(b=>b.status==='Pending').length}</span>`:''}</button>`).join('');
 app.innerHTML=`<div class="shell"><aside class="sidebar" aria-label="Main navigation"><div class="brand"><img src="/assets/brand.svg" alt="">HallSync</div><div class="workspace-switch"><span class="school-icon">${icon('building')}</span><div>Faculty of Science<small>University of Kelaniya</small></div></div><div class="nav-group-label">WORKSPACE</div><nav class="nav-group">${nav(navs)}</nav>${S.user.admin?`<div class="nav-group-label">ADMINISTRATION</div><nav class="nav-group">${nav(adminNavs)}</nav>`:''}<div class="sidebar-bottom"><div class="identity"><div class="avatar">${esc(initials(S.user.username))}</div><div><strong>${esc(S.user.username)}</strong><small>${roleLabel(S.user.role)}</small></div>${button(icon('logout'),'logout','icon-btn','aria-label="Sign out" title="Sign out"')}</div><div class="connection-status"><span class="dot"></span> Workspace connected · Asia/Colombo</div></div></aside><div class="workspace"><header class="topbar"><div class="row">${button(icon('menu'),'menu','icon-btn mobile-menu','aria-label="Open navigation"')}<div class="breadcrumb">Workspace <span>/</span> <strong>${names[S.page]||'Overview'}</strong></div></div><div class="topbar-tools"><span class="top-date">${icon('calendar')} ${fmtDate(today(),{weekday:'short',day:'numeric',month:'long',year:'numeric'})}</span><span class="pill"><span class="dot"></span> Faculty of Science</span><span class="avatar top-avatar" aria-label="Signed in as ${esc(S.user.username)}">${esc(initials(S.user.username))}</span></div></header><main class="main" id="main" tabindex="-1"></main></div></div>`;
}
async function navigate(page,replace=false){
 if(!S.user)return renderAuth('login');
 if(!names[page]||(adminNavs.some(n=>n[0]===page)&&!S.user.admin)||(page==='bookings'&&!S.user.canRequest))page='overview';
 S.page=page;if(replace)history.replaceState(null,'','#'+page);else if(location.hash!=='#'+page)history.pushState(null,'','#'+page);
 shell();const main=document.querySelector('#main');main.innerHTML='<div class="loading-panel"><span class="spinner"></span> Making room for you…</div>';
 try{await renderPage(main);}catch(e){main.innerHTML=`<div class="panel page-error"><h2>Let’s try that again.</h2><p>${esc(e.message)}</p>${button(icon('refresh')+' Refresh','refresh','btn btn-primary')}</div>`;}
}
function pageTitle(eyebrow,title,subtitle,action=''){return `<div class="page-title"><div><div class="eyebrow">${eyebrow}</div><h1>${title}</h1>${subtitle?`<p>${subtitle}</p>`:''}</div>${action}</div>`;}
function roomCard(h,available=false){
 const equipment=S.refs.equipment.filter(e=>e.hallId===h.id&&e.workingQuantity>0);const projector=equipment.find(e=>/projector|screen/i.test(e.facilityName));
 return `<article class="room-card"><div class="room-image"><img src="${roomImage(h.id)}" alt="Lecture hall illustration" loading="lazy"><span class="tag ${h.status==='Inactive'?'gray':'green'}"><span class="dot"></span>${available?'Available for your time':h.status==='Active'?'Open for reservations':'Currently inactive'}</span>${h.floor?`<span class="room-floor">Floor ${esc(h.floor)}</span>`:''}</div><div class="room-content"><div class="room-category">LECTURE SPACE · ${esc(h.buildingCode)}</div><h3>${esc(h.number)}</h3><div class="room-location">${icon('pin')}${esc(h.buildingName)}</div><div class="room-meta"><span>${icon('users')} ${h.capacity} seats</span>${projector?`<span>${icon('screen')} ${esc(projector.facilityName)}</span>`:`<span>${icon('building')} Campus hall</span>`}</div><div class="room-footer"><small>Make room for your next session</small>${button('View space '+icon('arrow'),'room-detail','text-btn',`data-id="${h.id}"`)}</div></div></article>`;
}
async function renderPage(main){
 switch(S.page){
 case 'overview':return renderOverview(main);
 case 'spaces':return renderSpaces(main);
 case 'bookings':case 'reviews':return renderBookings(main);
 case 'schedule':return renderSchedule(main);
 case 'catalog':return renderCatalog(main);
 case 'care':return renderCare(main);
 case 'people':return renderPeople(main);
 case 'reports':return renderReports(main);
 }
}
function renderOverview(main){
 const halls=S.refs.halls.filter(h=>h.status==='Active'),pending=S.bookings.filter(b=>b.status==='Pending'),day=S.schedule.filter(b=>b.date===today()).sort((a,b)=>a.start.localeCompare(b.start));
 const pendingCount=S.user.admin?pending.length:pending.filter(b=>b.requesterId===S.user.id).length;
 const stat=(label,value,sub,i)=>`<div class="stat"><div class="stat-top">${label}${icon(i)}</div><strong>${value}</strong><small>${sub}</small></div>`;
 main.innerHTML=pageTitle('A LITTLE MORE ROOM FOR POSSIBILITY',`Good ${Number(timeNow().slice(0,2))<12?'morning':Number(timeNow().slice(0,2))<17?'afternoon':'evening'}, ${esc(S.user.username)}.`,"Your spaces, schedules and campus — all in one place.",S.user.canRequest?button(icon('plus')+' New booking','new-booking','btn btn-primary'):'')+`<div class="content-grid"><section class="hero"><div class="eyebrow">SPACE FOR YOUR NEXT BIG IDEA</div><h2 class="serif">Find your space.<br><em>Make it happen.</em></h2><p>From focused tutorials to full-house lectures.<br>The right hall is just a few clicks away.</p>${button('Explore available halls '+icon('arrow'),'go-spaces','btn')}<img class="hero-illustration" src="/assets/space-1.svg" alt="Lecture hall illustration"></section><section class="panel agenda"><div class="agenda-head"><span class="eyebrow">ON THE AGENDA</span><span class="tag">Today</span></div><h3>${fmtDate(today(),{weekday:'long',day:'numeric',month:'short'})}</h3>${day.length?`<div class="agenda-list">${day.slice(0,2).map(e=>`<div class="agenda-row"><time>${time(e.start)}</time><div><strong>${esc(e.courseCode||e.type)}</strong><small>${esc(e.hallLabel)} · ${time(e.end)}</small></div></div>`).join('')}</div>`:'<div class="agenda-empty">A little breathing room.<br>No scheduled sessions today.</div>'}${button('View schedule '+icon('arrow'),'go-schedule','text-btn')}</section></div><section class="stats-grid" aria-label="Workspace summary">${stat('Active halls',halls.length,'Ready for future reservations','building')}${stat('Scheduled today',day.length,'Approved campus sessions','calendar')}${stat(S.user.canRequest?'Awaiting approval':'This week',S.user.canRequest?pendingCount:S.schedule.length,S.user.canRequest?'Requests that need a decision':'Approved scheduled sessions','clock')}${stat('Total seating',halls.reduce((n,h)=>n+h.capacity,0),'Across active lecture halls','users')}</section><section><div class="section-title"><div><h2>Spaces to bring ideas together.</h2><p>Find a setting that fits your session.</p></div>${button('Browse all halls '+icon('arrow'),'go-spaces','text-btn')}</div><div class="room-grid">${halls.length?halls.slice(0,3).map(h=>roomCard(h)).join(''):empty('Your spaces start here','An administrator can add lecture halls in Campus manager.')}</div></section>`;
}
function searchDefaults(){return S.search||{date:plusDays(today(),1),start:'09:00',end:'10:00',attendance:30};}
async function renderSpaces(main,results){
 const f=searchDefaults();
 main.innerHTML=pageTitle('A PLACE FOR EVERY PURPOSE','Find your next space.','Choose a time, tell us your group size, and find a hall that fits.')+`<form id="availability-form" class="filter-bar">${field('Date','date',f.date,'date',`required min="${today()}"`)}${field('From','start',time(f.start),'time','required')}${field('Until','end',time(f.end),'time','required')}${field('People','attendance',f.attendance,'number','required min="1" max="2147483647"')}<button type="submit" class="btn btn-primary">${icon('search')} Find a hall</button></form><div class="search-controls"><p id="space-count">${results?'Available for your selected time':'Explore your campus spaces'}</p><div class="row wrap"><label class="search-input"><span class="visually-hidden">Search halls</span>${icon('search')}<input id="room-query" type="search" placeholder="Search halls or buildings" value="${esc(S.roomQuery)}"></label><label><span class="visually-hidden">Filter building</span><select id="building-filter"><option value="">All buildings</option>${S.refs.buildings.map(b=>`<option value="${esc(b.code)}" ${S.building===b.code?'selected':''}>${esc(b.name)}</option>`).join('')}</select></label>${button('Reset','clear-spaces','btn btn-quiet btn-small')}</div></div><div id="space-results" class="room-grid"></div>`;
 S.spaceResults=results??S.refs.halls.filter(h=>h.status==='Active');S.availableSearch=Boolean(results);filterSpaces();
}
function filterSpaces(){if(S.page!=='spaces')return;const q=S.roomQuery.toLowerCase();const rooms=(S.spaceResults||[]).filter(h=>(!S.building||h.buildingCode===S.building)&&`${h.number} ${h.buildingCode} ${h.buildingName}`.toLowerCase().includes(q));document.querySelector('#space-results').innerHTML=rooms.length?rooms.map(h=>roomCard(h,S.availableSearch)).join(''):empty('A different time might do the trick.','No halls match these filters. Try another time, a smaller group, or reset the filters.');document.querySelector('#space-count').textContent=`${rooms.length} ${rooms.length===1?'space':'spaces'} ${S.availableSearch?'available for your time':'to explore'}`;}
function myBookings(){return S.bookings.filter(b=>S.page==='reviews'||b.requesterId===S.user.id);}
function renderBookings(main){
 if(S.page==='reviews'&&S.filter==='All')S.filter='Pending';
 const all=myBookings();const filters=['All','Pending','Approved','History'];
 let rows=all.filter(b=>S.filter==='All'||(S.filter==='History'?['Cancelled','Rejected','Completed'].includes(b.status):b.status===S.filter));rows.sort((a,b)=>b.date.localeCompare(a.date)||b.start.localeCompare(a.start));
 main.innerHTML=pageTitle(S.page==='reviews'?'KEEP YOUR CAMPUS MOVING':'YOUR TIME, WELL PLANNED',S.page==='reviews'?'A little clarity. A better schedule.':'Your bookings, beautifully organised.',S.page==='reviews'?'Review requests and keep every reservation in the right place.':'Keep track of your requests, confirmed sessions and booking history.',S.page==='bookings'?button(icon('plus')+' New booking','new-booking','btn btn-primary'):button(icon('refresh')+' Refresh','refresh','btn'))+`<div class="toolbar"><div class="segmented" aria-label="Booking status">${filters.map(f=>button(`${f}${f==='Pending'?` · ${all.filter(b=>b.status==='Pending').length}`:''}`,'filter-bookings',`segment ${S.filter===f?'active':''}`,`data-filter="${f}" aria-pressed="${S.filter===f}"`)).join('')}</div><span class="muted"><small>${rows.length} ${rows.length===1?'reservation':'reservations'}</small></span></div><div class="booking-list">${rows.length?rows.map(bookingCard).join(''):`<div class="panel">${empty(S.filter==='Pending'?'All caught up.':'Nothing here just yet.',S.filter==='Pending'?'There are no requests waiting for a decision.':'Your reservations will appear here as you make them.',S.page==='bookings'?button('Find a hall '+icon('arrow'),'go-spaces','btn btn-primary'):'')}</div>`}</div>`;
}
function bookingCard(b){
 const review=S.page==='reviews',own=b.requesterId===S.user.id;
 const actions=[];
 if(review&&b.status==='Pending'&&!own){actions.push(button('Approve','review-booking','btn btn-primary btn-small',`data-id="${b.id}" data-decision="Approved"`),button('Decline','review-booking','btn btn-small',`data-id="${b.id}" data-decision="Rejected"`));}
 if(review&&b.status==='Pending'&&own)actions.push('<span class="tag gold">Another admin must review</span>');
 if(!review&&b.status==='Pending'&&own&&future(b))actions.push(button(icon('settings'),'edit-booking','icon-btn',`data-id="${b.id}" aria-label="Edit booking ${b.id}" title="Edit request"`));
 if(['Pending','Approved'].includes(b.status)&&future(b))actions.push(button(icon('close'),'cancel-booking','icon-btn',`data-id="${b.id}" aria-label="Cancel booking ${b.id}" title="Cancel booking"`));
 if(review&&b.status==='Approved'&&ended(b))actions.push(button('Complete','complete-booking','btn btn-small',`data-id="${b.id}"`));
 actions.push(button(icon('arrow'),'booking-detail','icon-btn',`data-id="${b.id}" aria-label="View booking ${b.id}" title="View details"`));
 return `<article class="booking-card"><div class="date-badge"><span>${fmtDate(b.date,{month:'short'})}</span><strong>${Number(b.date.slice(8))}</strong></div><div class="booking-info"><div class="booking-title"><h3>${esc(b.purpose)}</h3>${tag(b.status)}</div><div class="booking-sub"><span>${icon('clock')}${longTime(b.start)} – ${longTime(b.end)}</span><span>${icon('pin')}${esc(b.hallLabel)}</span><span>${icon('users')}${b.attendance} people</span>${review?`<span>By ${esc(b.requesterName)}</span>`:''}</div></div><div class="booking-actions">${actions.join('')}</div></article>`;
}
async function renderSchedule(main){
 const selected=S.scheduleDate||today();const base=dateObj(selected);const weekday=base.getDay();const monday=plusDays(selected,-((weekday+6)%7));
 const events=await api(`/schedule?from=${monday}&to=${plusDays(monday,6)}`);
 const chosen=events.filter(e=>e.date===selected).sort((a,b)=>a.start.localeCompare(b.start));
 main.innerHTML=pageTitle('CAMPUS AT A GLANCE','A little rhythm to your week.','Approved sessions, one clear schedule. All times are Asia/Colombo.')+`<div class="row between wrap"><div class="row">${button(icon('back'),'schedule-week','icon-btn','data-offset="-7" aria-label="Previous week"')}<h3>${fmtDate(monday,{day:'numeric',month:'short'})} — ${fmtDate(plusDays(monday,6),{day:'numeric',month:'short',year:'numeric'})}</h3>${button(icon('arrow'),'schedule-week','icon-btn','data-offset="7" aria-label="Next week"')}</div><label class="row"><span class="muted"><small>Jump to date</small></span><input id="schedule-date" type="date" value="${selected}" aria-label="Schedule date"></label></div><div class="week-strip">${Array.from({length:7},(_,i)=>{const d=plusDays(monday,i),count=events.filter(e=>e.date===d).length;return button(`<span>${fmtDate(d,{weekday:'short'})}</span><strong>${Number(d.slice(8))}</strong><small>${count?`${count} session${count===1?'':'s'}`:'No sessions'}</small>`,'schedule-day',`day-tile ${d===selected?'selected':''}`,`data-date="${d}" aria-pressed="${d===selected}"`);}).join('')}</div><div class="section-title"><h2>${fmtDate(selected,{weekday:'long',day:'numeric',month:'long'})}</h2><span class="tag green">${chosen.length} sessions</span></div><div class="timeline">${chosen.length?chosen.map(e=>`<article class="timeline-event"><time class="timeline-time">${time(e.start)}</time><div class="timeline-card"><div><h3>${esc(e.courseCode||e.type)}</h3><p>${esc(e.hallLabel)} · ${longTime(e.start)} – ${longTime(e.end)}</p></div>${tag(e.status)}</div></article>`).join(''):`<div class="panel">${empty('A clear day ahead.','There are no approved or completed sessions on this date.')}</div>`}</div>`;
}
const catalogs={
 departments:{title:'Departments',icon:'building',singular:'department',rows:()=>S.refs.departments,columns:['Department','ID'],cells:d=>[esc(d.departmentName),idOf(d)]},
 buildings:{title:'Buildings',icon:'building',singular:'building',rows:()=>S.refs.buildings,columns:['Building','Code','Location'],cells:b=>[`<strong>${esc(b.name)}</strong>`,esc(b.code),esc(b.location||'—')]},
 halls:{title:'Lecture halls',icon:'screen',singular:'lecture hall',rows:()=>S.refs.halls,columns:['Hall','Building','Capacity','Floor','Status',''],cells:h=>[`<strong>${esc(h.number)}</strong>`,esc(h.buildingName),`${h.capacity} seats`,esc(h.floor||'—'),tag(h.status),button('Manage','hall-settings','btn btn-small',`data-id="${h.id}"`)]},
 courses:{title:'Course units',icon:'book',singular:'course unit',rows:()=>S.refs.courses,columns:['Course','Title','Department'],cells:c=>[`<strong>${esc(c.code)}</strong>`,esc(c.title),esc(deptName(c.departmentId))]},
 groups:{title:'Student groups',icon:'users',singular:'student group',rows:()=>S.refs.groups,columns:['Group','Department','Intake','Study year'],cells:g=>[`<strong>${esc(g.name)}</strong>`,esc(deptName(g.departmentId)),g.intakeYear,g.studyYear]},
 facilities:{title:'Equipment types',icon:'tool',singular:'equipment type',rows:()=>S.refs.facilities,columns:['Equipment type','Description'],cells:f=>[`<strong>${esc(f.name)}</strong>`,esc(f.description||'—')]}
};
function tableHTML(headers,rows){return `<div class="table-wrap"><table class="data-table"><thead><tr>${headers.map(h=>`<th scope="col">${esc(h)}</th>`).join('')}</tr></thead><tbody>${rows.map(row=>`<tr>${row.map(v=>`<td>${v}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`;}
function renderCatalog(main){
 if(!S.catalog){main.innerHTML=pageTitle('THE FOUNDATIONS OF A GREAT CAMPUS','Everything in its right place.','Manage your halls, buildings and academic reference data.')+`<div class="catalog-grid">${Object.entries(catalogs).map(([key,c])=>button(`<span class="catalog-icon">${icon(c.icon)}</span><span><strong>${c.title}</strong><small>${c.rows().length} records</small></span><span class="spacer"></span>${icon('arrow')}`,'open-catalog','catalog-card',`data-kind="${key}"`)).join('')}</div>`;return;}
 const c=catalogs[S.catalog];main.innerHTML=button(icon('back')+' Campus manager','catalog-back','text-btn back-link')+pageTitle('CAMPUS MANAGER',c.title,'Keep the details that make reservations work.',button(icon('plus')+' Add '+c.singular,'new-catalog','btn btn-primary',`data-kind="${S.catalog}"`))+(c.rows().length?tableHTML(c.columns,c.rows().map(c.cells)):`<div class="panel">${empty('Ready when you are.',`Add your first ${c.singular} to get started.`)}</div>`);
}
async function renderPeople(main){
 const department=S.peopleDepartment||'';const people=await api('/accounts'+(department?'?department='+department:''));S.people=people;
 main.innerHTML=pageTitle('THE PEOPLE BEHIND THE PLANS','A connected campus.','Manage application accounts and provision the right access.',button(icon('plus')+' Add person','new-person','btn btn-primary'))+`<div class="toolbar"><span class="muted"><small>${people.length} ${people.length===1?'account':'accounts'}</small></span><label class="row"><span class="muted"><small>Department</small></span><select id="people-department" aria-label="Filter accounts by department">${opts(S.refs.departments,d=>d.departmentName,department,'All departments')}</select></label></div>`+(people.length?tableHTML(['Person','Department','Role','Status'],people.map(p=>[`<div class="row"><span class="avatar">${esc(initials(p.fullName))}</span><span><strong>${esc(p.fullName)}</strong><small>${esc(p.username)} · ${esc(p.email)}</small></span></div>`,esc(p.department||'No department'),`<span class="tag ${p.role==='AdminStaff'?'gold':p.role==='Lecturer'?'blue':'gray'}">${roleLabel(p.role)}</span>`,tag(p.active?'Active':'Inactive')])):`<div class="panel">${empty('No accounts in this department.','Choose another department or add a person.')}</div>`);
}
async function renderCare(main){
 const blocks=S.care==='blocks'?await api('/blocks'):[];S.blocks=blocks;
 main.innerHTML=pageTitle('A LITTLE CARE GOES A LONG WAY','Keep your spaces at their best.','Track equipment and make room for maintenance.',button(icon('plus')+(S.care==='blocks'?' Add hall block':' Update equipment'),S.care==='blocks'?'new-block':'new-equipment','btn btn-primary'))+`<div class="toolbar"><div class="segmented">${button('Hall equipment','care-view',`segment ${S.care==='equipment'?'active':''}`,'data-view="equipment"')}${button('Maintenance & closures','care-view',`segment ${S.care==='blocks'?'active':''}`,'data-view="blocks"')}</div><span class="muted"><small>${S.care==='equipment'?'Working quantities shown in hall details':'Cancelled blocks stay in your history'}</small></span></div>`;
 if(S.care==='equipment'){const rows=S.refs.equipment;main.innerHTML+=rows.length?tableHTML(['Hall','Equipment','Total','Working','Needs attention',''],rows.map(e=>[`<strong>${esc(e.hallLabel)}</strong>`,esc(e.facilityName),e.quantity,`<span class="tag green">${e.workingQuantity}</span>`,e.quantity-e.workingQuantity?`<span class="tag gold">${e.quantity-e.workingQuantity}</span>`:'—',button('Update','edit-equipment','btn btn-small',`data-hall="${e.hallId}" data-facility="${e.facilityId}"`)])):`<div class="panel">${empty('Let’s get your halls equipped.','Add facility types in Campus manager, then assign quantities to a hall.')}</div>`;}
 else main.innerHTML+=`<div class="panel">${blocks.length?blocks.map(b=>`<article class="block-card"><div><div class="row wrap"><h3>${esc(b.reason)}</h3>${tag(b.status)}</div><p>${esc(b.hallLabel)} · ${fmtDate(b.start.slice(0,10))}, ${time(b.start.slice(11))} → ${fmtDate(b.end.slice(0,10))}, ${time(b.end.slice(11))}</p><p>Recorded by ${esc(b.recordedBy)}</p></div>${b.status==='Active'?button('Cancel block','cancel-block','btn btn-small',`data-id="${b.id}"`):''}</article>`).join(''):empty('All clear.','Hall maintenance and closure periods will appear here.')}</div>`;
}
const reportLabels={BookingDetails:'Reservations',StatusTotals:'Status summary',HallFacilities:'Hall equipment',HallUsage:'Scheduled usage'};
async function renderReports(main){
 const [totals,usage,report]=await Promise.all([api('/reports?kind=StatusTotals'),api('/reports?kind=HallUsage'),api('/reports?kind='+S.report)]);S.reportData=report;
 const statusCount=Object.fromEntries(totals.rows.map(r=>[r[0],Number(r[1])])),total=Object.values(statusCount).reduce((a,b)=>a+b,0),minutes=usage.rows.reduce((a,r)=>a+Number(r[3]),0);
 main.innerHTML=pageTitle('SEE THE BIGGER PICTURE','Good decisions start with clarity.','Reservation and equipment reports, ready when you need them.',`<a class="btn btn-primary" href="/api/reports/download?kind=${S.report}" download>${icon('download')} Export CSV</a>`)+`<div class="report-grid"><section class="panel panel-pad"><div class="eyebrow">RESERVATIONS AT A GLANCE</div><div class="metric-large">${total}</div><p class="report-note">All recorded booking requests</p><div class="status-bars">${['Approved','Pending','Completed','Cancelled','Rejected'].map(status=>`<div><div class="bar-label"><span>${status}</span><strong>${statusCount[status]||0}</strong></div><progress value="${statusCount[status]||0}" max="${total||1}" aria-label="${status} bookings"></progress></div>`).join('')}</div></section><section class="panel panel-pad"><div class="eyebrow">SCHEDULED HALL TIME</div><div class="metric-large">${(minutes/60).toLocaleString(undefined,{maximumFractionDigits:1})}<span class="muted"><small> hours</small></span></div><p class="report-note">Reserved time across Approved and Completed sessions.</p><div class="auth-divider"></div><h3>Time thoughtfully allocated.</h3><p class="setup-note">These figures reflect scheduled reservations, not measured attendance or actual occupancy. Export the detailed records for your own analysis.</p><div class="equipment-list"><span class="equipment-item">${icon('building')}${S.refs.halls.length} halls</span><span class="equipment-item">${icon('calendar')}${usage.rows.length} hall-days</span></div></section></div><div class="section-title"><h2>Explore the details.</h2><span class="muted"><small>${report.rows.length} records</small></span></div><div class="report-type">${Object.entries(reportLabels).map(([kind,label])=>button(label,'report-kind',`btn btn-small ${S.report===kind?'active':''}`,`data-kind="${kind}"`)).join('')}</div>`+(report.rows.length?tableHTML(report.columns.map(c=>c.replace(/([a-z])([A-Z])/g,'$1 $2')),report.rows.map(r=>r.map(esc))):`<div class="panel">${empty('A fresh start.','This report will fill up as your campus makes reservations.')}</div>`);
}
function openRoom(id){
 const h=S.refs.halls.find(h=>h.id===id);if(!h)return;
 const eq=S.refs.equipment.filter(e=>e.hallId===id);
 showModal(h.number,h.buildingName,`<img class="space-detail-image" src="${roomImage(id)}" alt="Illustration of a lecture space"><div class="row wrap space-facts"><span class="equipment-item">${icon('users')}${h.capacity} seats</span>${h.floor?`<span class="equipment-item">${icon('building')}Floor ${esc(h.floor)}</span>`:''}${tag(h.status)}</div><div class="auth-divider"></div><h3>Everything in place.</h3><div class="equipment-list">${eq.length?eq.map(e=>`<span class="equipment-item">${icon('screen')}${esc(e.facilityName)} <strong>${e.workingQuantity}/${e.quantity}</strong> working</span>`).join(''):'<p class="muted"><small>No equipment is listed for this hall yet.</small></p>'}</div><p class="form-help">Room illustrations are for orientation. Availability is checked for the date, time and group size you choose.</p>`,`<span class="muted"><small>${esc(h.buildingCode)} / ${esc(h.number)}</small></span>${S.user.canRequest&&h.status==='Active'?button('Book this space '+icon('arrow'),'book-room','btn btn-primary',`data-id="${id}"`):button('Close','close-modal','btn')}`);
}
async function openWizard(hallId,old){
 let groupIds=[];if(old)groupIds=await api('/bookings/groups?id='+old.id);
 const f=searchDefaults(),h=S.refs.halls.find(h=>h.id===hallId)||S.refs.halls.find(h=>h.status==='Active');
 if(!h){toast('Add an active hall before creating a booking.',true);return;}
 S.wizard={step:1,id:old?.id,hallId:old?.hallId||h.id,courseId:old?.courseId||null,date:old?.date||f.date,start:time(old?.start||f.start),end:time(old?.end||f.end),attendance:old?.attendance||f.attendance,type:old?.type||'Lecture',purpose:old?.purpose||'',groupIds};renderWizard();
}
function renderWizard(){
 const w=S.wizard;if(!w)return;if(!S.refs.halls.some(h=>h.id===Number(w.hallId)&&h.status==='Active'))w.hallId=S.refs.halls.find(h=>h.status==='Active')?.id;const h=S.refs.halls.find(h=>h.id===Number(w.hallId));if(!h){closeModal();toast('Add an active hall before creating a booking.',true);return;}
 const steps=`<div class="steps" aria-label="Booking steps">${['Your session','Your audience','Review & request'].map((n,i)=>`<span class="step ${w.step===i+1?'active':w.step>i+1?'done':''}"><span class="step-number">${w.step>i+1?'✓':i+1}</span>${n}</span>`).join('')}</div>`;
 let body;
 if(w.step===1)body=`<div class="form-grid">${select('Lecture hall','hallId',opts(S.refs.halls.filter(h=>h.status==='Active'),h=>`${roomLabel(h)} · ${h.capacity} seats`,w.hallId), 'required')}${select('Session type','type',['Lecture','Tutorial','Exam','Event'].map(t=>`<option ${w.type===t?'selected':''}>${t}</option>`).join(''))}${field('Date','date',w.date,'date',`required min="${today()}"`)}${field('Expected attendance','attendance',w.attendance,'number','required min="1" max="2147483647"')}${field('Starts at','start',w.start,'time','required')}${field('Ends at','end',w.end,'time','required')}<label class="field span-2">What is the session for?<textarea name="purpose" required maxlength="255" placeholder="For example, INTE 12223 — Object-Oriented Programming lecture">${esc(w.purpose)}</textarea></label></div>`;
 else if(w.step===2){const academic=['Lecture','Tutorial'].includes(w.type);body=`<div class="stack"><div class="notice">${icon('info')}<span>${academic?'Lectures and tutorials need a course unit and at least one student group.':'For events and exams, course and group selections are optional.'}</span></div>${select(`Course unit ${academic?'<span class="required">*</span>':'(optional)'}`,'courseId',opts(S.refs.courses,c=>`${c.code} · ${c.title}`,w.courseId,'Select a course'),academic?'required':'')}<label class="field">Student groups ${academic?'<span class="required">*</span>':'(optional)'}</label><div class="checks">${S.refs.groups.length?S.refs.groups.map(g=>`<label class="check-row"><input type="checkbox" name="groupIds" value="${g.id}" ${w.groupIds.includes(g.id)?'checked':''}><span>${esc(g.name)}<small class="muted"> · ${g.intakeYear} · ${esc(deptName(g.departmentId))}</small></span></label>`).join(''):'<p class="form-help">No student groups are configured. Ask an administrator to add them.</p>'}</div></div>`;}
 else {const course=S.refs.courses.find(c=>c.id===w.courseId);body=`<dl class="summary"><div><dt>Date</dt><dd>${fmtDate(w.date)}</dd></div><div><dt>Time · Asia/Colombo</dt><dd>${longTime(w.start)} — ${longTime(w.end)}</dd></div><div><dt>Session</dt><dd>${esc(w.type)}</dd></div><div><dt>Attendance</dt><dd>${w.attendance} people</dd></div><div><dt>Course</dt><dd>${esc(course?.code||'Not selected')}</dd></div><div><dt>Responsible requester</dt><dd>${esc(S.user.username)}</dd></div><div class="span-2"><dt>Purpose</dt><dd>${esc(w.purpose)}</dd></div><div class="span-2"><dt>Student groups</dt><dd>${w.groupIds.length?w.groupIds.map(id=>esc(S.refs.groups.find(g=>g.id===id)?.name||id)).join(', '):'None selected'}</dd></div></dl><div class="form-help">Your request will be Pending until an independent administrator approves it. A Pending request does not reserve the hall.</div>`;}
 showModal(w.id?'Update your request':'Make room for your session.',w.id?`Editing Pending booking #${w.id}`:'A few details. One well-planned session.',`${steps}<div class="wizard-room"><img src="${roomImage(h.id)}" alt=""><div><strong>${esc(roomLabel(h))}</strong><small>${esc(h.buildingName)} · ${h.capacity} seats</small></div></div><form id="wizard-form"><div class="error-box" role="alert"></div>${body}<div class="modal-footer wizard-actions">${w.step>1?button(icon('back')+' Back','wizard-back','btn btn-quiet'):button('Cancel','close-modal','btn btn-quiet')}<button type="submit" class="btn btn-primary">${w.step===3?(w.id?'Save request':'Send booking request'):'Continue'} ${icon('arrow')}</button></div></form>`);
}
function bookingDetail(id){
 const b=S.bookings.find(b=>b.id===id);if(!b)return;
 showModal('Your reservation, at a glance.',`Booking #${b.id} · ${b.hallLabel}`,`<div class="row between">${tag(b.status)}<small class="muted">${esc(b.type)}</small></div><dl class="summary"><div><dt>Date</dt><dd>${fmtDate(b.date)}</dd></div><div><dt>Time · Asia/Colombo</dt><dd>${longTime(b.start)} — ${longTime(b.end)}</dd></div><div><dt>People</dt><dd>${b.attendance}</dd></div><div><dt>Course</dt><dd>${esc(b.courseCode||'—')}</dd></div><div><dt>Requester</dt><dd>${esc(b.requesterName)}</dd></div><div><dt>Requested</dt><dd>${esc(b.requestedAt?.replace('T',' ')||'—')}</dd></div><div class="span-2"><dt>Purpose</dt><dd>${esc(b.purpose)}</dd></div>${b.reviewerName?`<div><dt>Reviewer</dt><dd>${esc(b.reviewerName)}</dd></div><div><dt>Decision</dt><dd>${esc(b.decision)}</dd></div><div class="span-2"><dt>Review note</dt><dd>${esc(b.reviewNote||'No note added.')}</dd></div>`:''}</dl>`,button('Done','close-modal','btn btn-primary'));
}
function confirmAction(title,message,endpoint,data,success){
 showModal(title,message,`<form id="confirm-form" data-endpoint="${endpoint}"><div class="error-box" role="alert"></div><div class="notice">${icon('info')}<span>This action updates the record and keeps its history.</span></div><div class="modal-footer">${button('Keep it as it is','close-modal','btn btn-quiet')}<button type="submit" class="btn btn-primary">Confirm ${icon('check')}</button></div></form>`);S.confirm={endpoint,data,success};
}
function reviewBooking(id,decision){
 const b=S.bookings.find(b=>b.id===id);if(!b)return;
 showModal(decision==='Approved'?'Give this session the green light.':'Decline this request.',`${b.hallLabel} · ${fmtDate(b.date)} · ${time(b.start)}–${time(b.end)}`,`<form id="review-form" data-id="${id}" data-decision="${decision}"><div class="error-box" role="alert"></div><div class="wizard-room"><div><strong>${esc(b.purpose)}</strong><small>Requested by ${esc(b.requesterName)} · ${b.attendance} people</small></div></div><label class="field">Review note (optional)<textarea name="note" maxlength="500" placeholder="Add a short note for the requester."></textarea></label><div class="form-help">${decision==='Approved'?'Availability and capacity will be checked again before approval.':'The request will remain in the booking history.'}</div><div class="modal-footer">${button('Go back','close-modal','btn btn-quiet')}<button type="submit" class="btn btn-primary">${decision==='Approved'?'Approve booking':'Decline request'} ${icon('check')}</button></div></form>`);
}
function catalogForm(kind){
 const c=catalogs[kind];if(!c)return;const dep=()=>select('Department','departmentId',opts(S.refs.departments,d=>d.departmentName,null), 'required');let fields='';
 if(kind==='departments')fields=field('Department name','name','','text','required maxlength="100"');
 if(kind==='buildings')fields=field('Building code','code','','text','required maxlength="20"')+field('Building name','name','','text','required maxlength="100"')+field('Location (optional)','location','','text','maxlength="255"');
 if(kind==='facilities')fields=field('Equipment type','name','','text','required maxlength="100"')+field('Description (optional)','description','','text','maxlength="255"');
 if(kind==='courses')fields=dep()+field('Course code','code','','text','required maxlength="20"')+field('Course title','title','','text','required maxlength="150"');
 if(kind==='groups')fields=dep()+field('Group name','name','','text','required maxlength="100"')+`<div class="form-grid">${field('Intake year','intakeYear',today().slice(0,4),'number','required min="1900" max="9999"')}${field('Study year','studyYear',1,'number','required min="1" max="255"')}</div>`;
 if(kind==='halls')fields=select('Building','buildingId',opts(S.refs.buildings,b=>`${b.code} · ${b.name}`,null),'required')+`<div class="form-grid">${field('Hall number','number','','text','required maxlength="20"')}${field('Floor (optional)','floor','','text','maxlength="10"')}</div>`+field('Seating capacity','capacity',50,'number','required min="1" max="2147483647"');
 showModal('Add '+c.singular+'.','A few details to keep your campus connected.',`<form id="catalog-form" data-kind="${kind}" class="stack"><div class="error-box" role="alert"></div>${fields}<div class="modal-footer">${button('Cancel','close-modal','btn btn-quiet')}<button type="submit" class="btn btn-primary">Save ${c.singular} ${icon('check')}</button></div></form>`);
}
function personForm(){showModal('Make room for someone new.','Provision an account with the right access.',`<form id="person-form" class="auth-form"><div class="error-box" role="alert"></div><div class="form-grid">${field('Full name','fullName','','text','required maxlength="150"')}${field('Email','email','','email','required maxlength="254"')}${field('Username','username','','text','required maxlength="50"')}${select('Role','role',['Student','Lecturer','AdminStaff'].map(r=>`<option value="${r}">${roleLabel(r)}</option>`).join(''))}${select('Department','departmentId',opts(S.refs.departments,d=>d.departmentName,null,'No department'))}<div></div>${passwordField('Password')}${passwordField('Confirm password','confirmation')}</div><div class="modal-footer">${button('Cancel','close-modal','btn btn-quiet')}<button type="submit" class="btn btn-primary">Create account ${icon('arrow')}</button></div></form>`);}
function hallSettings(id){const h=S.refs.halls.find(h=>h.id===id);showModal('Keep this hall in good shape.',roomLabel(h),`<form id="hall-settings-form" data-id="${id}" class="stack"><div class="error-box" role="alert"></div>${field('Seating capacity','capacity',h.capacity,'number','required min="1"')}${select('Operational status','status',['Active','Inactive'].map(s=>`<option ${h.status===s?'selected':''}>${s}</option>`).join(''))}<div class="notice">${icon('shield')}<span>Changes cannot invalidate an existing approved reservation.</span></div><div class="modal-footer">${button('Cancel','close-modal','btn btn-quiet')}<button type="submit" class="btn btn-primary">Save changes ${icon('check')}</button></div></form>`);}
function equipmentForm(hallId,facilityId){const old=S.refs.equipment.find(e=>e.hallId===hallId&&e.facilityId===facilityId);showModal('Everything ready for the next session.','Set the total and working quantity of a hall’s equipment.',`<form id="equipment-form" class="stack"><div class="error-box" role="alert"></div>${select('Hall','hallId',opts(S.refs.halls,h=>roomLabel(h),hallId),'required')}${select('Equipment type','facilityId',opts(S.refs.facilities,f=>f.name,facilityId),'required')}<div class="form-grid">${field('Total quantity','quantity',old?.quantity||1,'number','required min="1"')}${field('Working quantity','workingQuantity',old?.workingQuantity??1,'number','required min="0"')}</div><div class="modal-footer">${button('Cancel','close-modal','btn btn-quiet')}<button type="submit" class="btn btn-primary">Update equipment ${icon('check')}</button></div></form>`);}
function blockForm(){const d=plusDays(today(),1);showModal('A little time for maintenance.','Block a hall for a closure, repair or other planned work.',`<form id="block-form" class="stack"><div class="error-box" role="alert"></div>${select('Hall','hallId',opts(S.refs.halls,h=>roomLabel(h),null),'required')}<div class="form-grid">${field('Starts','start',d+'T09:00','datetime-local','required')}${field('Ends','end',d+'T12:00','datetime-local','required')}</div><label class="field">Reason<textarea name="reason" maxlength="255" required placeholder="For example, projector maintenance"></textarea></label><div class="notice">${icon('info')}<span>Approved reservations must be resolved before a hall can be blocked.</span></div><div class="modal-footer">${button('Cancel','close-modal','btn btn-quiet')}<button type="submit" class="btn btn-primary">Create hall block ${icon('check')}</button></div></form>`);}
function formData(form){const d=Object.fromEntries(new FormData(form));for(const k of ['id','hallId','courseId','departmentId','buildingId','facilityId','capacity','attendance','quantity','workingQuantity','intakeYear','studyYear'])if(k in d)d[k]=d[k]===''?null:Number(d[k]);return d;}
function passwordsMatch(d){if(d.password!==d.confirmation)throw new Error('The passwords do not match. Please enter them again.');if(Array.from(d.password||'').length<8)throw new Error('Choose a password with at least 8 characters.');delete d.confirmation;}
async function afterSave(message){closeModal();await refreshData();await navigate(S.page,true);toast(message);}
function clearPrivateState(){S.refs=null;S.bookings=[];S.schedule=[];S.people=[];S.blocks=[];S.reportData=null;S.wizard=null;S.confirm=null;}
const actions={
 restart:()=>start(), 'show-login':()=>renderAuth('login'),'show-register':()=>renderAuth('register'),'show-admin':()=>renderAuth('admin'),
 'toggle-password':el=>{const input=el.closest('.password-wrap').querySelector('input');input.type=input.type==='password'?'text':'password';el.setAttribute('aria-label',input.type==='password'?'Show password':'Hide password');},
 menu:()=>document.querySelector('.shell').classList.toggle('menu-open'),
 logout:async()=>{const data=await api('/auth/logout',{});clearPrivateState();Object.assign(S,data);closeModal();history.replaceState(null,'',location.pathname);renderAuth('login');},
 refresh:async()=>{await refreshData();await navigate(S.page,true);},
 'go-spaces':()=>navigate('spaces'),'go-schedule':()=>navigate('schedule'),
 'close-modal':()=>closeModal(),
 'clear-spaces':()=>{S.roomQuery='';S.building='';S.search=null;renderSpaces(document.querySelector('#main'));},
 'room-detail':el=>openRoom(Number(el.dataset.id)),
 'book-room':el=>openWizard(Number(el.dataset.id)),
 'new-booking':()=>openWizard(),
 'edit-booking':el=>openWizard(null,S.bookings.find(b=>b.id===Number(el.dataset.id))),
 'wizard-back':()=>{S.wizard.step--;renderWizard();},
 'filter-bookings':el=>{S.filter=el.dataset.filter;renderBookings(document.querySelector('#main'));},
 'booking-detail':el=>bookingDetail(Number(el.dataset.id)),
 'review-booking':el=>reviewBooking(Number(el.dataset.id),el.dataset.decision),
 'cancel-booking':el=>confirmAction('Let this space open up again?',`Cancel booking #${el.dataset.id}? You can make a new request whenever you need one.`,'/bookings/action',{id:Number(el.dataset.id),action:'Cancelled'},'Booking cancelled. Its history has been kept.'),
 'complete-booking':el=>confirmAction('A session well concluded.',`Mark booking #${el.dataset.id} as Completed?`,'/bookings/action',{id:Number(el.dataset.id),action:'Completed'},'Booking marked Completed.'),
 'schedule-day':el=>{S.scheduleDate=el.dataset.date;navigate('schedule',true);},
 'schedule-week':el=>{S.scheduleDate=plusDays(S.scheduleDate||today(),Number(el.dataset.offset));navigate('schedule',true);},
 'open-catalog':el=>{S.catalog=el.dataset.kind;renderCatalog(document.querySelector('#main'));},
 'catalog-back':()=>{S.catalog=null;renderCatalog(document.querySelector('#main'));},
 'new-catalog':el=>catalogForm(el.dataset.kind),
 'hall-settings':el=>hallSettings(Number(el.dataset.id)),
 'new-person':()=>personForm(),
 'care-view':el=>{S.care=el.dataset.view;navigate('care',true);},
 'new-equipment':()=>equipmentForm(),
 'edit-equipment':el=>equipmentForm(Number(el.dataset.hall),Number(el.dataset.facility)),
 'new-block':()=>blockForm(),
 'cancel-block':el=>confirmAction('Open this hall for reservations?',`Cancel maintenance block #${el.dataset.id}?`,'/blocks/cancel',{id:Number(el.dataset.id)},'Hall block cancelled.'),
 'report-kind':el=>{S.report=el.dataset.kind;navigate('reports',true);}
};
document.addEventListener('click',async event=>{
 const nav=event.target.closest('[data-nav]');if(nav){S.filter=nav.dataset.nav==='reviews'?'Pending':'All';await navigate(nav.dataset.nav);return;}
 const el=event.target.closest('[data-action]');if(!el||el.disabled||el.dataset.busy)return;
 const fn=actions[el.dataset.action];if(!fn)return;
 el.dataset.busy='1';try{await fn(el);}catch(e){toast(e.message,true);}finally{delete el.dataset.busy;}
});
document.addEventListener('input',event=>{if(event.target.id==='room-query'){S.roomQuery=event.target.value;filterSpaces();}});
document.addEventListener('change',event=>{
 if(event.target.id==='building-filter'){S.building=event.target.value;filterSpaces();}
 if(event.target.id==='schedule-date'){S.scheduleDate=event.target.value||today();navigate('schedule',true);}
 if(event.target.id==='people-department'){S.peopleDepartment=event.target.value;navigate('people',true);}
});
document.addEventListener('keydown',event=>{if(event.key==='Escape')document.querySelector('.shell')?.classList.remove('menu-open');});
modal.addEventListener('close',()=>{if(!modal.open)modal.innerHTML='';});
window.addEventListener('popstate',()=>{if(S.user)navigate(location.hash.slice(1)||'overview',true);});
document.addEventListener('submit',async event=>{
 const form=event.target;if(!(form instanceof HTMLFormElement))return;event.preventDefault();if(form.dataset.busy)return;
 form.dataset.busy='1';const error=form.querySelector('.error-box');if(error)error.textContent='';busy(form,true);
 try{
  let d=formData(form);
  switch(form.id){
   case 'connection-form':Object.assign(S,await api('/setup/connection',d));renderAuth('login');toast('Your workspace is connected.');break;
   case 'login-form':Object.assign(S,await api('/auth/signin',d));await refreshData();S.filter='All';await navigate('overview');break;
   case 'register-form':passwordsMatch(d);await api('/auth/register',d);renderAuth('login');toast('Your account is ready. Sign in to explore your campus.');break;
   case 'admin-setup-form':passwordsMatch(d);await api('/setup/admin',d);renderAuth('login');toast('Administrator created. Sign in with your new account.');break;
   case 'availability-form':{
    if(d.end<=d.start)throw new Error('Choose an end time after the start time.');
    const results=await api(`/availability?date=${d.date}&start=${d.start}&end=${d.end}&attendance=${d.attendance}`);S.search=d;renderSpaces(document.querySelector('#main'),results);break;
   }
   case 'wizard-form':{
    const w=S.wizard;
    if(w.step===1){if(d.end<=d.start)throw new Error('Choose an end time after the start time.');if(d.date<today()||(d.date===today()&&d.start<=timeNow()))throw new Error('Choose a start time in the future.');const h=S.refs.halls.find(h=>h.id===d.hallId);if(!h||d.attendance>h.capacity)throw new Error('This group is larger than the hall capacity. Choose a larger hall.');Object.assign(w,d);w.step=2;renderWizard();}
    else if(w.step===2){d.groupIds=new FormData(form).getAll('groupIds').map(Number);if(['Lecture','Tutorial'].includes(w.type)&&(!d.courseId||!d.groupIds.length))throw new Error('Choose a course and at least one student group.');Object.assign(w,d);w.step=3;renderWizard();}
    else {const saved=await api('/bookings',w);await refreshData();const id=saved.id;showModal('Your request is on its way.','A little planning, a lot of possibility.',`<div class="confirmation"><span class="success-mark">${icon('check')}</span><h2>Booking #${id} submitted.</h2><p>Your request is Pending. An independent administrator will review it before the hall is reserved.</p>${button('View my bookings '+icon('arrow'),'wizard-finished','btn btn-primary')}</div>`);actions['wizard-finished']=()=>{closeModal();S.filter='Pending';navigate('bookings');};}
    break;
   }
   case 'review-form':await api('/bookings/action',{id:Number(form.dataset.id),action:form.dataset.decision,note:d.note||null});await afterSave(form.dataset.decision==='Approved'?'Booking approved. The hall is reserved.':'Request declined. The record has been kept.');break;
   case 'confirm-form':await api(S.confirm.endpoint,S.confirm.data);await afterSave(S.confirm.success);break;
   case 'catalog-form':await api('/catalog/'+form.dataset.kind,d);await afterSave('Campus details updated.');break;
   case 'person-form':passwordsMatch(d);await api('/accounts',d);await afterSave('Account created. They can sign in with their new credentials.');break;
   case 'hall-settings-form':await api('/catalog/hall-status',{hallId:Number(form.dataset.id),...d});await afterSave('Hall settings updated.');break;
   case 'equipment-form':if(d.workingQuantity>d.quantity)throw new Error('Working quantity cannot be greater than the total.');await api('/catalog/equipment',d);await afterSave('Equipment quantities updated.');break;
   case 'block-form':if(d.end<=d.start)throw new Error('Choose an end after the start of the block.');await api('/blocks',d);await afterSave('Hall block created.');break;
  }
 }catch(e){formError(form,e.message);}
 finally{form.querySelectorAll('input[type=password]').forEach(input=>{if(['login-form','admin-setup-form','register-form','person-form','connection-form'].includes(form.id))input.value='';});busy(form,false);delete form.dataset.busy;}
});
start();
