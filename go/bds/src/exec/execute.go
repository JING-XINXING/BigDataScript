package exec

import (
	"io/ioutil"
	"log"
	"fileutil"
	"os"
	"os/exec"
	"os/signal"
	"strconv"
	"strings"
	"syscall"
	"tee"
	"time"
)

/*
  Calculate the 'checksum' of a file and compare it to
	the '# Checksum' line.
	The main pourpose is to know when the file has been fully
	written to disk, so we can execute it without having a
	'text file busy' error
*/
func (be *BdsExec) checksum() bool {
	buff, err := ioutil.ReadFile(be.command);
	if err != nil {
		return false
	}

	// Find 'checksum' line position
	cmd := string(buff)
	idx := strings.Index(cmd, CHECKSUM_LINE_START)
	if idx < 0 {
		return false
	}
	idx-- // Don't include '\n' prior to line

	// Extract checksum value
	chsumStr := strings.TrimSpace(string(buff[idx+len(CHECKSUM_LINE_START):]))
	chsum, err := strconv.ParseInt(chsumStr, 16, 32)
	chsum32 := uint32(chsum);
	if err != nil {
		return false
	}

	// Checksum all bytes before 'idx'
	var sum uint32
	for i := 0 ; i < idx ; i++ {
		sum = sum * 33 + uint32(buff[i])
	}

	// Is checksum correct?
	return sum == chsum32
}

/*
  This function reads a 'shell' file created by Task.java
	(see Task.createProgramFile() method) and checks that the 'checksum'
	line at the end of the file matches the result. It waits for some
	predefined time until the line appears.
	The main purpose is to avoid 'text file busy' errors that are
	triggered by executing a shell file that is still begin written (or
	flushed) to the disk.
	Ideally we'd like to have some sort of 'lsof' command line
	functionality but it might be difficult to do it in a platform
	independent way.
*/
func (be *BdsExec) checksumWait() bool {
	if be.noCheckSum {
		return true
	}
	for i := 0 ; i < MAX_CHECKSUM_ITERS ; i++ {
			if DEBUG {
				log.Printf("Debug checksumWait (iteration %d): '%s'\n", i, be.command)
			}
		 	if be.checksum() {
				return true
			}
			time.Sleep(CHECKSUM_SLEEP_TIME) // Sleep for a while
		}
	return false
}

/*
	Execute a command enforcing a timeout and writing exit status to 'exitFile'
*/
func (be *BdsExec) executeCommandTimeout(osSignal chan os.Signal) int {
	if DEBUG {
		log.Printf("Debug executeCommandTimeout: Start\n")
	}

	// Wait for execution to finish or timeout
	exitStr := ""
	if be.timeSecs <= 0 {
		be.timeSecs = 31536000 // Default: One year
	}

	// Create a timeout process
	// References: http://blog.golang.org/2010/09/go-concurrency-patterns-timing-out-and.html
	exitCode := make(chan string, 1)
	go execute(be.cmd, exitCode)

	// Wait until executions ends, timeout or OS signal
	kill := false
	run := true
	for run {
		select {
		case exitStr = <-exitCode:
			kill = false
			run = false
			if DEBUG {
				log.Printf("Debug executeCommandTimeout: Execution finished (%s)\n", exitStr)
			}

		case <-time.After(time.Duration(be.timeSecs) * time.Second):
			run = false
			kill = true
			exitStr = "Time out"
			if DEBUG {
				log.Printf("Debug executeCommandTimeout: Timeout!\n")
			}

		case sig := <-osSignal:
			// Ignore some signals (e.g. "window changed")
			sigStr := sig.String()
			if sigStr != "window changed" && sigStr != "child exited" && sigStr != "window size changes" {
				if VERBOSE || DEBUG {
					log.Printf("bds: Received OS signal '%s'\n", sigStr)
				}

				kill = true
				exitStr = "Signal received"
				run = false
			}
		}
	}

	// Write exitCode to file
	be.updateExitFile(exitStr)

	// Should we kill child process?
	if kill {
		// WARNING: The kill(0) signal will also kill this process, so nothing
		// may be execited after this call (platform dependent)
		be.kill()
	}

	// Analyze exit code
	switch exitStr  {
		case "0":
			return EXITCODE_OK	// OK, exit value should be zero
		case "Time out":
			return EXITCODE_TIMEOUT	// Timeout exit code
		default:
			return EXITCODE_ERROR	// Other error conditions
	}
}

/*
	Execute a command and writing exit status to 'exitCode'
*/
func execute(cmd *exec.Cmd, exitCode chan string) {
	if DEBUG {
		log.Printf("Debug execute: Start\n")
	}

	// Wait for command to finish
	if err := cmd.Wait(); err != nil {
		if DEBUG {
			log.Printf("Debug execute: Failed (%s)\n", err)
		}

		exitCode <- err.Error()
		return
	}

	if DEBUG {
		log.Printf("Debug execute: Finished OK\n")
	}

	exitCode <- "0"
}

/*
	Execute a command (using arguments 'args')
	Redirect stdout to outFile    (unless file name is empty)
	Redirect stderr to errFile    (unless file name is empty)
	Write exit code to exitFile   (unless file name is empty)
	Timeout after timeout seconds (unless time is zero)
*/
func (be *BdsExec) executeCommand() int {
	if DEBUG {
		log.Printf("Debug executeCommand: %v (len: %d)\n", be.cmdargs, len(be.cmdargs))
	}

	// Redirect all signals to channel (e.g. Ctrl-C)
	osSignal := make(chan os.Signal)

	if be.taskLoggerFile != "" {
		// Main bds program
		signal.Notify(osSignal) // Capture all signals
	} else {
		be.checksumWait()

		// Set a new process group.
		// We want to be able to kill all child processes, without killing the
		// calling program. E.g. When running using a local executor, the Java Bds
		// calls 'bds exec', so sending a kill to the group when a timeOut occurs,
		// would also kill the parent Java program and kill the whole bds execution
		// (clearly not what we want).
		// To avoid this, we create a new group thus we can send a kill signal to
		// this new process group.

		gpidOri, _ :=  syscall.Getpgid(0);
		if err := syscall.Setpgid(0, 0); err != nil {
			// During an ssh remote execution we will no be albe to do this.
			// In this case, we assume that the SSH daemon will catch the sinals
			// and kill al child processes.
			if DEBUG {
				log.Printf("Error executeCommand: Error setting process group: %s", err)
			}
		}

		if DEBUG {
			gpidNew, _ :=  syscall.Getpgid(0);
			log.Printf("Info: Setting new process group. Original GPID: %d, new GPID: %d\n", gpidOri, gpidNew)
		}
	}

	// Create command
	be.cmd = exec.Command(be.command)
	be.cmd.Args = be.cmdargs

	// Copy stdout
	stdout := tee.NewTee(be.outFile, false)
	defer stdout.Close()
	be.cmd.Stdout = stdout

	// Copy stderr
	stderr := tee.NewTee(be.errFile, true)
	defer stderr.Close()
	be.cmd.Stderr = stderr

	// Connect to stdin
	be.cmd.Stdin = os.Stdin

	// Start process
	err := be.cmd.Start()
	if err != nil {
		log.Fatal(err)
	}

	be.exitCode = be.executeCommandTimeout(osSignal)
	if DEBUG {
		log.Printf("Debug executeCommand: Exit code %d\n", be.exitCode)
	}
	return be.exitCode
}

/*
	Write exit information to 'exitFile'
*/
func (be *BdsExec) updateExitFile(exitStr string) {
	if (be.exitFile != "") && (be.exitFile != "-") {
		if DEBUG {
			log.Printf("Debug updateExitFile: Writing exit status '%s' to exit file '%s'\n", exitStr, be.exitFile )
		}
		fileutil.WriteFile(be.exitFile, exitStr)
	}
}
