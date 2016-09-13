package org.janelia.it.jacs.compute.metrics_logging.mdb;

import org.apache.log4j.Logger;



import javax.ejb.MessageDriven;
import javax.ejb.ActivationConfigProperty;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.Date;
import javax.annotation.Resource;
import javax.ejb.MessageDrivenContext;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import javax.jms.ObjectMessage;
import org.janelia.it.jacs.compute.access.ComputeDAO;
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
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "10"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 1, timeout = 10000)
@TransactionAttribute(REQUIRED)
public class MetricsLoggingSingletonMDB implements MessageListener {
    
    private static final String DROP_MSG_WARNING = "  Dropping this low-priority message.";
    private Logger logger = Logger.getLogger( MetricsLoggingSingletonMDB.class );
    
    @Resource private MessageDrivenContext context;

    @Override
    public void onMessage(Message message) {
        Long sessionId;
        String userLogin;
        String toolName;
        String category;
        String action;
        Date timestamp;
        
        UserToolEvent event = null;
        UserToolEvent[] events = null;
        
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
                
                event = new UserToolEvent(sessionId, userLogin, toolName, category, action, timestamp);
            } catch ( JMSException jmse ) {
                logger.error("Failed to obtain/use parameters for logging." + DROP_MSG_WARNING);
                jmse.printStackTrace();
            }
         
        }
        else if (message instanceof ObjectMessage) {
            ObjectMessage objMessage = (ObjectMessage)message;
            try {
                Object messageObject = objMessage.getObject();
                if (messageObject instanceof UserToolEvent) {
                    event = (UserToolEvent) messageObject;
                }
                else if (messageObject instanceof UserToolEvent[]) {
                    events = (UserToolEvent[]) messageObject;
                }
                else {
                    final String errorMessage = "Unexpected object type.  User Tool Event expected." + DROP_MSG_WARNING;
                    logger.error(errorMessage);
                }
            } catch ( JMSException jmse ) {
                logger.error("Failed to obtain/use user tool event for logging." + DROP_MSG_WARNING);
                jmse.printStackTrace();
            }
        }
        else {
            logger.error("Invalid message type delivered.  Expected Map or Object Message, received " + message.getClass().getName() + DROP_MSG_WARNING);
        }
        
        try {
            if (event != null) {
                // Pump this event, using the compute infrastructure.
                new ComputeDAO(logger).addEventToSession(event);
            } else if (events != null) {
                ComputeDAO dao = new ComputeDAO(logger);
                for (UserToolEvent unbatchedEvent: events) {
                    dao.addEventToSession(unbatchedEvent);
                }
            }
        } catch (Throwable ce) {
            // This block will catch runtime exceptions.
            // Runtime exceptions will cause this rollback.
            // Unknown at this time, whether this can cause
            // stale context resource to accumulate in event
            // of RTE from called method.
            logger.error("Failed to log the user tool event." + DROP_MSG_WARNING);
            ce.printStackTrace();
            context.setRollbackOnly();
        }
    }

}
