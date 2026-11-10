package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.SensitiveWordCreateDTO;
import com.example.petcommunity3.dto.SensitiveWordUpdateDTO;
import com.example.petcommunity3.entity.SensitiveWord;

import java.util.List;

public interface SensitiveWordService {
    // 检查一段文本：如果包含启用状态的敏感词，就抛出异常阻止继续保存。
    void checkText(String text);

    // 检查帖子：标题和正文都要检查。
    void checkPost(String title, String content);

    // 新增敏感词：后台维护敏感词库。
    void create(SensitiveWordCreateDTO dto);

    // 查询敏感词列表：可按启用状态筛选。
    List<SensitiveWord> list(Integer status);

    // 修改敏感词：更新词内容、等级或状态。
    void update(SensitiveWordUpdateDTO dto);

    // 停用敏感词：不物理删除，保留维护记录。
    void disable(Long id);
}
