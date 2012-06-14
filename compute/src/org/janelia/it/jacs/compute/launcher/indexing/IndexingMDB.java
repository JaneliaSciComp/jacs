package org.janelia.it.jacs.compute.launcher.indexing;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.annotation.ejb.Depends;
import org.jboss.annotation.ejb.PoolClass;

/**
 * An MBD for handling reindexing requests asynchronously.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MessageDriven(name = "IndexingMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/indexing"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "10"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 10, timeout = 10000)
@Depends ({"jboss:custom=IndexingManager"})
public class IndexingMDB implements MessageListener {
	
	@Depends({"jboss:custom=IndexingManager"})
	private IndexingManagerManagement indexingManager;
	
	@Override
	public void onMessage(Message message) {
        try {
    		Long entityId = message.getLongProperty("ENTITY_ID");
    		indexingManager.scheduleIndexing(entityId);
        }
        catch (Exception e) {
        	throw new EJBException(e);
        }
	}

}
