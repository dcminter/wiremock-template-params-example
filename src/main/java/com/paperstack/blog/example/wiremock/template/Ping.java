package com.paperstack.blog.example.wiremock.template;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Ping {
    private final String response;

    public Ping(@JsonProperty("response") final String response) {
        this.response = response;
    }

    @JsonProperty("response")
    public String getResponse() {
        return response;
    }
}
