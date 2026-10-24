function handleApiAction(res) {
  if(res.code === 0){
    switch(res.action){
      case "reload":
        location.reload();
        break;
      case "closeAndOpen":
        window.parent.layui.index.openTab({ title: res.title || '详情', href: res.url });
        window.parent.layui.index.closeCurrentTab();
        break;
      case "globalJump":
        window.top.location.href = res.url;
        break;
      case "none":
      default:
        layer.msg(res.msg || '操作完成');
    }
  } else if(res.code === 401){
    window.top.location.href = "/";
  } else {
    layer.msg(res.msg || '操作失败', {icon: 2});
  }
}