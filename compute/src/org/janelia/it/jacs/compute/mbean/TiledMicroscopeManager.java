package org.janelia.it.jacs.compute.mbean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.tiledMicroscope.TmNeuronPBUpdateTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:11 PM
 * To change this template use File | Settings | File Templates.
 */
@Singleton
@Startup
@Remote(TiledMicroscopeManagerMBean.class)
public class TiledMicroscopeManager extends AbstractComponentMBean implements TiledMicroscopeManagerMBean {

    public TiledMicroscopeManager() {
        super("jacs");
    }

    public void createTiledMicroscopeEntityTypes() {
        try {
            EJBFactory.getRemoteTiledMicroscopeBean().createTiledMicroscopeEntityTypes();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void convertAllTmWorkspaceToProtobuf() {
        try {
            Logger logger = Logger.getLogger(TiledMicroscopeManager.class);
            EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
            if (entityBean == null) {
                throw new RuntimeException("Failed to get Entity EJB");
            }
            List<Entity> workspaces = entityBean.getEntitiesByTypeName(null, EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
            
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            if (computeBean == null) {
                throw new RuntimeException("Failed to get Compute EJB");
            }

            for (Entity workspace: workspaces) {
                String workspaceIdStr = workspace.getId().toString();
                HashSet<TaskParameter> taskParameters = new HashSet<>();
                taskParameters.add(new TaskParameter(TmNeuronPBUpdateTask.PARAM_workspaceId, workspaceIdStr, null));
                Task task = new GenericTask(new HashSet<Node>(), "group:mouselight", new ArrayList<Event>(),
                        taskParameters, TmNeuronPBUpdateTask.PROCESS_NAME, TmNeuronPBUpdateTask.PROCESS_NAME);
                task = computeBean.saveOrUpdateTask(task);
                computeBean.submitJob(task.getJobName(), task.getObjectId());
                logger.info("Submitted " + task.getJobName() + " for workspace " + workspace.getId());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void test() {
        System.out.println("TiledMicroscopeManager test() start");

        System.out.println("Part I : Create new Tiled Microscope Workspace");



        System.out.println("TiledMicroscopeManager test() end");
    }

}
