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

package org.janelia.it.jacs.web.gwt.rnaSeq.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.janelia.it.jacs.model.tasks.rnaSeq.RnaSeqPipelineTask;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 31, 2010
 * Time: 3:38:29 PM
 */
public class SubmitRnaSeqPipelineJob {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.rnaSeq.client.SubmitRnaSeqPipelineJob");

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    private RnaSeqPipelineTask rnaSeqPipelineTask;
    private JobSubmissionListener _listener;

    public SubmitRnaSeqPipelineJob(RnaSeqPipelineTask rnaSeqPipelineTask, JobSubmissionListener jobSubmissionListener) {
        this.rnaSeqPipelineTask=rnaSeqPipelineTask;
        _listener = jobSubmissionListener;
    }

    public void runJob() {
        try {
            if (null == rnaSeqPipelineTask || null == rnaSeqPipelineTask.getParameterKeySet()) {
                _logger.error("RnaSeqPipelineTask is null or the parameters are null. Returning from syncTask.");
                return;
            }
            _logger.debug("Running RnaSeq Pipeline");
            _dataservice.submitJob(rnaSeqPipelineTask, new AsyncCallback() {
                public void onFailure(Throwable throwable) {
                    // track the failure of submission
                    SystemWebTracker.trackActivity("SubmitRnaSeqPipelineJobFailure");
                    notifyFailure(throwable);
                }

                public void onSuccess(Object object) {
                    // track the success of submission
                    SystemWebTracker.trackActivity("SubmitRnaSeqJobSuccessful");
                    String jobNumber = (String) object;
                    rnaSeqPipelineTask.setObjectId(new Long((String) object));
                    notifySuccess(jobNumber);
                }
            });
        }
        catch (Throwable throwable) {
            _logger.error("Error in SubmitRnaSeqPipelineJob runJob():" + throwable.getMessage(), throwable);
            notifyFailure(throwable);
        }
    }

    private void notifyFailure(Throwable throwable) {
        SystemWebTracker.trackActivity("SubmitRnaSeqPipelineJobJobFailure");
        if (_listener != null)
            _listener.onFailure(throwable);
    }

    private void notifySuccess(String jobId) {
        SystemWebTracker.trackActivity("SubmitRnaSeqPipelineJobSuccessful");
        if (_listener != null)
            _listener.onSuccess(jobId);
    }

}
