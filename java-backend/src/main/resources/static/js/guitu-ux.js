(function(){
  'use strict';
  var notice=document.createElement('div');
  notice.className='guitu-network';notice.setAttribute('role','status');notice.hidden=true;
  document.body.appendChild(notice);
  function connection(){
    if(navigator.onLine===false){notice.textContent='网络已断开，请连接网络后重试。当前填写的内容仍保留在页面中。';notice.hidden=false;}
    else{notice.hidden=true;notice.textContent='';}
  }
  window.addEventListener('offline',connection);window.addEventListener('online',connection);connection();
  if(window===window.top&&!document.body.classList.contains('guitu-shell')){
    var home=document.createElement('a');home.href='/index.html';home.className='guitu-page-home';home.textContent='← 返回工作台';document.body.insertBefore(home,document.body.firstChild);
  }
  if(!document.body.classList.contains('guitu-shell'))return;
  var dialog=document.getElementById('guituSearch'),input=document.getElementById('guituSearchInput'),results=document.getElementById('guituSearchResults'),opener=document.getElementById('guituSearchOpen');
  function renderSearch(){
    var words=input.value.trim().toLowerCase().split(/\s+/),seen={},matches=[];
    var entries=[{title:'工作台',href:'page/welcome.html'},{title:'基本资料',href:'page/user-setting.html'},{title:'修改密码',href:'page/user-password.html'}];
    document.querySelectorAll('.layuimini-menu-left [layuimini-href]').forEach(function(link){entries.push({title:link.textContent.trim(),href:link.getAttribute('layuimini-href')});});
    entries.forEach(function(entry){if(!seen[entry.href]&&words.every(function(word){return entry.title.toLowerCase().indexOf(word)!==-1;})){seen[entry.href]=true;matches.push(entry);}});
    results.textContent='';
    if(!matches.length){var empty=document.createElement('li');empty.className='guitu-search-empty';empty.textContent='未找到相关功能，试试“任务”“资料”或“课程”。';results.appendChild(empty);}
    matches.forEach(function(entry){var li=document.createElement('li'),a=document.createElement('a');a.textContent=entry.title;a.href=entry.href;a.setAttribute('layuimini-href',entry.href);a.addEventListener('click',function(event){event.preventDefault();dialog.close();});li.appendChild(a);results.appendChild(li);});
  }
  function openSearch(){if(dialog.open)return;input.value='';renderSearch();dialog.showModal();input.focus();}
  opener.addEventListener('click',openSearch);
  document.getElementById('guituSearchClose').addEventListener('click',function(){dialog.close();});
  dialog.addEventListener('click',function(event){if(event.target===dialog){var rect=dialog.getBoundingClientRect();if(event.clientX<rect.left||event.clientX>rect.right||event.clientY<rect.top||event.clientY>rect.bottom)dialog.close();}});
  dialog.addEventListener('close',function(){opener.focus();});
  input.addEventListener('input',renderSearch);
  input.addEventListener('keydown',function(event){if(event.key==='ArrowDown'||event.key==='Enter'){var first=results.querySelector('a');if(first){event.preventDefault();if(event.key==='Enter')first.click();else first.focus();}}});
  document.addEventListener('keydown',function(event){if((event.ctrlKey||event.metaKey)&&event.key.toLowerCase()==='k'){event.preventDefault();openSearch();}});
  function nameFrames(){document.querySelectorAll('.layui-tab-item iframe').forEach(function(frame){if(!frame.title){var item=frame.closest('.layui-tab-item'),tabs=document.querySelectorAll('.layui-tab-content>.layui-tab-item'),index=Array.prototype.indexOf.call(tabs,item),tab=document.querySelectorAll('.layui-tab-title>li')[index];frame.title=tab?tab.textContent.replace(/ဆ/g,'').trim():'暖寻灯塔内容';}});}
  new MutationObserver(nameFrames).observe(document.querySelector('.layui-tab-content'),{childList:true,subtree:true});nameFrames();
})();
