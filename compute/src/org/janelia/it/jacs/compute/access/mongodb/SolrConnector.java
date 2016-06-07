package org.janelia.it.jacs.compute.access.mongodb;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.Bytes;
import com.mongodb.DBCursor;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.StreamingUpdateSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.CoreAdminParams;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.large.MongoLargeOperations;
import org.janelia.it.jacs.compute.access.solr.AncestorSet;
import org.janelia.it.jacs.compute.access.solr.SimpleAnnotation;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.support.*;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.janelia.it.jacs.shared.solr.SolrDocTypeEnum;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.jongo.QueryModifier;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * A connector for loading the full text Solr index from the data in the Mongo Database. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SolrConnector {

    private static final Logger log = Logger.getLogger(SolrConnector.class);
    
    protected static final String JANELIA_MODEL_PACKAGE = "org.janelia.it.jacs.model.domain";
	protected static final int MAX_ID_LIST_SIZE = 200;

	protected static final int SOLR_LOADER_BATCH_SIZE = 20000;
	protected static final int SOLR_LOADER_COMMIT_SIZE = 200000;
	protected static final int SOLR_LOADER_QUEUE_SIZE = 100;
	protected static final int SOLR_LOADER_THREAD_COUNT = 2;

	protected static final String SOLR_SERVER_URL = SystemConfigurationProperties.getString("Solr.ServerURL");
	protected static final String SOLR_MAIN_CORE = SystemConfigurationProperties.getString("Solr.MainCore");
	protected static final String SOLR_BUILD_CORE = SystemConfigurationProperties.getString("Solr.BuildCore");

	protected final boolean useBuildCore;
	protected final boolean streamingUpdates;

	protected SolrServer solr;
	protected MongoLargeOperations largeOp;
	protected DomainDAO dao;
    
    // Caches
    Map<Class<?>, List<Field>> classFields = new HashMap<>();
    
    // Current indexing context
    private Set<SimpleAnnotation> annotations = new HashSet<>();
    private Multimap<String,String> fullTextStrings = HashMultimap.<String,String>create();

    public SolrConnector(DomainDAO dao) throws UnknownHostException {
        this(dao, false, false);
    }

    public SolrConnector(DomainDAO dao, boolean useBuildCore, boolean streamingUpdates) {
        this.dao = dao;
        this.useBuildCore = useBuildCore;
		this.streamingUpdates = streamingUpdates;
    }

	protected void init() throws DaoException {
		if (solr==null) {
			try {
				if (streamingUpdates) {
					solr = new StreamingUpdateSolrServer(SOLR_SERVER_URL+(useBuildCore?SOLR_BUILD_CORE:SOLR_MAIN_CORE), SOLR_LOADER_QUEUE_SIZE, SOLR_LOADER_THREAD_COUNT);
				}
				else {
					solr = new CommonsHttpSolrServer(SOLR_SERVER_URL+(useBuildCore?SOLR_BUILD_CORE:SOLR_MAIN_CORE));
				}
				solr.ping();
			}
			catch (MalformedURLException e) {
				throw new RuntimeException("Illegal Solr.ServerURL value in system properties: "+SOLR_SERVER_URL);
			}
			catch (IOException e) {
				throw new DaoException("Problem pinging SOLR at: "+SOLR_SERVER_URL);
			}
			catch (SolrServerException e) {
				throw new DaoException("Problem pinging SOLR at: "+SOLR_SERVER_URL);
			}
			this.largeOp = new MongoLargeOperations(dao);
		}
	}
    
	public void indexAllDocuments() throws DaoException {
		
        log.trace("indexAllDocuments()");
    	
    	if (!useBuildCore || !streamingUpdates) {
    		throw new IllegalStateException("indexAllEntities called on SolrConnector which has useBuildCore=false or streamingUpdates=false");
    	}
    	
    	log.info("Building disk-based entity maps");
    	this.largeOp = new MongoLargeOperations(dao);
    	largeOp.buildAncestorMap();
    	largeOp.buildAnnotationMap();

		Reflections reflections = new Reflections(JANELIA_MODEL_PACKAGE);
		Set<Class<?>> searchClasses = reflections.getTypesAnnotatedWith(SearchType.class);
    	List<SolrInputDocument> docs = new ArrayList<>();
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
				
	        	AncestorSet ancestorSet = (AncestorSet)largeOp.getValue(MongoLargeOperations.ANCESTOR_MAP, domainObject.getId());
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
    	doc.setField("class", domainObject.getClass().getName(), 1.0f);
    	doc.setField("collection", DomainUtils.getCollectionName(domainObject), 1.0f);
    	
		Map<String,Object> attrs = new HashMap<>();
		for(Field field : fields) {
			SearchAttribute searchAttributeAnnot = field.getAnnotation(SearchAttribute.class);
			try {
				Object value = ReflectionHelper.getFieldValue(domainObject, field.getName());	
				if (value != null) {
                    attrs.put(searchAttributeAnnot.key(), value);
                    if (!StringUtils.isEmpty(searchAttributeAnnot.facet())) {
                        attrs.put(searchAttributeAnnot.facet(), value);
                    }
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
                    if (!StringUtils.isEmpty(searchAttributeAnnot.facet())) {
                        attrs.put(searchAttributeAnnot.facet(), value);
                    }
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


	public void removeDocuments(List<Long> domainObjIds) throws DaoException {
        // Get all Solr documents
		log.info ("removeDocuments(domainObj.size="+domainObjIds.size()+")");

		// Get all Solr documents
		try {
			init();
			int currSize = 0;
			StringBuffer sqBuf = new StringBuffer();
			for(Long domainObjId : domainObjIds) {
				if (currSize>=MAX_ID_LIST_SIZE) {
					solr.deleteByQuery(sqBuf.toString());
					sqBuf = new StringBuffer();
					currSize = 0;
				}

				if (sqBuf.length()>0) sqBuf.append(" OR ");
				sqBuf.append("id:"+domainObjId);
				currSize++;
			}

			if (currSize>0) {
				solr.deleteByQuery(sqBuf.toString());
			}
		}
		catch (Exception e) {
			log.error("Error removing documents from SOLR" ,e);
			throw new DaoException("Error removing documents from SOLR",e);
		}

		commit();
	}

	public void updateIndices(List<DomainObject> domainObjects) throws DaoException {
		log.trace("updateIndex(domainObj.size="+domainObjects.size()+")");

		init();
		// Get all Solr documents
		List<Long> domainObjIds = new ArrayList<>();
		for (DomainObject domainObject : domainObjects) {
			domainObjIds.add(domainObject.getId());
		}
		Map<Long,SolrDocument> solrDocMap = search(domainObjIds);

		// Create updated Solr documents
		List<SolrInputDocument> inputDocs = new ArrayList<>();
		for (DomainObject domainObject : domainObjects) {
			SolrInputDocument inputDoc = null;
			SolrDocument existingDoc = solrDocMap.get(domainObject.getId());
			Class clazz = domainObject.getClass();
			Set<Field> fields = ReflectionUtils.getAllFields(clazz, ReflectionUtils.withAnnotation(SearchAttribute.class));
			Set<Method> methods = ReflectionUtils.getAllMethods(clazz, ReflectionUtils.withAnnotation(SearchAttribute.class));

			if (existingDoc!=null) {
				// generate ancestor list
				Set<Long> ancestorIds = new HashSet<Long>();
				TreeNode treeNode = null;
				DomainObject testObj = domainObject;
				int depth = 0;
				while (depth<10 && (treeNode=dao.getParentTreeNodes(null, Reference.createFor(testObj)))!=null) {
					ancestorIds.add(treeNode.getId());
					testObj = treeNode;
					depth++;
				}
				inputDoc = createDocument(existingDoc, domainObject, fields, methods, ancestorIds);
			}
			else {
				inputDoc = createDocument(null, domainObject, fields, methods, null);
			}

			inputDocs.add(inputDoc);
			log.info("Updating index for " + domainObject.getName() + " (id=" + domainObject.getId() + ") ");
		}

		// Index the entire batch
		index(inputDocs);
		commit();
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
			String key = Reference.createFor(domainObject).toString();
            if (visited.contains(key)) {
                return;
            }
            visited.add(key);
            Set<SimpleAnnotation> objectAnnotations = (Set<SimpleAnnotation>)largeOp.getValue(MongoLargeOperations.ANNOTATION_MAP, id);
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

		if (object == null) return;

		if (!isTraversable(field, rootObject)) {
			return;
		}

		if (Modifier.isTransient(field.getModifiers())) {
			return;
		}

		if (object instanceof String) {
			addFullTextString(field.getName(), (String) object);
			return;
		}

		log.debug(indent + "indexing " + object + "." + field.getName());
		Object childObj = ReflectionHelper.getFieldValue(object, field);
		if (childObj==null) return;

        Class<?> childClass = childObj.getClass();

		if (childClass.isEnum()) {
			addFullTextString(field.getName(), childObj.toString());
		}
		else if (childObj instanceof String) {
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
            if (visited.contains(ref.toString())) {
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
        
        log.debug(indent + "indexing collection " + object + "." + field.getName());
        
        for(Object collectionObject : collection) {
            if (collectionObject==null) {
                continue;
            }
            Class<?> clazz = collectionObject.getClass();
            if (clazz.getName().startsWith(JANELIA_MODEL_PACKAGE)) {
				if (collectionObject instanceof Reference) {
					Reference ref = (Reference) collectionObject;
					// Don't fetch objects which we've already visited
					if (visited.contains(ref.toString())) {
						return;
					}
					log.trace(indent+"fetching reference "+ref);
					DomainObject obj = dao.getDomainObject(null, ref);
					findStrings(visited, rootObject, obj, false, indent + "  ");
				}

                findStrings(visited, rootObject, collectionObject, false, indent+"  ");
            }
            else if (collectionObject instanceof String) {
				addFullTextString(field.getName(), collectionObject.toString());
            }
            else if (collectionObject instanceof Number) {
                // Ignore
            }
            else {
                log.warn(indent+"Encountered collection with objects of type "+clazz.getName());
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

	private boolean isTraversable(Field field, Object rootObject) {
		SearchTraversal searchTraversal = field.getAnnotation(SearchTraversal.class);

		// Default to traversing every unannotated field
		if (searchTraversal==null) {
			return true;
		}

		for(Class<?> allowedClass : searchTraversal.value()) {
			if (allowedClass.equals(rootObject.getClass())) {
				return true;
			}
		}
		// Annotation exists, but this field is not traversable from the given root object
		return false;
	}

	protected void index(List<SolrInputDocument> docs) throws DaoException {
		init();
		if (docs==null||docs.isEmpty()) return;
		try {
			solr.add(docs);
		}
		catch (Exception e) {
			throw new DaoException("Error indexing with SOLR",e);
		}
	}

	protected void swapBuildCore() throws Exception {
		CoreAdminRequest car = new CoreAdminRequest();
		car.setCoreName(SOLR_BUILD_CORE);
		car.setOtherCoreName(SOLR_MAIN_CORE);
		car.setAction(CoreAdminParams.CoreAdminAction.SWAP);
		car.process(new CommonsHttpSolrServer(SOLR_SERVER_URL));
	}

//	protected List<SolrInputDocument> createSageDocs(Collection<SageTerm> terms) {
//
//		String dt = SolrDocTypeEnum.SAGE_TERM.toString();
//
//		int id = 0;
//		List<SolrInputDocument> docs = new ArrayList<>();
//		for(SageTerm term : terms) {
//			SolrInputDocument doc = new SolrInputDocument();
//			doc.addField("id", dt+"_"+id, 1.0f);
//			doc.addField("doc_type", dt, 1.0f);
//			doc.addField("name", term.getName(), 1.0f);
//			doc.addField("data_type_t", term.getDataType(), 1.0f);
//			doc.addField("definition_t", term.getDefinition(), 1.0f);
//			doc.addField("display_name_t", term.getDisplayName(), 1.0f);
//			doc.addField("cv_t", term.getCv(), 1.0f);
//			docs.add(doc);
//			id++;
//		}
//		return docs;
//	}

	/**
	 * Commit any outstanding changes to the index.
	 * @throws DaoException
	 */
	public void commit() throws DaoException {
		if (log.isTraceEnabled()) {
			log.trace("commit()");
		}

		init();
		try {
			solr.commit();
		}
		catch (Exception e) {
			throw new DaoException("Error commiting index with SOLR",e);
		}
	}

	/**
	 * Clear the entire index and commit.
	 * @throws DaoException
	 */
	public void clearIndex() throws DaoException {
		if (log.isTraceEnabled()) {
			log.trace("clearIndex()");
		}

		init();
		try {
			log.info("Clearing SOLR index");
			solr.deleteByQuery("*:*");
			solr.commit();
		}
		catch (Exception e) {
			throw new DaoException("Error clearing index with SOLR",e);
		}
	}

	/**
	 * Optimize the index (this is a very expensive operation, especially if the index is large!)
	 * @throws DaoException
	 */
	public void optimize() throws DaoException {
		if (log.isTraceEnabled()) {
			log.trace("optimize()");
		}

		init();
		try {
			log.info("Optimizing SOLR index");
			solr.optimize();
		}
		catch (Exception e) {
			throw new DaoException("Error optimizing index with SOLR",e);
		}
	}

	/**
	 * Run the given query against the index.
	 * @param query
	 * @return
	 * @throws DaoException
	 */
	public QueryResponse search(SolrQuery query) throws DaoException {
		if (log.isTraceEnabled()) {
			log.trace("search(query="+query.getQuery()+")");
		}

		init();
		try {
			log.debug("Running SOLR query: "+query);
			return solr.query(query);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new DaoException("Error searching with SOLR",e);
		}
	}

	/**
	 * Runs a special id query against the index, breaking it up into several queries if necessary.
	 * @param domainObjectIds
	 * @return
	 * @throws DaoException
	 */
	public Map<Long,SolrDocument> search(List<Long> domainObjectIds) throws DaoException {
		if (log.isTraceEnabled()) {
			log.trace("search(entityIds.size="+domainObjectIds.size()+")");
		}

		init();
		Map<Long,SolrDocument> docMap = new HashMap<>();
		try {
			int currSize = 0;
			StringBuffer sqBuf = new StringBuffer();
			for(Long entityId : domainObjectIds) {

				if (currSize>=MAX_ID_LIST_SIZE) {
					SolrQuery query = new SolrQuery(sqBuf.toString());
					query.setRows(currSize);
					QueryResponse qr = search(query);
					Iterator<SolrDocument> i = qr.getResults().iterator();
					while (i.hasNext()) {
						SolrDocument doc = i.next();
						docMap.put(new Long(doc.get("id").toString()), doc);
					}
					sqBuf = new StringBuffer();
					currSize = 0;
				}

				if (sqBuf.length()>0) sqBuf.append(" OR ");
				sqBuf.append("id:"+entityId);
				currSize++;
			}

			if (currSize>0) {
				SolrQuery query = new SolrQuery(sqBuf.toString());
				query.setRows(currSize);
				QueryResponse qr = search(query);
				Iterator<SolrDocument> i = qr.getResults().iterator();
				while (i.hasNext()) {
					SolrDocument doc = i.next();
					docMap.put(new Long(doc.get("id").toString()), doc);
				}
			}

			return docMap;
		}
		catch (Exception e) {
			throw new DaoException("Error searching with SOLR",e);
		}
	}

	/**
	 * Update the document for the given entity to add a new ancestor (usually a new parent).
	 * @param domainObjectId
	 * @param newAncestorId
	 * @throws DaoException
	 */
	public void addNewAncestor(Long domainObjectId, Long newAncestorId) throws DaoException {
		if (log.isTraceEnabled()) {
			log.trace("addNewAncestor(entityId="+domainObjectId+", newAncestorId="+newAncestorId+")");
		}

		List<Long> domainObjectIds = new ArrayList<>();
		domainObjectIds.add(domainObjectId);
		addNewAncestor(domainObjectIds, newAncestorId);
	}

	/**
	 * Update the documents for all of the given entities and add the same new ancestor (usually a new parent) to
	 * each one.
	 * @param domainObjectIds
	 * @param newAncestorId
	 * @throws DaoException
	 */
	public void addNewAncestor(List<Long> domainObjectIds, Long newAncestorId) throws DaoException {
		if (log.isTraceEnabled()) {
			log.trace("addNewAncestor(entityIds.size="+domainObjectIds.size()+", newAncestorId="+newAncestorId+")");
		}

		// Get all Solr documents
		Map<Long,SolrDocument> solrDocMap = search(domainObjectIds);

		log.info("Adding new ancestor to "+domainObjectIds.size()+" entities. Found "+solrDocMap.size()+" documents.");

		// Create updated Solr documents
		List<SolrInputDocument> inputDocs = new ArrayList<>();
		for(SolrDocument existingDoc : solrDocMap.values()) {
			SolrInputDocument inputDoc = ClientUtils.toSolrInputDocument(existingDoc);

			Collection<Long> ancestorIds = null;

			SolrInputField field = inputDoc.getField("ancestor_ids");
			if (field==null) {
				ancestorIds = new ArrayList<>();
			}
			else {
				ancestorIds = (Collection<Long>)field.getValue();
				if (ancestorIds==null) {
					ancestorIds = new ArrayList<>();
				}
			}

			ancestorIds.add(newAncestorId);

			inputDoc.removeField("ancestor_ids");
			inputDoc.addField("ancestor_ids", ancestorIds, 0.2f);

			inputDocs.add(inputDoc);
			log.info("Updating index for "+inputDoc.getFieldValue("name")+" (id="+inputDoc.getFieldValue("id")+"), adding ancestor "+newAncestorId);
		}

		// Index the entire batch

		index(inputDocs);
		commit();
	}
}