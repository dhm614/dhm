package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.dto.ApplicationReviewDTO;
import com.example.petcommunity3.dto.MerchantApplicationCreateDTO;
import com.example.petcommunity3.entity.MerchantApplication;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.MerchantApplicationMapper;
import com.example.petcommunity3.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MerchantApplicationFlowTest {

    @Test
    void submitCreatesPendingApplicationAndUpdatesUserStatus() {
        UserMapper userMapper = mock(UserMapper.class);
        MerchantApplicationMapper applicationMapper = mock(MerchantApplicationMapper.class);
        MerchantApplicationServiceImpl service = new MerchantApplicationServiceImpl(userMapper, applicationMapper);
        User user = normalUser(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(applicationMapper.selectCount(any())).thenReturn(0L);

        MerchantApplicationCreateDTO dto = new MerchantApplicationCreateDTO();
        dto.setMerchantName("宠趣生活馆");
        dto.setContactName("张三");
        dto.setContactPhone("13800000001");
        dto.setBusinessType("综合经营");
        dto.setBusinessAddress("北京市海淀区宠物路 1 号");
        dto.setReason("经营宠物用品门店");
        dto.setAttachmentName("business-plan.docx");
        dto.setAttachmentUrl("/uploads/merchant-application/plan.docx");
        service.submit(1L, dto);

        ArgumentCaptor<MerchantApplication> applicationCaptor = ArgumentCaptor.forClass(MerchantApplication.class);
        verify(applicationMapper).insert(applicationCaptor.capture());
        assertThat(applicationCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(applicationCaptor.getValue().getMerchantName()).isEqualTo("宠趣生活馆");
        assertThat(applicationCaptor.getValue().getContactName()).isEqualTo("张三");
        assertThat(applicationCaptor.getValue().getContactPhone()).isEqualTo("13800000001");
        assertThat(applicationCaptor.getValue().getBusinessType()).isEqualTo("综合经营");
        assertThat(applicationCaptor.getValue().getBusinessAddress()).isEqualTo("北京市海淀区宠物路 1 号");
        assertThat(applicationCaptor.getValue().getReason()).isEqualTo("经营宠物用品门店");
        assertThat(applicationCaptor.getValue().getAttachmentName()).isEqualTo("business-plan.docx");
        assertThat(applicationCaptor.getValue().getAttachmentUrl()).isEqualTo("/uploads/merchant-application/plan.docx");
        assertThat(applicationCaptor.getValue().getStatus()).isEqualTo(0);
        assertThat(user.getMerchantApplyStatus()).isEqualTo(1);
        verify(userMapper).updateById(user);
    }

    @Test
    void submitRejectsDuplicatePendingApplication() {
        UserMapper userMapper = mock(UserMapper.class);
        MerchantApplicationMapper applicationMapper = mock(MerchantApplicationMapper.class);
        MerchantApplicationServiceImpl service = new MerchantApplicationServiceImpl(userMapper, applicationMapper);
        when(userMapper.selectById(1L)).thenReturn(normalUser(1L));
        when(applicationMapper.selectCount(any())).thenReturn(1L);

        MerchantApplicationCreateDTO dto = new MerchantApplicationCreateDTO();
        fillRequiredFields(dto);
        dto.setReason("重复申请");

        assertThatThrownBy(() -> service.submit(1L, dto))
                .hasMessage("已有待审核申请");
    }

    @Test
    void submitRejectsMissingBasicInformation() {
        UserMapper userMapper = mock(UserMapper.class);
        MerchantApplicationMapper applicationMapper = mock(MerchantApplicationMapper.class);
        MerchantApplicationServiceImpl service = new MerchantApplicationServiceImpl(userMapper, applicationMapper);
        when(userMapper.selectById(1L)).thenReturn(normalUser(1L));

        MerchantApplicationCreateDTO dto = new MerchantApplicationCreateDTO();
        dto.setReason("经营宠物用品门店");

        assertThatThrownBy(() -> service.submit(1L, dto))
                .hasMessage("请完整填写商户基本信息");
    }

    @Test
    void submitRejectsUnsupportedAttachmentExtension() {
        UserMapper userMapper = mock(UserMapper.class);
        MerchantApplicationMapper applicationMapper = mock(MerchantApplicationMapper.class);
        MerchantApplicationServiceImpl service = new MerchantApplicationServiceImpl(userMapper, applicationMapper);
        when(userMapper.selectById(1L)).thenReturn(normalUser(1L));

        MerchantApplicationCreateDTO dto = new MerchantApplicationCreateDTO();
        fillRequiredFields(dto);
        dto.setReason("经营宠物用品门店");
        dto.setAttachmentName("business-plan.txt");
        dto.setAttachmentUrl("/uploads/merchant-application/business-plan.txt");

        assertThatThrownBy(() -> service.submit(1L, dto))
                .hasMessage("申请附件仅支持doc、docx或pdf格式");
    }

    @Test
    void approvedReviewGrantsMerchantQualification() {
        UserMapper userMapper = mock(UserMapper.class);
        MerchantApplicationMapper applicationMapper = mock(MerchantApplicationMapper.class);
        MerchantApplicationServiceImpl service = new MerchantApplicationServiceImpl(userMapper, applicationMapper);
        MerchantApplication application = new MerchantApplication();
        application.setId(8L);
        application.setUserId(1L);
        application.setStatus(0);
        User user = normalUser(1L);
        when(applicationMapper.selectById(8L)).thenReturn(application);
        when(userMapper.selectById(1L)).thenReturn(user);

        ApplicationReviewDTO dto = new ApplicationReviewDTO();
        dto.setStatus(1);
        dto.setRemark("资料完整");
        service.review(8L, 3L, dto);

        assertThat(application.getStatus()).isEqualTo(1);
        assertThat(application.getReviewerId()).isEqualTo(3L);
        assertThat(user.getIsMerchant()).isEqualTo(1);
        assertThat(user.getMerchantApplyStatus()).isEqualTo(2);
        verify(applicationMapper).updateById(application);
        verify(userMapper).updateById(user);
    }

    private User normalUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole("USER");
        user.setStatus(1);
        user.setIsMerchant(0);
        user.setMerchantApplyStatus(0);
        return user;
    }

    private void fillRequiredFields(MerchantApplicationCreateDTO dto) {
        dto.setMerchantName("宠趣生活馆");
        dto.setContactName("张三");
        dto.setContactPhone("13800000001");
        dto.setBusinessType("综合经营");
        dto.setBusinessAddress("北京市海淀区宠物路 1 号");
    }
}
