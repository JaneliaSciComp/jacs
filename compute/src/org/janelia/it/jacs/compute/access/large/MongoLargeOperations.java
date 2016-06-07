package org.janelia.it.jacs.compute.access.large;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.mongodb.MongoDbImport;
import org.janelia.it.jacs.compute.access.solr.AncestorSet;
import org.janelia.it.jacs.compute.access.solr.SimpleAnnotation;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
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
	
    private static final Logger log = Logger.getLogger(MongoLargeOperations.class);
    
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
        this();
        this.dao = dao;
    }
    
    public MongoLargeOperations() {
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

    	log.info("Building ancestor map for all tree nodes");
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

    	log.info("    Loaded tree node graph, now to find the ancestors...");

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
	

    /**
     * Builds a map of image paths to Sage properties.
     * @throws DaoException
     */
    public void buildSageImagePropMap() throws DaoException {

        SageDAO sage = new SageDAO(log);

        try {
            log.info("Building property map for all lines");

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

            log.info("Retrieved properties for "+lineMap.size()+" lines");

            log.info("Building property map for all SAGE images");

            Set<String> dataSetNames = new LinkedHashSet<>();
            dataSetNames.add(MongoDbImport.SCREEN_DEFAULT_DATA_SET_GAL4);
            dataSetNames.add(MongoDbImport.SCREEN_DEFAULT_DATA_SET_LEXA);
            for(Entity dataSet : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
                String dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
                dataSetNames.add(dataSetIdentifier);
            }

            int i = 0;
            for(String dataSetIdentifier : dataSetNames) {

                log.info("  Building property map for all SAGE images in Data Set '"+dataSetIdentifier+"'");

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

                        associateImageProperties(allProps);
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

            log.info("Retrieved properties for "+i+" images");

        }
        catch (ComputeException e) {
            throw new DaoException(e);
        }
    }

    protected void associateImageProperties(Map<String,Object> imageProps) throws DaoException {
        String name = imageProps.get("image_query_name").toString();
        name = name.substring(name.lastIndexOf('/')+1); // Get just the filename
        name = ArchiveUtils.getDecompressedFilepath(name); // Strip bz2 for lookup purposes 
        putValue(SAGE_IMAGEPROP_MAP, name, imageProps);
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
