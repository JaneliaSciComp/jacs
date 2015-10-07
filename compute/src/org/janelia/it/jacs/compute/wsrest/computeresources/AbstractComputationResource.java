package org.janelia.it.jacs.compute.wsrest.computeresources;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.util.HibernateSessionUtils;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by goinac on 9/2/15.
 */
abstract public class AbstractComputationResource<T extends Task, R extends FileNode> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractComputationResource.class);

    private final String resourceName;

    protected AbstractComputationResource(String resourceName) {
        this.resourceName = resourceName;
    }

    protected T init(T task) throws ProcessingException {
        if (task == null) {
            throw new ProcessingException(HttpServletResponse.SC_BAD_REQUEST);
        }
        prepareTask(task);
        return persistObject(task);
    }

    private <P> P persistObject(P persistentObject) throws ProcessingException {
        Session session = null;
        try {
            session = HibernateSessionUtils.getSession();
            Transaction tx = session.beginTransaction();
            session.persist(persistentObject);
            tx.commit();
            return persistentObject;
        } catch (Exception e) {
            LOG.warn("Error persisting {}", persistentObject, e);
            throw new ProcessingException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        } finally {
            HibernateSessionUtils.closeSession(session);
        }
    }

    protected void prepareTask(T task) {
        if (task.getJobName() == null || task.getJobName().length() == 0) {
            task.setJobName(resourceName);
        }
    }

    abstract protected R createResultNode(T task, String visibility);

    private String getVisibility(T task) {
        String visibility = Node.VISIBILITY_PRIVATE;

        if (User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner())) {
            visibility = Node.VISIBILITY_PUBLIC;
        }

        return visibility;
    }

    protected void submitJob(T task) throws ProcessingException {
        Map<String, Object> jobConfig = prepareProcessConfiguration(task);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.submitJob(resourceName, jobConfig);
        } catch (Exception e) {
            LOG.error("Error while submitting the job", e);
            throw new ProcessingException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    protected Map<String, Object> prepareProcessConfiguration(T task) throws ProcessingException {
        try {
            R resultNode = createResultNode(task, getVisibility(task));
            resultNode = persistObject(resultNode);
            task.addOutputNode(resultNode);
            FileUtil.ensureDirExists(resultNode.getDirectoryPath());
            FileUtil.cleanDirectory(resultNode.getDirectoryPath());
            Map<String, Object> processConfiguration = new HashMap<>();
            processConfiguration.put(ProcessDataConstants.PROCESS_ID, task.getObjectId());
            processConfiguration.put(ProcessDataConstants.RESULT_FILE_NODE_ID, resultNode.getObjectId());
            processConfiguration.put(ProcessDataConstants.RESULT_FILE_NODE_ID, resultNode.getObjectId());
            processConfiguration.put(ProcessDataConstants.RESULT_FILE_NODE_DIR, resultNode.getDirectoryPath());
            return processConfiguration;
        } catch (Exception e) {
            LOG.error("Error while creating the result node", e);
            throw new ProcessingException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

}
