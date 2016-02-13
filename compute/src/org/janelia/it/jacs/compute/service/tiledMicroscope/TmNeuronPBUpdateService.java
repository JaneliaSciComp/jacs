package org.janelia.it.jacs.compute.service.tiledMicroscope;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.api.TiledMicroscopeBeanLocal;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuronPBUpdateNode;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;

/**
 * Launch this to update Tiled Microscope Neurons to ProtoBuf serialized objects.
 *
 * Created by fosterl on 2/12/16.
 */
@SuppressWarnings("unused")
public class TmNeuronPBUpdateService extends AbstractEntityService {
    private Logger logger = Logger.getLogger(TmNeuronPBUpdateService.class);

    public static final String WORKSPACE_ID_PARAM = "WORKSPACE_ID";

    private Long workspaceId;
    private FileNode importationNode;

    @Override
    protected void execute() throws Exception {
        try {
            workspaceId = getGuidItem(WORKSPACE_ID_PARAM);

            Long nodeId = getOrCreateResultNode();

            // NOTE: the default value for any boolean through JMX (from JBoss, at least) is TRUE.
            //  Therefore, we want our most-common-case to match TRUE.  So TRUE-> do NOT issue debug info.
            logger.info(
                    "Running Protobuf Neuron Update. ownerKey=" + ownerKey
                    + ", startingId=" + this.workspaceId + "."
            );

            // Contact the EJB, and do the launch.
            computeBean.saveEvent(task.getObjectId(), Event.RUNNING_EVENT, "Running", new Date());

            TiledMicroscopeBeanLocal tmEJB = EJBFactory.getLocalTiledMicroscopeBean();
			//TODO: make an alternative EJB method that accepts the task ID from the task object known here.
			TmWorkspace workspace = tmEJB.loadWorkspace(workspaceId);
			// Validate converted workspace.
			TmWorkspace.Version wsVersion = workspace.getVersion();
			if (wsVersion != TmWorkspace.Version.PB_1) {
				logger.error("Conversion failed.  Workspace " + workspaceId + " still at " + wsVersion);
			}
			else {
				// Check neurons counts between old and new.
				List<TmNeuron> neurons = workspace.getNeuronList();
				EntityBeanLocal entityEJB = EJBFactory.getLocalEntityBean();
				Entity workspaceEntity = entityEJB.getEntityById(workspaceId);
				entityEJB.loadLazyEntity(workspaceEntity, false);
				Set<Entity> children = workspaceEntity.getChildren();
				int expectedNeuronCount = 0;
				for (Entity child: children) {
					if (child.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
						expectedNeuronCount ++;
					}
				}
				if (expectedNeuronCount != neurons.size()) {
					logger.error("Conversion failed.  Workspace " + workspaceId 
					    + " had " + expectedNeuronCount 
						+ " entity neurons.  It now has " + neurons.size() 
						+ " converted, entity-data neurons.");
				}
			}

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
            importationNode = new TmNeuronPBUpdateNode(
                    User.SYSTEM_USER_LOGIN,
                    task,
					"" + workspaceId,
                    "Update Neurons to Protbuf Serialized, in: " + workspaceId,   // description
                    Node.VISIBILITY_PUBLIC,          // visibility
                    Node.DIRECTORY_DATA_TYPE,        // data type
                    null                             // relative session path
            );
            computeBean.saveOrUpdateNode(importationNode);
            FileUtil.ensureDirExists(importationNode.getDirectoryPath());

        }
        else {
            importationNode = (TmNeuronPBUpdateNode)computeBean.getResultNodeByTaskId( parentTaskId );
        }

        return importationNode.getObjectId();
    }

}
