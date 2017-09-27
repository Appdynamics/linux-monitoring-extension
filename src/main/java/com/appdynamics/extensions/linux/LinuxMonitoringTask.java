package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.linux.config.Configuration;
import com.appdynamics.extensions.linux.config.MountedNFS;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.apache.log4j.Logger;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinuxMonitoringTask implements Runnable{

    public static final Logger logger = Logger.getLogger(LinuxMonitoringTask.class);

    private MonitorConfiguration configuration;

    private String metricPrefix;

    private String configFileName;

    private Map<String, ?> configYml;

    private Cache<String, Long> prevMetricsMap;

    public MonitorConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(MonitorConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public Map<String, ?> getConfigYml() {
        return configYml;
    }

    public void setConfigYml(Map<String, ?> configYml) {
        this.configYml = configYml;
    }

    public LinuxMonitoringTask(MonitorConfiguration conf, String metricPrefix, String configFileName, Cache<String, Long> prevMetricsMap){
        this.configuration = conf;
        this.metricPrefix = metricPrefix;
        this.configYml = conf.getConfigYml();
        this.configFileName = configFileName;
        this.prevMetricsMap = prevMetricsMap;

    }

    public void run() {
        Configuration config = YmlReader.readFromFile(this.configFileName, Configuration.class);

        Map<String, Object> statsMap = populateMetrics(config);
        printNestedMap(statsMap, config.getMetricPrefix());

    }

    private Map<String, Object> populateMetrics(Configuration config) {
        Stats stats = new Stats(logger, config.getMetrics());
        Map<String, Object> statsMap = new HashMap<String, Object>();
        List<MetricData> list;

        if ((list = stats.getDiskStats()) != null) {
            statsMap.put("disk", list);
        }

        if ((list = stats.getCPUStats()) != null) {
            statsMap.put("CPU", list);
        }

        if ((list = stats.getDiskUsage()) != null) {
            statsMap.put("disk usage", list);
        }
        if ((list = stats.getFileStats()) != null) {
            statsMap.put("file", list);
        }
        if ((list = stats.getLoadStats()) != null) {
            statsMap.put("load average", list);
        }
        if ((list = stats.getMemStats()) != null) {
            statsMap.put("memory", list);
        }
        if ((list = stats.getNetStats()) != null) {
            statsMap.put("network", list);
        }
        if ((list = stats.getPageSwapStats()) != null) {
            statsMap.put("page", list);
        }
        if ((list = stats.getProcStats()) != null) {
            statsMap.put("process", list);
        }
        if ((list = stats.getSockStats()) != null) {
            statsMap.put("socket", list);
        }
        MountedNFS[] mountedNFS = config.getMountedNFS();
        if(mountedNFS != null) { //Null check to MountedNFS
            List<MetricData> nfsStats = new ArrayList<MetricData>();

            if ((list = stats.getMountStatus(mountedNFS)) != null) {
                nfsStats.addAll(list);
            }

            if ((list = stats.getMountIOStats(mountedNFS)) != null) {
                System.out.println("NFS IO Stats: " + list.size());
                nfsStats.addAll(list);
            }
            statsMap.put("mountedNFSStats", nfsStats);
        }else{
            logger.info("NFS mount is null");
        }
        logger.debug("StatsMap size: " + statsMap.size());
        return statsMap;
    }

    private void printNestedMap(Map<String, Object> map, String metricPath) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();

            List<MetricData> val = (ArrayList)entry.getValue();
            for(MetricData metricData: val) {

                if(metricData.isCollectDelta()){
                    Long[] prevValues = processDelta(metricPath, metricData.getStats());
                    if (prevValues[0] != null) {
                        printMetric(metricPath + key + "|" +metricData.getName() , getDeltaValue(metricData.getStats(), prevValues), metricData.getMetricType());
                    }
                }else{
                    printMetric(metricPath + key + "|" +metricData.getName(), metricData.getStats(), metricData.getMetricType());
                }

                // compute Avg IO utilization using metric in diskstats
                if ("time spent doing I/Os (ms)".equals(key)) {
                    Long[] prevValues = processDelta(metricPath, metricData.getStats());
                    if (prevValues[0] != null) {
                        printMetric(metricPath + "Avg I/O Utilization %", getDeltaValue(val, prevValues), metricData.getMetricType());
                    }
                }
            }
        }
    }

    private Long[] processDelta(String metricName, Object timeSpentDoingIOInms) {
        String timeElapsed = "timeElapsed";
        Long prevMetricValue = prevMetricsMap.getIfPresent(metricName);
        Long prevTimeElapsed = prevMetricsMap.getIfPresent(metricName + timeElapsed);
        Long[] prevValues = {prevMetricValue, prevTimeElapsed};
        if (timeSpentDoingIOInms != null) {
            prevMetricsMap.put(metricName, Long.valueOf((String) timeSpentDoingIOInms));
            prevMetricsMap.put(metricName + timeElapsed, System.currentTimeMillis());
        }
        return prevValues;
    }

    private Double getDeltaValue(Object currentValue, Long[] prevValues) {
        if (currentValue == null) {
            return null;
        }
        long currentTime = System.currentTimeMillis();
        long prevTime = prevValues[1];
        Long deltaIOTimeSpent = Long.valueOf((String) currentValue) - prevValues[0];
        Long duration = currentTime - prevTime;

        Double avgIOUtilizationInPercent = (double) deltaIOTimeSpent / (double) duration * 100.0;
        return avgIOUtilizationInPercent;
    }

    private void printMetric(String metricName, Object metricValue, String metricType) {
        if (metricValue != null) {

            this.configuration.getMetricWriter().printMetric(metricName, toWholeNumber(metricValue), metricType);
        }
    }

    protected static BigDecimal toWholeNumber(Object attribute) {
        if (attribute instanceof String) {
            String attrString = (String) attribute;
            Double d = Double.valueOf(attrString);
            return d > 0 && d < 1.0d ? BigDecimal.ONE : BigDecimal.valueOf(Math.round(d));
        } else if (attribute instanceof Long) {
            return (BigDecimal) attribute;
        } else if (attribute instanceof Double) {
            Double f1 = (Double) attribute;
            return f1.doubleValue() > 0.0D && f1.doubleValue() < 1.0D ? BigDecimal.ONE : BigDecimal.valueOf(Math.round(f1.doubleValue()));
        } else if (attribute instanceof Float) {
            Float f = (Float) attribute;
            return f.floatValue() > 0.0F && f.floatValue() < 1.0F ? BigDecimal.ONE : BigDecimal.valueOf(Math.round(((Float) attribute).floatValue()));
        }
        return null;
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }

}
