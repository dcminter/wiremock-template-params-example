package com.paperstack.blog.example.wiremock.template;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@ConstructorBinding
@Validated
@ConfigurationProperties("pingalyser")
public class PingalyserProperties {

    @NotEmpty
    private final String targetUri;

    public PingalyserProperties(final String targetUri) {
        this.targetUri = targetUri;
    }

    public String getTargetUri() {
        return targetUri;
    }
}
