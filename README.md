# AppDynamics Linux Monitoring Extension

This extension works only with the standalone machine agent. It has been tested against Linux 2.6.32 on Ubuntu; info to be updated as tests against other distributions and Linux versions are completed.

##Use Case

The Linux monitoring extension gathers metrics for a Linux machine and sends them to the AppDynamics Metric Browser.


##Installation

1. Run 'ant package' from the linux-monitoring-extension directory
2. Download the file LinuxMonitor.zip located in the 'dist' directory into \<machineagent install dir\>/monitors/
3. Unzip the downloaded file
4. By default all the metrics will be shown under "Custom Metrics|Linux", To change this, open \<machineagent install dir\>/monitors/LinuxMonitor/, open monitor.xml and configure the metric-path parameter.
     <pre>
     &lt;argument name="metric-path" is-required="false" default-value="Custom Metrics|Ubuntu" /&gt;
     
</pre>
5. Restart the Machine Agent. 
 
In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | Linux (or the custom path you specified).

##Directory Structure

<table><tbody>
<tr>
<th align="left"> File/Folder </th>
<th align="left"> Description </th>
</tr>
<tr>
<td class='confluenceTd'> conf </td>
<td class='confluenceTd'> Contains the monitor.xml </td>
</tr>
<tr>
<td class='confluenceTd'> lib </td>
<td class='confluenceTd'> Contains third-party project references </td>
</tr>
<tr>
<td class='confluenceTd'> src </td>
<td class='confluenceTd'> Contains source code to the Linux Monitoring Extension </td>
</tr>
<tr>
<td class='confluenceTd'> dist </td>
<td class='confluenceTd'> Only obtained when using ant. Run 'ant build' to get binaries. Run 'ant package' to get the distributable .zip file </td>
</tr>
<tr>
<td class='confluenceTd'> build.xml </td>
<td class='confluenceTd'> Ant build script to package the project (required only if changing Java code) </td>
</tr>
</tbody>
</table>	

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

##Custom Dashboard

![](https://github.com/Appdynamics/linux-monitoring-extension/blob/master/Memory_Process.png?raw=true)
![](https://github.com/Appdynamics/linux-monitoring-extension/blob/master/CPU.png?raw=true)

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere] (http://appsphere.appdynamics.com/t5/eXchange/Linux-Monitoring-Extension/idi-p/5721) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).


