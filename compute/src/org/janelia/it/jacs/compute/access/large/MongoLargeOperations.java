package org.janelia.it.jacs.compute.access.large;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAO;
import org.janelia.it.jacs.compute.access.solr.AncestorSet;
import org.janelia.it.jacs.compute.access.solr.SimpleAnnotation;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Large, memory-bound operations which need to be done on disk using EhCache, lest we run out of heap space.
 * 
 * This version uses the Mongo DB instead of MySQL.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoLargeOperations {
	
    private static final Logger logger = Logger.getLogger(MongoLargeOperations.class);

	public static final String ANNOTATION_MAP = "annotationMapCache";
	public static final String ANCESTOR_MAP = "ancestorMapCache";
	public static final String NEO4J_MAP = "neo4jMapCache";
	public static final String SAGE_IMAGEPROP_MAP = "sageImagePropCache";
	public static final String SCREEN_SCORE_MAP = "screenScoreCache";
	
	protected static CacheManager manager;
	// Local cache for faster access to caches, without all the unnecessary checking that the CacheManager does
	protected static Map<String,Cache> caches = new HashMap<String,Cache>();

    private DomainDAO dao; 

    public MongoLargeOperations(String serverUrl, String databaseName) throws UnknownHostException {
		this.dao = new DomainDAO(serverUrl, databaseName);
    }
    
    public MongoLargeOperations() {
        synchronized (MongoLargeOperations.class) {
	        if (manager==null) {
	        	manager = new CacheManager(getClass().getResource("/ehcache2-jacs.xml"));
	        	caches.put(ANNOTATION_MAP, manager.getCache(ANNOTATION_MAP));
	        	caches.put(ANCESTOR_MAP, manager.getCache(ANCESTOR_MAP));
	        	caches.put(NEO4J_MAP, manager.getCache(NEO4J_MAP));
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
    public void buildAnnotationMap() throws DaoException {

    	logger.info("Building annotation map of all entities and annotations");
    	Cache annotationMapCache = caches.get(ANNOTATION_MAP);  

		int i = 0;
    	for(Iterator<Annotation> iterator = dao.getCollectionByName("annotations").find().as(Annotation.class).iterator(); iterator.hasNext(); ) {
    		Annotation annotation = iterator.next();
    		Long targetId = annotation.getTarget().getTargetId();
            Set<SimpleAnnotation> annots = (Set<SimpleAnnotation>)getValue(annotationMapCache, targetId);
			if (annots == null) {
				annots = new HashSet<SimpleAnnotation>();
			}
			OntologyTerm keyTerm = getOntologyTerm(annotation.getKeyTerm());
			OntologyTerm valueTerm = getOntologyTerm(annotation.getValueTerm());
			
			String key = keyTerm.getName();
			String value = valueTerm.getName(); // or regular value?
			
			
			annots.add(new SimpleAnnotation(annotation.getName(), key, value, annotation.getOwnerKey()));
			putValue(annotationMapCache, targetId, annots);
			i++;
    	}
    	
		logger.info("    Processed "+i+" annotations on "+annotationMapCache.getSize()+" targets");
        logger.info("    Done, annotationMap.size="+annotationMapCache.getSize());
    }
    
    private OntologyTerm getOntologyTerm(OntologyTermReference keyTerm) {
		// TODO
		return null;
	}

	/**
     * Builds a map of entity ids to sets of ancestor ids on disk using EhCache.
     * @throws DaoException
     */
    public void buildAncestorMap() throws DaoException {

    	logger.info("Building ancestor map for all entities");
    	
//    	Cache ancestorMapCache = caches.get(ANCESTOR_MAP);
//    	Connection conn = null;
//    	PreparedStatement stmt = null;
//    	ResultSet rs = null;
//    	try {
//	        conn = annotationDAO.getJdbcConnection();
//	        
//            StringBuilder sql = new StringBuilder();
//	        sql.append("select ed.child_entity_id, e.id ");
//    		sql.append("from entity e ");
//	        sql.append("join entityData ed on e.id=ed.parent_entity_id ");
//	        sql.append("where ed.child_entity_id is not null ");
//	        sql.append("order by ed.child_entity_id ");
//
//	        Long currChildId = null;
//	        AncestorSet ancestorSet = new AncestorSet();
//	        	
//	        stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
//	        stmt.setFetchSize(Integer.MIN_VALUE);
//
//			rs = stmt.executeQuery();
//	    	logger.info("    Processing results");
//			while (rs.next()) {
//				Long childId = rs.getBigDecimal(1).longValue();
//				Long entityId = rs.getBigDecimal(2).longValue();
//				if (currChildId!=null && !childId.equals(currChildId)) {
//					putValue(ancestorMapCache, currChildId, ancestorSet);
//					ancestorSet = new AncestorSet();
//				}
//				currChildId = childId;
//				ancestorSet.getAncestors().add(entityId);
//                }
//
//			if (currChildId!=null) {
//				putValue(ancestorMapCache, currChildId, ancestorSet);
//			}	
//			
//    	}
//    	catch (SQLException e) {
//    		throw new DaoException(e);
//    	}
//        finally {
//        	try {
//        		if (rs!=null) rs.close();
//        		if (stmt!=null) stmt.close();
//        		if (conn!=null) conn.close();	
//        	}
//            catch (Exception e) {
//                logger.warn("Error closing JDBC connection",e);
//            }
//        }
//
//    	logger.info("    Loaded entity graph, now to find the ancestors...");
//
//    	for(Object entityIdObj : ancestorMapCache.getKeys()) {
//    		calculateAncestors((Long)entityIdObj, new HashSet<Long>(), 0);
//        }
//
//    	logger.info("    Verifying ancestors...");
//
//    	for(Object entityIdObj : ancestorMapCache.getKeys()) {
//    		Long entityId = (Long)entityIdObj;
//    		AncestorSet ancestorSet = (AncestorSet)getValue(ancestorMapCache, entityId);
//    		if (!ancestorSet.isComplete()) {
//    			logger.warn("Incomplete ancestor set for "+entityId);
//    		}
//        }
//
//    	logger.info("    Done, ancestorMap.size="+ancestorMapCache.getSize());
    }

    private static final HashSet<Long> EMPTY_SET = new HashSet<Long>();
//    boolean debugAncestors = false;
    private Set<Long> calculateAncestors(Long entityId, Set<Long> visited, int level) {

    	Cache ancestorMapCache = caches.get(ANCESTOR_MAP);
    	
    	if (level>10000) {
    		throw new IllegalStateException("Something went wrong calculating ancestors");
    	}
    	
//    	StringBuffer b = new StringBuffer();
//    	for(int i=0; i<level; i++) {
//    		b.append("    ");
//    	}
//    	if (debugAncestors) logger.info(b+""+entityId);
    	
    	AncestorSet ancestorSet = (AncestorSet)getValue(ancestorMapCache, entityId);
    	
    	if (ancestorSet==null) {
    		// Hit a root, it has no ancestors
//    		if (debugAncestors) logger.info(b+""+entityId+" is root");
    		return EMPTY_SET;
    	}
    		
    	if (ancestorSet.isComplete()) {
    		// The work's already been done
//    		if (debugAncestors) logger.info(b+""+entityId+" is complete");
    		return ancestorSet.getAncestors();
    	}

    	if (visited.contains(entityId)) {
    		// Loop detected because the set isn't complete but we're hitting the same entity again. Break out of it..
    		ancestorSet.setComplete(true);
    		putValue(ancestorMapCache, entityId, ancestorSet);
//    		if (debugAncestors) logger.info(b+""+entityId+" is loop");
    		return ancestorSet.getAncestors();
    	}

    	visited.add(entityId);
    	
    	Set<Long> ancestors = ancestorSet.getAncestors();
    	for(Long parentId : new HashSet<Long>(ancestors)) {
    		ancestors.addAll(calculateAncestors(parentId, visited, level+1));
    	}
    	
    	ancestorSet.setComplete(true);
    	putValue(ancestorMapCache, entityId, ancestorSet);
    	
//    	if (debugAncestors) logger.info(b+""+entityId+" has "+ancestorSet.getAncestors().size()+" ancestors");
    	return ancestorSet.getAncestors();
    }
    
    /**
     * Builds a map of image paths to Sage properties.
     * @throws DaoException
     */
    public void buildSageImagePropMap() throws DaoException {
    	
//    	logger.info("Building property map for all Sage images");
//    	SageDAO sage = new SageDAO(logger);
//    	Connection conn = null;
//    	
//    	try {
//    		conn = annotationDAO.getJdbcConnection();
//
//	    	for(Entity dataSet : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
//	    		
//	    		String dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
//	    		logger.info("  Building property map for all Sage images in Data Set '"+dataSetIdentifier+"'");
//	    		
//	        	ResultSetIterator iterator = null;
//	        	
//	        	try {
//                    iterator = sage.getAllImagePropertiesByDataSet(dataSetIdentifier);
//	        		while (iterator.hasNext()) {
//	            		Map<String,Object> row = iterator.next();
//	    				associateImageProperties(conn, row);
//	            	}
//	        	}
//	        	catch (RuntimeException e) {
//	        		if (e.getCause() instanceof SQLException) {
//	        			throw new DaoException(e);
//	        		}
//	        		throw e;
//	        	}
//	            finally {
//	            	if (iterator!=null) iterator.close();
//	            }
//	    	}
//	    	
//			logger.info("  Building property map for all Sage images in the flylight_flip image family");
//			
//	    	ResultSetIterator iterator = null;
//	    	
//	    	try {
//	    		iterator = sage.getImagesByFamily("flylight_flip");
//	    		while (iterator.hasNext()) {
//	        		Map<String,Object> row = iterator.next();
//					associateImageProperties(conn, row);
//	        	}
//	    	}
//	    	catch (RuntimeException e) {
//	    		if (e.getCause() instanceof SQLException) {
//	    			throw new DaoException(e);
//	    		}
//	    		throw e;
//	    	}
//	        finally {
//	        	if (iterator!=null) iterator.close();
//	        }
//    	}
//    	catch (ComputeException e) {
//    		throw new DaoException(e);
//    	}
//    	finally {
//	    	try {
//	            if (conn!=null) conn.close(); 
//	    	}
//	    	catch (SQLException e) {
//                logger.warn("Error closing JDBC connection",e);
//	    	}
//    	}
    }
    
    private void associateImageProperties(Connection conn, Map<String,Object> imageProps) throws DaoException {
//    	String imagePath = (String)imageProps.get("path");
//    	String[] path = imagePath.split("/"); // take just the filename
//    	String filename = path[path.length-1];
//		for(Long imageId : annotationDAO.getImageIdsWithName(conn, null, filename)) {
//			putValue(SAGE_IMAGEPROP_MAP, imageId, imageProps);
//		}
    }
    
    public Object getValue(String cacheName, Object key) {
		return getValue(getCache(cacheName), key);
    }

    public void putValue(String cacheName, Object key, Object value) {
    	putValue(getCache(cacheName), key, value);
    }

    private Cache getCache(String cacheName) {
    	if (caches.containsKey(cacheName)) {
    		return caches.get(cacheName);
    	}
    	else {
    		throw new IllegalArgumentException("Unknown cache: "+cacheName);
    	}
    }
    
    private Object getValue(Cache cache, Object key) {
		Element e = cache.get(key);
		return e!=null ? e.getObjectValue() : null;
    }

    private void putValue(Cache cache, Object key, Object value) {
		Element e = new Element(key, value);
		cache.put(e);
    }
}
