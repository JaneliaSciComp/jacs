package org.janelia.it.jacs.compute.wsrest.computeresources;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.engine.data.ProcessData;
import org.janelia.it.jacs.model.tasks.SessionTask;
import org.janelia.it.jacs.model.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.servlet.http.HttpServletResponse;


/**
 * Created by goinac on 9/2/15.
 */
abstract public class AbstractAsyncComputationResource {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractAsyncComputationResource.class);

    private final String appId;

    private SessionFactory sessionFactory;

    public AbstractAsyncComputationResource(String appId) {
        this.appId = appId;
    }

    protected Task init(Task task) throws ProcessingException {
        if (task == null) {
            throw new ProcessingException(HttpServletResponse.SC_BAD_REQUEST);
        }
        Session session = null;
        try {
            session = openSession();
            Transaction tx = session.beginTransaction();
            task.setJobName(appId);
            session.persist(task);
            tx.commit();
            return task;
        } catch (Exception e) {
            LOG.warn("Error initializing the processor", e);
            throw new ProcessingException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        } finally {
            try {
                if (session != null && session.isOpen()) session.close();
            } catch (Exception ignore) {
            }
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

}
