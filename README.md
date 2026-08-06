# WeCom Spring Boot Demo

这是一个 JDK 11 + Spring Boot + Vue 的企业微信自建应用示例，包含：

- 通过自建应用发送文本消息。
- 接收企业微信回调消息并完成 URL 校验、签名校验、AES 解密。
- 用 Vue 页面发送消息并显示已发送/已接收消息，形成简单对话页。

## 环境要求

- JDK 11
- Maven 3.6+
- 一个企业微信自建应用

## 企业微信后台配置

1. 企业微信管理后台 -> 应用管理 -> 自建 -> 创建应用。
2. 记录 `CorpID`、应用 `AgentId`、应用 `Secret`。
3. 设置应用可见范围。
4. 在应用的「接收消息」中配置：
   - URL: `https://你的域名/wecom/callback`
   - Token: 和 `WECOM_CALLBACK_TOKEN` 保持一致。
   - EncodingAESKey: 和 `WECOM_ENCODING_AES_KEY` 保持一致。
5. 在应用接口配置中设置可信 IP 为本服务公网出口 IP。

## 启动

PowerShell:

```powershell
$env:WECOM_CORP_ID="wwxxxxxxxxxxxxxxxx"
$env:WECOM_AGENT_ID="1000002"
$env:WECOM_APP_SECRET="你的应用Secret"
$env:WECOM_CALLBACK_TOKEN="你的回调Token"
$env:WECOM_ENCODING_AES_KEY="43位EncodingAESKey"

mvn spring-boot:run
```

启动后打开：

```text
http://localhost:8080/
```

## 后端接口

发送文本消息：

```bash
curl -X POST http://localhost:8080/wecom/messages/text \
  -H "Content-Type: application/json" \
  -d '{"touser":"zhangsan","content":"你好，这是一条来自自建应用的消息"}'
```

查询页面对话记录：

```text
GET /wecom/conversation/messages
```

清空页面对话记录：

```text
DELETE /wecom/conversation/messages
```

企业微信回调：

```text
GET  /wecom/callback
POST /wecom/callback
```

## 说明

当前对话记录保存在内存中，服务重启会清空。生产环境建议改成数据库，例如 MySQL、PostgreSQL 或 Redis。

前端 Vue 页面使用 CDN 引入 Vue 3，如果你的服务器不能访问公网，可以下载 Vue 文件后改成本地静态资源引用。
