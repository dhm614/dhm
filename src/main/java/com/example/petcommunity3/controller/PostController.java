package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.ImageUpdateDTO;
import com.example.petcommunity3.dto.PostCreateDTO;
import com.example.petcommunity3.dto.PostUpdateDTO;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.service.PostService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    // 帖子接口入口：负责把发帖、查帖、改帖、删帖这些 HTTP 请求转交给 PostService。
    private final PostService postService;

    // 构造方法注入 PostService，Spring 会自动找到对应的 PostServiceImpl。
    public PostController(PostService postService) {
        this.postService = postService;
    }

    // POST /api/posts
    // 前端传标题、内容、话题等 JSON，Controller 转成 PostCreateDTO 后调用发帖业务。
    @PostMapping
    public Result<Void> create(@RequestBody PostCreateDTO dto) {
        postService.create(dto);
        return Result.success(null);
    }

    // GET /api/posts
    // 查询正常状态的帖子列表，具体排序和过滤规则在 Service 层。
    @GetMapping
    public Result<List<Post>> list() {
        List<Post> posts = postService.list();
        return Result.success(posts);
    }

    @GetMapping("/my")
    public Result<List<Post>> listMine(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户身份不能为空");
        }
        return Result.success(postService.listByUserId(userId));
    }

    // GET /api/posts/{id}
    // 查询单个帖子详情，id 从路径中读取。
    @GetMapping("/{id}")
    public Result<Post> detail(@PathVariable Long id) {
        Post post = postService.getDetail(id);
        return Result.success(post);
    }

    // PUT /api/posts
    // 修改自己的帖子，Service 会校验帖子存在以及操作者是不是作者本人。
    @PutMapping
    public Result<Void> update(@RequestBody PostUpdateDTO dto) {
        postService.update(dto);
        return Result.success(null);
    }

    // DELETE /api/posts/{id}?userId=1
    // 删除帖子时同时传 id 和 userId，防止用户删除不属于自己的帖子。
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        postService.delete(id, userId);
        return Result.success(null);
    }

    // GET /api/posts/topic?topic=新手养猫
    // 根据话题名称筛选帖子，给话题圈子页面使用。
    @GetMapping("/topic")
    public Result<List<Post>> listByTopic(@RequestParam String topic) {
        List<Post> posts = postService.listByTopic(topic);
        return Result.success(posts);
    }

    // PUT /api/posts/{id}/cover
    // 文件上传得到图片 URL 后，调用这个接口把封面地址写入帖子。
    @PutMapping("/{id}/cover")
    public Result<Void> updateCover(@PathVariable Long id, @RequestBody ImageUpdateDTO dto) {
        postService.updateCover(id, dto.getUserId(), dto.getUrl());
        return Result.success(null);
    }
}
