package com.example.petcommunity3.service;

import com.example.petcommunity3.vo.UploadFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public interface FileStorageService {
    // 上传文件：接收 Controller 传来的 MultipartFile，保存后返回访问地址等信息。
    UploadFileVO upload(MultipartFile file, String module);

    void validateManagedUrl(String url, String module, Set<String> allowedExtensions);
}
