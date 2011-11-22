package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 11/21/11
 * Time: 11:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlyLightScreenDiscoveryService extends FileDiscoveryService {

    protected void processFolderForData(Entity folder) throws Exception {

        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing folder="+dir.getAbsolutePath()+" id="+folder.getId());

        if (!dir.canRead()) {
        	logger.info("Cannot read from folder "+dir.getAbsolutePath());
        	return;
        }

        processFlyLightScreenFolder(folder);
    }

    protected void processFlyLightScreenFolder(Entity folder) throws Exception {

        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));

        List<File> fileList=getOrderedFilesInDir(dir);

        for (File file : fileList) {
            if (!file.isDirectory()) {
                if (file.getName().endsWith(".reg.local.raw")) {
                    Entity alignedStack = createAlignedStackFromFile(file);
                    addToParent(folder, alignedStack, null, EntityConstants.ATTRIBUTE_ENTITY);
                }
            }
        }

        processChildFolders(folder);

    }

    private Entity createAlignedStackFromFile(File file) throws Exception {
        Entity alignedStack = new Entity();
        alignedStack.setUser(user);
        alignedStack.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_ALIGNED_BRAIN_STACK));
        alignedStack.setCreationDate(createDate);
        alignedStack.setUpdatedDate(createDate);
        alignedStack.setName(file.getName());
        alignedStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        alignedStack = annotationBean.saveOrUpdateEntity(alignedStack);
        logger.info("Saved ALIGNED_BRAIN_STACK stack as "+alignedStack.getId());
        return alignedStack;
    }

}
