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
import org.janelia.it.jacs.model.domain.FlyLine;
import org.janelia.it.jacs.model.domain.Folder;
import org.janelia.it.jacs.model.domain.LSMImage;
import org.janelia.it.jacs.model.domain.NeuronFragment;
import org.janelia.it.jacs.model.domain.PatternMask;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.ScreenSample;
import org.janelia.it.jacs.model.domain.TreeNode;
import org.janelia.it.jacs.model.domain.Workspace;
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
    
    public static final String[] domainTypes = {"workspace","sample","screenSample","patternMask","flyLine","lsm","fragment","annotation","ontology"};
    public static final Class<?>[] domainClasses = {TreeNode.class,Sample.class,ScreenSample.class,PatternMask.class,FlyLine.class,LSMImage.class,NeuronFragment.class,Annotation.class,Ontology.class};
    
    public static final String MONGO_DATABASE = "jacs";

    protected MongoClient m;
    protected DB db;
    protected Jongo jongo;
    protected MongoCollection subjectCollection;
    protected MongoCollection workspaceCollection;
    protected MongoCollection sampleCollection;
    protected MongoCollection screenSampleCollection;
    protected MongoCollection patternMaskCollection;
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
            subjectCollection = jongo.getCollection("subject");
            workspaceCollection = jongo.getCollection("workspace");
            sampleCollection = jongo.getCollection("sample");
            screenSampleCollection = jongo.getCollection("screenSample");
            patternMaskCollection = jongo.getCollection("patternMask");
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
    
    public Jongo getJongo() {
        return jongo;
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
    
    public List<DomainObject> getDomainObjects(String subjectKey, List<Reference> references) {

        List<DomainObject> domainObjects = new ArrayList<DomainObject>();
        if (references==null || references.isEmpty()) return domainObjects;
        
        Multimap<String,Long> referenceMap = LinkedHashMultimap.<String,Long>create();
        for(Reference reference : references) {
            referenceMap.put(reference.getTargetType(), reference.getTargetId());
        }
        
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
    
    public Collection<Workspace> getWorkspaces(String subjectKey) {
        System.out.println(Workspace.class.getName());
        return toList(workspaceCollection.find("{class:#,ownerKey:#}",Workspace.class.getName(),subjectKey).as(Workspace.class));
    }

    public Collection<Ontology> getOntologies(String subjectKey) {
        return toList(ontologyCollection.find("{readers:#}",subjectKey).as(Ontology.class));
    }
    
    @Deprecated
    public List<Folder> getRootFolders(String subjectKey) {
        return toList(workspaceCollection.find("{root:true,readers:#}",subjectKey).as(Folder.class));
    }
    
    public List<LSMImage> getLsmsBySampleId(String subjectKey, Long id) {
        return toList(lsmCollection.find("{sampleId:#,readers:#}",id, subjectKey).as(LSMImage.class));
    }
    
    public List<NeuronFragment> getNeuronFragmentsBySampleId(String subjectKey, Long sampleIdd) {
        return toList(fragmentCollection.find("{sampleId:#,readers:#}",sampleIdd,subjectKey).as(NeuronFragment.class));
    }
    
    public List<NeuronFragment> getNeuronFragmentsBySeparationId(String subjectKey, Long separationId) {
        return toList(fragmentCollection.find("{separationId:#,readers:#}",separationId,subjectKey).as(NeuronFragment.class));
    }
    
    public List<Annotation> getAnnotations(String subjectKey, Long targetId) {
        return toList(annotationCollection.find("{targetId:#,readers:#}",targetId,subjectKey).as(Annotation.class));
    }
    
    public List<Annotation> getAnnotations(String subjectKey, Collection<Long> targetIds) {
        return toList(annotationCollection.find("{targetId:{$in:#},readers:#}",targetIds,subjectKey).as(Annotation.class));
    }
    
    public List<ScreenSample> getScreenSamples(String subjectKey) {
        return toList(screenSampleCollection.find("{readers:#}",subjectKey).as(ScreenSample.class));
    }
    
    public List<PatternMask> getPatternMasks(String subjectKey, Long screenSampleId) {
        return toList(patternMaskCollection.find("{screenSampleId:#,readers:#}",screenSampleId,subjectKey).as(PatternMask.class));
    }
   
    public Folder getFolderById(String subjectKey, Long id) {
        return workspaceCollection.findOne("{_id:#,readers:#}",id,subjectKey).as(Folder.class);
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
//        log.debug("  updated "+wr.getN()+" "+type);
        
//        
//        if (wr.getN()!=ids.size()) { 
//            
//            Set<Long> idSet = new HashSet<Long>(ids);
//            Set<Long> returnSet = new HashSet<Long>();
//            
//            int i = 0;
//            for(Object o : collection.find("{_id:{$in:#}}",ids).as(getObjectClass(type))) {
//                Sample s = (Sample)o;
//                returnSet.add(s.getId());
//            }
//
//            log.warn("WARN: Changing permissions on "+ids.size()+" items only affected "+wr.getN()+" Got: "+i);
//            idSet.removeAll(returnSet);
//            
//            log.warn(idSet);
//        }
        
        if ("folder".equals(type)) {
            for(Long id : ids) {
            
                Folder folder = collection.findOne("{_id:#,readers:#}",id,subjectKey).as(Folder.class);
    
                if (folder==null) {
                    throw new IllegalArgumentException("Could not find folder with id="+id);
                }
                
                Multimap<String,Long> groupedIds = HashMultimap.<String,Long>create();
                for(Reference ref : folder.getChildren()) {
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
