package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.SensitiveWordCreateDTO;
import com.example.petcommunity3.dto.SensitiveWordUpdateDTO;
import com.example.petcommunity3.entity.SensitiveWord;
import com.example.petcommunity3.service.SensitiveWordService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensitive-words")
public class SensitiveWordController {
    // 敏感词接口入口：后台维护敏感词，发帖和评论时会调用敏感词检查。
    private final SensitiveWordService sensitiveWordService;

    // 注入敏感词业务层，Controller 只负责接收维护请求。
    public SensitiveWordController(SensitiveWordService sensitiveWordService) {
        this.sensitiveWordService = sensitiveWordService;
    }

    // POST /api/sensitive-words
    // 新增一个敏感词，默认启用。
    @PostMapping
    public Result<Void> create(@RequestBody SensitiveWordCreateDTO dto) {
        sensitiveWordService.create(dto);
        return Result.success(null);
    }

    // GET /api/sensitive-words?status=1
    // 查询敏感词列表，status 可选，1 表示启用，0 表示停用。
    @GetMapping
    public Result<List<SensitiveWord>> list(@RequestParam(required = false) Integer status) {
        return Result.success(sensitiveWordService.list(status));
    }

    // PUT /api/sensitive-words
    // 修改敏感词内容、等级或启用状态。
    @PutMapping
    public Result<Void> update(@RequestBody SensitiveWordUpdateDTO dto) {
        sensitiveWordService.update(dto);
        return Result.success(null);
    }

    // DELETE /api/sensitive-words/{id}
    // 停用敏感词，不直接删除，方便保留维护记录。
    @DeleteMapping("/{id}")
    public Result<Void> disable(@PathVariable Long id) {
        sensitiveWordService.disable(id);
        return Result.success(null);
    }
}
