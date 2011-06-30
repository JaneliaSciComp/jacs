/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.shared.utils;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2006
 * Time: 1:44:26 PM
 */
public class SystemCall {

    private Logger logger;

    public static final String UNIX_SHELL = "sh";
    public static final String UNIX_SHELL_FLAG = "-c";
    // Different flavors of Windows will need different shell and flag information
    public static final String WIN_SHELL = "cmd.exe";
    public static final String WIN_SHELL_FLAG = "/c";

    public static final String SCRATCH_DIR_PROP = "SystemCall.ScratchDir";
    public static final String SHELL_PATH_PROP = "SystemCall.ShellPath";
    public static final String STREAM_DIRECTOR_PROP = "SystemCall.StreamDirector";

    private static final String DEFAULT_SCRATCH_DIR = "/scratch/jboss/tmp_exec";
    private static final String DEFAULT_SHELL_PATH = "/bin/bash";
    private static final String DEFAULT_STREAM_DIRECTOR = ">&";

    private File scratchParent = null;
    private File scratchDir = null;
    private String shellPath = null;
    private String streamDirector = null;

    private Random random = new Random();

    public SystemCall(Logger logger) {
        this.logger = logger;
    }

    public SystemCall(Properties props, File scratchParentDir, Logger logger) {
        this.logger = logger;
        configure(props, scratchParentDir);
    }

    protected void configure(Properties props, File scratchParentDir) {
        if (props != null) {
            String scratchDirParentName = props.getProperty(SCRATCH_DIR_PROP);
            if (scratchDirParentName != null) {
                scratchParent = new File(scratchDirParentName);
            }
            else {
                scratchParent = scratchParentDir;
            }
            String shellPathString = props.getProperty(SHELL_PATH_PROP);
            if (shellPathString != null) shellPath = shellPathString;
            String streamDirectorString = props.getProperty(STREAM_DIRECTOR_PROP);
            if (streamDirectorString != null) streamDirector = streamDirectorString;
        }
        if (scratchParent == null) {
            scratchParent = scratchParentDir;
        }
        if (scratchParent == null) {
            scratchParent = new File(DEFAULT_SCRATCH_DIR);
        }
        if (shellPath == null) shellPath = DEFAULT_SHELL_PATH;
        if (streamDirector == null) streamDirector = DEFAULT_STREAM_DIRECTOR;
        if (!scratchParent.exists()) {
            if (!scratchParent.mkdirs()) {
                throw new RuntimeException("Could not create " + scratchParent.getAbsolutePath());
            }
        }
        if (!scratchParent.canWrite()) {
            throw new RuntimeException("Directory " + scratchParent.getAbsolutePath() + " can not be written to");
        }
        if (scratchDir == null) {
            this.scratchDir = new File(scratchParent, getNextIDString());
        }
        logger.info("Creating scratchDir=" + scratchDir.getAbsolutePath());
        if (!this.scratchDir.exists()) {
            if (!scratchDir.mkdirs()) {
                throw new RuntimeException("Could not create scratch directory " + this.scratchDir.getAbsolutePath());
            }
            try {
                Thread.sleep(1000);
            }
            catch (Exception ex) {/*Do nothing*/}
            if (!scratchDir.exists()) {
                throw new RuntimeException("Could not verify new scratch directory=" + this.scratchDir.getAbsolutePath());
            }
            else {
                logger.info("New scratch directory verified=" + scratchDir.getAbsolutePath());
            }

        }
    }

    public int execute(String call, boolean captureOutput) throws IOException, InterruptedException {
        if (scratchDir.exists()) {
//            logger.info("SystemCall execute verified scratchDir="+scratchDir.getAbsolutePath());
        }
        else {
            throw new IOException("Could not verify scratchDir=" + scratchDir.getAbsolutePath());
        }
        File tmpFile = new File(scratchDir, "system_exec_" + getNextIDString());
        File outFile = new File(tmpFile.getAbsolutePath() + ".out");
        FileWriter writer = new FileWriter(tmpFile);
        try {
            if (captureOutput) {
                writer.write(call + " " + streamDirector + " " + outFile.getAbsolutePath());
            }
            else {
                writer.write(call);
            }
        }
        finally {
            writer.close();
        }
        boolean notExists;
        boolean notSize = true;
        int tries = 0;
        do {
            tries++;
            if (tries > 20)
                throw new RuntimeException("Execution file " + tmpFile.getAbsolutePath() + " was not written by operation system in time");
            Thread.sleep(100); // sleep for 100 miliseconds
            notExists = !(tmpFile.exists());
            if (!notExists) notSize = !(tmpFile.length() > 0);
        }
        while (notExists || notSize);
        if (tries > 1)
            System.err.println("Note: system required " + tries + " 100msec wait periods for system_exec file");
        String execString = shellPath + " " + tmpFile.getAbsolutePath();
        logger.info("SystemCall executing script shell=" + shellPath + " path=" + tmpFile.getAbsolutePath());
        Process process = null;
        int exitVal;
        try {
            process = Runtime.getRuntime().exec(execString);
            exitVal = Integer.MIN_VALUE;
            while (exitVal == Integer.MIN_VALUE) {
                try {
                    exitVal = process.waitFor();
                }
                catch (InterruptedException ex) {
                    System.err.println("SystemCall.execute() : intercepted InterruptedException. Continuing...");
                }
            }
        }
        finally {
            if (null != process) {
                closeStream(process.getOutputStream());
                closeStream(process.getInputStream());
                closeStream(process.getErrorStream());
                process.destroy(); // force immediate release of resources back to OS
            }
        }
        if (exitVal == 0) {
            boolean tmpDelSuccess = tmpFile.delete();
            boolean tmpOutSuccess = outFile.delete();
            if (!tmpDelSuccess) {
                System.err.println("SystemCall unable to delete temp file: " + tmpFile.getAbsolutePath());
            }
            if (!tmpOutSuccess) {
                System.err.println("SystemCall unable to delete output file: " + outFile.getAbsolutePath());
            }
            // scratchDir.delete(); We do not want to delete scratchDir because this could be reused by class - use cleanup()
        }
        return exitVal;
    }

    protected String getNextIDString() {
        return "" + new Date().getTime() + "_" + Math.abs(random.nextLong());
    }

    /**
     * This method is used when the developer wants to run a command line, verbatim.
     * OS-specific character sequences on the command line must be processed specially, and this method
     * is intended to take that into account. Runtime.exec() cannot handle os streams by itself.
     * This code was found online from a public source.
     *
     * @param desiredCommandLine - line which would be pasted into a shell for execution
     * @param isUnixStyleSystem  - this can be derived and does not need to be passed
     * @return int - the exit value of the command run
     * @throws IOException          - error executing the thread
     * @throws InterruptedException - error used to stop the wait state
     */
    public int emulateCommandLine(String desiredCommandLine, boolean isUnixStyleSystem) throws IOException, InterruptedException {
        return this.emulateCommandLine(desiredCommandLine, isUnixStyleSystem, null, null, 0);
    }

    public int emulateCommandLine(String desiredCommandLine, boolean isUnixStyleSystem, int timeoutSeconds) throws IOException, InterruptedException {
        return this.emulateCommandLine(desiredCommandLine, isUnixStyleSystem, null, null, timeoutSeconds);
    }

    /**
     * This method is used when the developer wants to run a command line, verbatim.
     * OS-specific character sequences on the command line must be processed specially, and this method
     * is intended to take that into account. Runtime.exec() cannot handle os streams by itself.
     * This code was found online from a public source.
     *
     * @param desiredCommandLine - line which would be pasted into a shell for execution
     * @param isUnixStyleSystem  - this can be derived and does not need to be passed
     * @param envVariables       - environment variables which can be overridden
     * @param directory          - directory to run the comand from
     * @return int - the exit value of the command run
     * @throws IOException          - error executing the thread
     * @throws InterruptedException - error used to stop the wait state
     */
    public int emulateCommandLine(String desiredCommandLine, boolean isUnixStyleSystem, String[] envVariables, File directory, int timeoutSeconds) throws IOException, InterruptedException {
        String[] args = new String[]{"", "", ""};
        if (logger.isDebugEnabled()) {
            logger.debug("Executing: " + desiredCommandLine);
        }
        if (isUnixStyleSystem) {
            args[0] = UNIX_SHELL;
            args[1] = UNIX_SHELL_FLAG;
        }
        else {
            args[0] = WIN_SHELL;
            args[1] = WIN_SHELL_FLAG;
        }
        args[2] = desiredCommandLine;

        Process proc = null;
        StreamGobbler errorGobbler, outputGobbler;
        int exitVal = -1;
        try {
            Runtime rt = Runtime.getRuntime();
            proc = rt.exec(args, envVariables, directory);
            // any error message?
            errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");

            // any output?
            outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT");

            // kick them off
            errorGobbler.start();
            outputGobbler.start();

            // any error??? make sure execution produces a real exit value
            if (timeoutSeconds==0) {
                exitVal = proc.waitFor();
            } else {
                Date startTime=new Date();
                boolean finishOnTime=false;
                while( (new Date().getTime() - startTime.getTime()) < timeoutSeconds*1000) {
                    Thread.sleep(1000);
                    try {
                        exitVal=proc.exitValue();
                        finishOnTime=true;
                    } catch (IllegalThreadStateException e) {}
                }
                if (!finishOnTime) {
                    logger.error("Process exceeded maximum timeout of " + timeoutSeconds + " seconds");
                    exitVal=1;
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("ExitValue: " + exitVal);
            }
        }
        finally {
            if (null != proc) {
                closeStream(proc.getOutputStream());
                closeStream(proc.getInputStream());
                closeStream(proc.getErrorStream());
                proc.destroy(); // force immediate release of resources back to OS
            }
        }
        return exitVal;

    }

    private void closeStream(Closeable c) {
        if (c != null) {
            try {
                c.close();
            }
            catch (IOException e) {
                // ignored
            }
        }
    }

    class StreamGobbler extends Thread {
        InputStream is;
        String type;

        StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        public void run() {
            BufferedReader br = null;
            try {
                InputStreamReader isr = new InputStreamReader(is);
                br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(type + ">" + line);
                    }
                }
            }
            catch (IOException ioe) {
                // ioe.printStackTrace(); this was causing occasional harmless output
                if (!ioe.getMessage().equalsIgnoreCase("Stream closed")) {
                    logger.error("IOException: " + ioe.getMessage() + " (not stream closed) in SystemCall StreamGobbler type=" + type);
                }
            }
            finally {
                if (null != br) {
                    try {
                        br.close();
                    }
                    catch (IOException e) {
                        logger.error("Error trying to close the SystemCall buffered stream reader. " + e.getMessage());
                    }
                }
            }
        }

    }

    public void cleanup() {
        if (scratchDir != null) {
            FileUtil.cleanDirectory(scratchDir);
            boolean scratchDirSuccess = scratchDir.delete();
            if (!scratchDirSuccess) {
                System.err.println("SystemCall unable to delete scratch dir: " + scratchDir.getAbsolutePath());
            }
        }
    }

    public File getScratchDir() {
        return scratchDir;
    }

}
