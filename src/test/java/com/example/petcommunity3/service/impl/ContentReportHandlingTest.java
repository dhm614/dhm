package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.dto.ReportHandleDTO;
import com.example.petcommunity3.entity.ContentReport;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.mapper.ContentReportMapper;
import com.example.petcommunity3.mapper.PostCommentMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentReportHandlingTest {
    @Test
    void acceptedPostReportUsesAdministratorDisabledStatus() {
        ContentReportMapper reportMapper = mock(ContentReportMapper.class);
        PostMapper postMapper = mock(PostMapper.class);
        ContentReportServiceImpl service = createService(reportMapper, postMapper);
        ContentReport report = report(5L, 12L);
        Post post = new Post();
        post.setId(12L);
        post.setStatus(1);
        when(reportMapper.selectById(5L)).thenReturn(report);
        when(postMapper.selectById(12L)).thenReturn(post);

        service.handle(5L, handleDto(1, "举报成立"));

        assertThat(post.getStatus()).isEqualTo(3);
        verify(postMapper).updateById(post);
    }

    @Test
    void rejectedReportDoesNotChangeTargetStatus() {
        ContentReportMapper reportMapper = mock(ContentReportMapper.class);
        PostMapper postMapper = mock(PostMapper.class);
        ContentReportServiceImpl service = createService(reportMapper, postMapper);
        ContentReport report = report(5L, 12L);
        when(reportMapper.selectById(5L)).thenReturn(report);

        service.handle(5L, handleDto(2, "证据不足"));

        verify(postMapper, never()).selectById(12L);
        assertThat(report.getStatus()).isEqualTo(2);
        assertThat(report.getHandleResult()).isEqualTo("证据不足");
    }

    private ContentReportServiceImpl createService(ContentReportMapper reportMapper, PostMapper postMapper) {
        return new ContentReportServiceImpl(
                reportMapper,
                mock(UserMapper.class),
                postMapper,
                mock(PostCommentMapper.class)
        );
    }

    private ContentReport report(Long id, Long targetId) {
        ContentReport report = new ContentReport();
        report.setId(id);
        report.setTargetType("POST");
        report.setTargetId(targetId);
        report.setStatus(0);
        return report;
    }

    private ReportHandleDTO handleDto(Integer status, String result) {
        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus(status);
        dto.setHandleResult(result);
        return dto;
    }
}
