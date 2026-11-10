package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.PostCreateDTO;
import com.example.petcommunity3.dto.PostUpdateDTO;
import com.example.petcommunity3.entity.Post;

import java.util.List;

public interface PostService {
    // 发布帖子：校验用户、标题、内容、话题和敏感词后插入 post 表。
    void create(PostCreateDTO dto);

    // 查询帖子列表：返回正常状态的帖子，通常按发布时间倒序。
    List<Post> list();

    // 查询用户自己的全部帖子，包含待审核、驳回和封禁状态。
    List<Post> listByUserId(Long userId);

    // 查询帖子详情：根据帖子 id 查询单条帖子，不存在则抛异常。
    Post getDetail(Long id);

    // 修改帖子：只允许作者修改自己的帖子。
    void update(PostUpdateDTO dto);

    // 删除帖子：只允许作者删除自己的帖子，同时维护话题帖子数量。
    void delete(Long id, Long userId);

    // 按话题查询帖子：给话题圈子页面使用。
    List<Post> listByTopic(String topic);

    // 更新帖子封面：文件上传后把封面 URL 写入 post.cover。
    void updateCover(Long id, Long userId, String cover);
}
