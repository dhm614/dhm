package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.StoreCreateDTO;
import com.example.petcommunity3.dto.StoreUpdateDTO;
import com.example.petcommunity3.entity.Store;
import com.example.petcommunity3.vo.StoreDistanceVO;

import java.math.BigDecimal;
import java.util.List;

public interface StoreService {
    // 新增门店：保存门店名称、地址、电话、经纬度等信息。
    void create(Long operatorId, StoreCreateDTO dto);

    // 查询门店列表：返回正常营业的门店。
    List<Store> list();

    // 查询门店详情：根据门店 id 查询单条门店记录。
    Store getDetail(Long id);

    // 修改门店：更新门店资料。
    void update(Long operatorId, StoreUpdateDTO dto);

    // 停用门店：把 status 改为 0，不删除历史门店。
    void disable(Long operatorId, Long id);

    // 附近门店：根据用户经纬度计算距离并排序。
    List<StoreDistanceVO> listNearby(BigDecimal longitude, BigDecimal latitude, Integer limit);
}
