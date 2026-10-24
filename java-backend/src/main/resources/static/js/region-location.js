(function (global) {
  'use strict';

  // Same browser-only map credentials already used by the search maps.
  var mapKey = '8c25fcd7450b05cd9d63fd310c2f3652';
  var securityCode = 'f8ad23bb5e2995594eb1c0d1a8c9e85f';
  var sdkReady = null;

  function timed(work, message, duration) {
    return new Promise(function (resolve, reject) {
      var timer = setTimeout(function () { reject(new Error(message)); }, duration || 12000);
      work(function (result) { clearTimeout(timer); resolve(result); }, function (error) { clearTimeout(timer); reject(error); });
    });
  }

  function loadMap() {
    if (global.AMap && global.AMap.Geocoder) return Promise.resolve(global.AMap);
    if (sdkReady) return sdkReady;
    sdkReady = timed(function (resolve, reject) {
      if (global.AMap) {
        global.AMap.plugin('AMap.Geocoder', function () { resolve(global.AMap); });
        return;
      }
      global._AMapSecurityConfig = {securityJsCode: securityCode};
      var script = document.createElement('script');
      script.src = 'https://webapi.amap.com/maps?v=2.0&key=' + encodeURIComponent(mapKey) + '&plugin=AMap.Geocoder';
      script.async = true;
      script.onload = function () {
        if (global.AMap && global.AMap.Geocoder) resolve(global.AMap);
        else reject(new Error('地址服务暂时不可用，请重试或手动填写地区。'));
      };
      script.onerror = function () { script.remove(); reject(new Error('地址服务加载失败，请检查网络或手动填写地区。')); };
      document.head.appendChild(script);
    }, '地址服务加载超时，请重试或手动填写地区。').catch(function (error) { sdkReady = null; throw error; });
    return sdkReady;
  }

  function locate() {
    if (!global.isSecureContext || !navigator.geolocation) return Promise.reject(new Error('当前浏览器无法定位，请使用 HTTPS 和支持定位的浏览器，或手动填写地区。'));
    return timed(function (resolve, reject) {
      navigator.geolocation.getCurrentPosition(resolve, function (error) {
        var messages = {
          1: '未允许位置权限。可在浏览器网站设置中允许后重试，或手动填写地区。',
          2: '暂时无法获取当前位置，请检查设备定位和网络，或手动填写地区。',
          3: '定位超时，请重试或手动填写地区。'
        };
        reject(new Error(messages[error.code] || '定位失败，请重试或手动填写地区。'));
      }, {enableHighAccuracy: true, timeout: 12000, maximumAge: 60000});
    }, '定位超时，请重试或手动填写地区。', 15000);
  }

  function regionFor(map, position) {
    var coords = position.coords;
    if (!Number.isFinite(coords.longitude) || !Number.isFinite(coords.latitude)) return Promise.reject(new Error('未获取有效位置，请重试或手动填写地区。'));
    return timed(function (resolve, reject) {
      map.convertFrom([coords.longitude, coords.latitude], 'gps', function (state, result) {
        if (state === 'complete' && result && result.locations && result.locations.length) resolve(result.locations[0]);
        else reject(new Error('位置转换失败，请重试或手动填写地区。'));
      });
    }, '位置转换超时，请重试或手动填写地区。').then(function (point) {
      return timed(function (resolve, reject) {
        new map.Geocoder({extensions: 'base'}).getAddress(point, function (state, result) {
          var component = result && result.regeocode && result.regeocode.addressComponent;
          if (state !== 'complete' || !component) { reject(new Error('暂时无法解析所在地区，请重试或手动填写。')); return; }
          var parts = [component.province, component.city, component.district].filter(function (value) { return typeof value === 'string' && value.trim(); });
          var address = parts.filter(function (value, index) { return parts.indexOf(value) === index; }).join('');
          if (!address) { reject(new Error('此位置暂无省市区信息，请手动填写地区。')); return; }
          resolve(address);
        });
      }, '地区解析超时，请重试或手动填写。');
    });
  }

  function init(field) {
    var input = field.querySelector('input[name="location"]');
    var button = field.querySelector('[data-region-locate]');
    var status = field.querySelector('[data-region-status]');
    if (!input || !button || !status) return;
    var generation = 0, busy = false, label = button.textContent;
    status.setAttribute('aria-live', 'polite');
    function finish() { busy = false; button.disabled = false; button.textContent = label; button.removeAttribute('aria-busy'); }
    function cancel(message) { generation++; finish(); status.textContent = message; }
    input.addEventListener('input', function () { if (busy) cancel('已保留手动输入的地区。'); });
    if (input.form) input.form.addEventListener('reset', function () { cancel('可手动填写，或点击自动定位。'); });
    global.addEventListener('pagehide', function () { if (busy) cancel('定位已停止，可点击重新定位。'); });
    button.addEventListener('click', async function () {
      if (busy || input.disabled || input.readOnly) return;
      busy = true;
      var version = ++generation, original = input.value;
      button.disabled = true; button.textContent = '定位中…'; button.setAttribute('aria-busy', 'true');
      status.textContent = '请允许浏览器位置权限，正在获取当前位置…';
      try {
        var position = await locate();
        if (version !== generation) return;
        status.textContent = '正在由高德解析所在省市区…';
        var map = await loadMap();
        if (version !== generation) return;
        var address = await regionFor(map, position);
        if (version !== generation) return;
        // A value changed by another form script must also win over a late GPS result.
        if (input.value !== original) { status.textContent = '地区已修改，已保留当前输入。'; return; }
        if (input.maxLength > 0 && address.length > input.maxLength) { status.textContent = '定位地区名称过长，请手动填写。'; return; }
        finish();
        input.value = address;
        input.dispatchEvent(new Event('input', {bubbles: true}));
        input.dispatchEvent(new Event('change', {bubbles: true}));
        status.textContent = '已填入当前所在地区，请核对；可手动修改。';
      } catch (error) {
        if (version === generation) status.textContent = error.message || '定位失败，请手动填写地区。';
      } finally {
        if (version === generation) finish();
      }
    });
  }

  document.querySelectorAll('[data-region-field]').forEach(init);
})(window);
