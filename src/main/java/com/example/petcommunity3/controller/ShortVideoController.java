package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.ShortVideoCreateDTO;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.service.ShortVideoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class ShortVideoController {
    private final ShortVideoService shortVideoService;

    public ShortVideoController(ShortVideoService shortVideoService) {
        this.shortVideoService = shortVideoService;
    }

    @PostMapping
    public Result<Void> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody ShortVideoCreateDTO dto) {
        if (userId == null) {
            throw new RuntimeException("用户身份不能为空");
        }
        dto.setUserId(userId);
        shortVideoService.create(dto);
        return Result.success(null);
    }

    @GetMapping
    public Result<List<Post>> list(@RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(shortVideoService.list(limit));
    }

    @GetMapping("/my")
    public Result<List<Post>> listMine(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户身份不能为空");
        }
        return Result.success(shortVideoService.listByUserId(userId));
    }

    @GetMapping("/{id}")
    public Result<Post> detail(@PathVariable Long id) {
        return Result.success(shortVideoService.getDetail(id));
    }
}
