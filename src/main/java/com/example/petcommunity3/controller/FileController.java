package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.service.FileStorageService;
import com.example.petcommunity3.service.MerchantPermissionService;
import com.example.petcommunity3.vo.UploadFileVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/files")
public class FileController {
    // 文件接口入口：接收前端上传的图片或其它文件，再交给文件存储 Service 处理。
    private final FileStorageService fileStorageService;
    private final MerchantPermissionService merchantPermissionService;

    public FileController(FileStorageService fileStorageService,
                          MerchantPermissionService merchantPermissionService) {
        // 构造方法注入文件存储服务，Controller 不关心文件最终存到本地、MinIO 还是 OSS。
        this.fileStorageService = fileStorageService;
        this.merchantPermissionService = merchantPermissionService;
    }

    // POST /api/files/upload：multipart/form-data 上传文件；module 用来区分头像、帖子、商品等业务目录。
    @PostMapping("/upload")
    public Result<UploadFileVO> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "common") String module,
                                       @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        String safeModule = normalizeModule(module);
        if ("product".equalsIgnoreCase(safeModule)) {
            merchantPermissionService.requireMerchantOrAdmin(userId);
        }
        if ("short-video".equalsIgnoreCase(safeModule)
                || "short-video-cover".equalsIgnoreCase(safeModule)) {
            merchantPermissionService.requireActiveUser(userId);
        }
        return Result.success(fileStorageService.upload(file, safeModule));
    }

    private String normalizeModule(String module) {
        String value = StringUtils.hasText(module) ? module.trim() : "common";
        value = value.replaceAll("[^a-zA-Z0-9_-]", "");
        return value.isBlank() ? "common" : value;
    }
}
