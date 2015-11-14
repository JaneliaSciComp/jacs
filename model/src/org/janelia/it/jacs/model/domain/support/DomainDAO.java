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
import org.janelia.it.jacs.model.domain.Preference;
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

    protected MongoCollection preferenceCollection;
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
        this.preferenceCollection = getCollectionByClass(Preference.class);
    }

    public final MongoCollection getCollectionByClass(Class<?> domainClass) {
        String collectionName = DomainUtils.getCollectionName(domainClass);
        return jongo.getCollection(collectionName);
    }

    public MongoCollection getCollectionByName(String collectionName) {
        if (collectionName==null) throw new IllegalArgumentException("collectionName argument may not be null");
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

    /**
     * Return all the subjects.
     */
    public List<Subject> getSubjects() {
        return toList(subjectCollection.find().as(Subject.class));
    }

    /**
     * Return all the preferences for a given subject.
     */
    public List<Preference> getPreferences(String subjectKey) {
        return toList(preferenceCollection.find("{subjectKey:#}",subjectKey).as(Preference.class));
    }

    /**
     * Saves the given subject preference.
     * @param subjectKey
     * @param preference
     * @return
     * @throws Exception
     */
    public Preference save(String subjectKey, Preference preference) throws Exception {

        if (preference.getId()==null) {
            preference.setId(getNewId());
            preferenceCollection.insert(preference);
        }
        else {
            preferenceCollection.update("{_id:#,subjectKey:#}", preference.getId(), subjectKey).with(preference);
        }
        
        log.info("Saved "+preference.getClass().getName()+"#"+preference.getId());
        return preference;
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
    private <T extends DomainObject> List<T> toList(MongoCursor<T> cursor, Collection<Long> ids) {
        if (ids==null) {
            List<T> list = new ArrayList<>();
            for(T item : cursor) {
                list.add(item);
            }
            return list;
        }
        List<T> list = new ArrayList<>(ids.size());
        Map<Long,T> map = new HashMap<>(ids.size());
        for(T item : cursor) {
            map.put(item.getId(), item);
        }
        for(Long id : ids) {
            T item = map.get(id);
            if  (item!=null) {
                list.add(item);
            }
        }
        return list;
    }

    /**
     * Retrieve a refresh copy of the given domain object from the database. 
     */
    public <T extends DomainObject> T getDomainObject(String subjectKey, T domainObject) {
        return (T)getDomainObject(subjectKey, domainObject.getClass(), domainObject.getId());
    }
    
    /**
     * Get the domain object referenced by the collection name and id. 
     */
    public <T extends DomainObject> T getDomainObject(String subjectKey, Class<T> domainClass, Long id) {
        Reference reference = new Reference(domainClass.getName(), id);
        return (T)getDomainObject(subjectKey, reference);
    }

    /**
     * Get the domain object referenced by the given Reference.
     */
    public DomainObject getDomainObject(String subjectKey, Reference reference) {
        List<Long> ids = new ArrayList<>();
        ids.add(reference.getTargetId());
        List<DomainObject> objs = getDomainObjects(subjectKey, reference.getTargetClassName(), ids);
        if (objs.isEmpty()) {
            return null;
        }
        return objs.get(0);
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
            referenceMap.put(reference.getTargetClassName(), reference.getTargetId());
        }

        for(String className : referenceMap.keySet()) {
            List<DomainObject> objs = getDomainObjects(subjectKey, className, referenceMap.get(className));
            domainObjects.addAll(objs);
        }

        return domainObjects;
    }

    public <T extends DomainObject> List<T> getDomainObjects(String subjectKey, String className, Collection<Long> ids) {
        if (className==null) {
            return new ArrayList<>();
        }
        Class<T> clazz = (Class<T>)DomainUtils.getObjectClassByName(className);
        return getDomainObjects(subjectKey, clazz, ids);
    }
    
    /**
     * Get the domain objects in the given collection name with the specified ids.
     */
    public <T extends DomainObject> List<T> getDomainObjects(String subjectKey, Class<T> domainClass, Collection<Long> ids) {

        if (domainClass==null) {
            return new ArrayList<>();
        }
        
        long start = System.currentTimeMillis();
        log.trace("getDomainObjects(subjectKey={},className="+domainClass.getName()+",ids="+ids+")");

        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);

        String collectionName = DomainUtils.getCollectionName(domainClass);
        MongoCursor<T> cursor = null;
        if (ids==null) {
            if (subjects == null) {
                cursor = getCollectionByName(collectionName).find().as(domainClass);
            }
            else {
                cursor = getCollectionByName(collectionName).find("{readers:{$in:#}}", ids, subjects).as(domainClass);
            }
        }
        else {
            if (subjects == null) {
                cursor = getCollectionByName(collectionName).find("{_id:{$in:#}}", ids).as(domainClass);
            }
            else {
                cursor = getCollectionByName(collectionName).find("{_id:{$in:#},readers:{$in:#}}", ids, subjects).as(domainClass);
            }
        }

        List<T> list = toList(cursor, ids);
        log.trace("Getting "+list.size()+" "+collectionName+" objects took "+(System.currentTimeMillis()-start)+" ms");
        return list;
    }

    public List<DomainObject> getDomainObjects(String subjectKey, ReverseReference reverseRef) {
        Set<String> subjects = subjectKey==null?null:getSubjectSet(subjectKey);
        
        Class<? extends DomainObject> clazz = DomainUtils.getObjectClassByName(reverseRef.getReferringClassName());
        String collectionName = DomainUtils.getCollectionName(reverseRef.getReferringClassName());

        MongoCursor<? extends DomainObject> cursor = null;
        if (subjects==null) {
        	cursor = getCollectionByName(collectionName).find("{'"+reverseRef.getReferenceAttr()+"':#}", reverseRef.getReferenceId()).as(clazz);
        }
        else {
        	cursor = getCollectionByName(collectionName).find("{'"+reverseRef.getReferenceAttr()+"':#,readers:{$in:#}}", reverseRef.getReferenceId(), subjects).as(clazz);
        }
        
        List<DomainObject> list = toList(cursor);
        if (list.size()!=reverseRef.getCount()) {
            log.warn("Reverse reference ("+reverseRef.getReferringClassName()+":"+reverseRef.getReferenceAttr()+":"+reverseRef.getReferenceId()+
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
        String collectionName = DomainUtils.getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(collectionName);
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
                    throw new IllegalStateException("Updated "+result.getN()+" records instead of one: "+collectionName+"#"+domainObject.getId());
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
        
        String collectionName = DomainUtils.getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(collectionName);
        
        WriteResult result = collection.remove("{_id:#,writers:#}", domainObject.getId(), subjectKey);
        if (result.getN()!=1) {
            throw new IllegalStateException("Deleted "+result.getN()+" records instead of one: "+collectionName+"#"+domainObject.getId());
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
                log.trace("  {}#{}",reference.getTargetClassName(),reference.getTargetId());
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
                log.warn("Adding broken ref to collection "+ref.getTargetClassName()+" at the end");
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
            if (ref.getTargetClassName()==null) {
                throw new IllegalArgumentException("Cannot add child without a target class name");
            }
            treeNode.addChild(ref);
        }
        log.info("Adding "+references.size()+" objects to "+treeNode.getName());
        saveImpl(subjectKey, treeNode);
        return getDomainObject(subjectKey, treeNode);
    }

    public TreeNode addChildren(String subjectKey, TreeNode treeNodeArg, Collection<Reference> references, Integer index) throws Exception {
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
            if (ref.getTargetClassName()==null) {
                throw new IllegalArgumentException("Cannot add child without a target class name");
            }
            if (index!=null) {
                treeNode.insertChild(index+i, ref);
            }
            else {
                treeNode.addChild(ref);
            }
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
            if (ref.getTargetClassName()==null) {
                throw new IllegalArgumentException("Cannot add child without a target class name");
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
            String className = ref.getTargetClassName();
            if (objectSet.getClassName()==null) {
                if (ref.getTargetClassName()==null) {
                    throw new IllegalArgumentException("Cannot add member without a class name");
                }
                objectSet.setClassName(className);
            }
            else if (!className.equals(objectSet.getClassName())) {
                throw new IllegalArgumentException("Cannot add "+className+" to object set of "+objectSet.getClassName());
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

    public DomainObject updateProperty(String subjectKey, String className, Long id, String propName, String propValue) {
        Class<? extends DomainObject> clazz = DomainUtils.getObjectClassByName(className);
        DomainObject domainObject = getDomainObject(subjectKey, clazz, id);
        try {
            set(domainObject, propName, propValue);
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not update object attribute "+propName,e);
        }
        String collectionName = DomainUtils.getCollectionName(className);
        MongoCollection collection = getCollectionByName(collectionName);
        WriteResult wr = collection.update("{_id:#,writers:#}",domainObject.getId(),subjectKey).with("{$set: {"+propName+":#, updatedDate:#}}",propValue,new Date());
        if (wr.getN()!=1) {
            log.warn("Could not update "+collectionName+"#"+domainObject.getId()+"."+propName);
        }
        return getDomainObject(subjectKey, domainObject);
    }

    public void changePermissions(String subjectKey, String className, Long id, String granteeKey, String rights, boolean grant) throws Exception {
        changePermissions(subjectKey, className, Arrays.asList(id), granteeKey, rights, grant);
    }
    
    public void changePermissions(String subjectKey, String className, Collection<Long> ids, String granteeKey, String rights, boolean grant) throws Exception {

        String collectionName = DomainUtils.getCollectionName(className);
        
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
                            groupedIds.put(ref.getTargetClassName(), ref.getTargetId());
                        }
    
                        for(String refClassName : groupedIds.keySet()) {
                            Collection<Long> refIds = groupedIds.get(refClassName);
                            changePermissions(subjectKey, refClassName, refIds, granteeKey, rights, grant);
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
                        changePermissions(subjectKey, set.getClassName(), set.getMembers(), granteeKey, rights, grant);
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