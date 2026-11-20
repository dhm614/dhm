package com.example.petcommunity3.service;

import com.example.petcommunity3.entity.RecommendConfig;
import com.example.petcommunity3.mapper.RecommendConfigMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 推荐参数读取与热更新。
 *
 * 参数放在 recommend_config 表，服务端缓存 60 秒；
 * 管理员通过接口改完参数最多 60 秒生效，也可主动失效。
 */
@Service
public class RecommendConfigService {
    private static final long CACHE_TTL_MS = 60_000L;

    private final RecommendConfigMapper configMapper;

    private volatile Snapshot snapshot;

    public RecommendConfigService(RecommendConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public Map<String, String> all() {
        Snapshot current = snapshot;
        long now = System.currentTimeMillis();
        if (current == null || current.expireAt <= now) {
            current = reload();
            snapshot = current;
        }
        return current.values;
    }

    public double getDouble(String key, double defaultValue) {
        String raw = all().get(key);
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public int getInt(String key, int defaultValue) {
        String raw = all().get(key);
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 批量更新参数，写库后立即失效缓存。
     */
    public Map<String, String> update(Map<String, String> patch) {
        if (patch != null) {
            for (Map.Entry<String, String> entry : patch.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key == null || key.trim().isEmpty() || value == null) {
                    continue;
                }
                configMapper.upsert(key.trim(), value.trim(), null);
            }
        }
        snapshot = null;
        return all();
    }

    public void invalidate() {
        snapshot = null;
    }

    private Snapshot reload() {
        Map<String, String> values = new HashMap<>();
        for (RecommendConfig config : configMapper.selectList(null)) {
            if (config.getConfigKey() != null) {
                values.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        return new Snapshot(Collections.unmodifiableMap(values), System.currentTimeMillis() + CACHE_TTL_MS);
    }

    private static final class Snapshot {
        private final Map<String, String> values;
        private final long expireAt;

        private Snapshot(Map<String, String> values, long expireAt) {
            this.values = values;
            this.expireAt = expireAt;
        }
    }
}
