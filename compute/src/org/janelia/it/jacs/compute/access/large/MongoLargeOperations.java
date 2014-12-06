package org.janelia.it.jacs.compute.access.large;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Cache;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAO;
import org.janelia.it.jacs.compute.access.solr.AncestorSet;
import org.janelia.it.jacs.compute.access.solr.SimpleAnnotation;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;

/**
 * Large, memory-bound operations which need to be done on disk using EhCache, lest we run out of heap space.
 * 
 * This version uses the Mongo DB instead of MySQL.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoLargeOperations extends LargeOperations {
	
    private static final Logger logger = Logger.getLogger(MongoLargeOperations.class);

    private DomainDAO dao; 

    public MongoLargeOperations(DomainDAO dao) {
		this.dao = dao;
    }
    
    /**
     * Builds a map of entity ids to sets of SimpleAnnotations on disk using EhCache.
     * @throws DaoException
     */
    public void buildAnnotationMap() throws DaoException {

    	logger.info("Building annotation map of all entities and annotations");
    	Cache annotationMapCache = caches.get(ANNOTATION_MAP);  

		int i = 0;
    	for(Iterator<Annotation> iterator = dao.getCollectionByClass(Annotation.class).find().as(Annotation.class).iterator(); iterator.hasNext(); ) {
    		Annotation annotation = iterator.next();
    		Long targetId = annotation.getTarget().getTargetId();
            Set<SimpleAnnotation> annots = (Set<SimpleAnnotation>)getValue(annotationMapCache, targetId);
			if (annots == null) {
				annots = new HashSet<SimpleAnnotation>();
			}
			String key = annotation.getKey();
			String value = annotation.getValue();
			annots.add(new SimpleAnnotation(annotation.getName(), key, value, annotation.getOwnerKey()));
			putValue(annotationMapCache, targetId, annots);
			i++;
    	}
    	
		logger.info("    Processed "+i+" annotations on "+annotationMapCache.getSize()+" targets");
        logger.info("    Done, annotationMap.size="+annotationMapCache.getSize());
    }

	/**
     * Builds a map of entity ids to sets of ancestor ids on disk using EhCache.
     * @throws DaoException
     */
    public void buildAncestorMap() throws DaoException {

    	logger.info("Building ancestor map for all entities");
    	Cache ancestorMapCache = caches.get(ANCESTOR_MAP);
    	
    	for(Iterator<TreeNode> iterator = dao.getDomainObjects(TreeNode.class).iterator(); iterator.hasNext(); ) {
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

    	logger.info("    Loaded entity graph, now to find the ancestors...");

    	for(Object entityIdObj : ancestorMapCache.getKeys()) {
    		calculateAncestors((Long)entityIdObj, new HashSet<Long>(), 0);
        }

    	logger.info("    Verifying ancestors...");

    	for(Object entityIdObj : ancestorMapCache.getKeys()) {
    		Long entityId = (Long)entityIdObj;
    		AncestorSet ancestorSet = (AncestorSet)getValue(ancestorMapCache, entityId);
    		if (!ancestorSet.isComplete()) {
    			logger.warn("Incomplete ancestor set for "+entityId);
    		}
        }

    	logger.info("    Done, ancestorMap.size="+ancestorMapCache.getSize());
    }

    /**
     * Builds a map of image paths to Sage properties.
     * @throws DaoException
     */
    public void buildSageImagePropMap() throws DaoException {
    	
    	logger.info("Building property map for all Sage images");
    	SageDAO sage = new SageDAO(logger);

    	for(Iterator<DataSet> iterator = dao.getDomainObjects(DataSet.class).iterator(); iterator.hasNext(); ) {
    		DataSet dataSet = iterator.next();
    		String dataSetIdentifier = dataSet.getIdentifier();
    		logger.info("  Building property map for all Sage images in Data Set '"+dataSetIdentifier+"'");
        	ResultSetIterator rsIterator = sage.getAllImagePropertiesByDataSet(dataSetIdentifier);
    		while (iterator.hasNext()) {
        		Map<String,Object> row = rsIterator.next();
				associateImageProperties(row);
        	}
    	}
    }
    
    private void associateImageProperties(Map<String,Object> imageProps) throws DaoException {
    	String imagePath = (String)imageProps.get("path");
    	String[] path = imagePath.split("/"); // take just the filename
    	String filename = path[path.length-1];
    	for(Iterator<LSMImage> iterator = dao.getCollectionByClass(LSMImage.class).find("{'files.Stack':#}",filename).projection("{_id:1}").as(LSMImage.class).iterator(); iterator.hasNext(); ) {
    		LSMImage image = iterator.next();
    		putValue(SAGE_IMAGEPROP_MAP, image.getId(), imageProps);
    	}
    }
}
