package com.example.petcommunity3.service;

import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.entity.ServiceItem;
import com.example.petcommunity3.entity.Store;
import com.example.petcommunity3.entity.Topic;
import com.example.petcommunity3.vo.SearchResultVO;

import java.util.List;

public interface SearchService {
    // 综合搜索：一次性查询帖子、商品、门店、服务项目、话题并组装返回。
    SearchResultVO searchAll(String keyword, Integer limit);

    // 搜索帖子：按关键词匹配帖子标题或内容。
    List<Post> searchPosts(String keyword, Integer limit);

    // 搜索商品：按关键词匹配商品名称或描述。
    List<Product> searchProducts(String keyword, Integer limit);

    // 搜索门店：按关键词匹配门店名称、地址等信息。
    List<Store> searchStores(String keyword, Integer limit);

    // 搜索服务项目：按关键词匹配洗护、寄养、诊疗等服务。
    List<ServiceItem> searchServiceItems(String keyword, Integer limit);

    // 搜索话题：按关键词匹配社区话题名称。
    List<Topic> searchTopics(String keyword, Integer limit);
}