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

package org.janelia.it.jacs.compute.launcher.blast;

import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.def.SeriesDef;
import org.janelia.it.jacs.compute.engine.launcher.ILauncher;
import org.janelia.it.jacs.compute.engine.launcher.LauncherException;
import org.janelia.it.jacs.compute.engine.launcher.ejb.SequenceLauncherSLSB;
import org.janelia.it.jacs.compute.engine.service.ServiceException;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * @author Tareq Nabeel
 */
@Stateless
@TransactionAttribute(value = TransactionAttributeType.MANDATORY)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 60, timeout = 10000)
public class BlastLauncherSLSB extends SequenceLauncherSLSB {

    /**
     * You don't have to give up and rethrow the exception to the initiator.  You can retry if you really
     * know what you're doing.  Beware of retries.  The transaction might have sunk into a corrupted state.
     * Put different error codes into your Service exception when you throw them within your services and
     * analyze the code in the catch blocks below if you want to go for retries.  ServiceException
     * has an error code data member.
     *
     * @param launcher    The Sequence Launcher instance
     * @param seriesDef   sequence definition
     * @param processData running state of the process
     * @throws LauncherException
     * @throws ServiceException
     */
    protected void launch(ILauncher launcher, SeriesDef seriesDef, IProcessData processData) throws ComputeException {
        launcher.launch(seriesDef, processData);
    }
}
