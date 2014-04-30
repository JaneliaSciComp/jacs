package org.janelia.it.jacs.compute.access.mongodb;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.domain.Annotation;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Folder;
import org.janelia.it.jacs.model.domain.LSMImage;
import org.janelia.it.jacs.model.domain.NeuronFragment;
import org.janelia.it.jacs.model.domain.PatternMask;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.ScreenSample;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.marshall.jackson.JacksonMapper;

import com.fasterxml.jackson.databind.MapperFeature;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

public class DomainDAO {

    private static final Logger log = Logger.getLogger(DomainDAO.class);
    
    public static final String[] domainTypes = {"folder","sample","screenSample","patternMask","lsm","fragment","annotation","aontology"};
    public static final Class<?>[] domainClasses = {Folder.class,Sample.class,ScreenSample.class,PatternMask.class,LSMImage.class,NeuronFragment.class,Annotation.class,Ontology.class};
    
    protected static final String MONGO_DATABASE = "jacs";

    protected MongoClient m;
    protected DB db;
    protected Jongo jongo;
    protected MongoCollection folderCollection;
    protected MongoCollection sampleCollection;
    protected MongoCollection lsmCollection;
    protected MongoCollection fragmentCollection;
    protected MongoCollection annotationCollection;
    protected MongoCollection ontologyCollection;
    
    protected Map<String,Class<? extends DomainObject>> domainClassMap;
    
    public DomainDAO(String serverUrl) {
        try {
            m = new MongoClient(serverUrl);
            m.setWriteConcern(WriteConcern.JOURNALED);
            db = m.getDB(MONGO_DATABASE);
            jongo = new Jongo(db, 
                    new JacksonMapper.Builder()
                        .enable(MapperFeature.AUTO_DETECT_GETTERS)
                        .enable(MapperFeature.AUTO_DETECT_SETTERS)
                        .build());
            folderCollection = jongo.getCollection("folder");
            sampleCollection = jongo.getCollection("sample");
            lsmCollection = jongo.getCollection("lsm");
            fragmentCollection = jongo.getCollection("fragment");
            annotationCollection = jongo.getCollection("annotation");
            ontologyCollection = jongo.getCollection("ontology");
            
            domainClassMap = new HashMap<String,Class<? extends DomainObject>>();
            for(int i=0; i<DomainDAO.domainTypes.length; i++) {
                String domainType = DomainDAO.domainTypes[i];
                Class<? extends DomainObject> domainClass = (Class<? extends DomainObject>)DomainDAO.domainClasses[i];
                domainClassMap.put(domainType, domainClass);
            }
        }
        catch (UnknownHostException e) {
            throw new RuntimeException("Unknown host: "+serverUrl);
        }
    }
    
    public void setWriteConcern(WriteConcern writeConcern) {
        m.setWriteConcern(writeConcern);
    }

    public Map<String, Class<? extends DomainObject>> getDomainClassMap() {
        return Collections.unmodifiableMap(domainClassMap);
    }

    public MongoCollection getCollection(String type) {
        return jongo.getCollection(type);
    }
    
    public Class<? extends DomainObject> getObjectClass(String type) {
        return domainClassMap.get(type);
    }
    
    private <T> List<T> toList(Iterable<? extends T> iterable) {
        List<T> list = new ArrayList<T>();
        for(T item : iterable) {
            list.add(item);
        }
        return list;
    }

    public List<Ontology> getOntologies(String subjectKey) {
        return toList(ontologyCollection.find("{readers:#}",subjectKey).as(Ontology.class));
    }
    
    public List<Folder> getRootFolders(String subjectKey) {
        return toList(folderCollection.find("{root:true,readers:#}",subjectKey).as(Folder.class));
    }
    
    public List<DomainObject> getDomainObjects(String subjectKey, List<Reference> references) {
        
        Multimap<String,Long> referenceMap = LinkedHashMultimap.<String,Long>create();
        for(Reference reference : references) {
            referenceMap.put(reference.getTargetType(), reference.getTargetId());
        }
        
        List<DomainObject> domainObjects = new ArrayList<DomainObject>();
        for(String type : referenceMap.keySet()) {
            domainObjects.addAll(getDomainObjects(subjectKey, type, referenceMap.get(type)));
        }
        
        return domainObjects;
    }

    public List<DomainObject> getDomainObjects(String subjectKey, String type, Collection<Long> ids) {
        return toList(getCollection(type).find("{_id:{$in:#},readers:#}", ids, subjectKey).as(getObjectClass(type)));
    }

    public List<DomainObject> getDomainObjects(String subjectKey, ReverseReference reverseRef) {
        String type = reverseRef.getReferringType();
        List<DomainObject> list = toList(getCollection(type).find("{"+reverseRef.getReferenceAttr()+":#,readers:#}", reverseRef.getReferenceId(), subjectKey).as(getObjectClass(type)));
        if (list.size()!=reverseRef.getCount()) {
            log.warn("Reverse reference ("+reverseRef.getReferringType()+":"+reverseRef.getReferenceAttr()+":"+reverseRef.getReferenceId()+
                    ") denormalized count ("+reverseRef.getCount()+") does not match actual count ("+list.size()+")");
        }
        return list;
    }
    
    @Deprecated
    public Folder getFolderById(String subjectKey, Long id) {
        return folderCollection.findOne("{_id:#,readers:#}",id,subjectKey).as(Folder.class);
    }

    @Deprecated
    public Sample getSampleById(String subjectKey, Long id) {
        return sampleCollection.findOne("{_id:#,readers:#}", id, subjectKey).as(Sample.class);
    }

    @Deprecated
    public List<Sample> getSamplesByIds(String subjectKey, List<Long> ids) {
        return toList(sampleCollection.find("{_id:{$in:#},readers:#}", ids, subjectKey).as(Sample.class));
    }

    public List<LSMImage> getLsmsBySampleId(String subjectKey, Long id) {
        return toList(lsmCollection.find("{sampleId:#,readers:#}",id, subjectKey).as(LSMImage.class));
    }
    
    public void changePermissions(String subjectKey, String type, Long id, String granteeKey, String rights, boolean grant) throws Exception {
        Collection<Long> ids = new ArrayList<Long>();
        ids.add(id);
        changePermissions(subjectKey, type, ids, granteeKey, rights, grant);
    }
    
    public void changePermissions(String subjectKey, String type, Collection<Long> ids, String granteeKey, String rights, boolean grant) throws Exception {
        String op = grant ? "addToSet" : "pull";
        String attr = rights.equals("w") ? "writers" : "readers";
        MongoCollection collection = getCollection(type);
        
        WriteResult wr = collection.update("{_id:{$in:#},writers:#}",ids,subjectKey).multi().with("{$"+op+":{"+attr+":#}}",granteeKey);
        log.debug("  updated "+wr.getN()+" "+type);
        
        if (wr.getN()!=ids.size()) {
            // TODO: check this for real. 
            //System.out.println("WARN: Changing permissions on "+ids.size()+" items only affected "+wr.getN());
        }
        
        if ("folder".equals(type)) {
            for(Long id : ids) {
            
                Folder folder = collection.findOne("{_id:#,readers:#}",id,subjectKey).as(Folder.class);
    
                if (folder==null) {
                    throw new IllegalArgumentException("Could not find folder with id="+id);
                }
                
                Multimap<String,Long> groupedIds = HashMultimap.<String,Long>create();
                for(Reference ref : folder.getReferences()) {
                    groupedIds.put(ref.getTargetType(), ref.getTargetId());
                }
                
                for(String refType : groupedIds.keySet()) {
                    Collection<Long> refIds = groupedIds.get(refType);
                    changePermissions(subjectKey, refType, refIds, granteeKey, rights, grant);
                }
            }
        }
        else if ("sample".equals(type)) {
            WriteResult wr1 = fragmentCollection.update("{sampleId:{$in:#},writers:#}",ids,subjectKey).multi().with("{$addToSet:{"+attr+":#}}",granteeKey);
            log.debug("  updated "+wr1.getN()+" fragment");
            WriteResult wr2 = lsmCollection.update("{sampleId:{$in:#},writers:#}",ids,subjectKey).multi().with("{$addToSet:{"+attr+":#}}",granteeKey);
            log.debug("  updated "+wr2.getN()+" lsm");
        }
    }
    
    
    
}
