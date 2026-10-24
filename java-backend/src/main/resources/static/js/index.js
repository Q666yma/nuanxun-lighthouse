$(function () {
    if (document.getElementById('oldAnalysis')) echart_1();
    if (document.getElementById('chart_2')) echart_2();
    if (document.getElementById('chart_3')) echart_3();
    if (document.getElementById('reason')) echart_4();

    function echart_1() {
        // 基于准备好的dom，初始化echarts实例
        var myChart = echarts.init(document.getElementById('oldAnalysis'));
        option = {
            title: {
                text: '走失人口年龄分析（示例数据）',
                top: 10,
                left: 20,
                textStyle: {
                    fontSize: 18,
                    color: '#fff'
                }
            },
            tooltip: {
                trigger: 'item',
                formatter: "{b}: {c} ({d}%)",
            },
            legend: {
                right: 20,
                top: 35,
                data: ['0-14', '15-24', '25-44', '45-59', '60-70', '70-80', '80+'],
                textStyle: {
                    color: '#fff'
                }
            },
            series: [{
                type: 'pie',
                radius: ['0', '60%'],
                center: ['50%', '60%'],
                color: ['#3fa7dc', '#044bbe', '#2ca3fd', '#4b9b7e', '#d4b106', '#d94e5d', '#845ec2'],
                label: {
                    normal: {
                        formatter: '{b}\n{d}%'
                    },
                },
                data: [
                    { value: 17, name: '0-14' },
                    { value: 8, name: '15-24' },
                    { value: 5, name: '25-44' },
                    { value: 10, name: '45-59' },
                    { value: 18, name: '60-70' },
                    { value: 25, name: '70-80', selected: true },
                    { value: 17, name: '80+' }
                ]
            }]
        };
        // 使用刚指定的配置项和数据显示图表。
        myChart.setOption(option);
        window.addEventListener("resize", function () {
            myChart.resize();
        });
    }

    function echart_2() {
        var myChart = echarts.init(document.getElementById('chart_2'));
        var data = {
            id: 'multipleBarsLines',
            title: '每月救援活动统计',
            legendBar: ['救援数量', '中立占比', '负面占比'],
            symbol: '',
            legendLine: ['同期对比'],
            xAxis: ['一月', '二月', '三月', '四月', '五月', '六月'],
            yAxis: [
                [7, 12, 9, 15, 5, 11]
            ],
            lines: [
                [8, 10, 7, 12, 6, 9]
            ],
            barColor: ['#3FA7DC', '#7091C4', '#5170A2'],
            lineColor: ['#D9523F'],
        };

        var myData = (function test() {
            var yAxis = data.yAxis || [];
            var lines = data.lines || [];
            var legendBar = data.legendBar || [];
            var legendLine = data.legendLine || [];
            var symbol = data.symbol || ' ';
            var seriesArr = [];
            var legendArr = [];

            yAxis && yAxis.forEach((item, index) => {
                legendArr.push({
                    name: legendBar && legendBar.length > 0 && legendBar[index]
                });
                seriesArr.push({
                    name: legendBar && legendBar.length > 0 && legendBar[index],
                    type: 'bar',
                    barGap: '0.5px',
                    data: item,
                    barWidth: data.barWidth || 12,
                    label: {
                        normal: {
                            show: true,
                            formatter: '{c}' + symbol,
                            position: 'top',
                            textStyle: {
                                color: '#fff',
                                fontStyle: 'normal',
                                fontFamily: '微软雅黑',
                                textAlign: 'left',
                                fontSize: 11,
                            },
                        },
                    },
                    itemStyle: {
                        normal: {
                            barBorderRadius: 4,
                            color: data.barColor[index]
                        },
                    }
                });
            });

            lines && lines.forEach((item, index) => {
                legendArr.push({
                    name: legendLine && legendLine.length > 0 && legendLine[index]
                })
                seriesArr.push({
                    name: legendLine && legendLine.length > 0 && legendLine[index],
                    type: 'line',
                    data: item,
                    itemStyle: {
                        normal: {
                            color: data.lineColor[index],
                            lineStyle: {
                                width: 3,
                                type: 'solid',
                            }
                        }
                    },
                    label: {
                        normal: {
                            show: false,
                            position: 'top',
                        }
                    },
                    symbol: 'circle',
                    symbolSize: 10,
                    symbolOffset: [0, 5],  // 可以根据需要调整symbol的位置
                });
            });

            return {
                seriesArr,
                legendArr
            };
        })();

        option = {
            title: {
                show: true,
                top: '5%',
                left: '3%',
                text: data.title,
                textStyle: {
                    fontSize: 18,
                    color: '#fff'
                },
                subtext: data.subTitle,
                link: ''
            },
            tooltip: {
                trigger: 'axis',
                formatter: function (params) {
                    var time = '';
                    var str = '';
                    for (var i of params) {
                        time = i.name.replace(/\n/g, '') + '<br/>';
                        if (i.data == 'null' || i.data == null) {
                            str += i.seriesName + '：无数据' + '<br/>'
                        } else {
                            str += i.seriesName + '：' + i.data + '%<br/>'
                        }
                    }
                    return time + str;
                },
                axisPointer: {
                    type: 'none'
                },
            },
            legend: {
                right: data.legendRight || '30%',
                top: '12%',
                right: '5%',
                itemGap: 16,
                itemWidth: 10,
                itemHeight: 10,
                data: myData.legendArr,
                textStyle: {
                    color: '#fff',
                    fontStyle: 'normal',
                    fontFamily: '微软雅黑',
                    fontSize: 12,
                }
            },
            grid: {
                top: '15%',
                bottom: '15%',
                left: '15%',
                right: '15%',
            },
            xAxis: {
                type: 'category',
                data: data.xAxis,
                axisTick: {
                    show: false,
                },
                axisLine: {
                    show: true,
                    lineStyle: {
                        color: '#1AA1FD',
                    },
                    symbol: ['none', 'arrow']
                },
                axisLabel: {
                    show: true,
                    interval: '0',
                    textStyle: {
                        lineHeight: 16,
                        padding: [2, 2, 0, 2],
                        height: 50,
                        fontSize: 12,
                    },
                    rich: {
                        Sunny: {
                            height: 50,
                            padding: [0, 5, 0, 5],
                            align: 'center',
                        },
                    },
                    formatter: function (params, index) {
                        var newParamsName = "";
                        var splitNumber = 5;
                        var paramsNameNumber = params && params.length;
                        if (paramsNameNumber && paramsNameNumber <= 4) {
                            splitNumber = 4;
                        } else if (paramsNameNumber >= 5 && paramsNameNumber <= 7) {
                            splitNumber = 4;
                        } else if (paramsNameNumber >= 8 && paramsNameNumber <= 9) {
                            splitNumber = 5;
                        } else if (paramsNameNumber >= 10 && paramsNameNumber <= 14) {
                            splitNumber = 5;
                        } else {
                            params = params && params.slice(0, 15);
                        }

                        var provideNumber = splitNumber;
                        var rowNumber = Math.ceil(paramsNameNumber / provideNumber) || 0;
                        if (paramsNameNumber > provideNumber) {
                            for (var p = 0; p < rowNumber; p++) {
                                var tempStr = "";
                                var start = p * provideNumber;
                                var end = start + provideNumber;
                                if (p == rowNumber - 1) {
                                    tempStr = params.substring(start, paramsNameNumber);
                                } else {
                                    tempStr = params.substring(start, end) + "\n";
                                }
                                newParamsName += tempStr;
                            }
                        } else {
                            newParamsName = params;
                        }
                        params = newParamsName;
                        return '{Sunny|' + params + '}';
                    },
                    color: '#1AA1FD',
                },
            },
            yAxis: {
                axisLine: {
                    show: true,
                    lineStyle: {
                        color: '#1AA1FD',
                    },
                    symbol: ['none', 'arrow']
                },
                type: 'value',
                min: -2,  // 设置最小值
                max: 24,  // 根据你折线的最高值调整这个数值，避免数据过于集中
                axisTick: {
                    show: false
                },
                axisLabel: {
                    show: false
                },
                splitLine: {
                    show: false,
                    lineStyle: {
                        color: '#1AA1FD',
                        type: 'solid'
                    },
                },
            },
            series: myData.seriesArr
        };

        myChart.setOption(option);
        window.addEventListener("resize", function () {
            myChart.resize();
        });
    }


    function echart_3() {
        var myChart = echarts.init(document.getElementById('chart_3'));
        var uploadedDataURL = "/page/test.json";
        myChart.showLoading();
        var data = [];
        $.getJSON(uploadedDataURL, function (geoJson) {
            echarts.registerMap('mp', geoJson);
            data = geoJson.features.map((item) => { // 显示窗口的数据转换
                return {
                    value: Math.ceil((Math.random() * 10)),
                    name: item.properties.name
                }
            });
            myChart.hideLoading();
            option = {
                backgroundColor: '#0c1e31', // 背景
                tooltip: { // 窗口外框
                    backgroundColor: 'rgba(0,0,0,0)',
                    trigger: 'item',
                },
                title: {
                    text: '走失人口区域分析',
                    x: 'center',
                    textStyle: {
                        color: '#FFF'
                    },
                    left: '1%'

                },
                legend: {
                    show: false,
                },
                series: [{
                    tooltip: { // 显示的窗口
                        trigger: 'item',

                    },
                    name: '',
                    type: 'map',
                    map: 'mp', // 自定义扩展图表类型
                    // zoom: 0.65, //缩放
                    aspectScale: 0.75,
                    zoom: 1.1,
                    roam: true,
                    showLegendSymbol: true,
                    label: { // 文字
                        show: true,
                        color: '#fff',
                        fontSize: 10
                    },
                    itemStyle: { //地图样式
                        normal: {
                            borderColor: 'rgba(147, 235, 248, 1)',
                            borderWidth: 1,
                            areaColor: {
                                type: 'radial',
                                x: 0.5,
                                y: 0.5,
                                r: 0.8,
                                colorStops: [{
                                    offset: 0,
                                    color: 'rgba(147, 235, 248, 0)' // 0% 处的颜色
                                }, {
                                    offset: 1,
                                    color: 'rgba(147, 235, 248, .2)' // 100% 处的颜色
                                }],
                                globalCoord: false // 缺省为 false
                            },
                            shadowColor: 'rgba(128, 217, 248, 1)',
                            shadowOffsetX: -2,
                            shadowOffsetY: 2,
                            shadowBlur: 10
                        }
                    },
                    emphasis: { //鼠标移入动态的时候显示的默认样式
                        itemStyle: {
                            areaColor: '#389BB7',
                            borderColor: '#404a59',
                            borderWidth: 1
                        }
                    },
                    layoutCenter: ['50%', '50%'],
                    layoutSize: '160%',
                    markPoint: {
                        symbol: 'none'
                    },
                    data: data,
                }],
            }
            myChart.setOption(option);

        })
    }
    /*
    function echart_3() {
        var myChart = echarts.init(document.getElementById('chart_3'));
        var data = []
        var uploadedDataURL = "/page/test.json";
        $.getJSON(uploadedDataURL, function (geoJson) {
            echarts.registerMap('chart_3', geoJson);
            data = geoJson.features.map((item) => { // 显示窗口的数据转换
                console.log(item);
                return {
                    value: (Math.random() * 1000).toFixed(2),
                    name: item.properties.name
                }
            });
            myChart.hideLoading();
            var option = {
                backgroundColor: '#0c1e31',
                tooltip: {
                    trigger: 'item',
                },
                title: {
                    text: '走失人员区域分析',
                    x: 'center',
                    textStyle: {
                        color: '#FFF'
                    },
                    left: '1%'

                },


                geo: {
                    map: 'chart_3',
                    aspectScale: 0.75,
                    zoom: 1.1,
                    roam: true,
                    data:data,
                    itemStyle: {
                        normal: {
                            borderColor: 'rgba(147, 235, 248, 1)',
                            borderWidth: 1,
                            areaColor: {
                                type: 'radial',
                                x: 0.5,
                                y: 0.5,
                                r: 0.8,
                                colorStops: [{
                                    offset: 0,
                                    color: 'rgba(175,238,238, 0)' // 0% 处的颜色
                                }, {
                                    offset: 1,
                                    color: 'rgba(	47,79,79, .1)' // 100% 处的颜色
                                }],
                                globalCoord: false // 缺省为 false
                            },
                            shadowColor: 'rgba(128, 217, 248, 1)',
                            shadowOffsetX: -2,
                            shadowOffsetY: 2,
                            shadowBlur: 10
                        },
                        emphasis: {
                            areaColor: '#389BB7',
                            borderWidth: 0
                        }
                    }
                },

                series:[
                    {
                        tooltip: { // 显示的窗口
                            trigger: 'item',
                        },
                        type: 'map',
                        roam: true,
                        label: {
                            normal: {
                                show: true,
                                textStyle: {
                                    color: '#06a8d8'
                                }
                            },
                            emphasis: {
                                textStyle: {
                                    color: '#F4E925'
                                }
                            }
                        },

                        itemStyle: { //地图样式
                            normal: {
                                areaColor: '#031525',
                                borderColor: '#FFFFFF',
                            },
                            emphasis: {   //鼠标一如时默认样式
                                areaColor: '#2B91B7'
                            }
                        },
                        zoom: 1.1,
                        roam: true,
                        data: data
                    }

                ]
            }
            myChart.setOption(option);
        })

    }
    */

    function echart_4() {
        var myChart = echarts.init(document.getElementById('reason'));
        option = {
            color: ['#44aff0', '#4777f5', '#5045f6', '#ad46f3', '#f845f1', '#f9a825'],
            tooltip: {
                trigger: 'item',
                formatter: "{b} : {c} 占比 ({d}%)",
                textStyle: {
                    fontSize: 16,
                },
            },
            series: [{
                type: 'pie',
                clockwise: false,
                startAngle: 90,
                radius: '75%',
                center: ['44%', '50%'],
                hoverAnimation: false,
                roseType: 'radius',
                data: [
                    { value: 290, name: '迷路' },
                    { value: 210, name: '记忆力障碍' },
                    { value: 140, name: '精神健康问题' },
                    { value: 90, name: '意外事故' },
                    { value: 180, name: '认知障碍' },
                    { value: 90, name: '其他' }
                ],
                itemStyle: {
                    normal: {
                        borderColor: '#273454',
                        borderWidth: '5',
                    },
                },
                label: {
                    show: true,
                    position: 'outside',
                    rich: {
                        hr: {
                            backgroundColor: 't',
                            borderRadius: 100,
                            width: 0,
                            height: 10,
                            padding: [3, 3, 0, -16],
                            shadowColor: '#1c1b3a',
                            shadowBlur: 1,
                            shadowOffsetX: '0',
                            shadowOffsetY: '2',
                        },
                        a: {
                            padding: [-35, 15, -20, 5],
                        }
                    }
                },
                labelLine: {
                    normal: {
                        length: 20,
                        length2: 30,
                        lineStyle: {
                            width: 1,
                        }
                    }
                },
            }],
        };
        myChart.setOption(option);

    }


});
