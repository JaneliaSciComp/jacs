package org.janelia.it.jacs.compute.access.large;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.solr.AncestorSet;
import org.janelia.it.jacs.compute.access.solr.SimpleAnnotation;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.user_data.User;

/**
 * Large operations which need to be done on disk using EhCache, lest we run out of memory.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LargeOperations {
	
    private static final Logger logger = Logger.getLogger(LargeOperations.class);

	public static final String ANNOTATION_MAP = "annotationMapCache";
	public static final String ANCESTOR_MAP = "ancestorMapCache";
	public static final String NEO4J_MAP = "neo4jMapCache";
	public static final String SAGE_IMAGEPROP_MAP = "sageImagePropCache";
	
	protected static CacheManager manager;
	protected static Cache annotationMapCache;
	protected static Cache ancestorMapCache;
	protected static Cache neo4jMapCache;
	protected static Cache sageImagePropCache;
	
	protected AnnotationDAO annotationDAO;
	
    public LargeOperations(AnnotationDAO annotationDAO) {
    	this.annotationDAO = annotationDAO;
        synchronized (LargeOperations.class) {
	        if (manager==null) {
	        	manager = new CacheManager(getClass().getResource("/ehcache2-jacs.xml"));
	        	annotationMapCache = manager.getCache(ANNOTATION_MAP);
	        	ancestorMapCache = manager.getCache(ANCESTOR_MAP);
	        	neo4jMapCache = manager.getCache(NEO4J_MAP);
	        	sageImagePropCache = manager.getCache(SAGE_IMAGEPROP_MAP);
	        }
        }
    }

    /**
     * Builds a map of entity ids to sets of SimpleAnnotations on disk using EhCache.
     * @throws DaoException
     */
    public void buildAnnotationMap() throws DaoException {

    	logger.info("Building annotation map of all entities and annotations");
    	
    	Connection conn = null;
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	try {
	        conn = annotationDAO.getJdbcConnection();
	        
	        StringBuffer sql = new StringBuffer();
	        sql.append("select a.id, aedt.value, aedk.value, aedv.value, u.user_login ");
	        sql.append("from entity a ");
	        sql.append("left outer join entityData aedt on a.id=aedt.parent_entity_id ");
	        sql.append("left outer join entityData aedk on a.id=aedk.parent_entity_id ");
	        sql.append("left outer join entityData aedv on a.id=aedv.parent_entity_id ");
	        sql.append("left outer join user_accounts u on a.user_id=u.user_id ");
	        sql.append("where a.entity_type_id = ? ");
	        sql.append("and aedt.entity_att_id = ? ");
	        sql.append("and aedk.entity_att_id = ? ");
	        sql.append("and aedv.entity_att_id = ? ");
	        sql.append("order by u.user_login, aedt.value");

	        EntityType annotationType = annotationDAO.getEntityTypeByName(EntityConstants.TYPE_ANNOTATION);
	        EntityAttribute targetAttr = annotationDAO.getEntityAttributeByName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
	        EntityAttribute keyAttr = annotationDAO.getEntityAttributeByName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM);
	        EntityAttribute valueAttr = annotationDAO.getEntityAttributeByName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM);

	        stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        stmt.setFetchSize(Integer.MIN_VALUE);
	        
	        stmt.setLong(1, annotationType.getId());
	        stmt.setLong(2, targetAttr.getId());
	        stmt.setLong(3, keyAttr.getId());
	        stmt.setLong(4, valueAttr.getId());
	        
			rs = stmt.executeQuery();
			logger.info("    Processing results");
			int i = 0;
			while (rs.next()) {
				Long annotationId = rs.getBigDecimal(1).longValue();
				String entityIdStr = rs.getString(2);
				String key = rs.getString(3);
				String value = rs.getString(4);
				String owner = rs.getString(5);
				
				Long entityId = null;
				try {
					entityId = new Long(entityIdStr);
				}
				catch (NumberFormatException e) {
					logger.warn("Cannot parse annotation target id for annotation="+annotationId);
				}
				
				Set<SimpleAnnotation> annots = (Set<SimpleAnnotation>)getValue(annotationMapCache, entityId);
				if (annots == null) {
					annots = new HashSet<SimpleAnnotation>();
				}

				if (entityId == 1679282476884688994L) {
					logger.info("        Found annotation, id:"+annotationId+", owner:"+owner+", target:"+entityId+", key:"+key+" value:"+value);
				}
				
				annots.add(new SimpleAnnotation(key, value, owner));
				putValue(annotationMapCache, entityId, annots);
				i++;
			}
			logger.info("    Processed "+i+" annotations on "+annotationMapCache.getSize()+" targets");
    	}
    	catch (SQLException e) {
    		throw new DaoException(e);
    	}
        finally {
        	try {
        		if (rs!=null) rs.close();
        		if (stmt!=null) stmt.close();
        		if (conn!=null) conn.close();	
        	}
            catch (Exception e) {
        		throw new DaoException(e);
            }
        }
    	
        logger.info("    Done, annotationMap.size="+annotationMapCache.getSize());
    }
    
    /**
     * Builds a map of entity ids to sets of ancestor ids on disk using EhCache.
     * @throws DaoException
     */
    public void buildAncestorMap() throws DaoException {

    	logger.info("Building ancestor map for all entities");
    	
    	Connection conn = null;
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	try {
	        conn = annotationDAO.getJdbcConnection();
	        
	        StringBuffer sql = new StringBuffer();
	        sql.append("select ed.child_entity_id, e.id ");
    		sql.append("from entity e ");
	        sql.append("join entityData ed on e.id=ed.parent_entity_id ");
	        sql.append("where ed.child_entity_id is not null ");
	        sql.append("order by ed.child_entity_id ");
	        
	        Long currChildId = null;
	        AncestorSet ancestorSet = new AncestorSet();
	        	
	        stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        stmt.setFetchSize(Integer.MIN_VALUE);
	    	
			rs = stmt.executeQuery();
	    	logger.info("    Processing results");
			while (rs.next()) {
				Long childId = rs.getBigDecimal(1).longValue();
				Long entityId = rs.getBigDecimal(2).longValue();
				if (currChildId!=null && !childId.equals(currChildId)) {
					putValue(ancestorMapCache, currChildId, ancestorSet);
					ancestorSet = new AncestorSet();
				}
				currChildId = childId;
				ancestorSet.getAncestors().add(entityId);
			}
			
			if (currChildId!=null) {
				putValue(ancestorMapCache, currChildId, ancestorSet);
			}	
			
    	}
    	catch (SQLException e) {
    		throw new DaoException(e);
    	}
        finally {
        	try {
        		if (rs!=null) rs.close();
        		if (stmt!=null) stmt.close();
        		if (conn!=null) conn.close();	
        	}
            catch (Exception e) {
        		throw new DaoException(e);
            }
        }

    	logger.info("    Loaded entity graph, now to find the ancestors...");

    	int i = 0;
    	for(Object entityIdObj : ancestorMapCache.getKeys()) {
    		calculateAncestors((Long)entityIdObj, new HashSet<Long>(), 0);
    		i++;
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
    
    boolean debugAncestors = false;
    private Set<Long> calculateAncestors(Long entityId, Set<Long> visited, int level) {

    	if (level>10000) {
    		throw new IllegalStateException("Something went wrong calculating ancestors");
    	}
    	
    	StringBuffer b = new StringBuffer();
    	for(int i=0; i<level; i++) {
    		b.append("    ");
    	}
    	if (debugAncestors) logger.info(b+""+entityId);
    	
    	AncestorSet ancestorSet = (AncestorSet)getValue(ancestorMapCache, entityId);
    	
    	if (ancestorSet==null) {
    		// Hit a root, it has no ancestors
    		if (debugAncestors) logger.info(b+""+entityId+" is root");
    		return new HashSet<Long>();
    	}
    		
    	if (ancestorSet.isComplete()) {
    		// The work's already been done
    		if (debugAncestors) logger.info(b+""+entityId+" is complete");
    		return ancestorSet.getAncestors();
    	}

    	if (visited.contains(entityId)) {
    		// Loop detected because the set isn't complete but we're hitting the same entity again. Break out of it..
    		ancestorSet.setComplete(true);
    		putValue(ancestorMapCache, entityId, ancestorSet);
    		if (debugAncestors) logger.info(b+""+entityId+" is loop");
    		return ancestorSet.getAncestors();
    	}

    	visited.add(entityId);
    	
    	Set<Long> ancestors = ancestorSet.getAncestors();
    	for(Long parentId : new HashSet<Long>(ancestors)) {
    		ancestors.addAll(calculateAncestors(parentId, visited, level+1));
    	}
    	
    	ancestorSet.setComplete(true);
    	putValue(ancestorMapCache, entityId, ancestorSet);
    	
    	if (debugAncestors) logger.info(b+""+entityId+" has "+ancestorSet.getAncestors().size()+" ancestors");
    	return ancestorSet.getAncestors();
    }
    
    /**
     * Builds a map of image paths to Sage properties.
     * @throws DaoException
     */
    public void buildSageImagePropMap() throws DaoException {

    	User systemUser = annotationDAO.getUserByName("system");
    	
    	logger.info("Building property map for all Sage images");
    	SageDAO sage = new SageDAO(logger);
    	ResultSetIterator iterator = sage.getFlylightImageProperties();
    	
    	try {
    		String currImagePath = null;
    		Map<String,String> currImageProps = null;
    		
        	while (iterator.hasNext()) {
        		Object[] row = iterator.next();
				
				if (row.length<3) {
					throw new DaoException("Unexpected number of values returned by getFlylightImageProperties. Got:"+row.length+" Expected:7");
				}
				
				String imagePath = (String)row[0];
				String key = (String)row[1];
				String value = (String)row[2];
				
				if (!imagePath.equals(currImagePath)) {
					if (currImagePath!=null) {
						associateImageProperties(systemUser.getUserId(), currImagePath, currImageProps);
					}
					currImageProps = new HashMap<String,String>();
					currImagePath = imagePath;
				}
				
				currImageProps.put(key, value);
        	}	
        	
        	associateImageProperties(systemUser.getUserId(), currImagePath, currImageProps);
    	}
    	catch (RuntimeException e) {
    		if (e.getCause() instanceof SQLException) {
    			throw new DaoException(e);
    		}
    		throw e;
    	}
        finally {
        	if (iterator!=null) iterator.close();
        }
    }
    
    private void associateImageProperties(Long systemUserId, String imagePath, Map<String,String> imageProps) throws DaoException {
    	String[] path = imagePath.split("/"); // take just the filename
    	String filename = path[path.length-1];
    	String slideCode = imageProps.get("slide_code");
    	List<Long> imageIds = annotationDAO.getImageIdsWithPath(systemUserId, filename, slideCode);
		for(Long imageId : imageIds) {
			putValue(sageImagePropCache, imageId, imageProps);
		}
    }
    
    public Object getValue(String cacheName, Object key) {
		return getValue(getCache(cacheName), key);
    }

    public void putValue(String cacheName, Object key, Object value) {
    	putValue(getCache(cacheName), key, value);
    }

    private Cache getCache(String cacheName) {
    	if (ANCESTOR_MAP.equals(cacheName)) {
    		return ancestorMapCache;
    	}
    	else if (ANNOTATION_MAP.equals(cacheName)) {
    		return annotationMapCache;
    	}
    	else if (NEO4J_MAP.equals(cacheName)) {
    		return neo4jMapCache;
    	}
    	else if (SAGE_IMAGEPROP_MAP.equals(cacheName)) {
    		return sageImagePropCache;
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
