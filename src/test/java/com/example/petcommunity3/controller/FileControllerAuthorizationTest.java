package com.example.petcommunity3.controller;

import com.example.petcommunity3.service.FileStorageService;
import com.example.petcommunity3.service.MerchantPermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class FileControllerAuthorizationTest {

    @Test
    void productUploadRequiresApprovedMerchantOrAdmin() {
        FileStorageService storageService = mock(FileStorageService.class);
        MerchantPermissionService permissionService = mock(MerchantPermissionService.class);
        FileController controller = new FileController(storageService, permissionService);
        MockMultipartFile file = new MockMultipartFile("file", "product.png", "image/png", new byte[]{1});
        doThrow(new RuntimeException("当前用户不是已审核商户"))
                .when(permissionService).requireMerchantOrAdmin(1L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.upload(file, "product", 1L))
                .hasMessage("当前用户不是已审核商户");

        verify(storageService, never()).upload(file, "product");
    }

    @Test
    void shortVideoModulesRejectMissingUserBeforeStorage() {
        FileStorageService storageService = mock(FileStorageService.class);
        MerchantPermissionService permissionService = rejectingActiveUserPermission("login required");
        FileController controller = new FileController(storageService, permissionService);
        MockMultipartFile video = new MockMultipartFile("file", "demo.mp4", "video/mp4", new byte[]{1});
        MockMultipartFile cover = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[]{1});

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.upload(video, "short-video", null))
                .hasMessage("login required");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.upload(cover, "short-video-cover", null))
                .hasMessage("login required");

        verify(storageService, never()).upload(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shortVideoModulesRejectDisabledUserBeforeStorage() {
        FileStorageService storageService = mock(FileStorageService.class);
        MerchantPermissionService permissionService = rejectingActiveUserPermission("user disabled");
        FileController controller = new FileController(storageService, permissionService);
        MockMultipartFile video = new MockMultipartFile("file", "demo.mp4", "video/mp4", new byte[]{1});
        MockMultipartFile cover = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[]{1});

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.upload(video, "short-video", 8L))
                .hasMessage("user disabled");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.upload(cover, "short-video-cover", 8L))
                .hasMessage("user disabled");

        verify(storageService, never()).upload(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void normalizedShortVideoModuleStillRequiresActiveUser() {
        FileStorageService storage = mock(FileStorageService.class);
        MerchantPermissionService permission = rejectingActiveUserPermission(null);
        FileController controller = new FileController(storage, permission);
        MockMultipartFile file = mock(MockMultipartFile.class);
        String module = new String(new char[]{' ', 's', 'h', 'o', 'r', 't', '-', 'v', 'i', 'd', 'e', 'o', '.', ' '});
        assertThatThrownBy(() -> controller.upload(file, module, null));
        verify(storage, never()).upload(any(), anyString());
    }

    private MerchantPermissionService rejectingActiveUserPermission(String message) {
        return mock(MerchantPermissionService.class, invocation -> {
            if ("requireActiveUser".equals(invocation.getMethod().getName())) {
                throw new RuntimeException(message);
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
    }
}
