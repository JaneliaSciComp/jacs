package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Subject;

/**
 * Base class for services dealing with entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractEntityService implements IService {

    protected Logger logger;
    protected Task task;
    protected IProcessData processData;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;
    protected String ownerKey;
	protected EntityHelper entityHelper;
	protected EntityBeanEntityLoader entityLoader;
	
	public void execute(IProcessData processData) throws ServiceException {
        try {

	        this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
	        this.task = ProcessDataHelper.getTask(processData);
	        this.processData = processData;
	        this.entityBean = EJBFactory.getLocalEntityBean();
	        this.computeBean = EJBFactory.getLocalComputeBean();
	        this.annotationBean = EJBFactory.getLocalAnnotationBean();
	        
	        String ownerName = ProcessDataHelper.getTask(processData).getOwner();
	        Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
	        this.ownerKey = subject.getKey();
	        
	        this.entityHelper = new EntityHelper(entityBean, computeBean, ownerKey, logger);
	        this.entityLoader = new EntityBeanEntityLoader(entityBean);
	        
	        execute();
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
	
	protected abstract void execute() throws Exception;

    protected Entity populateChildren(Entity entity) throws Exception {
		return entityLoader.populateChildren(entity);
    }
}
