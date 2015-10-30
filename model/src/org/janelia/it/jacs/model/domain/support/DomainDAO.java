package org.janelia.it.jacs.model.domain.support;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.screen.FlyLine;
import org.janelia.it.jacs.model.domain.screen.PatternMask;
import org.janelia.it.jacs.model.domain.screen.ScreenSample;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.jongo.marshall.jackson.JacksonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.MapperFeature;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * Data access object for the domain object model. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainDAO {

    private static final Logger log = LoggerFactory.getLogger(DomainDAO.class);
    
    protected MongoClient m;
    protected Jongo jongo;

    protected MongoCollection alignmentBoardCollection;
    protected MongoCollection annotationCollection;
    protected MongoCollection compartmentSetCollection;
    protected MongoCollection dataSetCollection;
    protected MongoCollection flyLineCollection;
    protected MongoCollection fragmentCollection;
    protected MongoCollection imageCollection;
    protected MongoCollection objectSetCollection;
    protected MongoCollection ontologyCollection;
    protected MongoCollection patternMaskCollection;
    protected MongoCollection sampleCollection;
    protected MongoCollection screenSampleCollection;
    protected MongoCollection subjectCollection;
    protected MongoCollection treeNodeCollection;

    public DomainDAO(String serverUrl, String databaseName) throws UnknownHostException {
        this(serverUrl, databaseName, null, null);
    }

    public DomainDAO(String serverUrl, String databaseName, String username, String password) throws UnknownHostException {

        if (username!=null && password!=null) {
            MongoCredential credential = MongoCredential.createMongoCRCredential(username, databaseName, password.toCharArray());
            this.m = new MongoClient(new ServerAddress(serverUrl), Arrays.asList(credential));
            log.info("Connected to MongoDB ("+databaseName+"@"+serverUrl+") as user "+username);
        }
        else {
            this.m = new MongoClient(serverUrl);
            log.info("Connected to MongoDB ("+databaseName+"@"+serverUrl+")");
        }

        m.setWriteConcern(WriteConcern.JOURNALED);
        this.jongo = new Jongo(m.getDB(databaseName),
                new JacksonMapper.Builder()
                        .enable(MapperFeature.AUTO_DETECT_GETTERS)
                        .enable(MapperFeature.AUTO_DETECT_SETTERS)
                        .build());
        this.alignmentBoardCollection = getCollectionByClass(AlignmentBoard.class);
        this.annotationCollection = getCollectionByClass(Annotation.class);
        this.compartmentSetCollection = getCollectionByClass(CompartmentSet.class);
        this.dataSetCollection = getCollectionByClass(DataSet.class);
        this.flyLineCollection = getCollectionByClass(FlyLine.class);
        this.fragmentCollection = getCollectionByClass(NeuronFragment.class);
        this.imageCollection = getCollectionByClass(Image.class);
        this.objectSetCollection = getCollectionByClass(ObjectSet.class);
        this.ontologyCollection = getCollectionByClass(Ontology.class);
        this.patternMaskCollection = getCollectionByClass(PatternMask.class);
        this.sampleCollection = getCollectionByClass(Sample.class);
        this.screenSampleCollection = getCollectionByClass(ScreenSample.class);
        this.subjectCollection = getCollectionByClass(Subject.class);
        this.treeNodeCollection = getCollectionByClass(TreeNode.class);
    }

    public MongoCollection getCollectionByClass(Class<?> domainClass) {
        String collectionName = getCollectionName(domainClass);
        return jongo.getCollection(collectionName);
    }

    public Jongo getJongo() {
        return jongo;
    }

    public MongoClient getMongo() {
        return m;
    }

    public void setWriteConcern(WriteConcern writeConcern) {
        m.setWriteConcern(writeConcern);
    }

    public Class<? extends DomainObject> getObjectClass(String collectionName) {
        return DomainUtils.getObjectClass(collectionName);
    }

    public String getCollectionName(Class<?> domainClass) {
        return DomainUtils.getCollectionName(domainClass);
    }

    private String getCollectionName(DomainObject domainObject) {
        return DomainUtils.getCollectionName(domainObject);
    }

    public MongoCollection getCollectionByName(String collectionName) {
        if (collectionName==null) throw new IllegalArgumentException("collectionName argument may not be null");
        return jongo.getCollection(collectionName);
    }

    /**
     * Return all the subjects.
     */
    public List<Subject> getSubjects() {
        return toList(subjectCollection.find().as(Subject.class));
    }

    /**
     * Return the set of subjectKeys which are readable by the given subject. This includes the subject itself, and all of the groups it is part of. 
     */
    private Set<String> getSubjectSet(String subjectKey) {
        Subject subject = subjectCollection.findOne("{key:#}",subjectKey).projection("{_id:0,class:1,groups:1}").as(Subject.class);
        if (subject==null) throw new IllegalArgumentException("No such subject: "+subjectKey);
        Set<String> groups = subject.getGroups();
        groups.add(subjectKey);
        return groups;
    }

    /**
     * Create a list of the result set in iteration order.
     */
    private <T> List<T> toList(MongoCursor<? extends T> cursor) {
        List<T> list = new ArrayList<>();
        for(T item : cursor) {
            list.add(item);
        }
        return list;
    }

    /**
     * Create a list of the result set in the order of the given id list. If ids is null then 
     * return the result set in the order it comes back.
     */
    private List<DomainObject> toList(MongoCursor<? extends DomainObject> cursor, Collection<Long> ids) {
        if (ids==null) {
            List<DomainObject> list = new ArrayList<>();
            for(DomainObject item : cursor) {
                list.add(item);
            }
            return list;
        }
        List<DomainObject> list = new ArrayList<>(ids.size());
        Map<Long,DomainObject> map = new HashMap<>(ids.size());
        for(DomainObject item : cursor) {
            map.put(item.getId(), item);
        }
        for(Long id : ids) {
            DomainObject item = map.get(id);
            if  (item!=null) {
                list.add(item);
            }
        }
        return list;
    }

    /**
     * Get the domain object referenced by the type and id. 
     */
    public <T extends DomainObject> T getDomainObject(String subjectKey, Class<T> domainClass, Long id) {
        return getDomainObject(subjectKey, getCollectionName(domainClass), id);
    }

    /**
     * Get the domain object referenced by the type and id. 
     */
    public <T extends DomainObject> T getDomainObject(String subjectKey, String collectionName, Long id) {
        Reference reference = new Reference(collectionName, id);
        return (T)getDomainObject(subjectKey, reference);
    }
    
    /**
     * Get the domain object referenced by the given Reference.
     */
    public DomainObject getDomainObject(String subjectKey, Reference reference) {
        List<Long> ids = new ArrayList<>();
        ids.add(reference.getTargetId());
        List<DomainObject> objs = getDomainObjects(subjectKey, reference.getTargetType(), ids);
        if (objs.isEmpty()) {
            return null;
        }
        return objs.get(0);
    }

    public <T extends DomainObject> T getDomainObject(String subjectKey, T domainObject) {
        Reference ref = new Reference();
        ref.setTargetId(domainObject.getId());
        ref.setTargetType(DomainUtils.getCollectionName(domainObject));
        return (T)getDomainObject(subjectKey, ref);
    }
    
    /**
     * Get the domain objects referenced by the given list of References.
     */
    public List<DomainObject> getDomainObjects(String subjectKey, List<Reference> references) {

        List<DomainObject> domainObjects = new ArrayList<>();
        if (references==null || references.isEmpty()) return domainObjects;

        log.trace("getDomainObjects(subjectKey="+subjectKey+",references.size="+references.size()+")");

        Multimap<String,Long> referenceMap = ArrayListMultimap.<String,Long>create();
        for(Reference reference : references) {
            if (reference==null) {
                log.warn("Requested null reference");
                continue;
            }
            referenceMap.put(reference.getTargetType(), reference.getTargetId());
        }

        for(String type : referenceMap.keySet()) {
            List<DomainObject> objs = getDomainObjects(subjectKey, type, referenceMap.get(type));
            //log.info("Found {} objects of type {}",objs.size(),type);
            domainObjects.addAll(objs);
        }

        return domainObjects;
    }

    public <T extends DomainObject> List<T> getDomainObjects(String subjectKey, Class<T> domainClass, Collection<Long> ids) {
        return getDomainObjects(subjectKey, getCollectionName(domainClass), ids);
    }
    
    /**
     * Get the domain objects of the given type and ids.
     */
    public <T extends DomainObject> List<T> getDomainObjects(String subjectKey, String type, Collection<Long> ids) {

        long start = System.currentTimeMillis();
        log.trace("getDomainObjects(subjectKey="+subjectKey+",type="+type+")");

        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);

        Class<? extends DomainObject> clazz = getObjectClass(type);
        if (clazz==null) {
            return new ArrayList<T>();
        }

        MongoCursor<? extends DomainObject> cursor = null;
        if (ids==null) {
            if (subjects == null) {
                cursor = getCollectionByName(type).find().as(clazz);
            }
            else {
                cursor = getCollectionByName(type).find("{readers:{$in:#}}", ids, subjects).as(clazz);
            }
        }
        else {
            if (subjects == null) {
                cursor = getCollectionByName(type).find("{_id:{$in:#}}", ids).as(clazz);
            }
            else {
                cursor = getCollectionByName(type).find("{_id:{$in:#},readers:{$in:#}}", ids, subjects).as(clazz);
            }
        }

        List<T> list = (List<T>)toList(cursor, ids);
        log.trace("Getting "+list.size()+" "+type+" objects took "+(System.currentTimeMillis()-start)+" ms");
        return list;
    }

    public List<DomainObject> getDomainObjects(String subjectKey, ReverseReference reverseRef) {
        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);
        String type = reverseRef.getReferringType();

        MongoCursor<? extends DomainObject> cursor = null;
        if (subjects==null) {
        	cursor = getCollectionByName(type).find("{'"+reverseRef.getReferenceAttr()+"':#}", reverseRef.getReferenceId()).as(getObjectClass(type));
        }
        else {
        	cursor = getCollectionByName(type).find("{'"+reverseRef.getReferenceAttr()+"':#,readers:{$in:#}}", reverseRef.getReferenceId(), subjects).as(getObjectClass(type));
        }
        
        List<DomainObject> list = toList(cursor);
        if (list.size()!=reverseRef.getCount()) {
            log.warn("Reverse reference ("+reverseRef.getReferringType()+":"+reverseRef.getReferenceAttr()+":"+reverseRef.getReferenceId()+
                    ") denormalized count ("+reverseRef.getCount()+") does not match actual count ("+list.size()+")");
        }
        return list;
    }

    public List<Annotation> getAnnotations(String subjectKey, Long targetId) {
        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);

        MongoCursor<Annotation> cursor = null;
        if (subjects==null) {
            cursor = annotationCollection.find("{targetId:#}",targetId).as(Annotation.class);
        }
        else {
            cursor = annotationCollection.find("{targetId:#,readers:{$in:#}}",targetId,subjects).as(Annotation.class);
        }

        return toList(cursor);
    }

    public List<Annotation> getAnnotations(String subjectKey, Collection<Long> targetIds) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(annotationCollection.find("{targetId:{$in:#},readers:{$in:#}}",targetIds,subjects).as(Annotation.class));
    }

    public Workspace getDefaultWorkspace(String subjectKey) {
        return treeNodeCollection.findOne("{class:#,ownerKey:#}",Workspace.class.getName(),subjectKey).as(Workspace.class);
    }

    public Collection<Workspace> getWorkspaces(String subjectKey) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(treeNodeCollection.find("{class:#,readers:{$in:#}}",Workspace.class.getName(),subjects).as(Workspace.class));
    }

    public Collection<Ontology> getOntologies(String subjectKey) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(ontologyCollection.find("{readers:{$in:#}}",subjects).as(Ontology.class));
    }

    public List<LSMImage> getLsmsBySampleId(String subjectKey, Long id) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(imageCollection.find("{sampleId:#,readers:{$in:#}}",id, subjects).as(LSMImage.class));
    }
    
    public List<ScreenSample> getScreenSampleByFlyLine(String subjectKey, String flyLine) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(screenSampleCollection.find("{flyLine:{$regex:#},readers:{$in:#}}",flyLine+".*", subjects).as(ScreenSample.class));
    }
    
    public List<NeuronFragment> getNeuronFragmentsBySampleId(String subjectKey, Long sampleId) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(fragmentCollection.find("{sampleId:#,readers:{$in:#}}",sampleId,subjects).as(NeuronFragment.class));
    }
    
    public List<NeuronFragment> getNeuronFragmentsBySeparationId(String subjectKey, Long separationId) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(fragmentCollection.find("{separationId:#,readers:{$in:#}}",separationId,subjects).as(NeuronFragment.class));
    }
    
    public List<ScreenSample> getScreenSamples(String subjectKey) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(screenSampleCollection.find("{readers:{$in:#}}",subjectKey,subjects).as(ScreenSample.class));
    }
    
    public List<PatternMask> getPatternMasks(String subjectKey, Long screenSampleId) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(patternMaskCollection.find("{screenSampleId:#,readers:{$in:#}}",screenSampleId,subjects).as(PatternMask.class));
    }
   
    public TreeNode getTreeNodeById(String subjectKey, Long id) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return treeNodeCollection.findOne("{_id:#,readers:{$in:#}}",id,subjects).as(TreeNode.class);
    }
    
    public TreeNode getParentTreeNodes(String subjectKey, Long id) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return treeNodeCollection.findOne("{'children.targetId':#,readers:{$in:#}}",id,subjects).as(TreeNode.class);
    }

    private <T extends DomainObject> T saveImpl(String subjectKey, T domainObject) throws Exception {
        String type = getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(type);
        try {
            if (domainObject.getId()==null) {
                domainObject.setId(getNewId());
                domainObject.setOwnerKey(subjectKey);
                Set<String> subjects = new HashSet<>();
                subjects.add(subjectKey);
                domainObject.setReaders(subjects);
                domainObject.setWriters(subjects);
                domainObject.setCreationDate(new Date());
                domainObject.setUpdatedDate(new Date());
                collection.save(domainObject);
            }
            else {
                WriteResult result = collection.update("{_id:#,writers:#,updatedDate:#}", domainObject.getId(), subjectKey, domainObject.getUpdatedDate()).with(domainObject);
                if (result.getN()!=1) {
                    throw new IllegalStateException("Updated "+result.getN()+" records instead of one: "+type+"#"+domainObject.getId());
                }
            }
            log.info("Saved "+domainObject.getClass().getName()+"#"+domainObject.getId());
            return domainObject;
        }
        catch (MongoException e) {
            throw new Exception(e);
        }
    }

    public <T extends DomainObject> T save(String subjectKey, T domainObject) throws Exception {
        log.info("Saving changes to '{}'",domainObject.getName());
        saveImpl(subjectKey, domainObject);
        return getDomainObject(subjectKey, domainObject);
    }

    public void remove(String subjectKey, DomainObject domainObject) throws Exception {
        
        String type = getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(type);
        
        WriteResult result = collection.remove("{_id:#,writers:#}", domainObject.getId(), subjectKey);
        if (result.getN()!=1) {
            throw new IllegalStateException("Deleted "+result.getN()+" records instead of one: "+type+"#"+domainObject.getId());
        }
        
        // TODO: remove dependant objects?
    }

    public Ontology reorderTerms(String subjectKey, Long ontologyId, Long parentTermId, int[] order) throws Exception {

        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyId);
        if (ontology==null) {
            throw new IllegalArgumentException("Ontology not found: "+ontologyId);
        }
        OntologyTerm parent = DomainUtils.findTerm(ontology, parentTermId);
        if (parent==null) {
            throw new IllegalArgumentException("Term not found: "+parentTermId);
        }
        
        List<OntologyTerm> childTerms = new ArrayList<>(parent.getTerms());

        if (log.isTraceEnabled()) {
            log.trace("{} has the following terms: ",parent.getName());
            for(OntologyTerm term : childTerms) {
                log.trace("  {}",term.getId());
            }
            log.trace("They should be put in this ordering: ");
            for(int i=0; i<order.length; i++) {
                log.trace("  {} -> {}",i,order[i]);
            }
        }

        int originalSize = childTerms.size();
        OntologyTerm[] reordered = new OntologyTerm[childTerms.size()];
        for (int i = 0; i < order.length; i++) {
            int j = order[i];
            reordered[j] = childTerms.get(i);
            childTerms.set(i, null);
        }

        parent.getTerms().clear();
        for(OntologyTerm ref : reordered) {
            parent.getTerms().add(ref);
        }
        for(OntologyTerm term : childTerms) {
            if (term!=null) {
                log.info("Adding broken term "+term.getId()+" at the end");
                parent.getTerms().add(term);
            }
        }

        if (childTerms.size()!=originalSize) {
            throw new IllegalStateException("Reordered children have new size "+childTerms.size()+" (was "+originalSize+")");
        }
        else {
            log.info("Reordering children of ontology term '{}'",parent.getName());
            saveImpl(subjectKey, ontology);
            return (Ontology)getDomainObject(subjectKey, ontology);
        }
    }

    public Ontology addTerms(String subjectKey, Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms, Integer index) throws Exception {
        
        if (terms==null) {
            throw new IllegalArgumentException("Cannot add null children");
        }
        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyId);
        if (ontology==null) {
            throw new IllegalArgumentException("Ontology not found: "+ontologyId);
        }
        OntologyTerm parent = DomainUtils.findTerm(ontology, parentTermId);
        if (parent==null) {
            throw new IllegalArgumentException("Term not found: "+parentTermId);
        }
        
        int i = 0;
        for(OntologyTerm childTerm : terms) {
            if (childTerm.getId()==null) {
                childTerm.setId(getNewId());
            }
            if (index!=null) {
                parent.insertChild(index+i, childTerm);
            }
            else {
                parent.addChild(childTerm);
            }
            i++;
        }
        log.info("Adding "+terms.size()+" terms to "+parent.getName());
        saveImpl(subjectKey, ontology);
        return getDomainObject(subjectKey, ontology);
    }
    
    public Ontology removeTerm(String subjectKey, Long ontologyId, Long parentTermId, Long termId) throws Exception {
        
        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyId);
        if (ontology==null) {
            throw new IllegalArgumentException("Ontology not found: "+ontologyId);
        }
        OntologyTerm parent = DomainUtils.findTerm(ontology, parentTermId);
        if (parent.getTerms()==null) {
            throw new Exception("Term has no children: "+parentTermId);
        }
        
        OntologyTerm removed = null;
        for(Iterator<OntologyTerm> iterator = parent.getTerms().iterator(); iterator.hasNext(); ) {
            OntologyTerm child = iterator.next();
            if (child!=null && child.getId()!=null && child.getId().equals(termId)) {
                removed = child;
                iterator.remove();
                break;
            }
        }
        if (removed==null) {
            throw new Exception("Could not find term to remove: "+termId);
        }
        log.info("Removing term '{}' from '{}'",removed.getName(),parent.getName());
        saveImpl(subjectKey, ontology);
        return (Ontology)getDomainObject(subjectKey, ontology);
    }
    
    public TreeNode reorderChildren(String subjectKey, TreeNode treeNodeArg, int[] order) throws Exception {

        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode==null) {
            throw new IllegalArgumentException("Tree node not found: "+treeNodeArg.getId());
        }
        if (!treeNode.hasChildren()) {
            log.warn("Tree node has no children to reorder: "+treeNode.getId());
            return treeNode;
        }

        List<Reference> references = new ArrayList<>(treeNode.getChildren());

        if (references.size()!=order.length) {
            throw new IllegalArgumentException("Order array must be the same size as the child array ("+order.length+"!="+references.size()+")");
        }
        
        if (log.isTraceEnabled()) {
            log.trace("{} has the following references: ",treeNode.getName());
            for(Reference reference : references) {
                log.trace("  {}#{}",reference.getTargetType(),reference.getTargetId());
            }
            log.trace("They should be put in this ordering: ");
            for(int i=0; i<order.length; i++) {
                log.trace("  {} -> {}",i,order[i]);
            }
        }

        int originalSize = references.size();
        Reference[] reordered = new Reference[references.size()];
        for (int i = 0; i < order.length; i++) {
            int j = order[i];
            reordered[j] = references.get(i);
            references.set(i, null);
        }

        treeNode.getChildren().clear();
        for(Reference ref : reordered) {
            treeNode.getChildren().add(ref);
        }
        for(Reference ref : references) {
            if (ref!=null) {
                log.info("Adding broken ref of type "+ref.getTargetType()+" at the end");
                treeNode.getChildren().add(ref);
            }
        }

        if (references.size()!=originalSize) {
            throw new IllegalStateException("Reordered children have new size "+references.size()+" (was "+originalSize+")");
        }
        else {
            log.info("Reordering children of tree node '{}'",treeNode.getName());
            saveImpl(subjectKey, treeNode);
            return getDomainObject(subjectKey, treeNode);
        }
    }

    public TreeNode addChildren(String subjectKey, TreeNode treeNodeArg, Collection<Reference> references) throws Exception {
        if (references==null) {
            throw new IllegalArgumentException("Cannot add null children");
        }
        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode==null) {
            throw new IllegalArgumentException("Tree node not found: "+treeNodeArg.getId());
        }
        for(Reference ref : references) {
            if (ref.getTargetId()==null) {
                throw new IllegalArgumentException("Cannot add child without an id");
            }
            if (ref.getTargetType()==null) {
                throw new IllegalArgumentException("Cannot add child without a type");
            }
            treeNode.addChild(ref);
        }
        log.info("Adding "+references.size()+" objects to "+treeNode.getName());
        saveImpl(subjectKey, treeNode);
        return getDomainObject(subjectKey, treeNode);
    }

    public TreeNode addChildren(String subjectKey, TreeNode treeNodeArg, Collection<Reference> references, int startIndex) throws Exception {
        if (references==null) {
            throw new IllegalArgumentException("Cannot add null children");
        }
        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode==null) {
            throw new IllegalArgumentException("Tree node not found: "+treeNodeArg.getId());
        }
        int i = 0;
        for(Reference ref : references) {
            if (ref.getTargetId()==null) {
                throw new IllegalArgumentException("Cannot add child without an id");
            }
            if (ref.getTargetType()==null) {
                throw new IllegalArgumentException("Cannot add child without a type");
            }
            treeNode.insertChild(startIndex+i, ref);
            i++;
        }
        log.info("Adding "+references.size()+" objects to "+treeNode.getName());
        saveImpl(subjectKey, treeNode);
        return getDomainObject(subjectKey, treeNode);
    }
    
    public TreeNode removeChildren(String subjectKey, TreeNode treeNodeArg, Collection<Reference> references) throws Exception {
        if (references==null) {
            throw new IllegalArgumentException("Cannot remove null children");
        }
        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode==null) {
            throw new IllegalArgumentException("Tree node not found: "+treeNodeArg.getId());
        }
        for(Reference ref : references) {
            if (ref.getTargetId()==null) {
                throw new IllegalArgumentException("Cannot add child without an id");
            }
            if (ref.getTargetType()==null) {
                throw new IllegalArgumentException("Cannot add child without a type");
            }
            treeNode.removeChild(ref);
        }
        log.info("Removing "+references.size()+" objects from "+treeNode.getName());
        saveImpl(subjectKey, treeNode);
        return getDomainObject(subjectKey, treeNode);
    }

    public TreeNode removeReference(String subjectKey, TreeNode treeNodeArg, Reference reference) throws Exception {
        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode==null) {
            throw new IllegalArgumentException("Tree node not found: "+treeNodeArg.getId());
        }
        if (treeNode.hasChildren()) {
            for(Iterator<Reference> i = treeNode.getChildren().iterator(); i.hasNext(); ) {
                Reference iref = i.next();
                if (iref.equals(reference)) {
                    i.remove();
                }
            }
            log.info("Removing reference from '{}'",treeNode.getName());
            saveImpl(subjectKey, treeNode);
        }
        return getDomainObject(subjectKey, treeNode);
    }

    public ObjectSet addMembers(String subjectKey, ObjectSet objectSet, Collection<Reference> references) throws Exception {
        if (references==null) {
            throw new IllegalArgumentException("Cannot add null members");
        }
        for(Reference ref : references) {
            if (ref.getTargetId()==null) {
                throw new IllegalArgumentException("Cannot add member without an id");
            }
            String type = ref.getTargetType();
            if (objectSet.getTargetType()==null) {
                if (ref.getTargetType()==null) {
                    throw new IllegalArgumentException("Cannot add member without a type");
                }
                objectSet.setTargetType(type);
            }
            else if (!type.equals(objectSet.getTargetType())) {
                throw new IllegalArgumentException("Cannot add reference to type "+type+" to object set of type "+objectSet.getTargetType());
            }
            objectSet.addMember(ref.getTargetId());
        }
        log.info("Adding "+references.size()+" objects to "+objectSet.getName());
        saveImpl(subjectKey, objectSet);
        return getDomainObject(subjectKey, objectSet);
    }

    public ObjectSet removeMembers(String subjectKey, ObjectSet objectSet, Collection<Reference> references) throws Exception {
        if (references==null) {
            throw new IllegalArgumentException("Cannot remove null members");
        }

        for(Reference ref : references) {
            if (ref.getTargetId()==null) {
                throw new IllegalArgumentException("Cannot remove member without an id");
            }
            objectSet.removeMember(ref.getTargetId());
        }
        log.info("Removing "+references.size()+" objects from "+objectSet.getName());
        saveImpl(subjectKey, objectSet);
        return getDomainObject(subjectKey, objectSet);
    }

    public DomainObject updateProperty(String subjectKey, String collectionName, Long id, String propName, String propValue) {
        DomainObject domainObject = getDomainObject(subjectKey, collectionName, id);
        try {
            set(domainObject, propName, propValue);
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not update object attribute "+propName,e);
        }
        String type = getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(type);
        WriteResult wr = collection.update("{_id:#,writers:#}",domainObject.getId(),subjectKey).with("{$set: {"+propName+":#, updatedDate:#}}",propValue,new Date());
        if (wr.getN()!=1) {
            log.warn("Could not update "+type+"#"+domainObject.getId()+"."+propName);
        }
        return getDomainObject(subjectKey, domainObject);
    }

    public void changePermissions(String subjectKey, String type, Long id, String granteeKey, String rights, boolean grant) throws Exception {
        changePermissions(subjectKey, type, Arrays.asList(id), granteeKey, rights, grant);
    }
    
    public void changePermissions(String subjectKey, String collectionName, Collection<Long> ids, String granteeKey, String rights, boolean grant) throws Exception {

        if ("unknown".equals(collectionName)) {
            return;
        }
        
        String op = grant ? "addToSet" : "pull";
        
        int numKeys = 1;
        StringBuilder sb = new StringBuilder("{");
        sb.append("$").append(op).append(":{");
        
        if (rights.contains("r")) {
            sb.append("readers:#");
        }
        if (rights.contains("w")) {
            if (sb.length()>0) {
                numKeys++;
                sb.append(",");
            }
            sb.append("writers:#");
        }
        
        sb.append("}}");
        String withClause = sb.toString();

        Object[] keys = new String[numKeys];
        for(int i=0; i<numKeys; i++) {
            keys[i] = granteeKey;
        }
        
        log.info("withClause: "+withClause);
        
        String logIds = ids.size()<6 ? ""+ids : ids.size()+" ids";

        if (grant) {
            log.info("Granting {} permissions on all {} documents with ids {} to {}",rights,collectionName,logIds,granteeKey);
        }
        else {
            log.info("Revoking {} permissions on all {} documents with ids {} to {}",rights,collectionName,logIds,granteeKey);
        }

        MongoCollection collection = getCollectionByName(collectionName);
        WriteResult wr = collection.update("{_id:{$in:#},writers:#}",ids,subjectKey).multi().with(withClause,keys);
        log.info("Changed permissions on "+wr.getN()+" documents");

        if (wr.getN()>0) {
            if ("treeNode".equals(collectionName)) {
                log.info("Changing permissions on all members of the folders: {}",logIds);
                for(Long id : ids) {
                    TreeNode node = collection.findOne("{_id:#,writers:#}",id,subjectKey).as(TreeNode.class);
                    if (node==null) {
                        throw new IllegalArgumentException("Could not find folder with id="+id);
                    }
    
                    if (node.hasChildren()) {
                        Multimap<String,Long> groupedIds = HashMultimap.<String,Long>create();
                        for(Reference ref : node.getChildren()) {
                            groupedIds.put(ref.getTargetType(), ref.getTargetId());
                        }
    
                        for(String refType : groupedIds.keySet()) {
                            Collection<Long> refIds = groupedIds.get(refType);
                            changePermissions(subjectKey, refType, refIds, granteeKey, rights, grant);
                        }
                    }
                }
            }
            else if ("objectSet".equals(collectionName)) {
                log.info("Changing permissions on all members of the object sets: {}",logIds);
                for(Long id : ids) {
                    ObjectSet set = collection.findOne("{_id:#,writers:#}",id,subjectKey).as(ObjectSet.class);
                    if (set==null) {
                        throw new IllegalArgumentException("Could not find object set with id="+id);
                    }
                    if (set.hasMembers()) {
                        changePermissions(subjectKey, set.getTargetType(), set.getMembers(), granteeKey, rights, grant);
                    }
                }
            }
            else if ("sample".equals(collectionName)) {
                
                log.info("Changing permissions on all fragments and lsms associated with samples: {}",logIds);
                
                WriteResult wr1 = fragmentCollection.update("{sampleId:{$in:#},writers:#}",ids,subjectKey).multi().with(withClause,keys);
                log.info("Updated permissions on {} fragments",wr1.getN());
                
                WriteResult wr2 = imageCollection.update("{sampleId:{$in:#},writers:#}",ids,subjectKey).multi().with(withClause,keys);
                log.info("Updated permissions on {} lsms",wr2.getN());
                
            }
            else if ("screenSample".equals(collectionName)) {
                log.info("Changing permissions on all patternMasks associated with screen samples: {}",logIds);
                patternMaskCollection.update("{screenSampleId:{$in:#},writers:#}",ids,subjectKey).multi().with(withClause,keys);
            }
        }
    }

    // Copy and pasted from ReflectionUtils in shared module
    private void set(Object obj, String attributeName, Object value) throws Exception {
        Class[] argTypes = {value.getClass()};
        Object[] argValues = {value};
        String methodName = getAccessor("set", attributeName);
        obj.getClass().getMethod(methodName, argTypes).invoke(obj, argValues);
    }

    // Copy and pasted from ReflectionUtils in shared module
    private static String getAccessor(String prefix, String attributeName) {
        String firstChar = attributeName.substring(0, 1).toUpperCase();
        return prefix+firstChar+attributeName.substring(1);
    }
    
    public Long getNewId() {
        return TimebasedIdentifierGenerator.generateIdList(1).get(0);
    }
    
    // UNSECURE METHODS, SERVER SIDE ONLY
    // TODO: MOVE THESE ELSEWHERE

    /**
     * Get the domain objects of the given type 
     */
//    public <T extends DomainObject> MongoCursor<T> getDomainObjects(Class<T> domainClass) {
//        return registerCollectionByClass(domainClass).find().as(domainClass);
//    }

    /**
     * Get the domain objects of the given type 
     */
//    public MongoCursor<? extends DomainObject> getDomainObjects(String type) {
//        Class<? extends DomainObject> clazz = getObjectClass(type);
//        if (clazz==null) {
//        	throw new IllegalArgumentException("No object type for "+type);
//        }
//
//        return getCollectionByName(type).find().as(clazz);
//    }

    /**
     * Get the raw domain objects of the given type 
     */
//    public MongoCursor<DBObject> getRawObjects(String type) {
//        return getCollectionByName(type).find().map(new RawResultHandler<DBObject>());
//    }

//    public static void main(String[] args) throws Exception {
//        
//        String MONGO_SERVER_URL = "mongodb2";
//        String MONGO_DATABASE = "jacs";
//        DomainDAO dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, "flyportal", "flyportal");
//        Collection<Workspace> workspaces = dao.getWorkspaces("user:asoy");
//        for(Workspace workspace : workspaces) {
//            System.out.println(workspace.getId()+" "+workspace);
//        }
//        dao.changePermissions("user:nerna", "objectSet", Arrays.asList(1827600279629987929L), "user:rokickik", "rw", true);
//        
//    }

}