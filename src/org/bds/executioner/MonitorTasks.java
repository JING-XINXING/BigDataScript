package org.bds.executioner;

import java.io.Serializable;

/**
 * Monitor a tasks: These "monitors" check that a task finished executing
 *
 * This is the Singleton object holding the monitors
 *
 * @author pcingola
 */
public class MonitorTasks implements Serializable {

	private static final long serialVersionUID = 654964341070558444L;

	private static MonitorTasks monitorTasks;

	private MonitorTaskExitFile monitorTaskExitFile;
	private MonitorTaskQueue monitorTaskAws;

	public static MonitorTasks get() {
		if (monitorTasks == null) monitorTasks = new MonitorTasks();
		return monitorTasks;
	}

	private MonitorTasks() {
	}

	public MonitorTaskQueue getMonitorTaskAws() {
		if (monitorTaskAws == null) monitorTaskAws = new MonitorTaskQueue();
		return monitorTaskAws;
	}

	public MonitorTaskExitFile getMonitorTaskExitFile() {
		if (monitorTaskExitFile == null) monitorTaskExitFile = new MonitorTaskExitFile();
		return monitorTaskExitFile;
	}

}