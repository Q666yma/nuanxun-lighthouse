
$(function () {
    echart_1();
    echart_2();
    echart_3();
    echart_4();

    function echart_1() {
        var myChart = echarts.init(document.getElementById('chart_1'));

        // 加载青岛市地图的 GeoJSON 数据
        $.getJSON('page/test.json', function(geoJson) {
            // 注册青岛地图
            echarts.registerMap('qingdao', geoJson);

            // 配置选项
            var option = {
                title: {
                    text: '走失人员区域热力图（青岛市）',
                    left: 'center',
                    textStyle: {
                        fontSize: 18,
                        color: '#fff'
                    }
                },
                tooltip: {
                    trigger: 'item',
                    formatter: '{b}: {c}人'
                },
                visualMap: {
                    min: 0,
                    max: 100,
                    left: 'right',
                    top: 'bottom',
                    text: ['高', '低'],
                    calculable: true,
                    inRange: {
                        color: ['#d4e157', '#ffca28', '#f57f17']
                    }
                },
                series: [{
                    name: '走失人员数量',
                    type: 'map',
                    map: 'qingdao',  // 使用青岛地图
                    label: {
                        show: true,
                        textStyle: {
                            color: '#fff'
                        }
                    },
                    data: [
                        {name: '南京市', value: 70},
                        {name: '苏州市', value: 50},
                        {name: '无锡市', value: 20},
                        {name: '常州市', value: 10},
                        {name: '扬州市', value: 5},
                        {name: '徐州市', value: 15},
                        {name: '南通市', value: 25},
                        {name: '连云港市', value: 18},
                        {name: '淮安市', value: 12},
                        {name: '盐城市', value: 22},
                        {name: '镇江市', value: 8},
                        {name: '泰州市', value: 17},
                        {name: '宿迁市', value: 6}
                    ]
                }]
            };

            // 使用刚指定的配置项和数据显示图表。
            myChart.setOption(option);
            window.addEventListener("resize", function () {
                myChart.resize();
            });
        });
    }




    function echart_2() {
        var myChart = echarts.init(document.getElementById('chart_2'));

        // 配置选项
        var option = {
            color: ['#44aff0', '#4777f5', '#5045f6', '#ad46f3'],
            title: {
                text: '走失人员常见走失地点',
                left: 'center',
                textStyle: {
                    fontSize: 18,
                    color: '#fff'
                }
            },
            tooltip: {
                trigger: 'item',
                formatter: '{b}: {c} ({d}%)',
                textStyle: {
                    fontSize: 16
                }
            },
            series: [{
                type: 'pie',
                radius: '60%',
                center: ['50%', '50%'],
                data: [
                    {value: 39.3, name: '商场'},
                    {value: 29.8, name: '公园'},
                    {value: 23.4, name: '学校'},
                    {value: 7.5, name: '饭店'}
                ],
                emphasis: {
                    itemStyle: {
                        shadowBlur: 10,
                        shadowOffsetX: 0,
                        shadowColor: 'rgba(0, 0, 0, 0.5)'
                    }
                },
                label: {
                    show: true,
                    position: 'outside',
                    formatter: '{b}: {d}%',
                    textStyle: {
                        color: '#fff'
                    }
                },
                labelLine: {
                    length: 20,
                    length2: 30,
                    lineStyle: {
                        color: '#fff',
                        width: 1
                    }
                }
            }]
        };

        // 使用刚指定的配置项和数据显示图表。
        myChart.setOption(option);
        window.addEventListener("resize", function () {
            myChart.resize();
        });
    }


    function echart_3() {
        var myChart = echarts.init(document.getElementById('chart_3'));

        // 配置选项
        var option = {
            color: ['#3fa7dc'],
            title: {
                text: '每月走失情况分析',
                left: 'center',
                textStyle: {
                    fontSize: 18,
                    color: '#fff'
                }
            },
            tooltip: {
                trigger: 'axis',
                formatter: '{b}: {c}人',
                textStyle: {
                    fontSize: 16
                }
            },
            xAxis: {
                type: 'category',
                data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
                axisLine: {
                    lineStyle: {
                        color: '#fff'
                    }
                },
                axisLabel: {
                    textStyle: {
                        color: '#fff'
                    }
                }
            },
            yAxis: {
                type: 'value',
                axisLine: {
                    lineStyle: {
                        color: '#fff'
                    }
                },
                axisLabel: {
                    textStyle: {
                        color: '#fff'
                    }
                }
            },
            series: [{
                type: 'bar',
                data: [12, 19, 23, 30, 45, 50, 55, 40, 35, 25, 18, 10],
                barWidth: '50%',
                itemStyle: {
                    color: '#3fa7dc',
                    borderColor: '#273454',
                    borderWidth: 2
                },
                label: {
                    show: true,
                    position: 'top',
                    textStyle: {
                        color: '#fff'
                    }
                }
            }]
        };

        // 使用刚指定的配置项和数据显示图表。
        myChart.setOption(option);
        window.addEventListener("resize", function () {
            myChart.resize();
        });
    }


    function echart_4() {
        var myChart = echarts.init(document.getElementById('chart_4'));

        // 配置选项
        var option = {
            color: ['#82b1ff'],
            title: {
                text: '人员走失时段分析',
                left: 'center',
                textStyle: {
                    fontSize: 18,
                    color: '#fff'
                }
            },
            tooltip: {
                trigger: 'item',
                formatter: '{b}: {c}人',
                textStyle: {
                    fontSize: 16,
                }
            },
            grid: {
                left: '5%', // 调整左边距
                right: '5%',
                bottom: '5%',
                containLabel: true
            },
            xAxis: {
                type: 'value',
                boundaryGap: [0, 0.01],
                axisLine: {
                    lineStyle: {
                        color: '#fff'
                    }
                }
            },
            yAxis: {
                type: 'category',
                data: ['0-4点', '5-8点', '9-12点', '13-16点', '17-20点', '21-24点'],
                axisLine: {
                    lineStyle: {
                        color: '#fff'
                    }
                },
                axisLabel: {
                    textStyle: {
                        color: '#fff'
                    }
                }
            },
            series: [{
                type: 'bar',
                data: [
                    {value: 10, name: '0-4点'},
                    {value: 25, name: '5-8点'},
                    {value: 35, name: '9-12点'},
                    {value: 45, name: '13-16点'},
                    {value: 30, name: '17-20点'},
                    {value: 20, name: '21-24点'}
                ],
                itemStyle: {
                    color: '#82b1ff',
                    borderColor: '#273454',
                    borderWidth: 2,
                },
                label: {
                    show: true,
                    position: 'right',
                    textStyle: {
                        color: '#fff'
                    }
                }
            }]
        };

        // 使用刚指定的配置项和数据显示图表。
        myChart.setOption(option);
        window.addEventListener("resize", function () {
            myChart.resize();
        });
    }




});