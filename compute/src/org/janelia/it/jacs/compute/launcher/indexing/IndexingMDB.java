package org.janelia.it.jacs.compute.launcher.indexing;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.solr.SolrDAO;
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
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 1, timeout = 10000)
public class IndexingMDB implements MessageListener {

	private static Logger logger = Logger.getLogger(IndexingMDB.class);

    private final SolrDAO _solrDAO = new SolrDAO(logger, false);
    
	@Override
	public void onMessage(Message message) {
        try {
    		Long entityId = message.getLongProperty("ENTITY_ID");
            _solrDAO.updateIndex(_solrDAO.getEntityById(entityId));    		
        }
        catch (Exception e) {
        	throw new EJBException(e);
        }
	}

}
