package org.janelia.it.jacs.compute.access.mongodb;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.compute.access.large.MongoLargeOperations;
import org.janelia.it.jacs.compute.access.solr.AncestorSet;
import org.janelia.it.jacs.compute.access.solr.SimpleAnnotation;
import org.janelia.it.jacs.compute.access.solr.SolrDAO;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchTraversal;
import org.janelia.it.jacs.model.domain.support.SearchType;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.janelia.it.jacs.shared.solr.SolrDocTypeEnum;
import org.jongo.QueryModifier;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.Bytes;
import com.mongodb.DBCursor;

/**
 * A connector for loading the full text Solr index from the data in the Mongo Database. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrConnector extends SolrDAO {

    private static final Logger log = Logger.getLogger(SolrConnector.class);

    private static String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
    private static String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");
    private static String MONGO_USERNAME = SystemConfigurationProperties.getString("MongoDB.Username");
    private static String MONGO_PASSWORD = SystemConfigurationProperties.getString("MongoDB.Password");
    
    protected static final String JANELIA_MODEL_PACKAGE = "org.janelia.it.jacs.model.domain";
    protected static final int SOLR_LOADER_BATCH_SIZE = 5000;
    protected static final int SOLR_LOADER_COMMIT_SIZE = 10000;
    
    private DomainDAO dao;
    
    // Caches
    Map<Class<?>, List<Field>> classFields = new HashMap<Class<?>,List<Field>>();
    
    // Current indexing context
    private Set<SimpleAnnotation> annotations = new HashSet<SimpleAnnotation>();
    private Multimap<String,String> fullTextStrings = HashMultimap.<String,String>create();
    
    public SolrConnector() throws UnknownHostException {
    	super(log, true, true);
		this.dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
    }
    
	public void indexAllDocuments() throws DaoException {
		
        log.trace("indexAllDocuments()");
    	
    	if (!useBuildCore || !streamingUpdates) {
    		throw new IllegalStateException("indexAllEntities called on SolrConnector which has useBuildCore=false or streamingUpdates=false");
    	}
    	
    	log.info("Building disk-based entity maps");
    	this.largeOp = new MongoLargeOperations(dao, this);
    	largeOp.buildAncestorMap();
    	largeOp.buildAnnotationMap();

		Reflections reflections = new Reflections(JANELIA_MODEL_PACKAGE);
		Set<Class<?>> searchClasses = reflections.getTypesAnnotatedWith(SearchType.class);
    	List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        int total = 0;
        
		for (Class<?> clazz : searchClasses) {

            long start = System.currentTimeMillis();
            
	    	log.info("Getting objects of type "+clazz.getName());
	    	
			String collectionName = DomainUtils.getCollectionName(clazz);
			Set<Field> fields = ReflectionUtils.getAllFields(clazz, ReflectionUtils.withAnnotation(SearchAttribute.class));
			Set<Method> methods = ReflectionUtils.getAllMethods(clazz, ReflectionUtils.withAnnotation(SearchAttribute.class));
			
			Iterator<?> iterator = dao.getCollectionByName(collectionName).find("{$or:[{class:{$exists:0}},{class:#}]}",clazz.getName()).with(new QueryModifier() {
                public void modify(DBCursor cursor) {
                    cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
                }
	        }).as(clazz).iterator();

            int i = 0;
	    	log.info("    Processing results");
			while(iterator.hasNext()) {
				DomainObject domainObject = (DomainObject)iterator.next();
				
				if (i>0) {
	            	if (i%SOLR_LOADER_BATCH_SIZE==0) {
	            		log.info("    Adding "+docs.size()+" docs (i="+i+")");
	            		index(docs);
	            		docs.clear();
	            	}
            		if (i%SOLR_LOADER_COMMIT_SIZE==0) {
            	    	log.info("    Committing SOLR index");
            			commit();
            		}
				}
				
	        	AncestorSet ancestorSet = (AncestorSet)largeOp.getValue(LargeOperations.ANCESTOR_MAP, domainObject.getId());
	        	Set<Long> ancestors = ancestorSet==null ? null : ancestorSet.getAncestors(); 
	        	docs.add(createDocument(null, domainObject, fields, methods, ancestors));

				i++;
				total++;
			}

	        if (!docs.isEmpty()) {
	            log.info("    Adding "+docs.size()+" docs (i="+i+")");
	            index(docs);
	            docs.clear();
	        }

            log.info("  Indexing '"+collectionName+"' took "+(System.currentTimeMillis()-start)+" ms");
		}
    	
        try {
    		commit();
    		optimize();
        	log.info("Completed indexing "+total+" objects");
            swapBuildCore();
            log.info("Build core swapped to main core. The new index is now live.");
        }
        catch (Exception e) {
        	throw new DaoException(e);
        }
	}
	
	private SolrInputDocument createDocument(SolrDocument existingDoc, DomainObject domainObject, Set<Field> fields, Set<Method> methods, Set<Long> ancestorIds) throws DaoException {

    	SolrInputDocument doc = existingDoc==null ? new SolrInputDocument() : ClientUtils.toSolrInputDocument(existingDoc);
    	doc.setField("doc_type", SolrDocTypeEnum.DOCUMENT.toString(), 1.0f);
    	
		Map<String,Object> attrs = new HashMap<String,Object>();
		for(Field field : fields) {
			SearchAttribute searchAttributeAnnot = field.getAnnotation(SearchAttribute.class);
			try {
				Object value = ReflectionHelper.getFieldValue(domainObject, field.getName());	
				if (value != null) {
					attrs.put(searchAttributeAnnot.key(), value);
				}
			}
			catch (NoSuchFieldException e) {
				throw new DaoException("No such field "+field.getName()+" on object "+domainObject,e);
			}
		}

        for(Method method : methods) {
            SearchAttribute searchAttributeAnnot = method.getAnnotation(SearchAttribute.class);
            try {
                Object value = method.invoke(domainObject);  
                if (value != null) {
                    attrs.put(searchAttributeAnnot.key(), value);
                } 
            }
            catch (InvocationTargetException | IllegalAccessException e) {
                throw new DaoException("Problem executing "+method.getName()+" on object "+domainObject,e);
            }
        }

		if (existingDoc!=null) {
			// This is slightly flawed in that attributes are not removed from the index if they are removed from 
			// the entity. However, in that case we can tolerate a 1-day reindexing delay.
			for(String key : attrs.keySet()) {
    			doc.removeField(key);
	    	}
		}
		
    	for(String key : attrs.keySet()) {
    		Object value = attrs.get(key);
    		if (value!=null) {
    			doc.addField(key, value, 1.0f);	
    		}
    	}

    	if (ancestorIds != null) {
    		if (existingDoc!=null) {
    			doc.removeField("ancestor_ids");
    		}
    		doc.addField("ancestor_ids", ancestorIds, 0.2f);
    	}
    	
    	// Clear state before calling findStrings to populate it
    	annotations.clear();
		fullTextStrings.clear();
		
		findStrings(new HashSet<String>(), domainObject, domainObject, true, "  ");
		Collection<String> strings = new HashSet<>(fullTextStrings.values());
		doc.setField("fulltext_mt", strings, 0.8f);
                
        for(SimpleAnnotation annotation : annotations) {        
            for(String subject : annotation.getSubjectsCsv().split(",")) {
                subject = subject.contains(":") ? subject.split(":")[1] : subject;
                doc.addField(subject+"_annotations", annotation.getTag(), 1.0f);
            }
        }
		
		return doc;
	}
		
	/**
	 * Find all the searchable strings in the given domain object graph. 
	 * @param visited set of object keys (class#id) that we have already visited, so that we don't get stuck in an infinite loop
	 * @param object the root object of the object graph to traverse
	 * @param ignoreSearchAttrs should we ignore attributes of the root object which are marked with the @SearchAttribute annotation?
	 */
	private void findStrings(Set<String> visited, DomainObject rootObject, Object object, boolean ignoreSearchAttrs, String indent) {
		
		if (object==null) return;
		Class<?> clazz = object.getClass();
				
        log.debug(indent+"indexing "+clazz.getName());
        
        if (object instanceof DomainObject) {
            DomainObject domainObject = (DomainObject)object;
            Long id = domainObject.getId();
            String key = clazz.getName()+"#"+id;
            if (visited.contains(key)) {
                return;
            }
            visited.add(key);
            Set<SimpleAnnotation> objectAnnotations = (Set<SimpleAnnotation>)largeOp.getValue(LargeOperations.ANNOTATION_MAP, id);
            if (objectAnnotations!=null) {
                annotations.addAll(objectAnnotations);
            }
        }
		
		List<Field> fields = classFields.get(clazz);
		if (fields == null) {
		    fields = new ArrayList<>(ReflectionUtils.getAllFields(clazz));
		    classFields.put(clazz, fields);
		}
		
		for(Field field : fields) {
			SearchAttribute searchAttributeAnnot = field.getAnnotation(SearchAttribute.class);
			if (ignoreSearchAttrs && searchAttributeAnnot!=null) continue;
			try {
				findStrings(visited, rootObject, object, field, indent+"  ");
			}
			catch (Exception e) {
				log.error(indent+"Error finding strings for field "+field.getName(),e);
			}
		}
	}
	
	/**
	 * Find strings in the specified field of the given object. 
	 * @param visited
	 * @param rootObject
	 * @param object
	 * @param field
	 * @param indent
	 * @throws Exception
	 */
	private void findStrings(Set<String> visited, DomainObject rootObject, Object object, Field field, String indent) throws Exception {

		if (object==null) return;

        if (!isTraversable(field, rootObject)) {
            return;
        }
        
        if (object instanceof String) {
            addFullTextString(field.getName(), (String)object);
            return;
        }
        
        log.debug(indent+"indexing "+object+"."+field.getName());

        Object childObj = ReflectionHelper.getFieldValue(object, field);
        if (childObj==null) return;
        Class<?> childClass = childObj.getClass();
		
		if (childObj instanceof String) {
            addFullTextString(field.getName(), childObj.toString());
		}
		else if (childObj instanceof Map) {
            Map map = (Map)childObj;
            findStrings(visited, rootObject, object, field, map.values(), indent+"  ");
		}
		else if (childObj instanceof List) {
			List list = (List)childObj;
			findStrings(visited, rootObject, object, field, list, indent+"  ");
		}
		else if (childObj instanceof Set) {
			Set set = (Set)childObj;
            findStrings(visited, rootObject, object, field, set, indent+"  ");
		}
		else if (childObj instanceof Reference) {
            Reference ref = (Reference) childObj;
            // Don't fetch objects which we've already visited
            String key = ref.getTargetClassName()+"#"+ref.getTargetId();
            if (visited.contains(key)) {
                return;
            }
            log.trace(indent+"fetching reference "+ref);
            DomainObject obj = dao.getDomainObject(null, ref);
            findStrings(visited, rootObject, obj, false, indent + "  ");
        } 
		else if (childObj instanceof ReverseReference) {
            ReverseReference ref = (ReverseReference) childObj;
            log.trace(indent+"fetching reverse reference"+ref);
            List<DomainObject> objs = dao.getDomainObjects(null, ref);
            for (DomainObject obj : objs) {
                findStrings(visited, rootObject, obj, false, indent + "  ");
            }
		}
		else if (childClass.getName().startsWith(JANELIA_MODEL_PACKAGE)) {	
			findStrings(visited, rootObject, childObj, false, indent+"  ");
		}
        else if (childClass.getName().startsWith("java.")) {
            // Ignore other Java objects like Dates and Booleans 
        }
		else {
			log.warn("Encountered unknown class: "+childClass.getName());
		}
	}
	
	private boolean isTraversable(Field field, Object rootObject) {
        SearchTraversal searchTraversal = field.getAnnotation(SearchTraversal.class);
        
        // Default to traversing every unannotated field
        if (searchTraversal==null) return true;
        
        for(Class<?> allowedClass : searchTraversal.value()) {
            if (allowedClass.equals(rootObject.getClass())) {
                return true;
            }
        }
        // Annotation exists, but this field is not traversable from the given root object
        return false;
	}

	/**
	 * Find strings in the field of the given object, which reduces to the given collection. 
	 * @param visited
	 * @param rootObject
	 * @param object
	 * @param field
	 * @param collection
	 * @param indent
	 * @throws Exception
	 */
    private void findStrings(Set<String> visited, DomainObject rootObject, Object object, Field field, Collection<?> collection, String indent) throws Exception {
        
        log.debug(indent+"indexing collection "+object+"."+field.getName());
        
        for(Object collectionObject : collection) {
            if (collectionObject==null) {
                continue;
            }
            Class<?> clazz = collectionObject.getClass();
            if (clazz.getName().startsWith(JANELIA_MODEL_PACKAGE)) {
                findStrings(visited, rootObject, collectionObject, false, indent+"  ");
            }
            else if (collectionObject instanceof String) {
                findStrings(visited, rootObject, collectionObject, field, indent+"  ");   
            }
            else if (collectionObject instanceof Number) {
                // Ignore
            }
            else {
                log.warn("Encountered collection with objects of type "+clazz.getName());
            }
        }   
    }
	
	private void addFullTextString(String key, String value) {
		if (key==null || value==null) return;

		if ("files".equals(key)) {
			// Don't index Neuron Fragment files
			if (value.startsWith("neuronSeparatorPipeline")|| value.contains("maskChan")) return;
		}
		else if ("name".equals(key)) {
			// Don't index Neuron Fragment names
			if (value.startsWith("Neuron Fragment ")) return;
		}
		
		fullTextStrings.put(key, value);
	}

}