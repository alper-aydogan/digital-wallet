package com.alper.digitalwallet.infrastructure.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@ConditionalOnProperty(prefix = "app.rate-limiting", name = "enabled", havingValue = "true")
public class RateLimitRedisConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        return RedisClient.create("redis://" + host + ":" + port);
    }

    @Bean
    public ProxyManager<byte[]> bucketProxyManager(RedisClient redisClient) {
        return LettuceBasedProxyManager.builderFor(redisClient).build();
    }
}

