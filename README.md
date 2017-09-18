Humio agent for DC/OS
===

Introduction
---

Humio is…

Goal
---

The goal for this Mesos Framework…

Getting started
===

At this point two ways of deploying the framework is supported
 * Universe package
 * Marathon config

By default, the agent will ship all task logs (`stdout` and `stderr` files) to Humio. If you have logs that you do
not intent to send to Humio, please give the task a `HUMIO_IGNORE`: `true` label.
 
Universe
---


Marathon
---

Task labels
===
All labels are optional.

| Label name | Allowed value | Description |
|------------|---------------|-------------|
| `HUMIO_IGNORE` | `true`    | If set to `true` the agent will ignore the logs |
| `HUMIO_TYPE` | `*` | Humio parser |
| `HUMIO_MULTILINE_PATTERN` | regex | Filebeat regex|
| `HUMIO_MULTILINE_NEGATE` | `true`,`false` | Negate…? |
| `HUMIO_MULTILINE_MATCH` | `before`,`after` | Match…? |

Log fields
===

The agent will add the following fields to each log entry

| Field name                 | Description          |
|----------------------------|----------------------|
| `mesos_framework_slave_id` | Mesos slave id       |
| `mesos_framework_id`       | Mesos framework id   |
| `mesos_framework_name`     | Mesos framework name |
| `mesos_task_id`            | Mesos task id        |

Dashboards
===

TODO