# 🤖 AI智能体 · 时政新闻简报系统

> 独立实现的完整全栈 AI Agent 项目。  
> 基于 **ReAct 架构**，集成阿里云通义千问大模型与 Tavily 搜索 API，实现了一个能够自主搜索、推理、汇总时政新闻的 AI 智能体，并配套中国风 UI 前端。

---

## 📋 目录

- [项目简介](#项目简介)
- [技术架构](#技术架构)
- [功能特性](#功能特性)
- [核心实现](#核心实现)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [环境配置](#环境配置)

---

## 项目简介

本项目是一个具备**自主工具调用能力**的 AI 智能体应用，核心能力：

- 用户输入自然语言问题（如"今日简报"、"最新 AI 新闻"）
- Agent 自动判断意图，决策是否调用搜索工具、调用几次
- 实时抓取过去 24 小时的时政/科技/经济等维度新闻
- 前端采用中国风水墨 UI 设计

**项目亮点：**
- 手动实现 ReAct（Reasoning + Acting）思考-行动循环，不依赖框架封装的黑盒
- 自定义工具调用管理，完全掌控上下文维护与 Agent 状态机
- SSE 流式输出 + 异步多线程，避免长任务阻塞
- 精细的 Prompt Engineering，支持综合日报型 / 专项话题型双模式智能切换

---

## 技术架构

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| **Java** | 21 | 主语言 |
| **Spring Boot** | 3.5.x | Web 框架 |
| **Spring AI** | 1.0.0 | AI 应用抽象层（ChatClient、ToolCallback） |
| **Spring AI Alibaba** | 1.0.0.2 | 阿里云 DashScope / 通义千问接入 |
| **通义千问 (qwen-max)** | — | 核心 LLM，负责推理与工具调用决策 |
| **Tavily Search API** | — | 实时新闻搜索工具（24h 时效性） |
| **Hutool** | 5.8.x | HTTP 请求、JSON 解析 |
| **Lombok** | 1.18.x | 简化 POJO |
| **Knife4j** | 4.4.0 | Swagger API 文档（调试用） |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue 3** | ^3.4 | 响应式 UI 框架 |
| **Vue Router** | ^4.3 | 前端路由 |
| **Pinia** | ^2.1 | 全局状态管理（聊天记录、流式状态） |
| **Axios** | ^1.6 | HTTP 请求 |
| **Marked** | ^12.0 | Markdown 渲染（AI 回复富文本） |
| **Vite** | ^5.2 | 构建工具 |
| **SCSS** | — | 中国风主题样式变量系统 |

---

## 功能特性

### ✅ 已实现

| 功能 | 描述 |
|------|------|
| **智能意图识别** | 自动区分"综合日报型"与"专项话题型"，动态决定工具调用次数（1次 vs 5次） |
| **时政新闻搜索** | 调用 Tavily API，精确限定过去 24 小时，覆盖政治/经济/社会/科技/国际五大维度 |
| **结构化新闻简报** | 自动整合多次工具结果，按模板输出带 emoji 的格式化简报 |
| **SSE 实时流式输出** | 后端 SseEmitter + 前端 EventSource |
| **ReAct 智能体循环** | think → act → think → ... 自主循环，直到调用 terminate 工具结束 |
| **会话状态管理** | 前端 Pinia Store 管理消息列表、流式状态、连接错误 |
| **中国风 UI** | 水墨纸张质感、毛笔字体、卷轴装饰，响应式适配移动端 |
| **API 文档** | 集成 Knife4j，访问 `/api/doc.html` 即可调试接口 |

---

## 核心实现

### ReAct Agent 架构

```
用户输入
   │
   ▼
BaseAgent.runStream()          ← 异步线程 + SseEmitter
   │
   ▼
循环 (maxStep=8)
   │
   ├─► ReActAgent.step()
   │      │
   │      ├─► think()          ← 调用 LLM，判断是否需要工具
   │      │      │
   │      │      ├── 需要工具 → 返回 true
   │      │      └── 不需要  → 保存最终答案，状态 FINISHED
   │      │
   │      └─► act()            ← 执行工具调用，更新消息上下文
   │             │
   │             └── 检测到 terminate 工具 → 状态 FINISHED
   │
   └─► state == FINISHED → SSE 推送最终答案 → complete()
```

### Agent 类继承关系

```
BaseAgent (状态机 + 执行循环 + SSE 流式)
    └── ReActAgent (step = think + act 模板方法)
            └── ToolCallAgent (工具调用管理 + 上下文维护)
                    └── NewsManus (系统提示词 + 新闻场景配置)
```

### 工具注册与调用流程

1. `ToolRegistration` 读取配置文件中的 Tavily API Key，实例化 `PolicyNewsTool`，注册为 Spring Bean
2. `ToolCallAgent` 构造时，禁用 Spring AI 内置自动工具执行（`withInternalToolExecutionEnabled(false)`），改为手动维护消息上下文
3. 每次 `think()` 后，若 LLM 决定调用工具，`act()` 中由 `ToolCallingManager` 统一执行，结果追加到 `messageList`
4. LLM 在完成所有工具调用后，直接输出汇总结论，`think()` 检测到无 ToolCall 即终止循环

### 双模式 Prompt 设计

```
综合日报型（"今日简报"、"每日新闻"）
  └── 调用工具 5 次：DOMESTIC_POLITICS / MACRO_ECONOMY / SOCIAL / TECHNOLOGY / INTERNATIONAL
  └── 输出：📅 每日情报简报（含5个版块 + 今日要点总结）

专项话题型（"AI新闻"、"芯片"）
  └── 调用工具 1 次：category=AUTO，specificFocus=用户关键词
  └── 输出：🔍 话题快讯（最多5条 + 话题小结）
```

---

## 快速开始

### 环境准备

| 依赖 | 要求 |
|------|------|
| JDK | 21+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| 通义千问 API Key | [阿里云百炼控制台](https://bailian.console.aliyun.com/) 申请 |
| Tavily API Key | [tavily.com](https://tavily.com/) 申请（有免费额度） |

### 后端启动

**1. 配置 API Key**

复制配置文件模板（⚠️ 不要直接修改并提交原文件）：

```bash
cp src/main/resources/application.yml src/main/resources/application-local.yml
```

在 `application-local.yml` 中填入真实 Key：

```yaml
spring:
  ai:
    dashscope:
      api-key: sk-你的通义千问ApiKey

search-api:
  api-key: tvly-你的TavilyApiKey
```

**2. 启动**

```bash
mvn spring-boot:run -Dspring.profiles.active=local
```

> 后端服务启动在 `http://localhost:8123`，接口文档：`http://localhost:8123/api/doc.html`

### 前端启动

```bash
cd ai-Agent-frontend
npm install
npm run dev
```

> 前端启动在 `http://localhost:5173`，已配置代理转发 `/api` 到后端

---

## 项目结构

```
ai-Agent/
├── src/main/java/com/ran/aiagent/
│   ├── AiAgentApplication.java          # 启动类
│   ├── controller/
│   │   └── AiController.java            # 对外接口（SSE 流式接口）
│   ├── agent/
│   │   ├── BaseAgent.java               # Agent 基类：状态机 + 执行循环 + SSE
│   │   ├── ReActAgnet.java              # ReAct 模板：step = think + act
│   │   ├── ToolCallAgent.java           # 工具调用核心逻辑
│   │   ├── NewsManus.java               # 新闻智能体（Prompt 配置）
│   │   └── model/AgentState.java        # Agent 状态枚举
│   ├── tools/
│   │   ├── PolicyNewsTool.java          # 时政新闻搜索工具（Tavily）
│   │   ├── TerminateTool.java           # 终止工具
│   │   └── ToolRegistration.java        # 工具注册配置类
│   └── advisor/
│       └── MyLoggerAdvisor.java         # 请求/响应日志拦截
│
├── src/main/resources/
│   └── application.yml                  
│
├── ai-Agent-frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── views/
│   │   │   ├── HomeView.vue             # 首页（欢迎 + 入口）
│   │   │   └── NewsView.vue             # 新闻对话页（SSE 流式聊天）
│   │   ├── components/
│   │   │   ├── ChatBubble.vue           # 聊天气泡（含 Markdown 渲染）
│   │   │   └── Typewriter.vue           # 打字机动效组件
│   │   ├── stores/chatStore.js          # Pinia 聊天状态管理
│   │   ├── services/sseService.js       # SSE 连接封装
│   │   └── router/                      # 前端路由配置
│   └── vite.config.js                   # Vite 配置（含 /api 代理）
│
└── pom.xml                              # Maven 依赖管理
```

---

## 环境配置

### application.yml 说明

```yaml
spring:
  ai:
    dashscope:
      api-key: sk-xxxxxx          # ⚠️ 阿里云通义千问 API Key
      chat:
        options:
          model: qwen-max          # 可替换为 qwen-plus / qwen-turbo

search-api:
  api-key: tvly-xxxxxx            # ⚠️ Tavily Search API Key

server:
  port: 8123
  servlet:
    context-path: /api
```

