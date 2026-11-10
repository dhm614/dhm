package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.petcommunity3.dto.ShortVideoCreateDTO;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.service.FileStorageService;
import com.example.petcommunity3.service.MerchantPermissionService;
import com.example.petcommunity3.service.SensitiveWordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortVideoServiceTest {
    @Mock
    private PostMapper postMapper;

    @Mock
    private TopicMapper topicMapper;

    @Mock
    private SensitiveWordService sensitiveWordService;

    @TempDir
    Path tempDir;

    private ShortVideoServiceImpl service;
    private FileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() throws Exception {
        initializeTableInfo(PostMapper.class, Post.class);
        initializeTableInfo(TopicMapper.class, Topic.class);
        Files.createDirectories(tempDir.resolve("short-video"));
        Files.createDirectories(tempDir.resolve("short-video-cover"));
        Files.createDirectories(tempDir.resolve("post"));
        Files.write(tempDir.resolve("short-video/demo.mp4"), new byte[]{1});
        Files.write(tempDir.resolve("short-video-cover/demo.jpg"), new byte[]{1});
        Files.write(tempDir.resolve("short-video-cover/demo.txt"), new byte[]{1});
        Files.write(tempDir.resolve("post/demo.mp4"), new byte[]{1});
        Files.write(tempDir.resolve("post/demo.jpg"), new byte[]{1});
        fileStorageService = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(fileStorageService, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(fileStorageService, "urlPrefix", "/uploads");
        service = createService(activeUserPermission(), fileStorageService);
    }

    @Test
    void createVideoStoresPendingVideoPost() {
        ShortVideoCreateDTO dto = validDto();
        service.create(dto);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(postCaptor.capture());
        Post post = postCaptor.getValue();
        assertThat(post.getUserId()).isEqualTo(1L);
        assertThat(post.getTitle()).isEqualTo("猫咪追光");
        assertThat(post.getContent()).isEqualTo("记录猫咪追逐阳光的瞬间");
        assertThat(post.getCover()).isEqualTo("/uploads/short-video-cover/demo.jpg");
        assertThat(post.getContentType()).isEqualTo("VIDEO");
        assertThat(post.getVideoUrl()).isEqualTo("/uploads/short-video/demo.mp4");
        assertThat(post.getLikeCount()).isZero();
        assertThat(post.getCommentCount()).isZero();
        assertThat(post.getCollectCount()).isZero();
        assertThat(post.getIsPinned()).isZero();
        assertThat(post.getIsFeatured()).isZero();
        assertThat(post.getStatus()).isZero();
        verify(sensitiveWordService).checkPost(dto.getTitle(), dto.getContent());
    }

    @Test
    void createVideoRejectsMissingRequiredFieldsWithChineseErrors() {
        ShortVideoCreateDTO missingUserId = validDto();
        missingUserId.setUserId(null);
        assertThatThrownBy(() -> service.create(missingUserId)).hasMessage("用户ID不能为空");

        ShortVideoCreateDTO missingTitle = validDto();
        missingTitle.setTitle(" ");
        assertThatThrownBy(() -> service.create(missingTitle)).hasMessage("视频标题不能为空");

        ShortVideoCreateDTO missingContent = validDto();
        missingContent.setContent(null);
        assertThatThrownBy(() -> service.create(missingContent)).hasMessage("视频简介不能为空");

        ShortVideoCreateDTO missingCover = validDto();
        missingCover.setCover("");
        assertThatThrownBy(() -> service.create(missingCover)).hasMessage("封面URL不能为空");

        ShortVideoCreateDTO missingVideoUrl = validDto();
        missingVideoUrl.setVideoUrl(null);
        assertThatThrownBy(() -> service.create(missingVideoUrl)).hasMessage("视频URL不能为空");
    }

    @Test
    void createVideoRejectsNonMp4Url() {
        ShortVideoCreateDTO dto = validDto();
        dto.setVideoUrl("/uploads/short-video/demo.mov");

        assertThatThrownBy(() -> service.create(dto)).hasMessage("视频仅支持MP4格式");
        verify(postMapper, never()).insert(any(Post.class));
    }

    @Test
    void createVideoRejectsDisabledUserBeforeInsert() throws Exception {
        ShortVideoCreateDTO dto = validDto();
        dto.setUserId(8L);
        ShortVideoServiceImpl disabledUserService = createService(
                rejectingActiveUserPermission("user disabled"), fileStorageService);

        assertThatThrownBy(() -> disabledUserService.create(dto)).hasMessage("user disabled");

        verify(postMapper, never()).insert(any(Post.class));
    }

    @Test
    void createVideoRejectsExternalVideoUrl() {
        ShortVideoCreateDTO dto = validDto();
        dto.setVideoUrl("https://example.com/demo.mp4");

        assertThatThrownBy(() -> service.create(dto));

        verify(postMapper, never()).insert(any(Post.class));
    }

    @Test
    void createVideoRejectsWrongModuleUrls() {
        ShortVideoCreateDTO wrongVideoModule = validDto();
        wrongVideoModule.setVideoUrl("/uploads/post/demo.mp4");
        assertThatThrownBy(() -> service.create(wrongVideoModule));

        ShortVideoCreateDTO wrongCoverModule = validDto();
        wrongCoverModule.setCover("/uploads/post/demo.jpg");
        assertThatThrownBy(() -> service.create(wrongCoverModule));

        verify(postMapper, never()).insert(any(Post.class));
    }

    @Test
    void createVideoRejectsMissingManagedFileAndInvalidCoverExtension() {
        ShortVideoCreateDTO missingVideo = validDto();
        missingVideo.setVideoUrl("/uploads/short-video/missing.mp4");
        assertThatThrownBy(() -> service.create(missingVideo));

        ShortVideoCreateDTO invalidCover = validDto();
        invalidCover.setCover("/uploads/short-video-cover/demo.txt");
        assertThatThrownBy(() -> service.create(invalidCover));

        verify(postMapper, never()).insert(any(Post.class));
    }

    @Test
    void createVideoRejectsUrlOutsideConfiguredMinioModulePrefix() throws Exception {
        FileStorageServiceImpl minioStorage = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(minioStorage, "storageType", "minio");
        ReflectionTestUtils.setField(minioStorage, "minioEndpoint", "https://minio.example.com");
        ReflectionTestUtils.setField(minioStorage, "minioAccessKey", "access");
        ReflectionTestUtils.setField(minioStorage, "minioSecretKey", "secret");
        ReflectionTestUtils.setField(minioStorage, "minioBucket", "pet-community");
        ReflectionTestUtils.setField(minioStorage, "minioPublicUrl", "https://cdn.example.com/pet");
        ShortVideoServiceImpl minioService = createService(activeUserPermission(), minioStorage);
        ShortVideoCreateDTO dto = validDto();
        dto.setVideoUrl("https://cdn.example.com/pet/post/demo.mp4");
        dto.setCover("https://cdn.example.com/pet/short-video-cover/demo.jpg");

        assertThatThrownBy(() -> minioService.create(dto));

        dto.setVideoUrl(new String(new char[]{
                'h', 't', 't', 'p', 's', ':', '/', '/', 'c', 'd', 'n', '.', 'e', 'x', 'a', 'm', 'p', 'l', 'e', '.', 'c', 'o', 'm',
                '/', 'p', 'e', 't', '/', 's', 'h', 'o', 'r', 't', '-', 'v', 'i', 'd', 'e', 'o', '/', '%', '2', 'e', '%', '2', 'e',
                '/', 'p', 'o', 's', 't', '/', 'd', 'e', 'm', 'o', '.', 'm', 'p', '4'
        }));
        assertThatThrownBy(() -> minioService.create(dto));

        verify(postMapper, never()).insert(any(Post.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void createVideoChecksThatTopicIsApprovedWithoutUpdatingPostCount() {
        ShortVideoCreateDTO dto = validDto();
        dto.setTopic("萌宠日常");
        Topic topic = new Topic();
        topic.setName("萌宠日常");
        topic.setStatus(1);
        when(topicMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(topic);

        service.create(dto);

        ArgumentCaptor<LambdaQueryWrapper<Topic>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(topicMapper).selectOne(wrapperCaptor.capture());
        wrapperCaptor.getValue().getSqlSegment();
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs())
                .containsValue("萌宠日常")
                .containsValue(1);
        verify(topicMapper, never()).updateById(any(Topic.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listDefaultsToEightApprovedVideosOrderedByNewest() {
        Post video = new Post();
        when(postMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(video));

        List<Post> result = service.list(null);

        ArgumentCaptor<LambdaQueryWrapper<Post>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper).selectList(wrapperCaptor.capture());
        LambdaQueryWrapper<Post> wrapper = wrapperCaptor.getValue();
        assertThat(result).containsExactly(video);
        String sqlSegment = wrapper.getSqlSegment();
        assertThat(wrapper.getParamNameValuePairs())
                .containsValue(Post.CONTENT_TYPE_VIDEO)
                .containsValue(1);
        assertThat(sqlSegment)
                .contains("create_time")
                .contains("DESC")
                .contains("LIMIT 8");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listClampsLimitBetweenOneAndTwelve() {
        when(postMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.list(0);
        service.list(99);

        ArgumentCaptor<LambdaQueryWrapper<Post>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper, org.mockito.Mockito.times(2)).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getAllValues().get(0).getSqlSegment()).contains("LIMIT 1");
        assertThat(wrapperCaptor.getAllValues().get(1).getSqlSegment()).contains("LIMIT 12");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listByUserReturnsAllOwnedVideosWithoutStatusFilter() {
        when(postMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.listByUserId(6L);

        ArgumentCaptor<LambdaQueryWrapper<Post>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper).selectList(captor.capture());
        LambdaQueryWrapper<Post> wrapper = captor.getValue();
        String sql = wrapper.getSqlSegment();
        assertThat(wrapper.getParamNameValuePairs())
                .containsValue(6L)
                .containsValue(Post.CONTENT_TYPE_VIDEO)
                .doesNotContainValue(0)
                .doesNotContainValue(1)
                .doesNotContainValue(2)
                .doesNotContainValue(3);
        assertThat(sql).contains("create_time").contains("DESC");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void detailQueryRejectsPendingVideosAndNormalPosts() {
        when(postMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.getDetail(7L)).hasMessage("视频不存在");

        ArgumentCaptor<LambdaQueryWrapper<Post>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper).selectOne(wrapperCaptor.capture());
        wrapperCaptor.getValue().getSqlSegment();
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs())
                .containsValue(7L)
                .containsValue(Post.CONTENT_TYPE_VIDEO)
                .containsValue(1);
    }

    @Test
    void detailReturnsApprovedVideo() {
        Post video = new Post();
        video.setId(7L);
        video.setContentType(Post.CONTENT_TYPE_VIDEO);
        video.setStatus(1);
        when(postMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(video);

        assertThat(service.getDetail(7L)).isSameAs(video);
    }

    private ShortVideoCreateDTO validDto() {
        ShortVideoCreateDTO dto = new ShortVideoCreateDTO();
        dto.setUserId(1L);
        dto.setTitle("猫咪追光");
        dto.setContent("记录猫咪追逐阳光的瞬间");
        dto.setCover("/uploads/short-video-cover/demo.jpg");
        dto.setVideoUrl("/uploads/short-video/demo.mp4");
        return dto;
    }

    private MerchantPermissionService activeUserPermission() {
        return mock(MerchantPermissionService.class, invocation -> {
            if ("requireActiveUser".equals(invocation.getMethod().getName())) {
                User user = new User();
                user.setId(invocation.getArgument(0));
                user.setStatus(1);
                return user;
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
    }

    private MerchantPermissionService rejectingActiveUserPermission(String message) {
        return mock(MerchantPermissionService.class, invocation -> {
            if ("requireActiveUser".equals(invocation.getMethod().getName())) {
                throw new RuntimeException(message);
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
    }

    private ShortVideoServiceImpl createService(MerchantPermissionService permissionService,
                                                FileStorageService storageService) {
        return new ShortVideoServiceImpl(
                postMapper, topicMapper, sensitiveWordService, permissionService, storageService);
    }

    private void initializeTableInfo(Class<?> mapperClass, Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(mapperClass.getName());
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
