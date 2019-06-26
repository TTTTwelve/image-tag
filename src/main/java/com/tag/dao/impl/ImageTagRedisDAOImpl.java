package com.tag.dao.impl;

import com.redis.RedisCache;
import com.redis.RedisCacheFactory;
import com.tag.dao.ImageTagRedisDAO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.Tuple;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by jiaqiwu on 2019/6/26.
 */
@Service
public class ImageTagRedisDAOImpl implements ImageTagRedisDAO {

    private RedisCache redisCache;

    @PostConstruct
    private void init() {
        redisCache = RedisCacheFactory.getRedisCache();
    }

    private String getKey(long imageId) {
        return "it:" + imageId;
    }

    @Override
    public void insert(long imageId, String result) {
        try (ShardedJedis jedis = redisCache.getJedisPool().getResource()) {
            jedis.zadd(getKey(imageId), System.currentTimeMillis(), result);
        }
    }

    @Override
    public String findOne(long imageId) {
        try (ShardedJedis jedis = redisCache.getJedisPool().getResource()) {
            Integer start = 0;

            int limit = 1;

            Set<Tuple> list = jedis.zrevrangeWithScores(getKey(imageId), start, start + limit);
            if (CollectionUtils.isEmpty(list)) {
                return null;
            }

            return list.stream().collect(Collectors.toList()).get(0).getElement();
        }
    }

    @Override
    public void clean(long imageId) {
        try (ShardedJedis jedis = redisCache.getJedisPool().getResource()) {
            jedis.del(getKey(imageId));
        }
    }
}
