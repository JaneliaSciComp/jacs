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

package org.janelia.it.jacs.server.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.server.utils.SystemException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 20, 2008
 * Time: 3:13:32 PM
 */
public class ExportAPI {
    static Logger logger = Logger.getLogger(ExportAPI.class.getName());

    TaskDAO taskDAO;
    ComputeBeanRemote computeBean;

    public void setTaskDAO(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    public void setComputeBean(ComputeBeanRemote computeBean) {
        this.computeBean = computeBean;
    }

    public String submitExportTask(User requestingUser, ExportTask exportTask) throws SystemException {
        try {
            exportTask.setOwner(requestingUser.getUserLogin());
            exportTask.setJobName(exportTask.getSuggestedFilename());
            exportTask = (ExportTask) taskDAO.saveOrUpdateTask(exportTask);
            computeBean.submitJob("FileExport", exportTask.getObjectId());
            logger.debug("Export job submitted successfully.");
            return exportTask.getObjectId().toString();
        }
        catch (Throwable e) {
            logger.error("Error processing submitExportTask: " + e.getMessage());
        }
        throw new SystemException("Unable to submit the export task.");
    }
}
