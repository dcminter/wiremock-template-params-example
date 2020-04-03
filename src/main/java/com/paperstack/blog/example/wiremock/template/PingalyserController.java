package com.paperstack.blog.example.wiremock.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

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
        final Optional<Ping> ping = Optional.of(restTemplate.getForObject(properties.getTargetUri(), Ping.class));
        return ping.map(p -> respond(p.getResponse())).orElse("No ping payload :'(");
    }

    private String respond(final String responseText) {
        return "pong".equals(responseText) ? "Pingy!" : "Not so pingy :'(";
    }
}
