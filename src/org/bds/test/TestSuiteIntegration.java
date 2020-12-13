package org.bds.test;

import org.bds.test.integration.TestCasesAws;
import org.bds.test.integration.TestCasesCheckpoint;
import org.bds.test.integration.TestCasesClusterSsh;
import org.bds.test.unit.TestCasesClusterGeneric;
import org.bds.test.unit.TestCasesCommandLineOptions;
import org.bds.test.unit.TestCasesExecutioners;
import org.bds.test.unit.TestCasesFunctionDeclaration;
import org.bds.test.unit.TestCasesGraph;
import org.bds.test.unit.TestCasesInterpolate;
import org.bds.test.unit.TestCasesLang;
import org.bds.test.unit.TestCasesRemote;
import org.bds.test.unit.TestCasesReport;
import org.bds.test.unit.TestCasesRun;
import org.bds.test.unit.TestCasesRun2;
import org.bds.test.unit.TestCasesRun3;
import org.bds.test.unit.TestCasesTail;
import org.bds.test.unit.TestCasesTaskDetached;
import org.bds.test.unit.TestCasesTaskImproper;
import org.bds.test.unit.TestCasesTesting;
import org.bds.test.unit.TestCasesVm;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Invoke all test cases
 *
 * @author pcingola
 */

@RunWith(Suite.class)
@SuiteClasses({ TestCasesTail.class, //
		TestCasesVm.class, // Virtual machine
		TestCasesLang.class, // Language (compiler)
		TestCasesInterpolate.class, // Variable interpolation
		TestCasesExecutioners.class, // Task executioners
		TestCasesFunctionDeclaration.class, // Function declaration
		TestCasesRun.class, // Running bds code
		TestCasesRun2.class, // Running bds code
		TestCasesRun3.class, // Running bds code (classes / object)
		TestCasesGraph.class, // Running bds code: Task graphs and dependencies
		TestCasesCheckpoint.class, // Running bds code: Checkpoint and recovery
		TestCasesTesting.class, // Check bds unit testing system
		TestCasesCommandLineOptions.class, // Check command line options
		TestCasesClusterGeneric.class, // Running on a generic cluster
		TestCasesClusterSsh.class, // Run on an 'ssh cluster'
		TestCasesRemote.class, // Accessing remote data (cloud storage)
		TestCasesReport.class, // Report generation
		TestCasesTaskImproper.class, // Improper tasks
		TestCasesTaskDetached.class, // Detached tasks
		TestCasesAws.class, // Executioned AWS
})
public class TestSuiteIntegration {

}
