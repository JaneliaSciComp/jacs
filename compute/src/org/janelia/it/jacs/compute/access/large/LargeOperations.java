package org.janelia.it.jacs.compute.access.large;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Large, memory-bound operations which need to be done on disk using EhCache, lest we run out of heap space.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LargeOperations {
	
    private static final Logger logger = Logger.getLogger(LargeOperations.class);

	public static final String ANNOTATION_MAP = "annotationMapCache";
	public static final String ANCESTOR_MAP = "ancestorMapCache";
	public static final String NEO4J_MAP = "neo4jMapCache";
	public static final String SAGE_IMAGEPROP_MAP = "sageImagePropCache";
	public static final String SCREEN_SCORE_MAP = "screenScoreCache";
	
	protected static CacheManager manager;
	// Local cache for faster access to caches, without all the unnecessary checking that the CacheManager does
	protected static Map<String,Cache> caches = new HashMap<>();
	
	protected AnnotationDAO annotationDAO;

    public LargeOperations(AnnotationDAO annotationDAO) {
    	this();
    	this.annotationDAO = annotationDAO;
    }
    
    public LargeOperations() {
        synchronized (LargeOperations.class) {
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
    	Connection conn = null;
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	try {
	        conn = annotationDAO.getJdbcConnection();
	        
            StringBuilder sql = new StringBuilder();
	        sql.append("select a.id, a.name, aedt.value target_id, a.owner_key, group_concat(eap.subject_key separator ',') permitted ");
	        sql.append("from entity a ");
	        sql.append("left outer join entityData aedt on a.id=aedt.parent_entity_id and aedt.entity_att=? ");
	        sql.append("left outer join entity_actor_permission eap on a.id=eap.entity_id ");
	        sql.append("where a.entity_type = ? ");
	        sql.append("group by a.id ");

	        stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        stmt.setFetchSize(Integer.MIN_VALUE);

            stmt.setString(1, EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
	        stmt.setString(2, EntityConstants.TYPE_ANNOTATION);
	        
			rs = stmt.executeQuery();
			logger.info("    Processing results");
			int i = 0;
			while (rs.next()) {
				Long annotationId = rs.getBigDecimal(1).longValue();
				String annotationName = rs.getString(2);
				String entityIdStr = rs.getString(3);
				String owner = rs.getString(4);
				String permittedCsv = rs.getString(5);
				
				Long entityId = null;
				try {
					entityId = new Long(entityIdStr);
				}
				catch (NumberFormatException e) {
					logger.warn("Cannot parse annotation target id for annotation="+annotationId);
					continue;
				}
				
                @SuppressWarnings("unchecked")
                Set<SimpleAnnotation> annots = (Set<SimpleAnnotation>)getValue(annotationMapCache, entityId);
				if (annots == null) {
					annots = new HashSet<>();
				}
				
				String subjectsCsv = owner;
				if (!StringUtils.isEmpty(permittedCsv)) {
				    subjectsCsv += ","+permittedCsv;
				}
								
				annots.add(new SimpleAnnotation(annotationName, subjectsCsv));
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
                logger.warn("Error closing JDBC connection",e);
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
    	
    	Cache ancestorMapCache = caches.get(ANCESTOR_MAP);
    	Connection conn = null;
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	try {
	        conn = annotationDAO.getJdbcConnection();
	        
            StringBuilder sql = new StringBuilder();
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
                logger.warn("Error closing JDBC connection",e);
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

    private static final HashSet<Long> EMPTY_SET = new HashSet<>();
//    boolean debugAncestors = false;
    protected Set<Long> calculateAncestors(Long entityId, Set<Long> visited, int level) {

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
    	for(Long parentId : new HashSet<>(ancestors)) {
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
    	
    	SageDAO sage = new SageDAO(logger);
    	Connection conn = null;
    	
    	try {
    		conn = annotationDAO.getJdbcConnection();

            logger.info("Building property map for all lines");
            
            ResultSetIterator lineIterator = null;
            Map<String, Map<String,Object>> lineMap = new HashMap<>();
            try {
                lineIterator = sage.getAllLineProperties();
                while (lineIterator.hasNext()) {
                    Map<String,Object> lineProperties = lineIterator.next();
                    lineMap.put((String)lineProperties.get(SageDAO.LINE_PROP_LINE_TERM),lineProperties);
                }
            }
            catch (RuntimeException e) {
                if (e.getCause() instanceof SQLException) {
                    throw new DaoException(e);
                }
                throw e;
            }
            finally {
                if (lineIterator!=null) lineIterator.close();
            }
            
            logger.info("Retrieved properties for "+lineMap.size()+" lines");
            
            logger.info("Building property map for all SAGE images");

            int i = 0;
	    	for(Entity dataSet : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
	    		
	    		String dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
	    		logger.info("  Building property map for all SAGE images in Data Set '"+dataSetIdentifier+"'");

	        	ResultSetIterator iterator = null;
	        	
	        	try {
                    iterator = sage.getAllImagePropertiesByDataSet(dataSetIdentifier);
	        		while (iterator.hasNext()) {
						Map<String,Object> imageProperties = iterator.next();
	            		
	            		Map<String,Object> allProps = new HashMap<>(imageProperties);
                        
                        String line = (String)imageProperties.get(SageDAO.IMAGE_PROP_LINE_TERM);
                        if (line!=null) {
                            Map<String,Object> lineProperties = lineMap.get(line);
                            if (lineProperties!=null) {
                                allProps.putAll(lineProperties);
                            }
                        }
                        
						associateImageProperties(conn, allProps);
						i++;
	            	}
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
	    	
            logger.info("Retrieved properties for "+i+" images");
	    	
    	}
    	catch (ComputeException e) {
    		throw new DaoException(e);
    	}
    	finally {
	    	try {
	            if (conn!=null) conn.close(); 
	    	}
	    	catch (SQLException e) {
                logger.warn("Error closing JDBC connection",e);
	    	}
    	}
    }
    
    private void associateImageProperties(Connection conn, Map<String,Object> imageProps) throws DaoException {
    	String imagePath = (String)imageProps.get(SageDAO.IMAGE_PROP_PATH);
    	if (imagePath==null) {
    	    logger.error("Null image property path encountered at imageProps="+imageProps);
    	    throw new IllegalStateException("Null image property path");
    	}
    	String[] path = imagePath.split("/"); // take just the filename
    	String filename = path[path.length-1];
		for(Long imageId : annotationDAO.getImageIdsWithName(conn, null, filename)) {
			putValue(SAGE_IMAGEPROP_MAP, imageId, imageProps);
		}
    }
    
    public Object getValue(String cacheName, Object key) {
		return getValue(getCache(cacheName), key);
    }

    public void putValue(String cacheName, Object key, Object value) {
    	putValue(getCache(cacheName), key, value);
    }

    protected Cache getCache(String cacheName) {
    	if (caches.containsKey(cacheName)) {
    		return caches.get(cacheName);
    	}
    	else {
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
