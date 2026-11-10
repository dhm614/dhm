package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.FollowActionDTO;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.service.UserFollowService;
import com.example.petcommunity3.vo.FollowCountVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follows")
public class UserFollowController {
    // 关注接口入口：处理关注、取消关注、关注列表、粉丝列表和关注关系判断。
    private final UserFollowService userFollowService;

    // 注入关注业务层，重复关注校验和通知生成都在 Service 中完成。
    public UserFollowController(UserFollowService userFollowService) {
        this.userFollowService = userFollowService;
    }

    // POST /api/follows
    // followerId 表示关注者，followedId 表示被关注者。
    @PostMapping
    public Result<Void> follow(@RequestBody FollowActionDTO dto) {
        userFollowService.follow(dto);
        return Result.success(null);
    }

    // DELETE /api/follows?followerId=1&followedId=2
    // 取消关注，两个用户 id 都从 URL 参数读取。
    @DeleteMapping
    public Result<Void> unfollow(@RequestParam Long followerId, @RequestParam Long followedId) {
        userFollowService.unfollow(followerId, followedId);
        return Result.success(null);
    }

    // GET /api/follows/followings?userId=1
    // 查询 userId 关注了哪些人。
    @GetMapping("/followings")
    public Result<List<User>> listFollowings(@RequestParam Long userId) {
        return Result.success(userFollowService.listFollowings(userId));
    }

    // GET /api/follows/followers?userId=1
    // 查询 userId 的粉丝列表。
    @GetMapping("/followers")
    public Result<List<User>> listFollowers(@RequestParam Long userId) {
        return Result.success(userFollowService.listFollowers(userId));
    }

    // GET /api/follows/count?userId=1
    // 查询关注数和粉丝数。
    @GetMapping("/count")
    public Result<FollowCountVO> count(@RequestParam Long userId) {
        return Result.success(userFollowService.count(userId));
    }

    // GET /api/follows/check?followerId=1&followedId=2
    // 判断 followerId 是否已经关注 followedId，常用于前端按钮状态。
    @GetMapping("/check")
    public Result<Boolean> check(@RequestParam Long followerId, @RequestParam Long followedId) {
        return Result.success(userFollowService.check(followerId, followedId));
    }
}
