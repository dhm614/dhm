package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.petcommunity3.dto.StrayAdoptionCreateDTO;
import com.example.petcommunity3.dto.StrayAdoptionReviewDTO;
import com.example.petcommunity3.entity.StrayAdoptionApplication;
import com.example.petcommunity3.entity.StrayAnimal;
import com.example.petcommunity3.mapper.StrayAdoptionApplicationMapper;
import com.example.petcommunity3.mapper.StrayAnimalMapper;
import com.example.petcommunity3.service.MerchantPermissionService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StrayAdoptionFlowTest {

    @Test
    void submitCreatesPendingApplicationAndMovesAnimalToAdoptionPending() {
        TestContext context = contextWithAvailableAnimal();

        context.service.submit(7L, validCreate());

        verify(context.permissionService).requireActiveUser(7L);
        ArgumentCaptor<StrayAdoptionApplication> applicationCaptor =
                ArgumentCaptor.forClass(StrayAdoptionApplication.class);
        verify(context.applicationMapper).insert(applicationCaptor.capture());
        StrayAdoptionApplication application = applicationCaptor.getValue();
        assertThat(application.getAnimalId()).isEqualTo(11L);
        assertThat(application.getApplicantUserId()).isEqualTo(7L);
        assertThat(application.getContactPhone()).isEqualTo("13800000007");
        assertThat(application.getLivingCondition()).isEqualTo("自有住房，有封闭阳台");
        assertThat(application.getPetExperience()).isEqualTo("有三年养猫经验");
        assertThat(application.getApplicationReason()).isEqualTo("愿意长期照顾并定期反馈");
        assertThat(application.getStatus()).isEqualTo(StrayAdoptionApplication.STATUS_PENDING);

        ArgumentCaptor<LambdaUpdateWrapper<StrayAnimal>> animalUpdateCaptor = animalUpdateCaptor();
        verify(context.animalMapper).update(isNull(), animalUpdateCaptor.capture());
        assertThat(animalUpdateCaptor.getValue().getSqlSegment().toLowerCase())
                .contains("id =", "status =", "rescue_status");
        assertThat(animalUpdateCaptor.getValue().getParamNameValuePairs().values())
                .contains(11L, 1, StrayAnimal.RESCUE_WAITING,
                        StrayAnimal.RESCUE_ADOPTION_PENDING);
    }

    @Test
    void submitValidatesUserAndRequiredFields() {
        TestContext invalidUserContext = context();
        doThrow(new RuntimeException("用户不存在或已被禁用"))
                .when(invalidUserContext.permissionService).requireActiveUser(7L);

        assertThatThrownBy(() -> invalidUserContext.service.submit(7L, validCreate()))
                .hasMessage("用户不存在或已被禁用");

        TestContext missingFieldContext = context();
        StrayAdoptionCreateDTO dto = validCreate();
        dto.setContactPhone(" ");
        assertThatThrownBy(() -> missingFieldContext.service.submit(7L, dto))
                .hasMessage("请完整填写收留申请信息");
        verify(missingFieldContext.animalMapper, never()).selectOne(any());
    }

    @Test
    void submitRejectsDuplicatePendingOrApprovedApplication() {
        TestContext context = contextWithAvailableAnimal();
        when(context.applicationMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> context.service.submit(7L, validCreate()))
                .hasMessage("您已对该动物提交过有效收留申请");
        verify(context.applicationMapper, never()).insert(any(StrayAdoptionApplication.class));

        ArgumentCaptor<LambdaQueryWrapper<StrayAdoptionApplication>> captor = applicationQueryCaptor();
        verify(context.applicationMapper).selectCount(captor.capture());
        assertThat(captor.getValue().getSqlSegment().toLowerCase())
                .contains("animal_id", "applicant_user_id", "status");
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(11L, 7L, StrayAdoptionApplication.STATUS_PENDING,
                        StrayAdoptionApplication.STATUS_APPROVED);
    }

    @Test
    void submitRejectsMissingDisabledAdoptedOrClosedAnimal() {
        TestContext missingContext = context();
        assertThatThrownBy(() -> missingContext.service.submit(7L, validCreate()))
                .hasMessage("流浪动物不存在");

        StrayAnimal disabled = availableAnimal();
        disabled.setStatus(0);
        assertUnavailableOnSubmit(disabled);

        StrayAnimal adopted = availableAnimal();
        adopted.setRescueStatus(StrayAnimal.RESCUE_ADOPTED);
        assertUnavailableOnSubmit(adopted);

        StrayAnimal closed = availableAnimal();
        closed.setRescueStatus(StrayAnimal.RESCUE_CLOSED);
        assertUnavailableOnSubmit(closed);
    }

    @Test
    void submitLocksAnimalAndRejectsConcurrentStateChange() {
        TestContext context = contextWithAvailableAnimal();
        when(context.animalMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> context.service.submit(7L, validCreate()))
                .hasMessage("当前动物不可申请收留");

        ArgumentCaptor<LambdaQueryWrapper<StrayAnimal>> queryCaptor = animalQueryCaptor();
        verify(context.animalMapper).selectOne(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getSqlSegment().toLowerCase()).contains("for update");
    }

    @Test
    void listMineFiltersUserAndOrdersNewestFirst() {
        TestContext context = context();
        StrayAdoptionApplication application = pendingApplication(7L);
        when(context.applicationMapper.selectList(any())).thenReturn(List.of(application));

        assertThat(context.service.listMine(7L)).containsExactly(application);
        verify(context.permissionService).requireActiveUser(7L);
        ArgumentCaptor<LambdaQueryWrapper<StrayAdoptionApplication>> captor = applicationQueryCaptor();
        verify(context.applicationMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment().toLowerCase())
                .contains("applicant_user_id", "order by create_time desc");
        assertThat(captor.getValue().getParamNameValuePairs().values()).contains(7L);
    }

    @Test
    void listForAdminFiltersOptionalStatusAndOrdersNewestFirst() {
        TestContext filteredContext = context();
        StrayAdoptionApplication application = pendingApplication(7L);
        when(filteredContext.applicationMapper.selectList(any())).thenReturn(List.of(application));

        assertThat(filteredContext.service.listForAdmin(StrayAdoptionApplication.STATUS_PENDING))
                .containsExactly(application);

        ArgumentCaptor<LambdaQueryWrapper<StrayAdoptionApplication>> filteredCaptor = applicationQueryCaptor();
        verify(filteredContext.applicationMapper).selectList(filteredCaptor.capture());
        assertThat(filteredCaptor.getValue().getSqlSegment().toLowerCase())
                .contains("status", "order by create_time desc");
        assertThat(filteredCaptor.getValue().getParamNameValuePairs().values())
                .contains(StrayAdoptionApplication.STATUS_PENDING);

        TestContext unfilteredContext = context();
        when(unfilteredContext.applicationMapper.selectList(any())).thenReturn(List.of());
        unfilteredContext.service.listForAdmin(null);
        ArgumentCaptor<LambdaQueryWrapper<StrayAdoptionApplication>> unfilteredCaptor = applicationQueryCaptor();
        verify(unfilteredContext.applicationMapper).selectList(unfilteredCaptor.capture());
        assertThat(unfilteredCaptor.getValue().getSqlSegment().toLowerCase().trim())
                .isEqualTo("order by create_time desc");
    }

    @Test
    void ownerCanCancelPendingApplicationAndRestoreAnimalWhenNoActiveApplicationRemains() {
        TestContext context = context();
        when(context.applicationMapper.selectById(21L)).thenReturn(pendingApplication(7L));
        when(context.applicationMapper.selectCount(any())).thenReturn(0L);

        context.service.cancel(21L, 7L);

        ArgumentCaptor<LambdaUpdateWrapper<StrayAdoptionApplication>> applicationUpdateCaptor =
                applicationUpdateCaptor();
        verify(context.applicationMapper).update(isNull(), applicationUpdateCaptor.capture());
        assertPendingApplicationCondition(applicationUpdateCaptor.getValue(), 21L);
        assertThat(applicationUpdateCaptor.getValue().getParamNameValuePairs().values())
                .contains(StrayAdoptionApplication.STATUS_CANCELLED);
        assertAnimalLocked(context);
        assertAnimalRestored(context);
        assertActiveApplicationCountQuery(context, 11L);
    }

    @Test
    void cancelDoesNotRestoreAnimalWhenAnotherActiveApplicationRemains() {
        TestContext context = context();
        when(context.applicationMapper.selectById(21L)).thenReturn(pendingApplication(7L));
        when(context.applicationMapper.selectCount(any())).thenReturn(1L);

        context.service.cancel(21L, 7L);

        verify(context.animalMapper, never()).update(isNull(), any());
    }

    @Test
    void cancelRejectsNonOwnerProcessedOrConcurrentProcessing() {
        TestContext nonOwnerContext = context();
        when(nonOwnerContext.applicationMapper.selectById(21L)).thenReturn(pendingApplication(8L));
        assertThatThrownBy(() -> nonOwnerContext.service.cancel(21L, 7L))
                .hasMessage("无权取消该收留申请");

        TestContext processedContext = context();
        StrayAdoptionApplication approved = pendingApplication(7L);
        approved.setStatus(StrayAdoptionApplication.STATUS_APPROVED);
        when(processedContext.applicationMapper.selectById(21L)).thenReturn(approved);
        assertThatThrownBy(() -> processedContext.service.cancel(21L, 7L))
                .hasMessage("仅待审核申请可取消");

        TestContext concurrentContext = context();
        when(concurrentContext.applicationMapper.selectById(21L)).thenReturn(pendingApplication(7L));
        when(concurrentContext.applicationMapper.update(isNull(), any())).thenReturn(0);
        assertThatThrownBy(() -> concurrentContext.service.cancel(21L, 7L))
                .hasMessage("收留申请已处理");
    }

    @Test
    void rejectionRequiresRemarkAndRestoresAnimalWhenNoActiveApplicationRemains() {
        TestContext invalidContext = context();
        when(invalidContext.applicationMapper.selectById(21L)).thenReturn(pendingApplication(7L));
        StrayAdoptionReviewDTO invalid = review(StrayAdoptionApplication.STATUS_REJECTED, " ");
        assertThatThrownBy(() -> invalidContext.service.review(21L, 3L, invalid))
                .hasMessage("请填写驳回原因");
        verify(invalidContext.applicationMapper, never()).update(isNull(), any());

        TestContext context = context();
        when(context.applicationMapper.selectById(21L)).thenReturn(pendingApplication(7L));
        when(context.applicationMapper.selectCount(any())).thenReturn(0L);

        context.service.review(21L, 3L,
                review(StrayAdoptionApplication.STATUS_REJECTED, "居住条件不符合要求"));

        ArgumentCaptor<LambdaUpdateWrapper<StrayAdoptionApplication>> updateCaptor =
                applicationUpdateCaptor();
        verify(context.applicationMapper).update(isNull(), updateCaptor.capture());
        assertPendingApplicationCondition(updateCaptor.getValue(), 21L);
        assertThat(updateCaptor.getValue().getParamNameValuePairs().values())
                .contains(StrayAdoptionApplication.STATUS_REJECTED, 3L, "居住条件不符合要求")
                .anyMatch(LocalDateTime.class::isInstance);
        assertAnimalLocked(context);
        assertAnimalRestored(context);
        assertActiveApplicationCountQuery(context, 11L);
    }

    @Test
    void rejectionKeepsAnimalPendingWhenAnotherActiveApplicationRemains() {
        TestContext context = context();
        when(context.applicationMapper.selectById(21L)).thenReturn(pendingApplication(7L));
        when(context.applicationMapper.selectCount(any())).thenReturn(1L);

        context.service.review(21L, 3L,
                review(StrayAdoptionApplication.STATUS_REJECTED, "资料不足"));

        verify(context.animalMapper, never()).update(isNull(), any());
    }

    @Test
    void approvalRejectsWhenAnotherApprovedApplicationExists() {
        TestContext context = context();
        when(context.applicationMapper.selectById(21L)).thenReturn(pendingApplication(7L));
        when(context.animalMapper.selectOne(any())).thenReturn(availableAnimal());
        when(context.applicationMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> context.service.review(21L, 3L,
                review(StrayAdoptionApplication.STATUS_APPROVED, null)))
                .hasMessage("该动物已有通过的收留申请");

        verify(context.applicationMapper, never()).update(isNull(), any());
        ArgumentCaptor<LambdaQueryWrapper<StrayAdoptionApplication>> captor = applicationQueryCaptor();
        verify(context.applicationMapper).selectCount(captor.capture());
        assertThat(captor.getValue().getSqlSegment().toLowerCase())
                .contains("animal_id", "id", "status")
                .doesNotContain("applicant_user_id");
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(11L, 21L, StrayAdoptionApplication.STATUS_APPROVED);
    }

    @Test
    void approvalUpdatesPendingApplicationAndKeepsAnimalInAdoptionPending() {
        TestContext context = context();
        when(context.applicationMapper.selectById(21L)).thenReturn(pendingApplication(7L));
        when(context.animalMapper.selectOne(any())).thenReturn(availableAnimal());
        when(context.applicationMapper.selectCount(any())).thenReturn(0L);

        context.service.review(21L, 3L,
                review(StrayAdoptionApplication.STATUS_APPROVED, "资料核验通过"));

        ArgumentCaptor<LambdaUpdateWrapper<StrayAdoptionApplication>> applicationUpdateCaptor =
                applicationUpdateCaptor();
        verify(context.applicationMapper).update(isNull(), applicationUpdateCaptor.capture());
        assertPendingApplicationCondition(applicationUpdateCaptor.getValue(), 21L);
        assertThat(applicationUpdateCaptor.getValue().getParamNameValuePairs().values())
                .contains(StrayAdoptionApplication.STATUS_APPROVED, 3L, "资料核验通过");

        ArgumentCaptor<LambdaUpdateWrapper<StrayAnimal>> animalUpdateCaptor = animalUpdateCaptor();
        verify(context.animalMapper).update(isNull(), animalUpdateCaptor.capture());
        assertThat(animalUpdateCaptor.getValue().getSqlSegment().toLowerCase())
                .contains("id =", "status =", "rescue_status");
        assertThat(animalUpdateCaptor.getValue().getParamNameValuePairs().values())
                .contains(11L, 1, StrayAnimal.RESCUE_WAITING,
                        StrayAnimal.RESCUE_ADOPTION_PENDING);
    }

    @Test
    void reviewRejectsMissingProcessedOrConcurrentlyProcessedApplication() {
        TestContext missingContext = context();
        assertThatThrownBy(() -> missingContext.service.review(99L, 3L,
                review(StrayAdoptionApplication.STATUS_APPROVED, null)))
                .hasMessage("收留申请不存在");

        TestContext processedContext = context();
        StrayAdoptionApplication processed = pendingApplication(7L);
        processed.setStatus(StrayAdoptionApplication.STATUS_CANCELLED);
        when(processedContext.applicationMapper.selectById(21L)).thenReturn(processed);
        assertThatThrownBy(() -> processedContext.service.review(21L, 3L,
                review(StrayAdoptionApplication.STATUS_APPROVED, null)))
                .hasMessage("收留申请已处理");

        TestContext concurrentContext = context();
        when(concurrentContext.applicationMapper.selectById(21L)).thenReturn(pendingApplication(7L));
        when(concurrentContext.animalMapper.selectOne(any())).thenReturn(availableAnimal());
        when(concurrentContext.applicationMapper.selectCount(any())).thenReturn(0L);
        when(concurrentContext.applicationMapper.update(isNull(), any())).thenReturn(0);
        assertThatThrownBy(() -> concurrentContext.service.review(21L, 3L,
                review(StrayAdoptionApplication.STATUS_APPROVED, null)))
                .hasMessage("收留申请已处理");
    }

    @Test
    void reviewValidatesReviewerAndStatus() {
        TestContext context = context();
        assertThatThrownBy(() -> context.service.review(21L, null,
                review(StrayAdoptionApplication.STATUS_APPROVED, null)))
                .hasMessage("审核人ID不能为空");
        assertThatThrownBy(() -> context.service.review(21L, 3L, review(3, null)))
                .hasMessage("审核状态只能是1或2");
        verify(context.applicationMapper, never()).selectById(any());
    }

    @Test
    void stateChangingMethodsAreTransactional() throws NoSuchMethodException {
        assertThat(StrayAdoptionServiceImpl.class
                .getMethod("submit", Long.class, StrayAdoptionCreateDTO.class)
                .getAnnotation(Transactional.class)).isNotNull();
        assertThat(StrayAdoptionServiceImpl.class
                .getMethod("cancel", Long.class, Long.class)
                .getAnnotation(Transactional.class)).isNotNull();
        assertThat(StrayAdoptionServiceImpl.class
                .getMethod("review", Long.class, Long.class, StrayAdoptionReviewDTO.class)
                .getAnnotation(Transactional.class)).isNotNull();
    }

    private void assertUnavailableOnSubmit(StrayAnimal animal) {
        TestContext context = context();
        when(context.animalMapper.selectOne(any())).thenReturn(animal);

        assertThatThrownBy(() -> context.service.submit(7L, validCreate()))
                .hasMessage("当前动物不可申请收留");
        verify(context.applicationMapper, never()).insert(any(StrayAdoptionApplication.class));
    }

    private void assertPendingApplicationCondition(
            LambdaUpdateWrapper<StrayAdoptionApplication> wrapper,
            Long applicationId) {
        assertThat(wrapper.getSqlSegment().toLowerCase()).contains("id =", "status =");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains(applicationId, StrayAdoptionApplication.STATUS_PENDING);
    }

    private void assertActiveApplicationCountQuery(TestContext context, Long animalId) {
        ArgumentCaptor<LambdaQueryWrapper<StrayAdoptionApplication>> captor = applicationQueryCaptor();
        verify(context.applicationMapper).selectCount(captor.capture());
        assertThat(captor.getValue().getSqlSegment().toLowerCase())
                .contains("animal_id", "status in");
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(animalId,
                        StrayAdoptionApplication.STATUS_PENDING,
                        StrayAdoptionApplication.STATUS_APPROVED);
    }

    private void assertAnimalRestored(TestContext context) {
        ArgumentCaptor<LambdaUpdateWrapper<StrayAnimal>> animalUpdateCaptor = animalUpdateCaptor();
        verify(context.animalMapper).update(isNull(), animalUpdateCaptor.capture());
        assertThat(animalUpdateCaptor.getValue().getSqlSegment().toLowerCase())
                .contains("id =", "rescue_status =");
        assertThat(animalUpdateCaptor.getValue().getParamNameValuePairs().values())
                .contains(11L, StrayAnimal.RESCUE_ADOPTION_PENDING, StrayAnimal.RESCUE_WAITING);
    }

    private void assertAnimalLocked(TestContext context) {
        ArgumentCaptor<LambdaQueryWrapper<StrayAnimal>> queryCaptor = animalQueryCaptor();
        verify(context.animalMapper).selectOne(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getSqlSegment().toLowerCase()).contains("for update");
    }

    private TestContext contextWithAvailableAnimal() {
        TestContext context = context();
        when(context.animalMapper.selectOne(any())).thenReturn(availableAnimal());
        return context;
    }

    private TestContext context() {
        initializeTableInfo(StrayAdoptionApplicationMapper.class.getName(),
                StrayAdoptionApplication.class);
        initializeTableInfo(StrayAnimalMapper.class.getName(), StrayAnimal.class);
        StrayAdoptionApplicationMapper applicationMapper = mock(StrayAdoptionApplicationMapper.class);
        StrayAnimalMapper animalMapper = mock(StrayAnimalMapper.class);
        MerchantPermissionService permissionService = mock(MerchantPermissionService.class);
        when(applicationMapper.update(isNull(), any())).thenReturn(1);
        when(animalMapper.update(isNull(), any())).thenReturn(1);
        StrayAdoptionServiceImpl service = new StrayAdoptionServiceImpl(
                applicationMapper, animalMapper, permissionService);
        return new TestContext(service, applicationMapper, animalMapper, permissionService);
    }

    private void initializeTableInfo(String namespace, Class<?> entityType) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(namespace);
        TableInfoHelper.initTableInfo(assistant, entityType);
    }

    private StrayAdoptionCreateDTO validCreate() {
        StrayAdoptionCreateDTO dto = new StrayAdoptionCreateDTO();
        dto.setAnimalId(11L);
        dto.setContactPhone(" 13800000007 ");
        dto.setLivingCondition(" 自有住房，有封闭阳台 ");
        dto.setPetExperience(" 有三年养猫经验 ");
        dto.setApplicationReason(" 愿意长期照顾并定期反馈 ");
        return dto;
    }

    private StrayAnimal availableAnimal() {
        StrayAnimal animal = new StrayAnimal();
        animal.setId(11L);
        animal.setStatus(1);
        animal.setRescueStatus(StrayAnimal.RESCUE_WAITING);
        return animal;
    }

    private StrayAdoptionApplication pendingApplication(Long applicantUserId) {
        StrayAdoptionApplication application = new StrayAdoptionApplication();
        application.setId(21L);
        application.setAnimalId(11L);
        application.setApplicantUserId(applicantUserId);
        application.setContactPhone("13800000007");
        application.setLivingCondition("自有住房，有封闭阳台");
        application.setPetExperience("有三年养猫经验");
        application.setApplicationReason("愿意长期照顾并定期反馈");
        application.setStatus(StrayAdoptionApplication.STATUS_PENDING);
        return application;
    }

    private StrayAdoptionReviewDTO review(Integer status, String remark) {
        StrayAdoptionReviewDTO dto = new StrayAdoptionReviewDTO();
        dto.setStatus(status);
        dto.setReviewRemark(remark);
        return dto;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<LambdaQueryWrapper<StrayAdoptionApplication>> applicationQueryCaptor() {
        return ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<LambdaQueryWrapper<StrayAnimal>> animalQueryCaptor() {
        return ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<LambdaUpdateWrapper<StrayAdoptionApplication>> applicationUpdateCaptor() {
        return ArgumentCaptor.forClass((Class) LambdaUpdateWrapper.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<LambdaUpdateWrapper<StrayAnimal>> animalUpdateCaptor() {
        return ArgumentCaptor.forClass((Class) LambdaUpdateWrapper.class);
    }

    private record TestContext(StrayAdoptionServiceImpl service,
                               StrayAdoptionApplicationMapper applicationMapper,
                               StrayAnimalMapper animalMapper,
                               MerchantPermissionService permissionService) {
    }
}
