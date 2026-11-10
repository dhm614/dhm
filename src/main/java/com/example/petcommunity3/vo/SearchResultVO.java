package com.example.petcommunity3.vo;

import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.entity.ServiceItem;
import com.example.petcommunity3.entity.Store;
import com.example.petcommunity3.entity.Topic;
import lombok.Data;

import java.util.List;

@Data
public class SearchResultVO {
    private List<Post> posts;
    private List<Product> products;
    private List<Store> stores;
    private List<ServiceItem> serviceItems;
    private List<Topic> topics;
}
