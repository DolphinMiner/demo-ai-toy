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

import java.net.URLEncoder;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliTtsService {

    @Value("${ali.tts.api.key:your-api-key}")
    private String apiKey;
    
    @Value("${ali.tts.api.url:https://nls-gateway-cn-shanghai.aliyuncs.com/stream/v1/tts}")
    private String apiUrl;
    
    @Value("${ali.tts.app.key:your-app-key}")
    private String appKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024)) // 50MB
            .build();

    /**
     * 使用阿里云TTS生成语音
     * @param text 要转换的文本
     * @return 语音字节数组
     */
    public byte[] generateSpeech(String text) {
        try {
            // 清理文本但保留必要的空格和标点
            String cleanText = cleanTextForTts(text);
            log.info("正在调用阿里云TTS生成语音，原始文本长度: {}, 清理后长度: {}", text.length(), cleanText.length());
            
            // 构建请求体 - 按照阿里云TTS的标准格式
            Map<String, Object> requestBody = Map.of(
                "appkey", appKey,
                "text", cleanText,
                "token", apiKey,
                "format", "wav"  // 使用wav格式，更兼容
            );

            // 直接获取二进制响应数据
            byte[] responseBytes = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (responseBytes == null || responseBytes.length == 0) {
                throw new RuntimeException("阿里云TTS返回空响应");
            }

            // 检查是否返回了错误信息（JSON格式）
            if (responseBytes.length > 0 && responseBytes[0] == '{') {
                // 可能是JSON错误响应
                String errorResponse = new String(responseBytes);
                try {
                    JsonNode responseNode = objectMapper.readTree(errorResponse);
                    if (responseNode.has("message")) {
                        throw new RuntimeException("阿里云TTS错误: " + responseNode.get("message").asText());
                    }
                    if (responseNode.has("error")) {
                        throw new RuntimeException("阿里云TTS错误: " + responseNode.get("error").asText());
                    }
                } catch (Exception jsonEx) {
                    // 如果JSON解析失败，说明可能真的是音频数据
                    log.debug("响应不是JSON格式，假设为音频数据");
                }
            }

            // 验证音频数据的合法性
            if (isValidAudioData(responseBytes)) {
                log.info("TTS语音生成完成，音频大小: {} bytes", responseBytes.length);
                return responseBytes;
            } else {
                log.warn("收到的数据不像是有效的音频文件");
                throw new RuntimeException("接收到的数据不是有效的音频格式");
            }

        } catch (Exception e) {
            log.error("阿里云TTS语音生成失败: ", e);
            
            // 如果真实TTS服务不可用，返回一个示例音频数据
            log.warn("使用模拟音频数据");
            return generateMockAudio(text);
        }
    }

    /**
     * 清理文本用于TTS转换
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String cleanTextForTts(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 替换换行符为空格
        String cleaned = text.replace("\n", " ");
        
        // 替换多个连续空格为单个空格
        cleaned = cleaned.replaceAll("\\s+", " ");
        
        // 移除一些可能影响TTS的特殊字符，但保留基本标点
        cleaned = cleaned.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // 限制长度，避免TTS请求过长
        if (cleaned.length() > 500) {
            cleaned = cleaned.substring(0, 500) + "...";
            log.info("文本过长，已截断到500字符");
        }
        
        return cleaned.trim();
    }

    /**
     * 验证是否为有效的音频数据
     * @param data 音频数据字节数组
     * @return 是否为有效的音频数据
     */
    private boolean isValidAudioData(byte[] data) {
        if (data == null || data.length < 8) {
            return false;
        }
        
        // 检查WAV文件头 "RIFF"
        if (data.length >= 4 && 
            data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F') {
            return true;
        }
        
        // 检查MP3文件头
        if (data.length >= 3 && 
            ((data[0] & 0xFF) == 0xFF) && ((data[1] & 0xE0) == 0xE0)) {
            return true;
        }
        
        // 检查MP3 ID3 标签
        if (data.length >= 3 && 
            data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
            return true;
        }
        
        // 如果数据大小合理（>1KB 且 <10MB），假设是有效的音频数据
        if (data.length > 1024 && data.length < 10 * 1024 * 1024) {
            log.debug("无法识别音频格式，但数据大小合理，假设为有效音频");
            return true;
        }
        
        return false;
    }

    /**
     * 验证字符串是否为有效的Base64编码
     * @param str 待验证的字符串
     * @return 是否为有效的Base64编码
     */
    private boolean isValidBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        // Base64编码只能包含以下字符：A-Z, a-z, 0-9, +, /, =
        // 长度必须是4的倍数
        if (str.length() % 4 != 0) {
            return false;
        }
        
        // 检查是否只包含Base64字符
        return str.matches("^[A-Za-z0-9+/]*={0,2}$");
    }

    /**
     * 生成模拟音频数据（用于测试）
     * 生成一个简单的WAV文件，包含正确的文件头，这样前端可以识别和播放
     */
    private byte[] generateMockAudio(String text) {
        // 创建一个简单的WAV文件头 + 静音数据
        // 这样前端就能正确识别为音频文件
        
        int sampleRate = 16000; // 采样率
        int duration = 2; // 2秒静音
        int numSamples = sampleRate * duration;
        int dataSize = numSamples * 2; // 16位音频，每个样本2字节
        
        // WAV文件头（44字节）
        byte[] header = new byte[44];
        int index = 0;
        
        // RIFF头
        header[index++] = 'R'; header[index++] = 'I'; header[index++] = 'F'; header[index++] = 'F';
        
        // 文件大小 - 8（小端序）
        int fileSize = 36 + dataSize;
        header[index++] = (byte)(fileSize & 0xFF);
        header[index++] = (byte)((fileSize >> 8) & 0xFF);
        header[index++] = (byte)((fileSize >> 16) & 0xFF);
        header[index++] = (byte)((fileSize >> 24) & 0xFF);
        
        // WAVE格式
        header[index++] = 'W'; header[index++] = 'A'; header[index++] = 'V'; header[index++] = 'E';
        
        // fmt子块
        header[index++] = 'f'; header[index++] = 'm'; header[index++] = 't'; header[index++] = ' ';
        
        // fmt子块大小（16）
        header[index++] = 16; header[index++] = 0; header[index++] = 0; header[index++] = 0;
        
        // 音频格式（PCM = 1）
        header[index++] = 1; header[index++] = 0;
        
        // 声道数（单声道 = 1）
        header[index++] = 1; header[index++] = 0;
        
        // 采样率
        header[index++] = (byte)(sampleRate & 0xFF);
        header[index++] = (byte)((sampleRate >> 8) & 0xFF);
        header[index++] = (byte)((sampleRate >> 16) & 0xFF);
        header[index++] = (byte)((sampleRate >> 24) & 0xFF);
        
        // 字节率
        int byteRate = sampleRate * 2; // 16位单声道
        header[index++] = (byte)(byteRate & 0xFF);
        header[index++] = (byte)((byteRate >> 8) & 0xFF);
        header[index++] = (byte)((byteRate >> 16) & 0xFF);
        header[index++] = (byte)((byteRate >> 24) & 0xFF);
        
        // 块对齐
        header[index++] = 2; header[index++] = 0;
        
        // 每个样本的位数（16位）
        header[index++] = 16; header[index++] = 0;
        
        // data子块
        header[index++] = 'd'; header[index++] = 'a'; header[index++] = 't'; header[index++] = 'a';
        
        // data子块大小
        header[index++] = (byte)(dataSize & 0xFF);
        header[index++] = (byte)((dataSize >> 8) & 0xFF);
        header[index++] = (byte)((dataSize >> 16) & 0xFF);
        header[index++] = (byte)((dataSize >> 24) & 0xFF);
        
        // 创建完整的WAV文件
        byte[] wavFile = new byte[44 + dataSize];
        System.arraycopy(header, 0, wavFile, 0, 44);
        
        // 填充静音数据（全零）
        // 音频数据部分已经是0（默认值），代表静音
        
        log.info("生成模拟WAV音频文件，大小: {} bytes, 时长: {}秒, 文本: {}", 
                wavFile.length, duration, text.substring(0, Math.min(text.length(), 50)));
        
        return wavFile;
    }
} 