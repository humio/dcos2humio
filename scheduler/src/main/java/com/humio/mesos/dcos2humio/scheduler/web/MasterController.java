package com.humio.mesos.dcos2humio.scheduler.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/master")
public class MasterController {
    @PostMapping("/teardown")
    public ResponseEntity<String> teardown() {
        return ResponseEntity.ok("See you");
    }
}
