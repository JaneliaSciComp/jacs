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

package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.QueueMessage;
import org.janelia.it.jacs.compute.engine.service.GridSubmitHelperMap;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 2, 2007
 * Time: 5:00:41 PM
 */
public class AdministrativeManager implements AdministrativeManagerMBean {

    private static final Logger LOGGER = Logger.getLogger(AdministrativeManager.class);

    public AdministrativeManager() {
    }

    /**
     * Method to remove any core dump files (core* and hs_err_pid*) in any directory underneath that passed
     * in as a parameter (one level down, not recursive)
     *
     * @param rootDirectory - directory where the core file search starts and goes to subdirectories
     */
    public void cleanupCoreDumps(String rootDirectory) {
        String tmpDirPath = "";
        try {
            File systemRecDir = new File(rootDirectory);
            String[] recruitmentDirs = systemRecDir.list();
            for (String recruitmentDir : recruitmentDirs) {
                File tmpFile = new File(systemRecDir.getAbsolutePath() + File.separator + recruitmentDir);
                tmpDirPath = tmpFile.getAbsolutePath() + File.separator;
                if (tmpFile.isDirectory()) {
                    File[] coreFiles = tmpFile.listFiles(new CoreFilenameFilter());
                    for (File coreFile : coreFiles) {
                        if (LOGGER.isDebugEnabled())
                            LOGGER.debug("\n\n\nDirectory " + tmpDirPath + "\nCalling delete on " + coreFile.getAbsolutePath());
                        boolean deleteSuccess = coreFile.delete();
                        if (!deleteSuccess){
                            System.err.println("Unable to delete file "+coreFile.getAbsolutePath());
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("\n\n\n*****\nError cleaning up dir " + tmpDirPath + "\n" + e.getMessage() + "*****\n\n\n");
        }
    }

    public class CoreFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.indexOf("core") >= 0) || (name.indexOf("hs_err_pid") >= 0 || name.indexOf(".cleaned") > 0);
        }
    }

    public void cleanupOOSFiles(String systemBlastRootDir) {
        try {
            File systemRecDir = new File(systemBlastRootDir);
            File[] blastDirs = systemRecDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return !name.startsWith("genome") && !name.startsWith("recruitment");
                }
            });
            for (File blastDir : blastDirs) {
                if (blastDir.isDirectory()) {
                    File[] outputDirs = blastDir.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.startsWith("r_");
                        }
                    });
                    for (File outputDir : outputDirs) {
                        if (outputDir.exists()) {
                            File[] oosFiles = outputDir.listFiles(new FilenameFilter() {
                                public boolean accept(File dir, String name) {
                                    return (name.toLowerCase().indexOf(".oos") >= 0);
                                }
                            });
                            for (File oosFile : oosFiles) {
                                System.out.println("Deleting:" + oosFile.getAbsolutePath());
                                boolean deleteSuccess = oosFile.delete();
                                if (!deleteSuccess){
                                    System.err.println("Unable to delete file "+oosFile.getAbsolutePath());
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("\n\n\n*****\nError cleaning up dir\n" + e.getMessage() + "*****\n\n\n");
        }
    }


    public void cleanSystemDirs(String nodeName, boolean debug) {
        try {
            List<Node> systemNodes = EJBFactory.getLocalComputeBean().getNodesByClassAndUser(nodeName, User.SYSTEM_USER_LOGIN);
            System.out.println("There are " + systemNodes.size() + " " + nodeName + " nodes owned by system.");
            for (Node systemNode : systemNodes) {
                if (!debug) {
                    EJBFactory.getRemoteComputeBean().deleteNode(User.SYSTEM_USER_LOGIN, systemNode.getObjectId(), true);
                }
                else {
                    System.out.println("Would have deleted node " + systemNode.getObjectId());
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanUpUsersAgainstLDAP() {
        final ArrayList<String> protectedDirs = new ArrayList<String>();
        protectedDirs.add("dma");
        protectedDirs.add("system");
        protectedDirs.add("load");
        protectedDirs.add("luceneIdx");
        protectedDirs.add("luceneIdxprotein");
        protectedDirs.add("nobody");
        protectedDirs.add("usersPendingDelete");
        File filestoreDir = new File(SystemConfigurationProperties.getString("FileStore.CentralDir"));
        String[] userDirectories = filestoreDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                File testFile = new File(dir.getAbsolutePath() + File.separator + name);
                File homeTest = new File("/home/" + name);
                return testFile.isDirectory() && (!homeTest.exists() && !protectedDirs.contains(name));
            }
        });
        try {
            SystemCall call = new SystemCall(LOGGER);
            for (String userDirectory : userDirectories) {
                call.emulateCommandLine("mv " + filestoreDir.getAbsolutePath() + File.separator + userDirectory + " " +
                        filestoreDir.getAbsolutePath() + File.separator + "usersPendingDelete" + File.separator + ".", true);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void nodeCheck(String username) {
        String rootDirectory = SystemConfigurationProperties.getString("FileStore.CentralDir");
        File filestoreDir = new File(rootDirectory);
        String[] userDirectories = filestoreDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("1");
            }
        });
        for (String userDirectory : userDirectories) {
            if (userDirectory.equals(username)) {
//                File userDir = new File(rootDirectory+File.separator+username) {
//
//                }
            }
        }
    }

    public void resubmitJobs(String processDefinition, String taskId) {
        try {
            EJBFactory.getRemoteComputeBean().addEventToTask(Long.valueOf(taskId), new Event(Event.RESUBMIT_EVENT, new Date(), Event.RESUBMIT_EVENT));
            EJBFactory.getRemoteComputeBean().submitJob(processDefinition, Long.valueOf(taskId));
        }
        catch (DaoException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // This should be showing us the processes being monitored.
    public void showCurrentGridProcessMap() {
        Set<String> keySet = GridSubmitHelperMap.getInstance().getDataMapKeys();
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("GridSubmitHelperMap Info:\n");
        try {
            // iterate over all monitored processes
            for (String submissionKey : keySet) {
                Map<String, Object> submissionData = GridSubmitHelperMap.getInstance().getFromDataMap(submissionKey);
                Process proc = (Process) submissionData.get(GridSubmitHelperMap.PROCESS_OBJECT);
                QueueMessage queueMessage = (QueueMessage) submissionData.get(GridSubmitHelperMap.ORIGINAL_QUEUE_MESSAGE_KEY);
                Task originalTask = (Task) queueMessage.getObjectMap().get("TASK");
                if (proc != null) {
                    sbuf.append("Process: ").append(proc.toString()).append("\n");
                    sbuf.append("Submission Key: ").append(submissionKey).append("\n");
                    if (null != originalTask) {
                        sbuf.append("Task ID: ").append(originalTask.getObjectId()).append("\n");
                        sbuf.append("Task Owner: ").append(originalTask.getOwner()).append("\n");
                        sbuf.append("Task Name: ").append(originalTask.getTaskName()).append("\n");
                        sbuf.append("Job Name: ").append(originalTask.getJobName()).append("\n");
                    }
                }
            }
            System.out.println(sbuf.toString());
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

}