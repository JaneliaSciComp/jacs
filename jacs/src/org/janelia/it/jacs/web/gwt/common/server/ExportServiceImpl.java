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

