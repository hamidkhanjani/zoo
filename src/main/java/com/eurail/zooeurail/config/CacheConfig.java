package com.eurail.zooeurail.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final String ANIMALS_BY_ID = "animalsById";
    private static final String ROOMS_BY_ID = "roomsById";
    private static final String FAVORITES_AGG_BY_TITLE = "favoriteRoomsAggByTitle";

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        if ("redis".equalsIgnoreCase(cacheType)) {
            return redisCacheManager(redisConnectionFactory);
        }
        return caffeineCacheManager();
    }

    private CacheManager caffeineCacheManager() {
        // Default to Caffeine with per-cache specs via SimpleCacheManager.
        SimpleCacheManager manager = new SimpleCacheManager();

        CaffeineCache animalsById = new CaffeineCache(ANIMALS_BY_ID,
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(animalsByIdTtl)
                        .build());

        CaffeineCache roomsById = new CaffeineCache(ROOMS_BY_ID,
                Caffeine.newBuilder()
                        .maximumSize(2_000)
                        .expireAfterWrite(roomsByIdTtl)
                        .build());

        CaffeineCache favoriteRoomsAggByTitle = new CaffeineCache(FAVORITES_AGG_BY_TITLE,
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(favoritesAggTtl)
                        .build());

        manager.setCaches(List.of(animalsById, roomsById, favoriteRoomsAggByTitle));
        return manager;
    }

    private CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // Use JSON for values; keep string keys
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        RedisSerializationContext.SerializationPair<Object> valuePair =
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(valuePair)
                .entryTtl(Duration.ofMinutes(10));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(ANIMALS_BY_ID, defaultConfig.entryTtl(animalsByIdTtl));
        cacheConfigs.put(ROOMS_BY_ID, defaultConfig.entryTtl(roomsByIdTtl));
        cacheConfigs.put(FAVORITES_AGG_BY_TITLE, defaultConfig.entryTtl(favoritesAggTtl));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}
