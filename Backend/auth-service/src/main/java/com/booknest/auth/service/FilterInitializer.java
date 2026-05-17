package com.booknest.auth.service;

import com.booknest.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes the Redis Bloom Filter on application startup.
 *
 * Two steps are performed in order:
 *   1. BF.RESERVE — create the filter in Redis (no-op if it already exists).
 *   2. Warm-up    — load all existing user emails from the DB into the filter.
 *
 * Without step 2, existing users would be blocked from logging in after
 * a Redis flush (e.g., cache clear or first-time setup).
 */
import org.springframework.context.annotation.Profile;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class FilterInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BloomFilterService bloomFilterService;

    @Override
    public void run(String... args) {
        // Step 1: Create the Bloom filter structure in Redis (if not already there)
        bloomFilterService.initializeFilter();

        // Step 2: Load existing emails from DB into the filter
        log.info("Warming up Bloom Filter with existing user emails...");
        userRepository.findAll().forEach(user -> {
            bloomFilterService.addEmail(user.getEmail());
        });
        log.info("Bloom Filter warm-up complete.");
    }
}
