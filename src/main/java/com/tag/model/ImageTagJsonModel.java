package com.tag.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by jiaqiwu on 2019/6/19.
 */
public class ImageTagJsonModel {

    private List<TagModel> tagModels;

    private String error;

    public ImageTagJsonModel(@JsonProperty("tagModels") List<TagModel> tagModels){
        this.tagModels = tagModels;
    }

    public ImageTagJsonModel(@JsonProperty("error") String error){
        this.error = error;
    }

    public List<TagModel> getTagModels() {
        return tagModels;
    }


    public String getError() {
        return error;
    }
}
