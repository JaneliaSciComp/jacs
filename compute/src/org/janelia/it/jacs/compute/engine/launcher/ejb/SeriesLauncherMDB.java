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

package org.janelia.it.jacs.compute.engine.launcher.ejb;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.data.QueueMessage;
import org.janelia.it.jacs.compute.engine.def.SeriesDef;
import org.janelia.it.jacs.compute.engine.launcher.ILauncher;
import org.janelia.it.jacs.compute.engine.launcher.ProcessLauncher;
import org.janelia.it.jacs.compute.engine.launcher.SequenceLauncher;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.engine.service.ValidServiceException;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;

import javax.ejb.EJBException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

/**
 * This MDB wraps a SeriesLauncher and is used for asynchronous launching of a series action.
 * Because it runs in a new transaction context, it rollbacks the transaction when something goes wrong
 * in the launcher.  Before it does so it sends a jms response to the initiator of the process action if
 * a response was required.
 *
 * @author Tareq Nabeel
 */
public abstract class SeriesLauncherMDB implements MessageListener {
    private static Logger logger = Logger.getLogger(SeriesLauncherMDB.class);

    public void onMessage(Message message) {
        if (logger.isInfoEnabled()) {
            logger.info(getClass().getName() + " onMessage...");
        }
        processMessage(message);
    }

    protected void processMessage(Message message) {
        QueueMessage queueMessage = null;
        SeriesDef seriesToProcess = null;
        try {
            queueMessage = new QueueMessage((ObjectMessage) message, false);
            // ActionToProcess could have changed after execution of service
            // It's important to grab this first
            seriesToProcess = getActionToProcess(queueMessage);
            getLocalLauncher(seriesToProcess).launch(seriesToProcess, queueMessage);
            if (seriesToProcess.getQueueToLinkTo() != null) {
                JmsUtil.forwardToLinkedToQueue(queueMessage, seriesToProcess);
            }
            else {
                JmsUtil.replyToReceivedFromQueue(queueMessage, seriesToProcess);
            }
        }
        catch (ComputeException e) {
            handleException(seriesToProcess, queueMessage, e);
        }
        catch (JMSException e) {
            handleException(seriesToProcess, queueMessage, new ServiceException(e));
        }
    }

    private ILauncher getLocalLauncher(SeriesDef sDef) throws ComputeException {
        if (sDef.isProcess())
            return new ProcessLauncher();
        else if (sDef.isSequence())
            return new SequenceLauncher();
        else
            throw new ComputeException("Invalid SeriesDef object. Only children of Series are allowed");
    }

    protected void handleException(SeriesDef seriesToProcess, QueueMessage queueMessage, ComputeException e) {
        JmsUtil.replyToReceivedFromQueue(queueMessage, seriesToProcess, e);

        if (e instanceof ValidServiceException) {
            // Do Nothing as this a valid exception
        }
        else {
            throw new EJBException(e);   // rollback
        }
    }

    private SeriesDef getActionToProcess(QueueMessage queueMessage) {
        try {
            return (SeriesDef) queueMessage.getActionToProcess();
        }
        catch (MissingDataException e) {
            throw new EJBException(e);
        }
    }
}
