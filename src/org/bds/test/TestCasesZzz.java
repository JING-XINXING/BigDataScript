package org.bds.test;

import org.bds.Config;
import org.bds.run.BdsRun;
import org.junit.Before;

/**
 * Quick test cases when creating a new feature...
 *
 * @author pcingola
 *
 */
public class TestCasesZzz extends TestCasesBase {

	@Before
	public void beforeEachTest() {
		BdsRun.reset();
		Config.get().load();
	}

}
