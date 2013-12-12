package org.janelia.it.jacs.compute.mservice;

import com.google.common.util.concurrent.*;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.mservice.action.CalledAction;
import org.janelia.it.jacs.compute.mservice.action.EntityAction;
import org.janelia.it.jacs.compute.mservice.trigger.EntityTrigger;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.User;

import java.util.*;
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
    protected String ownerKey;
    protected int maxThreads;
    ListeningExecutorService listeningExecutorService;
    List<ListenableFuture<Object>> futureList=new ArrayList<ListenableFuture<Object>>();
    List<EntityTrigger> triggerList=new ArrayList<EntityTrigger>();
    Map<Entity, Integer> levelMap=new HashMap<Entity, Integer>();
    Map<String, Object> actionContext=Collections.synchronizedMap(new HashMap<String, Object>());

    // If maxThreads==0, this means don't use threads - run single-threaded
    public MService(String username, int maxThreads) throws Exception {
    	ownerKey = "user:"+username;
        this.maxThreads=maxThreads;
        if (maxThreads>0) {
            listeningExecutorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(maxThreads));
        } else {
            listeningExecutorService=null;
        }
    }

    /////////// Utilities ////////////////////////////////////////////////////////////////////////////////////////////

    protected EntityBeanLocal getEntityBean() {
        return EJBFactory.getLocalEntityBean();
    }

    protected ComputeBeanLocal getComputeBean() {
        return EJBFactory.getLocalComputeBean();
    }

    protected Entity getTopLevelFolder(String topLevelFolderName, boolean createIfNecessary) throws Exception {
        FileDiscoveryHelper helper=getFileDiscoveryHelper();
        return helper.createOrVerifyRootEntity(topLevelFolderName, createIfNecessary, false /* load tree */);
    }

    protected FileDiscoveryHelper getFileDiscoveryHelper() {
        ComputeBeanLocal computeBean=getComputeBean();
        EntityBeanLocal entityBean=getEntityBean();
        return new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
    }

    /////////// run method variations ///////////////////////////////////////////////////////////////////////////////

    public void run(Entity startingEntity, EntityTrigger trigger) throws Exception {
        triggerList.add(trigger);
        searchEntityContents(startingEntity);
    }

    public void run(Entity startingEntity, List<EntityTrigger> triggerList) throws Exception {
        this.triggerList.addAll(triggerList);
        searchEntityContents(startingEntity);
    }

    //////////////// searchEntityContents - the main search method ///////////////////////////////////////////////////

    private void searchEntityContents(Entity parent) throws Exception {
        searchEntityContents(parent, 0 /* trigger level */);
    }

    private void searchEntityContents(Entity parent, int triggerLevel) throws Exception {
        Integer parentLevel = levelMap.get(parent);
        if (parentLevel == null) {
            parentLevel = 0;
            levelMap.put(parent, parentLevel);
        }

        EntityTrigger trigger = triggerList.get(triggerLevel);
        parent = getEntityBean().getEntityAndChildren(parent.getId());
        Set<Entity> children = parent.getChildren();
        logger.info("level=" + parentLevel + " : name=" + parent.getName() + " type=" + parent.getEntityTypeName() + " triggerLevel=" + triggerLevel+" children="+children.size());
        long childIndex=0;
        for (Entity child : children) {
            logger.info("Child "+childIndex + " name="+child.getName()+" id="+child.getId());
            childIndex++;
            Integer childLevel = parentLevel + 1;
            levelMap.put(child, childLevel);
            EntityTrigger.TriggerResponse response = trigger.evaluate(parent, child, childLevel);
            if (response.performAction) {
                for (final EntityAction action : trigger.getActionList()) {
                    if (listeningExecutorService == null || action.isBlocking()) {
                        logger.info("Running action within current thread for id=" + child.getId());
                        try {
                            CalledAction result = action.getCallable(parent, child, actionContext).call();
                            clearActionKeys(result.getContextKey(), action);
                        }
                        catch (Exception ex) {
                            action.handleFailure(ex);
                        }
                    } else {
                        logger.info("Submitting action thread to executorService for id=" + child.getId());
                        ListenableFuture<Object> callback = listeningExecutorService.submit(action.getCallable(parent, child, actionContext));
                        Futures.addCallback(callback, new FutureCallback<Object>() {
                            @Override
                            public void onSuccess(Object o) {
                                CalledAction result=(CalledAction)o;
                                clearActionKeys(result.getContextKey(), action);
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                try {
                                    action.handleFailure(throwable);
                                } catch (Exception ex) {
                                    logger.error(ex);
                                    ex.printStackTrace();
                                }
                            }
                        });
                        futureList.add(callback);
                    }
                }
                if (triggerLevel < triggerList.size() - 1) {
                    // Keep going but use the next trigger
                    searchEntityContents(child, triggerLevel + 1);
                }
            } else {
                if (response.continueSearch && trigger.isRecursive()) {
                    // continue with current trigger level
                    searchEntityContents(child, triggerLevel);
                }
            }
        }
    }

    private void clearActionKeys(String contextKey, EntityAction action) {
        boolean ignoreContext = false;
        if (contextKey == null) {
            ignoreContext = true;
        }
        for (String key : action.getContextKeysToClearOnDone()) {
            if (ignoreContext) {
                actionContext.remove(key);
            } else {
                String actualKey = contextKey + ":" + key;
                actionContext.remove(actualKey);
            }
        }
    }

}
