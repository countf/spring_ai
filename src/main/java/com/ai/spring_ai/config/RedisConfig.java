package com.ai.spring_ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // 连接池最大连接数
        poolConfig.setMaxTotal(20);
        // 最大空闲连接数
        poolConfig.setMaxIdle(5);
        // 最小空闲连接数
        poolConfig.setMinIdle(1);

        // 禁用JMX注册
        poolConfig.setJmxEnabled(false);
        return new JedisPool(poolConfig, host, port);
    }
}