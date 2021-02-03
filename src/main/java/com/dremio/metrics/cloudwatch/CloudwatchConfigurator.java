/*
 * Copyright (C) 2017-2019 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.metrics.cloudwatch;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.dremio.telemetry.api.config.ConfigModule;
import com.dremio.telemetry.api.config.ReporterConfigurator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Objects;

import io.github.azagniotov.metrics.reporter.cloudwatch.CloudWatchReporter;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * Configurator for JMX
 */
@JsonTypeName("cloudwatch")
public class CloudwatchConfigurator extends ReporterConfigurator {

    private final Region region;
    private final TimeUnit rateUnit;
    private final TimeUnit durationUnit;
    private long intervalMs;
    private final String[] tags;
    private CloudWatchReporter cloudWatchReporter;

    @JsonCreator
    public CloudwatchConfigurator(@JsonProperty("region") String region,
                                  @JsonProperty("rate") TimeUnit rateUnit,
                                  @JsonProperty("duration") TimeUnit durationUnit,
                                  @JsonProperty("intervalMs") long intervalMs,
                                  @JsonProperty("tags") String[] tags) {
        super();
        this.region = Region.of(region);
        this.rateUnit = Optional.ofNullable(rateUnit).orElse(TimeUnit.SECONDS);
        this.durationUnit = Optional.ofNullable(durationUnit).orElse(TimeUnit.MILLISECONDS);
        this.intervalMs = intervalMs;
        this.tags = tags;
    }

    @Override
    public void configureAndStart(String name, MetricRegistry registry, MetricFilter filter) {
        final CloudWatchAsyncClient amazonCloudWatchAsync =
                CloudWatchAsyncClient
                        .builder()
                        .region(region)
                        .build();

        // todo percentiles and meter units
        cloudWatchReporter =
                CloudWatchReporter.forRegistry(registry, amazonCloudWatchAsync, CloudwatchConfigurator.class.getName())
                        .convertRatesTo(rateUnit)
                        .convertDurationsTo(durationUnit)
                        .filter(filter)
                        .withPercentiles(CloudWatchReporter.Percentile.P75, CloudWatchReporter.Percentile.P99)
                        .withOneMinuteMeanRate()
                        .withMeanRate()
                        .withArithmeticMean()
                        .withStdDev()
                        .withStatisticSet()
                        .withReportRawCountValue()
                        .withHighResolution()
                        .withMeterUnitSentToCW(StandardUnit.BYTES)
                        .withGlobalDimensions(tags)
                        .build();

        cloudWatchReporter.start(intervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CloudwatchConfigurator that = (CloudwatchConfigurator) o;
        return intervalMs == that.intervalMs &&
                Objects.equal(region, that.region) &&
                rateUnit == that.rateUnit &&
                durationUnit == that.durationUnit &&
                Objects.equal(cloudWatchReporter, that.cloudWatchReporter);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(region, rateUnit, durationUnit, intervalMs, cloudWatchReporter);
    }

    @Override
    public void close() {
        if (cloudWatchReporter != null) {
            cloudWatchReporter.stop();
        }
    }

    /**
     * Module that may be added to a jackson object mapper
     * so it can parse jmx config.
     */
    public static class Module extends ConfigModule {
        @Override
        public void setupModule(com.fasterxml.jackson.databind.Module.SetupContext context) {
            context.registerSubtypes(CloudwatchConfigurator.class);
        }
    }
}
