package com.example.petcommunity3.service;

import com.example.petcommunity3.entity.RecommendConfig;
import com.example.petcommunity3.mapper.RecommendConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 参数层：解析、缺省回退、缓存与热更新。
 */
@ExtendWith(MockitoExtension.class)
class RecommendConfigServiceTest {

    @Mock
    private RecommendConfigMapper configMapper;

    @Test
    void shouldParseValuesAndFallBackToDefault() {
        when(configMapper.selectList(any())).thenReturn(List.of(
                row("weight.interest", "0.7"),
                row("diversity.max_per_author", "3"),
                row("broken", "not-a-number")));

        RecommendConfigService service = new RecommendConfigService(configMapper);

        assertEquals(0.7d, service.getDouble("weight.interest", 0.5d), 1e-9);
        assertEquals(3, service.getInt("diversity.max_per_author", 2));
        assertEquals(0.5d, service.getDouble("missing", 0.5d), 1e-9);
        assertEquals(9, service.getInt("broken", 9));
    }

    @Test
    void shouldCacheWithinTtl() {
        when(configMapper.selectList(any())).thenReturn(List.of(row("weight.interest", "0.7")));

        RecommendConfigService service = new RecommendConfigService(configMapper);
        service.getDouble("weight.interest", 0d);
        service.getDouble("weight.interest", 0d);
        service.all();

        verify(configMapper, times(1)).selectList(any());
    }

    @Test
    void updateShouldUpsertOnlyProvidedKeysAndRefreshCache() {
        when(configMapper.selectList(any())).thenReturn(List.of(row("weight.interest", "0.7")));

        RecommendConfigService service = new RecommendConfigService(configMapper);
        service.all();

        Map<String, String> result = service.update(Map.of("weight.interest", "0.9", " ", "ignored"));

        verify(configMapper, times(1)).upsert(org.mockito.ArgumentMatchers.eq("weight.interest"),
                org.mockito.ArgumentMatchers.eq("0.9"), isNull());
        assertEquals(0.7d, Double.parseDouble(result.get("weight.interest")), 1e-9);
    }

    private static RecommendConfig row(String key, String value) {
        RecommendConfig config = new RecommendConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}
