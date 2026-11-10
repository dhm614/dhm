package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.ReportCreateDTO;
import com.example.petcommunity3.dto.ReportHandleDTO;
import com.example.petcommunity3.entity.ContentReport;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.PostComment;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.ContentReportMapper;
import com.example.petcommunity3.mapper.PostCommentMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.ContentReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ContentReportServiceImpl implements ContentReportService {
    // ContentReportServiceImpl 负责举报处理模块：用户举报帖子/评论，后台审核并处理目标内容。
    private final ContentReportMapper contentReportMapper;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final PostCommentMapper postCommentMapper;

    public ContentReportServiceImpl(ContentReportMapper contentReportMapper,
                                    UserMapper userMapper,
                                    PostMapper postMapper,
                                    PostCommentMapper postCommentMapper) {
        this.contentReportMapper = contentReportMapper;
        this.userMapper = userMapper;
        this.postMapper = postMapper;
        this.postCommentMapper = postCommentMapper;
    }

    @Override
    public void create(ReportCreateDTO dto) {
        // 创建举报：校验举报人、举报类型、目标内容，并防止重复举报。
        if (dto.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        if (!StringUtils.hasText(dto.getTargetType())) {
            throw new RuntimeException("举报类型不能为空");
        }

        if (dto.getTargetId() == null) {
            throw new RuntimeException("举报对象ID不能为空");
        }

        if (!StringUtils.hasText(dto.getReason())) {
            throw new RuntimeException("举报原因不能为空");
        }

        User user = userMapper.selectById(dto.getUserId());
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            throw new RuntimeException("用户不存在或已被禁用");
        }

        String targetType = dto.getTargetType().toUpperCase();

        if ("POST".equals(targetType)) {
            Post post = postMapper.selectById(dto.getTargetId());
            if (post == null || !Integer.valueOf(1).equals(post.getStatus())) {
                throw new RuntimeException("帖子不存在或已下架");
            }
        } else if ("COMMENT".equals(targetType)) {
            PostComment comment = postCommentMapper.selectById(dto.getTargetId());
            if (comment == null || !Integer.valueOf(1).equals(comment.getStatus())) {
                throw new RuntimeException("评论不存在或已删除");
            }
        } else {
            throw new RuntimeException("举报类型只能是POST或COMMENT");
        }

        ContentReport existReport = contentReportMapper.selectOne(
                new LambdaQueryWrapper<ContentReport>()
                        .eq(ContentReport::getTargetType, targetType)
                        .eq(ContentReport::getTargetId, dto.getTargetId())
                        .eq(ContentReport::getUserId, dto.getUserId())
                        .eq(ContentReport::getStatus, 0)
        );

        if (existReport != null) {
            throw new RuntimeException("请勿重复举报");
        }

        ContentReport report = new ContentReport();
        report.setTargetType(targetType);
        report.setTargetId(dto.getTargetId());
        report.setUserId(dto.getUserId());
        report.setReason(dto.getReason());
        report.setStatus(0);

        contentReportMapper.insert(report);
    }

    @Override
    public List<ContentReport> list(Integer status) {
        // 举报列表：后台可按处理状态筛选举报记录。
        LambdaQueryWrapper<ContentReport> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(ContentReport::getStatus, status);
        }

        wrapper.orderByDesc(ContentReport::getCreateTime);

        return contentReportMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void handle(Long id, ReportHandleDTO dto) {
        // 处理举报：更新举报状态；如果处理结果为成立，就处罚对应帖子或评论。
        if (id == null) {
            throw new RuntimeException("举报ID不能为空");
        }

        if (dto.getStatus() == null) {
            throw new RuntimeException("处理状态不能为空");
        }

        if (!Integer.valueOf(1).equals(dto.getStatus()) && !Integer.valueOf(2).equals(dto.getStatus())) {
            throw new RuntimeException("处理状态只能是1或2");
        }

        ContentReport report = contentReportMapper.selectById(id);
        if (report == null) {
            throw new RuntimeException("举报记录不存在");
        }

        if (!Integer.valueOf(0).equals(report.getStatus())) {
            throw new RuntimeException("该举报已处理");
        }

        if (Integer.valueOf(1).equals(dto.getStatus())) {
            punishTarget(report);
        }

        report.setStatus(dto.getStatus());
        report.setHandleResult(dto.getHandleResult());

        contentReportMapper.updateById(report);
    }

    private void punishTarget(ContentReport report) {
        // 处罚目标：举报帖子就下架帖子，举报评论就隐藏评论。
        if ("POST".equals(report.getTargetType())) {
            Post post = postMapper.selectById(report.getTargetId());
            if (post != null) {
                post.setStatus(3);
                postMapper.updateById(post);
            }
        }

        if ("COMMENT".equals(report.getTargetType())) {
            PostComment comment = postCommentMapper.selectById(report.getTargetId());
            if (comment != null) {
                comment.setStatus(0);
                postCommentMapper.updateById(comment);
            }
        }
    }
}
