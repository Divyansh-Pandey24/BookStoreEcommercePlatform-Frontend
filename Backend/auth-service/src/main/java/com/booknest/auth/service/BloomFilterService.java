package com.booknest.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

// This service implements a distributed, thread-safe, persistent Bloom Filter cache using Redis Stack's RedisBloom module.
@Service
@RequiredArgsConstructor
@Slf4j
public class BloomFilterService {

    /**
     * Unique key identifier for the Bloom Filter structure in Redis.
     */
    private static final String BLOOM_KEY         = "booknest:emails:bloom";
    
    /**
     * Target capacity (100,000 slots) reserved for email storage to maintain optimal density.
     */
    private static final long   EXPECTED_CAPACITY = 100_000L;
    
    /**
     * Acceptable false-positive error rate threshold (1.0%). As capacity grows, this bounds collision chances.
     */
    private static final double ERROR_RATE        = 0.01;

    /**
     * Spring Redis utility class managing connection routing and string deserialisation.
     */
    private final StringRedisTemplate redisTemplate;

    // Reserves memory and configures parameters for the Redis Bloom Filter.
    public void initializeFilter() {
        try {
            // Execute native Redis command BF.RESERVE using dynamic byte array arguments.
            redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.execute("BF.RESERVE",
                        BLOOM_KEY.getBytes(),
                        String.valueOf(ERROR_RATE).getBytes(),
                        String.valueOf(EXPECTED_CAPACITY).getBytes()
                )
            );
            // Log successful creation of the Bloom filter context.
            log.info("Redis Bloom filter created: key={} capacity={} errorRate={}",
                    BLOOM_KEY, EXPECTED_CAPACITY, ERROR_RATE);
        } catch (Exception e) {
            // Ignore key-already-exists error as the filter persists across application Restarts.
            log.info("Redis Bloom filter already exists — skipping BF.RESERVE. ({})", e.getMessage());
        }
    }

    // Normalizes and inserts a new email address into the Redis Bloom Filter.
    public void addEmail(String email) {
        // Abort execution if parameters are invalid.
        if (email == null || email.isBlank()) return;
        
        // Normalize email to lowercase to guarantee case-insensitive comparisons and prevent duplicate states.
        String normalised = email.toLowerCase();

        // Prepare Lua script to return native Redis BF.ADD execution responses.
        String script = "return redis.call('BF.ADD', KEYS[1], ARGV[1])";
        
        // Execute the script on connection commands, mapping outputs as integer metrics.
        redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.scriptingCommands().eval(
                        script.getBytes(),
                        org.springframework.data.redis.connection.ReturnType.INTEGER,
                        1,
                        BLOOM_KEY.getBytes(),
                        normalised.getBytes()
                )
        );
        log.debug("Added to Bloom filter: {}", normalised);
    }

    // Verifies whether an email might have been registered previously (guarantees zero false negatives).
    public boolean mightExist(String email) {
        // Return false directly if the query email context is invalid.
        if (email == null || email.isBlank()) return false;
        
        // Case normalization matching the addEmail pipeline.
        String normalised = email.toLowerCase();

        // Invoke BF.EXISTS command via Lua script to check the bit arrays.
        String script = "return redis.call('BF.EXISTS', KEYS[1], ARGV[1])";
        Object result = redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.scriptingCommands().eval(
                        script.getBytes(),
                        org.springframework.data.redis.connection.ReturnType.INTEGER,
                        1,
                        BLOOM_KEY.getBytes(),
                        normalised.getBytes()
                )
        );

        // Compare response: 1L represents potential hit, 0L representing absolute absence.
        boolean exists = Long.valueOf(1L).equals(result);
        log.debug("Bloom filter check [{}]: mightExist={}", normalised, exists);
        return exists;
    }
}
