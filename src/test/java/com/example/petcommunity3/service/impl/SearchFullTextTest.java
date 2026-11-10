package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.mapper.PostMapper;
import com.example.petcommunity3.mapper.ProductMapper;
import com.example.petcommunity3.mapper.ServiceItemMapper;
import com.example.petcommunity3.mapper.StoreMapper;
import com.example.petcommunity3.mapper.TopicMapper;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchFullTextTest {

    @Test
    void postSearchUsesFullTextMapperMethod() {
        PostMapper postMapper = mock(PostMapper.class);
        Post post = new Post();
        post.setId(7L);
        when(postMapper.searchApprovedByFullText("猫粮", 5)).thenReturn(List.of(post));
        SearchServiceImpl service = service(postMapper, mock(ProductMapper.class));

        List<Post> result = service.searchPosts("猫粮", 5);

        assertThat(result).containsExactly(post);
        verify(postMapper).searchApprovedByFullText("猫粮", 5);
    }

    @Test
    void postFullTextSearchOnlyQueriesNormalPosts() throws NoSuchMethodException {
        org.apache.ibatis.annotations.Select select = PostMapper.class
                .getMethod("searchApprovedByFullText", String.class, int.class)
                .getAnnotation(org.apache.ibatis.annotations.Select.class);

        assertThat(String.join(" ", select.value())).contains("content_type = 'POST'");
    }

    @Test
    void postSearchLikeFallbackOnlyQueriesNormalPosts() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(PostMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, Post.class);

        PostMapper postMapper = mock(PostMapper.class);
        when(postMapper.searchApprovedByFullText("猫粮", 5)).thenReturn(List.of());
        when(postMapper.selectList(any())).thenReturn(List.of());
        SearchServiceImpl service = service(postMapper, mock(ProductMapper.class));

        service.searchPosts("猫粮", 5);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<Post>> wrapperCaptor = org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(postMapper).selectList(wrapperCaptor.capture());
        wrapperCaptor.getValue().getSqlSegment();
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs()).containsValue(Post.CONTENT_TYPE_POST);
    }

    @Test
    void productSearchUsesFullTextMapperMethod() {
        ProductMapper productMapper = mock(ProductMapper.class);
        Product product = new Product();
        product.setId(8L);
        when(productMapper.searchAvailableByFullText("零食", 6)).thenReturn(List.of(product));
        SearchServiceImpl service = service(mock(PostMapper.class), productMapper);

        List<Product> result = service.searchProducts("零食", 6);

        assertThat(result).containsExactly(product);
        verify(productMapper).searchAvailableByFullText("零食", 6);
    }

    private SearchServiceImpl service(PostMapper postMapper, ProductMapper productMapper) {
        return new SearchServiceImpl(
                postMapper,
                productMapper,
                mock(StoreMapper.class),
                mock(ServiceItemMapper.class),
                mock(TopicMapper.class)
        );
    }
}
