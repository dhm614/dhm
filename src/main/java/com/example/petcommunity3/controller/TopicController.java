package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.TopicCreateDTO;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.service.TopicService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {
    // 话题接口入口：前台可查询话题列表，后台或运营可创建话题。
    private final TopicService topicService;

    // 注入话题业务层，具体查重、默认排序、状态设置都放在 Service 中。
    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    // POST /api/topics
    // 创建话题，前端传入名称、描述、封面等数据。
    @PostMapping
    public Result<Void> create(@RequestBody TopicCreateDTO dto) {
        topicService.create(dto);
        return Result.success(null);
    }

    // GET /api/topics
    // 查询正常启用的话题列表，通常给发帖选择话题或话题圈子页面使用。
    @GetMapping
    public Result<List<Topic>> list() {
        List<Topic> topics = topicService.list();
        return Result.success(topics);
    }

}
