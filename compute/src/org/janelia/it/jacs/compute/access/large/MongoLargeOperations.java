package org.janelia.it.jacs.compute.access.large;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Cache;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.solr.AncestorSet;
import org.janelia.it.jacs.compute.access.solr.SimpleAnnotation;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;

/**
 * Large, memory-bound operations which need to be done on disk using EhCache, lest we run out of heap space.
 * 
 * This version uses the Mongo DB instead of MySQL.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoLargeOperations extends LargeOperations {
	
    private static final Logger log = Logger.getLogger(MongoLargeOperations.class);

    private DomainDAO dao; 

    public MongoLargeOperations(DomainDAO dao, AnnotationDAO annotationDAO) {
        super(annotationDAO);
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

    	for(Object entityIdObj : ancestorMapCache.getKeys()) {
    		Long entityId = (Long)entityIdObj;
    		AncestorSet ancestorSet = (AncestorSet)getValue(ancestorMapCache, entityId);
    		if (!ancestorSet.isComplete()) {
    			log.warn("Incomplete ancestor set for "+entityId);
    		}
        }

    	log.info("    Done, ancestorMap.size="+ancestorMapCache.getSize());
    }

    protected void associateImageProperties(Connection conn, Map<String,Object> imageProps) throws DaoException {
        String sageId = imageProps.get("image_query_id").toString();
        putValue(SAGE_IMAGEPROP_MAP, sageId, imageProps);
    }
}
