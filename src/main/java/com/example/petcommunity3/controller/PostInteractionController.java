package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.PostActionDTO;
import com.example.petcommunity3.dto.PostCommentCreateDTO;
import com.example.petcommunity3.entity.PostComment;
import com.example.petcommunity3.service.PostInteractionService;
import com.example.petcommunity3.vo.PostCommentTreeVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/post-interactions")
public class PostInteractionController {
    // 帖子互动接口入口：评论、点赞、收藏都属于帖子互动，不直接放在 PostController 里。
    private final PostInteractionService postInteractionService;

    // 构造方法注入互动业务层，Controller 只做请求转发和统一返回。
    public PostInteractionController(PostInteractionService postInteractionService) {
        this.postInteractionService = postInteractionService;
    }

    // POST /api/post-interactions/comments
    // 发表评论，请求体里包含 postId、userId、content 等评论信息。
    @PostMapping("/comments")
    public Result<Void> comment(@RequestBody PostCommentCreateDTO dto) {
        postInteractionService.comment(dto);
        return Result.success(null);
    }

    // GET /api/post-interactions/comments?postId=1
    // 查询某个帖子下的正常评论列表。
    @GetMapping("/comments")
    public Result<List<PostComment>> listComments(@RequestParam Long postId) {
        List<PostComment> comments = postInteractionService.listComments(postId);
        return Result.success(comments);
    }

    @GetMapping("/comments/tree")
    public Result<List<PostCommentTreeVO>> listCommentTree(@RequestParam Long postId) {
        return Result.success(postInteractionService.listCommentTree(postId));
    }

    // POST /api/post-interactions/likes
    // 给帖子点赞，PostActionDTO 中带 postId 和 userId。
    @PostMapping("/likes")
    public Result<Void> like(@RequestBody PostActionDTO dto) {
        postInteractionService.like(dto);
        return Result.success(null);
    }

    // DELETE /api/post-interactions/likes
    // 取消点赞，这里仍然用请求体传 postId 和 userId。
    @DeleteMapping("/likes")
    public Result<Void> unlike(@RequestBody PostActionDTO dto) {
        postInteractionService.unlike(dto);
        return Result.success(null);
    }

    // POST /api/post-interactions/collects
    // 收藏帖子，逻辑和点赞类似，但写入的是 post_collect 表。
    @PostMapping("/collects")
    public Result<Void> collect(@RequestBody PostActionDTO dto) {
        postInteractionService.collect(dto);
        return Result.success(null);
    }

    // DELETE /api/post-interactions/collects
    // 取消收藏，同时 Service 会维护帖子的 collectCount。
    @DeleteMapping("/collects")
    public Result<Void> uncollect(@RequestBody PostActionDTO dto) {
        postInteractionService.uncollect(dto);
        return Result.success(null);
    }
}
