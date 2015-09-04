package org.janelia.it.jacs.compute.wsrest.computeresources;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.engine.data.ProcessData;
import org.janelia.it.jacs.model.tasks.SessionTask;
import org.janelia.it.jacs.model.tasks.Task;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;


/**
 * Created by goinac on 9/2/15.
 */
abstract public class AbstractAsyncComputationResource {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractAsyncComputationResource.class);

    private final String appId;
    private final SubmitDrmaaJobService processingService;

    @Context
    private SecurityContext securityContext;
    @PersistenceContext
    private EntityManager entityManager;

    private SessionFactory sessionFactory;

    public AbstractAsyncComputationResource(String appId, SubmitDrmaaJobService processingService) {
        this.appId = appId;
        this.processingService = processingService;
    }

    protected Task init(ProcessData processData) throws ProcessingException {
        if (processData == null) {
            throw new ProcessingException(HttpServletResponse.SC_BAD_REQUEST);
        }
        Session session = null;
        try {
            session = openSession();
            System.out.println("!!!!!! EM " + entityManager);
            Task task = new SessionTask(
                    null, /*inputNodes*/
                    "goinac",
                    null /* task events */,
                    null /* task parameters*/);
            task.setJobName(appId);
            session.persist(task);
            processData.putItem(ProcessData.PROCESS_ID, task.getObjectId());
            processData.putItem(ProcessData.TASK, task);
            processingService.init(processData);
            return task;
        } catch (Exception e) {
            LOG.warn("Error initializing the processor", e);
            throw new ProcessingException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        } finally {
            session.close();
        }
    }

    protected SessionFactory getSessionFactory() {
        try {
            if (sessionFactory==null) {
                InitialContext namingContext = new InitialContext();

                sessionFactory = (SessionFactory) namingContext.lookup("java:/hibernate/ComputeSessionFactory");
            }
            return sessionFactory;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected Session openSession() {
        SessionFactory sessionFactory = getSessionFactory();
        return sessionFactory.openSession();
    }

    private String getSubjectKey() {
        if (securityContext==null || securityContext.getUserPrincipal() == null)
            throw new UnauthorizedException("User has no security context");
        return securityContext.getUserPrincipal().getName();
    }

}
