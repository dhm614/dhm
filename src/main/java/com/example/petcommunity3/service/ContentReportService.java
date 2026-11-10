package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.ReportCreateDTO;
import com.example.petcommunity3.dto.ReportHandleDTO;
import com.example.petcommunity3.entity.ContentReport;

import java.util.List;

public interface ContentReportService {
    // 创建举报：用户举报帖子或评论，生成待处理记录。
    void create(ReportCreateDTO dto);

    // 查询举报列表：status 可选，0 待处理、1 已处理、2 已驳回。
    List<ContentReport> list(Integer status);

    // 处理举报：管理员确认成立或驳回，成立时会处理对应帖子或评论。
    void handle(Long id, ReportHandleDTO dto);
}
