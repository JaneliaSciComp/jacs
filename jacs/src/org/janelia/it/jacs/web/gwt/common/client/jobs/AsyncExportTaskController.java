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

package org.janelia.it.jacs.web.gwt.common.client.jobs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.web.gwt.common.client.popup.CancelListener;
import org.janelia.it.jacs.web.gwt.common.client.popup.download.AsyncTaskMonitorPopupImpl;
import org.janelia.it.jacs.web.gwt.common.client.service.ExportService;
import org.janelia.it.jacs.web.gwt.common.client.service.ExportServiceAsync;

/**
 * Concrete implementation of AsyncFileDownloadTaskController that starts an export task.
 *
 * @author Michael Press
 */
public class AsyncExportTaskController extends AsyncFileDownloadTaskController {
    private static ExportServiceAsync _exportService = (ExportServiceAsync) GWT.create(ExportService.class);

    static {
        ((ServiceDefTarget) _exportService).setServiceEntryPoint("export.srv");
    }

    public AsyncExportTaskController(ExportTask task) {
        super(task);
    }

    protected AsyncTaskMonitorPopup createPopup(CancelListener cancelListener) {
        return new AsyncTaskMonitorPopupImpl("Export", "export", cancelListener);
    }

    protected void submitTask(final AsyncCallback callback) {
        _exportService.submitExportTask((ExportTask) getTask(), new AsyncCallback() {
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            public void onSuccess(Object result) {
                callback.onSuccess(result);
            }
        });
    }

    protected String getTaskType() {
        return "export";
    }
}