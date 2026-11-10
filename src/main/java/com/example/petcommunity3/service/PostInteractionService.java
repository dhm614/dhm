package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.PostActionDTO;
import com.example.petcommunity3.dto.PostCommentCreateDTO;
import com.example.petcommunity3.entity.PostComment;
import com.example.petcommunity3.vo.PostCommentTreeVO;

import java.util.List;

public interface PostInteractionService {
    // 发表评论：校验用户、帖子和敏感词，插入评论并增加帖子评论数。
    void comment(PostCommentCreateDTO dto);

    // 查询评论：根据帖子 id 查询正常状态评论。
    List<PostComment> listComments(Long postId);

    List<PostCommentTreeVO> listCommentTree(Long postId);

    // 点赞帖子：写入 post_like，并增加帖子点赞数。
    void like(PostActionDTO dto);

    // 取消点赞：删除 post_like，并减少帖子点赞数。
    void unlike(PostActionDTO dto);

    // 收藏帖子：写入 post_collect，并增加帖子收藏数。
    void collect(PostActionDTO dto);

    // 取消收藏：删除 post_collect，并减少帖子收藏数。
    void uncollect(PostActionDTO dto);
}
