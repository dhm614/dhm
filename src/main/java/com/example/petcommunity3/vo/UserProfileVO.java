package com.example.petcommunity3.vo;

import com.example.petcommunity3.entity.Pet;
import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.User;
import lombok.Data;

import java.util.List;

@Data
public class UserProfileVO {
    private User user;
    private FollowCountVO followCount;
    private Long postCount;
    private Long petCount;
    private Long likedPostCount;
    private Long collectedPostCount;
    private Boolean followedByViewer;
    private List<Pet> pets;
    private List<Post> recentPosts;
}
