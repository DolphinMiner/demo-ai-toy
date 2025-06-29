package com.example.imageai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ImageAnalysisRequest {
    
    @NotBlank(message = "图片URL不能为空")
//    @Pattern(regexp = "^https?://.*\\.(jpg|jpeg|png|gif|bmp|webp)$",
//             message = "请提供有效的图片URL")
    private String imageUrl;
} 