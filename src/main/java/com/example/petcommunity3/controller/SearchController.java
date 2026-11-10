package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.entity.ServiceItem;
import com.example.petcommunity3.entity.Store;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.service.SearchService;
import com.example.petcommunity3.vo.SearchResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    // 搜索接口入口：把关键词交给 SearchService，同时搜索帖子、商品、门店、服务项目和话题。
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        // 构造方法注入搜索服务，具体搜索条件和数据库查询放在 Service 层。
        this.searchService = searchService;
    }

    // GET /api/search：综合搜索，返回多个分类结果。
    @GetMapping
    public Result<SearchResultVO> searchAll(@RequestParam String keyword,
                                            @RequestParam(required = false) Integer limit) {
        return Result.success(searchService.searchAll(keyword, limit));
    }

    // GET /api/search/posts：只搜索帖子。
    @GetMapping("/posts")
    public Result<List<Post>> searchPosts(@RequestParam String keyword,
                                          @RequestParam(required = false) Integer limit) {
        return Result.success(searchService.searchPosts(keyword, limit));
    }

    // GET /api/search/products：只搜索商品。
    @GetMapping("/products")
    public Result<List<Product>> searchProducts(@RequestParam String keyword,
                                                @RequestParam(required = false) Integer limit) {
        return Result.success(searchService.searchProducts(keyword, limit));
    }

    // GET /api/search/stores：只搜索门店。
    @GetMapping("/stores")
    public Result<List<Store>> searchStores(@RequestParam String keyword,
                                            @RequestParam(required = false) Integer limit) {
        return Result.success(searchService.searchStores(keyword, limit));
    }

    // GET /api/search/service-items：只搜索服务项目。
    @GetMapping("/service-items")
    public Result<List<ServiceItem>> searchServiceItems(@RequestParam String keyword,
                                                        @RequestParam(required = false) Integer limit) {
        return Result.success(searchService.searchServiceItems(keyword, limit));
    }

    // GET /api/search/topics：只搜索话题。
    @GetMapping("/topics")
    public Result<List<Topic>> searchTopics(@RequestParam String keyword,
                                            @RequestParam(required = false) Integer limit) {
        return Result.success(searchService.searchTopics(keyword, limit));
    }
}