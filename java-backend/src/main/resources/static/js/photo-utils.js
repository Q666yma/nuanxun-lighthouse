(function (global) {
  'use strict';
  // Keep quality checking and face enrollment on the same image bytes.
  var MAX_BYTES = Math.floor(1.4 * 1024 * 1024);
  function readImage(file) {
    return new Promise(function (resolve, reject) {
      var url = URL.createObjectURL(file), image = new Image();
      image.onload = function () { URL.revokeObjectURL(url); resolve(image); };
      image.onerror = function () { URL.revokeObjectURL(url); reject(new Error('无法读取图片，请使用 JPG 或 PNG 照片')); };
      image.src = url;
    });
  }
  function toBlob(canvas, quality) {
    return new Promise(function (resolve, reject) {
      canvas.toBlob(function (blob) { if (blob) resolve(blob); else reject(new Error('图片处理失败，请重新选择照片')); }, 'image/jpeg', quality);
    });
  }
  async function prepare(file) {
    if (!file || file.size > 10 * 1024 * 1024) throw new Error('请选择 10 MB 以内的照片');
    if (!/\.(jpe?g|png|gif|bmp)$/i.test(file.name)) throw new Error('请使用 JPG、PNG、GIF 或 BMP 图片');
    if (file.size <= MAX_BYTES) return file;
    var image = await readImage(file), canvas = document.createElement('canvas');
    var ratio = Math.min(1, 1600 / Math.max(image.naturalWidth, image.naturalHeight));
    canvas.width = Math.max(1, Math.round(image.naturalWidth * ratio));
    canvas.height = Math.max(1, Math.round(image.naturalHeight * ratio));
    var context = canvas.getContext('2d');
    context.fillStyle = '#fff'; context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(image, 0, 0, canvas.width, canvas.height);
    for (var quality = 0.9; quality >= 0.45; quality -= 0.1) {
      var blob = await toBlob(canvas, quality);
      if (blob.size <= MAX_BYTES) return new File([blob], file.name.replace(/\.[^.]+$/, '') + '.jpg', {type:'image/jpeg'});
    }
    throw new Error('照片压缩后仍然过大，请裁剪到照片中人物的面部和上半身后重试');
  }
  global.GuituPhotos = {prepare:prepare};
})(window);
