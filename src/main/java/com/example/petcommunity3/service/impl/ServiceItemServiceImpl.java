package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.ServiceItemCreateDTO;
import com.example.petcommunity3.dto.ServiceItemUpdateDTO;
import com.example.petcommunity3.entity.ServiceItem;
import com.example.petcommunity3.entity.Store;
import com.example.petcommunity3.mapper.ServiceItemMapper;
import com.example.petcommunity3.mapper.StoreMapper;
import com.example.petcommunity3.service.MerchantPermissionService;
import com.example.petcommunity3.service.ServiceItemService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ServiceItemServiceImpl implements ServiceItemService {
    // ServiceItemServiceImpl 负责服务项目模块：门店的洗护、寄养、诊疗等项目管理。
    private final ServiceItemMapper serviceItemMapper;
    private final StoreMapper storeMapper;
    private final MerchantPermissionService merchantPermissionService;

    public ServiceItemServiceImpl(ServiceItemMapper serviceItemMapper,
                                  StoreMapper storeMapper,
                                  MerchantPermissionService merchantPermissionService) {
        this.serviceItemMapper = serviceItemMapper;
        this.storeMapper = storeMapper;
        this.merchantPermissionService = merchantPermissionService;
    }

    @Override
    public void create(Long operatorId, ServiceItemCreateDTO dto) {
        // 创建服务项目：先确认门店存在且启用，再保存服务名称、分类、价格等信息。
        if (dto.getStoreId() == null) {
            throw new RuntimeException("门店ID不能为空");
        }

        merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, dto.getStoreId());
        Store store = storeMapper.selectById(dto.getStoreId());
        if (store == null || !Integer.valueOf(1).equals(store.getStatus())) {
            throw new RuntimeException("门店不存在");
        }

        if (!StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("服务名称不能为空");
        }

        if (!StringUtils.hasText(dto.getCategory())) {
            throw new RuntimeException("服务分类不能为空");
        }

        if (dto.getPrice() == null) {
            throw new RuntimeException("服务价格不能为空");
        }

        if (dto.getDuration() == null) {
            throw new RuntimeException("服务时长不能为空");
        }

        ServiceItem item = new ServiceItem();
        item.setStoreId(dto.getStoreId());
        item.setName(dto.getName());
        item.setCategory(dto.getCategory());
        item.setPrice(dto.getPrice());
        item.setDuration(dto.getDuration());
        item.setCover(dto.getCover());
        item.setDescription(dto.getDescription());
        item.setStatus(1);

        serviceItemMapper.insert(item);
    }

    @Override
    public List<ServiceItem> listByStoreId(Long storeId) {
        // 按门店查询服务项目：前台预约时先选门店，再看该门店有哪些服务。
        if (storeId == null) {
            throw new RuntimeException("门店ID不能为空");
        }

        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getStoreId, storeId)
                        .eq(ServiceItem::getStatus, 1)
                        .orderByDesc(ServiceItem::getCreateTime)
        );
    }

    @Override
    public ServiceItem getDetail(Long id) {
        // 服务项目详情：按 ID 查询并确认项目存在。
        if (id == null) {
            throw new RuntimeException("服务项目ID不能为空");
        }

        ServiceItem item = serviceItemMapper.selectById(id);

        if (item == null || !Integer.valueOf(1).equals(item.getStatus())) {
            throw new RuntimeException("服务项目不存在");
        }

        return item;
    }

    @Override
    public void update(Long operatorId, ServiceItemUpdateDTO dto) {
        // 修改服务项目：先查原项目，再覆盖可修改字段。
        if (dto.getId() == null) {
            throw new RuntimeException("服务项目ID不能为空");
        }

        ServiceItem item = serviceItemMapper.selectById(dto.getId());

        if (item == null) {
            throw new RuntimeException("服务项目不存在");
        }
        merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, item.getStoreId());

        if (dto.getStoreId() != null) {
            merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, dto.getStoreId());
            Store store = storeMapper.selectById(dto.getStoreId());
            if (store == null || !Integer.valueOf(1).equals(store.getStatus())) {
                throw new RuntimeException("门店不存在");
            }
            item.setStoreId(dto.getStoreId());
        }

        item.setName(dto.getName());
        item.setCategory(dto.getCategory());
        item.setPrice(dto.getPrice());
        item.setDuration(dto.getDuration());
        item.setCover(dto.getCover());
        item.setDescription(dto.getDescription());

        if (dto.getStatus() != null) {
            item.setStatus(dto.getStatus());
        }

        serviceItemMapper.updateById(item);
    }

    @Override
    public void disable(Long operatorId, Long id) {
        // 下架服务项目：逻辑删除，把 status 改成 0。
        if (id == null) {
            throw new RuntimeException("服务项目ID不能为空");
        }

        ServiceItem item = serviceItemMapper.selectById(id);

        if (item == null) {
            throw new RuntimeException("服务项目不存在");
        }
        merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, item.getStoreId());

        item.setStatus(0);
        serviceItemMapper.updateById(item);
    }
}
