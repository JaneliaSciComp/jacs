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

package org.janelia.it.jacs.compute.service.blast.submit;

import org.jboss.annotation.ejb.PoolClass;
import org.janelia.it.jacs.compute.engine.data.QueueMessage;
import org.janelia.it.jacs.compute.engine.service.BaseServiceMDB;
import org.janelia.it.jacs.compute.engine.service.IService;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * @author Tareq Nabeel
 *         <p/>
 *         !!!!!!!!!!!!!!!!!!!!!!!!!!!
 *         This service can only be used for synchronous execution of BLAST - use with extreme care
 */
@MessageDriven(name = "SubmitBlastJobMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/submitBlastJob"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "10"),
//        @ActivationConfigProperty(propertyName = "MaxMessages", propertyValue = "10"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 10, timeout = 10000)
public class SubmitBlastJobMDB extends BaseServiceMDB {

    public IService getService(QueueMessage queueMessage) {
        return new SynchronousBlastJobService();
    }

}
