package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.dto.TopicCreateDTO;
import com.example.petcommunity3.dto.TopicReviewDTO;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.AppointmentMapper;
import com.example.petcommunity3.mapper.PostCommentMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.ProductOrderMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopicAuditFlowTest {

    @Test
    void userCreatedTopicIsPendingAndIgnoresSubmittedSort() {
        TopicMapper topicMapper = mock(TopicMapper.class);
        TopicServiceImpl service = new TopicServiceImpl(topicMapper);

        TopicCreateDTO dto = new TopicCreateDTO();
        dto.setName("新手养猫");
        dto.setDescription("普通用户提交的话题");
        dto.setSort(99);

        when(topicMapper.selectOne(any())).thenReturn(null);
        when(topicMapper.insert(any(Topic.class))).thenReturn(1);

        service.create(dto);

        ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
        verify(topicMapper).insert(topicCaptor.capture());
        assertThat(topicCaptor.getValue().getStatus()).isEqualTo(0);
        assertThat(topicCaptor.getValue().getSort()).isEqualTo(0);
    }

    @Test
    void adminCreatedTopicIsApprovedWithSortValue() {
        TopicMapper topicMapper = mock(TopicMapper.class);
        AdminServiceImpl service = adminService(topicMapper);

        TopicCreateDTO dto = new TopicCreateDTO();
        dto.setName("狗狗训练");
        dto.setDescription("管理员创建的话题");
        dto.setSort(30);

        when(topicMapper.selectOne(any())).thenReturn(null);
        when(topicMapper.insert(any(Topic.class))).thenReturn(1);

        service.createTopic(dto);

        ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
        verify(topicMapper).insert(topicCaptor.capture());
        assertThat(topicCaptor.getValue().getStatus()).isEqualTo(1);
        assertThat(topicCaptor.getValue().getSort()).isEqualTo(30);
    }

    @Test
    void adminReviewCanApproveTopicAndSetSortValue() {
        TopicMapper topicMapper = mock(TopicMapper.class);
        AdminServiceImpl service = adminService(topicMapper);
        Topic topic = new Topic();
        topic.setId(5L);
        topic.setName("宠物医疗");
        topic.setStatus(0);
        topic.setSort(0);

        TopicReviewDTO dto = new TopicReviewDTO();
        dto.setStatus(1);
        dto.setSort(66);

        when(topicMapper.selectById(5L)).thenReturn(topic);

        service.reviewTopic(5L, dto);

        ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
        verify(topicMapper).updateById(topicCaptor.capture());
        assertThat(topicCaptor.getValue().getStatus()).isEqualTo(1);
        assertThat(topicCaptor.getValue().getSort()).isEqualTo(66);
    }

    @Test
    void adminReviewRequiresExplicitSortWhenApprovingTopic() {
        TopicMapper topicMapper = mock(TopicMapper.class);
        AdminServiceImpl service = adminService(topicMapper);

        TopicReviewDTO dto = new TopicReviewDTO();
        dto.setStatus(1);

        assertThatThrownBy(() -> service.reviewTopic(5L, dto))
                .hasMessage("通过话题时必须填写排序值");
    }

    @Test
    void adminDeleteTopicHidesIt() {
        TopicMapper topicMapper = mock(TopicMapper.class);
        AdminServiceImpl service = adminService(topicMapper);
        Topic topic = new Topic();
        topic.setId(6L);
        topic.setName("临时话题");
        topic.setStatus(1);

        when(topicMapper.selectById(6L)).thenReturn(topic);

        service.deleteTopic(6L);

        ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
        verify(topicMapper).updateById(topicCaptor.capture());
        assertThat(topicCaptor.getValue().getStatus()).isEqualTo(2);
    }

    @Test
    void adminGuardRejectsMissingUserId() {
        AdminServiceImpl service = adminService(mock(TopicMapper.class));

        assertThatThrownBy(() -> service.requireAdmin(null))
                .hasMessage("请先登录管理员账号");
    }

    @Test
    void adminGuardRejectsNormalUser() {
        UserMapper userMapper = mock(UserMapper.class);
        AdminServiceImpl service = new AdminServiceImpl(
                userMapper,
                mock(PostMapper.class),
                mock(PostCommentMapper.class),
                mock(AppointmentMapper.class),
                mock(ProductOrderMapper.class),
                mock(NotificationService.class),
                mock(TopicMapper.class),
                mock(com.example.petcommunity3.mapper.MerchantApplicationMapper.class)
        );
        User user = new User();
        user.setId(2L);
        user.setRole("USER");
        user.setStatus(1);

        when(userMapper.selectById(2L)).thenReturn(user);

        assertThatThrownBy(() -> service.requireAdmin(2L))
                .hasMessage("管理员权限不足");
    }

    @Test
    void adminGuardAllowsAdminUser() {
        UserMapper userMapper = mock(UserMapper.class);
        AdminServiceImpl service = new AdminServiceImpl(
                userMapper,
                mock(PostMapper.class),
                mock(PostCommentMapper.class),
                mock(AppointmentMapper.class),
                mock(ProductOrderMapper.class),
                mock(NotificationService.class),
                mock(TopicMapper.class),
                mock(com.example.petcommunity3.mapper.MerchantApplicationMapper.class)
        );
        User user = new User();
        user.setId(1L);
        user.setRole("ADMIN");
        user.setStatus(1);

        when(userMapper.selectById(1L)).thenReturn(user);

        service.requireAdmin(1L);

        verify(userMapper).selectById(1L);
    }

    private AdminServiceImpl adminService(TopicMapper topicMapper) {
        return new AdminServiceImpl(
                mock(UserMapper.class),
                mock(PostMapper.class),
                mock(PostCommentMapper.class),
                mock(AppointmentMapper.class),
                mock(ProductOrderMapper.class),
                mock(NotificationService.class),
                topicMapper,
                mock(com.example.petcommunity3.mapper.MerchantApplicationMapper.class)
        );
    }
}
