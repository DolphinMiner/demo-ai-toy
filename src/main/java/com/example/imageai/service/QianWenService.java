package com.example.imageai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QianWenService {

    @Value("${qianwen.api.key:your-api-key}")
    private String apiKey;

    @Value("${qianwen.api.url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024)) // 50MB
            .build();


    /**
     * 使用通义千问分析图片内容（直接使用图片URL）
     * @param imageUrl 图片URL
     * @return 图片描述文本
     */
    public String analyzeImageWithUrl(String imageUrl) {
        String content1 = "请详高度概括这张图片的内容，包括主要物体、场景、颜色、动作等细节,100字左右";
        String content2 = "请根据图片内容生成一个小故事,100字左右";
        try {
            // 构建请求体 - 使用OpenAI兼容格式
            Map<String, Object> requestBody = Map.of(
                "model", "qwen-vl-plus",
                "messages", java.util.List.of(
                    Map.of(
                        "role", "user",
                        "content", java.util.List.of(
                            Map.of(
                                "type", "image_url",
                                "image_url", Map.of("url", imageUrl)
                            ),
                            Map.of(
                                "type", "text",
                                "text", content2
                            )
                        )
                    )
                )
            );

            log.info("正在调用通义千问API分析图片URL: {}", imageUrl);
            
            String response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            // 解析响应 - OpenAI兼容格式
            JsonNode responseNode = objectMapper.readTree(response);
            
            if (responseNode.has("choices") && 
                responseNode.get("choices").isArray() &&
                responseNode.get("choices").size() > 0) {
                
                JsonNode firstChoice = responseNode.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    String content = firstChoice.get("message").get("content").asText();
                    log.info("通义千问URL分析完成，内容长度: {}", content.length());
                    return content;
                }
            }
            
            // 检查是否有错误信息
            if (responseNode.has("error")) {
                JsonNode error = responseNode.get("error");
                String errorMessage = error.has("message") ? error.get("message").asText() : "未知错误";
                throw new RuntimeException("通义千问API错误: " + errorMessage);
            }
            
            throw new RuntimeException("通义千问API响应格式异常: " + response);
            
        } catch (Exception e) {
            log.error("通义千问图片URL分析失败: {}", imageUrl, e);
            throw new RuntimeException("图片URL分析失败: " + e.getMessage(), e);
        }
    }


    /**
     * 使用通义千问分析图片内容
     *
     * @param imageBytes 图片字节数组
     * @return 图片描述文本
     */
    public String analyzeImage(byte[] imageBytes) {
        try {
            // 将图片转为Base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 构建请求体
            Map<String, Object> requestBody = Map.of(
                    "model", "qwen-vl-plus",
                    "input", Map.of(
                            "messages", java.util.List.of(
                                    Map.of(
                                            "role", "user",
                                            "content", java.util.List.of(
                                                    Map.of("image", "data:image/jpeg;base64," + base64Image),
                                                    Map.of("text", "请详细描述这张图片的内容，包括主要物体、场景、颜色、动作等细节,200字左右。")
                                            )
                                    )
                            )
                    ),
                    "parameters", Map.of(
                            "result_format", "message"
                    )
            );

            log.info("正在调用通义千问API分析图片");

            String response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            // 解析响应
            JsonNode responseNode = objectMapper.readTree(response);

            if (responseNode.has("output") &&
                    responseNode.get("output").has("choices") &&
                    responseNode.get("output").get("choices").isArray() &&
                    responseNode.get("output").get("choices").size() > 0) {

                JsonNode firstChoice = responseNode.get("output").get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    String content = firstChoice.get("message").get("content").asText();
                    log.info("通义千问分析完成，内容长度: {}", content.length());
                    return content;
                }
            }

            // 如果正常路径失败，尝试从错误信息中获取信息
            if (responseNode.has("message")) {
                throw new RuntimeException("通义千问API错误: " + responseNode.get("message").asText());
            }

            throw new RuntimeException("通义千问API响应格式异常: " + response);

        } catch (Exception e) {
            log.error("通义千问图片分析失败: ", e);
            throw new RuntimeException("图片分析失败: " + e.getMessage(), e);
        }
    }
} 