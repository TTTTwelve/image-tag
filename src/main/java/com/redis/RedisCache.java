package com.redis;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.util.Pool;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by jiaqiwu on 2019/6/26.
 */
public class RedisCache {

    private static final Logger logger = LoggerFactory.getLogger(RedisCache.class);

    private ShardedJedisPool shardedJedisPool;

    // 少一点儿，减少对资源的占用
    public static final int PIPELINE_BATCH_SIZE = 50;

    // Number of connections to Redis that just sit there and do nothing
    private static final int POOL_MAX_IDEL = 25;

    // Minimum number of idle connections to Redis - these can be seen as always open and ready to serve
    private static final int POOL_MIN_IDEL = 15;

    private static final int POOL_MAX_TOTAL = 500;

    // 压力大的时候尽量不串
    private static final int TIMEOUT = 6000;

    private static final long SLEEP_BEFORE_CLOSE_OLD_CLIENT = TimeUnit.SECONDS.toMillis(10);

    RedisCache() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (shardedJedisPool != null) {
                    try {
                        Field fieldPool = Pool.class.getDeclaredField("internalPool");
                        fieldPool.setAccessible(true);
                        GenericObjectPool pool = (GenericObjectPool) fieldPool.get(shardedJedisPool);

                        logger.info("Redis pool stats: borrowed={}, returned={}, created={}, " +
                                        "destroyed={}, destroyedByEvictor={}, destroyedByBorrowValidation={}",
                                pool.getBorrowedCount(),
                                pool.getReturnedCount(),
                                pool.getCreatedCount(),
                                pool.getDestroyedCount(),
                                pool.getDestroyedByEvictorCount(),
                                pool.getDestroyedByBorrowValidationCount());
                    } catch (Throwable ex) {
                        logger.warn("找不到属性: Pool.internalPool", ex);
                    }
                }
            }
        }, 60 * 1000, 60 * 1000 * 3);
    }

    @Override
    protected void finalize() throws Throwable {
        this.shardedJedisPool.destroy();
    }

    public ShardedJedisPool getJedisPool() {
        if (shardedJedisPool != null) {
            return shardedJedisPool;
        }
        synchronized (this) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMaxTotal(POOL_MAX_TOTAL);
            poolConfig.setMaxIdle(POOL_MAX_IDEL);
            poolConfig.setMinIdle(POOL_MIN_IDEL);
            poolConfig.setBlockWhenExhausted(true);
            // this.setMinEvictableIdleTimeMillis(60000L);
            // this.setTimeBetweenEvictionRunsMillis(30000L);
            poolConfig.setMinEvictableIdleTimeMillis(1000 * 120); // 避免过大的shard导致大量内存分配
            poolConfig.setTimeBetweenEvictionRunsMillis(1000 * 60); // 不用太频繁

            List<JedisShardInfo> shards = new ArrayList<>();
            shards.add(new JedisShardInfo("localhost", 6379, TIMEOUT, "t1"));

            logger.info("初始化 jedis: {}", shards);

            ShardedJedisPool oldPool = this.shardedJedisPool;
            this.shardedJedisPool = new ShardedJedisPool(poolConfig, shards);

            // 关闭旧的
            if (oldPool != null) {
                try {
                    Thread.sleep(SLEEP_BEFORE_CLOSE_OLD_CLIENT);
                    logger.info("关闭旧的 jedis: {}", oldPool);
                    oldPool.destroy();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
        return shardedJedisPool;
    }
}
