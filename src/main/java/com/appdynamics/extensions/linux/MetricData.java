/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.linux;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by akshay.srivastava on 9/12/18.
 */
public class MetricData {

    public static final Logger logger = Logger.getLogger(MetricData.class);

    private String name;
    private String metricType;
    private boolean collectDelta;
    private String multiplier;
    private Object stats;
    private Map<String, String> propertiesMap;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public boolean isCollectDelta() {
        return collectDelta;
    }

    public void setCollectDelta(boolean collectDelta) {
        this.collectDelta = collectDelta;
    }

    public void setMultiplier(String multiplier){
        this.multiplier = multiplier;
    }

    public Object getStats() {
        return stats;
    }

    public void setStats(Object stats) {
        this.stats = stats;
    }

    public Map<String, String> getPropertiesMap() {
        return propertiesMap;
    }

    public void setPropertiesMap(Map<String, String> propertiesMap) {
        this.propertiesMap = propertiesMap;
    }

    /**
     * Construct properties map as per entries in config.yml
     */
    public void constructProperties(){

        logger.debug("Aggregators defined for " + this.name + ": " + this.metricType);

        String[] metricType = this.metricType. split("\\.");

        Map<String, String> propertiesMap = new HashMap<String, String>();

        propertiesMap.put("aggregationType", metricType[0]);
        propertiesMap.put("timeRollUpType", metricType[1]);
        propertiesMap.put("clusterRollUpType", metricType[2]);

        propertiesMap.put("delta", Boolean.toString(this.collectDelta));
        propertiesMap.put("multiplier", this.multiplier);

        setPropertiesMap(propertiesMap);
    }
}
