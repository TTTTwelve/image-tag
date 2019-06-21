package com.tag.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by jiaqiwu on 2019/6/19.
 */
public class TagModel {

    private final Float confidence;
    private final String description;
    private final String mid;
    private final Float score;
    private final Float topicality;

    public TagModel(@JsonProperty("confidence") Float confidence,
                    @JsonProperty("description") String description,
                    @JsonProperty("mid") String mid,
                    @JsonProperty("score") Float score,
                    @JsonProperty("topicality") Float topicality) {
        this.confidence = confidence;
        this.description = description;
        this.mid = mid;
        this.score = score;
        this.topicality = topicality;
    }


    public Float getConfidence() {
        return confidence;
    }

    public String getDescription() {
        return description;
    }

    public String getMid() {
        return mid;
    }

    public Float getScore() {
        return score;
    }

    public Float getTopicality() {
        return topicality;
    }
}
