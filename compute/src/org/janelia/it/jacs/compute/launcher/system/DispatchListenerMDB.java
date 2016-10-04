package org.janelia.it.jacs.compute.launcher.system;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.*;

/**
 * This typically runs on the same machine on which the actual computation is initiated and it kicks off
 * the actual dispatcher.
 */
@MessageDriven(activationConfig = {
        //@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
//        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/dispatchRequest"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "300")//,
        //@ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000")//,
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
//        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 300, timeout = 10000)
public class DispatchListenerMDB implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(DispatchListenerMDB.class);
    private static final String START_JOB_NAME = "StartComputation";

    @Override
    public void onMessage(Message message) {
        ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
        Long processId = null;
        try {
            MapMessage mapMessage = (MapMessage) message;
            processId = mapMessage.getLong(IProcessData.PROCESS_ID);
            Long jobId = Objects.firstNonNull(mapMessage.getLong(IProcessData.JOB_ID), -1L);
            LOG.info("Dispatch job for taskId/dispatchId: {}:{}", processId, jobId);
            computeBean.submitJob(START_JOB_NAME, ImmutableMap.<String, Object>of(
                                    IProcessData.PROCESS_ID, processId,
                                    IProcessData.JOB_ID, jobId));
        } catch (Exception e) {
            LOG.error("Error submitting start computation task", e);
            computeBean.recordProcessError(START_JOB_NAME, processId, e);
        }
    }
}
