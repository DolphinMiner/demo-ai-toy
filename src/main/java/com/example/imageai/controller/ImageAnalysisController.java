package com.example.imageai.controller;

import com.example.imageai.dto.ImageAnalysisRequest;
import com.example.imageai.dto.ImageAnalysisResponse;
import com.example.imageai.service.ImageAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/image")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ImageAnalysisController {

    private final ImageAnalysisService imageAnalysisService;

    /**
     * 分析图片并生成语音
     * @param request 包含图片URL的请求
     * @return 包含图片描述和语音数据的响应
     */
    @PostMapping(value = "/analyze", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImageAnalysisResponse> analyzeImage(@Valid @RequestBody ImageAnalysisRequest request) {
        try {
            long start = System.currentTimeMillis();
            log.info("开始分析图片: {}", request.getImageUrl());
            ImageAnalysisResponse response = imageAnalysisService.analyzeImageAndGenerateVoice(request.getImageUrl());
            log.info("图片分析完成，描述长度: {}", response.getDescription().length());
            long end = System.currentTimeMillis();
            System.out.println("---------【Time Cost】---------：: " + (end - start));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("图片分析失败: ", e);
            return ResponseEntity.internalServerError()
                    .body(ImageAnalysisResponse.builder()
                            .success(false)
                            .error("图片分析失败: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("服务运行正常");
    }
} 