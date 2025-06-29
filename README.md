# 图片分析与语音生成服务

基于SpringBoot 3.2和Java 21开发的图片内容分析和语音生成服务。

## 功能特性

1. **图片内容分析**: 接收图片URL，使用通义千问大模型分析图片内容
2. **语音生成**: 将分析结果通过阿里云TTS服务转换为语音
3. **RESTful API**: 提供标准的REST接口供前端调用
4. **错误处理**: 完善的异常处理和日志记录

## 技术栈

- **Java 21**
- **SpringBoot 3.2**
- **Spring WebFlux** (用于HTTP客户端)
- **通义千问API** (图片分析)
- **阿里云TTS服务** (语音合成)

## 快速开始

### 环境要求

- Java 21+
- Maven 3.6+

### 配置API密钥

在运行前，您需要配置以下环境变量：

```bash
export QIANWEN_API_KEY=your-qianwen-api-key
export ALI_TTS_API_KEY=your-ali-tts-api-key  
export ALI_TTS_APP_KEY=your-ali-tts-app-key
```

### 编译和运行

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/image-voice-ai-1.0.0.jar
```

### API接口

#### 图片分析接口

**POST** `/api/v1/image/analyze`

请求体:
```json
{
  "imageUrl": "https://example.com/image.jpg"
}
```

响应:
```json
{
  "success": true,
  "description": "图片内容描述...",
  "audioData": "base64编码的音频数据",
  "audioFormat": "mp3"
}
```

#### 健康检查接口

**GET** `/api/v1/image/health`

## 配置说明

### application.yml 配置项

- `qianwen.api.key`: 通义千问API密钥
- `qianwen.api.url`: 通义千问API地址
- `ali.tts.api.key`: 阿里云TTS API密钥
- `ali.tts.app.key`: 阿里云TTS应用密钥
- `ali.tts.api.url`: 阿里云TTS API地址

## 部署说明

### 生产环境部署

1. 设置正确的环境变量
2. 使用production配置文件：`--spring.profiles.active=prod`
3. 确保服务器有足够的内存处理图片和音频数据

### Docker部署

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/image-voice-ai-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 注意事项

1. 图片URL必须是公开可访问的
2. 支持的图片格式：JPG, JPEG, PNG, GIF, BMP, WEBP
3. 单次请求图片大小限制：10MB
4. API密钥需要有相应的服务权限

## 错误码说明

- `400`: 请求参数错误
- `500`: 服务内部错误（通常是API调用失败）

## 联系方式

如有问题，请提交Issue或联系开发团队。