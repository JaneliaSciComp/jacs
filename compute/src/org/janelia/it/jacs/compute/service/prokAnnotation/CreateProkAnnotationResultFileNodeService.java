
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkAnnotationLocalDirectoryImportTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.prokAnnotation.ProkAnnotationResultFileNode;

import java.io.IOException;

/**
 * @author Todd Safford
 */
public class CreateProkAnnotationResultFileNodeService implements IService {

    private Task task;
    private String sessionName;

    public void execute(IProcessData processData) throws CreateRecruitmentFileNodeException {
        try {
            this.task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            Long resultNodeId = createResultFileNode();
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultNodeId);
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
    }

    private Long createResultFileNode() throws DaoException, IOException {
        // if we get this far then we assume that no result persist exists and create one
        ProkAnnotationResultFileNode resultFileNode;
        String visibility = Node.VISIBILITY_PRIVATE;
        if (User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner())) {
            visibility = Node.VISIBILITY_PUBLIC;
        }
        resultFileNode = new ProkAnnotationResultFileNode(task.getOwner(), task,
                task.getParameter(ProkAnnotationLocalDirectoryImportTask.QUERY),
                task.getParameter(ProkAnnotationLocalDirectoryImportTask.QUERY), visibility, sessionName);
        resultFileNode.setPathOverride(task.getParameter(ProkAnnotationLocalDirectoryImportTask.PATH_TO_SOURCE_DATA));
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultFileNode);
        return resultFileNode.getObjectId();
    }

}