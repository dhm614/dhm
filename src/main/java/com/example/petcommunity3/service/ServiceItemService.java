package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.ServiceItemCreateDTO;
import com.example.petcommunity3.dto.ServiceItemUpdateDTO;
import com.example.petcommunity3.entity.ServiceItem;

import java.util.List;

public interface ServiceItemService {
    // 新增服务项目：给某个门店添加洗护、寄养、诊疗等服务。
    void create(Long operatorId, ServiceItemCreateDTO dto);

    // 查询门店服务：根据 storeId 查询该门店下的服务项目。
    List<ServiceItem> listByStoreId(Long storeId);

    // 查询服务详情：根据服务项目 id 查询单条记录。
    ServiceItem getDetail(Long id);

    // 修改服务项目：更新名称、价格、时长、描述、状态等信息。
    void update(Long operatorId, ServiceItemUpdateDTO dto);

    // 下架服务项目：把 status 改为 0。
    void disable(Long operatorId, Long id);
}
