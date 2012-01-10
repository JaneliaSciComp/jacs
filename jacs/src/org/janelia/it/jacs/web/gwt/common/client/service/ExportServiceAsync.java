
package org.janelia.it.jacs.web.gwt.common.client.service;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.tasks.export.ExportTask;

public interface ExportServiceAsync {
    public void submitExportTask(ExportTask exportTask, AsyncCallback callback);
}