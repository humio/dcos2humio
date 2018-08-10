package com.humio.mesos.dcos2humio.scheduler.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConfigurationPropertiesBinding
public class CsvStringToMapConverter implements Converter<String, Map<String, String>> {
    @Override
    public Map<String, String> convert(String source) {
        return Arrays.stream(source.split(";"))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .map(s -> s.split(":"))
                .filter(strings -> StringUtils.isNotEmpty(strings[0]))
                .collect(Collectors.toMap(
                        strings -> strings[0].trim(),
                        strings -> strings.length > 1 ? strings[1].trim() : ""
                ));
    }
}
