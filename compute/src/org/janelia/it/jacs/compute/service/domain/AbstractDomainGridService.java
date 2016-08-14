package org.janelia.it.jacs.compute.service.domain;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataAccessor;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Subject;

/**
 * Base class for services dealing with entities which have to run most of their processing on the grid.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractDomainGridService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    
    protected Logger logger;
    protected Task task;
    protected IProcessData processData;
    protected ProcessDataAccessor data;
    protected ComputeBeanLocal computeBean;
    protected DomainDAL domainDao;
    protected String ownerKey;

    @Override
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
        try {
            this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);
            this.processData = processData;
            this.data = new ProcessDataAccessor(processData, contextLogger);
            this.computeBean = EJBFactory.getLocalComputeBean();
            this.domainDao = DomainDAL.getInstance();
            
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            
            init();
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    protected abstract void init() throws Exception;
    
	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
}
