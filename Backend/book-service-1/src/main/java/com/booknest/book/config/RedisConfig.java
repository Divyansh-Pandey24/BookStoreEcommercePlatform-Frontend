package com.booknest.book.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Cache Configuration for Book Service.
 *
 * Configures Spring's @Cacheable / @CacheEvict to store book data
 * in Redis instead of the JVM heap.
 *
 * Cache strategy:
 *   - books:all       → all active books list    (TTL: 5 min)
 *   - books:id:{id}   → single book by ID        (TTL: 10 min)
 *   - books:featured  → featured books list      (TTL: 10 min)
 *
 * Values are serialized to JSON using Jackson so they survive
 * Redis restarts and are human-readable in RedisInsight.
 */
@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    /**
     * Jackson-based serializer that stores type information.
     * This allows Spring to deserialize the cached JSON back into
     * the correct BookResponse object.
     */
    private GenericJackson2JsonRedisSerializer buildSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Store class type info so deserialization works for List<BookResponse>
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    /**
     * Default Redis cache configuration:
     *   - Keys: plain String
     *   - Values: JSON (with type info)
     *   - Default TTL: 10 minutes
     *   - Null values are NOT cached (prevents caching "not found" as null)
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(buildSerializer())
                )
                .disableCachingNullValues();

        // books:all has a shorter TTL since it changes more frequently
        RedisCacheConfiguration allBooksConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(5));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("books:all", allBooksConfig)
                .withCacheConfiguration("books:featured", defaultConfig)
                .build();
    }
}
