package com.example.petcommunity3.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShortVideoUploadTest {

    @TempDir
    Path tempDir;

    private FileStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(service, "urlPrefix", "/uploads");
    }

    @Test
    void shortVideoAcceptsMp4() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "demo.mp4", "video/mp4", new byte[]{1, 2, 3}
        );

        assertThat(service.upload(file, "short-video").getUrl())
                .startsWith("/uploads/short-video/");
    }

    @Test
    void shortVideoRejectsMov() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "demo.mov", "video/quicktime", new byte[]{1}
        );

        assertThatThrownBy(() -> service.upload(file, "short-video"))
                .hasMessage("短视频仅支持MP4格式");
    }

    @Test
    void shortVideoRejectsFileOver100MbBeforeReadingBytes() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("large.mp4");
        when(file.getSize()).thenReturn(104857601L);

        assertThatThrownBy(() -> service.upload(file, "short-video"))
                .hasMessage("短视频不能超过100MB");
        verify(file, never()).getBytes();
    }

    @Test
    void shortVideoCoverRejectsMp4() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.mp4", "video/mp4", new byte[]{1}
        );

        assertThatThrownBy(() -> service.upload(file, "short-video-cover"))
                .hasMessage("短视频封面必须是图片");
    }
}
