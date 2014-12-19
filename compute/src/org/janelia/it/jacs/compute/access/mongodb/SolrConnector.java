package org.janelia.it.jacs.compute.access.mongodb;

import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchTraversal;
import org.janelia.it.jacs.model.domain.support.SearchType;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.janelia.it.jacs.shared.solr.SageTerm;
import org.janelia.it.jacs.shared.solr.SolrDocTypeEnum;
import org.janelia.it.jacs.shared.solr.SolrUtils;
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
    
    // Current indexing context
    private Class<?> currContext;
    private Set<SimpleAnnotation> annotations = new HashSet<SimpleAnnotation>();
    private Multimap<String,String> fullTextStrings = HashMultimap.<String,String>create();
    
    public SolrConnector() throws UnknownHostException {
    	super(log, true, true);
		this.dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
    }
    
	public void indexAllDocuments(Map<String, SageTerm> sageVocab) throws DaoException {
		
		if (log.isTraceEnabled()) {
            log.trace("indexAllDocuments(sageVocab.size="+sageVocab.size()+")");
        }
    	
    	if (!useBuildCore || !streamingUpdates) {
    		throw new IllegalStateException("indexAllEntities called on SolrConnector which has useBuildCore=false or streamingUpdates=false");
    	}
    	
    	this.sageVocab = sageVocab;
    	this.usedSageVocab = new HashSet<SageTerm>();

    	log.info("Building disk-based entity maps");
    	this.largeOp = new MongoLargeOperations(dao);
    	largeOp.buildSageImagePropMap();
    	largeOp.buildAncestorMap();
    	largeOp.buildAnnotationMap();

		Reflections reflections = new Reflections(JANELIA_MODEL_PACKAGE);
		Set<Class<?>> searchClasses = reflections.getTypesAnnotatedWith(SearchType.class);
    	List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        int total = 0;
        
		for (Class<?> clazz : searchClasses) {

			this.currContext = clazz;
	    	log.info("Getting objects of type "+clazz.getName());
	    	
			String type = dao.getCollectionName(clazz);
			Set<Field> fields = ReflectionUtils.getAllFields(clazz, ReflectionUtils.withAnnotation(SearchAttribute.class));

			Iterator<?> iterator = dao.getCollectionByName(type).find("{$or:[{class:{$exists:0}},{class:#}]}",clazz.getName()).with(new QueryModifier() {
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
	        	Map<String,Object> sageProps = (Map<String,Object>)largeOp.getValue(LargeOperations.SAGE_IMAGEPROP_MAP, domainObject.getId());
	        	docs.add(createDocument(null, domainObject, fields, ancestors, sageProps));

				i++;
				total++;
			}

	        if (!docs.isEmpty()) {
	            log.info("    Adding "+docs.size()+" docs (i="+i+")");
	            index(docs);
	            docs.clear();
	        }
		}
    	
        try {
        	log.info("Indexing Sage vocabularies");
        	index(createSageDocs(usedSageVocab));
        	
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
	
	private SolrInputDocument createDocument(SolrDocument existingDoc, DomainObject domainObject, Set<Field> fields, Set<Long> ancestorIds, Map<String,Object> sageProps) throws DaoException {

    	SolrInputDocument doc = existingDoc==null ? new SolrInputDocument() : ClientUtils.toSolrInputDocument(existingDoc);

		Class<?> clazz = domainObject.getClass();
		SearchType searchTypeAnnot = clazz.getAnnotation(SearchType.class);
		
    	doc.setField("doc_type", SolrDocTypeEnum.DOCUMENT.toString(), 1.0f);
    	doc.setField("entity_type", searchTypeAnnot.key(), 1.0f);

//		log.info("indexing "+searchTypeAnnot.label()+" "+domainObject.getName());
    	
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

    	if (sageVocab!=null && sageProps!=null) {
    		if (existingDoc!=null) {
        		for(String key : sageProps.keySet()) {
    				SageTerm sageTerm = sageVocab.get(key);
    				if (sageTerm==null) {
    					log.warn("Unrecognized SAGE term: "+key);
    					continue;
    				}
    				doc.removeField(SolrUtils.getSageFieldName(sageTerm));
        		}
    		}
    		for(String key : sageProps.keySet()) {
    			Object value = sageProps.get(key);
    			if (value != null) {
    				SageTerm sageTerm = sageVocab.get(key);
    				if (sageTerm==null) {
    					log.warn("Unrecognized SAGE term: "+key);
    					continue;
    				}
    				
    				// Keep track of the terms we use, so that they can be indexed as well
    				if (!usedSageVocab.contains(sageTerm)) usedSageVocab.add(sageTerm);
    				String fieldName = SolrUtils.getSageFieldName(sageTerm);
    				doc.addField(fieldName, value, 0.9f);
    			}
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
		for(String key : fullTextStrings.keySet()) {
			// Need to create new ArrayList to avoid ConcurrentModificationException when the Solr thread tries to read it
			doc.setField(key+"_d_txt", new ArrayList<String>(fullTextStrings.get(key)), 1.0f);
		}
                
        for(SimpleAnnotation annotation : annotations) {        
            doc.addField(annotation.getOwner()+"_annotations", annotation.getTag(), 1.0f);
        }
		
		return doc;
	}
	
	/**
	 * Find all the searchable strings in the given domain object graph. 
	 * @param visited set of object keys (class#id) that we have already visited, so that we don't get stuck in an infinite loop
	 * @param object the root object of the object graph to traverse
	 * @param ignoreSearchAttrs should we ignore attributes of the root object which are marked with the @SearchAttribute annotation?
	 */
	private void findStrings(Set<String> visited, Object rootObject, Object object, boolean ignoreSearchAttrs, String indent) {
		
		if (object==null) return;
		Class<?> clazz = object.getClass();
				
        log.info(indent+"indexing "+clazz.getName());
        
		try {
		    Long id = (Long)org.janelia.it.jacs.shared.utils.ReflectionUtils.get(object, "id");
            String key = clazz.getName()+"#"+id;
            if (visited.contains(key)) return;
            visited.add(key);
            
            // It has an id and it's a domain object, then we must add its annotations
            if (DomainObject.class.isAssignableFrom(clazz)) {
                log.trace(indent+"adding annotations for "+clazz+"#"+id);
                Set<SimpleAnnotation> objectAnnotations = (Set<SimpleAnnotation>)largeOp.getValue(LargeOperations.ANNOTATION_MAP, id);
                if (objectAnnotations!=null) {
                    annotations.addAll(objectAnnotations);
                }
            }
		}
		catch (NoSuchMethodException e) {
		    // Not everything has an id. In cases where an object does not have an id, we don't need to worry 
		    // about having visited before, because it's part of an object that does have an id. 
		}
		catch (Exception e) {
		    log.error("Error checking visited status",e);
		}
		
		for(Field field : ReflectionUtils.getAllFields(clazz)) {
			SearchAttribute searchAttributeAnnot = field.getAnnotation(SearchAttribute.class);
			if (ignoreSearchAttrs && searchAttributeAnnot!=null) continue;
			try {
				findStrings(visited, rootObject, object, field, indent);
			}
			catch (Exception e) {
				log.error("Error finding strings",e);
			}
		}
	}
	
	private void findStrings(Set<String> visited, Object rootObject, Object object, Field field, String indent) throws Exception {

		if (object==null) return;
		Object childObj = null;
        Class<?> childClass = null;
        
		if (field==null) {
		    childObj = object;
		    childClass = object.getClass();
	        log.info(indent+"indexing "+object+" of type "+childClass.getName());
		}
		else {
	        SearchTraversal searchTraversal = field.getAnnotation(SearchTraversal.class);
	        if (searchTraversal!=null) {
	            boolean found = false;
	            for(Class<?> allowedClass : searchTraversal.value()) {
	                if (allowedClass.equals(rootObject.getClass())) {
	                    found = true;
	                    break;
	                }
	            }
	            if (!found) {
	                return;
	            }
	        }
	        childClass = field.getType();
	        log.info(indent+"indexing field "+field.getName()+" from "+object+" of type "+childClass.getName());
		}
		
		if (String.class.isAssignableFrom(childClass)) {
            if (childObj==null) childObj = ReflectionHelper.getFieldValue(object, field);
            if (childObj!=null) {
                addFullTextString(field.getName(), childObj.toString());
            }
		}
		else if (Map.class.isAssignableFrom(childClass)) {
		    if (childObj==null) childObj = ReflectionHelper.getFieldValue(object, field);
            if (childObj!=null) {
                Map map = (Map)childObj;
    			for(Object key : map.keySet()) {
    				Object value = map.get(key);
    				findStrings(visited, rootObject, value, null, indent+"  ");
    			}
			}
		}
		else if (List.class.isAssignableFrom(childClass)) {
		    if (childObj==null) childObj = ReflectionHelper.getFieldValue(object, field);
		    if (childObj!=null) {
    			List list = (List)childObj;
    			for(Object value : list) {
                    findStrings(visited, rootObject, value, null, indent+"  ");
    			}
		    }
		}
		else if (Set.class.isAssignableFrom(childClass)) {
		    if (childObj==null) childObj = ReflectionHelper.getFieldValue(object, field);
		    if (childObj!=null) {
    			Set set = (Set)childObj;
    			for(Object value : set) {
                    findStrings(visited, rootObject, value, null, indent+"  ");
    			}
		    }
		}
		else if (Reference.class.isAssignableFrom(childClass)) {
            if (childObj == null) childObj = ReflectionHelper.getFieldValue(object, field);
            if (childObj!=null) {
                Reference ref = (Reference) childObj;
                DomainObject obj = dao.getDomainObject(null, ref);
                findStrings(visited, rootObject, obj, false, indent + "  ");
            }
        } 
		else if (ReverseReference.class.isAssignableFrom(childClass)) {
            if (childObj == null) childObj = ReflectionHelper.getFieldValue(object, field);
            if (childObj!=null) {
                ReverseReference ref = (ReverseReference) childObj;
                List<DomainObject> objs = dao.getDomainObjects(null, ref);
                for (DomainObject obj : objs) {
                    findStrings(visited, rootObject, obj, false, indent + "  ");
                }
            }
		}
		else if (childClass.getName().startsWith(JANELIA_MODEL_PACKAGE)) {	
		    if (childObj==null) childObj = ReflectionHelper.getFieldValue(object, field);
			findStrings(visited, rootObject, childObj, false, indent+"  ");
		}
        else if (childClass.getName().startsWith("java.")) {
            // Ignore other Java objects like Dates and Booleans 
        }
		else {
			log.warn("Encountered unknown class: "+childClass.getName());
		}
	}
	
	private void addFullTextString(String key, String value) {
		if (key==null || value==null) return;
		
		// Don't index permissions
		if ("readers".equals(key) || "writers".equals(key) || "ownerKey".equals(key)) return;

		// Optimization rules for indexing specific domain classes
		if (currContext==Sample.class) {
			if ("files".equals(key)) {
				// Don't index Neuron Fragment files
				if (value.startsWith("neuronSeparatorPipeline")|| value.contains("maskChan")) return;
			}
			else if ("name".equals(key)) {
				// Don't index Neuron Fragment names
				if (value.startsWith("Neuron Fragment ")) return;
			}
		}
		
		fullTextStrings.put(key, value);
	}

}