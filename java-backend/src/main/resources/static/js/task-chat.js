// /static/js/chat.js
$(function () {
    $('.task-chat').each(function () {
        initTaskChatByDom(this);
    });
});

function initTaskChatByDom(dom) {
    var $chat = $(dom);
    // 获取属性后立即转换为数字
    var taskId = parseInt($chat.data('task-id'));
    var userId = $chat.data('user-id');
    var userType = $chat.data('user-type');
    var userName = $chat.data('user-name');
    var taskStatus = $chat.data('task-status');
    var mediaType = null, mediaData = null;
    var lastMessageId = 0;
    var isFirstLoad = true;
    var isLoading = false;
    var autoRefreshTimer = null;
    var displayedIds = new Set();

    var $container = $chat.find('.chat-container');
    var $msgInput = $chat.find('.message-input');
    var $mediaPreview = $chat.find('.media-preview');
    var $previewImage = $chat.find('.preview-image');
    var $audioPreview = $chat.find('.audio-preview');
    var $audioFileName = $chat.find('.audio-file-name');
    var $previewAudio = $chat.find('.preview-audio');
    var $clearMedia = $chat.find('.clear-media-btn');
    var $sendMsg = $chat.find('.send-message-btn');

    // 上传功能（file input + ajax，兼容片段）
    $chat.find('.upload-image-btn').on('click', function () {
        $chat.find('.hidden-image-input').click();
    });
    $chat.find('.upload-audio-btn').on('click', function () {
        $chat.find('.hidden-audio-input').click();
    });
    $chat.find('.hidden-image-input').on('change', function () {
        if (this.files && this.files.length > 0) {
            var file = this.files[0];
            var formData = new FormData();
            formData.append('file', file);
            layui.layer.load(2);
            $.ajax({
                url: '/upload/image',
                type: 'POST',
                data: formData,
                processData: false,
                contentType: false,
                success: function (res) {
                    layui.layer.closeAll('loading');
                    if ((res.success === true || res.code === 0) && res.url) {
                        mediaType = 'image';
                        mediaData = res.url;
                        $previewImage.attr('src', res.url).show();
                        $audioPreview.hide();
                        $mediaPreview.show();
                        layui.layer.msg('图片上传成功', { icon: 1 });
                    } else {
                        layui.layer.msg(res.msg || '上传失败', { icon: 2 });
                    }
                },
                error: function () {
                    layui.layer.closeAll('loading');
                    layui.layer.msg('上传出错，请稍后重试', { icon: 2 });
                }
            });
        }
    });
    $chat.find('.hidden-audio-input').on('change', function () {
        if (this.files && this.files.length > 0) {
            var file = this.files[0];
            var formData = new FormData();
            formData.append('file', file);
            layui.layer.load(2);
            $.ajax({
                url: '/upload/audio',
                type: 'POST',
                data: formData,
                processData: false,
                contentType: false,
                success: function (res) {
                    layui.layer.closeAll('loading');
                    if ((res.success === true || res.code === 0) && res.url) {
                        mediaType = 'audio';
                        mediaData = res.url;
                        $previewAudio.attr('src', res.url);
                        $audioFileName.text(file.name || '语音文件');
                        $audioPreview.show();
                        $previewImage.hide();
                        $mediaPreview.show();
                        layui.layer.msg('音频上传成功', { icon: 1 });
                    } else {
                        layui.layer.msg(res.msg || '上传失败', { icon: 2 });
                    }
                },
                error: function () {
                    layui.layer.closeAll('loading');
                    layui.layer.msg('上传出错，请稍后重试', { icon: 2 });
                }
            });
        }
    });

    $clearMedia.on('click', function () { clearMedia(); });

    function clearMedia() {
        mediaType = null;
        mediaData = null;
        $mediaPreview.hide();
        $previewImage.attr('src', '').hide();
        $audioPreview.hide();
        $previewAudio.attr('src', '');
        $chat.find('.hidden-image-input, .hidden-audio-input').val('');
    }

    // 加载历史消息
    function loadMessages() {
        if (isLoading) return;
        isLoading = true;
        $container.html('<div class="layui-text" style="text-align: center; color: #999; padding: 20px 0;"><i class="layui-icon layui-icon-loading layui-anim layui-anim-rotate layui-anim-loop"></i> 加载消息历史...</div>');
        $.ajax({
            url: '/task/messages/' + taskId,
            type: 'GET',
            data: { before: lastMessageId > 0 ? lastMessageId : null, limit: 50 },
            success: function (res) {
                isLoading = false;
                if (res.code === 0) {
                    var messages = res.data.messages;
                    $container.empty();
                    displayedIds.clear();
                    if (messages.length === 0) {
                        $container.html('<div class="message-system"><div class="message-bubble">暂无消息记录</div></div>');
                        return;
                    }
                    $container.append('<div class="message-system"><div class="message-bubble">任务通讯已开始</div></div>');
                    messages.sort(function(a, b) { return Number(a.id) - Number(b.id); });
                    messages.forEach(function (msg) {
                        appendMessage(msg, true);
                    });
                    if (messages.length > 0) lastMessageId = Number(messages[messages.length - 1].id);
                    if (isFirstLoad) {
                        scrollToBottom();
                        isFirstLoad = false;
                    }
                } else {
                    $container.html('<div class="message-system"><div class="message-bubble">加载消息失败: ' + (res.msg || '未知错误') + '</div></div>');
                }
            },
            error: function () {
                isLoading = false;
                $container.html('<div class="message-system"><div class="message-bubble">加载消息失败，请刷新页面重试</div></div>');
            }
        });
    }
    function refreshNewMessages() {
        if (isLoading) return;
        isLoading = true;
        $.ajax({
            url: '/task/messages/' + taskId,
            type: 'GET',
            data: { after: lastMessageId, limit: 20 },
            success: function (res) {
                if (res.code === 0) {
                    var messages = res.data.messages;
                    if (messages.length > 0) {
                        var wasAtBottom = isUserAtBottom();
                        messages.sort(function(a, b) { return Number(a.id) - Number(b.id); });
                        messages.forEach(function (msg) {
                            appendMessage(msg, true);
                        });
                        lastMessageId = Math.max(lastMessageId, Number(messages[messages.length - 1].id));
                        if (wasAtBottom) {
                            scrollToBottom();
                        } else {
                            showNewMessageAlert();
                        }
                    }
                }
            },
            complete: function () { isLoading = false; }
        });
    }
    function appendMessage(message, isNew) {
        if (displayedIds.has(String(message.id))) return;
        var msgData;
        try { msgData = JSON.parse(message.messageData); } catch (e) { return; }
        if (!msgData || !msgData.sender) return;
        displayedIds.add(String(message.id));
        var isSelf = String(msgData.sender.id) === String(userId);
        var msgClass = isSelf ? 'message-sent' : 'message-received';
        var msgTime = formatTime(new Date(msgData.timestamp));
        var badgeClass = msgData.sender.type === 'rescuer' ? 'badge-rescuer' : 'badge-family';
        var badgeText = msgData.sender.type === 'rescuer' ? '救援者' : '家属';
        var html = '<div class="message-item ' + msgClass + '" data-id="' + message.id + '">';
        if (!isSelf) {
            html += '<div class="message-sender"><span class="' + badgeClass + '">' + badgeText + '</span>' + escapeHtml(msgData.sender.name) + '</div>';
        }
        html += '<div class="message-bubble">';
        if (msgData.type === 'text') {
            html += '<div class="message-text">' + escapeHtml(msgData.content) + '</div>';
        } else if (msgData.type === 'image' && msgData.media && msgData.media.url) {
            html += '<div class="message-image-container"><img src="' + escapeHtml(safeMediaUrl(msgData.media.url)) + '" class="message-image" onclick="previewImage(this.src)"></div>';
            if (msgData.content) html += '<div class="message-text" style="margin-top: 5px;">' + escapeHtml(msgData.content) + '</div>';
        } else if (msgData.type === 'audio' && msgData.media && msgData.media.url) {
            html += '<div class="message-audio-container"><audio controls class="message-audio" src="' + escapeHtml(safeMediaUrl(msgData.media.url)) + '"></audio></div>';
            if (msgData.content) html += '<div class="message-text" style="margin-top: 5px;">' + escapeHtml(msgData.content) + '</div>';
        }
        html += '<div class="message-time">' + msgTime + '</div>';
        html += '</div></div>';
        if (isNew) {
            $container.append(html);
        } else {
            $container.prepend(html);
        }
    }
    $sendMsg.on('click', function () {
        if ($sendMsg.prop('disabled')) return;
        var content = $msgInput.val().trim();
        if (!content && !mediaData) {
            layui.layer.msg('请输入消息内容或添加媒体', { icon: 2 });
            return;
        }
        
        // 构建消息数据时不要包含具体的用户信息，让后端来填充
        var msgData = {
            sender: {
                id: "PLACEHOLDER_USER_ID",      // 后端会替换
                type: "PLACEHOLDER_USER_TYPE",   // 后端会替换
                name: "PLACEHOLDER_USER_NAME"    // 后端会替换
            },
            content: content,
            type: mediaType || 'text',
            timestamp: new Date().getTime()
        };
        
        if (mediaData) msgData.media = { url: mediaData };
        
        $.ajax({
            url: '/task/send-message',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ 
                taskId: taskId, 
                messageData: JSON.stringify(msgData) 
            }),
            beforeSend: function () {
                $sendMsg.prop('disabled', true).text('发送中...');
            },
            success: function (res) {
                $sendMsg.prop('disabled', false).text('发送');
                if (res.code === 0) {
                    // 构建用于显示的消息数据（使用实际的用户信息）
                    var displayMsgData = {
                        sender: { id: userId, type: userType, name: userName },
                        content: content,
                        type: msgData.type,
                        timestamp: new Date().getTime()
                    };
                    if (msgData.media) displayMsgData.media = msgData.media;
                    
                    var tempMessage = { 
                        id: res.data && res.data.messageId || 'temp-' + new Date().getTime(),
                        messageData: JSON.stringify(displayMsgData) 
                    };
                    appendMessage(tempMessage, true);
                    $msgInput.val('');
                    clearMedia();
                    scrollToBottom();
                    // Poll from the last received cursor so concurrent messages are not skipped.
                    refreshNewMessages();
                } else {
                    layui.layer.msg(res.msg || '发送失败', { icon: 2 });
                }
            },
            error: function () {
                $sendMsg.prop('disabled', false).text('发送');
                layui.layer.msg('发送失败，请稍后重试', { icon: 2 });
            }
        });
    });

    function scrollToBottom() {
        var container = $container[0];
        container.scrollTop = container.scrollHeight;
    }
    function isUserAtBottom() {
        var container = $container[0];
        return (container.scrollHeight - container.scrollTop - container.clientHeight) < 50;
    }
    function showNewMessageAlert() {
        if ($container.find('#newMessageAlert').length === 0) {
            $('<div id="newMessageAlert" class="new-message-alert"><button class="layui-btn layui-btn-sm layui-btn-normal"><i class="layui-icon">&#xe601;</i> 新消息</button></div>')
                .appendTo($container)
                .css({ 'position': 'absolute', 'bottom': '10px', 'left': '50%', 'transform': 'translateX(-50%)', 'z-index': '100', 'text-align': 'center' })
                .on('click', function () {
                    scrollToBottom();
                    $(this).remove();
                });
        }
    }
    function formatTime(date) {
        var today = new Date();
        var isToday = date.getDate() === today.getDate() &&
            date.getMonth() === today.getMonth() &&
            date.getFullYear() === today.getFullYear();
        var hours = date.getHours().toString().padStart(2, '0');
        var minutes = date.getMinutes().toString().padStart(2, '0');
        if (isToday) {
            return hours + ':' + minutes;
        } else {
            var month = (date.getMonth() + 1).toString().padStart(2, '0');
            var day = date.getDate().toString().padStart(2, '0');
            return month + '-' + day + ' ' + hours + ':' + minutes;
        }
    }
    function escapeHtml(text) {
        if (!text) return '';
        var map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return String(text).replace(/[&<>"']/g, function (m) { return map[m]; });
    }
    function safeMediaUrl(value) {
        try {
            var url = new URL(value, window.location.origin);
            return url.origin === window.location.origin && url.pathname.startsWith('/uploads/') ? url.href : '';
        } catch (e) { return ''; }
    }
    window.previewImage = function (src) {
        layui.layer.photos({
            photos: { "data": [{ "src": src }] },
            anim: 5
        });
    };

    loadMessages();
    if (taskStatus === 'rescuing' || taskStatus === 'confirming') {
        autoRefreshTimer = setInterval(function () {
            refreshNewMessages();
        }, 5000);
    }
    $(window).on('beforeunload', function () {
        if (autoRefreshTimer) clearInterval(autoRefreshTimer);
    });
}
