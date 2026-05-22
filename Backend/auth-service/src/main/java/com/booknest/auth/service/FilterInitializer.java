package com.booknest.auth.service;

import com.booknest.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

// This startup initializer warms up the Redis Bloom Filter with historical email registries.
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class FilterInitializer implements CommandLineRunner {

    /**
     * Repository used to fetch all historical user registrations from SQL.
     */
    private final UserRepository userRepository;

    /**
     * Cache service wrapping RedisBloom command sets.
     */
    private final BloomFilterService bloomFilterService;

    // Entry point for startup execution task.
    @Override
    public void run(String... args) {
        // Step 1: Create the Bloom filter structure in Redis (BF.RESERVE) if it does not already exist.
        bloomFilterService.initializeFilter();

        // Step 2: Warm cache by querying all active users and inserting their lowercase email subjects into the filter.
        log.info("Warming up Bloom Filter with existing user emails...");
        userRepository.findAll().forEach(user -> {
            bloomFilterService.addEmail(user.getEmail());
        });
        log.info("Bloom Filter warm-up complete.");
    }
}
