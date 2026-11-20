package com.example.petcommunity3.vo;

import lombok.Data;

/**
 * 候选视频与用户近期正反馈内容的相似度（ItemCF），用于打分时的“短期兴趣”信号。
 */
@Data
public class SimilarScoreVO {
    private Long videoId;
    private Double score;
}
