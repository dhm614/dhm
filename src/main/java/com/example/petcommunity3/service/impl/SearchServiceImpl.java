package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.entity.ServiceItem;
import com.example.petcommunity3.entity.Store;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.ProductMapper;
import com.example.petcommunity3.mapper.ServiceItemMapper;
import com.example.petcommunity3.mapper.StoreMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import com.example.petcommunity3.service.SearchService;
import com.example.petcommunity3.vo.SearchResultVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {
    // 当前搜索模块使用数据库 like 查询实现，适合毕业设计演示；后续可替换为 Elasticsearch。
    private final PostMapper postMapper;
    private final ProductMapper productMapper;
    private final StoreMapper storeMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final TopicMapper topicMapper;

    public SearchServiceImpl(PostMapper postMapper,
                             ProductMapper productMapper,
                             StoreMapper storeMapper,
                             ServiceItemMapper serviceItemMapper,
                             TopicMapper topicMapper) {
        // 构造方法注入各模块 Mapper，综合搜索时会分别查多个表。
        this.postMapper = postMapper;
        this.productMapper = productMapper;
        this.storeMapper = storeMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.topicMapper = topicMapper;
    }

    @Override
    public SearchResultVO searchAll(String keyword, Integer limit) {
        // 综合搜索：分别调用下面的分类搜索方法，再组装成一个 SearchResultVO。
        SearchResultVO vo = new SearchResultVO();
        vo.setPosts(searchPosts(keyword, limit));
        vo.setProducts(searchProducts(keyword, limit));
        vo.setStores(searchStores(keyword, limit));
        vo.setServiceItems(searchServiceItems(keyword, limit));
        vo.setTopics(searchTopics(keyword, limit));
        return vo;
    }

    @Override
    public List<Post> searchPosts(String keyword, Integer limit) {
        // 帖子搜索：只查正常展示的帖子，并匹配标题、正文、话题。
        String word = normalizeKeyword(keyword);
        try {
            List<Post> posts = postMapper.searchApprovedByFullText(fullTextKeyword(word), limitValue(limit));
            if (!posts.isEmpty()) {
                return posts;
            }
        } catch (RuntimeException ignored) {
        }
        return searchPostsByLike(word, limit);
    }

    @Override
    public List<Product> searchProducts(String keyword, Integer limit) {
        // 商品搜索：只查上架商品，并匹配名称、分类、描述。
        String word = normalizeKeyword(keyword);
        try {
            List<Product> products = productMapper.searchAvailableByFullText(fullTextKeyword(word), limitValue(limit));
            if (!products.isEmpty()) {
                return products;
            }
        } catch (RuntimeException ignored) {
        }
        return searchProductsByLike(word, limit);
    }

    @Override
    public List<Store> searchStores(String keyword, Integer limit) {
        // 门店搜索：只查启用门店，并匹配门店名称、地址、描述。
        String word = normalizeKeyword(keyword);
        return storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .eq(Store::getStatus, 1)
                .and(wrapper -> wrapper.like(Store::getName, word)
                        .or().like(Store::getAddress, word)
                        .or().like(Store::getDescription, word))
                .orderByDesc(Store::getCreateTime)
                .last(limitSql(limit)));
    }

    @Override
    public List<ServiceItem> searchServiceItems(String keyword, Integer limit) {
        // 服务项目搜索：只查上架服务，并匹配名称、分类、描述。
        String word = normalizeKeyword(keyword);
        return serviceItemMapper.selectList(new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getStatus, 1)
                .and(wrapper -> wrapper.like(ServiceItem::getName, word)
                        .or().like(ServiceItem::getCategory, word)
                        .or().like(ServiceItem::getDescription, word))
                .orderByDesc(ServiceItem::getCreateTime)
                .last(limitSql(limit)));
    }

    @Override
    public List<Topic> searchTopics(String keyword, Integer limit) {
        // 话题搜索：只查启用话题，并匹配话题名称和描述。
        String word = normalizeKeyword(keyword);
        return topicMapper.selectList(new LambdaQueryWrapper<Topic>()
                .eq(Topic::getStatus, 1)
                .and(wrapper -> wrapper.like(Topic::getName, word)
                        .or().like(Topic::getDescription, word))
                .orderByDesc(Topic::getSort)
                .last(limitSql(limit)));
    }

    private String normalizeKeyword(String keyword) {
        // 搜索关键词不能为空；trim 去掉首尾空格，避免无意义搜索。
        if (!StringUtils.hasText(keyword)) {
            throw new RuntimeException("搜索关键词不能为空");
        }
        return keyword.trim();
    }

    private List<Post> searchPostsByLike(String word, Integer limit) {
        return postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .eq(Post::getContentType, Post.CONTENT_TYPE_POST)
                .and(wrapper -> wrapper.like(Post::getTitle, word)
                        .or().like(Post::getContent, word)
                        .or().like(Post::getTopic, word))
                .orderByDesc(Post::getCreateTime)
                .last(limitSql(limit)));
    }

    private List<Product> searchProductsByLike(String word, Integer limit) {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .and(wrapper -> wrapper.like(Product::getName, word)
                        .or().like(Product::getCategory, word)
                        .or().like(Product::getDescription, word))
                .orderByDesc(Product::getSales)
                .last(limitSql(limit)));
    }

    private String fullTextKeyword(String word) {
        String value = word.replaceAll("[+\\-<>()~*\"@]", " ").trim();
        return value.isBlank() ? word : value;
    }

    private int limitValue(Integer limit) {
        int value = limit == null ? 10 : limit;
        return Math.max(1, Math.min(value, 50));
    }

    private String limitSql(Integer limit) {
        // 限制每类最多返回 50 条，避免一个搜索请求返回过多数据。
        return "LIMIT " + limitValue(limit);
    }
}
