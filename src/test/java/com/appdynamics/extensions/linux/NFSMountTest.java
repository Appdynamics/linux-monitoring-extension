package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.linux.input.MetricStat;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.PathResolver;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NFSMountMetricsTask.class, CommandExecutor.class})
public class NFSMountTest {

    @Mock
    private TasksExecutionServiceProvider serviceProvider;

    @Mock
    private MetricWriteHelper metricWriter;

    @Mock
    private Phaser phaser;

    private MetricStat.MetricStats metricStat;

    private MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration("Linux Monitor", "Custom Metrics|Linux Monitor|", PathResolver.resolveDirectory(AManagedMonitor.class), Mockito.mock(AMonitorJob.class));

    @Mock
    private NFSMountMetricsTask nfsProcessor;

    private List<String> nfsFilter = new ArrayList<>();


    public static final Logger logger = LoggerFactory.getLogger(NFSMountTest.class);

    @Before
    public void init() throws Exception {

        contextConfiguration.setConfigYml("src/test/resources/conf/test-config.yml");
        contextConfiguration.setMetricXml("src/test/resources/conf/test-metrics.xml", MetricStat.MetricStats.class);

        Mockito.when(serviceProvider.getMetricWriteHelper()).thenReturn(metricWriter);

        metricStat = (MetricStat.MetricStats) contextConfiguration.getMetricsXml();

        nfsProcessor = Mockito.spy(new NFSMountMetricsTask(metricStat.getMetricStats(), contextConfiguration.getMetricPrefix(), contextConfiguration, metricWriter, phaser, new ArrayList<>()));
        nfsFilter.add(".*");

    }

    @Test
    public void testMountStats() {

        Map<String, String> mountedNFSExpectedValueMap = Maps.newHashMap();

        mountedNFSExpectedValueMap.put("Custom Metrics|Linux Monitor|mountedNFSStatus|NFS1|used (MB)", "0");
        mountedNFSExpectedValueMap.put("Custom Metrics|Linux Monitor|mountedNFSStatus|NFS1|size (MB)", "65536");
        mountedNFSExpectedValueMap.put("Custom Metrics|Linux Monitor|mountedNFSStatus|NFS1|available (MB)", "65536");
        mountedNFSExpectedValueMap.put("Custom Metrics|Linux Monitor|mountedNFSStatus|NFS1|use %", "0%");

        try {
            mockStatic(CommandExecutor.class);
            List<String> lines = Files.readAllLines(Paths.get("src/test/resources/commandsOutputs/NFSMountOutput"));
            when(CommandExecutor.execute(any(String[].class))).thenReturn(lines);
        } catch (Exception e) {
            logger.error("Exception mocking", e);
        }

        List<Map<String, String>> nfsMounts = (ArrayList<Map<String, String>>) contextConfiguration.getConfigYml().get("mountedNFS");

        for(Map<String, String> mountedNFS : nfsMounts) {
            for (Metric metric : nfsProcessor.getMountStatus(mountedNFS)) {

                String actualValue = metric.getMetricValue();
                String metricName = metric.getMetricPath();
                if (mountedNFSExpectedValueMap.containsKey(metricName)) {
                    String expectedValue = mountedNFSExpectedValueMap.get(metricName);
                    Assert.assertEquals("The value of the metric " + metricName, expectedValue, actualValue);
                    mountedNFSExpectedValueMap.remove(metricName);
                } else {
                    Assert.fail("Unknown Metric " + metricName);
                }
            }
        }

        Assert.assertTrue(mountedNFSExpectedValueMap.isEmpty());
    }

    @Test
    public void testMountIOStats() {

        Map<String, String> mountedNFSIOExpectedValueMap = Maps.newHashMap();

        mountedNFSIOExpectedValueMap.put("Custom Metrics|Linux Monitor|nfsIOStats|NFS1|read|avg RTT (ms)","17.471");
        mountedNFSIOExpectedValueMap.put("Custom Metrics|Linux Monitor|nfsIOStats|NFS1|write|kB/s","0.105");
        mountedNFSIOExpectedValueMap.put("Custom Metrics|Linux Monitor|nfsIOStats|NFS1|read|avg exe (ms)", "17.723");
        mountedNFSIOExpectedValueMap.put("Custom Metrics|Linux Monitor|nfsIOStats|NFS1|write|kB/op", "222.243");
        mountedNFSIOExpectedValueMap.put("Custom Metrics|Linux Monitor|nfsIOStats|NFS1|read|kB/op","91.656");
        mountedNFSIOExpectedValueMap.put("Custom Metrics|Linux Monitor|nfsIOStats|NFS1|write|ops/s","0.000");
        mountedNFSIOExpectedValueMap.put("Custom Metrics|Linux Monitor|nfsIOStats|NFS1|read|kB/s", "0.040");
        mountedNFSIOExpectedValueMap.put("Custom Metrics|Linux Monitor|nfsIOStats|NFS1|write|avg exe (ms)", "5.591");
        mountedNFSIOExpectedValueMap.put("Custom Metrics|Linux Monitor|nfsIOStats|NFS1|write|avg RTT (ms)","2.411");
        mountedNFSIOExpectedValueMap.put("Custom Metrics|Linux Monitor|nfsIOStats|NFS1|read|ops/s","0.000");

        try {
            mockStatic(CommandExecutor.class);
            List<String> lines = Files.readAllLines(Paths.get("src/test/resources/commandsOutputs/NFSIOOutput"));
            when(CommandExecutor.execute(any(String[].class))).thenReturn(lines);
        } catch (Exception e) {
            logger.error("Exception mocking", e);
        }

        List<Map<String, String>> nfsMounts = (ArrayList<Map<String, String>>) contextConfiguration.getConfigYml().get("mountedNFS");

        //for(Map<String, String> mountedNFS : nfsMounts) {
            for (Metric metric : nfsProcessor.getMountIOStats(nfsMounts)) {

                String actualValue = metric.getMetricValue();
                String metricName = metric.getMetricPath();
                if (mountedNFSIOExpectedValueMap.containsKey(metricName)) {
                    String expectedValue = mountedNFSIOExpectedValueMap.get(metricName);
                    Assert.assertEquals("The value of the metric " + metricName, expectedValue, actualValue);
                    mountedNFSIOExpectedValueMap.remove(metricName);
                } else {
                    Assert.fail("Unknown Metric " + metricName);
                }
            }
        //}

        Assert.assertTrue(mountedNFSIOExpectedValueMap.isEmpty());
    }

}
