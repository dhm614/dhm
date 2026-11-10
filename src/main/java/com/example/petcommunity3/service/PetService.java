package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.PetCreateDTO;
import com.example.petcommunity3.dto.PetUpdateDTO;
import com.example.petcommunity3.entity.Pet;

import java.util.List;

public interface PetService {
    // 创建宠物档案：前端传 PetCreateDTO，Service 负责校验并插入 pet 表。
    void create(PetCreateDTO dto);

    // 查询宠物列表：根据 userId 查询这个用户创建的所有宠物。
    List<Pet> listByUserId(Long userId);

    // 修改宠物档案：根据 id 和 userId 校验归属后更新宠物信息。
    void update(PetUpdateDTO dto);

    // 删除宠物档案：删除前校验宠物是否属于当前用户。
    void delete(Long id, Long userId);

    // 更新宠物头像：文件上传成功后，把头像 URL 保存到 avatar 字段。
    void updateAvatar(Long id, Long userId, String avatar);
}
