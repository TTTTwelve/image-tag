package com.tag.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by jiaqiwu on 2019/6/25.
 */
public class TagError {

    private Integer code;

    private String error;

    @JsonCreator
    public TagError(@JsonProperty("code") Integer code, @JsonProperty("error")String error){
        this.code = code;
        this.error = error;
    }


    public Integer getCode() {
        return code;
    }

    public String getError() {
        return error;
    }
}
