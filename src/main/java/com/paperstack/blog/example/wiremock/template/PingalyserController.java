package com.paperstack.blog.example.wiremock.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class PingalyserController {

    private final RestTemplate restTemplate;
    private final PingalyserProperties properties;

    @Autowired
    public PingalyserController(final RestTemplate restTemplate, final PingalyserProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }


    @GetMapping("/pingalyse")
    public String pingalyse() {
        final Ping ping = restTemplate.getForObject(properties.getTargetUri(), Ping.class);
        final String responseText = ping.getResponse();
        return "pong".equals(responseText) ? "Pingy!" : "Not so pingy :'(";
    }
}
