package org.janelia.it.jacs.compute.launcher.indexing;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.DependsOn;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;




/**
 * An MBD for handling reindexing requests asynchronously.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MessageDriven(name = "IndexingMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
//        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/jms/queue/indexing"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "10"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
//        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 10, timeout = 10000)
@DependsOn({"IndexingManager"})
public class IndexingMDB implements MessageListener {
	
//	@DependsOn({"jboss:custom=IndexingManager"})
	private IndexingManagerManagement indexingManager;
	
	@Override
	public void onMessage(Message message) {
        try {
    		Long objectId = message.getLongProperty("OBJECT_ID");
			String operation = message.getStringProperty("OPERATION");
			if (operation==null || operation.equals("UPDATE")) {
				String objectClazz = message.getStringProperty("OBJECT_CLASS");
				indexingManager.scheduleIndexing(objectId, objectClazz);
			} else if (operation.equals("ANCESTOR")) {
				Long newAncestorId = message.getLongProperty("NEW_ANCESTOR_ID");
    			indexingManager.scheduleAddNewAncestor(objectId, (Long)newAncestorId);
    		} else if (operation.equals("REMOVE")) {
				indexingManager.scheduleRemoval(objectId);
			}
        }
        catch (Exception e) {
        	throw new EJBException(e);
        }
	}

}
