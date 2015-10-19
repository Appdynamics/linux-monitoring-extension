# AppDynamics Linux Monitoring Extension

This extension works only with the standalone machine agent. It has been tested against Linux 2.6.32 on Ubuntu; info to be updated as tests against other distributions and Linux versions are completed.

##Use Case

The Linux monitoring extension gathers metrics for a Linux machine and sends them to the AppDynamics Metric Browser.


##Installation

1. To build from source, clone this repository and run 'mvn clean install'. This will produce a LinuxMonitor-VERSION.zip in the target directory. Alternatively, download the latest release archive from [Github](https://github.com/Appdynamics/linux-monitoring-extension/releases)
2. Unzip LinuxMonitor.zip and copy the 'LinuxMonitor' directory to `<MACHINE_AGENT_HOME>/monitors/`
3. Configure the extension by referring to the below section.
4. Restart the Machine Agent. 
 
In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | Linux (or the custom path you specified).


## Configuration

Note : Please make sure not to use tab (\t) while editing yaml files. You can validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the Linux Extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/LinuxMonitor/`.
2. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/LinuxMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/LinuxMonitor/config.yml" />
          ....
     </task-arguments>
    ```

## Metrics
### Metric Category: CPU

|Metric Name            	|Description|
|------------------------------	|------------|
|iowait					|IO Wait	
|system					|System
|user					|User

### Metric Category: disk

|Metric Name            	|Description|
|------------------------------	|------------|
|I/Os currently in progress		|Current I/O operations	
|reads completed successfully 	|Number of successfull reads
|reads merged					|Reads merged
|sectord read 					|Sectors read
|sectors written 				|Sectors written 
|time spent doing I/Os(ms) 		|time in ms spent doing I/Os 
|time spent reading(ms) 		|time in ms spent reading 
|time spent writing(ms)			|time in ms spent writing 		 
|writes completed 				|Number of writes completed 
|writes merged 					|Number of writes merged
|Avg I/O Utilization %          |Percentage of CPU time during which I/O requests were issued to the device

### Metric Category: file

|Metric Name            	|Description|
|------------------------------	|------------|
|agelimit						|Age limit
|fhalloc						|fhalloc
|fhfree 						|fhfree
|unused							|unused

### Metric Category: load average

|Metric Name            	|Description|
|------------------------------	|------------|
|load avg (1 min) 				|Load average 1 Minute
|load avg (10 min) 				|Load average 10 Minute
|load avg (15 min) 				|Load average 15 Minute

### Metric Category: memory

|Metric Name            	|Description|
|------------------------------	|------------|
|active 						|Active 
|buffers 						|Buffers 
|cached 						|Cached
|commit limit 					|Commit limit 
|dirty 							|Dirty
|free 							|Free 
|inactive 						|Inactive
|mapped 						|Mapped
|real free 						|Real free 
|real free %					|Real free percent
|total 							|Total
|used 							|Used
|used % 						|Used percent

### Metric Category: network

|Metric Name            	|Description|
|------------------------------	|------------|
|receive bytes 					|Receive bytes
|receive compressed 			|Receive compressed
|receive drop 					|Receive drop
|receive errs 					|Receive errors
|receive packets 				|Receive packets
|transmit bytes 				|Transmit bytes
|transmit compressed 			|Transmit compresses
|transmit drop 					|Transmit drop
|transmit errs 					|Transmit errors
|transmit packets 				|Transmit packets

### Metric Category: page

|Metric Name            	|Description|
|------------------------------	|------------|
|page fault 					|Page fault
|page in 						|Page in
|page out 						|Page out
|swap page in					|Swap page in
|swap page out					|Swap page out

### Metric Category: process

|Metric Name            	|Description|
|------------------------------	|------------|
|blocked 						|Blocked processes
|count 							|Total number of processes
|processes 						|Nnumber of processes
|running 						|Running processes
|runqueue 						|Processes in run queue

### Metric Category: socket

|Metric Name            	|Description|
|------------------------------	|------------|
|raw 							|Raw sockets
|tcp 							|TCP sockets
|udp 							|UDP sockets
|used 							|Used sockets

### Metric Category: nfsMountStatus
An availability status for any external network file system (NFS) mounts is reported by executing the command `df | grep <fileSystem> | wc -l`.
The file systems to be monitored are to be configured in config.yml. 
```
mountedNFS:
      - fileSystem: "/dev/sdb"
        displayName: "NFS1"
      - fileSystem: "/dev/sda"
        displayName: "NFS2"
```

Note : By default, a Machine agent or a AppServer agent can send a fixed number of metrics to the controller. To change this limit, please follow the instructions mentioned [here](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
For eg.
```
    java -Dappdynamics.agent.maxMetrics=2500 -jar machineagent.jar
```

##Custom Dashboard

![](https://github.com/Appdynamics/linux-monitoring-extension/blob/master/Memory_Process.png?raw=true)
![](https://github.com/Appdynamics/linux-monitoring-extension/blob/master/CPU.png?raw=true)

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere] (http://www.appdynamics.com/community/exchange/extension/linux-monitoring-extension) community.

##Support

For any questions or feature request, please contact [AppDynamics Support](mailto:help@appdynamics.com).


