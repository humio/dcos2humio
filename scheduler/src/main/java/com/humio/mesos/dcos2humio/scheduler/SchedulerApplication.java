package com.humio.mesos.dcos2humio.scheduler;

import com.humio.mesos.dcos2humio.scheduler.model.HumioConfig;
import com.humio.mesos.dcos2humio.scheduler.service.CsvStringToMapConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableScheduling
public class SchedulerApplication {
    public static void main(String[] args) {

        SpringApplication.run(SchedulerApplication.class, args);
    }

    @Bean
    public HumioConfig humioConfig() {
        return new HumioConfig();
    }

}
