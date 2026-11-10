package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.entity.Appointment;
import com.example.petcommunity3.entity.Pet;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.PostComment;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.entity.ProductOrder;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.AppointmentMapper;
import com.example.petcommunity3.mapper.PetMapper;
import com.example.petcommunity3.mapper.PostCommentMapper;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.ProductMapper;
import com.example.petcommunity3.mapper.ProductOrderMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.StatisticsService;
import com.example.petcommunity3.vo.AppointmentConversionVO;
import com.example.petcommunity3.vo.DailyTrendVO;
import com.example.petcommunity3.vo.StatisticsDashboardVO;
import com.example.petcommunity3.vo.StatisticsEnhancedVO;
import com.example.petcommunity3.vo.StatisticsOverviewVO;
import com.example.petcommunity3.vo.StatusCountVO;
import com.example.petcommunity3.vo.TopPostVO;
import com.example.petcommunity3.vo.TopProductVO;
import com.example.petcommunity3.vo.TopTopicVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    // StatisticsServiceImpl 负责后台数据统计：从多个业务表查询数据，再组装成看板 VO。
    private final UserMapper userMapper;
    private final PetMapper petMapper;
    private final PostMapper postMapper;
    private final PostCommentMapper postCommentMapper;
    private final AppointmentMapper appointmentMapper;
    private final ProductMapper productMapper;
    private final ProductOrderMapper productOrderMapper;
    // 统计模块只读取和聚合数据，所以主要依赖各模块 Mapper 做 count、list 和排序查询。
    private final TopicMapper topicMapper;

    public StatisticsServiceImpl(UserMapper userMapper,
                                 PetMapper petMapper,
                                 PostMapper postMapper,
                                 PostCommentMapper postCommentMapper,
                                 AppointmentMapper appointmentMapper,
                                 ProductMapper productMapper,
                                 ProductOrderMapper productOrderMapper,
                                 TopicMapper topicMapper) {
        // 构造方法注入所有需要参与统计的 Mapper。
        this.userMapper = userMapper;
        this.petMapper = petMapper;
        this.postMapper = postMapper;
        this.postCommentMapper = postCommentMapper;
        this.appointmentMapper = appointmentMapper;
        this.productMapper = productMapper;
        this.productOrderMapper = productOrderMapper;
        this.topicMapper = topicMapper;
    }

    @Override
    public StatisticsDashboardVO dashboard() {
        // 后台首页总看板：把总览、排行、状态分布、增强统计一次性组合返回。
        StatisticsDashboardVO vo = new StatisticsDashboardVO();
        vo.setOverview(overview());
        vo.setProductTop10(productTop10());
        vo.setTopicTop10(topicTop10());
        vo.setAppointmentStatus(appointmentStatus());
        vo.setOrderStatus(orderStatus());
        vo.setEnhanced(enhanced(7));
        return vo;
    }

    @Override
    public StatisticsOverviewVO overview() {
        // 基础总览：直接统计各核心表的数量，并汇总有效订单金额。
        StatisticsOverviewVO vo = new StatisticsOverviewVO();
        vo.setUserCount(userMapper.selectCount(null));
        vo.setPetCount(petMapper.selectCount(null));
        vo.setPostCount(postMapper.selectCount(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
        ));
        vo.setCommentCount(postCommentMapper.selectCount(new LambdaQueryWrapper<PostComment>().eq(PostComment::getStatus, 1)));
        vo.setAppointmentCount(appointmentMapper.selectCount(null));
        vo.setProductCount(productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)));
        vo.setOrderCount(productOrderMapper.selectCount(null));
        // 订单总金额不统计已取消订单，所以查询 status != 0 的订单后再求和。
        vo.setTotalOrderAmount(sumOrderAmount(productOrderMapper.selectList(
                new LambdaQueryWrapper<ProductOrder>().ne(ProductOrder::getStatus, 0)
        )));
        return vo;
    }

    @Override
    public List<TopProductVO> productTop10() {
        // 热门商品：按销量倒序取前 10，再转成专门给前端展示的 VO。
        return productMapper.selectList(
                        new LambdaQueryWrapper<Product>()
                                .eq(Product::getStatus, 1)
                                .orderByDesc(Product::getSales)
                                .last("LIMIT 10")
                )
                .stream()
                .map(product -> {
                    TopProductVO vo = new TopProductVO();
                    vo.setProductId(product.getId());
                    vo.setProductName(product.getName());
                    vo.setPrice(product.getPrice());
                    vo.setSales(product.getSales());
                    vo.setStock(product.getStock());
                    return vo;
                })
                .toList();
    }

    @Override
    public List<TopTopicVO> topicTop10() {
        // 热门话题：按话题下帖子数量倒序取前 10。
        return topicMapper.selectList(
                        new LambdaQueryWrapper<Topic>()
                                .eq(Topic::getStatus, 1)
                                .orderByDesc(Topic::getPostCount)
                                .last("LIMIT 10")
                )
                .stream()
                .map(topic -> {
                    TopTopicVO vo = new TopTopicVO();
                    vo.setTopicId(topic.getId());
                    vo.setTopicName(topic.getName());
                    vo.setPostCount(topic.getPostCount());
                    return vo;
                })
                .toList();
    }

    @Override
    public List<StatusCountVO> appointmentStatus() {
        // 预约状态分布：分别统计取消、已预约、已完成和待商户确认。
        return List.of(
                buildAppointmentStatus(0, "已取消"),
                buildAppointmentStatus(1, "已预约"),
                buildAppointmentStatus(2, "已完成"),
                buildAppointmentStatus(3, "待商户确认")
        );
    }

    @Override
    public List<StatusCountVO> orderStatus() {
        // 订单状态分布：分别统计取消、已下单、已完成。
        return List.of(
                buildOrderStatus(0, "已取消"),
                buildOrderStatus(1, "已下单"),
                buildOrderStatus(2, "已完成")
        );
    }

    @Override
    public StatisticsEnhancedVO enhanced(Integer days) {
        // 增强统计：包含今日数据、趋势、预约转化率、热门帖子等更适合图表展示的数据。
        // 计算今天的开始和结束时间，用于筛选今日新增数据。
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<ProductOrder> todayOrders = productOrderMapper.selectList(
                new LambdaQueryWrapper<ProductOrder>()
                        .ge(ProductOrder::getCreateTime, start)
                        .lt(ProductOrder::getCreateTime, end)
        );

        StatisticsEnhancedVO vo = new StatisticsEnhancedVO();
        vo.setTodayActiveUserCount(todayActiveUserCount(start, end));
        vo.setTodayPostCount(postMapper.selectCount(postDayWrapper(start, end)));
        vo.setTodayAppointmentCount(appointmentMapper.selectCount(dayWrapper(Appointment::getCreateTime, start, end)));
        vo.setTodayOrderCount((long) todayOrders.size());
        vo.setTodayOrderAmount(sumOrderAmount(todayOrders.stream()
                .filter(order -> !Integer.valueOf(0).equals(order.getStatus()))
                .toList()));
        vo.setAppointmentConversion(appointmentConversion());
        vo.setDailyTrend(dailyTrend(days));
        vo.setHotPosts(hotPosts(10));
        return vo;
    }

    @Override
    public List<DailyTrendVO> dailyTrend(Integer days) {
        // 每日趋势：days 为空默认 7 天，最多限制 31 天，避免一次查太多。
        int size = days == null ? 7 : Math.max(1, Math.min(days, 31));
        LocalDate startDate = LocalDate.now().minusDays(size - 1L);

        return java.util.stream.IntStream.range(0, size)
                .mapToObj(index -> buildDailyTrend(startDate.plusDays(index)))
                .toList();
    }

    @Override
    public AppointmentConversionVO appointmentConversion() {
        // 预约转化率：统计预约完成率，以及有过预约行为的用户占比。
        Long total = appointmentMapper.selectCount(null);
        Long finished = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>().eq(Appointment::getStatus, 2)
        );
        Long userCount = userMapper.selectCount(null);

        Set<Long> appointmentUsers = new HashSet<>();
        appointmentMapper.selectList(null).forEach(appointment -> {
            if (appointment.getUserId() != null) {
                appointmentUsers.add(appointment.getUserId());
            }
        });

        AppointmentConversionVO vo = new AppointmentConversionVO();
        vo.setTotalAppointmentCount(total);
        vo.setFinishedAppointmentCount(finished);
        vo.setFinishRate(rate(finished, total));
        vo.setUserCount(userCount);
        vo.setUserAppointmentRate(rate((long) appointmentUsers.size(), userCount));
        return vo;
    }

    @Override
    public List<TopPostVO> hotPosts(Integer limit) {
        // 热门帖子：按点赞数、评论数、收藏数综合排序。
        int size = limit == null ? 10 : Math.max(1, Math.min(limit, 50));
        return postMapper.selectList(
                        new LambdaQueryWrapper<Post>()
                                .eq(Post::getStatus, 1)
                                .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
                                .orderByDesc(Post::getCreateTime)
                                .last("LIMIT 100")
                )
                .stream()
                .map(this::buildTopPost)
                .sorted(Comparator.comparing(TopPostVO::getScore).reversed())
                .limit(size)
                .toList();
    }

    private DailyTrendVO buildDailyTrend(LocalDate date) {
        // 构建某一天的趋势数据：把这一天的用户、帖子、预约、订单分别统计出来。
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<ProductOrder> orders = productOrderMapper.selectList(
                new LambdaQueryWrapper<ProductOrder>()
                        .ge(ProductOrder::getCreateTime, start)
                        .lt(ProductOrder::getCreateTime, end)
                        .ne(ProductOrder::getStatus, 0)
        );

        DailyTrendVO vo = new DailyTrendVO();
        vo.setDate(date);
        vo.setUserCount(userMapper.selectCount(dayWrapper(User::getCreateTime, start, end)));
        vo.setPostCount(postMapper.selectCount(postDayWrapper(start, end)));
        vo.setAppointmentCount(appointmentMapper.selectCount(dayWrapper(Appointment::getCreateTime, start, end)));
        vo.setOrderCount((long) orders.size());
        vo.setOrderAmount(sumOrderAmount(orders));
        return vo;
    }

    private Long todayActiveUserCount(LocalDateTime start, LocalDateTime end) {
        // 简化版日活：把今天注册、发帖、预约、下单的用户 ID 合并去重。
        Set<Long> userIds = new HashSet<>();
        userMapper.selectList(dayWrapper(User::getCreateTime, start, end))
                .forEach(user -> userIds.add(user.getId()));
        postMapper.selectList(postDayWrapper(start, end))
                .forEach(post -> userIds.add(post.getUserId()));
        appointmentMapper.selectList(dayWrapper(Appointment::getCreateTime, start, end))
                .forEach(appointment -> userIds.add(appointment.getUserId()));
        productOrderMapper.selectList(dayWrapper(ProductOrder::getCreateTime, start, end))
                .forEach(order -> userIds.add(order.getUserId()));
        userIds.remove(null);
        return (long) userIds.size();
    }

    private TopPostVO buildTopPost(Post post) {
        // 把 Post 实体转成热门帖子 VO，并计算综合热度分。
        int likes = post.getLikeCount() == null ? 0 : post.getLikeCount();
        int comments = post.getCommentCount() == null ? 0 : post.getCommentCount();
        int collects = post.getCollectCount() == null ? 0 : post.getCollectCount();

        TopPostVO vo = new TopPostVO();
        vo.setPostId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setTopic(post.getTopic());
        vo.setLikeCount(likes);
        vo.setCommentCount(comments);
        vo.setCollectCount(collects);
        vo.setScore(likes * 3 + comments * 2 + collects);
        return vo;
    }

    private StatusCountVO buildAppointmentStatus(Integer status, String statusName) {
        // 构建单个预约状态的统计结果。
        StatusCountVO vo = new StatusCountVO();
        vo.setStatus(status);
        vo.setStatusName(statusName);
        vo.setCount(appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>().eq(Appointment::getStatus, status)));
        return vo;
    }

    private StatusCountVO buildOrderStatus(Integer status, String statusName) {
        // 构建单个订单状态的统计结果。
        StatusCountVO vo = new StatusCountVO();
        vo.setStatus(status);
        vo.setStatusName(statusName);
        vo.setCount(productOrderMapper.selectCount(new LambdaQueryWrapper<ProductOrder>().eq(ProductOrder::getStatus, status)));
        return vo;
    }

    private BigDecimal sumOrderAmount(List<ProductOrder> orders) {
        // 对订单金额求和，null 金额按 0 处理。
        return orders.stream()
                .map(ProductOrder::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Double rate(Long numerator, Long denominator) {
        // 统一计算百分比，保留两位小数；分母为空或为 0 时返回 0。
        if (denominator == null || denominator == 0) {
            return 0.0;
        }
        return Math.round((numerator == null ? 0.0 : numerator * 10000.0 / denominator)) / 100.0;
    }

    private LambdaQueryWrapper<Post> postDayWrapper(LocalDateTime start, LocalDateTime end) {
        return new LambdaQueryWrapper<Post>()
                .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
                .ge(Post::getCreateTime, start)
                .lt(Post::getCreateTime, end);
    }

    // 构建某一天的时间范围查询条件，避免每个统计都重复写 ge/lt。
    private <T> LambdaQueryWrapper<T> dayWrapper(com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, LocalDateTime> column,
                                                LocalDateTime start,
                                                LocalDateTime end) {
        return new LambdaQueryWrapper<T>().ge(column, start).lt(column, end);
    }
}
