layui.use(['layer', 'form', 'element'], function () {
  const { layer, form, element } = layui;

  /* 端点与会话 */
  const api = window.SCENE_API_URL;                 // = /api/scene-train
  let sid = '';
  let lastReport = null;
  let lastReportScene = '';
  let roundCount = 0;
  /* DOM */
  const $msg   = $('#msgBox'),
        $input = $('#input'),
        $btn   = $('#sendBtn'),
        $score = $('#scoreCard'),
        $scoreBody = $('#scoreBody');

  let scene = "";               // 当前场景 key
  let busy = false;
  let finished = false;
  let activeSceneRequest = 0;
  $('#restartScene').on('click',function(){if(!scene){layer.msg('请先选择场景');return;}if(busy)return;$msg.empty();$score.hide();initScene();});
  $('#viewSceneReport').on('click',function(){if(lastReport)showReport(lastReport);});
  $('#downloadSceneReport').on('click',function(){if(!lastReport)return;GuituFiles.save(new Blob([reportText(lastReport)],{type:'text/plain;charset=utf-8'}),'场景训练报告.txt');});
  $('#sceneReportHistory').on('click',loadHistory);

  /* ========= 场景选择监听 ========= */
  form.on('select(sceneSel)', function (data) {
    scene = data.value;
    if (!scene) {                // 未选中
      $btn.prop('disabled', true);
      return;
    }
    // 初始化界面
    $msg.empty();
    $score.hide();
    $btn.prop('disabled', true);
    initScene();
  });

  /* ========= 初始化场景（系统首句） ========= */
  async function initScene() {
    const request = ++activeSceneRequest;
    busy = true;
    finished = false;
    sid = crypto.randomUUID();lastReport=null;roundCount=0;
    $('#viewSceneReport,#downloadSceneReport').prop('disabled',true);
    $('#roundProgress').text('正在准备场景…');
    appendMsg("系统已进入场景，请开始对话", "sys");
    try {
      const res = await post({ cmd: "start", scene });
      if (request !== activeSceneRequest) return;
      appendMsg(res.reply, "bot");
      $('#roundProgress').text('已完成 0 / 5 轮');
    } catch (error) {
      if (request !== activeSceneRequest) return;
      appendMsg(error.message + '，可重新选择场景重试。', 'sys');
      scene = '';
    } finally {
      if (request === activeSceneRequest) { busy = false; $btn.prop('disabled', !scene); }
    }
  }

  /* ========= 发送按钮 / Enter ========= */
  $btn.on('click', send);
  $input.on('keydown', e => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  });

  async function send() {
    if (busy || finished) return;
    if (!scene) { layer.msg('请选择场景', { icon: 0 }); return; }

    const txt = $input.val().trim();
    if (!txt) { layer.msg('请输入内容', { icon: 0 }); return; }

    appendMsg(txt, "user");
    $input.val("");
    $btn.prop('disabled', true);
    busy = true;
    const request = activeSceneRequest;
    try {
      const data = await post({ cmd: "talk", content: txt });
      if (request !== activeSceneRequest) return;
      appendMsg(data.reply, "bot");
      if (data.score) showScore(data.score);
      roundCount++; $('#roundProgress').text('已完成 '+roundCount+' / 5 轮');
      finished = Boolean(data.done);
      if (data.done && data.report) {
        lastReport=data.report;lastReportScene=scene;
        $('#viewSceneReport,#downloadSceneReport').prop('disabled',false);showReport(data.report);
        if(data.reportSaved === false) appendMsg(data.reportMessage || '历史报告保存失败，请立即下载本轮报告。','sys');
      }
    } catch (error) {
      if (request !== activeSceneRequest) return;
      appendMsg(error.message, 'sys');
      if (!$input.val()) $input.val(txt);
    } finally {
      if (request === activeSceneRequest) { busy = false; $btn.prop('disabled', finished); }
    }
  }

  /* ========= UI ========= */
  function appendMsg(text, role) {
    const div = $('<div class="msg ' + role + '"></div>').append($('<div class="bubble"></div>').text(text || '暂无回复'));
    $msg.append(div);
    $msg.scrollTop($msg[0].scrollHeight);
  }

  function showScore(sc) {
    $scoreBody.html(`
      <p>安抚情绪：${scoreValue(sc.emotion)}</p>
      <p>信息收集：${scoreValue(sc.info)}</p>
      <p>安全意识：${scoreValue(sc.safe)}</p>
      <p style="color:#FF5722;margin-top:6px;">${escapeHtml(sc.advice)}</p>`);
    $score.show();
  }

  function sceneName(value) {return {fear:'走失人员情绪激动',refuse:'走失人员拒绝配合',night:'深夜寒冷救助'}[value] || '场景训练';}
  function reportText(rep, reportScene) {
    var lines=['训练小结','场景：'+sceneName(reportScene || lastReportScene || scene),'完成轮数：'+(rep.rounds || roundCount)];
    [['emotion','安抚情绪'],['info','信息收集'],['safe','安全意识']].forEach(function(x){lines.push(x[1]+'：'+scoreValue(rep[x[0]])+'；'+((rep.dimension_advice||{})[x[0]]||''));});
    if(rep.strengths)lines.push('优势：'+rep.strengths.join('、'));
    if(rep.improvements)lines.push('练习重点：'+rep.improvements.join('、'));
    lines.push(rep.advice||'');
    (rep.scoreLog||[]).forEach(function(row,i){lines.push('第'+(i+1)+'轮：情绪 '+scoreValue(row.emotion)+' / 信息 '+scoreValue(row.info)+' / 安全 '+scoreValue(row.safe)+'；'+(row.advice||''));});
    return lines.join('\n');
  }
  function showReport(rep, reportScene, createdAt) {
    var tpl='<div class="report-card"><h2>训练小结</h2><p>'+escapeHtml(sceneName(reportScene || lastReportScene || scene))+(createdAt?' · '+escapeHtml(new Date(createdAt).toLocaleString()):'')+'</p>';
    [['emotion','安抚情绪'],['info','信息收集'],['safe','安全意识']].forEach(function(x){var score=scoreValue(rep[x[0]]);tpl+='<div class="item"><span>'+x[1]+'：'+score+'</span>'+(typeof score==='number'?'<div class="layui-progress" lay-showPercent="yes"><div class="layui-progress-bar" lay-percent="'+score+'%"></div></div>':'')+'<p>'+escapeHtml((rep.dimension_advice||{})[x[0]])+'</p></div>';});
    tpl+='<p class="advice">'+escapeHtml(rep.advice)+'</p></div>';
    layer.open({type:1,title:false,shadeClose:true,area:[Math.min(480,innerWidth-24)+'px','80%'],skin:'report-layer',content:tpl,
      btn:['下载报告','关闭'],yes:function(){GuituFiles.save(new Blob([reportText(rep,reportScene)],{type:'text/plain;charset=utf-8'}),'场景训练报告.txt');},
      success:function(){element.render('progress');}});
  }

  async function getHistory(path) {
    const response=await fetch(api+path,{credentials:'same-origin'});
    const data=await response.json().catch(()=>({}));
    if(!response.ok || data.success===false || data.error) throw new Error(data.message||data.error||'历史报告加载失败');
    return data.data;
  }
  async function loadHistory() {
    const button=$('#sceneReportHistory');button.prop('disabled',true);
    try {
      const rows=await getHistory('/reports');
      if(!Array.isArray(rows))throw new Error('历史报告格式错误');
      if(!rows.length){layer.msg('还没有已完成的训练报告');return;}
      const list=$('<div class="layui-text" style="padding:16px"></div>');
      list.append($('<p></p>').text('最近完成的50份报告，仅本人可查看。'));
      rows.forEach(function(row){
        const item=$('<p style="margin-top:10px"></p>');
        const view=$('<button type="button" class="layui-btn layui-btn-primary layui-btn-sm"></button>').text('查看报告');
        item.append($('<span></span>').text(sceneName(row.scene)+' · '+new Date(row.createdAt).toLocaleString()+' ')).append(view);
        view.on('click',async function(){
          view.prop('disabled',true);
          try {const saved=await getHistory('/reports/'+encodeURIComponent(row.id));showReport(saved.report,saved.scene,saved.createdAt);}
          catch(error){layer.msg(error.message);}finally{view.prop('disabled',false);}
        });
        list.append(item);
      });
      layer.open({type:1,title:'我的历史训练报告',shadeClose:true,area:[Math.min(600,innerWidth-24)+'px','70%'],content:list});
    } catch(error){layer.msg(error.message);}finally{button.prop('disabled',false);}
  }

  /* ========= Ajax ========= */
  async function post(body) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 90000);
    try {
      const res = await fetch(api + '/chat', {
        method: 'POST', headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({...body, session_id: sid}), signal: controller.signal
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok || data.error || !data.reply) throw new Error(data.message || data.error || '训练服务暂时不可用，请稍后重试');
      return data;
    } catch (error) {
      if (error.name === 'AbortError') throw new Error('训练服务响应超时，请重试');
      throw error;
    } finally { clearTimeout(timeout); }
  }
  function escapeHtml(value) { return $('<span>').text(value || '').html(); }
  function scoreValue(value) { return typeof value==='number' && Number.isFinite(value) && value>=0 && value<=100 ? value : '未评分'; }
});
