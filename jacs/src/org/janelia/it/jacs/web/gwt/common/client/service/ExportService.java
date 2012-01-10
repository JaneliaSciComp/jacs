
package org.janelia.it.jacs.web.gwt.common.client.service;

import com.google.gwt.user.client.rpc.RemoteService;
import org.janelia.it.jacs.model.tasks.export.ExportTask;

public interface ExportService extends RemoteService {
    public String submitExportTask(ExportTask exportTask) throws GWTServiceException;
}