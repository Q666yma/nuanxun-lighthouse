(function(window){
  'use strict';
  async function load(select,status) {
    select.disabled=true;select.replaceChildren(new Option('请选择参与的任务',''));status.textContent='正在读取本人参与的任务…';
    try {
      var user=await GuituApi.request('/api/users/me'),page=1,rows=[],total=0;
      if(user.userType==='admin') {
        var requested=new URLSearchParams(location.search).get('taskId');
        if(!requested||!/^\d+$/.test(requested))throw new Error('请先从管理员任务详情进入具体任务的识别工具');
        var detail=await GuituApi.request('/task/'+requested);if(!detail.data||!detail.data.id)throw new Error('任务详情不可用');rows=[detail.data];
      } else {
        if(user.userType!=='rescuer'||user.rescuerStatus!=='available'||user.status==='disabled')throw new Error('此工具只向已审核并参与任务的搜救者开放');
        do {
        var result=await GuituApi.request('/task/my-list-data?page='+page+'&limit=100');
        if(!Array.isArray(result.data))throw new Error('任务列表数据异常');
        rows=rows.concat(result.data);total=Number(result.count)||rows.length;page++;
        }while(result.data.length&&rows.length<total);
      }
      rows.filter(function(task){return task.status==='rescuing'||task.status==='confirming';}).forEach(function(task){
        select.add(new Option('#'+task.id+' '+(task.elderName||task.location||'搜救任务')+' · '+GuituApi.status(task.status),String(task.id)));
      });
      if(select.options.length===1){status.textContent='暂无正在参与的任务。接单或获指派后可在任务范围内比对。';return;}
      select.disabled=false;var selected=new URLSearchParams(location.search).get('taskId');
      if(selected&&Array.from(select.options).some(function(option){return option.value===selected;}))select.value=selected;
      status.textContent='仅比对所选任务的授权资料，结果仍需人工核实。';select.dispatchEvent(new Event('change'));
    }catch(error){status.textContent=error.message;var retry=document.createElement('button');retry.type='button';retry.textContent='重新加载任务';retry.className='layui-btn layui-btn-primary layui-btn-sm';retry.addEventListener('click',function(){load(select,status);});status.append(' ',retry);}
  }
  function requireTask(select){if(!select.value||select.disabled)throw new Error('请先选择本人正在参与的任务');return select.value;}
  window.GuituTaskSelector={load:load,requireTask:requireTask};
})(window);
