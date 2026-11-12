package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.petcommunity3.dto.StrayAdoptionCreateDTO;
import com.example.petcommunity3.dto.StrayAdoptionReviewDTO;
import com.example.petcommunity3.entity.StrayAdoptionApplication;
import com.example.petcommunity3.entity.StrayAnimal;
import com.example.petcommunity3.mapper.StrayAdoptionApplicationMapper;
import com.example.petcommunity3.mapper.StrayAnimalMapper;
import com.example.petcommunity3.service.MerchantPermissionService;
import com.example.petcommunity3.service.StrayAdoptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StrayAdoptionServiceImpl implements StrayAdoptionService {
    private final StrayAdoptionApplicationMapper applicationMapper;
    private final StrayAnimalMapper animalMapper;
    private final MerchantPermissionService permissionService;

    public StrayAdoptionServiceImpl(StrayAdoptionApplicationMapper applicationMapper,
                                    StrayAnimalMapper animalMapper,
                                    MerchantPermissionService permissionService) {
        this.applicationMapper = applicationMapper;
        this.animalMapper = animalMapper;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional
    public void submit(Long userId, StrayAdoptionCreateDTO dto) {
        permissionService.requireActiveUser(userId);
        validateSubmission(dto);

        StrayAnimal animal = lockAnimal(dto.getAnimalId());
        if (animal == null) {
            throw new RuntimeException("流浪动物不存在");
        }
        requireAvailableAnimal(animal);

        Long duplicateCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<StrayAdoptionApplication>()
                        .eq(StrayAdoptionApplication::getAnimalId, dto.getAnimalId())
                        .eq(StrayAdoptionApplication::getApplicantUserId, userId)
                        .in(StrayAdoptionApplication::getStatus,
                                StrayAdoptionApplication.STATUS_PENDING,
                                StrayAdoptionApplication.STATUS_APPROVED)
        );
        if (hasRows(duplicateCount)) {
            throw new RuntimeException("您已对该动物提交过有效收留申请");
        }

        StrayAdoptionApplication application = new StrayAdoptionApplication();
        application.setAnimalId(dto.getAnimalId());
        application.setApplicantUserId(userId);
        application.setContactPhone(dto.getContactPhone().trim());
        application.setLivingCondition(dto.getLivingCondition().trim());
        application.setPetExperience(trimToNull(dto.getPetExperience()));
        application.setApplicationReason(dto.getApplicationReason().trim());
        application.setStatus(StrayAdoptionApplication.STATUS_PENDING);
        applicationMapper.insert(application);

        if (Integer.valueOf(StrayAnimal.RESCUE_WAITING).equals(animal.getRescueStatus())) {
            int updated = animalMapper.update(
                    null,
                    new LambdaUpdateWrapper<StrayAnimal>()
                            .eq(StrayAnimal::getId, animal.getId())
                            .eq(StrayAnimal::getStatus, 1)
                            .eq(StrayAnimal::getRescueStatus, StrayAnimal.RESCUE_WAITING)
                            .set(StrayAnimal::getRescueStatus,
                                    StrayAnimal.RESCUE_ADOPTION_PENDING)
            );
            if (updated != 1) {
                throw new RuntimeException("当前动物不可申请收留");
            }
        }
    }

    @Override
    public List<StrayAdoptionApplication> listMine(Long userId) {
        permissionService.requireActiveUser(userId);
        return applicationMapper.selectList(
                new LambdaQueryWrapper<StrayAdoptionApplication>()
                        .eq(StrayAdoptionApplication::getApplicantUserId, userId)
                        .orderByDesc(StrayAdoptionApplication::getCreateTime)
        );
    }

    @Override
    @Transactional
    public void cancel(Long applicationId, Long userId) {
        permissionService.requireActiveUser(userId);
        StrayAdoptionApplication application = applicationId == null
                ? null
                : applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("收留申请不存在");
        }
        if (!userId.equals(application.getApplicantUserId())) {
            throw new RuntimeException("无权取消该收留申请");
        }
        if (!Integer.valueOf(StrayAdoptionApplication.STATUS_PENDING)
                .equals(application.getStatus())) {
            throw new RuntimeException("仅待审核申请可取消");
        }
        lockAnimal(application.getAnimalId());

        int updated = applicationMapper.update(
                null,
                new LambdaUpdateWrapper<StrayAdoptionApplication>()
                        .eq(StrayAdoptionApplication::getId, applicationId)
                        .eq(StrayAdoptionApplication::getApplicantUserId, userId)
                        .eq(StrayAdoptionApplication::getStatus,
                                StrayAdoptionApplication.STATUS_PENDING)
                        .set(StrayAdoptionApplication::getStatus,
                                StrayAdoptionApplication.STATUS_CANCELLED)
                        .set(StrayAdoptionApplication::getUpdateTime, LocalDateTime.now())
        );
        if (updated != 1) {
            throw new RuntimeException("收留申请已处理");
        }
        restoreAnimalIfNoActiveApplication(application.getAnimalId());
    }

    @Override
    public List<StrayAdoptionApplication> listForAdmin(Integer status) {
        LambdaQueryWrapper<StrayAdoptionApplication> wrapper =
                new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(StrayAdoptionApplication::getStatus, status);
        }
        wrapper.orderByDesc(StrayAdoptionApplication::getCreateTime);
        return applicationMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void review(Long applicationId,
                       Long reviewerId,
                       StrayAdoptionReviewDTO dto) {
        validateReview(reviewerId, dto);
        StrayAdoptionApplication application = applicationId == null
                ? null
                : applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("收留申请不存在");
        }
        if (!Integer.valueOf(StrayAdoptionApplication.STATUS_PENDING)
                .equals(application.getStatus())) {
            throw new RuntimeException("收留申请已处理");
        }

        boolean approved = Integer.valueOf(StrayAdoptionApplication.STATUS_APPROVED)
                .equals(dto.getStatus());
        if (approved) {
            validateApproval(application);
        } else if (!StringUtils.hasText(dto.getReviewRemark())) {
            throw new RuntimeException("请填写驳回原因");
        } else {
            lockAnimal(application.getAnimalId());
        }

        LocalDateTime reviewTime = LocalDateTime.now();
        int updated = applicationMapper.update(
                null,
                new LambdaUpdateWrapper<StrayAdoptionApplication>()
                        .eq(StrayAdoptionApplication::getId, applicationId)
                        .eq(StrayAdoptionApplication::getStatus,
                                StrayAdoptionApplication.STATUS_PENDING)
                        .set(StrayAdoptionApplication::getStatus, dto.getStatus())
                        .set(StrayAdoptionApplication::getReviewerId, reviewerId)
                        .set(StrayAdoptionApplication::getReviewRemark,
                                trimToNull(dto.getReviewRemark()))
                        .set(StrayAdoptionApplication::getReviewTime, reviewTime)
                        .set(StrayAdoptionApplication::getUpdateTime, reviewTime)
        );
        if (updated != 1) {
            throw new RuntimeException("收留申请已处理");
        }

        if (approved) {
            keepAnimalInAdoptionPending(application.getAnimalId());
        } else {
            restoreAnimalIfNoActiveApplication(application.getAnimalId());
        }
    }

    private void validateSubmission(StrayAdoptionCreateDTO dto) {
        if (dto == null
                || dto.getAnimalId() == null
                || !StringUtils.hasText(dto.getContactPhone())
                || !StringUtils.hasText(dto.getLivingCondition())
                || !StringUtils.hasText(dto.getApplicationReason())) {
            throw new RuntimeException("请完整填写收留申请信息");
        }
    }

    private void validateReview(Long reviewerId, StrayAdoptionReviewDTO dto) {
        if (reviewerId == null) {
            throw new RuntimeException("审核人ID不能为空");
        }
        if (dto == null || dto.getStatus() == null
                || (!Integer.valueOf(StrayAdoptionApplication.STATUS_APPROVED)
                .equals(dto.getStatus())
                && !Integer.valueOf(StrayAdoptionApplication.STATUS_REJECTED)
                .equals(dto.getStatus()))) {
            throw new RuntimeException("审核状态只能是1或2");
        }
    }

    private void validateApproval(StrayAdoptionApplication application) {
        StrayAnimal animal = lockAnimal(application.getAnimalId());
        if (animal == null) {
            throw new RuntimeException("流浪动物不存在");
        }
        requireAvailableAnimal(animal);

        Long approvedCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<StrayAdoptionApplication>()
                        .eq(StrayAdoptionApplication::getAnimalId,
                                application.getAnimalId())
                        .ne(StrayAdoptionApplication::getApplicantUserId,
                                application.getApplicantUserId())
                        .eq(StrayAdoptionApplication::getStatus,
                                StrayAdoptionApplication.STATUS_APPROVED)
        );
        if (hasRows(approvedCount)) {
            throw new RuntimeException("该动物已有通过的收留申请");
        }
    }

    private StrayAnimal lockAnimal(Long animalId) {
        return animalMapper.selectOne(
                new LambdaQueryWrapper<StrayAnimal>()
                        .eq(StrayAnimal::getId, animalId)
                        .last("FOR UPDATE")
        );
    }

    private void requireAvailableAnimal(StrayAnimal animal) {
        if (!Integer.valueOf(1).equals(animal.getStatus())
                || (!Integer.valueOf(StrayAnimal.RESCUE_WAITING)
                .equals(animal.getRescueStatus())
                && !Integer.valueOf(StrayAnimal.RESCUE_ADOPTION_PENDING)
                .equals(animal.getRescueStatus()))) {
            throw new RuntimeException("当前动物不可申请收留");
        }
    }

    private void keepAnimalInAdoptionPending(Long animalId) {
        animalMapper.update(
                null,
                new LambdaUpdateWrapper<StrayAnimal>()
                        .eq(StrayAnimal::getId, animalId)
                        .eq(StrayAnimal::getStatus, 1)
                        .in(StrayAnimal::getRescueStatus,
                                StrayAnimal.RESCUE_WAITING,
                                StrayAnimal.RESCUE_ADOPTION_PENDING)
                        .set(StrayAnimal::getRescueStatus,
                                StrayAnimal.RESCUE_ADOPTION_PENDING)
        );
    }

    private void restoreAnimalIfNoActiveApplication(Long animalId) {
        Long activeCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<StrayAdoptionApplication>()
                        .eq(StrayAdoptionApplication::getAnimalId, animalId)
                        .in(StrayAdoptionApplication::getStatus,
                                StrayAdoptionApplication.STATUS_PENDING,
                                StrayAdoptionApplication.STATUS_APPROVED)
        );
        if (!hasRows(activeCount)) {
            animalMapper.update(
                    null,
                    new LambdaUpdateWrapper<StrayAnimal>()
                            .eq(StrayAnimal::getId, animalId)
                            .eq(StrayAnimal::getRescueStatus,
                                    StrayAnimal.RESCUE_ADOPTION_PENDING)
                            .set(StrayAnimal::getRescueStatus,
                                    StrayAnimal.RESCUE_WAITING)
            );
        }
    }

    private boolean hasRows(Long count) {
        return count != null && count > 0;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
