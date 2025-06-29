package com.example.imageai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;

import java.io.ByteArrayOutputStream;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageDownloadService {

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
            .build();

    /**
     * 下载图片
     * @param imageUrl 图片URL
     * @return 图片字节数组
     */
    public byte[] downloadImage(String imageUrl) {
        try {
            log.info("开始下载图片: {}", imageUrl);
            
            return webClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .timeout(Duration.ofSeconds(30))
                    .collectList()
                    .map(dataBuffers -> {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        dataBuffers.forEach(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            try {
                                outputStream.write(bytes);
                            } catch (Exception e) {
                                throw new RuntimeException("写入数据失败", e);
                            }
                        });
                        return outputStream.toByteArray();
                    })
                    .block();
                    
        } catch (Exception e) {
            log.error("下载图片失败: {}", imageUrl, e);
            throw new RuntimeException("下载图片失败: " + e.getMessage(), e);
        }
    }
} 