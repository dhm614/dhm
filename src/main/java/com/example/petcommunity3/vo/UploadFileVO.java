package com.example.petcommunity3.vo;

import lombok.Data;

@Data
public class UploadFileVO {
    private String originalName;
    private String fileName;
    private String url;
    private String thumbnailFileName;
    private String thumbnailUrl;
    private Long size;
    private String contentType;
    private String module;
}
