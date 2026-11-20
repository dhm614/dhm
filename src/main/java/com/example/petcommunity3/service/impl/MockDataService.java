package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.entity.UserBehavior;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.UserBehaviorMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.RecommendAggregateService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 生成推荐模块的模拟数据（仅用于联调与效果验证）。
 * 所有模拟视频标题带 [MOCK] 前缀，模拟用户名为 mock_u 前缀，可整批清理。
 */
@Service
public class MockDataService {
    private static final String MOCK_USER_PREFIX = "mock_u";
    private static final String MOCK_TITLE_PREFIX = "[MOCK] ";
    private static final String[] TOPICS = {"新手养猫", "狗狗训练", "宠物医疗"};
    private static final String[] VIDEO_URLS = {
            "http://localhost:9000/pet-community/short-video/01b05d54f3164cc48d0c1e1020a6c380.mp4",
            "http://localhost:9000/pet-community/short-video/e5b928f9cd0a4dfd9c6170a5bbc2db3b.mp4",
            "http://localhost:9000/pet-community/short-video/4eed71e20931485f9f13234a4717a7f5.mp4"
    };
    private static final String[] COVERS = {
            "http://localhost:9000/pet-community/short-video-cover/24a7f158486f448585e3466b475a21d6.png",
            "http://localhost:9000/pet-community/short-video-cover/e1155505a3b6480dac31c87a67aab781.png",
            "http://localhost:9000/pet-community/short-video-cover/3a821ec530ee406a91b03e25ef15a882.png"
    };
    private static final int BEHAVIOR_BATCH = 500;
    private static final int MOCK_VIDEO_SECONDS = 15;

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final UserBehaviorMapper behaviorMapper;
    private final RecommendAggregateService aggregateService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public MockDataService(UserMapper userMapper,
                           PostMapper postMapper,
                           UserBehaviorMapper behaviorMapper,
                           RecommendAggregateService aggregateService) {
        this.userMapper = userMapper;
        this.postMapper = postMapper;
        this.behaviorMapper = behaviorMapper;
        this.aggregateService = aggregateService;
    }

    public Map<String, Object> generate(int userCount, int videoCount, int behaviorCount) {
        int users = Math.max(1, Math.min(userCount, 200));
        int videos = Math.max(1, Math.min(videoCount, 2000));
        int behaviors = Math.max(0, Math.min(behaviorCount, 200_000));

        cleanup();

        List<User> mockUsers = createUsers(users);
        List<Post> mockVideos = createVideos(videos, mockUsers);
        int behaviorRows = createBehaviors(behaviors, mockUsers, mockVideos);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", mockUsers.size());
        result.put("videos", mockVideos.size());
        result.put("behaviors", behaviorRows);
        result.put("aggregate", aggregateService.refreshAll());
        return result;
    }

    private void cleanup() {
        List<Post> oldVideos = postMapper.selectList(
                new LambdaQueryWrapper<Post>().like(Post::getTitle, MOCK_TITLE_PREFIX));
        if (!oldVideos.isEmpty()) {
            List<Long> videoIds = oldVideos.stream().map(Post::getId).toList();
            behaviorMapper.delete(new LambdaQueryWrapper<UserBehavior>()
                    .in(UserBehavior::getTargetId, videoIds));
            postMapper.deleteBatchIds(videoIds);
        }

        List<User> oldUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().likeRight(User::getUsername, MOCK_USER_PREFIX));
        if (!oldUsers.isEmpty()) {
            List<Long> userIds = oldUsers.stream().map(User::getId).toList();
            behaviorMapper.delete(new LambdaQueryWrapper<UserBehavior>()
                    .in(UserBehavior::getUserId, userIds));
            userMapper.deleteBatchIds(userIds);
        }
    }

    private List<User> createUsers(int count) {
        List<User> users = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            User user = new User();
            user.setUsername(MOCK_USER_PREFIX + String.format("%02d", i));
            user.setPassword(passwordEncoder.encode("123456"));
            user.setNickname("模拟用户" + String.format("%02d", i));
            user.setRole("USER");
            user.setStatus(1);
            userMapper.insert(user);
            users.add(user);
        }
        return users;
    }

    private List<Post> createVideos(int count, List<User> users) {
        List<Post> posts = new ArrayList<>(count);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= count; i++) {
            Post post = new Post();
            post.setUserId(users.get((i - 1) % users.size()).getId());
            post.setTitle(MOCK_TITLE_PREFIX + "萌宠日常 " + i);
            post.setContent("模拟视频简介 " + i + "，用于推荐算法联调。");
            post.setCover(COVERS[i % COVERS.length]);
            post.setTopic(TOPICS[(i - 1) % TOPICS.length]);
            post.setContentType(Post.CONTENT_TYPE_VIDEO);
            post.setVideoUrl(VIDEO_URLS[i % VIDEO_URLS.length]);
            post.setLikeCount(0);
            post.setCommentCount(0);
            post.setCollectCount(0);
            post.setIsPinned(0);
            post.setIsFeatured(0);
            post.setStatus(1);
            post.setCreateTime(now.minusMinutes(ThreadLocalRandom.current().nextInt(0, 30 * 24 * 60)));
            postMapper.insert(post);
            posts.add(post);
        }
        return posts;
    }

    /**
     * 让每个用户有明确偏好：按序号分组，75% 的行为落在本组话题的视频上，
     * 25% 随机探索，模拟真实兴趣分布。
     */
    private int createBehaviors(int total, List<User> users, List<Post> videos) {
        if (total == 0 || users.isEmpty() || videos.isEmpty()) {
            return 0;
        }
        Map<String, List<Post>> videosByTopic = new HashMap<>();
        for (Post post : videos) {
            videosByTopic.computeIfAbsent(post.getTopic(), k -> new ArrayList<>()).add(post);
        }

        int perUser = Math.max(1, total / users.size());
        List<UserBehavior> batch = new ArrayList<>(BEHAVIOR_BATCH);
        int inserted = 0;
        LocalDateTime now = LocalDateTime.now();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int u = 0; u < users.size(); u++) {
            User user = users.get(u);
            String preferredTopic = TOPICS[u % TOPICS.length];
            List<Post> preferred = videosByTopic.getOrDefault(preferredTopic, videos);

            for (int i = 0; i < perUser; i++) {
                Post target = random.nextDouble() < 0.75
                        ? preferred.get(random.nextInt(preferred.size()))
                        : videos.get(random.nextInt(videos.size()));

                String action = pickAction(random);
                double ratio = watchRatio(action, random);

                UserBehavior behavior = new UserBehavior();
                behavior.setUserId(user.getId());
                behavior.setTargetType("VIDEO");
                behavior.setTargetId(target.getId());
                behavior.setAction(action);
                behavior.setWatchMs((int) Math.round(ratio * MOCK_VIDEO_SECONDS * 1000));
                behavior.setWatchRatio(BigDecimal.valueOf(ratio).setScale(3, RoundingMode.HALF_UP));
                behavior.setScene("FEED");
                behavior.setTraceId(null);
                behavior.setCreateTime(now.minusMinutes(random.nextInt(0, 7 * 24 * 60)));
                batch.add(behavior);

                if (batch.size() >= BEHAVIOR_BATCH) {
                    behaviorMapper.insertBatch(batch);
                    inserted += batch.size();
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            behaviorMapper.insertBatch(batch);
            inserted += batch.size();
        }
        return inserted;
    }

    private String pickAction(ThreadLocalRandom random) {
        int value = random.nextInt(100);
        if (value < 30) {
            return UserBehavior.ACTION_IMPRESSION;
        }
        if (value < 55) {
            return UserBehavior.ACTION_PLAY;
        }
        if (value < 75) {
            return UserBehavior.ACTION_PLAY_END;
        }
        if (value < 85) {
            return UserBehavior.ACTION_LIKE;
        }
        if (value < 90) {
            return UserBehavior.ACTION_COMMENT;
        }
        if (value < 95) {
            return UserBehavior.ACTION_COLLECT;
        }
        if (value < 98) {
            return UserBehavior.ACTION_SHARE;
        }
        return UserBehavior.ACTION_SKIP;
    }

    private double watchRatio(String action, ThreadLocalRandom random) {
        return switch (action) {
            case UserBehavior.ACTION_PLAY_END -> 0.9 + random.nextDouble() * 0.1;
            case UserBehavior.ACTION_PLAY -> 0.3 + random.nextDouble() * 0.55;
            case UserBehavior.ACTION_SKIP -> 0.02 + random.nextDouble() * 0.13;
            default -> 0d;
        };
    }
}
