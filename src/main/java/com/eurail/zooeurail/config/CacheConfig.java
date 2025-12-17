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

/**
 * Cache configuration for the Zoo application.
 * <p>
 * This configuration enables caching and provides a flexible cache manager
 * that can be backed by either Caffeine (in-memory) or Redis, depending on
 * the {@code app.cache.type} property.
 * <p>
 * Three caches are configured with customizable TTLs:
 * <ul>
 *   <li>{@code animalsById} - Caches animal entities by ID</li>
 *   <li>{@code roomsById} - Caches room entities by ID</li>
 *   <li>{@code favoriteRoomsAggByTitle} - Caches aggregated favorite room counts</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * The cache type to use: "caffeine" (default) or "redis".
     */
    @Value("${app.cache.type:caffeine}")
    private String cacheType;

    /**
     * Time-to-live for the animalsById cache.
     * Default is 10 minutes.
     */
    @Value("${app.cache.ttl.animals-by-id:10m}")
    private Duration animalsByIdTtl;

    /**
     * Time-to-live for the roomsById cache.
     * Default is 10 minutes.
     */
    @Value("${app.cache.ttl.rooms-by-id:10m}")
    private Duration roomsByIdTtl;

    /**
     * Time-to-live for the favoriteRoomsAggByTitle cache.
     * Default is 60 seconds.
     */
    @Value("${app.cache.ttl.favorites-agg:60s}")
    private Duration favoritesAggTtl;

    /**
     * Cache name for animal entities indexed by ID.
     */
    private static final String ANIMALS_BY_ID = "animalsById";

    /**
     * Cache name for room entities indexed by ID.
     */
    private static final String ROOMS_BY_ID = "roomsById";

    /**
     * Cache name for aggregated favorite room counts indexed by title.
     */
    private static final String FAVORITES_AGG_BY_TITLE = "favoriteRoomsAggByTitle";

    /**
     * Creates and configures the cache manager based on the configured cache type.
     * <p>
     * If {@code app.cache.type} is set to "redis", a Redis-backed cache manager is created.
     * Otherwise, a Caffeine-backed (in-memory) cache manager is used.
     *
     * @param redisConnectionFactory the Redis connection factory (required for Redis cache)
     * @return the configured cache manager
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        if ("redis".equalsIgnoreCase(cacheType)) {
            return redisCacheManager(redisConnectionFactory);
        }
        return caffeineCacheManager();
    }

    /**
     * Creates a Caffeine-backed cache manager with per-cache specifications.
     * <p>
     * Configures three caches:
     * <ul>
     *   <li>{@code animalsById} - Max 5,000 entries</li>
     *   <li>{@code roomsById} - Max 2,000 entries</li>
     *   <li>{@code favoriteRoomsAggByTitle} - Max 100 entries</li>
     * </ul>
     *
     * @return a SimpleCacheManager with Caffeine caches
     */
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

    /**
     * Creates a Redis-backed cache manager with JSON serialization for values.
     * <p>
     * Configures custom TTLs for each cache and uses transaction-aware caching.
     * Values are serialized using Jackson JSON serialization while keys remain as strings.
     *
     * @param connectionFactory the Redis connection factory
     * @return a RedisCacheManager with custom configurations
     */
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