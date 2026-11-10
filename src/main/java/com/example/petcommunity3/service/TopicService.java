package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.TopicCreateDTO;
import com.example.petcommunity3.entity.Topic;

import java.util.List;

public interface TopicService {
    // 创建话题：校验话题名称是否重复，并设置默认状态和排序。
    void create(TopicCreateDTO dto);

    // 查询话题列表：返回启用状态的话题，给发帖和话题圈子使用。
    List<Topic> list();
}
