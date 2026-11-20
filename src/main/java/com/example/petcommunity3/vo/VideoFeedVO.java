package com.example.petcommunity3.vo;

import lombok.Data;

import java.util.List;

@Data
public class VideoFeedVO {
    private List<VideoFeedItemVO> items;
    private String traceId;
    private String strategy;
    private int offset;
    private boolean hasMore;
}
