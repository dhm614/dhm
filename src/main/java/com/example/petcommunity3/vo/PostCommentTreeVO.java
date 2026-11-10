package com.example.petcommunity3.vo;

import com.example.petcommunity3.entity.PostComment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class PostCommentTreeVO {
    private Long id;
    private Long postId;
    private Long userId;
    private Long parentId;
    private String content;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<PostCommentTreeVO> children = new ArrayList<>();

    public static PostCommentTreeVO from(PostComment comment) {
        PostCommentTreeVO vo = new PostCommentTreeVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setUserId(comment.getUserId());
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setStatus(comment.getStatus());
        vo.setCreateTime(comment.getCreateTime());
        vo.setUpdateTime(comment.getUpdateTime());
        return vo;
    }
}
