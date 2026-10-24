// 地图初始化
let map, mouseTool, currentOverlay;
let markers = [];
let taskId = document.getElementById('taskId').textContent;
let currentUserId = 1;  // 在实际应用中，这应该从session获取

// 初始化函数
function initMap() {
    // 从后端获取地图配置
    fetch(`/api/map/task/${taskId}`)
        .then(response => response.json())
        .then(data => {
            // 初始化地图
            map = new AMap.Map('map', {
                zoom: data.zoomLevel || 15,
                center: [data.centerLng, data.centerLat]
            });
            
            // 加载UI组件库
            AMapUI.loadUI(['misc/MouseTool'], function(MouseTool) {
                mouseTool = new MouseTool(map);
                
                // 绑定事件
                mouseTool.on('draw', function(e) {
                    currentOverlay = e.obj;
                    document.getElementById('marker-info').style.display = 'block';
                });
                
                // 加载已有标记
                loadMarkers();
                
                // 绑定按钮事件
                bindEvents();
            });
        })
        .catch(error => {
            console.error('加载地图失败:', error);
            // 使用默认配置
            map = new AMap.Map('map', {
                zoom: 15,
                center: [116.397428, 39.90923]  // 默认北京中心
            });
        });
}

// 加载已有标记
function loadMarkers() {
    fetch(`/api/map/markers/${taskId}`)
        .then(response => response.json())
        .then(data => {
            data.forEach(marker => {
                const geometry = JSON.parse(marker.geometry);
                const properties = JSON.parse(marker.properties);
                
                // 根据类型创建不同的覆盖物
                let overlay;
                
                if (marker.markerType === 'polygon') {
                    overlay = new AMap.Polygon({
                        path: geometry.coordinates[0],
                        strokeColor: properties.color,
                        strokeWeight: 2,
                        fillColor: properties.color,
                        fillOpacity: 0.4
                    });
                } else if (marker.markerType === 'marker') {
                    overlay = new AMap.Marker({
                        position: geometry.coordinates,
                        title: properties.description
                    });
                } else if (marker.markerType === 'polyline') {
                    overlay = new AMap.Polyline({
                        path: geometry.coordinates,
                        strokeColor: properties.color,
                        strokeWeight: 5
                    });
                }
                
                if (overlay) {
                    // 添加到地图
                    map.add(overlay);
                    
                    // 添加信息窗体
                    if (properties.description) {
                        overlay.on('click', function() {
                            const infoWindow = new AMap.InfoWindow({
                                content: properties.description,
                                offset: new AMap.Pixel(0, -30)
                            });
                            
                            if (marker.markerType === 'marker') {
                                infoWindow.open(map, geometry.coordinates);
                            } else {
                                infoWindow.open(map, map.getCenter());
                            }
                        });
                    }
                    
                    // 保存引用
                    markers.push({
                        id: marker.id,
                        overlay: overlay
                    });
                }
            });
        })
        .catch(error => {
            console.error('加载标记失败:', error);
        });
}

// 绑定按钮事件
function bindEvents() {
    // 绘制多边形
    document.getElementById('draw-polygon').addEventListener('click', function() {
        closeDrawTools();
        mouseTool.polygon();
    });
    
    // 绘制标记点
    document.getElementById('draw-marker').addEventListener('click', function() {
        closeDrawTools();
        mouseTool.marker();
    });
    
    // 绘制路径
    document.getElementById('draw-path').addEventListener('click', function() {
        closeDrawTools();
        mouseTool.polyline();
    });
    
    // 清除当前
    document.getElementById('clear-current').addEventListener('click', function() {
        if (currentOverlay) {
            map.remove(currentOverlay);
            currentOverlay = null;
        }
        document.getElementById('marker-info').style.display = 'none';
    });
    
    // 保存标记
    document.getElementById('save-marker').addEventListener('click', function() {
        if (!currentOverlay) return;
        
        const description = document.getElementById('marker-description').value;
        const color = document.getElementById('marker-color').value;
        
        // 获取几何数据
        let geometryData;
        let markerType;
        
        if (currentOverlay instanceof AMap.Polygon) {
            markerType = 'polygon';
            geometryData = {
                type: 'Polygon',
                coordinates: [currentOverlay.getPath().map(point => [point.lng, point.lat])]
            };
        } else if (currentOverlay instanceof AMap.Marker) {
            markerType = 'marker';
            const position = currentOverlay.getPosition();
            geometryData = {
                type: 'Point',
                coordinates: [position.lng, position.lat]
            };
        } else if (currentOverlay instanceof AMap.Polyline) {
            markerType = 'polyline';
            geometryData = {
                type: 'LineString',
                coordinates: currentOverlay.getPath().map(point => [point.lng, point.lat])
            };
        }
        
        // 准备发送的数据
        const markerData = {
            mapTaskId: parseInt(taskId),
            markerType: markerType,
            geometry: JSON.stringify(geometryData),
            properties: JSON.stringify({
                description: description,
                color: color
            })
        };
        
        // 发送到后端
        fetch('/api/map/marker', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(markerData)
        })
        .then(response => response.json())
        .then(data => {
            console.log('标记保存成功:', data);
            
            // 给当前覆盖物添加点击事件
            if (currentOverlay && description) {
                currentOverlay.on('click', function() {
                    const infoWindow = new AMap.InfoWindow({
                        content: description,
                        offset: new AMap.Pixel(0, -30)
                    });
                    
                    if (markerType === 'marker') {
                        infoWindow.open(map, geometryData.coordinates);
                    } else {
                        infoWindow.open(map, map.getCenter());
                    }
                });
            }
            
            // 记录ID和引用
            markers.push({
                id: data.id,
                overlay: currentOverlay
            });
            
            // 重置当前状态
            currentOverlay = null;
            document.getElementById('marker-info').style.display = 'none';
            document.getElementById('marker-description').value = '';
            
            // 关闭绘图工具
            mouseTool.close();
        })
        .catch(error => {
            console.error('保存标记失败:', error);
            alert('保存失败，请重试');
        });
    });
}

// 关闭绘图工具
function closeDrawTools() {
    mouseTool.close();
    if (currentOverlay) {
        map.remove(currentOverlay);
        currentOverlay = null;
    }
    document.getElementById('marker-info').style.display = 'none';
}

// 初始化地图
document.addEventListener('DOMContentLoaded', initMap);