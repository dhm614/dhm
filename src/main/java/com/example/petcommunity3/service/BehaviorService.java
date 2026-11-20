package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.BehaviorEventDTO;

public interface BehaviorService {
    /**
     * 记录一条用户行为流水。userId 由服务端从请求头解析，不信任请求体。
     */
    void record(Long userId, BehaviorEventDTO dto);
}
