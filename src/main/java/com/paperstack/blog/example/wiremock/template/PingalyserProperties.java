package com.paperstack.blog.example.wiremock.template;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@ConstructorBinding
@Validated
@ConfigurationProperties("pingalyser")
public class PingalyserProperties {

    private final String targetUri;

    public PingalyserProperties(@NotEmpty final String targetUri) {
        this.targetUri = targetUri;
    }

    @NotNull
    public String getTargetUri() {
        return targetUri;
    }
}
