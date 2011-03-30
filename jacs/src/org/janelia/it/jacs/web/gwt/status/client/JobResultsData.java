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

package org.janelia.it.jacs.web.gwt.status.client;

import org.janelia.it.jacs.shared.tasks.JobInfo;

/**
 * Holds data about the currently selected job
 */
public class JobResultsData {
    private JobInfo _job;    // selected job
    private String _jobId;      // we might only have the id from URL param if entry point is loaded on later page
    private String _cameraAcc;

    public void setJob(JobInfo job) {
        _job = job;
    }

    public JobInfo getJob() {
        return _job;
    }

    /**
     * Temporary holding of job id while job is retrieved
     */
    public void setJobId(String jobId) {
        _jobId = jobId;
    }

    public String getJobId() {
        return _jobId;
    }

    public String getDetailAcc() {
        return _cameraAcc;
    }

    public void setDetailAcc(String cameraAcc) {
        _cameraAcc = cameraAcc;
    }

}
