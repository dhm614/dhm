package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.petcommunity3.dto.StrayAdoptionCreateDTO;
import com.example.petcommunity3.dto.StrayAnimalReportReviewDTO;
import com.example.petcommunity3.entity.StrayAdoptionApplication;
import com.example.petcommunity3.entity.StrayAnimal;
import com.example.petcommunity3.entity.StrayAnimalReport;
import com.example.petcommunity3.mapper.StrayAdoptionApplicationMapper;
import com.example.petcommunity3.mapper.StrayAnimalMapper;
import com.example.petcommunity3.mapper.StrayAnimalReportMapper;
import com.example.petcommunity3.vo.StrayAnimalDetailVO;
import com.example.petcommunity3.vo.StrayAnimalDistanceVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class StrayAnimalModelTest {

    @Test
    void entitiesExposeStableStatusConstants() {
        assertThat(StrayAnimalReport.STATUS_PENDING).isZero();
        assertThat(StrayAnimalReport.STATUS_APPROVED).isEqualTo(1);
        assertThat(StrayAnimalReport.STATUS_REJECTED).isEqualTo(2);
        assertThat(StrayAnimal.RESCUE_WAITING).isZero();
        assertThat(StrayAnimal.RESCUE_ADOPTION_PENDING).isEqualTo(1);
        assertThat(StrayAnimal.RESCUE_ADOPTED).isEqualTo(2);
        assertThat(StrayAnimal.RESCUE_CLOSED).isEqualTo(3);
        assertThat(StrayAdoptionApplication.STATUS_PENDING).isZero();
        assertThat(StrayAdoptionApplication.STATUS_APPROVED).isEqualTo(1);
        assertThat(StrayAdoptionApplication.STATUS_REJECTED).isEqualTo(2);
        assertThat(StrayAdoptionApplication.STATUS_CANCELLED).isEqualTo(3);
    }

    @Test
    void dtosExposeRequiredWorkflowFields() throws NoSuchFieldException {
        assertFieldType(StrayAnimalReportReviewDTO.class, "healthStatus", String.class);
        assertFieldType(StrayAnimalReportReviewDTO.class, "dangerLevel", Integer.class);
        assertFieldType(StrayAdoptionCreateDTO.class, "animalId", Long.class);
    }

    @Test
    void vosExposePublicAnimalAndDistanceFields() throws NoSuchFieldException {
        assertFieldType(StrayAnimalDetailVO.class, "id", Long.class);
        assertFieldType(StrayAnimalDetailVO.class, "longitude", BigDecimal.class);
        assertFieldType(StrayAnimalDetailVO.class, "foundTime", LocalDateTime.class);
        assertFieldType(StrayAnimalDetailVO.class, "dangerLevel", Integer.class);
        assertFieldType(StrayAnimalDetailVO.class, "professionalPhone", String.class);
        assertFieldType(StrayAnimalDistanceVO.class, "animal", StrayAnimalDetailVO.class);
        assertFieldType(StrayAnimalDistanceVO.class, "distanceKm", Double.class);
    }

    @Test
    void mappersBindToTheirEntities() {
        assertMapperEntity(StrayAnimalReportMapper.class, StrayAnimalReport.class);
        assertMapperEntity(StrayAnimalMapper.class, StrayAnimal.class);
        assertMapperEntity(StrayAdoptionApplicationMapper.class, StrayAdoptionApplication.class);
    }

    private void assertFieldType(Class<?> owner, String fieldName, Class<?> fieldType)
            throws NoSuchFieldException {
        assertThat(owner.getDeclaredField(fieldName).getType()).isEqualTo(fieldType);
    }

    private void assertMapperEntity(Class<?> mapperType, Class<?> entityType) {
        ParameterizedType baseMapper = (ParameterizedType) mapperType.getGenericInterfaces()[0];
        assertThat(baseMapper.getRawType()).isEqualTo(BaseMapper.class);
        assertThat(baseMapper.getActualTypeArguments()).containsExactly(entityType);
    }
}
