
package org.janelia.it.jacs.web.gwt.common.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.server.api.ExportAPI;
import org.janelia.it.jacs.server.utils.SystemException;
import org.janelia.it.jacs.web.gwt.common.client.service.ExportService;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;

public class ExportServiceImpl extends JcviGWTSpringController implements ExportService {

    static Logger logger = Logger.getLogger(ExportServiceImpl.class.getName());

    private ExportAPI exportAPI = new ExportAPI();

    public void setExportAPI(ExportAPI exportAPI) {
        this.exportAPI = exportAPI;
    }

    public String submitExportTask(ExportTask exportTask) throws GWTServiceException {
        try {
            return exportAPI.submitExportTask(getSessionUser(), exportTask);
        }
        catch (SystemException e) {
            e.printStackTrace();
            throw new GWTServiceException("Error submitting the export task.");
        }
    }
}

