package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.petcommunity3.dto.StrayAnimalUpdateDTO;
import com.example.petcommunity3.entity.StrayAdoptionApplication;
import com.example.petcommunity3.entity.StrayAnimal;
import com.example.petcommunity3.mapper.StrayAdoptionApplicationMapper;
import com.example.petcommunity3.mapper.StrayAnimalMapper;
import com.example.petcommunity3.vo.StrayAnimalDetailVO;
import com.example.petcommunity3.vo.StrayAnimalDistanceVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StrayAnimalPublicQueryTest {

    @Test
    void listNearbySortsByDistanceAndHidesPhoneForVisitor() {
        TestContext context = context();
        StrayAnimal far = animal(2L, "118.92000");
        StrayAnimal near = animal(1L, "118.82100");
        when(context.animalMapper.selectList(any())).thenReturn(List.of(far, near));

        List<StrayAnimalDistanceVO> result = context.service.listNearby(
                coordinate("118.82000"), coordinate("31.95300"), null, null);

        assertThat(result).extracting(item -> item.getAnimal().getId()).containsExactly(1L, 2L);
        assertThat(result).extracting(StrayAnimalDistanceVO::getDistanceKm).isSorted();
        assertThat(result.get(0).getAnimal().getProfessionalPhone()).isNull();
        verify(context.applicationMapper, never()).selectList(any());
    }

    @Test
    void listNearbyQueriesOnlyPublicCoordinates() {
        TestContext context = context();
        when(context.animalMapper.selectList(any())).thenReturn(List.of());

        context.service.listNearby(coordinate("118.82"), coordinate("31.953"), 20, null);

        ArgumentCaptor<LambdaQueryWrapper<StrayAnimal>> captor = animalQueryCaptor();
        verify(context.animalMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment().toLowerCase();
        assertThat(sql).contains("status", "rescue_status", "longitude is not null", "latitude is not null");
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(1, StrayAnimal.RESCUE_WAITING, StrayAnimal.RESCUE_ADOPTION_PENDING);
    }

    @Test
    void listNearbyUsesDefaultAndClampedLimits() {
        TestContext context = context();
        List<StrayAnimal> animals = new ArrayList<>();
        for (int i = 1; i <= 105; i++) {
            animals.add(animal((long) i, BigDecimal.valueOf(118.82 + i * 0.001).toPlainString()));
        }
        when(context.animalMapper.selectList(any())).thenReturn(animals);

        assertThat(context.service.listNearby(coordinate("118.82"), coordinate("31.953"), null, null)).hasSize(20);
        assertThat(context.service.listNearby(coordinate("118.82"), coordinate("31.953"), 0, null)).hasSize(1);
        assertThat(context.service.listNearby(coordinate("118.82"), coordinate("31.953"), 101, null)).hasSize(100);
    }

    @Test
    void listNearbyRejectsInvalidCoordinates() {
        TestContext context = context();

        assertThatThrownBy(() -> context.service.listNearby(null, coordinate("31.953"), 20, null))
                .hasMessage("经纬度不合法");
        assertThatThrownBy(() -> context.service.listNearby(coordinate("180.0001"), coordinate("31.953"), 20, null))
                .hasMessage("经纬度不合法");
        assertThatThrownBy(() -> context.service.listNearby(coordinate("118.82"), coordinate("-90.0001"), 20, null))
                .hasMessage("经纬度不合法");
    }

    @Test
    void listPhoneRequiresApprovedApplication() {
        TestContext context = contextWithAnimalList();
        when(context.applicationMapper.selectList(any()))
                .thenReturn(List.of(), List.of(approvedApplication()));

        StrayAnimalDetailVO hidden = context.service.listNearby(
                coordinate("118.82"), coordinate("31.953"), 20, 7L).get(0).getAnimal();
        StrayAnimalDetailVO visible = context.service.listNearby(
                coordinate("118.82"), coordinate("31.953"), 20, 7L).get(0).getAnimal();

        assertThat(hidden.getProfessionalPhone()).isNull();
        assertThat(visible.getProfessionalPhone()).isEqualTo("025-86189999");
        ArgumentCaptor<LambdaQueryWrapper<StrayAdoptionApplication>> captor = applicationQueryCaptor();
        verify(context.applicationMapper, org.mockito.Mockito.times(2)).selectList(captor.capture());
        LambdaQueryWrapper<StrayAdoptionApplication> wrapper = captor.getAllValues().get(0);
        assertThat(wrapper.getSqlSegment().toLowerCase())
                .contains("animal_id", "applicant_user_id", "status");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains(1L, 7L, StrayAdoptionApplication.STATUS_APPROVED);
    }

    @Test
    void getDetailMapsFieldsAndAppliesPhonePermission() {
        TestContext context = context();
        when(context.animalMapper.selectById(1L)).thenReturn(animal(1L, "118.821"));
        when(context.applicationMapper.selectList(any()))
                .thenReturn(List.of(), List.of(approvedApplication()));

        StrayAnimalDetailVO hidden = context.service.getDetail(1L, 7L);
        StrayAnimalDetailVO visible = context.service.getDetail(1L, 7L);

        assertThat(hidden.getId()).isEqualTo(1L);
        assertThat(hidden.getTemporaryName()).isEqualTo("小橘");
        assertThat(hidden.getSpecies()).isEqualTo("猫");
        assertThat(hidden.getProfessionalName()).isEqualTo("王医生");
        assertThat(hidden.getProfessionalOrganization()).isEqualTo("江宁宠物救助站");
        assertThat(hidden.getProfessionalPhone()).isNull();
        assertThat(hidden.getCaptureInstruction()).isEqualTo("使用航空箱诱导");
        assertThat(visible.getProfessionalPhone()).isEqualTo("025-86189999");
    }

    @Test
    void getDetailRejectsMissingOrNonPublicAnimal() {
        TestContext context = context();
        assertThatThrownBy(() -> context.service.getDetail(99L, null))
                .hasMessage("流浪动物不存在");

        StrayAnimal disabled = animal(2L, "118.821");
        disabled.setStatus(0);
        when(context.animalMapper.selectById(2L)).thenReturn(disabled);
        assertThatThrownBy(() -> context.service.getDetail(2L, null))
                .hasMessage("流浪动物不存在");

        StrayAnimal adopted = animal(3L, "118.821");
        adopted.setRescueStatus(StrayAnimal.RESCUE_ADOPTED);
        when(context.animalMapper.selectById(3L)).thenReturn(adopted);
        assertThatThrownBy(() -> context.service.getDetail(3L, null))
                .hasMessage("流浪动物不存在");
    }

    @Test
    void listForAdminFiltersRescueStatusAndOrdersNewestFirst() {
        TestContext context = contextWithAnimalList();

        assertThat(context.service.listForAdmin(StrayAnimal.RESCUE_WAITING)).hasSize(1);

        ArgumentCaptor<LambdaQueryWrapper<StrayAnimal>> captor = animalQueryCaptor();
        verify(context.animalMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment().toLowerCase())
                .contains("rescue_status", "order by create_time desc");
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(StrayAnimal.RESCUE_WAITING);
    }

    @Test
    void updateForAdminUpdatesDtoFieldsAndUpdateTime() {
        TestContext context = contextWithAnimal();
        LocalDateTime before = LocalDateTime.now();

        context.service.updateForAdmin(1L, validUpdate());

        ArgumentCaptor<StrayAnimal> captor = ArgumentCaptor.forClass(StrayAnimal.class);
        verify(context.animalMapper).updateById(captor.capture());
        StrayAnimal updated = captor.getValue();
        assertThat(updated.getHealthStatus()).isEqualTo("恢复良好");
        assertThat(updated.getDangerLevel()).isZero();
        assertThat(updated.getRescueStatus()).isEqualTo(StrayAnimal.RESCUE_ADOPTION_PENDING);
        assertThat(updated.getProfessionalName()).isEqualTo("李医生");
        assertThat(updated.getProfessionalOrganization()).isEqualTo("南京动物救助中心");
        assertThat(updated.getProfessionalPhone()).isEqualTo("025-12345678");
        assertThat(updated.getCaptureInstruction()).isEqualTo("保持安静后接近");
        assertThat(updated.getDewormingAdvice()).isEqualTo("一周后驱虫");
        assertThat(updated.getVaccineAdvice()).isEqualTo("观察后接种");
        assertThat(updated.getStatus()).isZero();
        assertThat(updated.getUpdateTime()).isAfterOrEqualTo(before);
    }

    @Test
    void updateForAdminValidatesRecordStatusAndRequiredFields() {
        assertThatThrownBy(() -> context().service.updateForAdmin(99L, validUpdate()))
                .hasMessage("流浪动物不存在");

        StrayAnimalUpdateDTO invalidStatus = validUpdate();
        invalidStatus.setRescueStatus(4);
        assertThatThrownBy(() -> contextWithAnimal().service.updateForAdmin(1L, invalidStatus))
                .hasMessage("救助状态不合法");

        List<Consumer<StrayAnimalUpdateDTO>> invalidators = List.of(
                dto -> dto.setHealthStatus(" "),
                dto -> dto.setProfessionalName(null),
                dto -> dto.setProfessionalOrganization(""),
                dto -> dto.setProfessionalPhone(" "),
                dto -> dto.setCaptureInstruction(null),
                dto -> dto.setDewormingAdvice(""),
                dto -> dto.setVaccineAdvice(" ")
        );
        for (Consumer<StrayAnimalUpdateDTO> invalidator : invalidators) {
            TestContext context = contextWithAnimal();
            StrayAnimalUpdateDTO dto = validUpdate();
            invalidator.accept(dto);
            assertThatThrownBy(() -> context.service.updateForAdmin(1L, dto))
                    .hasMessage("请完整填写流浪动物专业协助信息");
            verify(context.animalMapper, never()).updateById(any(StrayAnimal.class));
        }
    }

    private TestContext contextWithAnimalList() {
        TestContext context = context();
        when(context.animalMapper.selectList(any())).thenReturn(List.of(animal(1L, "118.821")));
        return context;
    }

    private TestContext contextWithAnimal() {
        TestContext context = context();
        when(context.animalMapper.selectById(1L)).thenReturn(animal(1L, "118.821"));
        return context;
    }

    private TestContext context() {
        initializeTableInfo(StrayAnimalMapper.class.getName(), StrayAnimal.class);
        initializeTableInfo(StrayAdoptionApplicationMapper.class.getName(), StrayAdoptionApplication.class);
        StrayAnimalMapper animalMapper = mock(StrayAnimalMapper.class);
        StrayAdoptionApplicationMapper applicationMapper = mock(StrayAdoptionApplicationMapper.class);
        return new TestContext(new StrayAnimalServiceImpl(animalMapper, applicationMapper),
                animalMapper, applicationMapper);
    }

    private void initializeTableInfo(String namespace, Class<?> entityType) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(namespace);
        TableInfoHelper.initTableInfo(assistant, entityType);
    }

    private StrayAnimal animal(Long id, String longitude) {
        StrayAnimal animal = new StrayAnimal();
        animal.setId(id);
        animal.setTemporaryName("小橘");
        animal.setSpecies("猫");
        animal.setGender("未知");
        animal.setEstimatedAge(2);
        animal.setPhotoUrl("/uploads/stray-animal/cat.jpg");
        animal.setFoundAddress("南京市江宁区弘景大道 99 号");
        animal.setLongitude(new BigDecimal(longitude));
        animal.setLatitude(new BigDecimal("31.953"));
        animal.setFoundTime(LocalDateTime.of(2026, 11, 11, 9, 30));
        animal.setAppearanceDescription("橘色短毛");
        animal.setHealthStatus("轻微外伤");
        animal.setDangerLevel(1);
        animal.setRescueStatus(StrayAnimal.RESCUE_WAITING);
        animal.setProfessionalName("王医生");
        animal.setProfessionalOrganization("江宁宠物救助站");
        animal.setProfessionalPhone("025-86189999");
        animal.setCaptureInstruction("使用航空箱诱导");
        animal.setDewormingAdvice("隔离观察后驱虫");
        animal.setVaccineAdvice("健康稳定后接种");
        animal.setStatus(1);
        animal.setCreateTime(LocalDateTime.of(2026, 11, 11, 10, 0));
        return animal;
    }

    private StrayAdoptionApplication approvedApplication() {
        StrayAdoptionApplication application = new StrayAdoptionApplication();
        application.setAnimalId(1L);
        application.setApplicantUserId(7L);
        application.setStatus(StrayAdoptionApplication.STATUS_APPROVED);
        return application;
    }

    private StrayAnimalUpdateDTO validUpdate() {
        StrayAnimalUpdateDTO dto = new StrayAnimalUpdateDTO();
        dto.setHealthStatus(" 恢复良好 ");
        dto.setDangerLevel(0);
        dto.setRescueStatus(StrayAnimal.RESCUE_ADOPTION_PENDING);
        dto.setProfessionalName(" 李医生 ");
        dto.setProfessionalOrganization(" 南京动物救助中心 ");
        dto.setProfessionalPhone(" 025-12345678 ");
        dto.setCaptureInstruction(" 保持安静后接近 ");
        dto.setDewormingAdvice(" 一周后驱虫 ");
        dto.setVaccineAdvice(" 观察后接种 ");
        dto.setStatus(0);
        return dto;
    }

    private BigDecimal coordinate(String value) {
        return new BigDecimal(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<LambdaQueryWrapper<StrayAnimal>> animalQueryCaptor() {
        return ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<LambdaQueryWrapper<StrayAdoptionApplication>> applicationQueryCaptor() {
        return ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
    }

    private record TestContext(StrayAnimalServiceImpl service,
                               StrayAnimalMapper animalMapper,
                               StrayAdoptionApplicationMapper applicationMapper) {
    }
}
