/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.extensions.linux.config;

import java.util.List;
import java.util.Map;

/**
 * Created by balakrishnav on 19/10/15.
 */
public class Configuration {

    private MountedNFS[] mountedNFS;
    private String metricPrefix;
    private String numberOfThreads;
    private List<String> diskIncludes;
    private List<Map<String, List<Map<String, String>>>> metrics;

    public MountedNFS[] getMountedNFS() {
        return mountedNFS;
    }

    public void setMountedNFS(MountedNFS[] mountedNFS) {
        this.mountedNFS = mountedNFS;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public List<Map<String, List<Map<String, String>>>> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Map<String, List<Map<String, String>>>> metrics) {
        this.metrics = metrics;
    }

    public String getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(String numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public List<String> getDiskIncludes() {
        return diskIncludes;
    }

    public void setDiskIncludes(List<String> diskIncludes) {
        this.diskIncludes = diskIncludes;
    }
}
