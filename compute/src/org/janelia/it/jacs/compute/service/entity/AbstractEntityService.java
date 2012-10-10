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
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Base class for services dealing with entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractEntityService implements IService {

    protected Logger logger;
    protected IProcessData processData;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;
    protected User user;
	protected EntityHelper entityHelper;
	
	public void execute(IProcessData processData) throws ServiceException {
        try {

	        this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
	        this.processData = processData;
	        this.entityBean = EJBFactory.getLocalEntityBean();
	        this.computeBean = EJBFactory.getLocalComputeBean();
	        this.annotationBean = EJBFactory.getLocalAnnotationBean();
	        this.user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
	        this.entityHelper = new EntityHelper(entityBean, computeBean, user);
	        
	        execute();
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
	
	protected abstract void execute() throws Exception;

    protected Entity populateChildren(Entity entity) {
    	if (entity==null || EntityUtils.areLoaded(entity.getEntityData())) return entity;
		EntityUtils.replaceChildNodes(entity, entityBean.getChildEntities(entity.getId()));
		return entity;
    }
}
