package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.SensitiveWordCreateDTO;
import com.example.petcommunity3.dto.SensitiveWordUpdateDTO;
import com.example.petcommunity3.entity.SensitiveWord;
import com.example.petcommunity3.mapper.SensitiveWordMapper;
import com.example.petcommunity3.service.SensitiveWordService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {
    // SensitiveWordServiceImpl 负责敏感词模块：维护敏感词库，并在发帖/评论前检查文本。
    private final SensitiveWordMapper sensitiveWordMapper;

    public SensitiveWordServiceImpl(SensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @Override
    public void checkText(String text) {
        // 检查单段文本：查询启用的敏感词，只要命中就抛出异常。
        if (!StringUtils.hasText(text)) {
            return;
        }

        List<SensitiveWord> words = sensitiveWordMapper.selectList(
                new LambdaQueryWrapper<SensitiveWord>()
                        .eq(SensitiveWord::getStatus, 1)
        );

        for (SensitiveWord sensitiveWord : words) {
            if (StringUtils.hasText(sensitiveWord.getWord())
                    && text.contains(sensitiveWord.getWord())) {
                throw new RuntimeException("内容包含敏感词：" + sensitiveWord.getWord());
            }
        }
    }

    @Override
    public void checkPost(String title, String content) {
        // 检查帖子：标题和正文都要走敏感词校验。
        checkText(title);
        checkText(content);
    }

    @Override
    public void create(SensitiveWordCreateDTO dto) {
        // 新增敏感词：后台录入词条，默认启用。
        if (!StringUtils.hasText(dto.getWord())) {
            throw new RuntimeException("敏感词不能为空");
        }

        SensitiveWord existWord = sensitiveWordMapper.selectOne(
                new LambdaQueryWrapper<SensitiveWord>()
                        .eq(SensitiveWord::getWord, dto.getWord())
        );

        if (existWord != null) {
            throw new RuntimeException("敏感词已存在");
        }

        SensitiveWord sensitiveWord = new SensitiveWord();
        sensitiveWord.setWord(dto.getWord());
        sensitiveWord.setLevel(dto.getLevel() == null ? 1 : dto.getLevel());
        sensitiveWord.setStatus(1);

        sensitiveWordMapper.insert(sensitiveWord);
    }

    @Override
    public List<SensitiveWord> list(Integer status) {
        // 敏感词列表：可按状态筛选，用于后台管理页面。
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(SensitiveWord::getStatus, status);
        }

        wrapper.orderByDesc(SensitiveWord::getCreateTime);

        return sensitiveWordMapper.selectList(wrapper);
    }

    @Override
    public void update(SensitiveWordUpdateDTO dto) {
        // 修改敏感词：先查原记录，再更新词条和状态。
        if (dto.getId() == null) {
            throw new RuntimeException("敏感词ID不能为空");
        }

        SensitiveWord sensitiveWord = sensitiveWordMapper.selectById(dto.getId());
        if (sensitiveWord == null) {
            throw new RuntimeException("敏感词不存在");
        }

        if (StringUtils.hasText(dto.getWord())) {
            sensitiveWord.setWord(dto.getWord());
        }

        if (dto.getLevel() != null) {
            sensitiveWord.setLevel(dto.getLevel());
        }

        if (dto.getStatus() != null) {
            sensitiveWord.setStatus(dto.getStatus());
        }

        sensitiveWordMapper.updateById(sensitiveWord);
    }

    @Override
    public void disable(Long id) {
        // 停用敏感词：逻辑删除，把 status 改成 0。
        SensitiveWord sensitiveWord = sensitiveWordMapper.selectById(id);
        if (sensitiveWord == null) {
            throw new RuntimeException("敏感词不存在");
        }

        sensitiveWord.setStatus(0);
        sensitiveWordMapper.updateById(sensitiveWord);
    }
}