package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.mservice.*;
import org.janelia.it.jacs.compute.mservice.action.*;
import org.janelia.it.jacs.compute.mservice.trigger.EntityTrigger;
import org.janelia.it.jacs.compute.mservice.trigger.EntityTypeNameTrigger;
import org.janelia.it.jacs.compute.mservice.trigger.EntityTypeTrigger;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.compute.service.fly.ScreenSampleLineCoordinationService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.*;

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
        return new FileDiscoveryHelper(entityBean, computeBean, "system", logger);
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

            List<EntityTrigger> triggerList=new ArrayList<EntityTrigger>();
            triggerList.add(new EntityTypeTrigger(EntityConstants.TYPE_SCREEN_SAMPLE));

            EntityTypeNameTrigger paTrigger=new EntityTypeNameTrigger(EntityConstants.TYPE_FOLDER, "Pattern Annotation");
            paTrigger.setRecursive(false);
            EntityChangeNameAction changeNameAction=new EntityChangeNameAction("Pattern Annotation", "Compartments");
            paTrigger.addAction(changeNameAction);
            triggerList.add(paTrigger);

            sampleMService.run(topLevelSampleFolder, triggerList);

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        logger.info("changePatternAnnotationFolderName() end");
    }

    public void moveCompartmentsFolderUnderMaskFolder() {
        logger.info("moveCompartmentsFolderUnderMaskFolder() start");
        try {

            // Get top-level folder
            FileDiscoveryHelper helper=getFileDiscoveryHelper();
            Entity topLevelSampleFolder = helper.createOrVerifyRootEntity(ScreenSampleLineCoordinationService.SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME,
                    false /* create if necessary */, false /* load tree */);
            if (topLevelSampleFolder == null) {
                throw new Exception("Top level folder with name=" + ScreenSampleLineCoordinationService.SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME + " is null");
            }

            // Create MService for Samples
            MService sampleMService=new MService("system", 0);

            List<EntityTrigger> triggerList=new ArrayList<EntityTrigger>();

            EntityTypeTrigger screenTrigger=new EntityTypeTrigger(EntityConstants.TYPE_SCREEN_SAMPLE);

            SubcontextPushAction subcontextPushAction=new SubcontextPushAction(true /* reset */);
            screenTrigger.addAction(subcontextPushAction);

            AddChildEntityContextAction addMaskFolderToContextAction=new AddChildEntityContextAction("Mask Annotation", "MaskFolder");
            screenTrigger.addAction(addMaskFolderToContextAction);

            triggerList.add(screenTrigger);

            EntityTypeNameTrigger paTrigger=new EntityTypeNameTrigger(EntityConstants.TYPE_FOLDER, "Compartments");
            paTrigger.setRecursive(false);

            MoveToContextEntityAction moveEntityAction=new MoveToContextEntityAction("MaskFolder");
            moveEntityAction.addContextKeyToClearOnDone("MaskFolder");
            paTrigger.addAction(moveEntityAction);
            SubcontextPopAction subcontextPopAction=new SubcontextPopAction();
            paTrigger.addAction(subcontextPopAction);

            triggerList.add(paTrigger);

            sampleMService.run(topLevelSampleFolder, triggerList);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        logger.info("moveCompartmentsFolderUnderMaskFolder() end");
    }

    public void changeMaskFolderName() {
        logger.info("changeMaskFolderName() start");
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

            List<EntityTrigger> triggerList=new ArrayList<EntityTrigger>();
            triggerList.add(new EntityTypeTrigger(EntityConstants.TYPE_SCREEN_SAMPLE));

            EntityTypeNameTrigger paTrigger=new EntityTypeNameTrigger(EntityConstants.TYPE_FOLDER, "Mask Annotation");
            paTrigger.setRecursive(false);
            EntityChangeNameAction changeNameAction=new EntityChangeNameAction("Mask Annotation", "Pattern Annotation");
            paTrigger.addAction(changeNameAction);
            triggerList.add(paTrigger);

            sampleMService.run(topLevelSampleFolder, triggerList);

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        logger.info("changeMaskFolderName() end");
    }



}
