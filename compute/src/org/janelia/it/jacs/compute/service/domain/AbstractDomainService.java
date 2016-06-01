package org.janelia.it.jacs.compute.service.domain;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ContextLogger;
import org.janelia.it.jacs.compute.service.common.ProcessDataAccessor;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.domain.util.DomainHelper;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Subject;

/**
 * Base class for services dealing with entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractDomainService implements IService {

    protected Logger logger;
    protected ContextLogger contextLogger;
    protected Task task;
    protected IProcessData processData;
    protected ProcessDataAccessor data;
    protected ComputeBeanLocal computeBean;
    protected DomainHelper domainHelper;
    protected DomainDAL domainDao;
    protected String ownerKey;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.contextLogger = new ContextLogger(logger);
            this.task = ProcessDataHelper.getTask(processData);
            this.contextLogger.appendToLogContext(task);
            this.processData = processData;
            this.data = new ProcessDataAccessor(processData, contextLogger);
            this.computeBean = EJBFactory.getLocalComputeBean();
            this.domainHelper = new DomainHelper(computeBean, ownerKey, logger, contextLogger);
            this.domainDao = DomainDAL.getInstance();
            
            final String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            final Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();

            execute();
        }
        catch (ServiceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    protected abstract void execute() throws Exception;
}
