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
package com.appdynamics.extensions.linux.config;

import java.util.List;

/**
 * Created by balakrishnav on 19/10/15.
 */
public class Configuration {

    private MountedNFS[] mountedNFS;
    private String metricPrefix;
    private List<String> diskIncludes;

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

    public List<String> getDiskIncludes() {
        return diskIncludes;
    }

    public void setDiskIncludes(List<String> diskIncludes) {
        this.diskIncludes = diskIncludes;
    }
}
