# 暖寻灯塔 · Nuanxun Lighthouse

面向走失人员寻找与公益救援的 Web 协同平台。家属发布求助并跟进进展，搜救志愿者参与任务、反馈线索，管理员负责审核与协调。

体验地址：<https://nuanxun.top/>。

## 功能

- 家属：求助发布、联系人资料、进度与结果确认、消息通知、平台搜救概览。
- 搜救志愿者：申请与审核、接单、地图标注、热力展示、位置共享与轨迹、任务交流及辅助识别。
- 管理员：用户与队员审核、任务协调、通知、内容管理及统计。
- 公共辅助模块：微课程、感谢墙、心理关怀、报告与寻人海报。

身份、审核状态及任务参与关系共同决定可访问的数据。热力图展示记录密度或标注语义，不代表找到概率或区域安全结论。

## 技术与目录

| 目录 | 内容 |
| --- | --- |
| `java-backend/` | Java 11、Spring Boot、MyBatis；业务接口、权限、HTML 模板及前端资源 |
| `python-service/` | Flask 智能辅助服务、推荐、报告、海报等 |
| `database/` | MySQL 表结构、迁移及技能标签基础数据 |
| `deploy/` | 本地 Nginx 同源反向代理示例 |
| `tools/` | 编译、启动及空库演示账号初始化工具 |
| `docs/` | 系统说明与技术架构 |

前端使用 HTML/CSS/JavaScript、Layui、ECharts 和高德地图；数据服务使用 MySQL、Redis。邮件、地图、智谱、百度人脸及讯飞声纹需要自行配置账号与授权。

## 本地运行

### 1. 准备环境

- JDK 11、Maven 3.8+、Python 3.11 或 3.12。
- MySQL 8.0.13+、Redis、Nginx；音视频功能还需要 FFmpeg。
- 将 `java`、`javac`、`mvn`、`python` 加入 PATH，或设置 `JAVA_HOME`、`MAVEN_HOME`。

首次编译通过 Maven 下载 Java 依赖；`javacv-platform` 包含多个平台的原生组件，下载量较大。课程视频需要自行补充至课程模块对应的资源路径。

```bash
git clone https://github.com/Q666yma/nuanxun-lighthouse.git
cd nuanxun-lighthouse
python -m venv .venv
```

激活虚拟环境：Windows PowerShell 使用 `.\.venv\Scripts\Activate.ps1`；Linux/macOS 使用 `source .venv/bin/activate`。然后安装 Python 依赖：

```bash
python -m pip install -r python-service/requirements.txt
```

### 2. 创建本地数据库与配置

创建字符集为 `utf8mb4` 的 `web` 数据库，为本地应用创建有相应读写和建表权限的数据库用户。使用 MySQL 客户端连接该库，按 `01` 至 `08` 的顺序执行 `database/` 中的 SQL。

将根目录 `.env.example` 复制为 `.env`，填写 `DB_USER`、`DB_PASSWORD`、`DB_JDBC_URL`、`VOLUNTEER_DB_URL`。Java 和 Python 必须指向同一个数据库。Python 数据库连接串中的密码需要 URL 编码。

启动本地 Redis（默认 `127.0.0.1:6379`）。若地址不同，可通过 Spring Boot 的环境变量如 `SPRING_REDIS_HOST`、`SPRING_REDIS_PORT` 覆盖。

需要演示账号时，在 `.env` 设置自选的 `DEMO_PASSWORD`（至少 8 个字符），运行：

```bash
python tools/run.py init
```

该工具仅允许在 `users` 表为空时执行，创建“灯塔管理员”“家属清禾”“队员远航”三个演示账号，密码均为自设的 `DEMO_PASSWORD`。

### 3. 启动服务

打开两个终端，在项目根目录、已激活的虚拟环境中分别运行：

```bash
python tools/run.py java
```

```bash
python tools/run.py python
```

工具自动读取根目录 `.env`；操作系统已有的同名环境变量优先。Java 默认监听 `127.0.0.1:8080`，Python 默认监听 `127.0.0.1:5000`。

将 `deploy/nginx-local.conf` 的 `server` 配置放入 Nginx 的 `http` 配置块中，启动或重新加载 Nginx。通过 <http://127.0.0.1:8088/> 访问完整系统。使用同源入口可保留 Java 会话，并转发 Python 功能请求。

### 4. 可选外部服务

在 `.env` 填写需要使用的邮件、AI、识别和地图配置。浏览器地图页面还需把 `YOUR_AMAP_WEB_KEY` 和 `YOUR_AMAP_SECURITY_JS_CODE` 换成自己的 Web 地图配置；可在 `java-backend/src/main/resources/templates/` 中搜索这两个占位符定位。

未配置或未授权的外部能力可能不可用；声纹接口尤其需要供应商账号开通相应权限。浏览器定位还依赖用户许可与安全上下文，公网部署应使用 HTTPS。

## 项目文档

- [系统说明与技术架构](docs/系统说明与技术架构.md)
