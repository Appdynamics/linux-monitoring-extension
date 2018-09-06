/**
 * Copyright 2015 AppDynamics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.extensions.linux;

import com.appdynamics.extensions.linux.config.MountedNFS;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Created by balakrishnav on 16/10/15.
 */
public class LinuxMonitorTest {

    private LinuxMonitor testClass;

    @Mock
    private MountedNFS mountedNFS;

    @Mock
    private NFSMountStatusProcessor nfsProcessor;

    @Before
    public void init() throws Exception {
        testClass = new LinuxMonitor();

        mountedNFS = Mockito.mock(MountedNFS.class);

        mountedNFS.setFileSystem("/dev/disk1");
        mountedNFS.setDisplayName("NFS1");

        nfsProcessor = Mockito.mock(NFSMountStatusProcessor.class);


    }

    @Test
    public void testLinuxMonitor() throws TaskExecutionException {
        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "src/test/resources/conf/config.yml");

        TaskOutput result = testClass.execute(taskArgs, null);
        assertTrue(result.getStatusMessage().contains("Linux Monitor Metric Upload Complete"));

    }

    @Test
    public void testNFSMountStatusProcessor() {
        NFSMountStatusProcessor processor = new NFSMountStatusProcessor();
        Assert.assertEquals(processor.execute("/dev/disk1").trim(), "3");

    }

    @Test
    public void testNFSMountMetrics() {

        Map<String, Object> metricData = new HashMap<String, Object>();

        for(int i=0;i<5;i++){
            metricData.put("tps", 38);
            metricData.put("kB_read/s", 1030);
            metricData.put("kB_wrtn/s", 74);
            metricData.put("kB_read", 489105);
            metricData.put("kB_wrtn", 35012);
        }
        Mockito.when(nfsProcessor.getNFSMetrics(mountedNFS)).thenReturn(metricData);

        Assert.assertEquals(metricData.size(), 5);

    }

    @Test(expected = TaskExecutionException.class)
    public void testWithNullArgsShouldResultInException() throws Exception {
        testClass.execute(null, null);
    }

}
