package com.example.imageai.service;

import com.example.imageai.dto.ImageAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageAnalysisService {

    private final QianWenService qianWenService;
    private final AliTtsService aliTtsService;
    private final ImageDownloadService imageDownloadService;

    /**
     * 分析图片并生成语音
     * @param imageUrl 图片URL
     * @return 分析结果和语音数据
     */
    public ImageAnalysisResponse analyzeImageAndGenerateVoice(String imageUrl) {
        try {
            // 1. 直接使用图片URL调用通义千问分析
            log.info("正在调用通义千问分析图片URL: {}", imageUrl);
            String description = qianWenService.analyzeImageWithUrl(imageUrl);
            
            // 2. 调用阿里TTS生成语音
            log.info("正在生成语音，文本长度: {}", description.length());
            byte[] audioBytes = aliTtsService.generateSpeech(description);
            
            // 3. 构建响应
            return ImageAnalysisResponse.builder()
                    .success(true)
                    .description(description)
                    .audioData(java.util.Base64.getEncoder().encodeToString(audioBytes))
                    .audioFormat("wav")
                    .build();
                    
        } catch (Exception e) {
            log.error("图片分析和语音生成失败: ", e);
            throw new RuntimeException("处理失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 分析图片并生成语音（备用方法 - 使用图片字节数组）
     * @param imageUrl 图片URL
     * @return 分析结果和语音数据
     */
    public ImageAnalysisResponse analyzeImageAndGenerateVoiceWithDownload(String imageUrl) {
        try {
            // 1. 下载图片
            log.info("正在下载图片: {}", imageUrl);
            byte[] imageBytes = imageDownloadService.downloadImage(imageUrl);
            
            // 2. 调用通义千问分析图片
            log.info("正在调用通义千问分析图片，图片大小: {} bytes", imageBytes.length);
            String description = qianWenService.analyzeImage(imageBytes);
            
            // 3. 调用阿里TTS生成语音
            log.info("正在生成语音，文本长度: {}", description.length());
            byte[] audioBytes = aliTtsService.generateSpeech(description);
            
            // 4. 构建响应
            return ImageAnalysisResponse.builder()
                    .success(true)
                    .description(description)
                    .audioData(java.util.Base64.getEncoder().encodeToString(audioBytes))
                    .audioFormat("wav")  // 更新为wav格式
                    .build();
                    
        } catch (Exception e) {
            log.error("图片分析和语音生成失败: ", e);
            throw new RuntimeException("处理失败: " + e.getMessage(), e);
        }
    }
} 