package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.StoreCreateDTO;
import com.example.petcommunity3.dto.StoreUpdateDTO;
import com.example.petcommunity3.entity.Store;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.StoreMapper;
import com.example.petcommunity3.service.MerchantPermissionService;
import com.example.petcommunity3.service.StoreService;
import com.example.petcommunity3.vo.StoreDistanceVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class StoreServiceImpl implements StoreService {
    // StoreServiceImpl 负责门店模块：门店入驻、查询、修改、停用和附近门店计算。
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final StoreMapper storeMapper;
    private final MerchantPermissionService merchantPermissionService;

    public StoreServiceImpl(StoreMapper storeMapper, MerchantPermissionService merchantPermissionService) {
        this.storeMapper = storeMapper;
        this.merchantPermissionService = merchantPermissionService;
    }

    @Override
    public void create(Long operatorId, StoreCreateDTO dto) {
        // 创建门店：校验名称和地址，再保存门店基础信息。
        if (!StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("门店名称不能为空");
        }
        if (!StringUtils.hasText(dto.getAddress())) {
            throw new RuntimeException("门店地址不能为空");
        }

        User operator = merchantPermissionService.requireMerchantOrAdmin(operatorId);
        Store store = new Store();
        if (!"ADMIN".equalsIgnoreCase(operator.getRole())) {
            store.setMerchantUserId(operatorId);
        }
        store.setName(dto.getName());
        store.setAddress(dto.getAddress());
        store.setPhone(dto.getPhone());
        store.setCover(dto.getCover());
        store.setLongitude(dto.getLongitude());
        store.setLatitude(dto.getLatitude());
        store.setBusinessHours(dto.getBusinessHours());
        store.setDescription(dto.getDescription());
        store.setStatus(1);
        storeMapper.insert(store);
    }

    @Override
    public List<Store> list() {
        // 门店列表：只返回启用中的门店，并按创建时间倒序。
        return storeMapper.selectList(
                new LambdaQueryWrapper<Store>()
                        .eq(Store::getStatus, 1)
                        .orderByDesc(Store::getCreateTime)
        );
    }

    @Override
    public Store getDetail(Long id) {
        // 门店详情：根据 ID 查询门店，不存在则抛出异常。
        if (id == null) {
            throw new RuntimeException("门店ID不能为空");
        }

        Store store = storeMapper.selectById(id);
        if (store == null || !Integer.valueOf(1).equals(store.getStatus())) {
            throw new RuntimeException("门店不存在");
        }
        return store;
    }

    @Override
    public void update(Long operatorId, StoreUpdateDTO dto) {
        // 修改门店：先查原记录，再更新名称、地址、经纬度、描述等字段。
        if (dto.getId() == null) {
            throw new RuntimeException("门店ID不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("门店名称不能为空");
        }
        if (!StringUtils.hasText(dto.getAddress())) {
            throw new RuntimeException("门店地址不能为空");
        }

        merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, dto.getId());
        Store store = storeMapper.selectById(dto.getId());
        if (store == null) {
            throw new RuntimeException("门店不存在");
        }

        store.setName(dto.getName());
        store.setAddress(dto.getAddress());
        store.setPhone(dto.getPhone());
        store.setCover(dto.getCover());
        store.setLongitude(dto.getLongitude());
        store.setLatitude(dto.getLatitude());
        store.setBusinessHours(dto.getBusinessHours());
        store.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            store.setStatus(dto.getStatus());
        }
        storeMapper.updateById(store);
    }

    @Override
    public void disable(Long operatorId, Long id) {
        // 停用门店：逻辑删除，把 status 改成 0。
        if (id == null) {
            throw new RuntimeException("门店ID不能为空");
        }

        merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, id);
        Store store = storeMapper.selectById(id);
        if (store == null) {
            throw new RuntimeException("门店不存在");
        }

        store.setStatus(0);
        storeMapper.updateById(store);
    }

    @Override
    public List<StoreDistanceVO> listNearby(BigDecimal longitude, BigDecimal latitude, Integer limit) {
        // 附近门店：计算用户坐标到门店坐标的距离，再按距离排序。
        if (longitude == null || latitude == null) {
            throw new RuntimeException("经纬度不能为空");
        }

        int size = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        double userLongitude = longitude.doubleValue();
        double userLatitude = latitude.doubleValue();

        return storeMapper.selectList(
                        new LambdaQueryWrapper<Store>()
                                .eq(Store::getStatus, 1)
                                .isNotNull(Store::getLongitude)
                                .isNotNull(Store::getLatitude)
                )
                .stream()
                .map(store -> buildDistanceVO(store, userLongitude, userLatitude))
                .sorted(Comparator.comparing(StoreDistanceVO::getDistanceKm))
                .limit(size)
                .toList();
    }

    private StoreDistanceVO buildDistanceVO(Store store, double userLongitude, double userLatitude) {
        // 把 Store 实体转成带距离字段的 VO。
        StoreDistanceVO vo = new StoreDistanceVO();
        vo.setStore(store);
        vo.setDistanceKm(distanceKm(
                userLongitude,
                userLatitude,
                store.getLongitude().doubleValue(),
                store.getLatitude().doubleValue()
        ));
        return vo;
    }

    private double distanceKm(double longitude1, double latitude1, double longitude2, double latitude2) {
        // 使用经纬度计算两点球面距离，结果单位是公里。
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        double deltaLat = Math.toRadians(latitude2 - latitude1);
        double deltaLng = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 100.0) / 100.0;
    }
}
