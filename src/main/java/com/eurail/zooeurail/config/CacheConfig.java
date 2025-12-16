package com.eurail.zooeurail.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.cache.type:caffeine}")
    private String cacheType;

    // TTLs (can be overridden via properties if needed later)
    @Value("${app.cache.ttl.animals-by-id:10m}")
    private Duration animalsByIdTtl;

    @Value("${app.cache.ttl.rooms-by-id:10m}")
    private Duration roomsByIdTtl;

    @Value("${app.cache.ttl.favorites-agg:60s}")
    private Duration favoritesAggTtl;

    @Bean
    public CacheManager cacheManager() {
        // Default to Caffeine with per-cache specs via SimpleCacheManager.
        SimpleCacheManager manager = new SimpleCacheManager();

        CaffeineCache animalsById = new CaffeineCache("animalsById",
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(animalsByIdTtl)
                        .build());

        CaffeineCache roomsById = new CaffeineCache("roomsById",
                Caffeine.newBuilder()
                        .maximumSize(2_000)
                        .expireAfterWrite(roomsByIdTtl)
                        .build());

        CaffeineCache favoriteRoomsAggByTitle = new CaffeineCache("favoriteRoomsAggByTitle",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(favoritesAggTtl)
                        .build());

        manager.setCaches(List.of(animalsById, roomsById, favoriteRoomsAggByTitle));
        return manager;
    }
}
