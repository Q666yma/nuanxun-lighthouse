/**
 * 生成装备清单 PDF
 * v8  — 修复多选只导出 1 条的问题
 */
layui.use(['layer', 'form', 'jquery'], function () {
  const { layer, form, $ } = layui;

  /* 打开弹窗 */
  $('#equipGenBtn').on('click', () => {
    layer.open({
      type   : 1,
      title  : '选择随身装备',
      area   : ['460px', 'auto'],
      content: $('#equipModal'),
      success: () => form.render()
    });
  });

  /* 生成 PDF */
  form.on('submit(equipConfirm)', async () => {
    /* ① 直接收集所有已勾选值 */
    const list = $('#equipModal input[name="items"]:checked')
                  .map(function () { return this.value; })
                  .get();

    if (!list.length) { layer.msg('请至少勾选一项', { icon: 0 }); return false; }

    /* ② 构造临时 DOM */
    const box = document.createElement('div');
    box.style.cssText = 'width:400px;padding:20px;background:#fff;font-size:14px;';
    box.innerHTML = `
      <h2 style="text-align:center;margin:0 0 12px;font-size:18px;">志愿者个人装备自查清单</h2>
      <p style="font-size:12px;margin:0 0 10px;">生成时间：${new Date().toLocaleString()}</p>
      <ul style="padding-left:20px;line-height:1.8;margin:0;">
        ${list.map((t,i)=>`<li>${i+1}. ${t}</li>`).join('')}
      </ul>`;
    document.body.appendChild(box);

    try {
      /* ③ html2canvas → jsPDF */
      const canvas  = await html2canvas(box, { scale: 2, backgroundColor: '#FFFFFF' });
      const imgData = canvas.toDataURL('image/jpeg', 1.0);
      document.body.removeChild(box);

      const JsPDF = window.jsPDF;
      if (!JsPDF) { layer.alert('jsPDF 未加载'); return false; }

      const doc   = new JsPDF({ format: 'a5', unit: 'mm' });
      const pageW = doc.internal.pageSize.getWidth();
      const imgW  = pageW - 20;
      const imgH  = canvas.height * imgW / canvas.width;

      doc.addImage(imgData, 'JPEG', 10, 10, imgW, imgH);
      doc.save('装备清单.pdf');
    } catch (err) {
      console.error(err);
      layer.alert('导出失败：' + err.message, { icon: 2 });
    }
    return false;           // 阻止表单默认提交
  });
});
