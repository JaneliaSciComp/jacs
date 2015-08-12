package org.janelia.it.workstation.gui.browser.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectCreateEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.gui.browser.model.DomainObjectComparator;
import org.janelia.it.workstation.gui.browser.model.DomainObjectId;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a unified domain model for client-side operations. All changes to the model should go through this class, as well as any domain object accesses
 * which need to maintain the domain object in memory for more than one user interaction.
 *
 * A few guiding principles:
 *
 * 1) The methods in this class can potentially access the data source, so they should ALWAYS be called from a worker thread, never from the EDT. By
 * contrast, all resulting events are queued on the EDT, so that GUI widgets can immediately make use of the results.
 *
 * 2) Only one instance of any given domain object is ever maintained by this model. If the domain object has changed, the existing instance is updated with new
 * information. Duplicate instances are always discarded. The canonical instance (cached instance) is returned by most methods, and clients can
 * synchronize with the cache by using those instances.
 *
 * 3) Invalidating a given domain object removes it from the cache entirely. Clients are encouraged to listen for the corresponding DomainObjectInvalidationEvent,
 * and discard their references to invalidated entities.
 *
 * 4) Updates to the cache are atomic (synchronized to the DomainObjectModel instance).
 *
 * 5) Cached entities may expire at any time, if they are not referenced outside of the cache. Therefore, you should never write any code that relies
 * on entities being cached or uncached.
 *
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainModel {
    
    private static final Logger log = LoggerFactory.getLogger(DomainModel.class);
    
    private final DomainDAO dao;
    private final Cache<DomainObjectId, DomainObject> objectCache;
    private final Map<DomainObjectId, Workspace> workspaceCache;
    
    public DomainModel(DomainDAO dao) {
        this.dao = dao;
        this.objectCache = CacheBuilder.newBuilder().softValues().removalListener(new RemovalListener<DomainObjectId, DomainObject>() {
            @Override
            public void onRemoval(RemovalNotification<DomainObjectId, DomainObject> notification) {
                synchronized (DomainModel.this) {
                    DomainObjectId id = notification.getKey();
                    //DomainObject domainObject = notification.getValue();
                    if (workspaceCache.containsKey(id)) {
                        workspaceCache.clear();
                    }
                }
            }
        }).build();
        this.workspaceCache = new LinkedHashMap<>();
    }
    
    /**
     * Ensure the given object is in the cache, or generate appropriate warnings/exceptions.
     *
     * @param domainObject
     */
    private DomainObjectId checkIfCanonicalDomainObject(DomainObject domainObject) {
        DomainObjectId id = DomainObjectId.createFor(domainObject);
        try {
            DomainObject presentObject = objectCache.getIfPresent(id);
            if (presentObject == null) {
                throw new IllegalStateException("Not in domain object model: " + DomainUtils.identify(domainObject));
            }
            else if (presentObject != domainObject) {
                throw new IllegalStateException("DomainModel: Instance mismatch: " + domainObject.getName()
                        + " (cached=" + System.identityHashCode(presentObject)
                        + ") vs (this=" + System.identityHashCode(domainObject) + ")");
            }
        }
        catch (IllegalStateException e) {
            log.warn("Problem verifying domain model", e);
        }
        return id;
    }
    
    /**
     * Put the object in the cache, or update the cached domain object if there is already a version in the cache. In the latter case, the updated cached
     * instance is returned, and the argument instance can be discarded.
     *
     * If the domain object has children which are already cached, those references will be replaced with references to the cached instances.
     *
     * This method may generate an DomainObjectUpdated event.
     *
     * @param domainObject
     * @return canonical domain object instance
     */
    private DomainObject putOrUpdate(DomainObject domainObject) {
        if (domainObject == null) {
            // This is a null object, which cannot go into the cache
            log.trace("putOrUpdate: object is null");
            return null;
        }
        synchronized (this) {
            DomainObjectId id = DomainObjectId.createFor(domainObject);
            DomainObject canonicalObject = objectCache.getIfPresent(id);
            if (canonicalObject != null) {
                if (canonicalObject!=domainObject) {
                    log.debug("putOrUpdate: Updating cached instance: {}", DomainUtils.identify(canonicalObject));
                    objectCache.put(id, domainObject);
                    notifyDomainObjectsInvalidated(canonicalObject);
                }
                else {
                    log.debug("putOrUpdate: Returning cached instance: {}", DomainUtils.identify(canonicalObject));
                }
            }
            else {
                canonicalObject = domainObject;
                log.debug("putOrUpdate: Caching: {}", DomainUtils.identify(canonicalObject));
                objectCache.put(id, domainObject);
            }
            
            return canonicalObject;
        }
    }
    
    /**
     * Call putOrUpdate on all the objects in a given collection.
     *
     * @param objects
     * @return canonical domain object instances
     */
    private List<DomainObject> putOrUpdateAll(Collection<DomainObject> objects) {
        List<DomainObject> putObjects = new ArrayList<>();
        for (DomainObject domainObject : objects) {
            putObjects.add(putOrUpdate(domainObject));
        }
        return putObjects;
    }
    
    /**
     * Clear the entire cache without raising any events. This is basically only useful for changing logins.
     */
    public void invalidateAll() {
        log.debug("Invalidating all objects");
        objectCache.invalidateAll();
        workspaceCache.clear();
        Events.getInstance().postOnEventBus(new DomainObjectInvalidationEvent());
    }

    /**
     * Invalidate the domain object in the cache, so that it will be reloaded on the next request.
     *
     * @param domainObject
     * @param recurse
     */
    public void invalidate(DomainObject domainObject) {
        List<DomainObject> objects = new ArrayList<>();
        objects.add(domainObject);
        invalidate(objects);
    }

    /**
     * Invalidate any number of entities in the cache, so that they will be reloaded on the next request.
     *
     * @param objects
     * @param recurse
     */
    private void invalidate(Collection<DomainObject> objects) {
        log.debug("Invalidating {} objects", objects.size());
        for (DomainObject domainObject : objects) {
            invalidateInternal(domainObject);
        }
        notifyDomainObjectsInvalidated(objects);
    }

    private void invalidateInternal(DomainObject domainObject) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("DomainModel illegally called from EDT!");
        }
        DomainObjectId id = checkIfCanonicalDomainObject(domainObject);
        log.debug("Invalidating cached instance {}", DomainUtils.identify(domainObject));

        objectCache.invalidate(id);
        workspaceCache.remove(id);

        // Reload the domain object and stick it into the cache
        DomainObject canonicalDomainObject;
        try {
            canonicalDomainObject = loadDomainObject(id);
            if (canonicalDomainObject==null) {
                log.warn("Object no longer exists: "+id);
                return;
            }
            canonicalDomainObject = putOrUpdate(canonicalDomainObject);
            log.debug("Got new canonical object: {}", DomainUtils.identify(canonicalDomainObject));
        }
        catch (Exception e) {
            log.error("Problem reloading invalidated object: {}", DomainUtils.identify(domainObject), e);
        }
    }

    /**
     * Reload the given domain object from the database, and update the cache.
     *
     * @param domainObject
     * @return canonical domain object instance
     * @throws Exception
     */
    public DomainObject reload(DomainObject domainObject) throws Exception {
        return reloadById(DomainObjectId.createFor(domainObject));
    }

    /**
     * Reload all the entities in the given collection, and update the cache.
     *
     * @param objects
     * @return canonical domain object instances
     * @throws Exception
     */
    public List<DomainObject> reload(Collection<DomainObject> objects) throws Exception {
        return reloadById(DomainUtils.getDomainObjectIdList(objects));
    }

    /**
     * Reload the given domain object from the database, and update the cache.
     *
     * @param domainObjectId
     * @return canonical domain object instance
     * @throws Exception
     */
    public DomainObject reloadById(DomainObjectId domainObjectId) throws Exception {
        synchronized (this) {
            DomainObject domainobject = loadDomainObject(domainObjectId);
            return putOrUpdate(domainobject);
        }
    }

    /**
     * Reload all the entities in the given collection, and update the cache.
     *
     * @param domainObjectId
     * @return canonical domain object instances
     * @throws Exception
     */
    public List<DomainObject> reloadById(List<DomainObjectId> domainObjectIds) throws Exception {
        synchronized (this) {
            List<DomainObject> objects = loadDomainObjects(domainObjectIds);
            return putOrUpdateAll(objects);
        }
    }
    
    private DomainObject loadDomainObject(DomainObjectId id) {
        log.debug("loadDomainObject({})",id);
        String type = dao.getTypeNameByClassName(id.getClassName());
        Reference ref = new Reference(type, id.getId());
        return dao.getDomainObject(SessionMgr.getSubjectKey(), ref);
    }
    
    private List<DomainObject> loadDomainObjects(Collection<DomainObjectId> ids) {
        List<DomainObject> objs = new ArrayList<>();
        for(DomainObjectId id : ids) {
            DomainObject domainObject = loadDomainObject(id);
            if (domainObject!=null) objs.add(domainObject);
        }
        return objs;
    }

    /**
     * Retrieve the given domain object, checking the cache first, and then falling back to the database.
     *
     * @param id
     * @return canonical domain object instance
     * @throws Exception
     */
    public DomainObject getDomainObjectById(DomainObjectId id) throws Exception {
        DomainObject domainObject = objectCache.getIfPresent(id);
        if (domainObject != null) {
            log.debug("getEntityById: returning cached domain object {}", DomainUtils.identify(domainObject));
            return domainObject;
        }
        synchronized (this) {
            return putOrUpdate(loadDomainObject(id));
        }
    }
    
    public TreeNode getDomainObject(Class<? extends DomainObject> domainClass, Long id) throws Exception {
        return (TreeNode)getDomainObjectById(new DomainObjectId(domainClass, id));
    }
    
    public List<DomainObject> getDomainObjects(List<Reference> references) {
                
        log.debug("getDomainObjects(references.size={})",references.size());
        
        DomainObject[] domainObjects = new DomainObject[references.size()];
        List<Reference> unsatisfiedRefs = new ArrayList<>();
        int i = 0;
        for(Reference ref : references) {
            DomainObjectId did = getIdForReference(ref);
            DomainObject domainObject = did==null?null:objectCache.getIfPresent(did);
            if (domainObject!=null) {
                domainObjects[i] = domainObject;
            }
            else {
                unsatisfiedRefs.add(ref);
            }
            i++;
        }
        
        if (!unsatisfiedRefs.isEmpty()) {
            List<DomainObject> objects = dao.getDomainObjects(SessionMgr.getSubjectKey(), unsatisfiedRefs);
            LinkedList<DomainObject> objQueue = new LinkedList<>(objects);
            synchronized (this) {
                for(int j=0; j<domainObjects.length; j++) {
                    if (domainObjects[j]==null) {
                        DomainObject domainObject = putOrUpdate(objQueue.pop());
                        domainObjects[j] = domainObject;
                    }
                }
            }
        }
        
        log.debug("getDomainObjects: returning {} objects",domainObjects.length);
        return Arrays.asList(domainObjects);
    }
    
    public DomainObject getDomainObject(String type, Long id) {
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        List<DomainObject> objects = getDomainObjects(type, ids);
        if (objects.isEmpty()) return null;
        return objects.get(0);
    }
    
    public List<DomainObject> getDomainObjects(String type, List<Long> ids) {
        
        log.debug("getDomainObjects(type={},ids.size={})",type,ids.size());
        
        DomainObject[] domainObjects = new DomainObject[ids.size()];
        List<Long> unsatisfiedIds = new ArrayList<>();
        int i = 0;
        for(Long id : ids) {
            DomainObjectId did = getIdForReference(new Reference(type, id));
            DomainObject domainObject = did==null?null:objectCache.getIfPresent(did);
            if (domainObject!=null) {
                domainObjects[i] = domainObject;
            }
            else {
                unsatisfiedIds.add(id);
            }
            i++;
        }
        
        log.debug("getDomainObjects: there are {} unsatisfied ids ",unsatisfiedIds.size());
        
        List<DomainObject> objects = dao.getDomainObjects(SessionMgr.getSubjectKey(), type, unsatisfiedIds);
        if (!objects.isEmpty()) {
            log.debug("getDomainObjects: got {} objects to fill unsatisfied ids",objects.size());
            Map<Long,DomainObject> map = DomainUtils.mapById(objects);
            
            synchronized (this) {
                for(int j=0; j<domainObjects.length; j++) {
                    if (domainObjects[j]==null) {
                        DomainObject domainObject = map.get(ids.get(j));
                        if (domainObject!=null) {
                            domainObjects[j] = putOrUpdate(domainObject);
                        }
                    }
                }
            }
        }
        
        log.debug("getDomainObjects: returning {} objects",domainObjects.length);
        return Arrays.asList(domainObjects);
    }
    
    public List<Annotation> getAnnotations(Long targetId) {
        // TODO: cache these?
        return dao.getAnnotations(SessionMgr.getSubjectKey(), targetId);
    }
    
    public List<Annotation> getAnnotations(Collection<Long> targetIds) {
        // TODO: cache these?
        return dao.getAnnotations(SessionMgr.getSubjectKey(), targetIds);
    }
    
    public Collection<Workspace> getWorkspaces() {
        synchronized (this) {
            if (workspaceCache.isEmpty()) {
                log.debug("Getting workspaces from database");
                for (Workspace workspace : dao.getWorkspaces(SessionMgr.getSubjectKey())) {
                    Workspace cachedRoot = (Workspace)putOrUpdate(workspace);
                    if (cachedRoot instanceof Workspace) {
                        workspaceCache.put(DomainObjectId.createFor(cachedRoot), cachedRoot);
                    }
                }
            }
        }
        List<Workspace> workspaces = new ArrayList<>(workspaceCache.values());
        Collections.sort(workspaces, new DomainObjectComparator());
        return workspaces;
        
    }
    
    public Workspace getDefaultWorkspace() throws Exception {
        return getWorkspaces().iterator().next();
    }
    
    public Collection<Ontology> getOntologies() throws Exception {
        List<Ontology> ontologies = new ArrayList<>();
        for (Ontology ontology : dao.getOntologies(SessionMgr.getSubjectKey())) {
            putOrUpdate(ontology);
            ontologies.add(ontology);
        }
        Collections.sort(ontologies, new DomainObjectComparator());
        return ontologies;
    }
    
    public void changePermissions(String type, Long id, String granteeKey, String rights, boolean grant) throws Exception {
        synchronized (this) {
            dao.changePermissions(granteeKey, type, id, granteeKey, rights, grant);
            putOrUpdate(getDomainObject(type, id));
        }
    }
    
    public void changePermissions(String type, Collection<Long> ids, String granteeKey, String rights, boolean grant) throws Exception {
        synchronized (this) {
            dao.changePermissions(granteeKey, type, ids, granteeKey, rights, grant);
            for(Long id : ids) {
                putOrUpdate(getDomainObject(type, id));
            }
        }
    }
    
    // TODO: replace this with creation and mutation methods
    public void save(TreeNode treeNode) throws Exception {
        synchronized (this) {
            dao.save(SessionMgr.getSubjectKey(), treeNode);
            putOrUpdate(treeNode);
        }
    }
    
    public void save(Filter filter) throws Exception {
        synchronized (this) {
            dao.save(SessionMgr.getSubjectKey(), filter);
            putOrUpdate(filter);
        }
    }
    
    // TODO: replace this with creation and mutation methods
    public void save(ObjectSet objectSet) throws Exception {
        synchronized (this) {
            dao.save(SessionMgr.getSubjectKey(), objectSet);
            putOrUpdate(objectSet);
        }
    }
    
    public void reorderChildren(TreeNode treeNode, int[] order) throws Exception {
        synchronized (this) {
            dao.reorderChildren(SessionMgr.getSubjectKey(), treeNode, order);
            putOrUpdate(treeNode);
        }
    }
    
    public void addChild(TreeNode treeNode, DomainObject domainObject) throws Exception {
        synchronized (this) {
            dao.addChild(SessionMgr.getSubjectKey(), treeNode, domainObject);
            putOrUpdate(treeNode);
        }
    }
    
    public void addChildren(TreeNode treeNode, Collection<DomainObject> domainObjects) throws Exception {
        synchronized (this) {
            dao.addChildren(SessionMgr.getSubjectKey(), treeNode, domainObjects);
            // TODO: when we make this remote, we need addChildren to return the updated object, 
            // or we need to get it separately to put it into the cache
            putOrUpdate(treeNode);
        }
    }
    
    public void removeChild(TreeNode treeNode, DomainObject domainObject) throws Exception {
        synchronized (this) {
            dao.removeChild(SessionMgr.getSubjectKey(), treeNode, domainObject);
            putOrUpdate(treeNode);
        }
    }
    
    public void removeChildren(TreeNode treeNode, Collection<DomainObject> domainObjects) throws Exception {
        synchronized (this) {
            dao.removeChildren(SessionMgr.getSubjectKey(), treeNode, domainObjects);
            putOrUpdate(treeNode);
        }
    }
    
    // TODO: replace this with automatic dead reference clean up
    public void removeReference(TreeNode treeNode, Reference reference) throws Exception {
        synchronized (this) {
            dao.removeReference(SessionMgr.getSubjectKey(), treeNode, reference);
            putOrUpdate(treeNode);
        }
    }
    
    public void addMember(ObjectSet objectSet, DomainObject domainObject) throws Exception {
        synchronized (this) {
            dao.addMember(SessionMgr.getSubjectKey(), objectSet, domainObject);
            putOrUpdate(objectSet);
        }
    }
    
    public void addMembers(ObjectSet objectSet, Collection<DomainObject> domainObjects) throws Exception {
        synchronized (this) {
            dao.addMembers(SessionMgr.getSubjectKey(), objectSet, domainObjects);
            putOrUpdate(objectSet);
        }
    }
    
    public void removeMember(ObjectSet objectSet, DomainObject domainObject) throws Exception {
        synchronized (this) {
            dao.removeMember(SessionMgr.getSubjectKey(), objectSet, domainObject);
            putOrUpdate(objectSet);
        }
    }
    
    public void removeMembers(ObjectSet objectSet, Collection<DomainObject> domainObjects) throws Exception {
        synchronized (this) {
            dao.removeMembers(SessionMgr.getSubjectKey(), objectSet, domainObjects);
            putOrUpdate(objectSet);
        }
    }
    
    public void updateProperty(DomainObject domainObject, String propName, String propValue) {
        synchronized (this) {
            dao.updateProperty(SessionMgr.getSubjectKey(), domainObject, propName, propValue);
            putOrUpdate(domainObject);
        }
    }
    
    
    
    private DomainObjectId getIdForReference(Reference ref) {
        Class<? extends DomainObject> clazz = dao.getObjectClass(ref.getTargetType());
        if (clazz==null) {
            log.warn("Unrecognized class for target type "+ref.getTargetType());
            return null;
        }
        String className = clazz.getName();
        return new DomainObjectId(className, ref.getTargetId());
    }
    
    private void notifyDomainObjectCreated(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectCreateEvent for {}", DomainUtils.identify(domainObject));
        }
        Events.getInstance().postOnEventBus(new DomainObjectCreateEvent(domainObject));
    }

    private void notifyDomainObjectChanged(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectChangeEvent for {}", DomainUtils.identify(domainObject));
        }
        Events.getInstance().postOnEventBus(new DomainObjectChangeEvent(domainObject));
    }

    private void notifyDomainObjectRemoved(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectRemoveEvent for {}", DomainUtils.identify(domainObject));
        }
        Events.getInstance().postOnEventBus(new DomainObjectRemoveEvent(domainObject));
    }

    private void notifyDomainObjectsInvalidated(Collection<DomainObject> objects) {
        if (log.isTraceEnabled()) {
            log.trace("Generating DomainObjectInvalidationEvent with {} entities", objects.size());
        }
        Events.getInstance().postOnEventBus(new DomainObjectInvalidationEvent(objects));
    }

    private void notifyDomainObjectsInvalidated(DomainObject domainObject) {
        if (log.isTraceEnabled()) {
            log.trace("Generating EntityInvalidationEvent for {}", DomainUtils.identify(domainObject));
        }
        Collection<DomainObject> invalidated = new ArrayList<>();
        invalidated.add(domainObject);
        Events.getInstance().postOnEventBus(new DomainObjectInvalidationEvent(invalidated));
    }

}
