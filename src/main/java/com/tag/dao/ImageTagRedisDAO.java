package com.tag.dao;

import org.jooq.lambda.tuple.Tuple2;

import java.util.List;

/**
 * Created by jiaqiwu on 2019/6/26.
 */
public interface ImageTagRedisDAO {

    void insert(long imageId, String result);

    String findOne(long imageId);

    void clean(long imageId);
}
