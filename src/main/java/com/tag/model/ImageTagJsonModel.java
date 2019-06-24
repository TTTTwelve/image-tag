package com.tag.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by jiaqiwu on 2019/6/19.
 */
public class ImageTagJsonModel {

    private Integer id;

    private List<TagModel> tagModels;

    private String error;

    public ImageTagJsonModel(@JsonProperty("id") Integer id, @JsonProperty("tagModels") List<TagModel> tagModels) {
        this.id = id;
        this.tagModels = tagModels;
    }

    public ImageTagJsonModel(@JsonProperty("id") Integer id, @JsonProperty("error") String error) {
        this.id = id;
        this.error = error;
    }


    public List<TagModel> getTagModels() {
        return tagModels;
    }


    public String getError() {
        return error;
    }

    public Integer getId() {
        return id;
    }
}
