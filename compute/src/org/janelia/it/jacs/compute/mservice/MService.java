package org.janelia.it.jacs.compute.mservice;

import org.apache.http.impl.entity.EntitySerializer;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.user_data.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 10/2/12
 * Time: 1:27 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class MService {

    public interface EntitySearchTrigger {
        public boolean evaluate(Entity parent, Entity entity, int level);
    }

    Logger logger = Logger.getLogger(MService.class);

    private FileDiscoveryHelper helper;
    private User user;

    public MService(String username) throws Exception {
        user=getComputeBean().getUserByName(username);
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
        return new FileDiscoveryHelper(entityBean, computeBean, user);
    }

    public abstract void run() throws Exception;

    protected Entity getTopLevelFolder(String topLevelFolderName, boolean createIfNecessary) throws Exception {
        FileDiscoveryHelper helper=getFileDiscoveryHelper();
        return helper.createOrVerifyRootEntity(topLevelFolderName, createIfNecessary, false /* load tree */);
    }

    protected void searchEntityContents(Entity parent, EntitySearchTrigger searchTrigger) {
        searchEntityContents(parent, null, null, searchTrigger);
    }

    private void searchEntityContents(Entity parent, Map<Entity, Integer> levelMap, String[] spaceArray,
                                      EntitySearchTrigger searchTrigger) {
        if (levelMap==null) {
            levelMap=new HashMap<Entity, Integer>();
        }
        if (spaceArray==null) {
            spaceArray=new String[1000];
            StringBuilder builder=new StringBuilder();
            for (int i=0;i<1000;i++) {
                spaceArray[i]=builder.toString();
                builder.append(" ");
            }
        }
        Integer parentLevel=levelMap.get(parent);
        if (parentLevel==null) {
            parentLevel=0;
            logger.info("level=0 : name="+parent.getName()+" type="+parent.getEntityType().getName());
        }
        parent=getEntityBean().getEntityAndChildren(parent.getId());
        Set<Entity> children=parent.getChildren();
        for (Entity child : children) {
            Integer childLevel=parentLevel+1;
            levelMap.put(child, childLevel);
            if (searchTrigger.evaluate(parent, child, childLevel)) {
                searchEntityContents(child, levelMap, spaceArray, searchTrigger);
            }
        }
    }

}
