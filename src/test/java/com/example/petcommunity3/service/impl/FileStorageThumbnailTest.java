package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.vo.UploadFileVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageThumbnailTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadImageReturnsThumbnailUrlAndCreatesThumbnailFile() throws Exception {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(service, "urlPrefix", "/uploads");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pet.png",
                "image/png",
                pngBytes()
        );

        UploadFileVO vo = service.upload(file, "post");

        assertThat(vo.getUrl()).startsWith("/uploads/post/");
        assertThat(vo.getThumbnailUrl()).startsWith("/uploads/post/thumb_");
        assertThat(Files.exists(tempDir.resolve("post").resolve(vo.getFileName()))).isTrue();
        assertThat(Files.exists(tempDir.resolve("post").resolve(vo.getThumbnailFileName()))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"doc", "docx", "pdf"})
    void uploadMerchantApplicationDocumentAcceptsSupportedExtensions(String extension) {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(service, "urlPrefix", "/uploads");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "merchant-application." + extension,
                "application/octet-stream",
                "test document".getBytes()
        );

        UploadFileVO vo = service.upload(file, "merchant-application");

        assertThat(vo.getOriginalName()).isEqualTo("merchant-application." + extension);
        assertThat(vo.getUrl()).startsWith("/uploads/merchant-application/");
        assertThat(vo.getThumbnailUrl()).isNull();
        assertThat(Files.exists(tempDir.resolve("merchant-application").resolve(vo.getFileName()))).isTrue();
    }

    @Test
    void uploadDocumentRejectsNonMerchantApplicationModule() {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "uploadPath", tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "business-plan.pdf",
                "application/pdf",
                "test document".getBytes()
        );

        assertThatThrownBy(() -> service.upload(file, "post"))
                .hasMessage("不支持的文件类型");
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 80, 60);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
