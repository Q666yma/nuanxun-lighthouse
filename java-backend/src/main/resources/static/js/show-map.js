(function () {
  'use strict';
  var started = false, map;

  function escape(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (c) {
      return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c];
    });
  }

  function valid(point) {
    return point && ['lng', 'lat'].every(function (key) {
      var value = point[key];
      return (typeof value === 'number' || typeof value === 'string' && value.trim() !== '') &&
        Number.isFinite(Number(value)) && Math.abs(Number(value)) <= (key === 'lng' ? 180 : 90);
    });
  }

  function density(rows) {
    var points = new Map();
    (rows || []).filter(valid).forEach(function (row) {
      var lng = Number(row.lng), lat = Number(row.lat), key = lng + ',' + lat;
      if (!points.has(key)) points.set(key, {lng: lng, lat: lat, count: 0});
      points.get(key).count++;
    });
    return Array.from(points.values());
  }

  function bindHeatmap(data, currentMap) {
    var toggle = document.getElementById('rescue-heat-toggle');
    var source = document.getElementById('rescue-heat-source');
    var note = document.getElementById('rescue-heat-status');
    if (!toggle || !source || !note) return;
    // Heat uses task or marker coordinates only; profile locations are not activity.
    var datasets = {markers: density(data.markers), tasks: density(data.tasks)};
    var labels = {markers: '搜救标记', tasks: '求助地点'};
    var visible = true, layer, loading, revision = 0;
    source.disabled = false;
    toggle.disabled = false;

    function loadLayer() {
      if (layer) return Promise.resolve(layer);
      if (loading) return loading;
      loading = new Promise(function (resolve, reject) {
        var expired = false;
        var timer = setTimeout(function () {
          expired = true;
          reject(new Error('热力图加载超时'));
        }, 10000);
        try {
          currentMap.plugin(['AMap.HeatMap'], function () {
            clearTimeout(timer);
            if (expired || currentMap !== map) return;
            try {
              layer = new AMap.HeatMap(currentMap, {radius: 25, opacity: [0, 0.8]});
              layer.hide();
              resolve(layer);
            } catch (error) { reject(error); }
          });
        } catch (error) { clearTimeout(timer); reject(error); }
      }).catch(function (error) { loading = null; throw error; });
      return loading;
    }

    async function render() {
      var currentRevision = ++revision;
      var rows = datasets[source.value] || [], label = labels[source.value];
      toggle.setAttribute('aria-pressed', String(visible));
      toggle.textContent = visible ? '关闭热力图' : '开启热力图';
      if (layer) layer.hide();
      if (!visible) {
        note.textContent = '热力图已关闭，任务、标记和队员资料地点仍可查看。';
        return;
      }
      if (!rows.length) {
        note.textContent = source.value === 'markers'
          ? '当前范围暂无带坐标的搜救标记。可在任务地图添加标记，或切换到“求助地点”。'
          : '当前范围暂无带坐标的求助地点，暂不显示热力。';
        return;
      }
      note.textContent = '正在加载' + label + '热力图…';
      try {
        var heat = await loadLayer();
        if (currentRevision !== revision || currentMap !== map) return;
        var max = rows.reduce(function (value, point) { return Math.max(value, point.count); }, 1);
        var count = rows.reduce(function (value, point) { return value + point.count; }, 0);
        heat.setDataSet({data: rows, max: max});
        heat.show();
        note.textContent = '按 ' + count + ' 条' + label + '记录显示分布密度，颜色越暖表示越集中。热度不代表走失风险或找到概率。';
      } catch (error) {
        if (currentRevision !== revision || currentMap !== map) return;
        note.textContent = '热力图暂时不可用，地图标记仍可查看；请关闭后重新开启热力图。';
      }
    }
    toggle.onclick = function () { visible = !visible; render(); };
    source.onchange = render;
    render();
  }

  async function init() {
    if (started) return;
    var holder = document.getElementById('rescue-map');
    if (!holder) return;
    started = true;
    var note = document.getElementById('rescue-map-status');
    if (!note) {
      note = document.createElement('p');
      note.id = 'rescue-map-status';
      note.className = 'rescue-map-status';
      holder.parentNode.append(note);
    }
    note.textContent = '正在读取授权范围内的任务和队员位置…';
    try {
      if (!window.AMap) throw new Error('地图服务加载失败，请刷新重试');
      map = new AMap.Map(holder, {zoom: 5, mapStyle: 'amap://styles/darkblue'});
      var response = await fetch('/mapshow/all', {credentials: 'same-origin', cache: 'no-store'});
      var data = await response.json();
      if (!response.ok || data.code) throw new Error(data.msg || data.message || '地图数据读取失败');
      var overlays = [], counts = {task: 0, marker: 0, rescuer: 0};
      function points(rows, type, color, label) {
        (rows || []).filter(valid).forEach(function (row) {
          counts[type]++;
          var title = row.elderName || row.name || label;
          var pin = new AMap.Marker({
            position: [Number(row.lng), Number(row.lat)], title: title, anchor: 'center',
            content: '<div style="background:' + color + ';border:2px solid white;border-radius:50%;width:12px;height:12px"></div>'
          });
          pin.on('click', function () {
            var target = row.taskId || type === 'task' && row.id;
            var status = type === 'rescuer' ? '队员资料地区，非实时位置' : type === 'marker' ? '任务标记点，非实时轨迹' :
              ({waiting: '等待救援', rescuing: '救援中', confirming: '待家属确认', finished: '已结束', cancelled: '已撤回'})[row.status] || row.status;
            var message = '<div style="padding:8px;color:#222"><strong>' + escape(title) + '</strong><p>' + escape(status) +
              '</p><p>经纬度：' + Number(row.lng).toFixed(5) + ', ' + Number(row.lat).toFixed(5) + '</p>' +
              (target ? '<a href="/task/task-map/' + encodeURIComponent(target) + '">进入任务地图、AI 热力与实时轨迹</a>' : '') + '</div>';
            new AMap.InfoWindow({content: message}).open(map, pin.getPosition());
          });
          overlays.push(pin);
        });
      }
      points(data.tasks, 'task', '#FF5722', '任务');
      points(data.markers, 'marker', '#1E9FFF', '搜救标记');
      points(data.rescuers, 'rescuer', '#5FB878', '队员');
      if (overlays.length) {
        map.add(overlays);
        map.setFitView(overlays, false, [30, 30, 30, 30]);
      }
      var scope = {family: '我的求助', rescuer: '本人参与任务', admin: '全站管理'}[data.scope] || '授权';
      note.textContent = scope + '范围：任务 ' + counts.task + '，标记 ' + counts.marker + '，队员资料地点 ' + counts.rescuer + '。' +
        (data.unlocatedTasks ? ' ' + data.unlocatedTasks + ' 个任务尚无可用坐标。' : '') +
        (data.unlocatedRescuers ? ' ' + data.unlocatedRescuers + ' 位队员尚无可用坐标。' : '') +
        ' 点击任务或标记可进入任务地图，查看 AI 热力与实时轨迹。';
      bindHeatmap(data, map);
    } catch (error) {
      note.textContent = error.message;
      var heatNote = document.getElementById('rescue-heat-status');
      if (heatNote) heatNote.textContent = '尚未读取地图数据，暂不显示热力。';
      var retry = document.createElement('button');
      retry.textContent = '重试';
      retry.type = 'button';
      retry.onclick = function () { if (map) map.destroy(); started = false; init(); };
      note.append(retry);
    }
  }
  window.RescueMapModule = {init: init, getChart: function () { return map; }};
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
