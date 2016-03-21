package org.janelia.it.jacs.compute.mbean;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.domain.FileDiscoveryHelperNG;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.annotation.CompartmentAnnotation3DTask;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/8/12
 * Time: 12:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class PatternSearch implements PatternSearchMBean {

    private static final Logger logger = Logger.getLogger(PatternSearch.class);

    //////// Utilities ////////////////////////////////////////////////////////////////////////////////////////////////

    protected EntityBeanLocal getEntityBean() {
        return EJBFactory.getLocalEntityBean();
    }

    protected ComputeBeanLocal getComputeBean() {
        return EJBFactory.getLocalComputeBean();
    }

    protected FileDiscoveryHelperNG getFileDiscoveryHelper() {
        ComputeBeanLocal computeBean=getComputeBean();
        EntityBeanLocal entityBean=getEntityBean();
        return new FileDiscoveryHelperNG(computeBean, "system", logger);
    }

    /////////////// Management Methods ////////////////////////////////////////////////////////////////////////////////

    public void runCompartmentAnnotation3DService(String user, String configurationName, String sampleIdListPath,
                                                  String patternChannelString) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            int patternChannel=new Integer(patternChannelString.trim());
            CompartmentAnnotation3DTask task = new CompartmentAnnotation3DTask(
                    new HashSet<Node>(),
                    user,
                    new ArrayList<Event>(),
                    taskParameters,
                    configurationName,
                    sampleIdListPath,
                    patternChannel);
            task = (CompartmentAnnotation3DTask)EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("CompartmentAnnotation3D", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
