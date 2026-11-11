package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.dto.StrayAnimalReportCreateDTO;
import com.example.petcommunity3.dto.StrayAnimalReportReviewDTO;
import com.example.petcommunity3.entity.StrayAnimal;
import com.example.petcommunity3.entity.StrayAnimalReport;
import com.example.petcommunity3.mapper.StrayAnimalMapper;
import com.example.petcommunity3.mapper.StrayAnimalReportMapper;
import com.example.petcommunity3.service.MerchantPermissionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StrayAnimalReportFlowTest {

    @Test
    void submitCreatesPendingReport() {
        TestContext context = context();
        when(context.reportMapper.selectList(any())).thenReturn(List.of());

        context.service.submit(7L, validReport());

        verify(context.permissionService).requireActiveUser(7L);
        ArgumentCaptor<StrayAnimalReport> captor = ArgumentCaptor.forClass(StrayAnimalReport.class);
        verify(context.reportMapper).insert(captor.capture());
        StrayAnimalReport report = captor.getValue();
        assertThat(report.getReporterUserId()).isEqualTo(7L);
        assertThat(report.getSpecies()).isEqualTo("猫");
        assertThat(report.getPhotoUrl()).isEqualTo("/uploads/stray-animal/cat.jpg");
        assertThat(report.getStatus()).isEqualTo(StrayAnimalReport.STATUS_PENDING);
    }

    @Test
    void submitRejectsInvalidUser() {
        TestContext context = context();
        doThrow(new RuntimeException("用户不存在或已被禁用"))
                .when(context.permissionService).requireActiveUser(7L);

        assertThatThrownBy(() -> context.service.submit(7L, validReport()))
                .hasMessage("用户不存在或已被禁用");
        verify(context.reportMapper, never()).insert(any(StrayAnimalReport.class));
    }

    @Test
    void submitRejectsMissingPhoto() {
        TestContext context = context();
        StrayAnimalReportCreateDTO dto = validReport();
        dto.setPhotoUrl(" ");

        assertThatThrownBy(() -> context.service.submit(7L, dto))
                .hasMessage("请完整填写流浪动物上报信息");
    }

    @Test
    void submitRejectsInvalidCoordinates() {
        TestContext context = context();
        StrayAnimalReportCreateDTO dto = validReport();
        dto.setLongitude(new BigDecimal("180.0001"));

        assertThatThrownBy(() -> context.service.submit(7L, dto))
                .hasMessage("经纬度不合法");
    }

    @Test
    void submitRejectsNearbySameSpeciesWithinTwentyFourHours() {
        TestContext context = context();
        StrayAnimalReport previous = new StrayAnimalReport();
        previous.setLongitude(new BigDecimal("118.82010"));
        previous.setLatitude(new BigDecimal("31.95310"));
        when(context.reportMapper.selectList(any())).thenReturn(List.of(previous));

        assertThatThrownBy(() -> context.service.submit(7L, validReport()))
                .hasMessage("24小时内已提交过附近同类动物");
        verify(context.reportMapper, never()).insert(any(StrayAnimalReport.class));
    }

    @Test
    void submitAllowsSameSpeciesFartherThanTwoHundredMeters() {
        TestContext context = context();
        StrayAnimalReport previous = new StrayAnimalReport();
        previous.setLongitude(new BigDecimal("118.83000"));
        previous.setLatitude(new BigDecimal("31.96000"));
        when(context.reportMapper.selectList(any())).thenReturn(List.of(previous));

        context.service.submit(7L, validReport());

        verify(context.reportMapper).insert(any(StrayAnimalReport.class));
    }

    @Test
    void listMethodsReturnMapperResults() {
        TestContext context = context();
        StrayAnimalReport report = new StrayAnimalReport();
        when(context.reportMapper.selectList(any())).thenReturn(List.of(report));

        assertThat(context.service.listMine(7L)).containsExactly(report);
        assertThat(context.service.listForAdmin(StrayAnimalReport.STATUS_PENDING)).containsExactly(report);
        verify(context.permissionService).requireActiveUser(7L);
    }

    @Test
    void getForAdminRejectsMissingReport() {
        TestContext context = context();

        assertThatThrownBy(() -> context.service.getForAdmin(99L))
                .hasMessage("上报记录不存在");
    }

    @Test
    void approvalCreatesPublicAnimalAndUpdatesReport() {
        TestContext context = context();
        StrayAnimalReport report = pendingReport();
        when(context.reportMapper.selectById(12L)).thenReturn(report);

        context.service.review(12L, 3L, approvedReview());

        ArgumentCaptor<StrayAnimal> captor = ArgumentCaptor.forClass(StrayAnimal.class);
        verify(context.animalMapper).insert(captor.capture());
        StrayAnimal animal = captor.getValue();
        assertThat(animal.getReportId()).isEqualTo(12L);
        assertThat(animal.getSpecies()).isEqualTo("猫");
        assertThat(animal.getPhotoUrl()).isEqualTo("/uploads/stray-animal/cat.jpg");
        assertThat(animal.getRescueStatus()).isEqualTo(StrayAnimal.RESCUE_WAITING);
        assertThat(animal.getStatus()).isEqualTo(1);
        assertThat(animal.getProfessionalPhone()).isEqualTo("025-86189999");
        assertThat(report.getStatus()).isEqualTo(StrayAnimalReport.STATUS_APPROVED);
        assertThat(report.getReviewerId()).isEqualTo(3L);
        assertThat(report.getReviewTime()).isNotNull();
        verify(context.reportMapper).updateById(report);
    }

    @Test
    void rejectionUpdatesReportWithoutPublishingAnimal() {
        TestContext context = context();
        StrayAnimalReport report = pendingReport();
        when(context.reportMapper.selectById(12L)).thenReturn(report);
        StrayAnimalReportReviewDTO dto = new StrayAnimalReportReviewDTO();
        dto.setStatus(StrayAnimalReport.STATUS_REJECTED);
        dto.setReviewRemark("照片无法确认位置");

        context.service.review(12L, 3L, dto);

        assertThat(report.getStatus()).isEqualTo(StrayAnimalReport.STATUS_REJECTED);
        assertThat(report.getReviewRemark()).isEqualTo("照片无法确认位置");
        verify(context.reportMapper).updateById(report);
        verify(context.animalMapper, never()).insert(any(StrayAnimal.class));
    }

    @Test
    void reviewRejectsMissingOrProcessedReport() {
        TestContext context = context();
        assertThatThrownBy(() -> context.service.review(99L, 3L, approvedReview()))
                .hasMessage("上报记录不存在");

        StrayAnimalReport report = pendingReport();
        report.setStatus(StrayAnimalReport.STATUS_APPROVED);
        when(context.reportMapper.selectById(12L)).thenReturn(report);
        assertThatThrownBy(() -> context.service.review(12L, 3L, approvedReview()))
                .hasMessage("上报记录已处理");
    }

    @Test
    void rejectionRequiresRemark() {
        TestContext context = context();
        when(context.reportMapper.selectById(12L)).thenReturn(pendingReport());
        StrayAnimalReportReviewDTO dto = new StrayAnimalReportReviewDTO();
        dto.setStatus(StrayAnimalReport.STATUS_REJECTED);

        assertThatThrownBy(() -> context.service.review(12L, 3L, dto))
                .hasMessage("请填写驳回原因");
    }

    @Test
    void approvalRequiresProfessionalPhone() {
        TestContext context = context();
        when(context.reportMapper.selectById(12L)).thenReturn(pendingReport());
        StrayAnimalReportReviewDTO dto = approvedReview();
        dto.setProfessionalPhone(" ");

        assertThatThrownBy(() -> context.service.review(12L, 3L, dto))
                .hasMessage("请完整填写审核发布信息");
    }

    @Test
    void reviewIsTransactional() throws NoSuchMethodException {
        Transactional annotation = StrayAnimalReportServiceImpl.class
                .getMethod("review", Long.class, Long.class, StrayAnimalReportReviewDTO.class)
                .getAnnotation(Transactional.class);

        assertThat(annotation).isNotNull();
    }

    private TestContext context() {
        StrayAnimalReportMapper reportMapper = mock(StrayAnimalReportMapper.class);
        StrayAnimalMapper animalMapper = mock(StrayAnimalMapper.class);
        MerchantPermissionService permissionService = mock(MerchantPermissionService.class);
        StrayAnimalReportServiceImpl service = new StrayAnimalReportServiceImpl(
                reportMapper,
                animalMapper,
                permissionService
        );
        return new TestContext(service, reportMapper, animalMapper, permissionService);
    }

    private StrayAnimalReportCreateDTO validReport() {
        StrayAnimalReportCreateDTO dto = new StrayAnimalReportCreateDTO();
        dto.setTemporaryName("小橘");
        dto.setSpecies("猫");
        dto.setGender("未知");
        dto.setEstimatedAge(2);
        dto.setPhotoUrl("/uploads/stray-animal/cat.jpg");
        dto.setFoundAddress("南京市江宁区弘景大道 99 号");
        dto.setLongitude(new BigDecimal("118.82000"));
        dto.setLatitude(new BigDecimal("31.95300"));
        dto.setFoundTime(LocalDateTime.of(2026, 11, 11, 9, 30));
        dto.setAppearanceDescription("橘色短毛，左耳有缺口");
        dto.setHealthDescription("精神一般，右前腿轻微跛行");
        dto.setEmergencyLevel(2);
        dto.setReporterPhone("13800000007");
        return dto;
    }

    private StrayAnimalReport pendingReport() {
        StrayAnimalReportCreateDTO dto = validReport();
        StrayAnimalReport report = new StrayAnimalReport();
        report.setId(12L);
        report.setReporterUserId(7L);
        report.setTemporaryName(dto.getTemporaryName());
        report.setSpecies(dto.getSpecies());
        report.setGender(dto.getGender());
        report.setEstimatedAge(dto.getEstimatedAge());
        report.setPhotoUrl(dto.getPhotoUrl());
        report.setFoundAddress(dto.getFoundAddress());
        report.setLongitude(dto.getLongitude());
        report.setLatitude(dto.getLatitude());
        report.setFoundTime(dto.getFoundTime());
        report.setAppearanceDescription(dto.getAppearanceDescription());
        report.setHealthDescription(dto.getHealthDescription());
        report.setEmergencyLevel(dto.getEmergencyLevel());
        report.setReporterPhone(dto.getReporterPhone());
        report.setStatus(StrayAnimalReport.STATUS_PENDING);
        return report;
    }

    private StrayAnimalReportReviewDTO approvedReview() {
        StrayAnimalReportReviewDTO dto = new StrayAnimalReportReviewDTO();
        dto.setStatus(StrayAnimalReport.STATUS_APPROVED);
        dto.setReviewRemark("信息核验通过");
        dto.setHealthStatus("轻微外伤，需观察");
        dto.setDangerLevel(1);
        dto.setProfessionalName("王医生");
        dto.setProfessionalOrganization("江宁宠物救助站");
        dto.setProfessionalPhone("025-86189999");
        dto.setCaptureInstruction("使用航空箱和食物诱导，避免徒手抓取");
        dto.setDewormingAdvice("隔离观察后进行体内外驱虫");
        dto.setVaccineAdvice("健康稳定后接种基础疫苗");
        return dto;
    }

    private record TestContext(StrayAnimalReportServiceImpl service,
                               StrayAnimalReportMapper reportMapper,
                               StrayAnimalMapper animalMapper,
                               MerchantPermissionService permissionService) {
    }
}
