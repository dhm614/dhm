package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.LoginDTO;
import com.example.petcommunity3.dto.RegisterDTO;
import com.example.petcommunity3.entity.Pet;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.entity.UserFollow;
import com.example.petcommunity3.mapper.PetMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.UserFollowMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.UserService;
import com.example.petcommunity3.vo.FollowCountVO;
import com.example.petcommunity3.vo.UserProfileVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    // Mapper 是真正访问数据库的对象，ServiceImpl 通过它们完成查询、插入和统计。
    private final UserMapper userMapper;
    private final PetMapper petMapper;
    private final PostMapper postMapper;
    private final UserFollowMapper userFollowMapper;
    // BCrypt 用来加密密码：数据库里保存的是加密结果，不保存用户输入的明文密码。
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper,
                           PetMapper petMapper,
                           PostMapper postMapper,
                           UserFollowMapper userFollowMapper) {
        // 构造方法注入：Spring 创建 UserServiceImpl 时，会把需要的 Mapper 自动传进来。
        this.userMapper = userMapper;
        this.petMapper = petMapper;
        this.postMapper = postMapper;
        this.userFollowMapper = userFollowMapper;
    }

    @Override
    public void register(RegisterDTO dto) {
        // 先校验前端传来的注册数据，避免空用户名或空密码进入数据库。
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new RuntimeException("密码不能为空");
        }

        // 按用户名查一次数据库；如果查到了，说明用户名已经被占用。
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (existUser != null) {
            throw new RuntimeException("用户已存在");
        }

        // 把 RegisterDTO 转成 User 实体类。DTO 是前端输入，User 才对应 sys_user 表。
        User user = new User();
        user.setUsername(dto.getUsername());
        // 密码必须加密后再入库，登录时再用 matches 对比原密码和加密密码。
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        // 注册用户默认是普通用户，并且默认启用。
        user.setRole("USER");
        user.setStatus(1);
        // insert 会根据 User 实体类和 @TableName/@TableId 配置，把数据插入 sys_user 表。
        userMapper.insert(user);
    }

    @Override
    public User login(LoginDTO dto) {
        // 登录只需要用户名和密码，所以这里接收的是 LoginDTO，不直接接收完整 User。
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new RuntimeException("密码不能为空");
        }

        // 根据用户名查询用户，后面要用查出来的加密密码和状态做登录判断。
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new RuntimeException("用户已被禁用");
        }
        // matches(原始密码, 数据库加密密码)：不要自己解密，BCrypt 负责安全比较。
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 返回给前端前清空密码，避免接口响应泄露密码字段。
        user.setPassword(null);
        return user;
    }

    @Override
    public UserProfileVO getProfile(Long userId, Long viewerId) {
        // userId 是被查看的用户；viewerId 是当前访问者，用来判断是否已关注。
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            throw new RuntimeException("用户不存在或已被禁用");
        }
        // 返回给前端前清空密码，避免接口响应泄露密码字段。
        user.setPassword(null);

        // 查询这个用户所有正常展示的帖子，用于统计帖子数、获赞数、收藏数。
        List<Post> userPosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .eq(Post::getStatus, 1)
                        .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
        );

        // 再单独查最近 10 条帖子，作为个人主页的列表展示数据。
        List<Post> recentPosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .eq(Post::getStatus, 1)
                        .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
                        .orderByDesc(Post::getCreateTime)
                        .last("LIMIT 10")
        );

        // 查询用户绑定的宠物档案，个人主页可以同时展示用户和宠物信息。
        List<Pet> pets = petMapper.selectList(
                new LambdaQueryWrapper<Pet>()
                        .eq(Pet::getUserId, userId)
                        .orderByDesc(Pet::getCreateTime)
        );

        // VO 是专门返回给前端的展示对象，把多个表的数据组合到一个结果里。
        UserProfileVO vo = new UserProfileVO();
        vo.setUser(user);
        vo.setFollowCount(buildFollowCount(userId));
        vo.setPostCount((long) userPosts.size());
        vo.setPetCount((long) pets.size());
        vo.setLikedPostCount(sumLikes(userPosts));
        vo.setCollectedPostCount(sumCollects(userPosts));
        vo.setFollowedByViewer(isFollowedByViewer(viewerId, userId));
        vo.setPets(pets);
        vo.setRecentPosts(recentPosts);
        return vo;
    }

    private FollowCountVO buildFollowCount(Long userId) {
        // 关注数和粉丝数来自 user_follow 表，只是查询条件相反。
        FollowCountVO vo = new FollowCountVO();
        vo.setFollowingCount(userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId)
        ));
        vo.setFollowerCount(userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowedId, userId)
        ));
        return vo;
    }

    private Long sumLikes(List<Post> posts) {
        // 把帖子列表里的 likeCount 加起来，null 按 0 处理。
        return posts.stream()
                .mapToLong(post -> post.getLikeCount() == null ? 0L : post.getLikeCount())
                .sum();
    }

    private Long sumCollects(List<Post> posts) {
        // 把帖子列表里的 collectCount 加起来，null 按 0 处理。
        return posts.stream()
                .mapToLong(post -> post.getCollectCount() == null ? 0L : post.getCollectCount())
                .sum();
    }

    private Boolean isFollowedByViewer(Long viewerId, Long userId) {
        // 未登录或查看自己主页时，不需要判断关注关系。
        if (viewerId == null || viewerId.equals(userId)) {
            return false;
        }

        Long count = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, viewerId)
                        .eq(UserFollow::getFollowedId, userId)
        );
        return count > 0;
    }
}
