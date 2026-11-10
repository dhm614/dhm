package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.FollowActionDTO;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.vo.FollowCountVO;

import java.util.List;

public interface UserFollowService {
    // 关注用户：写入 user_follow 表，并给被关注者生成通知。
    void follow(FollowActionDTO dto);

    // 取消关注：根据关注者和被关注者删除关注关系。
    void unfollow(Long followerId, Long followedId);

    // 查询关注列表：查询 userId 主动关注了哪些用户。
    List<User> listFollowings(Long userId);

    // 查询粉丝列表：查询哪些用户关注了 userId。
    List<User> listFollowers(Long userId);

    // 统计关注数和粉丝数。
    FollowCountVO count(Long userId);

    // 判断关注关系：用于前端决定显示“关注”还是“已关注”。
    Boolean check(Long followerId, Long followedId);
}
