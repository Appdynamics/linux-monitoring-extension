package com.appdynamics.extensions.linux;

public class MetricData {

    private String name;
    private String metricType;
    private boolean collectDelta;
    private Object stats;

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

    public Object getStats() {
        return stats;
    }

    public void setStats(Object stats) {
        this.stats = stats;
    }
}
