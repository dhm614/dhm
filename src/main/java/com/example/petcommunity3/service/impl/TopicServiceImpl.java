package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.TopicCreateDTO;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.service.TopicService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TopicServiceImpl implements TopicService {
    // TopicServiceImpl 负责社区话题模块：用户提交话题、查询启用话题。
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private final TopicMapper topicMapper;

    public TopicServiceImpl(TopicMapper topicMapper) {
        this.topicMapper = topicMapper;
    }

    @Override
    public void create(TopicCreateDTO dto) {
        // 创建话题：普通用户提交后通常进入待审核状态。
        if (dto == null || !StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("话题名称不能为空");
        }

        String name = dto.getName().trim();
        Topic existTopic = findByName(name);
        if (existTopic != null) {
            if (Integer.valueOf(STATUS_PENDING).equals(existTopic.getStatus())) {
                throw new RuntimeException("话题正在审核中");
            }
            if (Integer.valueOf(STATUS_APPROVED).equals(existTopic.getStatus())) {
                throw new RuntimeException("话题已存在");
            }
            existTopic.setDescription(dto.getDescription());
            existTopic.setCover(dto.getCover());
            existTopic.setSort(0);
            existTopic.setStatus(STATUS_PENDING);
            topicMapper.updateById(existTopic);
            return;
        }

        Topic topic = new Topic();
        topic.setName(name);
        topic.setDescription(dto.getDescription());
        topic.setCover(dto.getCover());
        topic.setPostCount(0);
        topic.setSort(0);
        topic.setStatus(STATUS_PENDING);

        topicMapper.insert(topic);
    }

    @Override
    public List<Topic> list() {
        // 话题列表：只展示审核通过且启用的话题。
        return topicMapper.selectList(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getStatus, STATUS_APPROVED)
                        .orderByDesc(Topic::getSort)
                        .orderByDesc(Topic::getCreateTime)
        );
    }

    private Topic findByName(String name) {
        // 按名称查话题，用于防止重复创建同名话题。
        return topicMapper.selectOne(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getName, name)
        );
    }
}
