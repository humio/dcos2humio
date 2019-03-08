package com.humio.mesos.dcos2humio.scheduler.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "humio")
@Data
public class HumioConfig {
    private Map<String, String> globalFields = new LinkedHashMap<>();

    private List<String> agents = new ArrayList<>();
}
