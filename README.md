# AppDynamics Linux Monitoring Extension

This extension works only with the Java agent.

##Use Case

The Linux monitoring extension gathers metrics for a linux machine and sends them to the AppDynamics Metric Browser.


##Installation

1. Run 'ant package' from the linux-monitoring-extension directory
2. Download the file LinuxMonitor.zip located in the 'dist' directory into \<machineagent install dir\>/monitors/
3. Unzip the downloaded file
4. By default all the metrics will be shown under "Custom Metrics|Linux", To change this, open \<machineagent install dir\>/monitors/LinuxMonitor/, open monitor.xml and configure the metric-path parameter.
     <pre>
     &lt;argument name="metric-path" is-required="false" default-value="" /&gt;
     
</pre>
5. Restart the Machine Agent. 
 
In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | Linux

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

##Custom Dashboard

![](https://raw.github.com/Appdynamics/linux-monitoring-extension/master/LinuxCustom.png)

##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/linux-monitoring-extension).

##Community

Find out more in the AppSphere community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).


