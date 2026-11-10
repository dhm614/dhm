package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.dto.TopicCreateDTO;
import com.example.petcommunity3.dto.TopicReviewDTO;
import com.example.petcommunity3.entity.Appointment;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.PostComment;
import com.example.petcommunity3.entity.ProductOrder;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.service.AdminService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    // 后台管理接口通过请求头 X-User-Id 传入当前操作者 ID，再由 Service 判断是否是管理员。
    private static final String USER_ID_HEADER = "X-User-Id";

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        // 构造方法注入 AdminService；Controller 只做入口和权限调用，具体审核逻辑在 Service。
        this.adminService = adminService;
    }

    // GET /api/admin/users：查询全部用户，传入 id 时按用户 ID 精确查询。
    @GetMapping("/users")
    public Result<List<User>> listUsers(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                        @RequestParam(required = false) Long id) {
        requireAdmin(operatorId);
        return Result.success(adminService.listUsers(id));
    }

    @PutMapping("/users/{id}/set-merchant")
    public Result<Void> setMerchantUser(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                        @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.setMerchantUser(id, operatorId);
        return Result.success(null);
    }

    @PutMapping("/users/{id}/set-normal")
    public Result<Void> setNormalUser(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                      @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.setNormalUser(id, operatorId);
        return Result.success(null);
    }

    // PUT /api/admin/users/{id}/disable：管理员禁用用户账号。
    @PutMapping("/users/{id}/disable")
    public Result<Void> disableUser(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                    @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.disableUser(id);
        return Result.success(null);
    }

    // PUT /api/admin/users/{id}/enable：管理员恢复用户账号。
    @PutMapping("/users/{id}/enable")
    public Result<Void> enableUser(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                   @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.enableUser(id);
        return Result.success(null);
    }

    // PUT /api/admin/posts/{id}/disable：管理员下架帖子。
    @PutMapping("/posts/{id}/disable")
    public Result<Void> disablePost(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                    @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.disablePost(id);
        return Result.success(null);
    }

    // PUT /api/admin/posts/{id}/enable：管理员恢复帖子展示。
    @PutMapping("/posts/{id}/enable")
    public Result<Void> enablePost(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                   @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.enablePost(id);
        return Result.success(null);
    }

    @PutMapping("/posts/{id}/pin")
    public Result<Void> pinPost(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.pinPost(id);
        return Result.success(null);
    }

    @PutMapping("/posts/{id}/unpin")
    public Result<Void> unpinPost(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                  @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.unpinPost(id);
        return Result.success(null);
    }

    @PutMapping("/posts/{id}/feature")
    public Result<Void> featurePost(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                    @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.featurePost(id);
        return Result.success(null);
    }

    @PutMapping("/posts/{id}/unfeature")
    public Result<Void> unfeaturePost(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                      @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.unfeaturePost(id);
        return Result.success(null);
    }

    // PUT /api/admin/comments/{id}/delete：管理员隐藏评论。
    @PutMapping("/comments/{id}/delete")
    public Result<Void> deleteComment(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                      @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.deleteComment(id);
        return Result.success(null);
    }

    // PUT /api/admin/comments/{id}/restore：管理员恢复评论。
    @PutMapping("/comments/{id}/restore")
    public Result<Void> restoreComment(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                       @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.restoreComment(id);
        return Result.success(null);
    }

    // GET /api/admin/audit/posts：管理员查看帖子审核列表，可按状态和关键词筛选。
    @GetMapping("/audit/posts")
    public Result<List<Post>> listPosts(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) String keyword) {
        requireAdmin(operatorId);
        return Result.success(adminService.listPosts(status, keyword));
    }

    @GetMapping("/audit/posts/{id}")
    public Result<Post> getPostDetail(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                      @PathVariable Long id) {
        requireAdmin(operatorId);
        return Result.success(adminService.getPostDetail(id));
    }

    // PUT /api/admin/audit/posts/{id}/review：管理员审核帖子，通过或拒绝。
    @PutMapping("/audit/posts/{id}/review")
    public Result<Void> reviewPost(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                   @PathVariable Long id,
                                   @RequestBody StatusUpdateDTO dto) {
        requireAdmin(operatorId);
        adminService.reviewPost(id, dto);
        return Result.success(null);
    }

    @GetMapping("/audit/videos")
    public Result<List<Post>> listVideos(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) String keyword) {
        requireAdmin(operatorId);
        return Result.success(adminService.listVideos(status, keyword));
    }

    @GetMapping("/audit/videos/{id}")
    public Result<Post> getVideoDetail(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                       @PathVariable Long id) {
        requireAdmin(operatorId);
        return Result.success(adminService.getVideoDetail(id));
    }

    @PutMapping("/audit/videos/{id}/review")
    public Result<Void> reviewVideo(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                    @PathVariable Long id,
                                    @RequestBody StatusUpdateDTO dto) {
        requireAdmin(operatorId);
        adminService.reviewVideo(id, dto);
        return Result.success(null);
    }

    @PutMapping("/videos/{id}/delete")
    public Result<Void> deleteVideo(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                    @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.deleteVideo(id);
        return Result.success(null);
    }

    @PutMapping("/videos/{id}/restore")
    public Result<Void> restoreVideo(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                     @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.restoreVideo(id);
        return Result.success(null);
    }

    // GET /api/admin/audit/comments：管理员查看评论列表，可按状态、帖子和关键词筛选。
    @GetMapping("/audit/comments")
    public Result<List<PostComment>> listComments(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) Long postId,
                                                  @RequestParam(required = false) String keyword) {
        requireAdmin(operatorId);
        return Result.success(adminService.listComments(status, postId, keyword));
    }

    // PUT /api/admin/audit/comments/{id}/hide：审核页面隐藏评论，底层复用 deleteComment 逻辑。
    @PutMapping("/audit/comments/{id}/hide")
    public Result<Void> hideComment(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                    @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.deleteComment(id);
        return Result.success(null);
    }

    // PUT /api/admin/audit/comments/{id}/restore：审核页面恢复评论。
    @PutMapping("/audit/comments/{id}/restore")
    public Result<Void> auditRestoreComment(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                            @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.restoreComment(id);
        return Result.success(null);
    }

    // GET /api/admin/audit/topics：管理员查看话题审核列表。
    @GetMapping("/audit/topics")
    public Result<List<Topic>> listTopics(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) String keyword) {
        requireAdmin(operatorId);
        return Result.success(adminService.listAuditTopics(status, keyword));
    }

    // PUT /api/admin/audit/topics/{id}/review：管理员审核用户提交的话题。
    @PutMapping("/audit/topics/{id}/review")
    public Result<Void> reviewTopic(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                    @PathVariable Long id,
                                    @RequestBody TopicReviewDTO dto) {
        requireAdmin(operatorId);
        adminService.reviewTopic(id, dto);
        return Result.success(null);
    }

    // POST /api/admin/topics：管理员直接创建官方话题。
    @PostMapping("/topics")
    public Result<Void> createTopic(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                    @RequestBody TopicCreateDTO dto) {
        requireAdmin(operatorId);
        adminService.createTopic(dto);
        return Result.success(null);
    }

    // DELETE /api/admin/topics/{id}：管理员删除或下架话题。
    @DeleteMapping("/topics/{id}")
    public Result<Void> deleteTopic(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId,
                                    @PathVariable Long id) {
        requireAdmin(operatorId);
        adminService.deleteTopic(id);
        return Result.success(null);
    }

    // GET /api/admin/appointments：管理员查看全平台预约单。
    @GetMapping("/appointments")
    public Result<List<Appointment>> listAppointments(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId) {
        requireAdmin(operatorId);
        List<Appointment> appointments = adminService.listAppointments();
        return Result.success(appointments);
    }

    // GET /api/admin/product-orders：管理员查看全平台商城订单。
    @GetMapping("/product-orders")
    public Result<List<ProductOrder>> listProductOrders(@RequestHeader(value = USER_ID_HEADER, required = false) Long operatorId) {
        requireAdmin(operatorId);
        List<ProductOrder> orders = adminService.listProductOrders();
        return Result.success(orders);
    }

    private void requireAdmin(Long operatorId) {
        // 每个后台接口执行前统一校验管理员身份，避免把权限判断散落到各个方法里。
        adminService.requireAdmin(operatorId);
    }
}
