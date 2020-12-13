package org.bds.test;

import org.bds.Config;
import org.bds.util.Gpr;
import org.junit.Before;
import org.junit.Test;

/**
 * Quick test cases when creating a new feature...
 *
 * @author pcingola
 *
 */
public class TestCasesZzz extends TestCasesBase {

	@Before
	public void beforeEachTest() {
		Config.reset();
		Config.get().load();
	}

	/**
	 * 'dep' and 'goal' using improper tasks
	 */
	@Test
	public void test08() {
		Gpr.debug("Test");

		String stdout = //
				"Task improper: Start\n" //
						+ "DEP_1: Start\n" //
						+ "DEP_1: End\n" //
						+ "DEP_2: Start\n" //
						+ "DEP_2: End\n" //
						+ "Task improper: End\n";

		runAndCheckStdout("test/run_task_improper_08.bds", stdout);
	}

}
