package org.janelia.it.jacs.compute.access.mongodb;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.domain.Folder;
import org.janelia.it.jacs.model.domain.LSMImage;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.marshall.jackson.JacksonMapper;

import com.fasterxml.jackson.databind.MapperFeature;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

public class DomainDAO {

    private static final Logger log = Logger.getLogger(DomainDAO.class);
    
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
        }
        catch (UnknownHostException e) {
            throw new RuntimeException("Unknown host: "+serverUrl);
        }
    }

    private MongoCollection getCollection(String name) {
        return jongo.getCollection(name);
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
        return toList(folderCollection.find("{root:true,ownerKey:#}",subjectKey).as(Folder.class));
    }
    
    public Folder getFolderById(String subjectKey, Long id) {
        return folderCollection.findOne("{_id:#,readers:#}",id,subjectKey).as(Folder.class);
    }
    
    public Sample getSampleById(String subjectKey, Long id) {
        return sampleCollection.findOne("{_id:#,readers:#}", id, subjectKey).as(Sample.class);
    }
    
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
