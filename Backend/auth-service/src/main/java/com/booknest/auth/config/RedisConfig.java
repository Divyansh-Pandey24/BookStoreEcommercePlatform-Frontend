package com.booknest.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis Configuration for the Auth Service.
 *
 * Connects to a Redis Stack instance (which includes the RedisBloom module).
 * The BloomFilterService uses the StringRedisTemplate to send raw
 * BF.ADD and BF.EXISTS commands to the shared Redis Bloom filter.
 *
 * All instances of auth-service share the SAME filter because they all
 * connect to the same Redis server — this is the key advantage over
 * the old in-memory Guava approach.
 */
@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    /**
     * Creates the connection factory pointing to Redis Stack.
     * Lettuce is the default async Redis client bundled with Spring Data Redis.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    /**
     * StringRedisTemplate is a String-typed RedisTemplate.
     * Used by BloomFilterService to execute raw BF commands.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
