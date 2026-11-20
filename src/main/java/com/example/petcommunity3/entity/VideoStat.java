package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("video_stat")
public class VideoStat {
    @TableId(type = IdType.INPUT)
    private Long videoId;

    private Integer impCnt;
    private Integer playCnt;
    private Integer playEndCnt;
    private Integer likeCnt;
    private Integer commentCnt;
    private Integer collectCnt;
    private Integer shareCnt;
    private BigDecimal avgWatchRatio;
    private BigDecimal hotScore;
    private LocalDateTime updateTime;

    public static VideoStat empty(Long videoId) {
        VideoStat stat = new VideoStat();
        stat.setVideoId(videoId);
        stat.setImpCnt(0);
        stat.setPlayCnt(0);
        stat.setPlayEndCnt(0);
        stat.setLikeCnt(0);
        stat.setCommentCnt(0);
        stat.setCollectCnt(0);
        stat.setShareCnt(0);
        stat.setAvgWatchRatio(BigDecimal.ZERO);
        stat.setHotScore(BigDecimal.ZERO);
        return stat;
    }
}
