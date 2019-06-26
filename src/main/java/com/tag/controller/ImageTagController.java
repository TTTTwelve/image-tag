package com.tag.controller;

import com.tag.constants.ImageTagErrorType;
import com.tag.dao.ImageTagRedisDAO;
import com.tag.helper.GoogleVisionLabelHelper;
import com.tag.helper.JSONCodec;
import com.tag.model.ImageTagJsonModel;
import com.tag.model.TagError;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jiaqiwu on 2019/6/20.
 */
@Controller
@RequestMapping("/google/vision")
public class ImageTagController {

    public final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ImageTagRedisDAO imageTagRedisDAO;

    @ResponseBody
    @RequestMapping(value = "/tag", method = RequestMethod.POST)
    public String tag(@RequestParam(value = "url", required = false) String url,
                      @RequestParam(value = "id", required = false) long id,
                      @RequestParam(value = "token", required = false) String token) {
        logger.info("imageurl={},imageId={},token={}", url, id, token);
        long start = System.currentTimeMillis();
        ImageTagJsonModel result;
        if (StringUtils.isNotBlank(url) && StringUtils.equals(token, getToken(url))) {
            try {
                String cacheString = imageTagRedisDAO.findOne(id);
                if (StringUtils.isNotBlank(cacheString)) {
                    result = JSONCodec.decode(cacheString, ImageTagJsonModel.class);
                    if (CollectionUtils.isNotEmpty(result.getTagModels())) {
                        return cacheString;
                    }
                }
                result = GoogleVisionLabelHelper.getLabelsByUrl(url);
                logger.info("耗时={}ms", System.currentTimeMillis() - start);
            } catch (IOException e) {
                logger.error("获取标签io异常", e);
                result = new ImageTagJsonModel(null, null, new TagError(ImageTagErrorType.IOError.getValue(), "获取标签io异常"));
            }
        } else {
            logger.error("校验失败url={}", url);
            result = new ImageTagJsonModel(null, null, new TagError(ImageTagErrorType.TokenError.getValue(), "校验失败url=" + url));
        }
        String resultStr = JSONCodec.encode(result);
        if (CollectionUtils.isNotEmpty(result.getTagModels())) {
            imageTagRedisDAO.insert(id, resultStr);
        }
        return resultStr;
    }

    @ResponseBody
    @RequestMapping(value = "/tag/list", method = RequestMethod.POST)
    public List<String> tagList(@RequestParam(value = "list", required = false) List<String> list,
                                @RequestParam(value = "token", required = false) String token) {
        logger.info("list={},token={}", list, token);
        long start = System.currentTimeMillis();
        List<String> resultList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list) && StringUtils.equals(token, getToken(list.get(0)))) {
            try {
                final int[] i = {0};
                resultList = GoogleVisionLabelHelper.getLabelsByUrlList(list.stream().map(e -> new Tuple2<>(i[0]++, e)).collect(Collectors.toList())).stream().map(JSONCodec::encode).collect(Collectors.toList());
                logger.info("耗时={}ms", System.currentTimeMillis() - start);
            } catch (IOException e1) {
                logger.error("", e1);
                resultList = list.stream().map(e -> JSONCodec.encode(new ImageTagJsonModel(null, null, new TagError(ImageTagErrorType.IOError.getValue(), "获取标签io异常")))).collect(Collectors.toList());
            }
        } else {
            logger.error("校验失败list={}", list);
            resultList = list.stream().map(e -> JSONCodec.encode(new ImageTagJsonModel(null, null, new TagError(ImageTagErrorType.TokenError.getValue(), "校验失败url=" + e)))).collect(Collectors.toList());
        }
        return resultList;
    }

    @ResponseBody
    @RequestMapping(value = "/tag/clean", method = RequestMethod.POST)
    public void tag(@RequestParam(value = "id", required = false) long id,
                    @RequestParam(value = "token", required = false) String token) {
        logger.info("清理缓存的图片标签，id={},token={}", id, token);
        if (StringUtils.equals(token, getToken(id + ""))) {
            imageTagRedisDAO.clean(id);
        }
    }

    private String getToken(String value) {
        return StringUtils.lowerCase(DigestUtils.md5Hex(DigestUtils.md5Hex(value) + "alohaimagetag"));
    }

}
