package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.PetCreateDTO;
import com.example.petcommunity3.dto.PetUpdateDTO;
import com.example.petcommunity3.entity.Pet;
import com.example.petcommunity3.mapper.PetMapper;
import com.example.petcommunity3.service.PetService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PetServiceImpl implements PetService {
    // PetServiceImpl 负责宠物档案模块：校验请求数据，并通过 PetMapper 操作 pet 表。
    private final PetMapper petMapper;

    public PetServiceImpl(PetMapper petMapper) {
        // 构造方法注入 PetMapper，后面所有宠物表的增删改查都通过它完成。
        this.petMapper = petMapper;
    }

    @Override
    public void create(PetCreateDTO dto) {
        // 新建宠物档案：先检查必填字段，再把 DTO 转成 Pet 实体入库。
        if (dto.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("宠物名称不能为空");
        }
        if (!StringUtils.hasText(dto.getSpecies())) {
            throw new RuntimeException("宠物品种不能为空");
        }

        // Pet 是实体类，对应数据库 pet 表的一行记录。
        Pet pet = new Pet();
        pet.setUserId(dto.getUserId());
        pet.setName(dto.getName());
        pet.setSpecies(dto.getSpecies());
        pet.setBreed(dto.getBreed());
        pet.setAge(dto.getAge());
        pet.setWeight(dto.getWeight());
        pet.setGender(dto.getGender());
        pet.setVaccinated(dto.getVaccinated() == null ? 0 : dto.getVaccinated());
        pet.setNeutered(dto.getNeutered() == null ? 0 : dto.getNeutered());
        pet.setAvatar(dto.getAvatar());
        pet.setRemark(dto.getRemark());
        // insert 会把 pet 对象里的字段保存到 pet 表。
        petMapper.insert(pet);
    }

    @Override
    public List<Pet> listByUserId(Long userId) {
        // 按 userId 查询宠物列表，只返回这个用户自己的宠物档案。
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        return petMapper.selectList(
                new LambdaQueryWrapper<Pet>()
                        .eq(Pet::getUserId, userId)
                        .orderByDesc(Pet::getCreateTime)
        );
    }

    @Override
    public void update(PetUpdateDTO dto) {
        // 修改宠物档案：必须同时有宠物 ID 和用户 ID，防止改到别人的宠物。
        if (dto.getId() == null) {
            throw new RuntimeException("宠物ID不能为空");
        }
        if (dto.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        // 先查出属于当前用户的宠物，再在这个对象上覆盖新字段。
        Pet pet = getOwnedPet(dto.getId(), dto.getUserId());
        pet.setName(dto.getName());
        pet.setSpecies(dto.getSpecies());
        pet.setBreed(dto.getBreed());
        pet.setAge(dto.getAge());
        pet.setWeight(dto.getWeight());
        pet.setGender(dto.getGender());
        pet.setVaccinated(dto.getVaccinated());
        pet.setNeutered(dto.getNeutered());
        pet.setAvatar(dto.getAvatar());
        pet.setRemark(dto.getRemark());
        petMapper.updateById(pet);
    }

    @Override
    public void delete(Long id, Long userId) {
        // 删除前先确认宠物属于该用户；确认通过后再按主键删除。
        getOwnedPet(id, userId);
        petMapper.deleteById(id);
    }

    @Override
    public void updateAvatar(Long id, Long userId, String avatar) {
        // 单独更新头像：只改 avatar 字段，不影响宠物其它资料。
        if (!StringUtils.hasText(avatar)) {
            throw new RuntimeException("头像URL不能为空");
        }

        Pet pet = getOwnedPet(id, userId);
        pet.setAvatar(avatar);
        petMapper.updateById(pet);
    }

    private Pet getOwnedPet(Long id, Long userId) {
        // 公共校验方法：通过 id + userId 查询，保证当前用户只能操作自己的宠物。
        if (id == null) {
            throw new RuntimeException("宠物ID不能为空");
        }
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        Pet pet = petMapper.selectOne(
                new LambdaQueryWrapper<Pet>()
                        .eq(Pet::getId, id)
                        .eq(Pet::getUserId, userId)
        );
        if (pet == null) {
            throw new RuntimeException("宠物不存在");
        }
        return pet;
    }
}