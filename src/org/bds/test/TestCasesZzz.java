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

	// TODO: Check task multiple inputs and multiple outputs in s3
	@Test
	public void test38() {
		Gpr.debug("Test");
		verbose = true;
		runAndCheck("test/z.bds", "a", "23");
	}

}
