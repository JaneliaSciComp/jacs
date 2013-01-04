package org.janelia.it.jacs.compute.service.neo4j;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.SolrBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Update the Neo4j database.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Neo4jLoadService implements IService {

    public transient static final String PARAM_clearDb = "clear db";
	
    protected Logger logger;
    protected Task task;
    protected SolrBeanLocal solrBean;
    private boolean cleardb = false;
    
    public void execute(IProcessData processData) throws ServiceException {

    	try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            solrBean = EJBFactory.getLocalSolrBean();
            cleardb = Boolean.parseBoolean(task.getParameter(PARAM_clearDb));
            solrBean.neo4jAllEntities(cleardb);
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running Neo4jLoadService", e);
        }
    }    
}
