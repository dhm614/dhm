package com.example.petcommunity3.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AggregateResultVO {
    private int statRows;
    private int tagRows;
    private int similarRows;
    private long costMs;
    private LocalDateTime finishedAt;
}
