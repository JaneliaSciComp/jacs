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

package org.janelia.it.jacs.compute.engine.service;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;

/**
 * This is the base class for all Service SLSBs.  It wraps a pojo service instance and can be used to demarcate
 * the transaction boundary of the service e.g. CreateBlastResultFileNodeSLSB needs to run in a separate transaction
 * context.  It extends this class without having to worry about preparation work needed for execution of the pojo
 * service.
 *
 * @author Tareq Nabeel
 */
public abstract class BaseServiceSLSB implements IService {
    public abstract IService getService(IProcessData processData);

    public void execute(IProcessData processData) throws ServiceException {
        try {
            ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            IService service = getService(processData);
            service.execute(processData);
        }
        catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }
}
