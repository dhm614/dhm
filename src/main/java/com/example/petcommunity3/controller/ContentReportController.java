package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.ReportCreateDTO;
import com.example.petcommunity3.dto.ReportHandleDTO;
import com.example.petcommunity3.entity.ContentReport;
import com.example.petcommunity3.service.AdminService;
import com.example.petcommunity3.service.ContentReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ContentReportController {
    private static final String USER_ID_HEADER = "X-User-Id";

    // 内容举报接口入口：用户提交举报，管理员查看和处理举报。
    private final ContentReportService contentReportService;
    private final AdminService adminService;

    // 注入举报业务层，举报对象校验和重复举报校验都在 Service 中。
    public ContentReportController(ContentReportService contentReportService,
                                   AdminService adminService) {
        this.contentReportService = contentReportService;
        this.adminService = adminService;
    }

    // POST /api/reports
    // 创建举报记录，targetType 可以是 POST 或 COMMENT。
    @PostMapping
    public Result<Void> create(@RequestBody ReportCreateDTO dto) {
        contentReportService.create(dto);
        return Result.success(null);
    }

    // GET /api/reports?status=0
    // 查看举报列表；status 可选，不传表示查全部。
    @GetMapping
    public Result<List<ContentReport>> list(
            @RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
            @RequestParam(required = false) Integer status) {
        adminService.requireAdmin(operatorId);
        return Result.success(contentReportService.list(status));
    }

    // PUT /api/reports/{id}/handle
    // 管理员处理举报，处理成立时会下架帖子或隐藏评论。
    @PutMapping("/{id}/handle")
    public Result<Void> handle(
            @RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
            @PathVariable Long id,
            @RequestBody ReportHandleDTO dto) {
        adminService.requireAdmin(operatorId);
        contentReportService.handle(id, dto);
        return Result.success(null);
    }
}
