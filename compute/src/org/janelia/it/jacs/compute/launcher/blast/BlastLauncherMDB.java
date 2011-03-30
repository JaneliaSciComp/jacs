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
import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * You don't have to give up and rollback the exception as the base SequenceLauncherMDB class does.
 * You can override the handleException method of the base SeriesLauncherMDB if you want to go for retries
 * and really know what you're doing.  Beware of retries.  The transaction might have sunk into a corrupted
 * state. Put different error codes into your Service exception when you throw them within your services and
 * analyze the code in the catch blocks below if you want to go for retries.  ServiceException
 * has an error code data member.
 *
 * @author Tareq Nabeel
 */
@MessageDriven(name = "BlastLauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/blastLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "60"),
//    @ActivationConfigProperty(propertyName="MaxMessages", propertyValue="30"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 60, timeout = 10000)
public class BlastLauncherMDB extends SeriesLauncherMDB {
}
