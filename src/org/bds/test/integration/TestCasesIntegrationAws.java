package org.bds.test.integration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bds.Config;
import org.bds.cluster.host.TaskResourcesAws;
import org.bds.data.DataS3;
import org.bds.executioner.ExecutionerCloudAws;
import org.bds.executioner.Executioners;
import org.bds.run.BdsRun;
import org.bds.task.Task;
import org.bds.test.TestCasesBase;
import org.bds.util.Gpr;
import org.bds.util.GprAws;
import org.bds.util.Timer;
import org.junit.Before;
import org.junit.Test;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;

/**
 * Test cases Classes / Objects
 *
 * @author pcingola
 *
 */
public class TestCasesIntegrationAws extends TestCasesBase {

	@Before
	public void beforeEachTest() {
		BdsRun.reset();
		Config.get().load();
	}

	protected String bucketUrl(String dirName, String fileName) {
		// Set the output file
		String bucket = awsBucketName();
		String region = awsRegion();
		String urlParen = "s3://" + bucket + "/tmp/bds/" + dirName;
		if (!region.isEmpty()) urlParen = "https://" + bucket + ".s3." + region + ".amazonaws.com/tmp/bds/" + dirName;
		String url = urlParen + "/" + fileName;
		return url;
	}

	/**
	 * Find AWS EC2 instance parameters for a given task
	 * @return DescribeInstancesResponse or null if not found
	 */
	protected DescribeInstancesResponse findAwsInstance(Task task) {
		// Find instance
		TaskResourcesAws resources = (TaskResourcesAws) task.getResources();

		// Filter specific instances
		String instanceId = task.getPid();
		if (instanceId == null || instanceId.isEmpty()) return null;

		// Query AWS
		debug("Creating 'DescribeInstancesRequest' request for instance Id '" + instanceId + "', task '" + task.getId() + "'");
		Set<String> instanceIds = new HashSet<>();
		instanceIds.add(instanceId);
		Ec2Client ec2 = GprAws.ec2Client(resources.getRegion());
		DescribeInstancesRequest request = DescribeInstancesRequest.builder().instanceIds(instanceIds).build();
		DescribeInstancesResponse response = ec2.describeInstances(request);
		debug("AWS Instances: " + response);

		return response;
	}

	/**
	 * Make sure there one (and only one) task in ExecutionerCloudAws
	 * Return the task or throw an error if not found
	 */
	protected Task findOneTaskAws() {
		// Note that we need to call `getRaw()` because the executioner is invalid at
		// this point, so the `get()` method would create a new executioner
		ExecutionerCloudAws excloud = (ExecutionerCloudAws) Executioners.getInstance().getRaw(Executioners.ExecutionerType.AWS);

		List<String> tids = excloud.getTaskIdsDone();
		assertTrue("There should be one task, found " + tids.size() + ", task Ids: " + tids, tids.size() == 1);

		// Find task ID
		String tid = tids.get(0);
		Task task = excloud.findTask(tid);
		assertNotNull("Could not find task '" + tid + "'", task);
		return task;
	}

	/**
	 * Find the first instance state in an AWS EC2 'DescribeInstancesResponse'
	 * @param response
	 * @return The first instance state, throw an error if nothing is found
	 */
	protected InstanceStateName instanceStateName(DescribeInstancesResponse response, String instanceId) {
		for (Reservation reservation : response.reservations()) {
			for (Instance instance : reservation.instances()) {
				debug("Found instance Id '" + instance.instanceId() + "'");
				if (instance.instanceId().equals(instanceId)) {
					InstanceStateName s = instance.state().name();
					log("Instance '" + instanceId + "' has state '" + s + "'");
					return s;
				}
			}
		}
		throw new RuntimeException("Could not find any instance ID '" + instanceId + "' in state in response: " + response);
	}

	/**
	 * Simple task command
	 */
	@Test
	public void test01_SimpleScript() {
		Gpr.debug("Test");

		String name = "run_aws_01";
		verbose = true;

		// Set the output file
		String region = awsRegion();
		String url = bucketUrl(name, "out.txt");

		// Cleanup: Make sure output file is deleted before starting
		DataS3 dout = new DataS3(url, region);
		dout.delete();

		// Run script
		runOk("test/" + name + ".bds");

		// Check: Check that the output file exists
		assertTrue("Output file '" + dout + "' does not exists", dout.exists());

		// Cleanup
		dout.delete();
	}

	/**
	 * TODO: Improper task
	 */
	@Test
	public void test02() {
		Gpr.debug("Test");
		throw new RuntimeException("UNIMPLEMENTED!");
	}

	/**
	 * TODO: AWS task with dependencies
	 */
	@Test
	public void test03() {
		Gpr.debug("Test");
		throw new RuntimeException("UNIMPLEMENTED!");
	}

	/**
	 * TODO: Improper task with dependencies
	 */
	@Test
	public void test04() {
		Gpr.debug("Test");
		throw new RuntimeException("UNIMPLEMENTED!");
	}

	/**
	 * TODO: 'dep' and 'goal'
	 */
	@Test
	public void test05() {
		Gpr.debug("Test");
		throw new RuntimeException("UNIMPLEMENTED!");
	}

	/**
	 * TODO: Execute a detached task
	 */
	@Test
	public void test06() {
		Gpr.debug("Test");
		throw new RuntimeException("UNIMPLEMENTED!");
	}

	/**
	 * Execute a detached task on AWS
	 * WARNIGN: This test might take several minutes to execute and creates an AWS EC2 instance!
	 */
	@Test
	public void test07_DetachedAwsTask() {
		Gpr.debug("Test");

		// Set the output file
		String region = awsRegion();
		String url = bucketUrl("run_aws_07", "out.txt");

		// Cleanup: Make sure output file is deleted before starting
		DataS3 dout = new DataS3(url, region);
		dout.delete();

		// Run: Execute bds code and check how long before it completes
		Timer t = new Timer();
		runOk("test/run_aws_07.bds");

		// Check: A detached task should finish immediately
		//        The original program's task loops for 60 seconds, so a detached task should take less than a minute
		assertTrue("Detached task taking too long: Probably not detached", t.elapsedSecs() < 15);

		verbose = true;

		// Wait: Make sure the task is executed on AWS, check instance and instance states
		waitAwsTask();

		// Check: Check that the output file exists
		assertTrue("Output file '" + dout + "' does not exists", dout.exists());

		// Cleanup
		dout.delete();
	}

	/**
	 * Wait for a single AWS task to finish
	 * Check that there is one (and only one) tasks
	 */
	protected void waitAwsTask() {
		int maxIterations = 120;

		// Wait until the instance is created
		Task task = findOneTaskAws();
		String tid = task.getId();
		waitInstanceCreate(task);
		String instanceId = task.getPid();
		debug("Task Id '" + tid + "', has AWS EC2 instance ID '" + instanceId + "'");

		// Wait until the instance terminates
		for (int i = 0; i < maxIterations; i++) {
			// Find instance
			DescribeInstancesResponse response = findAwsInstance(task);
			InstanceStateName state = instanceStateName(response, instanceId);

			debug("Task Id '" + tid + "', has AWS EC2 instance ID '" + instanceId + "', state '" + state + "'");
			if (state == InstanceStateName.TERMINATED) {
				log("Task Id '" + tid + "', has AWS EC2 instance ID '" + instanceId + "', state '" + state + "'");
				return; // OK, we finished successfully
			} else if (state == InstanceStateName.PENDING //
					|| state == InstanceStateName.RUNNING //
					|| state == InstanceStateName.SHUTTING_DOWN //
			) {
				// OK, wait until the instance terminates
			} else {
				throw new RuntimeException("Illegal state: Instance is in an illegal state. Task ID '', instance ID '" + instanceId + "', state '" + state + "'");
			}

			sleep(10); // Sleep for a few seconds
		}

		throw new RuntimeException("Timeout: Too many iterations waiting for instance to finish. Task ID '', instance ID '" + instanceId + "'");
	}

	/**
	 * Wait for an instance to be created
	 * Note: This is based on the fact that the task's PID has the instance ID after is instace for executing a task is created
	 * Waits for 'maxIterations' seconds, checking every second, if not ready thow an error
	 */
	protected void waitInstanceCreate(Task task) {
		int maxIterations = 60;

		for (int i = 0; i < maxIterations; i++) {
			String instanceId = task.getPid();
			if (instanceId != null && !instanceId.isEmpty()) {
				log("Found an instance '" + instanceId + "', for task Id '" + task.getId() + "'");
				return;
			}
			debug("Waiting for an instance, for task Id '" + task.getId() + "'");
			sleep(1);
		}

		throw new RuntimeException("Timeout waiting for an instance: Too many iterations waiting for cloud instance for task '" + task.getId() + "'");
	}

}
