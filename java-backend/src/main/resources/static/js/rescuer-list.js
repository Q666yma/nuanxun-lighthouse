(function(){
  'use strict';
  layui.use(['table','layer'],function(){
    var table=layui.table,layer=layui.layer,api=GuituApi,status=document.getElementById('statusMsg'),ready=false;
    function note(message){status.textContent=message;document.getElementById('debugInfo').textContent=message;}
    document.getElementById('debugBtn').onclick=function(){document.getElementById('debugPanel').style.display='block';};
    document.getElementById('toggleDebugBtn').onclick=function(){document.getElementById('debugPanel').style.display='none';};
    document.getElementById('clearLogBtn').onclick=function(){document.getElementById('debugInfo').textContent='';};
    async function load(){note('正在读取相关队员…');try{
      await api.request('/rescuer/check-permission');
      if(ready){table.reload('rescuerTable',{page:{curr:1}});return;}
      ready=true;table.render({elem:'#rescuerTable',id:'rescuerTable',url:'/rescuer/list-data',page:true,limit:15,limits:[10,15,20,50],toolbar:true,defaultToolbar:['filter','exports','print'],cols:[[
        {type:'numbers',title:'序号',width:60},{field:'name',title:'姓名',width:100,templet:function(r){return api.escape(r.name);}},
        {field:'status',title:'状态',width:100,templet:function(r){return api.escape(api.status(r.status));}},
        {field:'location',title:'所在地区',minWidth:150,templet:function(r){return api.escape(r.location);}},
        {field:'skillTags',title:'技能标签',minWidth:250,templet:function(r){return '<div class="tag-list">'+(r.skillTags||[]).map(function(t){return '<div class="tag-item">'+api.escape(t)+'</div>';}).join('')+'</div>';}},
        {title:'操作',width:100,toolbar:'#tableRowBar',align:'center'}]],done:function(result){note(result.code===0?'共 '+result.count+' 位相关队员。联系方式由队员自行决定是否共享。':result.msg||'读取失败，请重试');},error:function(){note('队员数据读取失败，请点击刷新重试');}});
    }catch(error){note(error.message);}}
    document.getElementById('refreshBtn').onclick=load;
    table.on('tool(rescuerTable)',function(event){if(event.event!=='contact')return;var row=event.data,box=document.createElement('div');box.style.padding='20px';
      function text(tag,value){var n=document.createElement(tag);n.textContent=value;box.append(n);return n;}
      text('h3',row.name||'相关队员');text('p',row.location||'未填写地区');
      if(row.shareContact&&(row.contactPhone||row.contactEmail)){
        [['电话',row.contactPhone,'tel:'],['邮箱',row.contactEmail,'mailto:']].forEach(function(v){if(!v[1])return;var p=text('p',v[0]+'：'),a=document.createElement('a');a.textContent=v[1];a.href=v[2]+encodeURIComponent(v[1]);p.append(a);});
      }else text('p','对方未共享联系方式，可从共同任务中沟通。');
      var tasks=row.sharedTasks||[];text('p','任务聊天由该任务家属、队员及获授权管理员共同查看。');
      tasks.forEach(function(task){var a=text('a',(task.elderName||'任务')+' · '+api.status(task.status)+' · 进入任务沟通');a.className='layui-btn layui-btn-sm';a.href='/task/task-detail/'+encodeURIComponent(task.id)+'#taskChat';});
      if(!tasks.length)text('p','暂无可进入的共同任务。');
      box.style.display='none';document.body.append(box);layer.open({type:1,title:'联系搜救队员',area:[Math.min(480,window.innerWidth-24)+'px','auto'],content:layui.$(box),end:function(){box.remove();}});
    });load();
  });
})();
