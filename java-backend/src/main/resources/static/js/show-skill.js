(function () {
    'use strict';
    var started = false, chart;

    async function init() {
        if (started) return;
        started = true;
        var el = document.getElementById('skillChart');
        if (!el || !window.echarts) return;
        chart = echarts.init(el);
        var title = document.getElementById('skill-chartTitle');
        var center = document.getElementById('skill-centerLogoContainer');
        var total = document.getElementById('skill-totalRescuers');
        var note = document.getElementById('skill-chartNote');
        center.style.display = 'none';
        total.textContent = '—';

        function showMessage(message) {
            center.style.display = 'none';
            if (note) note.textContent = '';
            var lines = message.split('\n');
            chart.setOption({
                title: {text: lines[0], subtext: lines[1] || '', left: 'center', top: 'center', textStyle: {color: '#dbe6f2', fontSize: 14}, subtextStyle: {color: '#b6c8da', fontSize: 12}},
                legend: {show: false}, tooltip: {show: false}, series: []
            }, true);
        }

        showMessage('正在读取队员技能…');
        try {
            var response = await fetch('/mapshow/skill-distribution', {credentials: 'same-origin', cache: 'no-store'});
            var data = await response.json();
            if (!response.ok) throw new Error(data.message || data.msg || '技能分布读取失败');
            var family = data.scope === 'family';
            var name = family ? '参与我的求助的队员技能' : '相关队员技能分布';
            // Skills overlap: one person may appear under several skills.
            // Plot the server's per-skill people counts, never category totals or pie shares.
            var rows = [];
            Object.keys(data.categoryDetails || {}).forEach(function (key) {
                (data.categoryDetails[key] || []).forEach(function (row) {
                    if (Number.isFinite(Number(row.value)) && Number(row.value) > 0) {
                        rows.push({name: row.name, value: Number(row.value)});
                    }
                });
            });
            rows.sort(function (a, b) {return b.value - a.value || a.name.localeCompare(b.name, 'zh-CN');});
            total.textContent = data.totalRescuers;
            total.parentNode.replaceChildren(
                document.createTextNode(family ? '参与我的求助的队员：' : '当前相关队员：'),
                total, document.createTextNode(' 人')
            );

            title.textContent = name;
            if (!rows.length) {
                showMessage(Number(data.totalRescuers) === 0
                    ? (family ? '暂无队员参与我的求助\n队员接单后将在这里展示技能' : '暂无相关搜救队员')
                    : '相关队员暂未填写技能');
                return;
            }
            var scroll = rows.length > 8;
            if (note) note.textContent = '同一队员可具备多项技能，人数不相加。' + (scroll ? '共 ' + rows.length + ' 项，拖动右侧查看。' : '');
            var maxPeople = Math.max.apply(null, rows.map(function (row) {return row.value;}));
            chart.setOption({
                title: {show: false}, legend: {show: false},
                grid: {left: 12, right: scroll ? 42 : 30, top: 28, bottom: 24, containLabel: true},
                tooltip: {trigger: 'axis', confine: true, axisPointer: {type: 'shadow'},
                    formatter: function (params) {
                        var row = params[0];
                        var safeName = String(row.name).replace(/[&<>"']/g, function (letter) {
                            return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[letter];
                        });
                        return safeName + '<br>具备该技能：' + row.value + ' 人<br>相关队员：' + data.totalRescuers + ' 人';
                    }},
                xAxis: {type: 'value', name: '人数', min: 0, max: maxPeople, minInterval: 1, splitNumber: 3,
                    nameTextStyle: {color: '#b6c8da'}, axisLabel: {color: '#b6c8da'},
                    splitLine: {lineStyle: {color: 'rgba(182,200,218,0.12)'}}, axisTick: {show: false}, axisLine: {show: false}},
                yAxis: {type: 'category', inverse: true, data: rows.map(function (row) {return row.name;}),
                    axisLabel: {color: '#dbe6f2', fontSize: 12}, axisTick: {show: false}, axisLine: {show: false}},
                dataZoom: scroll ? [
                    {type: 'slider', yAxisIndex: 0, orient: 'vertical', right: 4, top: 28, bottom: 24, width: 10,
                        startValue: 0, endValue: 7, zoomLock: true, showDetail: false,
                        borderColor: 'transparent', fillerColor: 'rgba(78,153,201,0.35)',
                        backgroundColor: 'rgba(182,200,218,0.08)', handleSize: 0},
                    {type: 'inside', yAxisIndex: 0, startValue: 0, endValue: 7, zoomLock: true,
                        zoomOnMouseWheel: false, moveOnMouseWheel: true, moveOnMouseMove: true}
                ] : [],
                series: [{name: '具备该技能的队员', type: 'bar', barMaxWidth: 18,
                    itemStyle: {color: '#4e99c9'}, label: {show: true, position: 'right', color: '#dbe6f2', formatter: '{c} 人'},
                    data: rows.map(function (row) {return row.value;})}]
            }, true);
        } catch (error) {
            total.textContent = '—';
            showMessage('技能数据暂时无法加载\n请稍后刷新重试');
        }
        window.addEventListener('resize', function () {chart.resize();});
    }

    window.SkillChartModule = {init: init, getChart: function () {return chart;}};
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
    else init();
})();
