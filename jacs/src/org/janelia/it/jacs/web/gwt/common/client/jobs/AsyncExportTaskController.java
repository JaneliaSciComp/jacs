
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