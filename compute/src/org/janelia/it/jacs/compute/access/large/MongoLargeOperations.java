package org.janelia.it.jacs.compute.access.large;

import java.sql.SQLException;
import java.util.HashMap;
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
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.jongo.MongoCursor;

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
			String key = annotation.getKey();
			String value = annotation.getValue();
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

    /**
     * Builds a map of image paths to Sage properties.
     * @throws DaoException
     */
    public void buildSageImagePropMap() throws DaoException {
    	
    	SageDAO sage = new SageDAO(log);

    	log.info("Building LSM filename lookup table...");
		Map<String,Long> lsmLookup = new HashMap<String,Long>();
		MongoCursor<LSMImage> cursor = dao.getCollectionByClass(LSMImage.class).find("{class:#}",LSMImage.class.getName()).as(LSMImage.class);
    	for(Iterator<LSMImage> lsmIterator = cursor.iterator(); lsmIterator.hasNext(); ) {
    		LSMImage image = lsmIterator.next();
    		String stackFilepath = image.getFiles().get(FileType.Stack);
    		if (stackFilepath==null) {
    			log.warn("LSMImage missing filepath: "+image.getId());
    			continue;
    		}
        	String[] path = stackFilepath.split("/"); // take just the filename
        	String filename = path[path.length-1];
    		lsmLookup.put(filename, image.getId());
    	}
    	log.info("Got "+lsmLookup.size()+" LSM filenames");

    	log.info("Building property map for all Sage images");
    	for(Iterator<DataSet> iterator = dao.getDomainObjects(DataSet.class).iterator(); iterator.hasNext(); ) {
    		DataSet dataSet = iterator.next();
    		String dataSetIdentifier = dataSet.getIdentifier();
    		log.info("  Building property map for all Sage images in Data Set '"+dataSetIdentifier+"'");

        	ResultSetIterator rsIterator = null;
        	try {
            	rsIterator = sage.getAllImagePropertiesByDataSet(dataSetIdentifier);
        		while (rsIterator.hasNext()) {
            		Map<String,Object> row = rsIterator.next();
                	String imagePath = (String)row.get("path");
                	String[] path = imagePath.split("/"); // take just the filename
                	String filename = path[path.length-1];
                	putValue(SAGE_IMAGEPROP_MAP, lsmLookup.get(filename), row);
            	}
        	}
        	catch (RuntimeException e) {
        		if (e.getCause() instanceof SQLException) {
        			throw new DaoException(e);
        		}
        		throw e;
        	}
            finally {
            	if (rsIterator!=null) rsIterator.close();
            }
    	}
    }
}
