package org.janelia.it.jacs.compute.launcher.indexing;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.ResourceAdapter;
import org.jboss.ejb3.StrictMaxPool;
import javax.inject.Inject;

/**
 * An MBD for handling reindexing requests asynchronously.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MessageDriven(name = "IndexingMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "/jms/queue/indexing"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "10"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000")
})
@PoolClass(value = StrictMaxPool.class, maxSize = 10, timeout = 10000)
@ResourceAdapter("hornetq-ra.rar")
public class IndexingMDB implements MessageListener {
	
	@Override
	public void onMessage(Message message) {
        try {
    		Long domainObject = message.getLongProperty("OBJECT_ID");
            String objectClass = message.getStringProperty("OBJECT_CLASS");
            String operation = message.getStringProperty("OPERATION");
                IndexingManager.scheduleIndexing(domainObject, objectClass, operation);
        }
        catch (Exception e) {
        	throw new EJBException(e);
        }
	}

}
