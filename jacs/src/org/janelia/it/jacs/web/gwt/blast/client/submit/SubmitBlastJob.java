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

package org.janelia.it.jacs.web.gwt.blast.client.submit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.web.gwt.blast.client.BlastService;
import org.janelia.it.jacs.web.gwt.blast.client.BlastServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;

import java.util.Set;

/**
 * @author Michael Press
 */
public class SubmitBlastJob {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.blast.client.submit.SubmitBlastJob");

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);
    private static BlastServiceAsync _blastService = (BlastServiceAsync) GWT.create(BlastService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
        ((ServiceDefTarget) _blastService).setServiceEntryPoint("blast.srv");
    }

    private BlastData _blastData;
    private JobSubmissionListener _listener;
    private AsyncCallback _uploadListener;

    public SubmitBlastJob(BlastData blastData, JobSubmissionListener jobSubmissionListener,
                          AsyncCallback uploadListener) {
        _blastData = blastData;
        _listener = jobSubmissionListener;
        _uploadListener = uploadListener;
    }

    public void runJob() {
        // If the query is a previous sequence, just run the job
        Set<String> previousQuerySet = _blastData.getQuerySequenceDataNodeMap().keySet();
        if (previousQuerySet != null && previousQuerySet.size() > 0)
            runBlastJob(previousQuerySet.iterator().next());
        else // Else we have to upload the sequence first, then run the job with the new queyr sequence's ID
            uploadQuerySequence();
    }

    private void uploadQuerySequence() {
        _dataservice.saveUserDefinedFastaNode(_blastData.getMostRecentlySpecifiedQuerySequenceName(),
                _blastData.getUserReferenceFASTA(), Node.VISIBILITY_PRIVATE, new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        _logger.error("Failed in attempt to save the user data node. Exception:" + throwable.getMessage());
                        notifyFailure(throwable);
                    }

                    public void onSuccess(Object object) {
                        if (object == null)
                            notifyFailure(null);
                        else {
                            _logger.debug("Successfully called the service to save the user data node. NodeId=" + object.toString());
                            UserDataNodeVO newVO = (UserDataNodeVO) object;
                            notifyUploadSuccess(newVO);
                            runBlastJob(newVO.getDatabaseObjectId());
                        }
                    }
                });
    }

    private void runBlastJob(String queryNodeId) {
        try {
            Task blastTask = _blastData.getBlastTask();
            if (null == blastTask || null == blastTask.getParameterKeySet()) {
                _logger.error("Blast Task in getData is null or the parameters are null. Returning from syncTask.");
                return;
            }
            if (null == _blastData.getSubjectSequenceDataNodeMap() || null == queryNodeId) {
                _logger.error("Blast subject dbs or query dbs are null. Returning from syncTask.");
                return;
            }
            blastTask.setParameter(BlastTask.PARAM_query, queryNodeId);
            blastTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(_blastData.getSubjectDatasetIdsList()));
            _logger.debug("Saved the query Node, now running Blast Job");
            _blastService.runBlastJob(blastTask, new AsyncCallback() {
                public void onFailure(Throwable throwable) {
                    // track the failure of Blast submission
                    SystemWebTracker.trackActivity("SubmitBlastJobFailure");
                    notifyFailure(throwable);
                }

                public void onSuccess(Object object) {
                    // track the success of Blast submission
                    SystemWebTracker.trackActivity("SubmitBlastJobSuccessful");
                    String jobNumber = (String) object;
                    _blastData.setJobNumber((String) object);
                    notifySuccess(jobNumber);
                }
            });
        }
        catch (Throwable throwable) {
            _logger.error("Error in setupBlastJob():" + throwable.getMessage(), throwable);
            notifyFailure(throwable);
        }
    }

    private void notifyFailure(Throwable throwable) {
        SystemWebTracker.trackActivity("SubmitBlastJobFailure");
        if (_listener != null)
            _listener.onFailure(throwable);
    }

    private void notifySuccess(String jobId) {
        SystemWebTracker.trackActivity("SubmitBlastJobSuccessful");
        if (_listener != null)
            _listener.onSuccess(jobId);
    }

    private void notifyUploadSuccess(UserDataNodeVO node) {
        if (_uploadListener != null)
            _uploadListener.onSuccess(node);
    }
}
