package org.janelia.it.jacs.compute.mservice;

import com.google.common.util.concurrent.*;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
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
    protected User user;
    protected int maxThreads;
    ListeningExecutorService listeningExecutorService;
    List<ListenableFuture<Object>> futureList=new ArrayList<ListenableFuture<Object>>();
    List<EntitySearchTrigger> triggerList=new ArrayList<EntitySearchTrigger>();
    List<EntityAction> actionList=new ArrayList<EntityAction>();
    Map<Entity, Integer> levelMap=new HashMap<Entity, Integer>();

    // If maxThreads==0, this means don't use threads - run single-threaded
    public MService(String username, int maxThreads) throws Exception {
        user=getComputeBean().getUserByName(username);
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
        return new FileDiscoveryHelper(entityBean, computeBean, user);
    }

    /////////// run method variations ///////////////////////////////////////////////////////////////////////////////

    public void run(Entity startingEntity, EntitySearchTrigger trigger, EntityAction action) throws Exception {
        triggerList.add(trigger);
        actionList.add(action);
        searchEntityContents(startingEntity);
    }

    public void run(Entity startingEntity, List<EntitySearchTrigger> triggerList, EntityAction action) throws Exception {
        this.triggerList.addAll(triggerList);
        actionList.add(action);
        searchEntityContents(startingEntity);
    }

    public void run(Entity startingEntity, EntitySearchTrigger trigger, List<EntityAction> actionList) throws Exception {
        triggerList.add(trigger);
        this.actionList.addAll(actionList);
        searchEntityContents(startingEntity);
    }

    public void run(Entity startingEntity, List<EntitySearchTrigger> triggerList, List<EntityAction> actionList) throws Exception {
        this.triggerList.addAll(triggerList);
        this.actionList.addAll(actionList);
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
        //logger.info("level=" + parentLevel + " : name=" + parent.getName() + " type=" + parent.getEntityType().getName() + " triggerLevel="+triggerLevel);
        EntitySearchTrigger trigger = triggerList.get(triggerLevel);
        parent = getEntityBean().getEntityAndChildren(parent.getId());
        Set<Entity> children = parent.getChildren();
        for (Entity child : children) {
            Integer childLevel = parentLevel + 1;
            levelMap.put(child, childLevel);
            EntitySearchTrigger.TriggerResponse response = trigger.evaluate(parent, child, childLevel);
            if (response.performAction) {
                if (triggerLevel == triggerList.size() - 1) {
                    // if this is the last trigger
                    if (listeningExecutorService != null) {
                        for (final EntityAction action : actionList) {
                            //logger.info("Submitting action thread to executorService");
                            ListenableFuture<Object> callback = listeningExecutorService.submit(action.getCallable(parent, child));
                            Futures.addCallback(callback, new FutureCallback<Object>() {
                                @Override
                                public void onSuccess(Object o) {
                                    action.processResult(o);
                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    action.handleFailure();
                                }
                            });
                            futureList.add(callback);
                        }
                    } else {
                        //logger.info("Running action within current thread");
                        for (final EntityAction action : actionList) {
                            Object result;
                            try {
                                result = action.getCallable(parent, child).call();
                                action.processResult(result);
                            }
                            catch (Exception ex) {
                                action.handleFailure();
                            }
                        }
                    }
                } else {
                    // this is not the last trigger
                    triggerLevel++;
                }
            }
            if (response.continueSearch) {
                searchEntityContents(child, triggerLevel);
            }
        }
    }

}
