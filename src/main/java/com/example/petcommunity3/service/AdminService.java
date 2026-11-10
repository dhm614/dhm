package com.example.petcommunity3.service;

import com.example.petcommunity3.entity.Appointment;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.PostComment;
import com.example.petcommunity3.entity.ProductOrder;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.dto.TopicCreateDTO;
import com.example.petcommunity3.dto.TopicReviewDTO;

import java.util.List;

public interface AdminService {
    // 管理员权限检查：后台接口调用前先确认操作者是 ADMIN。
    void requireAdmin(Long operatorId);

    // 查询用户：id 为空时返回全部用户，传入 id 时精确查询。
    List<User> listUsers(Long id);

    // 管理员直接把普通账号调整为商户账号。
    void setMerchantUser(Long id, Long operatorId);

    // 管理员把商户账号恢复为普通账号。
    void setNormalUser(Long id, Long operatorId);

    // 禁用用户：把用户 status 改为 0。
    void disableUser(Long id);

    // 启用用户：把用户 status 改为 1。
    void enableUser(Long id);

    // 下架帖子：把帖子 status 改为下架状态。
    void disablePost(Long id);

    // 恢复帖子：把帖子 status 改为正常状态。
    void enablePost(Long id);

    void pinPost(Long id);

    void unpinPost(Long id);

    void featurePost(Long id);

    void unfeaturePost(Long id);

    // 隐藏评论：把评论 status 改为删除状态。
    void deleteComment(Long id);

    // 恢复评论：把评论 status 改为正常状态。
    void restoreComment(Long id);

    // 后台查询帖子：支持按状态和关键词筛选。
    List<Post> listPosts(Integer status, String keyword);

    // 后台查看单个帖子审核详情。
    Post getPostDetail(Long id);

    // 审核帖子：管理员修改帖子审核状态。
    void reviewPost(Long id, StatusUpdateDTO dto);

    // 后台查询短视频：支持按状态和关键词筛选。
    List<Post> listVideos(Integer status, String keyword);

    // 后台查看单个短视频审核详情。
    Post getVideoDetail(Long id);

    // 审核短视频：管理员修改短视频审核状态。
    void reviewVideo(Long id, StatusUpdateDTO dto);

    // 删除或下架短视频。
    void deleteVideo(Long id);

    // 恢复短视频展示。
    void restoreVideo(Long id);

    // 后台查询评论：支持按状态、帖子 id 和关键词筛选。
    List<PostComment> listComments(Integer status, Long postId, String keyword);

    // 后台查询话题：支持按状态和关键词筛选。
    List<Topic> listAuditTopics(Integer status, String keyword);

    // 审核话题：管理员修改话题状态、排序或审核备注。
    void reviewTopic(Long id, TopicReviewDTO dto);

    // 管理员创建话题。
    void createTopic(TopicCreateDTO dto);

    // 管理员停用话题。
    void deleteTopic(Long id);

    // 后台查询全部预约。
    List<Appointment> listAppointments();

    // 后台查询全部商品订单。
    List<ProductOrder> listProductOrders();
}
