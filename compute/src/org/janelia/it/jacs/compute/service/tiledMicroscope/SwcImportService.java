package org.janelia.it.jacs.compute.service.tiledMicroscope;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.SwcImportNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;
import java.util.Date;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.TiledMicroscopeBeanLocal;
import org.janelia.it.jacs.model.tasks.Event;

/**
 * SWC Import proceeds from here.
 *
 * Created by fosterl on 6/17/14.
 */
@SuppressWarnings("unused")
public class SwcImportService extends AbstractEntityService {
    private Logger logger = Logger.getLogger(SwcImportService.class);

    public static final String FOLDER_NAME_PARAM = "FOLDER_NAME";
    public static final String USER_NAME_PARAM = "USER_NAME";
    public static final String SAMPLE_ID_PARAM = "SAMPLE_ID";
    public static final String WORKSPACE_NAME_PARAM = "WORKSPACE_NAME";

    private Long sampleId;
    private String userName;
    private String folderName;
    private String workspaceName;
    private FileNode importationNode;

    @Override
    protected void execute() throws Exception {
        try {
            sampleId = getGuidItem(SAMPLE_ID_PARAM);
            userName = (String) processData.getItem(USER_NAME_PARAM);
            folderName = (String) processData.getItem(FOLDER_NAME_PARAM);
            workspaceName = (String) processData.getItem(WORKSPACE_NAME_PARAM);

            Long nodeId = getOrCreateResultNode();

            // NOTE: the default value for any boolean through JMX (from JBoss, at least) is TRUE.
            //  Therefore, we want our most-common-case to match TRUE.  So TRUE-> do NOT issue debug info.
            logger.info(
                    "Running SWC Import. ownerKey=" + ownerKey
                    + ", startingId=" + this.sampleId
                    + ", userName=" + userName
                    + ", workspaceName=" + workspaceName
                    + ", folderName=" + folderName + "."
            );

            // Contact the EJB, and do the launch.
            computeBean.saveEvent(task.getObjectId(), Event.RUNNING_EVENT, "Running", new Date());

            TiledMicroscopeBeanLocal tmEJB = EJBFactory.getLocalTiledMicroscopeBean();
            tmEJB.importSWCFolder(folderName, ownerKey, sampleId, workspaceName);

            computeBean.saveEvent(task.getObjectId(), Event.COMPLETED_EVENT, "Completed", new Date());
        } catch (Exception ex) {
            computeBean.saveEvent(task.getObjectId(), Event.ERROR_EVENT, ex.getMessage(), new Date());
            throw ex;
        }
    }

    private Long getGuidItem( String itemName ) {
        Long rtnVal = null;
        // We'll accept a long if offered, but fall back to String.
        Object guidObj = processData.getItem( itemName );
        if ( guidObj == null ) {
            rtnVal = null;
        }
        else if ( guidObj instanceof Long ) {
            Long guid = (Long) processData.getItem( itemName );
            if ( guid == null  ||  guid <= 0 ) {
                rtnVal = null;
            }
        }
        else {
            rtnVal = Long.parseLong( guidObj.toString() );
        }
        return rtnVal;
    }

    /**
     * Create a node to act as identity for this run.
     *
     * @return ID of the run node.
     * @throws DaoException thrown by called methods.
     * @throws IOException thrown by called methods.
     */
    private Long getOrCreateResultNode() throws DaoException, IOException {
        Long parentTaskId = task.getParentTaskId();
        if ( parentTaskId == null ) {
            // Here, need establish the file node to hold the directory location.
            importationNode = new SwcImportNode(
                    User.SYSTEM_USER_LOGIN,
                    task,
                    folderName,                           // name
                    "Import from: " + folderName,      // description
                    Node.VISIBILITY_PUBLIC,          // visibility
                    Node.DIRECTORY_DATA_TYPE,        // data type
                    null                             // relative session path
            );
            computeBean.saveOrUpdateNode(importationNode);
            FileUtil.ensureDirExists(importationNode.getDirectoryPath());

        }
        else {
            importationNode = (SwcImportNode)computeBean.getResultNodeByTaskId( parentTaskId );
        }

        return importationNode.getObjectId();
    }

}
