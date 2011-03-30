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

package org.janelia.it.jacs.compute.drmaa;

import org.janelia.it.jacs.model.status.GridJobStatus;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: May 29, 2009
 * Time: 3:25:21 PM
 */
public class TextJobStatusLogger implements JobStatusLogger {
    private PrintWriter writer;

    public TextJobStatusLogger(PrintWriter pw) {
        writer = pw;
    }

    public TextJobStatusLogger(PrintStream ps) {
        writer = new PrintWriter(ps);
    }

    public TextJobStatusLogger(OutputStream os) {
        writer = new PrintWriter(os);
    }

    public long getTaskId() {
        return 0;
    }

    public void bulkAdd(Set<String> jobIds, String queue, GridJobStatus.JobState state) {
        writer.print("Processing new set of jobs on queue '" + queue + "' :");
        for (String id : jobIds)
            writer.print(" " + id);

        writer.println();
    }

    public void updateJobStatus(String jobId, GridJobStatus.JobState state) {
        writer.println("Job " + jobId + " status changed to :" + state);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void bulkUpdateJobStatus(Map<String, GridJobStatus.JobState> jobStates) {
        for (String id : jobStates.keySet()) {
            updateJobStatus(id, jobStates.get(id));
        }
    }

    public void updateJobInfo(String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) {
        GridJobStatus tmpStatus = new GridJobStatus();
        if (tmpStatus != null) {
            tmpStatus.setJobState(state);
            tmpStatus.updateFromMap(infoMap);
        }
        writer.println("Job's " + jobId + " status changed:");
        writer.println(tmpStatus.toString());

    }

    public void cleanUpData() {
        // NOTHING TO DO HERE
    }

}