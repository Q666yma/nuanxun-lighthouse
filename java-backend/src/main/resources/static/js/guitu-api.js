(function(window){
  'use strict';
  async function request(url, options) {
    options=Object.assign({credentials:'same-origin',cache:'no-store'},options||{});
    var controller=new AbortController(),timer=setTimeout(function(){controller.abort();},options.timeout||20000);
    delete options.timeout;options.signal=controller.signal;
    if(options.body && !(options.body instanceof FormData) && typeof options.body!=='string') {
      options.headers=Object.assign({'Content-Type':'application/json'},options.headers||{});options.body=JSON.stringify(options.body);
    }
    try {
      var response=await fetch(url,options),data;
      try{data=await response.json();}catch(error){throw new Error('服务返回的数据无法读取，请重试');}
      if(!response.ok||data.success===false||(data.code!==undefined&&data.code!==0)) {
        var failed=new Error(data.message||data.msg||data.error||data.export_message||'操作失败，请重试');failed.status=response.status;throw failed;
      }
      return data;
    } catch(error) {
      if(error.name==='AbortError')throw new Error('请求超时，请重试并查看最新状态');
      throw error;
    } finally {clearTimeout(timer);}
  }
  function id(){return crypto.randomUUID();}
  async function listAll(url){var rows=[],page=1,result;do{result=await request(url+(url.indexOf('?')<0?'?':'&')+'page='+page+'&limit=100');if(!Array.isArray(result.data))throw new Error('列表数据异常');rows=rows.concat(result.data);page++;}while(result.data.length&&rows.length<result.count);return rows;}
  function text(value){return value===null||value===undefined?'':String(value);}
  function escape(value){return text(value).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
  var statuses={waiting:'等待救援',rescuing:'救援中',confirming:'待家属确认',finished:'已完成',cancelled:'已撤回',visitor:'未申请',pending:'待审核',available:'已审核',rejected:'未通过',disabled:'已停用',active:'正常',processing:'处理中',resolved:'已处理',draft:'草稿',published:'已发布',not_requested:'未请求邮件',sent:'邮件服务已接受',failed:'发送失败'};
  window.GuituApi={request:request,listAll:listAll,id:id,escape:escape,text:text,status:function(value){return statuses[value]||text(value);}};
})(window);
