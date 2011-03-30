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
import org.janelia.it.jacs.model.tasks.prokAnnotation.PressDbTask;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:04 PM
 */
public class PressDbService extends ProkAnnotationBaseService {

    public String getCommandLine() throws ServiceException {
        String prokDirPath = this._targetDirectory;
        File prokDir = new File(prokDirPath);
        File targetFile = null;
        if (prokDir.exists() && prokDir.isDirectory()) {
            File[] tmpFiles = prokDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(task.getParameter(PressDbTask.PARAM_FILE_SUFFIX));
                }
            });
            if (null != tmpFiles && tmpFiles.length > 0) {
                targetFile = tmpFiles[0];
            }
        }
        if (null != targetFile && targetFile.exists() &&
                null != task.getParameter(PressDbTask.PARAM_FILE_SUFFIX) && !"".equals(task.getParameter(PressDbTask.PARAM_FILE_SUFFIX))) {
            return "pressdb " + targetFile.getAbsolutePath();
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the file moniker or suffix was undefined.");
        }
    }

}
