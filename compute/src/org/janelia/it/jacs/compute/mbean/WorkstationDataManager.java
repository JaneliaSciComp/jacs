package org.janelia.it.jacs.compute.mbean;

import java.util.ArrayList;
import java.util.HashSet;

import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MCFOUnifiedFileDiscoveryTask;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:37 AM
 */
public class WorkstationDataManager implements WorkstationDataManagerMBean {

	
    public WorkstationDataManager() {
    }

    public void runMultiColorFlipOutFileDiscoveryService(String user, boolean refresh) {
        try {
        	String topLevelFolderName = "FlyLight Single Neuron Pilot Data";
        	String inputDirList = "/groups/flylight/flylight/SingleNeuronPilotData";
    		String linkingDirName = "/groups/scicomp/jacsData/flylight/"+user+"/SingleNeuronPilotData";
    		MCFOUnifiedFileDiscoveryTask task = new MCFOUnifiedFileDiscoveryTask(new HashSet<Node>(), 
            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), inputDirList, topLevelFolderName, 
            		linkingDirName, refresh);
            task.setJobName("MultiColor FlipOut File Discovery Task");
            task = (MCFOUnifiedFileDiscoveryTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MCFOUnifiedFileDiscovery", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runMCFOStitchedFileDiscoveryService(String user, boolean refresh) {
        try {
        	String topLevelFolderName = "FlyLight Single Neuron Stitched Data";
        	String inputDirList = "/groups/flylight/flylight/flip/SecData/stitch";
    		String linkingDirName = "/groups/scicomp/jacsData/flylight/"+user+"/SingleNeuronStitchedData";
    		MCFOUnifiedFileDiscoveryTask task = new MCFOUnifiedFileDiscoveryTask(new HashSet<Node>(), 
            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), inputDirList, topLevelFolderName, 
            		linkingDirName, refresh);
            task.setJobName("MultiColor FlipOut Unified File Discovery Task");
            task = (MCFOUnifiedFileDiscoveryTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MCFOUnifiedFileDiscovery", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void setupEntityTypes() {
        try {
            AnnotationBeanRemote annotationBean = EJBFactory.getRemoteAnnotationBean();
            annotationBean.setupEntityTypes();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void deleteEntityById(String entityId) {
     try {
         Long id=new Long(entityId);
         AnnotationBeanRemote annotationBean=EJBFactory.getRemoteAnnotationBean();
         annotationBean.deleteEntityById(id);
     } catch (Exception ex) {
         ex.printStackTrace();
     }
    }
}
