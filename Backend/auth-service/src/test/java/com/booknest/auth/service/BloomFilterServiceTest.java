package com.booknest.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.RedisScriptingCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BloomFilterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private RedisScriptingCommands redisScriptingCommands;

    @InjectMocks
    private BloomFilterService bloomFilterService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void initializeFilter_Success() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<Object> callback = invocation.getArgument(0);
            return callback.doInRedis(redisConnection);
        });

        bloomFilterService.initializeFilter();

        verify(redisConnection, times(1)).execute(anyString(), any(), any(), any());
    }

    @Test
    void initializeFilter_ThrowsException() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenThrow(new RuntimeException("ERR item exists"));

        // Should not throw an exception, but catch it and log
        assertDoesNotThrow(() -> bloomFilterService.initializeFilter());
    }

    @Test
    void addEmail_Success() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<Object> callback = invocation.getArgument(0);
            when(redisConnection.scriptingCommands()).thenReturn(redisScriptingCommands);
            return callback.doInRedis(redisConnection);
        });

        bloomFilterService.addEmail("Test@Example.com");

        verify(redisScriptingCommands, times(1)).eval(any(), eq(ReturnType.INTEGER), eq(1), any(), any());
    }

    @Test
    void addEmail_NullOrEmpty() {
        bloomFilterService.addEmail(null);
        bloomFilterService.addEmail("");
        verify(redisTemplate, never()).execute(any(RedisCallback.class));
    }

    @Test
    void mightExist_True() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<Object> callback = invocation.getArgument(0);
            when(redisConnection.scriptingCommands()).thenReturn(redisScriptingCommands);
            when(redisScriptingCommands.eval(any(), eq(ReturnType.INTEGER), eq(1), any(), any())).thenReturn(1L);
            return callback.doInRedis(redisConnection);
        });

        assertTrue(bloomFilterService.mightExist("test@example.com"));
    }

    @Test
    void mightExist_False() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<Object> callback = invocation.getArgument(0);
            when(redisConnection.scriptingCommands()).thenReturn(redisScriptingCommands);
            when(redisScriptingCommands.eval(any(), eq(ReturnType.INTEGER), eq(1), any(), any())).thenReturn(0L);
            return callback.doInRedis(redisConnection);
        });

        assertFalse(bloomFilterService.mightExist("test@example.com"));
    }

    @Test
    void mightExist_NullOrEmpty() {
        assertFalse(bloomFilterService.mightExist(null));
        assertFalse(bloomFilterService.mightExist(""));
        verify(redisTemplate, never()).execute(any(RedisCallback.class));
    }
}
