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

package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.tasks.Task;

import java.rmi.RemoteException;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jun 25, 2008
 * Time: 3:04:56 PM
 * <p/>
 * <p/>
 * This class based on Tareq's famous 'BlastRunner' in same package.
 */
public class ExportRunner {

    // Could have list of static params here matching exportParams.properties

    private String jobName;
    private String userName;
    private boolean waitForCompletion = true;
    private String outputDir;
    private Task savedTask;
    private ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();

    /**
     * @throws Exception
     */
    public void run() throws RemoteException {
        submitJob();
    }

    private void submitJob() throws RemoteException {
        System.out.println("Submitted Job: " + jobName + " Id: " + savedTask.getObjectId());
        computeBean.submitJob("FileExport", savedTask.getObjectId());
    }

    public Task configureAndSaveTask(Task task) throws DaoException, RemoteException {
        setDefaultJobName();
        task.setJobName(jobName);
        savedTask = computeBean.saveOrUpdateTask(task);
        return savedTask;
    }

    private void setDefaultJobName() {
        jobName = "FileExportJob-" + String.valueOf(System.currentTimeMillis());
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isWaitForCompletion() {
        return waitForCompletion;
    }

    public void setWaitForCompletion(boolean waitForCompletion) {
        this.waitForCompletion = waitForCompletion;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public Task getSavedTask() {
        return savedTask;
    }

}
