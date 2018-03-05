package com.humio.mesos.dcos2humio.executor;

class LogField {
    private final String name;
    private final String value;

    public LogField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
