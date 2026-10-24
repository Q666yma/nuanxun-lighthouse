(function(){
  'use strict';
  var api=GuituApi,body=document.getElementById('adminBody'),status=document.getElementById('adminStatus');
  var currentUser=null;
  var view=new URLSearchParams(location.search).get('view')||'overview';
  var titles={overview:'管理首页',tasks:'任务协调',rescuers:'队员审核与管理',users:'用户管理',notifications:'通知与加急处理',content:'内容管理',statistics:'统计',system:'系统与操作记录'};
  if(!titles[view])view='overview';document.getElementById('adminTitle').textContent=titles[view];
  function node(tag,text,cls){var element=document.createElement(tag);if(text!==undefined)element.textContent=text;if(cls)element.className=cls;return element;}
  function button(text,run){var b=node('button',text,'layui-btn layui-btn-sm');b.type='button';b.addEventListener('click',run);return b;}
  function section(title){var wrapper=node('section');wrapper.append(node('h3',title));body.append(wrapper);return wrapper;}
  function field(form,label,name,type,options){
    var row=node('div',undefined,'layui-form-item'),lab=node('label',label,'layui-form-label'),wrap=node('div',undefined,'layui-input-block'),input;
    lab.htmlFor='admin-'+name;
    if(type==='select'){input=node('select');input.className='layui-input';(options||[]).forEach(function(item){input.add(new Option(item[1],item[0]));});}
    else if(type==='textarea'){input=node('textarea');input.className='layui-textarea';}
    else{input=node('input');input.type=type||'text';input.className='layui-input';}
    input.id='admin-'+name;input.name=name;wrap.append(input);row.append(lab,wrap);form.append(row);return input;
  }
  function formAt(container,title,fields,onSubmit){
    var form=node('form');form.append(node('h4',title));var inputs={};
    fields.forEach(function(f){inputs[f[1]]=field(form,f[0],f[1],f[2],f[3]);if(f[4])inputs[f[1]].required=true;});
    var save=node('button','提交','layui-btn');save.type='submit';var result=node('p');result.setAttribute('role','status');form.append(save,result);
    form.addEventListener('submit',async function(event){event.preventDefault();if(save.disabled)return;save.disabled=true;result.textContent='正在提交…';
      var values={};Object.keys(inputs).forEach(function(key){values[key]=inputs[key].value.trim();});
      try{var response=await onSubmit(values);result.textContent=response&&response.message||'操作已保存。请刷新查看最新记录。';}
      catch(error){result.textContent=error.message;}finally{save.disabled=false;}
    });container.append(form);return {form:form,inputs:inputs,result:result};
  }
  function table(container,rows,columns,actions){
    if(!rows.length){container.append(node('p','暂无记录'));return;}
    var table=node('table',undefined,'layui-table'),head=node('thead'),hr=node('tr'),tbody=node('tbody');
    columns.forEach(function(c){hr.append(node('th',c[1]));});if(actions)hr.append(node('th','操作'));head.append(hr);
    rows.forEach(function(row){var tr=node('tr');columns.forEach(function(c){var value=typeof c[0]==='function'?c[0](row):row[c[0]];tr.append(node('td',api.text(value)));});
      if(actions){var td=node('td');actions(row,td);tr.append(td);}tbody.append(tr);
    });table.append(head,tbody);container.append(table);
  }
  function modal(title,fields,submit){
    var panel=node('div');panel.style.padding='20px';panel.style.display='none';document.body.append(panel);var form=formAt(panel,title,fields,submit);
    layui.use(['layer'],function(layer){
      layer.open({type:1,title:api.escape(title),area:[Math.min(680,window.innerWidth-24)+'px','auto'],content:layui.$(panel),end:function(){panel.remove();}});
    });return form;
  }
  async function list(container,url,columns,actions){
    var page=1,tableArea=node('div'),nav=node('div'),before=button('上一页',function(){page--;load();}),after=button('下一页',function(){page++;load();}),info=node('span');
    nav.append(before,info,after);container.append(tableArea,nav);
    async function load(){before.disabled=after.disabled=true;tableArea.textContent='加载中…';try{var result=await api.request(url+(url.indexOf('?')<0?'?':'&')+'page='+page+'&limit=20');if(!Array.isArray(result.data))throw new Error('列表数据异常');tableArea.replaceChildren();table(tableArea,result.data,columns,actions);info.textContent=' 第'+page+'页 · 共'+result.count+'条 ';before.disabled=page===1;after.disabled=page*20>=result.count;}catch(error){tableArea.textContent=error.message;tableArea.append(button('重试',load));}}
    await load();return load;
  }
  async function summary(result){var d=result.data;
    table(body,[d],[['pendingRescuers','待审核队员'],['pendingUrgent','待处理加急'],['activeTasks','进行中任务'],['users','账号数']]);
    var links=node('p');[['队员审核','rescuers'],['任务协调','tasks'],['加急处理','notifications']].forEach(function(x){var a=node('a',x[0],'layui-btn');a.href='/page/admin.html?v=20260923-features&view='+x[1];links.append(a);});var dashboard=node('a','搜救可视化大屏','layui-btn layui-btn-normal');dashboard.href='/show/show-dummy';links.append(dashboard);body.append(links);
  }
  async function users(){await list(body,'/api/admin/users',[['username','账号'],['name','姓名'],[function(r){return {family:'家属',rescuer:'搜救者',admin:'管理员'}[r.userType]||r.userType;},'角色'],[function(r){return api.status(r.status);},'账号状态'],['rescuerStatus','队员审核'],['createdAt','创建时间']],function(row,td){
    if(row.uuid===currentUser.uuid){td.append(node('span','本人账号，请使用个人设置'));return;}
    td.append(button(row.status==='disabled'?'启用':'停用',function(){modal('修改账号状态',[['原因','reason','textarea',null,true]],function(v){return api.request('/api/admin/users/'+encodeURIComponent(row.uuid)+'/status',{method:'POST',body:{status:row.status==='disabled'?'active':'disabled',reason:v.reason}});});}));
    td.append(button('重置密码',function(){modal('重置账号密码',[['新密码','newPassword','password',null,true],['重置原因','reason','textarea',null,true]],function(v){if(new TextEncoder().encode(v.newPassword).length>72||v.newPassword.length<8)throw new Error('密码至少8位，最多72字节');return api.request('/api/admin/users/'+encodeURIComponent(row.uuid)+'/password',{method:'POST',body:v});});}));
    td.append(button('调整角色',function(){modal('调整账号角色',[['新角色','role','select',[['family','家属'],['rescuer','搜救者'],['admin','管理员']],true],['原因','reason','textarea',null,true]],function(v){if(!confirm('确认将该账号调整为'+v.role+'？'))throw new Error('已取消');return api.request('/api/admin/users/'+encodeURIComponent(row.uuid)+'/role',{method:'POST',body:v});});}));
  });}
  async function rescuers(){await list(body,'/api/admin/rescuers',[['name','姓名'],['username','账号'],['location','地区'],['skillsDescription','技能说明'],[function(r){return api.status(r.status);},'审核状态'],[function(r){return api.status(r.accountStatus);},'账号状态']],function(row,td){
    if(row.status!=='pending')return;
    td.append(button('审核申请',function(){modal('审核 '+row.name,[['决定','decision','select',[['approve','审核通过'],['reject','不通过']],true],['审核说明','reason','textarea',null,true]],function(v){return api.request('/api/admin/rescuers/'+encodeURIComponent(row.uuid)+'/review',{method:'POST',body:v});});}));
  });}
  async function tasks(){await list(body,'/api/admin/tasks',[['id','任务'],['elderName','走失人员'],['familyName','家属'],['location','地点'],[function(r){return api.status(r.status);},'状态']],function(row,td){
    var detail=node('a','处理详情','layui-btn layui-btn-primary layui-btn-sm');detail.href='/task/task-detail/'+row.id;td.append(detail);
    if(row.status==='finished'||row.status==='cancelled')return;
    td.append(button('调度队员',async function(){try{var result=await api.listAll('/api/admin/rescuers?status=available');var choices=result.filter(function(r){return r.accountStatus!=='disabled';}).map(function(r){return [r.uuid,r.name+' · '+r.location];});if(!choices.length)throw new Error('暂无已审核的可调度队员');modal('任务 #'+row.id+' 调度',[['队员','rescuerUuid','select',choices,true],['操作','action','select',[['assign','指派参与'],['unassign','撤销指派']],true],['原因','reason','textarea',null,true]],function(v){return api.request('/api/admin/tasks/'+row.id+'/'+v.action,{method:'POST',body:{rescuerUuid:v.rescuerUuid,reason:v.reason}});});}catch(error){layui.use(['layer'],function(layer){layer.msg(error.message);});}}));
    td.append(button('异常结束',function(){modal('异常结束任务 #'+row.id,[['处理原因','reason','textarea',null,true]],function(v){if(!confirm('确认异常结束此任务并记录原因？'))throw new Error('已取消');return api.request('/api/admin/tasks/'+row.id+'/close',{method:'POST',body:v});});}));
  });}
  async function notifications(){var urgent=section('任务加急与现场风险');await list(urgent,'/api/admin/urgent',[['taskId','任务'],['senderName','提交人'],['message','紧急情况'],[function(r){return {submitted:'待处理',pending:'待处理',processing:'处理中',resolved:'已处理'}[r.status]||r.status;},'处理状态'],['reply','处理回复'],['createdAt','提交时间']],function(row,td){if(row.status==='resolved')return;td.append(button('处理',function(){modal('处理加急记录',[['状态','status','select',[['processing','处理中'],['resolved','已处理']],true],['回复','reply','textarea',null,true]],function(v){return api.request('/api/admin/urgent/'+row.id+'/handle',{method:'POST',body:v});});}));});
    var compose=section('向明确任务发布站内通知'),requestId=api.id(),previous='';
    formAt(compose,'通知对象由任务参与关系确定',[['任务编号','taskId','number',null,true],['接收对象','audience','select',[['all','该任务所有参与者'],['family','任务家属'],['rescuers','参与搜救者']],true],['标题','title','text',null,true],['内容','message','textarea',null,true]],function(v){var current=JSON.stringify(v);if(current!==previous){requestId=api.id();previous=current;}v.taskId=Number(v.taskId);v.requestId=requestId;return api.request('/api/admin/notifications',{method:'POST',body:v});});
    await list(section('通知记录'),'/api/admin/notifications',[['taskId','任务'],['recipientName','接收人'],['title','标题'],['message','内容'],[function(r){return {not_requested:'未请求邮件',pending:'待发送',sending:'发送中',sent:'邮件服务已接受',failed:'发送失败'}[r.emailStatus]||r.emailStatus;},'邮件状态'],[function(r){return r.readAt?'已读':'未读';},'阅读状态'],['createdAt','时间']]);
  }
  async function content(){var compose=section('维护课程与公告'),editor=formAt(compose,'新建或编辑内容',[['内容编号（编辑时填）','id','number'],['内容类型','kind','select',[['course','课程'],['announcement','公告']],true],['标题','title','text',null,true],['正文/说明','body','textarea'],['课程文件地址','url','text'],['分类','category','text'],['时长','duration','text'],['受众','audience','select',[['both','家属与搜救者'],['family','家属'],['rescuer','搜救者']],true],['发布状态','status','select',[['draft','草稿'],['published','已发布']],true]],function(v){if(v.id)v.id=Number(v.id);else delete v.id;return api.request('/api/admin/content',{method:'POST',body:v});});
    await list(section('内容与感谢墙审核'),'/api/admin/content',[['id','编号'],['kind','类型'],['title','标题'],['authorName','作者'],['audience','受众'],[function(r){return api.status(r.status);},'状态']],function(row,td){
      if(row.kind!=='thanks')td.append(button('编辑',function(){Object.keys(editor.inputs).forEach(function(key){editor.inputs[key].value=api.text(row[key]);});editor.form.scrollIntoView({block:'start'});}));
      td.append(button('审核',function(){modal('审核内容',[['结果','status','select',[['published','发布'],['rejected','不通过']],true],['原因','reason','textarea',null,true]],function(v){return api.request('/api/admin/content/'+row.id+'/review',{method:'POST',body:v});});}));
      td.append(button('删除',async function(){if(!confirm('确认删除此内容？'))return;try{await api.request('/api/admin/content/'+row.id,{method:'DELETE'});status.textContent='内容已删除，请刷新列表。';}catch(error){status.textContent=error.message;}}));
    });
  }
  async function system(){var result=await api.request('/api/admin/system'),d=result.data;table(body,[d],[['database','数据库'],['notificationChannel','通知渠道'],[function(r){return r.faceConfigured?'已配置':'未配置';},'人脸配置'],[function(r){return r.voiceConfigured?'已配置':'未配置';},'声纹配置']]);body.append(node('p','配置存在不代表外部服务当前可用；比对与发送结果以具体操作返回为准。'));
    await list(section('关键操作记录'),'/api/admin/audit',[['actorName','操作者'],['action','操作'],['targetType','对象类型'],['targetId','对象'],['reason','原因'],['createdAt','时间']]);
  }
  async function load(){
    body.hidden=true;body.replaceChildren();status.textContent='正在加载…';
    try{
      var isSummary=view==='overview'||view==='statistics';
      var results=await Promise.all([
        api.request('/api/users/me'),
        isSummary?api.request('/api/admin/summary'):Promise.resolve(null)
      ]);
      var user=results[0];
      if(user.userType!=='admin'||user.status==='disabled')throw new Error('此页面仅管理员可用');
      currentUser=user;
      await ({overview:summary,statistics:summary,tasks:tasks,rescuers:rescuers,users:users,notifications:notifications,content:content,system:system}[view])(results[1]);
      status.textContent='';body.hidden=false;
    }catch(error){status.textContent=error.message;}
  }
  document.getElementById('adminRefresh').addEventListener('click',load);
  layui.use(['layer']);
  load();
})();
