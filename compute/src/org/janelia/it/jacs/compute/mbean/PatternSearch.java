package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.mservice.*;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.compute.service.fly.ScreenSampleLineCoordinationService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.User;

import java.util.*;
import java.util.concurrent.Callable;

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

    protected FileDiscoveryHelper getFileDiscoveryHelper() {
        ComputeBeanLocal computeBean=getComputeBean();
        EntityBeanLocal entityBean=getEntityBean();
        return new FileDiscoveryHelper(entityBean, computeBean, "system");
    }

    /////////////// Management Methods ////////////////////////////////////////////////////////////////////////////////

    public void changePatternAnnotationFolderName() {
        logger.info("changePatternAnnotationFolderName() start");
        try {
            // Get top-level folder
            FileDiscoveryHelper helper=getFileDiscoveryHelper();
            Entity topLevelSampleFolder = helper.createOrVerifyRootEntity(ScreenSampleLineCoordinationService.SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME,
                    false /* create if necessary */, false /* load tree */);
            if (topLevelSampleFolder == null) {
                throw new Exception("Top level folder with name=" + ScreenSampleLineCoordinationService.SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME + " is null");
            }

            // Create MService for Samples
            MService sampleMService=new MService("system", 10);

            List<EntitySearchTrigger> triggerList=new ArrayList<EntitySearchTrigger>();

            EntityTypeTrigger sampleTrigger=new EntityTypeTrigger(EntityConstants.TYPE_SCREEN_SAMPLE);
            sampleTrigger.setAlwaysContinue(true);

            triggerList.add(sampleTrigger);
            triggerList.add(new EntityTypeNameTrigger(EntityConstants.TYPE_FOLDER, "Pattern Annotation"));


            List<EntityAction> actionList=new ArrayList<EntityAction>();
            actionList.add(new LogEntityNameAction("PatternFolderSearch"));

            sampleMService.run(topLevelSampleFolder, triggerList, actionList);

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }





}
