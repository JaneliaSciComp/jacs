package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MultiColorFlipOutFileDiscoveryTask;
import org.janelia.it.jacs.model.user_data.Node;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:37 AM
 */
public class WorkstationDataManager implements WorkstationDataManagerMBean {

    private static final Logger logger = Logger.getLogger(WorkstationDataManager.class);

    public WorkstationDataManager() {
    }

    public void runMultiColorFlipOutFileDiscoveryService(String user, boolean runSeperationOnGrid) {
        try {
            MultiColorFlipOutFileDiscoveryTask task = new MultiColorFlipOutFileDiscoveryTask(new HashSet<Node>(), user, new ArrayList<Event>(), new HashSet<TaskParameter>(), runSeperationOnGrid);
            task.setJobName("MultiColor FlipOut File Discovery Task");
            task = (MultiColorFlipOutFileDiscoveryTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MultiColorFlipOutFileDiscovery", task.getObjectId());
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
