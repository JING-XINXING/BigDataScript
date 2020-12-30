package org.bds.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bds.BdsLog;
import org.bds.data.Data;
import org.bds.data.DataTask;
import org.bds.lang.BdsNode;
import org.bds.lang.value.Value;
import org.bds.lang.value.ValueList;
import org.bds.util.Timer;

/**
 * Output and Input files (and tasks) that are required for a task to successfully execute
 *
 * @author pcingola
 */
public class TaskDependency implements Serializable, BdsLog {

	private static final long serialVersionUID = -2026628428708457981L;

	boolean debug;
	protected BdsNode bdsNode; // The node that created this 'TaskDependency' (only used for logging & debugging purposes)
	protected List<Data> inputs; // Input files generated by this task
	protected List<Data> outputs; // Output files generated by this task
	protected String checkOutputs; // Errors that pop-up when checking output files
	protected List<Task> tasks; // Task that need to finish before this one is executed

	public TaskDependency() {
		this(null);
	}

	public TaskDependency(BdsNode bdsNode) {
		this.bdsNode = bdsNode;
		outputs = new ArrayList<>();
		inputs = new ArrayList<>();
		tasks = new ArrayList<>();
	}

	public void add(Task task) {
		tasks.add(task);
	}

	/**
	 * Add all dependencies from 'taskDependency' to this this one
	 */
	public void add(TaskDependency taskDependency) {
		addInputs(taskDependency.getInputs());
		addOutputs(taskDependency.getOutputs());
		tasks.addAll(taskDependency.getTasks());
	}

	public void addInput(Data input) {
		if (isDataTask(input)) {
			// Is 'input' a task ID?
			tasks.add(getTask(input));
		} else {
			// Not a taksID, must be an input 'data file'
			inputs.add(input);
		}
	}

	/**
	 * Add input. It can be either a taskId or a file
	 */
	public void addInput(String input) {
		// Is 'input' a task ID?
		Task task = getTask(input);

		if (task != null) {
			// It is a taskId, add task as dependency
			tasks.add(task);
		} else {
			// Not a taksID, must be an input 'data file'
			addInput(Data.factory(input));
		}
	}

	/**
	 * Add a list of inputs
	 */
	public void addInputs(Collection<Data> inputs) {
		for (Data in : inputs)
			addInput(in);
	}

	public void addInputs(ValueList inputs) {
		for (Value in : inputs)
			addInput(in.asString());
	}

	public void addOutput(Data output) {
		if (isDataTask(output)) throw new RuntimeException("Cannot have task as a dependency output, taskId: '" + output + "'");
		outputs.add(output);
	}

	/**
	 * Add output
	 */
	public void addOutput(String output) {
		Task task = getTask(output);
		if (task != null) {
			throw new RuntimeException("Cannot have task as a dependency output, taskId: '" + task.getId() + "'");
		} else {
			addOutput(Data.factory(output));
		}
	}

	public void addOutput(ValueList outputs) {
		for (Value out : outputs)
			addOutput(out.asString());
	}

	/**
	 * Add a list of outputs
	 */
	public void addOutputs(Collection<Data> outputs) {
		for (Data out : outputs)
			addOutput(out);
	}

	/**
	 * Check if output files are OK
	 * @return true if OK, false there is an error (output file does not exist or has zero length)
	 */
	public String checkOutputFiles(Task task) {
		if (checkOutputs != null) return checkOutputs;
		if (!task.isStateFinished() || outputs == null) return ""; // Nothing to check

		checkOutputs = "";
		for (Data dfile : outputs) {
			if (!dfile.exists()) checkOutputs += "Error: Output file '" + dfile + "' does not exist.";
			else if ((!task.isAllowEmpty()) && (dfile.size() <= 0)) checkOutputs += "Error: Output file '" + dfile + "' has zero length.";
		}

		if (task.verbose && !checkOutputs.isEmpty()) Timer.showStdErr(checkOutputs);
		return checkOutputs;
	}

	/**
	 * Mark output files to be deleted on exit
	 */
	public void deleteOutputFilesOnExit() {
		for (Data dfile : outputs) {
			if (dfile.exists()) dfile.deleteOnExit();
		}
	}

	/**
	 * Calculate the result of '<-' operator give two collections files (left hand side and right hand-side)
	 */
	public boolean depOperator() {
		// Empty dependency is always true
		if (outputs.isEmpty() && inputs.isEmpty()) return true;
		debug("Evaluating dependencies: " + (bdsNode != null && bdsNode.getFileName() != null ? (bdsNode.getFileName() + ":" + bdsNode.getLineNum()) : "null"));

		// Calculate minimum modification time of left hand side
		long minModifiedLeft = minModifiedLeft();
		if (minModifiedLeft < 0) return true;

		// Calculate maximum modification time of right hand side
		long maxModifiedRight = maxModifiedRight();
		if (maxModifiedRight < 0) return true;

		// Have all 'left' files been modified before 'right' files?
		// I.e. Have all goals been created after the input files?
		boolean ret = (minModifiedLeft < maxModifiedRight);
		debug("Modification times, minModifiedLeft (" + minModifiedLeft + ") < maxModifiedRight (" + maxModifiedRight + "): " + ret);
		return ret;
	}

	public List<Data> getInputs() {
		return inputs;
	}

	public List<Data> getOutputs() {
		return outputs;
	}

	protected Task getTask(Data dataTask) {
		return getTask(dataTask.getUrlOri());
	}

	protected Task getTask(String taskId) {
		return TaskDependecies.get().getTask(taskId);
	}

	public List<Task> getTasks() {
		return tasks;
	}

	public boolean hasTasks() {
		return !tasks.isEmpty();
	}

	protected boolean isDataTask(Data d) {
		return d instanceof DataTask;
	}

	@Override
	public boolean isDebug() {
		return debug;
	}

	boolean isTask(String tid) {
		return TaskDependecies.get().hasTask(tid);
	}

	@Override
	public String logMessagePrepend() {
		if (bdsNode != null) return bdsNode.logMessagePrepend();
		return getClass().getSimpleName();
	}

	/**
	 * Calculate maximum modification time of right hand side
	 * @return The max modified time for the files, or negative if the dependency must be true
	 */
	protected long maxModifiedRight() {
		long maxModifiedRight = Long.MIN_VALUE;
		for (Data dataIn : inputs) {
			// Is this file scheduled to be modified by a pending task?
			// If so, file time will change, thus we'll need to update
			List<Task> taskOutList = TaskDependecies.get().getTasksByOutput(dataIn);
			if (taskOutList != null && !taskOutList.isEmpty()) {
				for (Task t : taskOutList) {
					// If the task modifying 'file' is not finished, we'll need to update
					if (!t.isDone()) {
						debug("Right hand side: file '" + dataIn + "' will be modified by task '" + t.getId() + "' (task state: '" + t.getTaskState() + "')");
						return -1;
					}
				}
			}

			if (dataIn.exists()) {
				// Update max time
				long modTime = dataIn.getLastModified().getTime();
				maxModifiedRight = Math.max(maxModifiedRight, modTime);
				debug("Right hand side: file '" + dataIn + "' modified on " + modTime + ". Max modification time: " + maxModifiedRight);
			} else if (isDataTask(dataIn)) {
				// A task must be always executed to satisfy the dependency
				return -1;
			} else {
				// Make sure that we schedule the task if the input file doesn't exits
				// The reason to do this, is that probably the input file was defined
				// by some other task that is pending execution.
				debug("Right hand side: file '" + dataIn + "' doesn't exist");
				return -1;
			}
		}
		return maxModifiedRight;
	}

	/**
	 * Calculate the 'min modified time' for the left-hand side of the dependency operator.
	 * @return The min modified time for the file, or negative if the dependency must be true
	 */
	protected long minModifiedLeft() {
		long minModifiedLeft = Long.MAX_VALUE;
		for (Data dataOut : outputs) {
			// Any 'left' file does not exists? => We need to build this dependency
			if (!dataOut.exists()) {
				debug("Left hand side: file '" + dataOut + "' doesn't exist");
				return -1;
			}

			if (dataOut.isFile() && dataOut.size() <= 0) {
				debug("Left hand side: file '" + dataOut + "' is empty");
				return -1; // File is empty? => We need to build this dependency.
			} else if (dataOut.isDirectory()) {
				// Notice: If it is a directory, we must rebuild if it is empty
				List<Data> dirList = dataOut.list();
				if (dirList.isEmpty()) {
					debug("Left hand side: file '" + dataOut + "' is an empty dir");
					return -1;
				}
			} else if (isDataTask(dataOut)) {
				// Error: TaskID on the left hand side of dependecy operator?
				throw new RuntimeException("Cannot have a taskId on the left hand side of a dependency operator");
			}

			// Analyze modification time
			long modTime = dataOut.getLastModified().getTime();
			minModifiedLeft = Math.min(minModifiedLeft, modTime);
			debug("Left hand side: file '" + dataOut + "' modified on " + modTime + ". Min modification time: " + minModifiedLeft);
		}
		return minModifiedLeft;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("( ");

		if ((outputs != null && !outputs.isEmpty()) || (inputs != null && !inputs.isEmpty())) {

			if (outputs != null && !outputs.isEmpty()) {
				boolean comma = false;
				for (Data d : outputs) {
					sb.append((comma ? ", " : "") + "'" + d + "'");
					comma = true;
				}
			}

			sb.append(" <- ");

			if (inputs != null && !inputs.isEmpty()) {
				boolean comma = false;
				for (Data d : inputs) {
					sb.append((comma ? ", " : "") + "'" + d + "'");
					comma = true;
				}
			}
		}
		sb.append(" )");

		return sb.toString();
	}

}
