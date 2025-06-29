package com.example.imageai.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageAnalysisResponse {
    
    /**
     * 请求是否成功
     */
    private boolean success;
    
    /**
     * 图片描述文本
     */
    private String description;
    
    /**
     * 语音数据（Base64编码）
     */
    private String audioData;
    
    /**
     * 语音文件类型
     */
    private String audioFormat;
    
    /**
     * 错误信息（如果有）
     */
    private String error;
} 