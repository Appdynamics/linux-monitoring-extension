/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

import static org.mockito.Matchers.anyString;

/**
 * Created by akshay.srivastava on 14/01/19.
 */
@RunWith(PowerMockRunner.class)
public class LinuxMetricsTest {

    private Stats linuxStats;

    @Mock
    private TasksExecutionServiceProvider serviceProvider;

    @Mock
    private MetricWriteHelper metricWriter;

    @Mock
    private Phaser phaser;

    private MetricStat.MetricStats metricStat;

    private MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration("Linux Monitor", "Custom Metrics|Linux Monitor|", PathResolver.resolveDirectory(AManagedMonitor.class), Mockito.mock(AMonitorJob.class));


    @Before
    public void init() throws Exception {

        contextConfiguration.setConfigYml("src/test/resources/conf/test-config.yml");
        contextConfiguration.setMetricXml("src/test/resources/conf/test-metrics.xml", MetricStat.MetricStats.class);

        Mockito.when(serviceProvider.getMetricWriteHelper()).thenReturn(metricWriter);

        metricStat = (MetricStat.MetricStats) contextConfiguration.getMetricsXml();

        linuxStats = Mockito.spy(new Stats(contextConfiguration.getMetricPrefix(),metricStat.getMetricStats(),new ArrayList<>()));
    }

    @Test
    public void testDiskStats(){
        Map<String, String> memoryExpectedValueMap = Maps.newHashMap();

        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|diskStats|minor","7135");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|diskStats|writes completed","20428");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|diskStats|weighted time spent doing I/Os (ms)","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|diskStats|sectors written","12224");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|diskStats|I/Os currently in progress","26272");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|diskStats|reads completed successfully","1930298");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|diskStats|writes merged","626400");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|diskStats|time spent writing (ms)","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|diskStats|major","46500");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|diskStats|reads merged","34220");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|diskStats|time spent doing I/Os (ms)","46380");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|diskStats|time spent reading (ms)","13984");


        PowerMockito.when(linuxStats.getStream(anyString())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        File f = new File("src/test/resources/commandsOutputs/DiskOutput");
                        FileReader fr = new FileReader(f);
                        BufferedReader br  = new BufferedReader(fr);
                        return br;
                    }
                });

        List<String> diskFilter = new ArrayList<>();
        diskFilter.add(".*");

        for(Metric metric: linuxStats.getDiskStats(diskFilter)) {

            String actualValue = metric.getMetricValue();
            String metricName = metric.getMetricPath();
            if (memoryExpectedValueMap.containsKey(metricName)) {
                String expectedValue = memoryExpectedValueMap.get(metricName);
                Assert.assertEquals("The value of the metric " + metricName, expectedValue, actualValue);
                memoryExpectedValueMap.remove(metricName);
            } else {
                Assert.fail("Unknown Metric " + metricName);
            }
        }
    }

    @Test
    public void testfilteredCPUStats(){
        Map<String, String> memoryExpectedValueMap = Maps.newHashMap();

        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|cpuStats|cpu0|system","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|cpuStats|cpu0|guest_nice","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|cpuStats|cpu0|softirq","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|cpuStats|cpu0|idle","2436");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|cpuStats|cpu0|steal","88");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|cpuStats|cpu0|irq","415");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|cpuStats|cpu0|guest","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|cpuStats|cpu0|iowait","544630");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|cpuStats|cpu0|nice","1803");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|cpu|CPU (Cores) Logical","1");


        PowerMockito.when(linuxStats.getStream(anyString())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        File f = new File("src/test/resources/commandsOutputs/CPUOutput");
                        FileReader fr = new FileReader(f);
                        BufferedReader br  = new BufferedReader(fr);
                        return br;
                    }
                });

        List<String> cpuFilter = new ArrayList<>();
        cpuFilter.add("cpu0");

        for(Metric metric: linuxStats.getCPUStats(cpuFilter)) {

            String actualValue = metric.getMetricValue();
            String metricName = metric.getMetricPath();
            if (memoryExpectedValueMap.containsKey(metricName)) {
                String expectedValue = memoryExpectedValueMap.get(metricName);
                Assert.assertEquals("The value of the metric " + metricName, expectedValue, actualValue);
                memoryExpectedValueMap.remove(metricName);
            } else {
                Assert.fail("Unknown Metric " + metricName);
            }
        }
    }

    @Test
    public void testMemoryStats(){

        Map<String, String> memoryExpectedValueMap = Maps.newHashMap();

        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|writeback","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|mapped","193252");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|swap free","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|total","2045948");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|swap cached","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|active","1199932");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|commit limit","1022972");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|cached","578344");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|inactive","368096");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|free","280820");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|swap total","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|committed_As","3910336");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|buffers","142152");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|dirty","36");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|memStats|slab","131252");


        PowerMockito.when(linuxStats.getStream(anyString())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        File f = new File("src/test/resources/commandsOutputs/MemoryOutput");
                        FileReader fr = new FileReader(f);
                        BufferedReader br  = new BufferedReader(fr);
                        return br;
                    }
                });

        for(Metric metric: linuxStats.getLoadStats()) {

            String actualValue = metric.getMetricValue();
            String metricName = metric.getMetricPath();
            if (memoryExpectedValueMap.containsKey(metricName)) {
                String expectedValue = memoryExpectedValueMap.get(metricName);
                Assert.assertEquals("The value of the metric " + metricName, expectedValue, actualValue);
                memoryExpectedValueMap.remove(metricName);
            } else {
                Assert.fail("Unknown Metric " + metricName);
            }
        }

    }

    @Test
    public void testNetworkStats(){

        Map<String, String> memoryExpectedValueMap = Maps.newHashMap();

        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|transmit packets","4905");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|receive compressed","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|receive packets","7113");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|receive errs","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|transmit colls","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|receive bytes","5681713");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|receive fifo","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|transmit carrier","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|receive drop","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|transmit bytes","1442904");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|transmit compressed","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|received multicast","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|transmit drop","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|transmit fifo","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|transmit errs","0");
        memoryExpectedValueMap.put("Custom Metrics|Linux Monitor|netStats|enp0s3|receive frame","0");


        PowerMockito.when(linuxStats.getStream(anyString())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        File f = new File("src/test/resources/commandsOutputs/NetOutput");
                        FileReader fr = new FileReader(f);
                        BufferedReader br  = new BufferedReader(fr);
                        return br;
                    }
                });

        for(Metric metric: linuxStats.getNetStats()) {

            String actualValue = metric.getMetricValue();
            String metricName = metric.getMetricPath();
            if (memoryExpectedValueMap.containsKey(metricName)) {
                String expectedValue = memoryExpectedValueMap.get(metricName);
                Assert.assertEquals("The value of the metric " + metricName, expectedValue, actualValue);
                memoryExpectedValueMap.remove(metricName);
            } else {
                Assert.fail("Unknown Metric " + metricName);
            }
        }

    }

    @Test
    public void testLoadAvgStats(){

        Map<String, String> loadExpectedValueMap = Maps.newHashMap();

        loadExpectedValueMap.put("Custom Metrics|Linux Monitor|loadAvgStats|load avg (1 min)","0.29");
        loadExpectedValueMap.put("Custom Metrics|Linux Monitor|loadAvgStats|load avg (15 min)","0.16");
        loadExpectedValueMap.put("Custom Metrics|Linux Monitor|loadAvgStats|load avg (5 min)","0.18");

        PowerMockito.when(linuxStats.getStream(anyString())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        File f = new File("src/test/resources/commandsOutputs/LoadAvgOutput");
                        FileReader fr = new FileReader(f);
                        BufferedReader br  = new BufferedReader(fr);
                        return br;
                    }
                });

        for(Metric metric: linuxStats.getLoadStats()) {

            String actualValue = metric.getMetricValue();
            String metricName = metric.getMetricPath();
            if (loadExpectedValueMap.containsKey(metricName)) {
                String expectedValue = loadExpectedValueMap.get(metricName);
                Assert.assertEquals("The value of the metric " + metricName, expectedValue, actualValue);
                loadExpectedValueMap.remove(metricName);
            } else {
                Assert.fail("Unknown Metric " + metricName);
            }
        }
    }

    @Test
    public void testSockStats(){

        Map<String, String> sockExpectedValueMap = Maps.newHashMap();

        sockExpectedValueMap.put("Custom Metrics|Linux Monitor|sockUsedStats|used","741");
        sockExpectedValueMap.put("Custom Metrics|Linux Monitor|sockUsedStats|raw|inuse","0");
        sockExpectedValueMap.put("Custom Metrics|Linux Monitor|sockUsedStats|ifrag|inuse","0");
        sockExpectedValueMap.put("Custom Metrics|Linux Monitor|sockUsedStats|tcp|inuse","8");
        sockExpectedValueMap.put("Custom Metrics|Linux Monitor|sockUsedStats|udp|inuse","13");

        PowerMockito.when(linuxStats.getStream(anyString())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        File f = new File("src/test/resources/commandsOutputs/SocketOutput");
                        FileReader fr = new FileReader(f);
                        BufferedReader br  = new BufferedReader(fr);
                        return br;
                    }
                });

        for(Metric metric: linuxStats.getSockStats()) {

            String actualValue = metric.getMetricValue();
            String metricName = metric.getMetricPath();
            if (sockExpectedValueMap.containsKey(metricName)) {
                String expectedValue = sockExpectedValueMap.get(metricName);
                Assert.assertEquals("The value of the metric " + metricName, expectedValue, actualValue);
                sockExpectedValueMap.remove(metricName);
            } else {
                Assert.fail("Unknown Metric " + metricName);
            }
        }
    }

}
