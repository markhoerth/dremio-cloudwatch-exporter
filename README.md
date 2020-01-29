# Prometheus exporter for Dremio metrics
[![Build Status](https://travis-ci.org/rymurr/dremio-prometheus-exporter.svg?branch=master)](https://travis-ci.org/dremio-hub/dremio-prometheus-exporter)

This runs a [prometheus](https://https://prometheus.io/) endpoint which publishes all dremio metrics to Prometheus

## Build

1. change parent pom version in `pom.xml` to your dremio version (see https://github.com/dremio/dremio-oss to find the correct version) 
1. `mvn clean install` 
1. move `dremio-telemetry-cloudwatch-{version}-shaded.jar` to the `jars` directory in your dremio installation
1. restart dremio

## configuration

The file `dremio-telemetry.yaml` must exist on the classpath for your dremio installation and must contain the following

```yaml
metrics:
  - name: cloudwatch_reporter
    comment: >
      Publish metrics on cloudwatch
    reporter:
    type: cloudwatch
    port: 12543
```

An example has been provided in the root of this repo