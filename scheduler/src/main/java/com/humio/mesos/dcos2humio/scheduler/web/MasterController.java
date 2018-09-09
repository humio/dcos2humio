package com.humio.mesos.dcos2humio.scheduler.web;

import com.containersolutions.mesos.scheduler.events.TearDownFrameworkEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/master")
public class MasterController {
    private final ApplicationEventPublisher applicationEventPublisher;

    public MasterController(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @PostMapping("/teardown")
    public ResponseEntity<String> teardown() {
        applicationEventPublisher.publishEvent(new TearDownFrameworkEvent(this));
        return ResponseEntity.ok("See you");
    }
}
