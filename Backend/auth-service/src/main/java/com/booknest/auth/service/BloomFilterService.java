package com.booknest.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Distributed Bloom Filter Service using Redis Stack's RedisBloom module.
 *
 * WHY REDIS BLOOM over Guava (in-memory):
 *   - DISTRIBUTED: All auth-service instances share the SAME filter state.
 *     With Guava, each JVM had its own filter — a user registered on
 *     instance A would be blocked by instance B.
 *   - THREAD-SAFE: Redis is single-threaded; concurrent BF.ADD calls are safe.
 *   - PERSISTENT: The filter survives service restarts (Redis AOF/RDB).
 *   - CHARSET-SAFE: No dependency on JVM's defaultCharset.
 *
 * Redis commands used:
 *   BF.RESERVE  — create the filter with custom capacity and error rate (on startup)
 *   BF.ADD      — add an email to the filter
 *   BF.EXISTS   — check if an email might exist (false positives possible, no false negatives)
 *
 * Filter key  : "booknest:emails:bloom"
 * Capacity    : 100,000 entries
 * Error rate  : 1%
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BloomFilterService {

    private static final String BLOOM_KEY         = "booknest:emails:bloom";
    private static final long   EXPECTED_CAPACITY = 100_000L;
    private static final double ERROR_RATE        = 0.01;

    private final StringRedisTemplate redisTemplate;

    /**
     * Creates the Bloom filter structure in Redis.
     * BF.RESERVE sets capacity and desired error rate.
     * If the key already exists this throws, which we silently ignore —
     * it simply means the filter was already created in a previous run.
     */
    public void initializeFilter() {
        try {
            redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.execute("BF.RESERVE",
                        BLOOM_KEY.getBytes(),
                        String.valueOf(ERROR_RATE).getBytes(),
                        String.valueOf(EXPECTED_CAPACITY).getBytes()
                )
            );
            log.info("Redis Bloom filter created: key={} capacity={} errorRate={}",
                    BLOOM_KEY, EXPECTED_CAPACITY, ERROR_RATE);
        } catch (Exception e) {
            // "ERR item exists" — filter was already created, perfectly fine.
            log.info("Redis Bloom filter already exists — skipping BF.RESERVE. ({})", e.getMessage());
        }
    }

    /**
     * Adds an email to the distributed Bloom filter.
     * Emails are normalised to lowercase before insertion.
     *
     * @param email the user's email address
     */
    public void addEmail(String email) {
        if (email == null || email.isBlank()) return;
        String normalised = email.toLowerCase();

        // Using Lua to avoid Lettuce's ByteArrayOutput issues with integer returns
        String script = "return redis.call('BF.ADD', KEYS[1], ARGV[1])";
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

    /**
     * Checks whether an email MIGHT exist.
     *
     * Returns:
     *   false → email DEFINITELY does NOT exist (skip DB, reject immediately)
     *   true  → email MIGHT exist (proceed to DB — could be a false positive at 1%)
     *
     * @param email the user's email address
     * @return true if the email might exist, false if it definitely does not
     */
    public boolean mightExist(String email) {
        if (email == null || email.isBlank()) return false;
        String normalised = email.toLowerCase();

        // BF.EXISTS returns 1 (Long) if item might be in the set, 0 if definitely not.
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

        boolean exists = Long.valueOf(1L).equals(result);
        log.debug("Bloom filter check [{}]: mightExist={}", normalised, exists);
        return exists;
    }
}
