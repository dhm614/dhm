package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.ImageUpdateDTO;
import com.example.petcommunity3.dto.PetCreateDTO;
import com.example.petcommunity3.dto.PetUpdateDTO;
import com.example.petcommunity3.entity.Pet;
import com.example.petcommunity3.service.PetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {
    // 宠物档案接口入口：新增、查询、修改、删除宠物都先进入这里。
    private final PetService petService;

    // 构造方法注入 PetService，Controller 不直接操作数据库。
    public PetController(PetService petService) {
        this.petService = petService;
    }

    // POST /api/pets
    // 请求体 JSON 会转成 PetCreateDTO，用来创建当前用户的一只宠物。
    @PostMapping
    public Result<Void> create(@RequestBody PetCreateDTO dto) {
        petService.create(dto);
        return Result.success(null);
    }

    // GET /api/pets?userId=1
    // @RequestParam 从 URL 问号后读取 userId，查询这个用户名下的宠物列表。
    @GetMapping
    public Result<List<Pet>> list(@RequestParam Long userId) {
        List<Pet> pets = petService.listByUserId(userId);
        return Result.success(pets);
    }

    // PUT /api/pets
    // 使用 PetUpdateDTO 承接修改数据，Service 会校验这只宠物是否属于该用户。
    @PutMapping
    public Result<Void> update(@RequestBody PetUpdateDTO dto) {
        petService.update(dto);
        return Result.success(null);
    }

    // DELETE /api/pets/{id}?userId=1
    // id 来自路径，userId 来自请求参数，二者一起用来避免删除别人的宠物。
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        petService.delete(id, userId);
        return Result.success(null);
    }

    // PUT /api/pets/{id}/avatar
    // 单独更新宠物头像，适合文件上传成功后把图片 URL 保存到宠物记录里。
    @PutMapping("/{id}/avatar")
    public Result<Void> updateAvatar(@PathVariable Long id, @RequestBody ImageUpdateDTO dto) {
        petService.updateAvatar(id, dto.getUserId(), dto.getUrl());
        return Result.success(null);
    }
}
