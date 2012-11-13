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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 10/2/12
 * Time: 1:27 PM
 * To change this template use File | Settings | File Templates.
 */

/*
    The MService provides a means to:

    (1) visit a subtree of entities, and for each entity in the tree, decide whether the condition for the 'trigger' action occurs.
    (2) optionally, use a concurrent thread pool to asynchronously handle the trigger actions.

 */
public class MService {

    Logger logger = Logger.getLogger(MService.class);

    protected FileDiscoveryHelper helper;
    protected User user;
    protected int maxThreads;
    ExecutorService executorService;

    // If maxThreads==0, this means don't use threads - run single-threaded
    public MService(String username, int maxThreads) throws Exception {
        user=getComputeBean().getUserByName(username);
        this.maxThreads=maxThreads;
        if (maxThreads>0) {
            executorService=Executors.newFixedThreadPool(maxThreads);
        } else {
            executorService=null;
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
        return new FileDiscoveryHelper(entityBean, computeBean, user);
    }

    public void run(Entity startingEntity, EntitySearchTrigger trigger, EntityAction action) throws Exception {
        searchEntityContents(startingEntity, null, trigger, action);
    }

    protected Entity getTopLevelFolder(String topLevelFolderName, boolean createIfNecessary) throws Exception {
        FileDiscoveryHelper helper=getFileDiscoveryHelper();
        return helper.createOrVerifyRootEntity(topLevelFolderName, createIfNecessary, false /* load tree */);
    }

    private void searchEntityContents(Entity parent, Map<Entity, Integer> levelMap,
                                      EntitySearchTrigger trigger, EntityAction action) throws Exception {
        if (levelMap==null) {
            levelMap=new HashMap<Entity, Integer>();
        }
        Integer parentLevel=levelMap.get(parent);
        if (parentLevel==null) {
            parentLevel=0;
            levelMap.put(parent, parentLevel);
        }
        logger.info("level="+parentLevel+" : name="+parent.getName()+" type="+parent.getEntityType().getName());
        parent=getEntityBean().getEntityAndChildren(parent.getId());
        Set<Entity> children=parent.getChildren();
        for (Entity child : children) {
            Integer childLevel=parentLevel+1;
            levelMap.put(child, childLevel);
            EntitySearchTrigger.TriggerResponse response=trigger.evaluate(parent, child, childLevel);
            if (response.performAction) {
                if (executorService!=null) {
                    logger.info("Submitting action thread to executorService");
                    executorService.submit(action.getRunnable(parent, child));
                } else {
                    logger.info("Running action within current thread");
                    action.getRunnable(parent, child).run();
                }
            }
            if (response.continueSearch) {
                searchEntityContents(child, levelMap, trigger, action);
            }
        }
    }

}
