(function (global) {
  'use strict';
  function init(config) {
    var host = document.getElementById('taskLocationControls');
    if (!host || host.dataset.initialized || !config || !config.map || !config.taskId) return;
    host.dataset.initialized = 'true';
    var map = config.map, base = '/api/tasks/' + encodeURIComponent(config.taskId) + '/locations';
    var watch = null, polling = null, playback = null, generation = 0, sending = false, sharing = false;
    var lastSent = 0, markerLayers = [], trackLine = null, playMarker = null, track = [], nextBefore = 0, loading = false;
    host.innerHTML = '<div class="layui-form-item" style="display:flex;gap:8px;flex-wrap:wrap;align-items:center">' +
      '<button type="button" class="layui-btn layui-btn-sm" data-location="share">共享我的位置</button>' +
      '<button type="button" class="layui-btn layui-btn-primary layui-btn-sm" data-location="refresh">刷新队员位置</button>' +
      '<select data-location="member" class="layui-input" style="width:180px" aria-label="选择轨迹队员"><option value="">暂无位置记录</option></select>' +
      '<button type="button" class="layui-btn layui-btn-primary layui-btn-sm" data-location="track">查看轨迹</button>' +
      '<button type="button" class="layui-btn layui-btn-primary layui-btn-sm" data-location="older" hidden>更早轨迹</button>' +
      '<button type="button" class="layui-btn layui-btn-primary layui-btn-sm" data-location="play" disabled>轨迹回放</button>' +
      '<button type="button" class="layui-btn layui-btn-primary layui-btn-sm" data-location="clear">清除轨迹显示</button></div>' +
      '<p data-location="status" role="status">默认不共享位置。只有主动开启后，当前任务相关人员才能查看；停止后保留已提交的任务轨迹。</p>';
    function element(key) { return host.querySelector('[data-location="' + key + '"]'); }
    var shareButton = element('share'), status = element('status'), select = element('member');
    var canShare = config.userType === 'rescuer' && ['rescuing', 'confirming'].indexOf(config.taskStatus) >= 0;
    shareButton.hidden = !canShare;
    async function request(path, body, keepalive) {
      var response = await fetch(base + path, {method: body === undefined ? 'GET' : 'POST', credentials: 'same-origin',
        cache: 'no-store', keepalive: !!keepalive, headers: body === undefined ? {} : {'Content-Type': 'application/json'},
        body: body === undefined ? undefined : JSON.stringify(body)});
      var result; try { result = await response.json(); } catch (_) { throw new Error('服务器返回异常，请稍后重试'); }
      if (!response.ok || result.success === false) throw new Error(result.message || result.msg || '位置操作未完成');
      return result.data;
    }
    function time(value) { var date = new Date(value); return isNaN(date.getTime()) ? '时间未知' : date.toLocaleString(); }
    function visible() {
      return !document.hidden && (!global.frameElement || global.frameElement.getClientRects().length > 0);
    }
    function remove(layers) { if (layers && layers.length) map.remove(layers); }
    function haltPlayback() { if (playback !== null) clearInterval(playback); playback = null; element('play').textContent = '轨迹回放'; }
    function clearTrack() {
      haltPlayback(); remove([trackLine, playMarker].filter(Boolean)); trackLine = playMarker = null;
      track = []; nextBefore = 0; element('play').disabled = true; element('older').hidden = true;
    }
    async function convert(points) {
      var result = [];
      for (var start = 0; start < points.length; start += 40) {
        var batch = points.slice(start, start + 40).map(function (p) { return [Number(p.longitude), Number(p.latitude)]; });
        var converted = await new Promise(function (resolve, reject) {
          var timeout = setTimeout(function () { reject(new Error('地图坐标转换超时，请重试')); }, 12000);
          global.AMap.convertFrom(batch, 'gps', function (state, response) {
            clearTimeout(timeout);
            if (state === 'complete' && response && response.info === 'ok' && response.locations.length === batch.length) resolve(response.locations);
            else reject(new Error('地图坐标转换失败，未用原始 GPS 坐标代替显示'));
          });
        });
        result = result.concat(converted);
      }
      return result;
    }
    async function refresh(manual) {
      if (loading) return;
      loading = true;
      try {
        var members = await request(''), selected = select.value;
        var coordinates = await convert(members);
        remove(markerLayers); markerLayers = [];
        select.textContent = '';
        members.forEach(function (member, index) {
          var active = Number(member.live) === 1 || member.live === true;
          var option = document.createElement('option'); option.value = member.userId;
          option.textContent = member.name + (active ? ' · 正在共享' : ' · 历史位置'); select.appendChild(option);
          var marker = new global.AMap.Marker({position: coordinates[index], title: member.name + ' · ' + time(member.recordedAt), opacity: active ? 1 : 0.55});
          markerLayers.push(marker);
          marker.on('click', function () {
            select.value = member.userId;
            status.textContent = member.name + '：' + (active ? '最近上报' : '历史位置，当前未确认共享') + ' ' + time(member.recordedAt) + '；设备报告精度约 ' + Math.round(member.accuracy) + ' 米。';
          });
        });
        if (!members.length) { var empty = document.createElement('option'); empty.value = ''; empty.textContent = '暂无位置记录'; select.appendChild(empty); }
        else if (members.some(function (m) { return m.userId === selected; })) select.value = selected;
        if (markerLayers.length) map.add(markerLayers);
        if (manual) status.textContent = members.length ? '已显示 ' + members.length + ' 位队员的最近上报位置；半透明标记为历史记录。' : '尚无队员主动共享位置。';
      } catch (error) { if (manual) status.textContent = error.message; }
      finally { loading = false; }
    }
    async function stop(message, closing) {
      generation++; sharing = false; shareButton.disabled = true; shareButton.textContent = '共享我的位置';
      if (watch !== null) navigator.geolocation.clearWatch(watch); watch = null;
      try { await request('/sharing', {sharing: false}, closing); status.textContent = message || '已停止共享位置，已有轨迹保留在此任务中。'; }
      catch (error) { status.textContent = '本机已停止定位；服务器状态未确认，将在定位过期后显示为历史位置。'; }
      finally { shareButton.disabled = false; }
    }
    async function start() {
      if (!navigator.geolocation || !global.isSecureContext) { status.textContent = '此浏览器无法定位，请使用 HTTPS 并允许网站位置权限。'; return; }
      shareButton.disabled = true;
      try {
        await request('/sharing', {sharing: true}); sharing = true;
        var version = ++generation; shareButton.textContent = '停止共享位置';
        status.textContent = '等待设备定位。你可随时停止，离开此地图或切到后台时会停止共享。';
        watch = navigator.geolocation.watchPosition(async function (position) {
          if (!sharing || generation !== version || sending || Date.now() - lastSent < 10000) return;
          if (!visible()) { await stop('地图已离开前台，位置共享已停止。'); return; }
          sending = true;
          try {
            await request('', {latitude: position.coords.latitude, longitude: position.coords.longitude, accuracy: position.coords.accuracy,
              recordedAt: new Date(position.timestamp).toISOString(), coordinateSystem: 'wgs84'});
            if (generation === version) { lastSent = Date.now(); status.textContent = '正在共享，最近上报 ' + time(position.timestamp) + '；精度约 ' + Math.round(position.coords.accuracy) + ' 米。'; await refresh(false); }
          } catch (error) { if (generation === version) await stop('位置上报失败：' + error.message + '。已停止共享，可重试。'); }
          finally { sending = false; }
        }, function (error) {
          if (generation === version) stop(error.code === 1 ? '位置权限未获允许，已停止共享。' : '设备暂时无法定位，已停止共享，可稍后重试。');
        }, {enableHighAccuracy: true, maximumAge: 5000, timeout: 20000});
      } catch (error) { sharing = false; status.textContent = error.message; }
      finally { shareButton.disabled = false; }
    }
    async function loadTrack(older) {
      var uid = select.value; if (!uid) { status.textContent = '暂无可查看的队员轨迹。'; return; }
      element('track').disabled = element('older').disabled = true;
      try {
        var data = await request('/track?userId=' + encodeURIComponent(uid) + '&limit=1000' + (older && nextBefore ? '&beforeId=' + nextBefore : ''));
        if (select.value !== uid) return;
        var coordinates = await convert(data.points);
        if (select.value !== uid) return;
        haltPlayback(); remove([trackLine, playMarker].filter(Boolean)); playMarker = null;
        var incoming = data.points.map(function (point, index) { return {point: point, position: coordinates[index]}; });
        track = older ? incoming.concat(track) : incoming;
        nextBefore = data.nextBeforeId; element('older').hidden = !data.hasMore;
        if (track.length) {
          trackLine = new global.AMap.Polyline({path: track.map(function (p) { return p.position; }), strokeColor: '#1E9FFF', strokeWeight: 4, showDir: true});
          map.add(trackLine); map.setFitView([trackLine]);
        } else trackLine = null;
        element('play').disabled = !track.length;
        status.textContent = track.length ? '已显示 ' + track.length + ' 个设备上报点，时间 ' + time(track[0].point.recordedAt) + ' 至 ' + time(track[track.length - 1].point.recordedAt) + (data.hasMore ? '；还有更早记录。' : '。') : '此队员尚无轨迹记录。';
      } catch (error) { status.textContent = error.message; }
      finally { element('track').disabled = element('older').disabled = false; }
    }
    shareButton.addEventListener('click', function () { if (sharing) stop(); else start(); });
    element('refresh').addEventListener('click', function () { refresh(true); });
    element('track').addEventListener('click', function () { loadTrack(false); });
    element('older').addEventListener('click', function () { loadTrack(true); });
    element('clear').addEventListener('click', clearTrack);
    select.addEventListener('change', clearTrack);
    element('play').addEventListener('click', function () {
      if (playback !== null) { haltPlayback(); return; }
      if (!track.length) return;
      remove([playMarker].filter(Boolean)); var index = 0;
      playMarker = new global.AMap.Marker({position: track[0].position, title: '轨迹回放'}); map.add(playMarker);
      element('play').textContent = '暂停回放';
      playback = setInterval(function () {
        var item = track[index++]; if (!item) { haltPlayback(); return; }
        playMarker.setPosition(item.position); status.textContent = '回放 ' + index + '/' + track.length + ' · ' + time(item.point.recordedAt);
        if (index >= track.length) haltPlayback();
      }, 400);
    });
    function hidden() { if (sharing && !visible()) stop('地图已离开前台，位置共享已停止。'); }
    document.addEventListener('visibilitychange', hidden);
    global.addEventListener('pagehide', function () {
      if (polling !== null) clearInterval(polling); haltPlayback();
      if (sharing) stop('页面已离开，位置共享已停止。', true);
    });
    polling = setInterval(function () { hidden(); if (visible()) refresh(false); }, 15000);
    refresh(false);
  }
  global.GuituTaskLocation = {init: init};
})(window);
