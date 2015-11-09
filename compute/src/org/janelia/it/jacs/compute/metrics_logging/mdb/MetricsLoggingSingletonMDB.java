package org.janelia.it.jacs.compute.metrics_logging.mdb;

import org.apache.log4j.Logger;

import org.jboss.ejb3.StrictMaxPool;
import org.jboss.annotation.ejb.PoolClass;
import javax.ejb.MessageDriven;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.Date;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.ComputeException;
import static org.janelia.it.jacs.shared.annotation.metrics_logging.MetricsLoggingConstants.*;
import org.janelia.it.jacs.model.user_data.UserToolEvent;

/**
 * Enforces a throttling-to-one-thread writeback to avoid excessive resource
 * use by this, potentially-swarmed, bean.  This is a low-priority operation,
 * and can afford to wait, as long as the timestamp(s) are accurately added
 * to the map.
 * 
 * Created by fosterl on 11/4/2015.
 */
@SuppressWarnings("unused")
@MessageDriven(name = "MetricsLoggingSingletonMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = QUEUE ),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        // This value must remain at 1.  Single use/no concurrency.
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1"),
        // Setting a very low value on trax-timeout.  Twofold reason:
        //  1. should NOT need a very long time for this.
        //  2. should NOT queue up this lo-pri/singleton resource.
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "1"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = StrictMaxPool.class, maxSize = 1, timeout = 10000)
public class MetricsLoggingSingletonMDB implements MessageListener {
    
    private Logger logger = Logger.getLogger( MetricsLoggingSingletonMDB.class );

    @Override
    public void onMessage(Message message) {
        Long sessionId;
        String userLogin;
        String toolName;
        String category;
        String action;
        Date timestamp;
        
        if ( message instanceof  MapMessage ) {
            MapMessage mapMessage = (MapMessage)message;
            try {
                //Long sessionId, String userLogin, String toolName, String category, String action, Date timestamp
                sessionId = mapMessage.getLong(SESSION_ID_KEY);
                userLogin = mapMessage.getString(USER_LOGIN_KEY);
                toolName = mapMessage.getString(TOOL_NAME_KEY);
                category = mapMessage.getString(CATEGORY_KEY);
                action = mapMessage.getString(ACTION_KEY);
                timestamp = new Date();
                timestamp.setTime(mapMessage.getLong(TIMESTAMP_KEY));
                
            } catch ( JMSException jmse ) {
                logger.error("Failed to obtain/use parameters for logging.");
                jmse.printStackTrace();
                throw new EJBException(jmse);
            }
         
            UserToolEvent event = new UserToolEvent(sessionId, userLogin, toolName, category, action, timestamp);
            try {
                // Pump this event, using the compute infrastructure.
                new ComputeDAO(logger).addEventToSession(event);
            } catch (ComputeException ce) {
                logger.error("Failed to log the user tool event.  action=" + action);
                ce.printStackTrace();
                throw new EJBException(ce);
            }
        }
        else {
            logger.error("Invalid message type delivered.  Expected Map Message, received " + message.getClass().getName());
        }
    }

}
