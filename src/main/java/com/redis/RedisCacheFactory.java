package com.redis;

/**
 * Created by jiaqiwu on 2019/6/26.
 */
public class RedisCacheFactory {

    private static RedisCache redisCache;

    public static RedisCache getRedisCache() {
        if (redisCache == null) {
            redisCache = new RedisCache();
        }
        return redisCache;
    }

}
