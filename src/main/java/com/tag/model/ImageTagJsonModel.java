package com.tag.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by jiaqiwu on 2019/6/19.
 */
public class ImageTagJsonModel {

    private Integer id;

    private List<TagModel> tagModels;

    private TagError error;

    @JsonCreator
    public ImageTagJsonModel(@JsonProperty("id") Integer id, @JsonProperty("tagModels") List<TagModel> tagModels, @JsonProperty("error") TagError error) {
        this.id = id;
        this.error = error;
        this.tagModels = tagModels;
    }

    public List<TagModel> getTagModels() {
        return tagModels;
    }


    public TagError getError() {
        return error;
    }

    public Integer getId() {
        return id;
    }
}
