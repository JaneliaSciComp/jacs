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

package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.prokAnnotation.GipLauncherTask;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:34 PM
 */
public class GipLauncherService extends ProkAnnotationBaseService {

    public String getCommandLine() throws ServiceException {
        if (null != task.getParameter(GipLauncherTask.PARAM_ASSEMBLY_ID) && !"".equals(task.getParameter(GipLauncherTask.PARAM_ASSEMBLY_ID)) &&
                null != task.getParameter(GipLauncherTask.PARAM_GENE_TYPE) && !"".equals(task.getParameter(GipLauncherTask.PARAM_GENE_TYPE))) {
            return "gip_launcher.pl -U " + _databaseUser + " -P " + _databasePassword + " -D " + _targetDatabase +
                    " -A ISCURRENT" + " -G locus" +
                    " -g " + getDefaultProjectCode();
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the assembly id and/or gene type was undefined.");
        }
    }

    @Override
    protected String getSGEQueue() {
        return "-l fast";
    }

}