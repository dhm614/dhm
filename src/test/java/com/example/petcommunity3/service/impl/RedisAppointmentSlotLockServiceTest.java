package com.example.petcommunity3.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisAppointmentSlotLockServiceTest {

    @Test
    void usesLocalLockWhenRedisTemplateIsAbsent() {
        @SuppressWarnings("unchecked")
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        RedisAppointmentSlotLockService service = new RedisAppointmentSlotLockService(provider);
        ReflectionTestUtils.setField(service, "ttlSeconds", 1L);
        when(provider.getIfAvailable()).thenReturn(null);

        String result = service.executeWithLock("schedule:1", () -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void rejectsRequestWhenRedisLockIsAlreadyHeld() {
        @SuppressWarnings("unchecked")
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        RedisAppointmentSlotLockService service = new RedisAppointmentSlotLockService(provider);
        ReflectionTestUtils.setField(service, "ttlSeconds", 1L);

        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.setIfAbsent(
                eq("petcommunity:appointment:slot:schedule:1"),
                anyString(),
                any(Duration.class)
        )).thenReturn(false);

        assertThatThrownBy(() -> service.executeWithLock("schedule:1", () -> "ok"))
                .hasMessage("该预约时间正在锁定中，请稍后再试");
    }
}
