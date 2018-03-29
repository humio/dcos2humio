# Humio agent for Mesos and DC/OS

Log management for developers. Like tail and grep with aggregations and graphs built-in. See [Humio website](https://humio.com) for more information
## Goal

The goal for this Mesos Framework is to first of all add support for aggregating task logs of all Mesos tasks into Humio. Second to add information that makes sense in a DC/OS deployment.

# Getting started

First of all we recommend going through the [Task configuration](#taskconfiguration) section and at least add a `HUMIO_IGNORE` label to tasks that you do not want to end up in Humio. All other logs, in `stdout` and `stderr` are by default
forwarded to Humio

At this point two ways of deploying the framework is supported
 * Universe package
 * Marathon config
 
## Universe<a name="universe"></a>
Configuration parameters for user of the dcos CLI tool

| Property            | Description                                                                 |
|---------------------|-----------------------------------------------------------------------------|
| `humio.host`        | Hostname of Humio instance, i.e. `go.humio.com`                             |
| `humio.dataspace`   | Dataspace on Humio instance                                                 |
| `humio.ingestToken` | Ingest Token of dataspace                                                   |
| `service.name`      | DC/OS service name                                                          |
| `node.cpus`         | Amount of CPUs allocated to Humio agents on each node                       |
| `node.mem`          | Amount of memory allocated to Humio agents on each node                     |
| `node.datadir`      | Directory path for storing state on each node. Default is `/var/humio/data` |

## Marathon
The least recommended Marathon configuration should look something like this.

```json
{
  "id": "/humio-agent",
  "instances": 1,
  "cpus": 0.5,
  "mem": 512,
  "cmd": "SERVER_PORT=$PORT0 jre*/bin/java -Xms256m -Xmx512m -jar scheduler-*.jar",
  "networks": [
    {
      "mode": "host"
    }
  ],
  "portDefinitions": [
    {
      "name": "api",
      "protocol": "tcp",
      "port": 0,
      "labels": { "VIP_0": "/api.humio-agent:80" }
    }
  ],
  "env": {
    "SPRING_APPLICATION_NAME": "humio-agent",
    "HUMIO_EXECUTOR_URL": "https://github.com/humio/dcos2humio/releases/download/0.3-alpha-1/executor-0.3-alpha-1.jar",
    "HUMIO_HOST": "{{humio.host}}",
    "HUMIO_DATASPACE": "{{humio.dataspace}}",
    "HUMIO_INGESTTOKEN": "{{humio.ingestToken}}",
    "HUMIO_EXECUTOR_DATADIR": "/var/humio/data",
    "MESOS_RESOURCES_CPUS": "0.1",
    "MESOS_RESOURCES_mem": "512"
  },
  "labels": {
    "MARATHON_SINGLE_INSTANCE_APP": "true"
  },
  "uris": [
    "https://downloads.mesosphere.com/java/jre-8u131-linux-x64.tar.gz",
    "https://github.com/humio/dcos2humio/releases/download/0.3-alpha-1/scheduler-0.3-alpha-1.jar"
  ],
  "upgradeStrategy": {
    "minimumHealthCapacity": 0,
    "maximumOverCapacity": 0
  },
  "healthChecks": [
    {
      "protocol": "HTTP",
      "path": "/application/status",
      "gracePeriodSeconds": 900,
      "intervalSeconds": 10,
      "portIndex": 0,
      "timeoutSeconds": 30,
      "maxConsecutiveFailures": 0
    }
  ]
}
```

Don't forget to replace `{{humio.[host|dataspace|ingestToken]}}` as explained in [Universe](#Universe)

# Elastic beat configuration
Customize own configuration of beats. All variables are optional.

| Environment variable         | Allowed value                       | Description                                                                                  |
|------------------------------|-------------------------------------|----------------------------------------------------------------------------------------------|
| `HUMIO_FILEBEAT_CONFIGURL`   | url: http://example/filebeat.yml    | See https://www.elastic.co/guide/en/beats/filebeat/current/filebeat-configuration.html       |
| `HUMIO_METRICBEAT_CONFIGURL` | url: http://example/metricbeat.yml  | See https://www.elastic.co/guide/en/beats/metricbeat/current/metricbeat-configuration.html   |

# Task configuration<a name="taskconfiguration"></a>
Configuration of tasks is managed via Mesos Task Labels. All labels are optional.

| Label name                | Allowed value    | Description                                     |
|---------------------------|------------------|-------------------------------------------------|
| `HUMIO_IGNORE`            | `true`           | If set to `true` the agent will ignore the logs |
| `HUMIO_TYPE`              | string           | Humio parser                                    |
| `HUMIO_MULTILINE_PATTERN` | regex            | Label for the `multiline.pattern`               |
| `HUMIO_MULTILINE_NEGATE`  | `true`,`false`   | Label for the `multiline.negate`                |
| `HUMIO_MULTILINE_MATCH`   | `before`,`after` | Label for the `multiline.match`                 |
| `HUMIO_FIELD_*`           | string           | Add static field on all events                  |

For multiline configuration see the multiline section in the Filebeat documentation, https://www.elastic.co/guide/en/beats/filebeat/current/configuration-filebeat-options.html#multiline

Log fields
===

The agent will add the following fields to each log entry

| Field name             | Description                                                        |
|------------------------|--------------------------------------------------------------------|
| `mesos_slave_id`       | Mesos slave id                                                     |
| `mesos_framework_id`   | Mesos framework id                                                 |
| `mesos_framework_name` | Mesos framework name                                               |
| `mesos_task_id`        | Mesos task id                                                      |
| `mesos_service_id`     | For DC/OS deployments, the agent will provide a DC/OS service name |

Known limitations
===
* At this state only logs from the private agents are being forwarded to Humio. humio/dcos2humio#8
* Only `stdout` and `stderr` logs are harvested from tasks

Road map
===
* Forward DC/OS metrics. humio/dcos2humio#3
