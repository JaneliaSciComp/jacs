package org.janelia.it.jacs.compute.access.large;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.solr.AncestorSet;
import org.janelia.it.jacs.compute.access.solr.SimpleAnnotation;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

/**
 * Large, memory-bound operations which need to be done on disk using EhCache, lest we run out of heap space.
 * 
 * This version uses the Mongo DB instead of MySQL.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoLargeOperations {
	
    private static final Logger log = Logger.getLogger(MongoLargeOperations.class);
	public static final String ANNOTATION_MAP = "annotationMapCache";
	public static final String ANCESTOR_MAP = "ancestorMapCache";
    private DomainDAO dao;
	protected static Map<String,Cache> caches = new HashMap<>();
	private static final HashSet<Long> EMPTY_SET = new HashSet<>();

    public MongoLargeOperations(DomainDAO dao) {
        this.dao = dao;
    }
    /**
     * Builds a map of entity ids to sets of SimpleAnnotations on disk using EhCache.
     * @throws DaoException
     */
    public void buildAnnotationMap() throws DaoException {

    	log.info("Building annotation map of all entities and annotations");
    	Cache annotationMapCache = caches.get(ANNOTATION_MAP);  

		int i = 0;
    	for(Iterator<Annotation> iterator = dao.getCollectionByClass(Annotation.class).find().as(Annotation.class).iterator(); iterator.hasNext(); ) {
    		Annotation annotation = iterator.next();
    		Long targetId = annotation.getTarget().getTargetId();
            Set<SimpleAnnotation> annots = (Set<SimpleAnnotation>)getValue(annotationMapCache, targetId);
			if (annots == null) {
				annots = new HashSet<SimpleAnnotation>();
			}
			annots.add(new SimpleAnnotation(annotation.getName(), annotation.getOwnerKey()));
			putValue(annotationMapCache, targetId, annots);
			i++;
    	}
    	
		log.info("    Processed "+i+" annotations on "+annotationMapCache.getSize()+" targets");
        log.info("    Done, annotationMap.size="+annotationMapCache.getSize());
    }

	/**
     * Builds a map of entity ids to sets of ancestor ids on disk using EhCache.
     * @throws DaoException
     */
    public void buildAncestorMap() throws DaoException {

    	log.info("Building ancestor map for all entities");
    	Cache ancestorMapCache = caches.get(ANCESTOR_MAP);
    	
    	for(Iterator<TreeNode> iterator = dao.getDomainObjects(null, TreeNode.class, null).iterator(); iterator.hasNext(); ) {
    		TreeNode treeNode = iterator.next();
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

    	log.info("    Loaded entity graph, now to find the ancestors...");

    	for(Object entityIdObj : ancestorMapCache.getKeys()) {
    		calculateAncestors((Long)entityIdObj, new HashSet<Long>(), 0);
        }

    	log.info("    Verifying ancestors...");

    	for(Object domainObjectIdObj : ancestorMapCache.getKeys()) {
    		Long domainObjectId = (Long)domainObjectIdObj;
    		AncestorSet ancestorSet = (AncestorSet)getValue(ancestorMapCache, domainObjectId);
    		if (!ancestorSet.isComplete()) {
    			log.warn("Incomplete ancestor set for "+domainObjectId);
    		}
        }

    	log.info("    Done, ancestorMap.size="+ancestorMapCache.getSize());
    }

	protected Set<Long> calculateAncestors(Long domainObjectId, Set<Long> visited, int level) {

		Cache ancestorMapCache = caches.get(ANCESTOR_MAP);

		if (level>10000) {
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

	protected Object getValue(Cache cache, Object key) {
		Element e = cache.get(key);
		return e!=null ? e.getObjectValue() : null;
	}

	protected void putValue(Cache cache, Object key, Object value) {
		Element e = new Element(key, value);
		cache.put(e);
	}
}
