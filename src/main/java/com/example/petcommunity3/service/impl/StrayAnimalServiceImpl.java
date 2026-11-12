package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.StrayAnimalUpdateDTO;
import com.example.petcommunity3.entity.StrayAdoptionApplication;
import com.example.petcommunity3.entity.StrayAnimal;
import com.example.petcommunity3.mapper.StrayAdoptionApplicationMapper;
import com.example.petcommunity3.mapper.StrayAnimalMapper;
import com.example.petcommunity3.service.StrayAnimalService;
import com.example.petcommunity3.vo.StrayAnimalDetailVO;
import com.example.petcommunity3.vo.StrayAnimalDistanceVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StrayAnimalServiceImpl implements StrayAnimalService {
    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");

    private final StrayAnimalMapper animalMapper;
    private final StrayAdoptionApplicationMapper applicationMapper;

    public StrayAnimalServiceImpl(StrayAnimalMapper animalMapper,
                                  StrayAdoptionApplicationMapper applicationMapper) {
        this.animalMapper = animalMapper;
        this.applicationMapper = applicationMapper;
    }

    @Override
    public List<StrayAnimalDistanceVO> listNearby(BigDecimal longitude,
                                                  BigDecimal latitude,
                                                  Integer limit,
                                                  Long viewerId) {
        validateCoordinates(longitude, latitude);
        int size = limit == null ? 20 : Math.max(1, Math.min(limit, 100));

        List<StrayAnimal> animals = animalMapper.selectList(
                new LambdaQueryWrapper<StrayAnimal>()
                        .eq(StrayAnimal::getStatus, 1)
                        .in(StrayAnimal::getRescueStatus,
                                StrayAnimal.RESCUE_WAITING,
                                StrayAnimal.RESCUE_ADOPTION_PENDING)
                        .isNotNull(StrayAnimal::getLongitude)
                        .isNotNull(StrayAnimal::getLatitude)
        );
        Map<Long, String> phonesByAnimalId = new HashMap<>();
        animals.forEach(animal -> phonesByAnimalId.put(animal.getId(), animal.getProfessionalPhone()));
        List<StrayAnimalDistanceVO> result = animals
                .stream()
                .map(animal -> toDistanceVO(animal, longitude, latitude))
                .sorted(Comparator.comparing(StrayAnimalDistanceVO::getDistanceKm))
                .limit(size)
                .toList();

        if (viewerId != null && !result.isEmpty()) {
            List<Long> animalIds = result.stream()
                    .map(item -> item.getAnimal().getId())
                    .toList();
            Set<Long> authorizedIds = approvedAnimalIds(viewerId, animalIds);
            result.stream()
                    .filter(item -> authorizedIds.contains(item.getAnimal().getId()))
                    .forEach(item -> item.getAnimal().setProfessionalPhone(
                            phonesByAnimalId.get(item.getAnimal().getId())));
        }
        return result;
    }

    @Override
    public StrayAnimalDetailVO getDetail(Long id, Long viewerId) {
        StrayAnimal animal = id == null ? null : animalMapper.selectById(id);
        if (!isPublic(animal)) {
            throw new RuntimeException("流浪动物不存在");
        }
        boolean canViewPhone = viewerId != null
                && approvedAnimalIds(viewerId, List.of(id)).contains(id);
        return toDetailVO(animal, canViewPhone);
    }

    @Override
    public List<StrayAnimal> listForAdmin(Integer rescueStatus) {
        LambdaQueryWrapper<StrayAnimal> wrapper = new LambdaQueryWrapper<>();
        if (rescueStatus != null) {
            wrapper.eq(StrayAnimal::getRescueStatus, rescueStatus);
        }
        wrapper.orderByDesc(StrayAnimal::getCreateTime);
        return animalMapper.selectList(wrapper);
    }

    @Override
    public void updateForAdmin(Long id, StrayAnimalUpdateDTO dto) {
        StrayAnimal animal = id == null ? null : animalMapper.selectById(id);
        if (animal == null) {
            throw new RuntimeException("流浪动物不存在");
        }
        validateAdminUpdate(dto);

        animal.setHealthStatus(dto.getHealthStatus().trim());
        animal.setDangerLevel(dto.getDangerLevel());
        animal.setRescueStatus(dto.getRescueStatus());
        animal.setProfessionalName(dto.getProfessionalName().trim());
        animal.setProfessionalOrganization(dto.getProfessionalOrganization().trim());
        animal.setProfessionalPhone(dto.getProfessionalPhone().trim());
        animal.setCaptureInstruction(dto.getCaptureInstruction().trim());
        animal.setDewormingAdvice(dto.getDewormingAdvice().trim());
        animal.setVaccineAdvice(dto.getVaccineAdvice().trim());
        animal.setStatus(dto.getStatus());
        animal.setUpdateTime(LocalDateTime.now());
        animalMapper.updateById(animal);
    }

    private StrayAnimalDistanceVO toDistanceVO(StrayAnimal animal,
                                                BigDecimal longitude,
                                                BigDecimal latitude) {
        StrayAnimalDistanceVO vo = new StrayAnimalDistanceVO();
        vo.setAnimal(toDetailVO(animal, false));
        vo.setDistanceKm(distanceKm(longitude, latitude, animal.getLongitude(), animal.getLatitude()));
        return vo;
    }

    private StrayAnimalDetailVO toDetailVO(StrayAnimal animal, boolean includePhone) {
        StrayAnimalDetailVO vo = new StrayAnimalDetailVO();
        vo.setId(animal.getId());
        vo.setTemporaryName(animal.getTemporaryName());
        vo.setSpecies(animal.getSpecies());
        vo.setGender(animal.getGender());
        vo.setEstimatedAge(animal.getEstimatedAge());
        vo.setPhotoUrl(animal.getPhotoUrl());
        vo.setFoundAddress(animal.getFoundAddress());
        vo.setLongitude(animal.getLongitude());
        vo.setLatitude(animal.getLatitude());
        vo.setFoundTime(animal.getFoundTime());
        vo.setAppearanceDescription(animal.getAppearanceDescription());
        vo.setHealthStatus(animal.getHealthStatus());
        vo.setDangerLevel(animal.getDangerLevel());
        vo.setRescueStatus(animal.getRescueStatus());
        vo.setProfessionalName(animal.getProfessionalName());
        vo.setProfessionalOrganization(animal.getProfessionalOrganization());
        vo.setProfessionalPhone(includePhone ? animal.getProfessionalPhone() : null);
        vo.setCaptureInstruction(animal.getCaptureInstruction());
        vo.setDewormingAdvice(animal.getDewormingAdvice());
        vo.setVaccineAdvice(animal.getVaccineAdvice());
        return vo;
    }

    private Set<Long> approvedAnimalIds(Long viewerId, List<Long> animalIds) {
        return applicationMapper.selectList(
                        new LambdaQueryWrapper<StrayAdoptionApplication>()
                                .in(StrayAdoptionApplication::getAnimalId, animalIds)
                                .eq(StrayAdoptionApplication::getApplicantUserId, viewerId)
                                .eq(StrayAdoptionApplication::getStatus,
                                        StrayAdoptionApplication.STATUS_APPROVED)
                )
                .stream()
                .map(StrayAdoptionApplication::getAnimalId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean isPublic(StrayAnimal animal) {
        return animal != null
                && Integer.valueOf(1).equals(animal.getStatus())
                && (Integer.valueOf(StrayAnimal.RESCUE_WAITING).equals(animal.getRescueStatus())
                || Integer.valueOf(StrayAnimal.RESCUE_ADOPTION_PENDING).equals(animal.getRescueStatus()));
    }

    private void validateCoordinates(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null
                || latitude == null
                || longitude.compareTo(MIN_LONGITUDE) < 0
                || longitude.compareTo(MAX_LONGITUDE) > 0
                || latitude.compareTo(MIN_LATITUDE) < 0
                || latitude.compareTo(MAX_LATITUDE) > 0) {
            throw new RuntimeException("经纬度不合法");
        }
    }

    private void validateAdminUpdate(StrayAnimalUpdateDTO dto) {
        if (dto == null
                || dto.getRescueStatus() == null
                || dto.getRescueStatus() < StrayAnimal.RESCUE_WAITING
                || dto.getRescueStatus() > StrayAnimal.RESCUE_CLOSED) {
            throw new RuntimeException("救助状态不合法");
        }
        if (!StringUtils.hasText(dto.getHealthStatus())
                || !StringUtils.hasText(dto.getProfessionalName())
                || !StringUtils.hasText(dto.getProfessionalOrganization())
                || !StringUtils.hasText(dto.getProfessionalPhone())
                || !StringUtils.hasText(dto.getCaptureInstruction())
                || !StringUtils.hasText(dto.getDewormingAdvice())
                || !StringUtils.hasText(dto.getVaccineAdvice())) {
            throw new RuntimeException("请完整填写流浪动物专业协助信息");
        }
    }

    private double distanceKm(BigDecimal longitude1,
                              BigDecimal latitude1,
                              BigDecimal longitude2,
                              BigDecimal latitude2) {
        double lat1 = Math.toRadians(latitude1.doubleValue());
        double lat2 = Math.toRadians(latitude2.doubleValue());
        double deltaLat = Math.toRadians(latitude2.doubleValue() - latitude1.doubleValue());
        double deltaLng = Math.toRadians(longitude2.doubleValue() - longitude1.doubleValue());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double distance = EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(distance * 100.0) / 100.0;
    }
}
