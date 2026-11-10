package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.service.FileStorageService;
import com.example.petcommunity3.vo.UploadFileVO;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".mp4", ".mov", ".avi"
    );
    private static final Set<String> MERCHANT_APPLICATION_EXTENSIONS = Set.of(".doc", ".docx", ".pdf");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> THUMBNAIL_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");
    private static final long SHORT_VIDEO_MAX_SIZE = 100L * 1024 * 1024;
    private static final int THUMBNAIL_MAX_WIDTH = 240;
    private static final int THUMBNAIL_MAX_HEIGHT = 240;

    @Value("${petcommunity.upload.path:uploads}")
    private String uploadPath;

    @Value("${petcommunity.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Value("${petcommunity.storage.type:local}")
    private String storageType;

    @Value("${petcommunity.minio.endpoint:}")
    private String minioEndpoint;

    @Value("${petcommunity.minio.access-key:}")
    private String minioAccessKey;

    @Value("${petcommunity.minio.secret-key:}")
    private String minioSecretKey;

    @Value("${petcommunity.minio.bucket:pet-community}")
    private String minioBucket;

    @Value("${petcommunity.minio.public-url:}")
    private String minioPublicUrl;

    @Override
    public UploadFileVO upload(MultipartFile file, String module) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = getExtension(originalName);
        String safeModule = normalizeModule(module);
        boolean merchantApplicationDocument = "merchant-application".equals(safeModule)
                && MERCHANT_APPLICATION_EXTENSIONS.contains(extension);
        if (!ALLOWED_EXTENSIONS.contains(extension) && !merchantApplicationDocument) {
            throw new RuntimeException("不支持的文件类型");
        }
        validateModuleFile(file, safeModule, extension);

        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        byte[] fileBytes = readBytes(file);
        ThumbnailFile thumbnail = createThumbnail(fileBytes, fileName, extension);

        if (isMinioEnabled()) {
            return uploadToMinio(fileBytes, file, originalName, fileName, safeModule, thumbnail);
        }
        return uploadToLocal(fileBytes, file, originalName, fileName, safeModule, thumbnail);
    }

    @Override
    public void validateManagedUrl(String url, String module, Set<String> allowedExtensions) {
        if (!StringUtils.hasText(url)) {
            throw new RuntimeException("文件URL不能为空");
        }

        URI uri;
        try {
            uri = URI.create(url.trim()).normalize();
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("文件URL不合法");
        }
        if (uri.getQuery() != null || uri.getFragment() != null) {
            throw new RuntimeException("文件URL不合法");
        }

        String path = uri.getPath();
        if (!StringUtils.hasText(path)) {
            throw new RuntimeException("文件URL不合法");
        }
        String fileName = Paths.get(path).getFileName().toString();
        String extension = getExtension(fileName);
        if (allowedExtensions == null || !allowedExtensions.contains(extension)) {
            throw new RuntimeException("文件类型不符合模块要求");
        }

        String safeModule = normalizeModule(module);
        if (isMinioEnabled()) {
            String expectedPrefix = minioBaseUrl() + "/" + safeModule + "/";
            Path expectedPath = Paths.get(URI.create(expectedPrefix).getPath()).normalize();
            Path actualPath = Paths.get(path).normalize();
            if (!uri.isAbsolute()
                    || !uri.toString().startsWith(expectedPrefix)
                    || !actualPath.startsWith(expectedPath)
                    || actualPath.equals(expectedPath)) {
                throw new RuntimeException("文件URL不属于指定模块");
            }
            return;
        }

        if (uri.isAbsolute() || uri.getAuthority() != null) {
            throw new RuntimeException("文件URL不属于本地存储");
        }
        String expectedPrefix = trimTrailingSlash(normalizeUrlPrefix()) + "/" + safeModule + "/";
        if (!path.startsWith(expectedPrefix)) {
            throw new RuntimeException("文件URL不属于指定模块");
        }

        String relativePath = path.substring(expectedPrefix.length());
        Path moduleRoot = getUploadRoot().resolve(safeModule).normalize();
        Path targetFile = moduleRoot.resolve(relativePath).normalize();
        if (!StringUtils.hasText(relativePath)
                || !targetFile.startsWith(moduleRoot)
                || !Files.isRegularFile(targetFile)) {
            throw new RuntimeException("文件不存在或路径不合法");
        }
    }

    private UploadFileVO uploadToLocal(byte[] fileBytes,
                                       MultipartFile file,
                                       String originalName,
                                       String fileName,
                                       String safeModule,
                                       ThumbnailFile thumbnail) {
        Path targetDir = getUploadRoot().resolve(safeModule).normalize();
        Path targetFile = targetDir.resolve(fileName).normalize();
        if (!targetFile.startsWith(targetDir)) {
            throw new RuntimeException("文件路径不合法");
        }

        try {
            Files.createDirectories(targetDir);
            Files.write(targetFile, fileBytes);
            if (thumbnail != null) {
                Files.write(targetDir.resolve(thumbnail.fileName()).normalize(), thumbnail.bytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败");
        }

        String baseUrl = normalizeUrlPrefix() + "/" + safeModule + "/";
        return buildVO(originalName, fileName, safeModule, file, baseUrl + fileName,
                thumbnail == null ? null : thumbnail.fileName(),
                thumbnail == null ? null : baseUrl + thumbnail.fileName());
    }

    private UploadFileVO uploadToMinio(byte[] fileBytes,
                                       MultipartFile file,
                                       String originalName,
                                       String fileName,
                                       String safeModule,
                                       ThumbnailFile thumbnail) {
        String objectName = safeModule + "/" + fileName;
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(minioEndpoint.trim())
                    .credentials(minioAccessKey.trim(), minioSecretKey.trim())
                    .build();
            ensureBucket(minioClient);
            putObject(minioClient, objectName, fileBytes, file.getContentType());
            if (thumbnail != null) {
                putObject(minioClient, safeModule + "/" + thumbnail.fileName(), thumbnail.bytes(), "image/png");
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO文件上传失败");
        }

        String baseUrl = minioBaseUrl() + "/" + safeModule + "/";
        return buildVO(originalName, fileName, safeModule, file, baseUrl + fileName,
                thumbnail == null ? null : thumbnail.fileName(),
                thumbnail == null ? null : baseUrl + thumbnail.fileName());
    }

    private void ensureBucket(MinioClient minioClient) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioBucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
        }
    }

    private void putObject(MinioClient minioClient, String objectName, byte[] bytes, String contentType) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioBucket)
                .object(objectName)
                .stream(new ByteArrayInputStream(bytes), (long) bytes.length, -1L)
                .contentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream")
                .build());
    }

    private ThumbnailFile createThumbnail(byte[] sourceBytes, String fileName, String extension) {
        if (!THUMBNAIL_EXTENSIONS.contains(extension)) {
            return null;
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(sourceBytes));
            if (source == null) {
                return null;
            }
            double scale = Math.min(
                    (double) THUMBNAIL_MAX_WIDTH / source.getWidth(),
                    (double) THUMBNAIL_MAX_HEIGHT / source.getHeight()
            );
            scale = Math.min(scale, 1.0D);
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

            BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = target.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
            graphics.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(target, "png", output);
            return new ThumbnailFile("thumb_" + stripExtension(fileName) + ".png", output.toByteArray());
        } catch (IOException ex) {
            throw new RuntimeException("图片缩略图生成失败");
        }
    }

    private UploadFileVO buildVO(String originalName,
                                 String fileName,
                                 String safeModule,
                                 MultipartFile file,
                                 String url,
                                 String thumbnailFileName,
                                 String thumbnailUrl) {
        UploadFileVO vo = new UploadFileVO();
        vo.setOriginalName(originalName);
        vo.setFileName(fileName);
        vo.setModule(safeModule);
        vo.setSize(file.getSize());
        vo.setContentType(file.getContentType());
        vo.setUrl(url);
        vo.setThumbnailFileName(thumbnailFileName);
        vo.setThumbnailUrl(thumbnailUrl);
        return vo;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("读取上传文件失败");
        }
    }

    private void validateModuleFile(MultipartFile file, String module, String extension) {
        if ("short-video".equals(module)) {
            if (!".mp4".equals(extension)) {
                throw new RuntimeException("短视频仅支持MP4格式");
            }
            if (file.getSize() > SHORT_VIDEO_MAX_SIZE) {
                throw new RuntimeException("短视频不能超过100MB");
            }
        }
        if ("short-video-cover".equals(module) && !IMAGE_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("短视频封面必须是图片");
        }
    }

    private Path getUploadRoot() {
        Path path = Paths.get(uploadPath);
        if (!path.isAbsolute()) {
            path = Paths.get("").toAbsolutePath().resolve(path);
        }
        return path.normalize();
    }

    private String normalizeModule(String module) {
        String value = StringUtils.hasText(module) ? module.trim() : "common";
        value = value.replaceAll("[^a-zA-Z0-9_-]", "");
        return value.isBlank() ? "common" : value;
    }

    private String normalizeUrlPrefix() {
        String prefix = StringUtils.hasText(urlPrefix) ? urlPrefix.trim() : "/uploads";
        return prefix.startsWith("/") ? prefix : "/" + prefix;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean isMinioEnabled() {
        return "minio".equalsIgnoreCase(storageType)
                && StringUtils.hasText(minioEndpoint)
                && StringUtils.hasText(minioAccessKey)
                && StringUtils.hasText(minioSecretKey)
                && StringUtils.hasText(minioBucket);
    }

    private String minioBaseUrl() {
        String baseUrl = StringUtils.hasText(minioPublicUrl) ? minioPublicUrl.trim() : minioEndpoint.trim() + "/" + minioBucket;
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            throw new RuntimeException("文件后缀不能为空");
        }
        return fileName.substring(index).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? fileName : fileName.substring(0, index);
    }

    private record ThumbnailFile(String fileName, byte[] bytes) {
    }
}
