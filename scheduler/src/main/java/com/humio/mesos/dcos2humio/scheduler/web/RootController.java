package com.humio.mesos.dcos2humio.scheduler.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/")
public class RootController {
    @Value("${humio.host}")
    String humioHost;
    @Value("${humio.dataspace}")
    String humioDataspace;

    @GetMapping
    public ResponseEntity<String> forwardToHome() {
        return ResponseEntity
                .status(HttpStatus.TEMPORARY_REDIRECT)
                .location(URI.create("https://" + humioHost + "/" + humioDataspace))
                .build();
    }
}
