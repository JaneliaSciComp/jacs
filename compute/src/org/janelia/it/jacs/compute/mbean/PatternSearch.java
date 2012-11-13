package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.mservice.EntityAction;
import org.janelia.it.jacs.compute.mservice.EntitySearchTrigger;
import org.janelia.it.jacs.compute.mservice.MService;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.compute.service.fly.ScreenSampleLineCoordinationService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.User;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/8/12
 * Time: 12:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class PatternSearch implements PatternSearchMBean {

    private static final Logger logger = Logger.getLogger(PatternSearch.class);

    public class SampleSearchTrigger extends EntitySearchTrigger {

        public TriggerResponse evaluate(Entity parent, Entity entity, int level) {
            TriggerResponse response=new TriggerResponse();
            if (entity.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
                response.continueSearch=false;
                response.performAction=true;
            }
            else {
                response.continueSearch=true;
                response.performAction=false;
            }
            return response;
        }

    }

    public class PatternFolderTrigger extends EntitySearchTrigger {

        public TriggerResponse evaluate(Entity parent, Entity entity, int level) {
            TriggerResponse response=new TriggerResponse();
            if (entity.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER) &&
            entity.getName().equals("Pattern Annotation")) {
                response.continueSearch=false;
                response.performAction=true;
            }
            else {
                response.continueSearch=true;
                response.performAction=false;
            }
            return response;
        }

    }

    public class FoundSampleEntityAction extends EntityAction {

        public Runnable getRunnable(final Entity parentEntity, final Entity entity) throws Exception {
            return new Thread() {
                public void run() {
                logger.info("Found sample name="+entity.getName());}
            };
        }

    }

    public class FoundPatternAnnotationFolderEntityAction extends EntityAction {

        public Runnable getRunnable(final Entity parentEntity, final Entity entity) throws Exception {
            return new Thread() {
                public void run() {
                    logger.info("Found pattern annotation folder name="+entity.getName() + " id="+entity.getId());}
            };
        }

    }

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
            sampleMService.run(topLevelSampleFolder, new SampleSearchTrigger(), new FoundSampleEntityAction());

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

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



}
