package com.example.petcommunity3.service;

import java.util.concurrent.Callable;

public interface AppointmentSlotLockService {
    <T> T executeWithLock(String lockKey, Callable<T> action);
}
