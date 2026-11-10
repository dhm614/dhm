package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.ApplicationReviewDTO;
import com.example.petcommunity3.dto.MerchantApplicationCreateDTO;
import com.example.petcommunity3.entity.MerchantApplication;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.MerchantApplicationMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.MerchantApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MerchantApplicationServiceImpl implements MerchantApplicationService {
    private static final int APPLICATION_PENDING = 0;
    private static final int APPLICATION_APPROVED = 1;
    private static final int APPLICATION_REJECTED = 2;
    private static final List<String> BUSINESS_TYPES = Arrays.asList("门店服务", "商品销售", "综合经营");
    private static final Set<String> ATTACHMENT_EXTENSIONS = Set.of(".doc", ".docx", ".pdf");

    private final UserMapper userMapper;
    private final MerchantApplicationMapper applicationMapper;

    public MerchantApplicationServiceImpl(UserMapper userMapper,
                                          MerchantApplicationMapper applicationMapper) {
        this.userMapper = userMapper;
        this.applicationMapper = applicationMapper;
    }

    @Override
    @Transactional
    public void submit(Long userId, MerchantApplicationCreateDTO dto) {
        User user = getEnabledUser(userId);
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("管理员不能申请商户资格");
        }
        if (Integer.valueOf(1).equals(user.getIsMerchant())) {
            throw new RuntimeException("当前用户已经是商户");
        }
        if (dto == null
                || !StringUtils.hasText(dto.getMerchantName())
                || !StringUtils.hasText(dto.getContactName())
                || !StringUtils.hasText(dto.getContactPhone())
                || !StringUtils.hasText(dto.getBusinessType())
                || !StringUtils.hasText(dto.getBusinessAddress())
                || !StringUtils.hasText(dto.getReason())) {
            throw new RuntimeException("请完整填写商户基本信息");
        }
        if (!BUSINESS_TYPES.contains(dto.getBusinessType().trim())) {
            throw new RuntimeException("经营类型不正确");
        }
        validateAttachment(dto);
        Long pendingCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<MerchantApplication>()
                        .eq(MerchantApplication::getUserId, userId)
                        .eq(MerchantApplication::getStatus, APPLICATION_PENDING)
        );
        if (pendingCount != null && pendingCount > 0) {
            throw new RuntimeException("已有待审核申请");
        }

        MerchantApplication application = new MerchantApplication();
        application.setUserId(userId);
        application.setMerchantName(dto.getMerchantName().trim());
        application.setContactName(dto.getContactName().trim());
        application.setContactPhone(dto.getContactPhone().trim());
        application.setBusinessType(dto.getBusinessType().trim());
        application.setBusinessAddress(dto.getBusinessAddress().trim());
        application.setReason(dto.getReason().trim());
        if (StringUtils.hasText(dto.getAttachmentName())) {
            application.setAttachmentName(dto.getAttachmentName().trim());
            application.setAttachmentUrl(dto.getAttachmentUrl().trim());
        }
        application.setStatus(APPLICATION_PENDING);
        applicationMapper.insert(application);

        user.setIsMerchant(0);
        user.setMerchantApplyStatus(1);
        userMapper.updateById(user);
    }

    @Override
    public MerchantApplication getMyLatest(Long userId) {
        getEnabledUser(userId);
        List<MerchantApplication> applications = applicationMapper.selectList(
                new LambdaQueryWrapper<MerchantApplication>()
                        .eq(MerchantApplication::getUserId, userId)
                        .orderByDesc(MerchantApplication::getCreateTime)
                        .last("LIMIT 1")
        );
        return applications.isEmpty() ? null : applications.get(0);
    }

    @Override
    public List<MerchantApplication> list(Integer status) {
        LambdaQueryWrapper<MerchantApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(MerchantApplication::getStatus, status);
        }
        wrapper.orderByDesc(MerchantApplication::getCreateTime);
        return applicationMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void review(Long applicationId, Long reviewerId, ApplicationReviewDTO dto) {
        if (applicationId == null) {
            throw new RuntimeException("申请ID不能为空");
        }
        if (reviewerId == null) {
            throw new RuntimeException("审核人ID不能为空");
        }
        if (dto == null || dto.getStatus() == null
                || (!Integer.valueOf(APPLICATION_APPROVED).equals(dto.getStatus())
                && !Integer.valueOf(APPLICATION_REJECTED).equals(dto.getStatus()))) {
            throw new RuntimeException("审核状态只能是1或2");
        }

        MerchantApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("商户申请不存在");
        }
        if (!Integer.valueOf(APPLICATION_PENDING).equals(application.getStatus())) {
            throw new RuntimeException("申请已处理");
        }

        User user = getEnabledUser(application.getUserId());
        application.setStatus(dto.getStatus());
        application.setReviewerId(reviewerId);
        application.setReviewRemark(dto.getRemark());
        application.setReviewTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        boolean approved = Integer.valueOf(APPLICATION_APPROVED).equals(dto.getStatus());
        user.setIsMerchant(approved ? 1 : 0);
        user.setMerchantApplyStatus(approved ? 2 : 3);
        userMapper.updateById(user);
    }

    private User getEnabledUser(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户身份不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            throw new RuntimeException("用户不存在或已被禁用");
        }
        return user;
    }

    private void validateAttachment(MerchantApplicationCreateDTO dto) {
        boolean hasName = StringUtils.hasText(dto.getAttachmentName());
        boolean hasUrl = StringUtils.hasText(dto.getAttachmentUrl());
        if (hasName != hasUrl) {
            throw new RuntimeException("请先上传申请附件");
        }
        if (!hasName) {
            return;
        }
        String fileName = dto.getAttachmentName().trim().toLowerCase(Locale.ROOT);
        boolean supported = ATTACHMENT_EXTENSIONS.stream().anyMatch(fileName::endsWith);
        if (!supported) {
            throw new RuntimeException("申请附件仅支持doc、docx或pdf格式");
        }
    }
}
