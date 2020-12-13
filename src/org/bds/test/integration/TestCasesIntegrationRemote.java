package org.bds.test.integration;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.bds.Config;
import org.bds.data.Data;
import org.bds.data.DataHttp;
import org.bds.data.DataRemote;
import org.bds.test.TestCasesBase;
import org.bds.util.Gpr;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for "remote" Data files
 *
 * @author pcingola
 *
 */
public class TestCasesIntegrationRemote extends TestCasesBase {

	@Before
	public void beforeEachTest() {
		Config.reset();
		Config.get().load();
	}

	String getCurrPath() {
		try {
			return (new File(".")).getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Test parsing a remote HTTP file (DataHttp)
	 */
	@Test
	public void test01_parse_URLs_http() {
		Gpr.debug("Test");

		Data d = Data.factory("http://www.google.com/index.html");
		d.setVerbose(verbose);
		d.setDebug(debug);
		long lastMod = d.getLastModified().getTime();
		if (verbose) Gpr.debug("Path: " + d.getPath() + "\tlastModified: " + lastMod + "\tSize: " + d.size());

		// Check some features
		Assert.assertTrue(d instanceof DataHttp);
		Assert.assertEquals("http://www.google.com/index.html", d.toString());
		Assert.assertEquals("http://www.google.com/", d.getParent().toString());
		Assert.assertEquals("/index.html", d.getPath());
		Assert.assertEquals("/index.html", d.getAbsolutePath());
		Assert.assertEquals("/index.html", d.getCanonicalPath());
		Assert.assertEquals("index.html", d.getName());
		Assert.assertTrue("Is file?", d.isFile());
		Assert.assertFalse("Is directory?", d.isDirectory());
		Assert.assertFalse("Is downloaded?", d.isDownloaded());

		// Download file
		boolean ok = d.download();
		Assert.assertTrue("Download OK", ok);

		// Is it at the correct local file?
		Assert.assertEquals("/tmp/bds/http/www/google/com/index.html", d.getLocalPath());

		// Check last modified time
		File file = new File(d.getLocalPath());
		long lastModLoc = file.lastModified();
		Assert.assertTrue("Last modified check:" //
				+ "\n\tlastMod    : " + lastMod //
				+ "\n\tlastModLoc : " + lastModLoc //
				+ "\n\tDiff       : " + (lastMod - lastModLoc)//
				, Math.abs(lastMod - lastModLoc) < 2 * DataRemote.CACHE_TIMEOUT);
	}

	/**
	 * Test parsing a remote AWS S3 file (DataS3)
	 */
	@Test
	public void test01_parse_URLs_s3() {
		Gpr.debug("Test");

		String bucket = awsBucketName();
		String region = awsRegion();

		String urlParen = "s3://" + bucket + "/tmp/bds/remote_01";
		if (!region.isEmpty()) urlParen = "https://" + bucket + ".s3." + region + ".amazonaws.com/tmp/bds/remote_01";

		String url = urlParen + "/hello.txt";

		// Create S3 file
		Random rand = new Random();
		String txt = "OK: " + rand.nextLong();
		createS3File(url, region, txt);

		checkS3File(url, region, bucket, "/tmp/bds/remote_01/hello.txt", urlParen, txt);
	}

	/**
	 * Test parsing a remote AWS S3 file (DataS3) using "virtual hosting" style URL
	 */
	@Test
	public void test01_parse_URLs_s3_virtual_host() {
		Gpr.debug("Test");

		String bucket = awsBucketName();
		String region = awsRegion();
		String urlParen = "https://" + bucket + ".s3." + region + ".amazonaws.com/tmp/bds/remote_01";
		String url = urlParen + "/hello.txt";

		// Create S3 file
		Random rand = new Random();
		String txt = "OK: " + rand.nextLong();
		createS3File(url, region, txt);

		checkS3File(url, null, bucket, "/tmp/bds/remote_01/hello.txt", urlParen, txt);
	}

	/**
	 * Test executing a task with a remote dependency
	 */
	@Test
	public void test03_task_URL() {
		Gpr.debug("Test");
		runAndCheck("test/remote_03.bds", "first", "<!DOCTYPE html>");
	}

	/**
	 * Test executing a task with a remote dependency
	 * Replacement of task's `sys` command within double quotes
	 */
	@Test
	public void test04_task_URL() {
		Gpr.debug("Test");
		runAndCheck("test/remote_04.bds", "first", "<!DOCTYPE html>");
	}

	/**
	 * Test executing a task with a remote dependency
	 * Replacement of task's `sys` command within single quotes
	 */
	@Test
	public void test05_task_URL() {
		Gpr.debug("Test");
		runAndCheck("test/remote_05.bds", "first", "<!DOCTYPE html>");
	}

	/**
	 * Test executing a task with multiple remote dependencies
	 */
	@Test
	public void test06_task_URL() {
		Gpr.debug("Test");
		runAndCheck("test/remote_06.bds", "first", "<!DOCTYPE html>");
	}

	/**
	 * Test executing a task with multiple remote dependencies in a list
	 */
	@Test
	public void test07_task_URL() {
		Gpr.debug("Test");
		runAndCheck("test/remote_07.bds", "first", "<!DOCTYPE html>");
	}

	/**
	 * Test executing a task with multiple remote dependencies in a list
	 * Replacement of task's `sys` command with list variables
	 */
	@Test
	public void test08_task_URL() {
		Gpr.debug("Test");
		runAndCheck("test/remote_08.bds", "first", "<!DOCTYPE html>");
	}

	/**
	 * Test executing a task with a remote dependency
	 * Replacement of task's `sys` command with literal references
	 */
	@Test
	public void test09_task_URL() {
		Gpr.debug("Test");
		runAndCheck("test/remote_09.bds", "first", "<!DOCTYPE html>");
	}

	/**
	 * Download a remote file
	 */
	@Test
	public void test10_download() {
		Gpr.debug("Test");
		runAndCheck("test/remote_10.bds", "locFile", "/tmp/bds/http/pcingola/github/io/BigDataScript/index.html");
	}

	/**
	 * Test Data.factory for remote HTTP file
	 */
	@Test
	public void test106_url() {
		String url = "http://www.google.com";
		Data durl = Data.factory(url);
		Assert.assertFalse("Relative: " + durl.isRelative(), durl.isRelative());
		Assert.assertTrue("Exists: " + durl.exists(), durl.exists());
		Assert.assertFalse("Is dir: " + durl.isDirectory(), durl.isDirectory());
		Assert.assertTrue("Wrong data type: " + durl.getClass().getCanonicalName(), durl instanceof DataHttp);

		Assert.assertEquals("Path: " + durl.getPath(), "", durl.getPath());
		Assert.assertEquals("Canonical: " + durl.getAbsolutePath(), "", durl.getAbsolutePath());
		Assert.assertEquals("URL: " + durl.toString(), url, durl.toString());
	}

	/**
	 * Download a remote file to a specific location
	 */
	@Test
	public void test11_download() {
		Gpr.debug("Test");
		runAndCheck("test/remote_11.bds", "ok", "true");
	}

	/**
	 * Upload a local file to AWS S3
	 */
	@Test
	public void test12_upload() {
		Gpr.debug("Test");
		runAndCheck("test/remote_12.bds", "ok", "true");
	}

	/**
	 * Parsing an AWS S3 file that does not exists with multiple bds functions
	 */
	@Test
	public void test13_S3() {
		Gpr.debug("Test");
		String bucket = awsBucketName();
		String expectedOutput = "" //
				+ "canonical      : test_remote_13_file_does_not_exits_in_S3.txt\n" //
				+ "baseName       : test_remote_13_file_does_not_exits_in_S3.txt\n" //
				+ "baseName('txt'): test_remote_13_file_does_not_exits_in_S3\n" //
				+ "canRead        : false\n" + "canWrite       : false\n" //
				+ "dirName        : /tmp/bds/remote_13\n" //
				+ "extName        : txt\n" //
				+ "exists         : false\n" + "isDir          : false\n" //
				+ "isFile         : true\n" //
				+ "path           : /tmp/bds/remote_13/test_remote_13_file_does_not_exits_in_S3.txt\n" //
				+ "pathName       : /tmp/bds/remote_13\n" //
				+ "removeExt      : s3://" + bucket + "/tmp/bds/remote_13/test_remote_13_file_does_not_exits_in_S3\n" //
				+ "dirPath        : []\n" //
				+ "dir            : []\n" //
		;

		runAndCheckStdout("test/remote_13.bds", expectedOutput);
	}

	/**
	 * Creating an S3 file and parsing with multiple bds functions
	 */
	@Test
	public void test14_S3() {
		Gpr.debug("Test");
		String bucket = awsBucketName();

		// Create S3 file (create local file and upload)
		String s3file = "s3://" + bucket + "/tmp/bds/remote_14/hello.txt";
		createS3File(s3file, awsRegion(), "OK");

		// Expected output
		String expectedOutput = "" //
				+ "baseName       : hello.txt\n" //
				+ "baseName('txt'): hello\n" //
				+ "canRead        : true\n" //
				+ "canWrite       : true\n" //
				+ "dirName        : /tmp/bds/remote_14\n" //
				+ "extName        : txt\n" //
				+ "exists         : true\n" //
				+ "isDir          : false\n" //
				+ "isFile         : true\n" //
				+ "path           : /tmp/bds/remote_14/hello.txt\n" //
				+ "pathName       : /tmp/bds/remote_14\n" //
				+ "removeExt      : s3://" + bucket + "/tmp/bds/remote_14/hello\n" //
				+ "dirPath        : []\n" //
				+ "dir            : []\n" //
		;

		runAndCheckStdout("test/remote_14.bds", expectedOutput);
	}

	/**
	 * Creating two S3 files and parsing with multiple bds functions
	 * Listing the "directory"
	 */
	@Test
	public void test15_S3() {
		Gpr.debug("Test");
		String bucket = awsBucketName();
		String region = awsRegion();

		// Create S3 files
		createS3File("s3://" + bucket + "/tmp/bds/remote_15/test_dir/z1.txt", region, "OK");
		createS3File("s3://" + bucket + "/tmp/bds/remote_15/test_dir/z2.txt", region, "OK");

		String urlParen = "s3://" + bucket + "/tmp/bds/remote_15/test_dir";
		if (region != null && !region.isEmpty()) urlParen = "https://" + bucket + ".s3." + region + ".amazonaws.com/tmp/bds/remote_15/test_dir";

		String expectedOutput = "" //
				+ "baseName       : \n" //
				+ "baseName('txt'): \n" //
				+ "canRead        : true\n" //
				+ "canWrite       : true\n" //
				+ "dirName        : /tmp/bds/remote_15/test_dir\n" //
				+ "extName        : \n" //
				+ "exists         : true\n" //
				+ "isDir          : true\n" //
				+ "isFile         : false\n" //
				+ "path           : /tmp/bds/remote_15/test_dir/\n" //
				+ "pathName       : /tmp/bds/remote_15/test_dir\n" //
				+ "removeExt      : \n" //
				+ "dirPath        : [" + urlParen + "/z1.txt, " + urlParen + "/z2.txt]\n" //
				+ "dir            : [z1.txt, z2.txt]\n" //
		;

		runAndCheckStdout("test/remote_15.bds", expectedOutput);
	}

	/**
	 * Creating an S3 file using `write` function and parsing with multiple bds functions
	 */
	@Test
	public void test16_S3() {
		Gpr.debug("Test");
		String bucket = awsBucketName();

		String expectedOutput = "" //
				+ "baseName       : test_remote_16.txt\n" //
				+ "baseName('txt'): test_remote_16\n" //
				+ "canRead        : true\n" //
				+ "canWrite       : true\n" //
				+ "dirName        : /tmp/bds/remote_16\n" //
				+ "extName        : txt\n" //
				+ "exists         : true\n" //
				+ "isDir          : false\n" //
				+ "isFile         : true\n" //
				+ "path           : /tmp/bds/remote_16/test_remote_16.txt\n" //
				+ "pathName       : /tmp/bds/remote_16\n" //
				+ "removeExt      : s3://" + bucket + "/tmp/bds/remote_16/test_remote_16\n" //
				+ "size           : 6\n" //
				+ "dirPath        : []\n" //
				+ "dir            : []\n" //
		;

		runAndCheckStdout("test/remote_16.bds", expectedOutput);
	}

	/**
	 * Creating an S3 file using `file.write()` function and parsing with multiple bds functions
	 * Then deleting the file with `file.rm()` and re-parsing the same file to show it doesn't exists
	 */
	@Test
	public void test17_S3() {
		Gpr.debug("Test");
		String bucket = awsBucketName();
		String expectedOutput = "" //
				+ "baseName       : test_remote_17.txt\n" //
				+ "baseName('txt'): test_remote_17\n" //
				+ "canRead        : true\n" //
				+ "canWrite       : true\n" //
				+ "dirName        : /tmp/bds/remote_17\n" //
				+ "extName        : txt\n" //
				+ "exists         : true\n" //
				+ "isDir          : false\n" //
				+ "isFile         : true\n" //
				+ "path           : /tmp/bds/remote_17/test_remote_17.txt\n" //
				+ "pathName       : /tmp/bds/remote_17\n" //
				+ "removeExt      : s3://" + bucket + "/tmp/bds/remote_17/test_remote_17\n" //
				+ "size           : 6\n" //
				+ "dirPath        : []\n" //
				+ "dir            : []\n" //
				+ "\n" //
				+ "Delete file s3://" + bucket + "/tmp/bds/remote_17/test_remote_17.txt\n" //
				+ "canRead        : false\n" //
				+ "canWrite       : false\n" //
				+ "exists         : false\n" //
				+ "size           : -1\n" //
				+ "dirPath        : []\n" //
				+ "dir            : []\n" //
		;

		runAndCheckStdout("test/remote_17.bds", expectedOutput);
	}

	/**
	 * Creating and readin an AWS S3 file
	 */
	@Test
	public void test18_S3() {
		Gpr.debug("Test");
		runAndCheck("test/remote_18.bds", "ok", "true");
	}

	/**
	 * Downloading with FTP
	 */
	@Test
	public void test19_ftp_download() {
		Gpr.debug("Test");
		String localFilePath = runAndGet("test/remote_19.bds", "fLocal").toString();

		// Check that the file exists (remove tmp file after)
		File f = new File(localFilePath);
		Assert.assertTrue("Local file '" + localFilePath + "' does not exists", f.exists());
		f.delete();
	}

	/**
	 * Checking a remote FTP file exists
	 */
	@Test
	public void test20_ftp_exists() {
		Gpr.debug("Test");
		runAndCheck("test/remote_20.bds", "fExists", "true");
	}

	/**
	 * Listing a remote FTP directory
	 */
	@Test
	public void test21_ftp_dir() {
		Gpr.debug("Test");
		runAndCheck("test/remote_21.bds", "dHasReadme", "true");
	}

	/**
	 * Listing a remote FTP directory
	 */
	@Test
	public void test22_ftp_dir() {
		Gpr.debug("Test");
		runAndCheck("test/remote_22.bds", "dHasReadme", "true");
	}

	/**
	 * Downloading an FTP remote file using user credentials
	 */
	@Test
	public void test23_ftp_download_with_user() {
		Gpr.debug("Test");
		String localFilePath = runAndGet("test/remote_23.bds", "fLocal").toString();

		// Check that the file exists (remove tmp file after)
		File f = new File(localFilePath);
		Assert.assertTrue("Local file '" + localFilePath + "' does not exists", f.exists());
		f.delete();
	}

	/**
	 * Checking a remote FTP file exists, using user credentials
	 */
	@Test
	public void test24_ftp_exists() {
		Gpr.debug("Test");
		runAndCheck("test/remote_24.bds", "fExists", "true");
	}

	/**
	 * Listing a remote FTP directory, using user credentials (`dir()`)
	 */
	@Test
	public void test25_ftp_dir() {
		Gpr.debug("Test");
		runAndCheck("test/remote_25.bds", "dHasReadme", "true");
	}

	/**
	 * Listing a remote FTP directory, using user credentials (`dirPath()`)
	 */
	@Test
	public void test26_ftp_dir() {
		Gpr.debug("Test");
		runAndCheck("test/remote_26.bds", "dHasReadme", "true");
	}

	/**
	 * Listing a remote FTP directory using regex (`dir(regex)`)
	 */
	@Test
	public void test27_ftp_dir() {
		Gpr.debug("Test");
		runAndCheck("test/remote_27.bds", "dd", "[Homo_sapiens.GRCh37.75.dna.toplevel.fa.gz]");
	}

	/**
	 * Listing a remote FTP directory using regex (`dirPath(regex)`)
	 */
	@Test
	public void test28_ftp_dirPath() {
		Gpr.debug("Test");
		runAndCheck("test/remote_28.bds", "dd", "[ftp://ftp.ensembl.org/pub/release-75/fasta/homo_sapiens/dna/Homo_sapiens.GRCh37.75.dna.toplevel.fa.gz]");
	}

	/**
	 * Listing a remote HTTP directory using regex (`dirPath(regex)`)
	 */
	@Test
	public void test29_http_dir() {
		Gpr.debug("Test");
		runAndCheck("test/remote_29.bds", "dd", "[http://ftp.ensembl.org/pub/release-75/fasta/homo_sapiens/dna/Homo_sapiens.GRCh37.75.dna.toplevel.fa.gz]");
	}

	/**
	 * Listing a remote HTTP directory using regex (`dir(regex)`)
	 */
	@Test
	public void test30_http_dir() {
		Gpr.debug("Test");
		runAndCheck("test/remote_29.bds", "dd", "[Homo_sapiens.GRCh37.75.dna.toplevel.fa.gz]");
	}

	/**
	 * Creating and listing a remote AWS S3 directory using `path.dir()`
	 */
	@Test
	public void test31_s3_dir() {
		Gpr.debug("Test");

		String bucket = awsBucketName();

		// Create S3 files
		createS3File("s3://" + bucket + "/tmp/bds/remote_31/test_remote_31/bye.txt", awsRegion(), "OK");
		createS3File("s3://" + bucket + "/tmp/bds/remote_31/test_remote_31/bye_2.txt", awsRegion(), "OK");
		createS3File("s3://" + bucket + "/tmp/bds/remote_31/test_remote_31/hi.txt", awsRegion(), "OK");
		createS3File("s3://" + bucket + "/tmp/bds/remote_31/test_remote_31/hi_2.txt", awsRegion(), "OK");

		runAndCheck("test/remote_31.bds", "dd", "[bye.txt, bye_2.txt, hi.txt, hi_2.txt]");
	}

	/**
	 * Creating and listing a remote AWS S3 directory using `path.dir(regex)`
	 */
	@Test
	public void test32_s3_dirPath() {
		Gpr.debug("Test");
		String bucket = awsBucketName();

		// Create S3 files
		createS3File("s3://" + bucket + "/tmp/bds/remote_32/test_remote_32/bye.txt", awsRegion(), "OK");
		createS3File("s3://" + bucket + "/tmp/bds/remote_32/test_remote_32/bye_2.txt", awsRegion(), "OK");
		createS3File("s3://" + bucket + "/tmp/bds/remote_32/test_remote_32/hi.txt", awsRegion(), "OK");
		createS3File("s3://" + bucket + "/tmp/bds/remote_32/test_remote_32/hi_2.txt", awsRegion(), "OK");

		runAndCheck("test/remote_32.bds", "dd", "[bye.txt, bye_2.txt]");
	}

	/**
	 * Creating and listing a remote AWS S3 directory using `path.dirPath(regex)`
	 */
	@Test
	public void test33_s3_dirPath() {
		Gpr.debug("Test");
		String bucket = awsBucketName();
		String region = awsRegion();

		String urlParen = "s3://" + bucket + "/tmp/bds/remote_33/test_remote_33";
		if (region != null && !region.isEmpty()) urlParen = "https://" + bucket + ".s3." + region + ".amazonaws.com/tmp/bds/remote_33/test_remote_33";

		// Create S3 files
		createS3File("s3://" + bucket + "/tmp/bds/remote_33/test_remote_33/bye.txt", region, "OK");
		createS3File("s3://" + bucket + "/tmp/bds/remote_33/test_remote_33/bye_2.txt", region, "OK");
		createS3File("s3://" + bucket + "/tmp/bds/remote_33/test_remote_33/hi.txt", region, "OK");
		createS3File("s3://" + bucket + "/tmp/bds/remote_33/test_remote_33/hi_2.txt", region, "OK");
		runAndCheck("test/remote_33.bds", "dd", "[" + urlParen + "/bye.txt, " + urlParen + "/bye_2.txt]");
	}

	/**
	 * Task input in s3, output local file
	 */
	@Test
	public void test34() {
		runAndCheck("test/remote_34.bds", "outStr", "OK");
	}

	/**
	 *  Task input local, output s3 file
	 */
	@Test
	public void test35() {
		runAndCheck("test/remote_35.bds", "outStr", "IN: 'remote_35'");
	}

	/**
	 * Task input from S3, output to S3
	 */
	@Test
	public void test36() {
		runAndCheck("test/remote_36.bds", "outStr", "IN: 'remote_36'");
	}

}
