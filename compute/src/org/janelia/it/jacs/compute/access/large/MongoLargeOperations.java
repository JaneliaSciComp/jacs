package org.janelia.it.jacs.compute.access.large;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.solr.AncestorSet;
import org.janelia.it.jacs.compute.access.solr.SimpleAnnotation;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Large, memory-bound operations which need to be done on disk using EhCache, lest we run out of heap space.
 * 
 * This version uses the Mongo DB instead of MySQL.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoLargeOperations {

    private static final Logger LOG = LoggerFactory.getLogger(MongoLargeOperations.class);
    
    public static final String ANNOTATION_MAP = "annotationMapCache";
    public static final String ETL_ANNOTATION_MAP = "etlAnnotationMapCache";
    public static final String ANCESTOR_MAP = "ancestorMapCache";
    public static final String SAGE_IMAGEPROP_MAP = "sageImagePropCache";
    public static final String SCREEN_SCORE_MAP = "screenScoreCache";
    
    private DomainDAO dao;
    protected static CacheManager manager;
    protected static Map<String,Cache> caches = new HashMap<>();
    private static final HashSet<Long> EMPTY_SET = new HashSet<>();

    public MongoLargeOperations(DomainDAO dao) {
        this.dao = dao;
        synchronized (MongoLargeOperations.class) {
            if (manager==null) {
                manager = new CacheManager(getClass().getResource("/ehcache2-jacs.xml"));
                caches.put(ANNOTATION_MAP, manager.getCache(ANNOTATION_MAP));
                caches.put(ETL_ANNOTATION_MAP, manager.getCache(ETL_ANNOTATION_MAP));
                caches.put(ANCESTOR_MAP, manager.getCache(ANCESTOR_MAP));
                caches.put(SAGE_IMAGEPROP_MAP, manager.getCache(SAGE_IMAGEPROP_MAP));
                caches.put(SCREEN_SCORE_MAP, manager.getCache(SCREEN_SCORE_MAP));
            }
        }
    }

    public void clearCache(String cacheName) {
        Cache cache = getCache(cacheName);
        cache.removeAll();
    }

    /**
     * Builds a map of entity ids to sets of SimpleAnnotations on disk using EhCache.
     * @throws DaoException
     */
    @SuppressWarnings("unchecked")
    public void buildAnnotationMap() throws DaoException {

        LOG.info("Building annotation map of all entities and annotations");
        Cache annotationMapCache = caches.get(ANNOTATION_MAP);  

        int i = 0;
        for (Annotation annotation : dao.getCollectionByClass(Annotation.class).find().as(Annotation.class)) {
            Long targetId = annotation.getTarget().getTargetId();
            Set<SimpleAnnotation> annots = (Set<SimpleAnnotation>) getValue(annotationMapCache, targetId);
            if (annots == null) {
                annots = new HashSet<>();
            }
            annots.add(new SimpleAnnotation(annotation.getName(), annotation.getOwnerKey()));
            putValue(annotationMapCache, targetId, annots);
            i++;
        }
        
        LOG.info("Processed {} annotations on {} targets", i, annotationMapCache.getSize() );
    }

    /**
     * Builds a map of entity ids to sets of ancestor ids on disk using EhCache.
     * @throws DaoException
     */
    public void buildAncestorMap() throws DaoException {
        LOG.info("Building ancestor map for all tree nodes");
        Cache ancestorMapCache = caches.get(ANCESTOR_MAP);
        
        for (TreeNode treeNode : dao.getDomainObjects(null, TreeNode.class, null)) {
            if (!treeNode.hasChildren()) continue;
            for(Reference ref : treeNode.getChildren()) {
                Long childId = ref.getTargetId();
                
                AncestorSet ancestorSet = (AncestorSet)getValue(ancestorMapCache, childId);
                if (ancestorSet==null) {
                    ancestorSet = new AncestorSet();
                }
                
                ancestorSet.getAncestors().add(treeNode.getId());
                putValue(ancestorMapCache, childId, ancestorSet);
            }
        }

        LOG.info("Loaded tree node graph, now to find the ancestors...");

        for (Object entityIdObj : ancestorMapCache.getKeys()) {
            calculateAncestors((Long)entityIdObj, new HashSet<>(), 0);
        }

        LOG.info("Verifying ancestors...");

        for(Object domainObjectIdObj : ancestorMapCache.getKeys()) {
            Long domainObjectId = (Long)domainObjectIdObj;
            AncestorSet ancestorSet = (AncestorSet)getValue(ancestorMapCache, domainObjectId);
            if (!ancestorSet.isComplete()) {
                LOG.warn("Incomplete ancestor set for "+domainObjectId);
            }
        }

        LOG.info("Done, ancestorMap.size={}", ancestorMapCache.getSize());
    }

    protected Set<Long> calculateAncestors(Long domainObjectId, Set<Long> visited, int level) {

        Cache ancestorMapCache = caches.get(ANCESTOR_MAP);

        if (level > 10000) {
            throw new IllegalStateException("Something went wrong calculating ancestors");
        }

        AncestorSet ancestorSet = (AncestorSet)getValue(ancestorMapCache, domainObjectId);

        if (ancestorSet==null) {
            // Hit a root, it has no ancestors
            return EMPTY_SET;
        }

        if (ancestorSet.isComplete()) {
            // The work's already been done
            return ancestorSet.getAncestors();
        }

        if (visited.contains(domainObjectId)) {
            // Loop detected because the set isn't complete but we're hitting the same entity again. Break out of it..
            ancestorSet.setComplete(true);
            putValue(ancestorMapCache, domainObjectId, ancestorSet);
            return ancestorSet.getAncestors();
        }

        visited.add(domainObjectId);

        Set<Long> ancestors = ancestorSet.getAncestors();
        for(Long parentId : new HashSet<>(ancestors)) {
            ancestors.addAll(calculateAncestors(parentId, visited, level + 1));
        }

        ancestorSet.setComplete(true);
        putValue(ancestorMapCache, domainObjectId, ancestorSet);

        return ancestorSet.getAncestors();
    }

    public Object getValue(String cacheName, Object key) {
        return getValue(getCache(cacheName), key);
    }

    public void putValue(String cacheName, Object key, Object value) {
        putValue(getCache(cacheName), key, value);
    }

    public Cache getCache(String cacheName) {
        if (caches.containsKey(cacheName)) {
            return caches.get(cacheName);
        } else {
            throw new IllegalArgumentException("Unknown cache: "+cacheName);
        }
    }
    
    protected Object getValue(Cache cache, Object key) {
        Element e = cache.get(key);
        return e!=null ? e.getObjectValue() : null;
    }

    protected void putValue(Cache cache, Object key, Object value) {
        Element e = new Element(key, value);
        cache.put(e);
    }
}
