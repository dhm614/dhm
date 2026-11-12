package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.petcommunity3.dto.StrayAnimalReportCreateDTO;
import com.example.petcommunity3.dto.StrayAnimalReportReviewDTO;
import com.example.petcommunity3.entity.StrayAnimal;
import com.example.petcommunity3.entity.StrayAnimalReport;
import com.example.petcommunity3.mapper.StrayAnimalMapper;
import com.example.petcommunity3.mapper.StrayAnimalReportMapper;
import com.example.petcommunity3.service.MerchantPermissionService;
import com.example.petcommunity3.service.StrayAnimalReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StrayAnimalReportServiceImpl implements StrayAnimalReportService {
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DUPLICATE_DISTANCE_KM = 0.2;
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");

    private final StrayAnimalReportMapper reportMapper;
    private final StrayAnimalMapper animalMapper;
    private final MerchantPermissionService permissionService;

    public StrayAnimalReportServiceImpl(StrayAnimalReportMapper reportMapper,
                                        StrayAnimalMapper animalMapper,
                                        MerchantPermissionService permissionService) {
        this.reportMapper = reportMapper;
        this.animalMapper = animalMapper;
        this.permissionService = permissionService;
    }

    @Override
    public void submit(Long userId, StrayAnimalReportCreateDTO dto) {
        permissionService.requireActiveUser(userId);
        validateSubmission(dto);

        String species = dto.getSpecies().trim();
        List<StrayAnimalReport> recentReports = reportMapper.selectList(
                new LambdaQueryWrapper<StrayAnimalReport>()
                        .eq(StrayAnimalReport::getReporterUserId, userId)
                        .eq(StrayAnimalReport::getSpecies, species)
                        .in(StrayAnimalReport::getStatus,
                                StrayAnimalReport.STATUS_PENDING,
                                StrayAnimalReport.STATUS_APPROVED)
                        .gt(StrayAnimalReport::getCreateTime, LocalDateTime.now().minusHours(24))
        );
        boolean duplicate = recentReports.stream()
                .filter(report -> report.getLongitude() != null && report.getLatitude() != null)
                .anyMatch(report -> distanceKm(
                        dto.getLongitude(),
                        dto.getLatitude(),
                        report.getLongitude(),
                        report.getLatitude()) <= DUPLICATE_DISTANCE_KM);
        if (duplicate) {
            throw new RuntimeException("24小时内已提交过附近同类动物");
        }

        StrayAnimalReport report = new StrayAnimalReport();
        report.setReporterUserId(userId);
        report.setTemporaryName(trimToNull(dto.getTemporaryName()));
        report.setSpecies(species);
        report.setGender(trimToNull(dto.getGender()));
        report.setEstimatedAge(dto.getEstimatedAge());
        report.setPhotoUrl(dto.getPhotoUrl().trim());
        report.setFoundAddress(dto.getFoundAddress().trim());
        report.setLongitude(dto.getLongitude());
        report.setLatitude(dto.getLatitude());
        report.setFoundTime(dto.getFoundTime());
        report.setAppearanceDescription(dto.getAppearanceDescription().trim());
        report.setHealthDescription(dto.getHealthDescription().trim());
        report.setEmergencyLevel(dto.getEmergencyLevel());
        report.setReporterPhone(dto.getReporterPhone().trim());
        report.setStatus(StrayAnimalReport.STATUS_PENDING);
        reportMapper.insert(report);
    }

    @Override
    public List<StrayAnimalReport> listMine(Long userId) {
        permissionService.requireActiveUser(userId);
        return reportMapper.selectList(
                new LambdaQueryWrapper<StrayAnimalReport>()
                        .eq(StrayAnimalReport::getReporterUserId, userId)
                        .orderByDesc(StrayAnimalReport::getCreateTime)
        );
    }

    @Override
    public List<StrayAnimalReport> listForAdmin(Integer status) {
        LambdaQueryWrapper<StrayAnimalReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(StrayAnimalReport::getStatus, status);
        }
        wrapper.orderByDesc(StrayAnimalReport::getCreateTime);
        return reportMapper.selectList(wrapper);
    }

    @Override
    public StrayAnimalReport getForAdmin(Long id) {
        StrayAnimalReport report = id == null ? null : reportMapper.selectById(id);
        if (report == null) {
            throw new RuntimeException("上报记录不存在");
        }
        return report;
    }

    @Override
    @Transactional
    public void review(Long reportId, Long reviewerId, StrayAnimalReportReviewDTO dto) {
        if (reviewerId == null) {
            throw new RuntimeException("审核人ID不能为空");
        }
        if (dto == null || dto.getStatus() == null
                || (!Integer.valueOf(StrayAnimalReport.STATUS_APPROVED).equals(dto.getStatus())
                && !Integer.valueOf(StrayAnimalReport.STATUS_REJECTED).equals(dto.getStatus()))) {
            throw new RuntimeException("审核状态只能是1或2");
        }

        StrayAnimalReport report = reportId == null ? null : reportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("上报记录不存在");
        }

        if (Integer.valueOf(StrayAnimalReport.STATUS_REJECTED).equals(dto.getStatus())) {
            if (!StringUtils.hasText(dto.getReviewRemark())) {
                throw new RuntimeException("请填写驳回原因");
            }
        } else {
            validateApproval(dto);
        }

        LocalDateTime reviewTime = LocalDateTime.now();
        int updated = reportMapper.update(
                null,
                new LambdaUpdateWrapper<StrayAnimalReport>()
                        .eq(StrayAnimalReport::getId, reportId)
                        .eq(StrayAnimalReport::getStatus, StrayAnimalReport.STATUS_PENDING)
                        .set(StrayAnimalReport::getStatus, dto.getStatus())
                        .set(StrayAnimalReport::getReviewerId, reviewerId)
                        .set(StrayAnimalReport::getReviewRemark, trimToNull(dto.getReviewRemark()))
                        .set(StrayAnimalReport::getReviewTime, reviewTime)
        );
        if (updated != 1) {
            throw new RuntimeException("上报记录已处理");
        }

        if (Integer.valueOf(StrayAnimalReport.STATUS_APPROVED).equals(dto.getStatus())) {
            StrayAnimal animal = new StrayAnimal();
            animal.setReportId(reportId);
            animal.setTemporaryName(report.getTemporaryName());
            animal.setSpecies(report.getSpecies());
            animal.setGender(report.getGender());
            animal.setEstimatedAge(report.getEstimatedAge());
            animal.setPhotoUrl(report.getPhotoUrl());
            animal.setFoundAddress(report.getFoundAddress());
            animal.setLongitude(report.getLongitude());
            animal.setLatitude(report.getLatitude());
            animal.setFoundTime(report.getFoundTime());
            animal.setAppearanceDescription(report.getAppearanceDescription());
            animal.setHealthStatus(dto.getHealthStatus().trim());
            animal.setDangerLevel(dto.getDangerLevel());
            animal.setRescueStatus(StrayAnimal.RESCUE_WAITING);
            animal.setProfessionalName(dto.getProfessionalName().trim());
            animal.setProfessionalOrganization(dto.getProfessionalOrganization().trim());
            animal.setProfessionalPhone(dto.getProfessionalPhone().trim());
            animal.setCaptureInstruction(dto.getCaptureInstruction().trim());
            animal.setDewormingAdvice(dto.getDewormingAdvice().trim());
            animal.setVaccineAdvice(dto.getVaccineAdvice().trim());
            animal.setStatus(1);
            animalMapper.insert(animal);
        }
    }

    private void validateSubmission(StrayAnimalReportCreateDTO dto) {
        if (dto == null
                || !StringUtils.hasText(dto.getSpecies())
                || !StringUtils.hasText(dto.getPhotoUrl())
                || !StringUtils.hasText(dto.getFoundAddress())
                || dto.getLongitude() == null
                || dto.getLatitude() == null
                || dto.getFoundTime() == null
                || !StringUtils.hasText(dto.getAppearanceDescription())
                || !StringUtils.hasText(dto.getHealthDescription())
                || !StringUtils.hasText(dto.getReporterPhone())) {
            throw new RuntimeException("请完整填写流浪动物上报信息");
        }
        if (dto.getLongitude().compareTo(MIN_LONGITUDE) < 0
                || dto.getLongitude().compareTo(MAX_LONGITUDE) > 0
                || dto.getLatitude().compareTo(MIN_LATITUDE) < 0
                || dto.getLatitude().compareTo(MAX_LATITUDE) > 0) {
            throw new RuntimeException("经纬度不合法");
        }
    }

    private void validateApproval(StrayAnimalReportReviewDTO dto) {
        if (!StringUtils.hasText(dto.getHealthStatus())
                || dto.getDangerLevel() == null
                || !StringUtils.hasText(dto.getProfessionalName())
                || !StringUtils.hasText(dto.getProfessionalOrganization())
                || !StringUtils.hasText(dto.getProfessionalPhone())
                || !StringUtils.hasText(dto.getCaptureInstruction())
                || !StringUtils.hasText(dto.getDewormingAdvice())
                || !StringUtils.hasText(dto.getVaccineAdvice())) {
            throw new RuntimeException("请完整填写审核发布信息");
        }
    }

    private double distanceKm(BigDecimal longitude1,
                              BigDecimal latitude1,
                              BigDecimal longitude2,
                              BigDecimal latitude2) {
        double latitudeDelta = Math.toRadians(latitude2.doubleValue() - latitude1.doubleValue());
        double longitudeDelta = Math.toRadians(longitude2.doubleValue() - longitude1.doubleValue());
        double latitude1Radians = Math.toRadians(latitude1.doubleValue());
        double latitude2Radians = Math.toRadians(latitude2.doubleValue());
        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(latitude1Radians) * Math.cos(latitude2Radians)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
