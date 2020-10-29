package org.bds.executioner;

import java.util.List;

import org.bds.Config;
import org.bds.osCmd.Cmd;
import org.bds.osCmd.CmdLocal;
import org.bds.task.Task;
import org.bds.util.Gpr;

/**
 * Execute tasks in a cloud
 *
 * How tasks are run:
 * 	- A checkpointVm is created and saved to a bucket
 * 	- A startup script is created to run bds from the checkpoint
 * 	- An instance is created, the startup script is sent as parameter (terminate on shutdown should be enforced if possible)
 * 	- The instance executes the checkpoint
 * 	- The instance sends STDOUT / STDERR / EXIT messages via a queue (maybe a heartbeat message)
 *
 * Executioner
 * 	- The Executioner reads the queue and shows messages, and saves them to the local task.stdout / task.stderr / task.exitCode files
 * 	- Updates task status when EXIT message is received
 * 	- Instances are monitored for timeout
 * 	- Instances are monitored for failure
 * 	- All instances are terminated
 *	- Queue is deleted
 *
 * @author pcingola
 */
public class ExecutionerCloudAws extends ExecutionerCloud {

	public static final String KILL_COMMAND[] = { "@aws_ec2_terminate" };

	protected ExecutionerCloudAws(Config config) {
		super(config);
		monitorTask = MonitorTasks.get().getMonitorTaskQueue();
	}

	@Override
	protected void checkFinishedTasks() {
		// TODO Auto-generated method stub

	}

	/**
	 * Create "bds exec" command line options
	 */
	protected String[] createBdsExecCmd(Task task) {
		String[] argsAdd = { "-awsSqsName", queueThread.getName() };
		List<String> bdsExecArgs = createBdsExecCmdArgsList(task, argsAdd);
		return bdsExecArgs.toArray(Cmd.ARGS_ARRAY_TYPE);
	}

	@Override
	public synchronized Cmd createRunCmd(Task task) {
		task.createProgramFile(); // We must create a program file
		String[] bdsExecCmd = createBdsExecCmd(task);
		if (debug) log("Running task " + task.getId());
		// TODO: Create startup script (task's sys commands or checkpoint)
		// TODO: Find instance's parameters (type, image, disk, etc.)
		// TODO: Run instance
		// TODO: Store instance ID
		CmdLocal cmd = new CmdLocal(task.getId(), bdsExecCmd);
		cmd.setDebug(debug);
		cmd.setReadPid(true); // We execute using "bds exec" which prints PID number before executing the sub-process
		return cmd;
	}

	@Override
	protected CheckTasksRunning getCheckTasksRunning() {
		if (checkTasksRunning == null) {
			checkTasksRunning = new CheckTasksRunningAws(config, this);
			checkTasksRunning.setDebug(config.isDebug());
			checkTasksRunning.setVerbose(config.isVerbose());
		}
		return checkTasksRunning;
	}

	@Override
	public String[] osKillCommand(Task task) {
		return KILL_COMMAND;
	}

	@Override
	protected void postMortemInfo(Task task) {
		// TODO: Is there a way to collect some statistics on an instance that died recently?
		// TODO: We could try to check CloudWatch messages and metrics
	}

	@Override
	protected void runExecutionerLoopBefore() {
		// Start a new thread if needed, don't start a thread if it's already running
		if (queueThread == null) {
			queueThread = new QueueThreadAwsSqs(config, (MonitorTaskQueue) monitorTask, taskLogger);
			queueThread.start();
		}
	}

	//	@Override
	//	protected void runTask(Task task, Host host) {
	//		Gpr.debug("UNIMPLEMENTED !!! CMD: " + bdsExecCmd);
	//	}

	@Override
	protected void taskUpdateFinishedCleanUp(Task task) {
		Gpr.debug("UNIMPLEMENTED !!!");
	}

}
