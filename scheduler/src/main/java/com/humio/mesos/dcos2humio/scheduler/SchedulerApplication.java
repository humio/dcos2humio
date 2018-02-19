package com.humio.mesos.dcos2humio.scheduler;

import com.humio.mesos.dcos2humio.scheduler.model.HumioConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SchedulerApplication {
    @Bean
    public HumioConfig humioConfig() {
        return new HumioConfig();
    }

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}
