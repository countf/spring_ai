# Spring AI 智能体（Agent）项目说明文档

## 一、项目概述

本项目基于 Spring AI 框架构建，实现了一个具备**多轮对话记忆**、**RAG（检索增强生成）**、**Function Calling（工具调用）** 能力的 AI 智能体助手。

核心功能包括：

- 带会话级记忆的多轮对话

- 基于 Redis 的文档存储与相似度检索（RAG）

- AI自动调用工具（查天气、计算器）

- 模拟嵌入向量生成（用于 RAG 测试）

- 前端可视化交互页面

---


## 二、技术栈

### 后端

- 核心框架：Spring Boot 4.0.6 + Spring AI 2.0.0-M6

- 向量存储：Redis（Jedis 客户端）

- AI 适配：OpenAI 兼容接口（支持第三方大模型）

- 开发语言：Java 26

### 前端

- 原生 HTML + JavaScript

- 简单的CSS样式与逐字输出动画

---

## 三、项目结构

```Plain Text
com.ai.spring_ai/
├── SpringAiApplication.java       // 项目启动类
├── config/
│   └── RedisConfig.java           // Redis 连接池配置
├── Service/
│   ├── MockEmbeddingModel.java    // 模拟 Embedding 向量生成
│   └── RagService.java            // RAG 核心服务（存储/检索）
├── controller/
│   └── AiChatController.java      // 对话/工具/RAG 接口
├── resources/
│   └── application.yaml           // 核心配置文件
├── agent.html                     // 前端交互页面
└── pom.xml                        // Maven 依赖配置
```

---

## 四、核心功能说明

### 1\. 模拟 Embedding 向量生成（MockEmbeddingModel）

- 实现 `EmbeddingModel` 接口，生成 1024 维随机向量（用于 RAG 测试）

- 提供批量（`call`）和单个（`embed`）向量生成方法

- 相同文本通过哈希种子生成固定向量，保证测试一致性

### 2\. RAG 检索增强生成（RagService）

**核心能力**

- 文档存储：将文本生成模拟向量后存入 Redis（Key 格式：`doc:UUID`）

- 混合排序检索：

    - 关键词匹配得分（权重 70%）：计算问题与文档的关键词交集占比

    - 模拟语义相似度（权重 30%）：基于文本哈希生成固定相似度值

    - 按总分排序返回 TopK 结果

**核心方法**

|方法名|功能|
|---|---|
|`addDocument\(String text\)`|生成向量并存储文本到 Redis|
|`search\(String question, int topK\)`|混合排序检索 TopK 相关文档|
|`calculateHybridScore\(\)`|混合得分计算（关键词 \+ 语义）|

### 3\. 多轮对话与工具调用（AiChatController）

**核心接口**

|接口路径|请求方式|参数|功能|
|---|---|---|---|
|`/chat`|GET|sessionId、message|带记忆的多轮对话（集成 RAG \+ 工具）|
|`/chat/clear`|GET|sessionId|清空指定会话的历史记录|
|`/add`|GET|text|新增文本到 Redis（RAG 数据源）|
|`/search`|GET|question|检索 RAG 相关文档（返回文本列表）|

**工具调用（Function Calling）**
内置 2 个可被 AI 自动调用的工具：

- `getWeather\(String city\)`：查询指定城市天气

- `add\(Integer a, Integer b\)`：执行加法计算

### 4\. 前端交互页面（agent\.html）

- 会话 ID 管理：不同 ID 对应独立的对话记忆

- 逐字输出动画：模拟 AI 思考回答的过程

- 加载状态提示：按钮禁用 \+ 转圈动画

- 清空历史功能：一键清空指定会话的所有记录

---

## 五、配置说明（application\.yaml）

```yaml
spring:
  ai:
    openai:
      api-key: xxxx       # 替换为第三方平台 API 密钥
      base-url: xxxx/v1   # 替换为第三方平台接口地址（需带 /v1）
      chat:
        model: mimo-v2.5  # 替换为第三方平台模型名
server:
  port: 8080  # 项目端口
```

---

## 六、部署与运行步骤

### 1\. 环境准备

- 安装 Redis 并启动（默认端口 6379，无需密码）

- 配置 JDK 26 环境

- 替换 `application\.yaml` 中的 OpenAI 兼容接口信息

### 2\. 项目启动

```bash
# 编译项目
mvn clean compile

# 启动项目
mvn spring-boot:run
```

### 3\. 功能测试

访问前端页面：直接打开 `agent\.html`

![Image](https://internal-api-drive-stream.larkoffice.com/space/api/box/stream/download/authcode/?code=ZGU2OTViZDViOGEwM2ZiZmY1YmFjMDU5NGFhOTg5ZjVfMWQ5NGZhM2JhYWMzNWIyNTg0Y2FlNDUzNjUwY2JmNmRfSUQ6NzY0NDQ1MTYzNzA4Mzg2ODM2NF8xNzc5ODYyNjA0OjE3Nzk5NDkwMDRfVjM)

**基础操作**

- 输入会话 ID（如 test1）和问题，点击「发送」

- 测试工具调用：输入「北京今天天气」「计算 10\+20」

- 测试 RAG：先调用 `/add?text=自定义文本` 存入数据，再提问相关问题

---

## 七、关键注意事项

- Redis 依赖：项目中注释了 Spring AI 官方 Redis 向量库，使用原生 Jedis 实现

- 向量维度：统一使用 1024 维，需与大模型 Embedding 输出维度一致

- 会话记忆：基于内存 `ConcurrentHashMap` 存储，重启后丢失

- 模拟向量：`MockEmbeddingModel` 仅用于测试，生产需替换真实 Embedding

- 跨域配置：`@CrossOrigin\(origins = \&\#34;\*\&\#34;\)`，生产需限制域名

---

## 八、扩展方向

- 替换真实 Embedding 服务（如 OpenAI Embedding）

- 会话记忆持久化到 Redis / 数据库

- 优化 RAG 检索算法（使用 Redis 向量检索插件）

- 增加更多工具调用能力（如数据库查询、接口调用）

- 前端页面美化与功能扩展

---

