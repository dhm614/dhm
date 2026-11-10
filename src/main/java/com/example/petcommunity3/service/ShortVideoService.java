package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.ShortVideoCreateDTO;
import com.example.petcommunity3.entity.Post;

import java.util.List;

public interface ShortVideoService {
    void create(ShortVideoCreateDTO dto);

    List<Post> list(Integer limit);

    List<Post> listByUserId(Long userId);

    Post getDetail(Long id);
}
