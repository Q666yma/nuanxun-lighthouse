(function (global) {
  'use strict';

  function coordinate(value) {
    if (!Array.isArray(value) || value.length < 2) return null;
    if (value[0] === null || value[1] === null || typeof value[0] === 'boolean' || typeof value[1] === 'boolean' || value[0] === '' || value[1] === '') return null;
    var lng = Number(value[0]), lat = Number(value[1]);
    return isFinite(lng) && isFinite(lat) && Math.abs(lng) <= 180 && Math.abs(lat) <= 90 ? [lng, lat] : null;
  }

  function project(map, value) {
    var position = coordinate(value);
    if (!position) return null;
    try {
      var pixel = map.lngLatToContainer(position);
      return pixel && isFinite(pixel.x) && isFinite(pixel.y) ? {x: Number(pixel.x), y: Number(pixel.y)} : null;
    } catch (_) { return null; }
  }

  function paletteColors(palette) {
    var alternate = /orange|green|橙|绿/i.test(String(palette || ''));
    var colors = {positive: alternate ? [244, 139, 28] : [230, 62, 48], negative: alternate ? [28, 155, 111] : [36, 112, 224]};
    if (palette && typeof palette === 'object') {
      ['positive', 'negative'].forEach(function (key) {
        var value = palette[key];
        if (Array.isArray(value) && value.length === 3 && value.every(function (n) { return typeof n === 'number' && isFinite(n); })) {
          colors[key] = value.map(function (n) { return Math.max(0, Math.min(255, Math.round(n))); });
        }
      });
    }
    return colors;
  }

  function render(canvas, map, points, palette) {
    var stats = {positive: 0, negative: 0, neutral: 0, coloredPixels: 0, maxAlpha: 0};
    function publishStats() {
      if (canvas && canvas.dataset) {
        Object.keys(stats).forEach(function (key) {
          canvas.dataset['heatRender' + key.charAt(0).toUpperCase() + key.slice(1)] = String(stats[key]);
        });
      }
      return stats;
    }
    if (!canvas || !canvas.parentElement || !map || typeof map.lngLatToContainer !== 'function') return publishStats();
    var width = Math.max(0, Math.floor(canvas.parentElement.clientWidth));
    var height = Math.max(0, Math.floor(canvas.parentElement.clientHeight));
    canvas.width = width; canvas.height = height;
    canvas.style.width = width + 'px'; canvas.style.height = height + 'px';
    var output = canvas.getContext('2d');
    if (!output || !width || !height) return publishStats();
    var document = canvas.ownerDocument || global.document;
    function mask() {
      var layer = document.createElement('canvas');
      layer.width = width; layer.height = height;
      return layer.getContext('2d', {willReadFrequently: true});
    }
    var positive = mask(), negative = mask();
    if (!positive || !negative) return publishStats();

    function pointRadius(position, center, meters) {
      meters = isFinite(Number(meters)) ? Math.max(40, Math.min(180, Number(meters))) : 120;
      var east = project(map, [position[0] + meters / (111320 * Math.max(0.01, Math.cos(position[1] * Math.PI / 180))), position[1]]);
      var north = project(map, [position[0], position[1] + meters / 111320]);
      var radius = 0;
      if (east) radius = Math.hypot(east.x - center.x, east.y - center.y);
      if (north) radius = Math.max(radius, Math.hypot(north.x - center.x, north.y - center.y));
      return Math.max(3, Math.min(160, radius || 24));
    }

    function drawPoint(context, position, weight, meters) {
      position = coordinate(position);
      var center = project(map, position);
      if (!center) return false;
      var radius = pointRadius(position, center, meters);
      var gradient = context.createRadialGradient(center.x, center.y, 0, center.x, center.y, radius);
      gradient.addColorStop(0, 'rgba(255,255,255,' + weight + ')');
      gradient.addColorStop(0.3, 'rgba(255,255,255,' + weight * 0.8 + ')');
      gradient.addColorStop(0.7, 'rgba(255,255,255,' + weight * 0.25 + ')');
      gradient.addColorStop(1, 'rgba(255,255,255,0)');
      context.fillStyle = gradient;
      context.beginPath(); context.arc(center.x, center.y, radius, 0, Math.PI * 2); context.fill();
      return true;
    }

    function projectedLine(coordinates, minimum) {
      if (!Array.isArray(coordinates) || coordinates.length < minimum) return null;
      var pixels = coordinates.map(function (position) { return project(map, position); });
      return pixels.every(function (pixel) { return pixel !== null; }) ? pixels : null;
    }

    function path(context, pixels, closed) {
      context.moveTo(pixels[0].x, pixels[0].y);
      for (var i = 1; i < pixels.length; i++) context.lineTo(pixels[i].x, pixels[i].y);
      if (closed) context.closePath();
    }

    function drawLine(context, coordinates, weight) {
      var pixels = projectedLine(coordinates, 2);
      if (!pixels) return false;
      context.lineCap = 'round'; context.lineJoin = 'round';
      // The line remains a narrow band along the submitted route, not a filled search area.
      [[12, 0.1], [8, 0.2], [4, 0.55]].forEach(function (band) {
        context.beginPath(); path(context, pixels, false);
        context.lineWidth = band[0]; context.strokeStyle = 'rgba(255,255,255,' + weight * band[1] + ')'; context.stroke();
      });
      return true;
    }

    function drawPolygon(context, coordinates, weight) {
      if (!Array.isArray(coordinates) || !coordinates.length) return false;
      var rings = coordinates.map(function (ring) { return projectedLine(ring, 3); });
      // Reject a broken ring rather than accidentally filling a missing hole or joining invalid vertices.
      if (!rings.every(function (ring) { return ring !== null; })) return false;
      context.beginPath();
      rings.forEach(function (ring) { path(context, ring, true); });
      context.fillStyle = 'rgba(255,255,255,' + weight * 0.8 + ')';
      context.fill('evenodd');
      return true;
    }

    function drawGeometry(context, geometry, weight, meters, depth) {
      if (!geometry || depth > 8) return false;
      var coordinates = geometry.coordinates, drawn = false;
      function drawEach(items, draw) {
        if (!Array.isArray(items)) return false;
        items.forEach(function (item) { if (draw(item)) drawn = true; });
        return drawn;
      }
      switch (geometry.type) {
        case 'Feature': return drawGeometry(context, geometry.geometry, weight, meters, depth + 1);
        case 'GeometryCollection': return drawEach(geometry.geometries, function (item) { return drawGeometry(context, item, weight, meters, depth + 1); });
        case 'Point': return drawPoint(context, coordinates, weight, meters);
        case 'MultiPoint': return drawEach(coordinates, function (item) { return drawPoint(context, item, weight, meters); });
        case 'LineString': return drawLine(context, coordinates, weight);
        case 'MultiLineString': return drawEach(coordinates, function (item) { return drawLine(context, item, weight); });
        case 'Polygon': return drawPolygon(context, coordinates, weight);
        case 'MultiPolygon': return drawEach(coordinates, function (item) { return drawPolygon(context, item, weight); });
        default: return false;
      }
    }

    (Array.isArray(points) ? points : []).forEach(function (point) {
      if (!point || point.value === null || point.value === '' || typeof point.value === 'boolean') return;
      var value = Number(point.value);
      if (!isFinite(value)) return;
      if (value === 0) { stats.neutral++; return; }
      var context = value > 0 ? positive : negative, weight = Math.min(1, Math.abs(value));
      var geometry = point.geometry || {type: 'Point', coordinates: [point.lng, point.lat]};
      if (typeof geometry === 'string') {
        try { geometry = JSON.parse(geometry); } catch (_) { return; }
      }
      if (drawGeometry(context, geometry, weight, point.radiusMeters, 0)) stats[value > 0 ? 'positive' : 'negative']++;
    });

    var colors = paletteColors(palette);
    var positivePixels = positive.getImageData(0, 0, width, height).data;
    var negativePixels = negative.getImageData(0, 0, width, height).data;
    var result = output.createImageData(width, height), pixels = result.data;
    for (var i = 0; i < pixels.length; i += 4) {
      var p = positivePixels[i + 3] / 255, n = negativePixels[i + 3] / 255;
      if (!p && !n) continue;
      // Focus clues stay visible over searched regions. Repeated negative marks never become opaque.
      var positiveAlpha = 0.42 * p;
      var negativeAlpha = 0.34 * n * Math.max(0, 1 - p * 8);
      var total = positiveAlpha + negativeAlpha;
      if (!total) continue;
      for (var channel = 0; channel < 3; channel++) {
        pixels[i + channel] = Math.round((colors.positive[channel] * positiveAlpha + colors.negative[channel] * negativeAlpha) / total);
      }
      pixels[i + 3] = Math.round(Math.min(0.42, total) * 255);
      if (pixels[i + 3]) {
        stats.coloredPixels++;
        stats.maxAlpha = Math.max(stats.maxAlpha, pixels[i + 3]);
      }
    }
    output.putImageData(result, 0, 0);
    return publishStats();
  }

  global.GuituHeatRenderer = {render: render};
})(window);
