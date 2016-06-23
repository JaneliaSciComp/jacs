package org.janelia.it.jacs.model.domain.support;

import java.net.UnknownHostException;
import java.util.*;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Category;
import org.janelia.it.jacs.model.domain.ontology.EnumItem;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.screen.FlyLine;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.jongo.Aggregate;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.jongo.marshall.jackson.JacksonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static java.util.Arrays.asList;

/**
 * Data access object for the domain object model.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainDAO {

    private static final Logger log = LoggerFactory.getLogger(DomainDAO.class);

    protected MongoClient m;
    protected Jongo jongo;

    protected String databaseName;

    protected MongoCollection preferenceCollection;
    protected MongoCollection alignmentBoardCollection;
    protected MongoCollection alignmentContextCollection;
    protected MongoCollection annotationCollection;
    protected MongoCollection compartmentSetCollection;
    protected MongoCollection dataSetCollection;
    protected MongoCollection releaseCollection;
    protected MongoCollection flyLineCollection;
    protected MongoCollection fragmentCollection;
    protected MongoCollection imageCollection;
    protected MongoCollection ontologyCollection;
    protected MongoCollection sampleCollection;
    protected MongoCollection subjectCollection;
    protected MongoCollection treeNodeCollection;

    public DomainDAO(String serverUrl, String databaseName) throws UnknownHostException {
        this(serverUrl, databaseName, null, null);
    }

    public DomainDAO(String serverUrl, String databaseName, String username, String password) throws UnknownHostException {

        this.databaseName = databaseName;

        List<ServerAddress> members = new ArrayList<>();
        for (String serverMember : serverUrl.split(",")) {
            members.add(new ServerAddress(serverMember));
        }

        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            MongoCredential credential = MongoCredential.createMongoCRCredential(username, databaseName, password.toCharArray());
            this.m = new MongoClient(members, Arrays.asList(credential));
            log.info("Connected to MongoDB (" + databaseName + "@" + serverUrl + ") as user " + username);
        }
        else {
            this.m = new MongoClient(members);
            log.info("Connected to MongoDB (" + databaseName + "@" + serverUrl + ")");
        }

        m.setWriteConcern(WriteConcern.JOURNALED);
        this.jongo = new Jongo(m.getDB(databaseName),
                new JacksonMapper.Builder()
                .enable(MapperFeature.AUTO_DETECT_GETTERS)
                .enable(MapperFeature.AUTO_DETECT_SETTERS)
                .build());
        this.alignmentBoardCollection = getCollectionByClass(AlignmentBoard.class);
        this.alignmentContextCollection = getCollectionByClass(AlignmentContext.class);
        this.annotationCollection = getCollectionByClass(Annotation.class);
        this.compartmentSetCollection = getCollectionByClass(CompartmentSet.class);
        this.dataSetCollection = getCollectionByClass(DataSet.class);
        this.releaseCollection = getCollectionByClass(LineRelease.class);
        this.flyLineCollection = getCollectionByClass(FlyLine.class);
        this.fragmentCollection = getCollectionByClass(NeuronFragment.class);
        this.imageCollection = getCollectionByClass(Image.class);
        this.ontologyCollection = getCollectionByClass(Ontology.class);
        this.sampleCollection = getCollectionByClass(Sample.class);
        this.subjectCollection = getCollectionByClass(Subject.class);
        this.treeNodeCollection = getCollectionByClass(TreeNode.class);
        this.preferenceCollection = getCollectionByClass(Preference.class);
    }

    public final MongoCollection getCollectionByClass(Class<?> domainClass) {
        String collectionName = DomainUtils.getCollectionName(domainClass);
        return jongo.getCollection(collectionName);
    }

    public MongoCollection getCollectionByName(String collectionName) {
        if (collectionName == null) {
            throw new IllegalArgumentException("collectionName argument may not be null");
        }
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
     * Save the given subject.
     */
    public Subject save(Subject subject) {
        if (subject.getId() == null) {
            subject.setId(getNewId());
            subjectCollection.insert(subject);
        }
        else {
            subjectCollection.update("{_id:#}", subject.getId()).with(subject);
        }

        log.info("Saved " + subject.getClass().getName() + "#" + subject.getId());
        return subject;
    }

    /**
     * Return all the subjects.
     */
    public List<Subject> getSubjects() {
        return toList(subjectCollection.find().as(Subject.class));
    }

    /**
     * Return all the subjects.
     */
    public Subject getSubjectByKey(String subjectKey) {
        return subjectCollection.findOne("{key:#}", subjectKey).as(Subject.class);
    }

    /**
     * Return subject by name.
     */
    public Subject getSubjectByName(String subjectName) {
        return subjectCollection.findOne("{name:#}", subjectName).as(Subject.class);
    }

    /**
     * Return subject by name or key.
     */
    public Subject getSubjectByNameOrKey(String subjectName) {
        return subjectCollection.findOne("{$or:[{name:#},{key:#}]}", subjectName, subjectName).as(Subject.class);
    }

    /**
     * Return all the preferences for a given subject.
     */
    public List<Preference> getPreferences(String subjectKey) {
        return toList(preferenceCollection.find("{subjectKey:#}", subjectKey).as(Preference.class));
    }

    /**
     * Saves the given subject preference.
     *
     * @param subjectKey
     * @param preference
     * @return
     * @throws Exception
     */
    public Preference save(String subjectKey, Preference preference) throws Exception {

        if (preference.getId() == null) {
            preference.setId(getNewId());
            preferenceCollection.insert(preference);
        }
        else {
            preferenceCollection.update("{_id:#,subjectKey:#}", preference.getId(), subjectKey).with(preference);
        }

        log.info("Saved " + preference.getClass().getName() + "#" + preference.getId());
        return preference;
    }

    /**
     * Check whether the DomainObject has any ancestor references in TreeNode and ObjectSet.
     *
     * @param domainObject
     * @return boolean
     * @throws Exception
     */
    public List<Reference> getContainerReferences(DomainObject domainObject) throws Exception {

        log.info("Checking to see whether  " + domainObject.getId() + " has any parent references");
        if (domainObject.getId() == null) {
            return null;
        }

        String refStr = Reference.createFor(domainObject).toString();
        List<Reference> refList = new ArrayList<>();
        MongoCursor<TreeNode> treeCursor = treeNodeCollection.find("{children:#}", refStr).as(TreeNode.class);
        for (TreeNode item : treeCursor) {
            Reference newRef = Reference.createFor(item.getClass(), item.getId());
            refList.add(newRef);
        }
        return refList;
    }

    /**
     * Return the set of subjectKeys which are readable by the given subject. This includes the subject itself, and all of the groups it is part of.
     */
    private Set<String> getSubjectSet(String subjectKey) {
        if (subjectKey == null) {
            return null;
        }
        Subject subject = subjectCollection.findOne("{key:#}", subjectKey).projection("{_id:0,class:1,groups:1}").as(Subject.class);
        if (subject == null) {
            throw new IllegalArgumentException("No such subject: " + subjectKey);
        }
        Set<String> groups = subject.getGroups();
        groups.add(subjectKey);
        return groups;
    }

    /**
     * Create a list of the result set in iteration order.
     */
    private <T> List<T> toList(MongoCursor<? extends T> cursor) {
        List<T> list = new ArrayList<>();
        for (T item : cursor) {
            list.add(item);
        }
        return list;
    }

    /**
     * Create a list of the result set in the order of the given id list. If ids is null then
     * return the result set in the order it comes back.
     */
    private <T extends DomainObject> List<T> toList(MongoCursor<T> cursor, Collection<Long> ids) {
        if (ids == null) {
            List<T> list = new ArrayList<>();
            for (T item : cursor) {
                list.add(item);
            }
            return list;
        }
        List<T> list = new ArrayList<>(ids.size());
        Map<Long, T> map = new HashMap<>(ids.size());
        for (T item : cursor) {
            map.put(item.getId(), item);
        }
        for (Long id : ids) {
            T item = map.get(id);
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

    /**
     * Retrieve a refresh copy of the given domain object from the database.
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainObject> T getDomainObject(String subjectKey, T domainObject) {
        return (T) getDomainObject(subjectKey, domainObject.getClass(), domainObject.getId());
    }

    /**
     * Get the domain object referenced by the collection name and id.
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainObject> T getDomainObject(String subjectKey, Class<T> domainClass, Long id) {
        Reference reference = Reference.createFor(domainClass, id);
        return (T) getDomainObject(subjectKey, reference);
    }

    /**
     * Get the domain object referenced by the given Reference.
     */
    public DomainObject getDomainObject(String subjectKey, Reference reference) {
        List<DomainObject> objs = getDomainObjects(subjectKey, reference.getTargetClassName(), Arrays.asList(reference.getTargetId()));
        return objs.isEmpty() ? null : objs.get(0);
    }

    public <T extends DomainObject> List<T> getDomainObjectsAs(List<Reference> references, Class<T> clazz) {
        return getDomainObjectsAs(null, references, clazz);
    }

    public <T extends DomainObject> List<T> getDomainObjectsAs(String subjectKey, List<Reference> references, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (DomainObject object : getDomainObjects(subjectKey, references)) {
            if (clazz.isAssignableFrom(object.getClass())) {
                list.add((T) object);
            }
            else {
                throw new IllegalArgumentException("Referenced object is " + object.getClass().getSimpleName() + " not " + clazz.getSimpleName());
            }
        }
        return list;
    }

    /**
     * Get the domain objects referenced by the given list of References.
     */
    public List<DomainObject> getDomainObjects(String subjectKey, List<Reference> references) {

        List<DomainObject> domainObjects = new ArrayList<>();
        if (references == null || references.isEmpty()) {
            return domainObjects;
        }

        log.trace("getDomainObjects(subjectKey=" + subjectKey + ",references.size=" + references.size() + ")");

        Multimap<String, Long> referenceMap = ArrayListMultimap.<String, Long>create();
        for (Reference reference : references) {
            if (reference == null) {
                log.warn("Requested null reference");
                continue;
            }
            referenceMap.put(reference.getTargetClassName(), reference.getTargetId());
        }

        for (String className : referenceMap.keySet()) {
            List<DomainObject> objs = getDomainObjects(subjectKey, className, referenceMap.get(className));
            domainObjects.addAll(objs);
        }

        return domainObjects;
    }

    /**
     * Get the domain objects of a single class with the specified ids.
     *
     * @param subjectKey
     * @param className
     * @param ids
     * @return
     */
    public <T extends DomainObject> List<T> getDomainObjects(String subjectKey, String className, Collection<Long> ids) {
        Class<T> clazz = (Class<T>) DomainUtils.getObjectClassByName(className);
        return getDomainObjects(subjectKey, clazz, ids);
    }

    /**
     * Get the domain objects in the given collection name with the specified ids.
     */
    public <T extends DomainObject> List<T> getDomainObjects(String subjectKey, Class<T> domainClass, Collection<Long> ids) {

        if (domainClass == null) {
            return new ArrayList<>();
        }

        long start = System.currentTimeMillis();
        log.trace("getDomainObjects(subjectKey={},className=" + domainClass.getName() + ",ids=" + ids + ")");

        Set<String> subjects = subjectKey == null ? null : getSubjectSet(subjectKey);

        String collectionName = DomainUtils.getCollectionName(domainClass);
        MongoCursor<T> cursor = null;
        if (ids == null) {
            if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
                cursor = getCollectionByName(collectionName).find().as(domainClass);
            }
            else {
                cursor = getCollectionByName(collectionName).find("{readers:{$in:#}}", subjects).as(domainClass);
            }
        }
        else {
            if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
                cursor = getCollectionByName(collectionName).find("{_id:{$in:#}}", ids).as(domainClass);
            }
            else {
                cursor = getCollectionByName(collectionName).find("{_id:{$in:#},readers:{$in:#}}", ids, subjects).as(domainClass);
            }
        }

        List<T> list = toList(cursor, ids);
        log.trace("Getting " + list.size() + " " + collectionName + " objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    public <T extends DomainObject> List<T> getUserDomainObjects(String subjectKey, Class<T> domainClass) {
        return getUserDomainObjects(subjectKey, domainClass, null);
    }

    /**
     * Get the domain objects owned by the given user, in the given collection name, with the specified ids.
     */
    public <T extends DomainObject> List<T> getUserDomainObjects(String subjectKey, Class<T> domainClass, Collection<Long> ids) {

        if (domainClass == null) {
            return new ArrayList<>();
        }

        long start = System.currentTimeMillis();
        log.trace("getUserDomainObjects(subjectKey={},className=" + domainClass.getName() + ",ids=" + ids + ")");

        String collectionName = DomainUtils.getCollectionName(domainClass);
        MongoCursor<T> cursor = null;
        if (ids == null) {
            cursor = getCollectionByName(collectionName).find("{ownerKey:#}", subjectKey).as(domainClass);
        }
        else {
            cursor = getCollectionByName(collectionName).find("{_id:{$in:#},ownerKey:#", ids, subjectKey).as(domainClass);
        }

        List<T> list = toList(cursor, ids);
        log.trace("Getting " + list.size() + " " + collectionName + " objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    /**
     * Get the domain objects referenced by the given reverse reference.
     *
     * @param subjectKey
     * @param reverseRef
     * @return
     */
    public List<DomainObject> getDomainObjects(String subjectKey, ReverseReference reverseRef) {
        Set<String> subjects = subjectKey == null ? null : getSubjectSet(subjectKey);

        Class<? extends DomainObject> clazz = DomainUtils.getObjectClassByName(reverseRef.getReferringClassName());
        String collectionName = DomainUtils.getCollectionName(reverseRef.getReferringClassName());

        MongoCursor<? extends DomainObject> cursor = null;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = getCollectionByName(collectionName).find("{'" + reverseRef.getReferenceAttr() + "':#}", reverseRef.getReferenceId()).as(clazz);
        }
        else {
            cursor = getCollectionByName(collectionName).find("{'" + reverseRef.getReferenceAttr() + "':#,readers:{$in:#}}", reverseRef.getReferenceId(), subjects).as(clazz);
        }

        List<DomainObject> list = toList(cursor);
        if (list.size() != reverseRef.getCount()) {
            log.warn("Reverse reference (" + reverseRef.getReferringClassName() + ":" + reverseRef.getReferenceAttr() + ":" + reverseRef.getReferenceId()
                    + ") denormalized count (" + reverseRef.getCount() + ") does not match actual count (" + list.size() + ")");
        }
        return list;
    }

    /**
     * Get the domain object by name.
     */
    public <T extends DomainObject> List<T> getDomainObjectsByName(String subjectKey, Class<T> domainClass, String name) {

        if (domainClass == null) {
            return null;
        }

        long start = System.currentTimeMillis();
        log.trace("getDomainObjects(subjectKey={},className=" + domainClass.getName() + ",name=" + name + ")");

        Set<String> subjects = subjectKey == null ? null : getSubjectSet(subjectKey);

        String collectionName = DomainUtils.getCollectionName(domainClass);
        MongoCursor<T> cursor = null;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = getCollectionByName(collectionName).find("{name:#}", name).as(domainClass);
        }
        else {
            cursor = getCollectionByName(collectionName).find("{name:#,readers:{$in:#}}", name, subjects).as(domainClass);
        }

        List<T> list = toList(cursor);
        log.trace("Getting " + list.size() + " " + collectionName + " objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    /**
     * Get domain objects of a given type with a given specified property value.
     */
    public <T extends DomainObject> List<T> getDomainObjectsWithProperty(String subjectKey, Class<T> domainClass, String propName, String propValue) {

        if (domainClass == null) {
            return null;
        }

        long start = System.currentTimeMillis();
        log.trace("getDomainObjects(subjectKey={},className=" + domainClass.getName() + ",propName=" + propName + ",propValue=" + propValue + ")");

        Set<String> subjects = subjectKey == null ? null : getSubjectSet(subjectKey);

        String collectionName = DomainUtils.getCollectionName(domainClass);
        MongoCursor<T> cursor = null;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = getCollectionByName(collectionName).find("{" + propName + ":#}", propValue).as(domainClass);
        }
        else {
            cursor = getCollectionByName(collectionName).find("{" + propName + ":#,readers:{$in:#}}", propValue, subjects).as(domainClass);
        }

        List<T> list = toList(cursor);
        log.trace("Getting " + list.size() + " " + collectionName + " objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    public List<Annotation> getAnnotations(String subjectKey, Collection<Reference> references) {
        log.trace("getAnnotations(subjectKey={},references=" + references + ")");
        Set<String> subjects = getSubjectSet(subjectKey);

        List<String> targetRefs = new ArrayList<>();
        for (Reference reference : references) {
            targetRefs.add(reference.toString());
        }

        MongoCursor<Annotation> cursor = null;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = annotationCollection.find("{target:{$in:#}}", targetRefs).as(Annotation.class);
        }
        else {
            cursor = annotationCollection.find("{target:{$in:#},readers:{$in:#}}", targetRefs, subjects).as(Annotation.class);
        }

        return toList(cursor);
    }

    public Workspace getDefaultWorkspace(String subjectKey) {
        return treeNodeCollection.findOne("{class:#,ownerKey:#}", Workspace.class.getName(), subjectKey).as(Workspace.class);
    }

    public List<Workspace> getWorkspaces(String subjectKey) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(treeNodeCollection.find("{class:#,readers:{$in:#}}", Workspace.class.getName(), subjects).as(Workspace.class));
    }

    public List<Ontology> getOntologies(String subjectKey) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(ontologyCollection.find("{readers:{$in:#}}", subjects).as(Ontology.class));
    }

    public OntologyTerm getErrorOntologyCategory() {
        List<Ontology> ontologies = getDomainObjectsByName(DomainConstants.GENERAL_USER_GROUP_KEY, Ontology.class, DomainConstants.ERROR_ONTOLOGY_NAME);
        if (ontologies.size() > 1) {
            log.warn("Multiple error ontologies detected. Please ensure that " + DomainConstants.GENERAL_USER_GROUP_KEY + " only owns a single ontology with name " + DomainConstants.ERROR_ONTOLOGY_NAME);
        }
        for (Ontology ontology : ontologies) {
            OntologyTerm term = ontology.findTerm(DomainConstants.ERROR_ONTOLOGY_CATEGORY);
            if (term instanceof Category) {
                return term;
            }
        }
        throw new IllegalStateException("Error ontology category could not be found");
    }

    public Annotation createAnnotation(String subjectKey, Reference target, OntologyTermReference ontologyTermReference, Object value) throws Exception {

        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyTermReference.getOntologyId());
        OntologyTerm ontologyTerm = ontology.findTerm(ontologyTermReference.getOntologyTermId());

        OntologyTerm keyTerm = ontologyTerm;
        OntologyTerm valueTerm = null;
        String keyString = keyTerm.getName();
        String valueString = value == null ? null : value.toString();

        if (keyTerm instanceof EnumItem) {
            keyTerm = ontologyTerm.getParent();
            valueTerm = ontologyTerm;
            keyString = keyTerm.getName();
            valueString = valueTerm.getName();
        }

        final Annotation annotation = new Annotation();
        annotation.setKey(keyString);
        annotation.setValue(valueString);
        annotation.setTarget(target);

        annotation.setKeyTerm(new OntologyTermReference(ontology, keyTerm));
        if (valueTerm != null) {
            annotation.setValueTerm(new OntologyTermReference(ontology, valueTerm));
        }

        String tag = (annotation.getValue() == null ? annotation.getKey()
                : annotation.getKey() + " = " + annotation.getValue());
        annotation.setName(tag);

        Annotation savedAnnotation = save(subjectKey, annotation);
        log.info("Saved annotation as " + savedAnnotation.getId());

        // TODO: auto-share annotation based on auto-share template (this logic is currently in the client)
        return savedAnnotation;
    }

    public List<DataSet> getDataSets(String subjectKey) {
        Set<String> subjects = getSubjectSet(subjectKey);
        if (subjects == null) {
            return toList(dataSetCollection.find().as(DataSet.class));
        }
        else {
            return toList(dataSetCollection.find("{readers:{$in:#}}", subjects).as(DataSet.class));
        }
    }

    public DataSet getDataSetByIdentifier(String subjectKey, String dataSetIdentifier) {
        Set<String> subjects = getSubjectSet(subjectKey);
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            return dataSetCollection.findOne("{identifier:#}", dataSetIdentifier).as(DataSet.class);
        }
        else {
            return dataSetCollection.findOne("{readers:{$in:#},identifier:#}", subjects, dataSetIdentifier).as(DataSet.class);
        }
    }

    public List<Sample> getSamplesForDataSet(String subjectKey, String dataSetIdentifier) {

        long start = System.currentTimeMillis();
        log.trace("getSampleBySlideCode(subjectKey={},dataSetIdentifier={})", subjectKey, dataSetIdentifier);

        Set<String> subjects = subjectKey == null ? null : getSubjectSet(subjectKey);

        MongoCursor<Sample> cursor = null;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = sampleCollection.find("{dataSet:#,sageSynced:true}", dataSetIdentifier).as(Sample.class);
        }
        else {
            cursor = sampleCollection.find("{dataSet:#,sageSynced:true,readers:{$in:#}}", dataSetIdentifier, subjects).as(Sample.class);
        }

        List<Sample> list = toList(cursor);
        log.trace("Getting " + list.size() + " Sample objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    public List<LSMImage> getLSMsForDataSet(String subjectKey, String dataSetIdentifier) {

        long start = System.currentTimeMillis();
        log.trace("getSampleBySlideCode(subjectKey={},dataSetIdentifier={})", subjectKey, dataSetIdentifier);

        Set<String> subjects = subjectKey == null ? null : getSubjectSet(subjectKey);

        MongoCursor<LSMImage> cursor = null;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = imageCollection.find("{dataSet:#,sageSynced:true}", dataSetIdentifier).as(LSMImage.class);
        }
        else {
            cursor = imageCollection.find("{dataSet:#,sageSynced:true,readers:{$in:#}}", dataSetIdentifier, subjects).as(LSMImage.class);
        }

        List<LSMImage> list = toList(cursor);
        log.trace("Getting " + list.size() + " LSMImage objects took " + (System.currentTimeMillis() - start) + " ms");
        return list;
    }

    public Sample getSampleBySlideCode(String subjectKey, String dataSetIdentifier, String slideCode) {

        long start = System.currentTimeMillis();
        log.trace("getSampleBySlideCode(subjectKey={},dataSetIdentifier={},slideCode={})", subjectKey, dataSetIdentifier, slideCode);

        Set<String> subjects = subjectKey == null ? null : getSubjectSet(subjectKey);

        MongoCursor<Sample> cursor = null;
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            cursor = sampleCollection.find("{dataSet:#,slideCode:#,sageSynced:true}", dataSetIdentifier, slideCode).as(Sample.class);
        }
        else {
            cursor = sampleCollection.find("{dataSet:#,slideCode:#,sageSynced:true,readers:{$in:#}}", dataSetIdentifier, slideCode, subjects).as(Sample.class);
        }

        List<Sample> list = toList(cursor);
        log.trace("Getting " + list.size() + " Sample objects took " + (System.currentTimeMillis() - start) + " ms");
        if (list.isEmpty()) {
        	return null;
        }
        if (list.size()>1) {
        	log.warn("More than one active sample found for "+dataSetIdentifier+"/"+slideCode);
        }
        return list.get(0);
    }

    public List<LSMImage> getLsmsBySampleId(String subjectKey, Long sampleId) {
        String refStr = "Sample#"+sampleId;
        Set<String> subjects = getSubjectSet(subjectKey);
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            return toList(imageCollection.find("{sampleRef:#,sageSynced:true}", refStr).as(LSMImage.class));
        }
        else {
            return toList(imageCollection.find("{sampleRef:#,sageSynced:true,readers:{$in:#}}", refStr, subjects).as(LSMImage.class));
        }
    }

    public LSMImage getLsmBySageId(String subjectKey, Integer sageId) {
        Set<String> subjects = getSubjectSet(subjectKey);
        if (subjects == null || subjects.contains(Subject.ADMIN_KEY)) {
            return imageCollection.findOne("{sageId:#,sageSynced:true}", sageId).as(LSMImage.class);
        }
        else {
            return imageCollection.findOne("{sageId:#,sageSynced:true,readers:{$in:#}}", sageId, subjects).as(LSMImage.class);
        }
    }

    public List<NeuronFragment> getNeuronFragmentsBySampleId(String subjectKey, Long sampleId) {
        String refStr = "Sample#"+sampleId;
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(fragmentCollection.find("{sampleRef:#,readers:{$in:#}}", refStr, subjects).as(NeuronFragment.class));
    }

    public List<NeuronFragment> getNeuronFragmentsBySeparationId(String subjectKey, Long separationId) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return toList(fragmentCollection.find("{separationId:#,readers:{$in:#}}", separationId, subjects).as(NeuronFragment.class));
    }

    public Sample getSampleBySeparationId(Long separationId) {
        return sampleCollection.findOne("{objectiveSamples.pipelineRuns.results.results.id:#}", separationId).as(Sample.class);
    }

    public NeuronSeparation getNeuronSeparation(String subjectKey, Long separationId) throws Exception {
        Set<String> subjects = getSubjectSet(subjectKey);
        Aggregate.ResultsIterator<NeuronSeparation> results = sampleCollection.aggregate("{$match: {\"objectiveSamples.pipelineRuns.results.results.id\": " + separationId + "}}")
                .and("{$unwind: \"$objectiveSamples\"}")
                .and("{$unwind: \"$objectiveSamples.pipelineRuns\"}")
                .and("{$unwind: \"$objectiveSamples.pipelineRuns.results\"}")
                .and("{$unwind: \"$objectiveSamples.pipelineRuns.results.results\"}")
                .and("{$match: {\"objectiveSamples.pipelineRuns.results.results.id\": "+separationId + "}}")
                .and("{$project: {class : \"$objectiveSamples.pipelineRuns.results.results.class\" ," +
                        "id : \"$objectiveSamples.pipelineRuns.results.results.id\"," +
                        "name : \"$objectiveSamples.pipelineRuns.results.results.name\"," +
                        "filepath : \"$objectiveSamples.pipelineRuns.results.results.filepath\"," +
                        "creationDate : \"$objectiveSamples.pipelineRuns.results.results.creationDate\"," +
                        "fragments : \"$objectiveSamples.pipelineRuns.results.results.fragments\"," +
                        "hasWeights : \"$objectiveSamples.pipelineRuns.results.results.hasWeights\"}}")
                .as(NeuronSeparation.class);
        if (results.hasNext()) {
            return results.next();
        }
        return null;
    }

    public TreeNode getTreeNodeById(String subjectKey, Long id) {
        Set<String> subjects = getSubjectSet(subjectKey);
        return treeNodeCollection.findOne("{_id:#,readers:{$in:#}}", id, subjects).as(TreeNode.class);
    }

    public TreeNode getParentTreeNodes(String subjectKey, Reference ref) {
        String refStr = ref.toString();
        Set<String> subjects = getSubjectSet(subjectKey);
        if (subjects == null) {
            return treeNodeCollection.findOne("{'children':#}", refStr).as(TreeNode.class);
        }
        else {
            return treeNodeCollection.findOne("{'children':#,readers:{$in:#}}", refStr, subjects).as(TreeNode.class);
        }
    }

    private <T extends DomainObject> T saveImpl(String subjectKey, T domainObject) throws Exception {
        String collectionName = DomainUtils.getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(collectionName);
        try {
            Date now = new Date();
            if (domainObject.getId() == null) {
                Set<String> subjects = Sets.newHashSet(subjectKey);
                domainObject.setId(getNewId());
                domainObject.setOwnerKey(subjectKey);
                domainObject.setReaders(subjects);
                domainObject.setWriters(subjects);
                domainObject.setCreationDate(now);
                domainObject.setUpdatedDate(now);
                collection.save(domainObject);
            }
            else {
                domainObject.setUpdatedDate(now);
                WriteResult result = collection.update("{_id:#,writers:#}", domainObject.getId(), subjectKey).with(domainObject);
                if (result.getN() != 1) {
                    throw new IllegalStateException("Updated " + result.getN() + " records instead of one: " + collectionName + "#" + domainObject.getId());
                }
            }
            log.info("Saved " + domainObject.getClass().getName() + "#" + domainObject.getId());
            return domainObject;
        }
        catch (MongoException e) {
            throw new Exception(e);
        }
    }

    /**
     * Saves the given object and returns a saved copy.
     *
     * @param subjectKey The subject saving the object. If this is a new object, then this subject becomes the owner of the new object.
     * @param domainObject The object to be saved. If the id is not set, then a new object is created.
     * @return a copy of the saved object
     * @throws Exception
     */
    public <T extends DomainObject> T save(String subjectKey, T domainObject) throws Exception {
        saveImpl(subjectKey, domainObject);
        // TODO: The only reason this retrieves the saved object is to avoid errors during development where the client incorrectly 
        // depends on input object being returned. However, it's needlessly inefficient, so once we have remote clients written 
        // we may want to optimize by just returning domainObject here. 
        return getDomainObject(subjectKey, domainObject);
    }

    public void remove(String subjectKey, DomainObject domainObject) throws Exception {

        String collectionName = DomainUtils.getCollectionName(domainObject);
        MongoCollection collection = getCollectionByName(collectionName);

        WriteResult result = collection.remove("{_id:#,writers:#}", domainObject.getId(), subjectKey);
        if (result.getN() != 1) {
            throw new IllegalStateException("Deleted " + result.getN() + " records instead of one: " + collectionName + "#" + domainObject.getId());
        }

        // TODO: remove dependent objects?
    }

    public Ontology reorderTerms(String subjectKey, Long ontologyId, Long parentTermId, int[] order) throws Exception {

        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyId);
        if (ontology == null) {
            throw new IllegalArgumentException("Ontology not found: " + ontologyId);
        }
        OntologyTerm parent = ontology.findTerm(parentTermId);
        if (parent == null) {
            throw new IllegalArgumentException("Term not found: " + parentTermId);
        }

        List<OntologyTerm> childTerms = new ArrayList<>(parent.getTerms());

        if (log.isTraceEnabled()) {
            log.trace("{} has the following terms: ", parent.getName());
            for (OntologyTerm term : childTerms) {
                log.trace("  {}", term.getId());
            }
            log.trace("They should be put in this ordering: ");
            for (int i = 0; i < order.length; i++) {
                log.trace("  {} -> {}", i, order[i]);
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
        for (OntologyTerm ref : reordered) {
            parent.getTerms().add(ref);
        }
        for (OntologyTerm term : childTerms) {
            if (term != null) {
                log.warn("Adding broken term " + term.getId() + " at the end");
                parent.getTerms().add(term);
            }
        }

        if (childTerms.size() != originalSize) {
            throw new IllegalStateException("Reordered children have new size " + childTerms.size() + " (was " + originalSize + ")");
        }

        log.debug("Reordering children of ontology term '{}'", parent.getName());
        saveImpl(subjectKey, ontology);
        return getDomainObject(subjectKey, ontology);
    }

    public Ontology addTerms(String subjectKey, Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms, Integer index) throws Exception {

        if (terms == null) {
            throw new IllegalArgumentException("Cannot add null children");
        }
        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyId);
        if (ontology == null) {
            throw new IllegalArgumentException("Ontology not found: " + ontologyId);
        }
        OntologyTerm parent = ontology.findTerm(parentTermId);
        if (parent == null) {
            throw new IllegalArgumentException("Term not found: " + parentTermId);
        }

        int i = 0;
        for (OntologyTerm childTerm : terms) {
            if (childTerm.getId() == null) {
                childTerm.setId(getNewId());
            }
            if (index != null) {
                parent.insertChild(index + i, childTerm);
            }
            else {
                parent.addChild(childTerm);
            }
            i++;
        }
        log.debug("Adding " + terms.size() + " terms to " + parent.getName());
        saveImpl(subjectKey, ontology);
        return getDomainObject(subjectKey, ontology);
    }

    public Ontology removeTerm(String subjectKey, Long ontologyId, Long parentTermId, Long termId) throws Exception {

        Ontology ontology = getDomainObject(subjectKey, Ontology.class, ontologyId);
        if (ontology == null) {
            throw new IllegalArgumentException("Ontology not found: " + ontologyId);
        }
        OntologyTerm parent = ontology.findTerm(parentTermId);
        if (parent.getTerms() == null) {
            throw new Exception("Term has no children: " + parentTermId);
        }

        OntologyTerm removed = null;
        for (Iterator<OntologyTerm> iterator = parent.getTerms().iterator(); iterator.hasNext();) {
            OntologyTerm child = iterator.next();
            if (child != null && child.getId() != null && child.getId().equals(termId)) {
                removed = child;
                iterator.remove();
                break;
            }
        }
        if (removed == null) {
            throw new Exception("Could not find term to remove: " + termId);
        }
        log.info("Removing term '{}' from '{}'", removed.getName(), parent.getName());
        saveImpl(subjectKey, ontology);
        return getDomainObject(subjectKey, ontology);
    }

    public TreeNode reorderChildren(String subjectKey, TreeNode treeNodeArg, int[] order) throws Exception {

        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode == null) {
            throw new IllegalArgumentException("Tree node not found: " + treeNodeArg.getId());
        }
        if (!treeNode.hasChildren()) {
            log.warn("Tree node has no children to reorder: " + treeNode.getId());
            return treeNode;
        }

        List<Reference> references = new ArrayList<>(treeNode.getChildren());

        if (references.size() != order.length) {
            throw new IllegalArgumentException("Order array must be the same size as the child array (" + order.length + "!=" + references.size() + ")");
        }

        if (log.isTraceEnabled()) {
            log.trace("{} has the following references: ", treeNode.getName());
            for (Reference reference : references) {
                log.trace("  {}#{}", reference.getTargetClassName(), reference.getTargetId());
            }
            log.trace("They should be put in this ordering: ");
            for (int i = 0; i < order.length; i++) {
                log.trace("  {} -> {}", i, order[i]);
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
        for (Reference ref : reordered) {
            treeNode.getChildren().add(ref);
        }
        for (Reference ref : references) {
            if (ref != null) {
                log.warn("Adding broken ref to collection " + ref.getTargetClassName() + " at the end");
                treeNode.getChildren().add(ref);
            }
        }

        if (references.size() != originalSize) {
            throw new IllegalStateException("Reordered children have new size " + references.size() + " (was " + originalSize + ")");
        }

        log.info("Reordering children of TreeNode#" , treeNode.getId());
        saveImpl(subjectKey, treeNode);
        return getDomainObject(subjectKey, treeNode);
    }

    public List<DomainObject> getChildren(String subjectKey, TreeNode treeNode) {
        return getDomainObjects(subjectKey, treeNode.getChildren());
    }

    public TreeNode addChildren(String subjectKey, TreeNode treeNodeArg, Collection<Reference> references) throws Exception {
        return addChildren(subjectKey, treeNodeArg, references, null);
    }

    public TreeNode addChildren(String subjectKey, TreeNode treeNodeArg, Collection<Reference> references, Integer index) throws Exception {
        if (references == null) {
            throw new IllegalArgumentException("Cannot add null children");
        }
        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode == null) {
            throw new IllegalArgumentException("Tree node not found: " + treeNodeArg.getId());
        }
        Set<String> refs = new HashSet<>();
        for (Reference reference : treeNode.getChildren()) {
            refs.add(reference.toString());
        }
        int i = 0;
        for (Reference ref : references) {
            if (ref.getTargetId() == null) {
                throw new IllegalArgumentException("Cannot add child without an id");
            }
            if (ref.getTargetClassName() == null) {
                throw new IllegalArgumentException("Cannot add child without a target class name");
            }
            if (refs.contains(ref.toString())) {
                log.trace("Tree node "+treeNode.getId()+" already contains " + ref);
                continue;
            }
            if (index != null) {
                treeNode.insertChild(index + i, ref);
            }
            else {
                treeNode.addChild(ref);
            }
            i++;
        }
        log.info("Adding {} children to TreeNode#{}",i,treeNode.getId());
        saveImpl(subjectKey, treeNode);
        return getDomainObject(subjectKey, treeNode);
    }

    public TreeNode removeChildren(String subjectKey, TreeNode treeNodeArg, Collection<Reference> references) throws Exception {
        if (references == null) {
            throw new IllegalArgumentException("Cannot remove null children");
        }
        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode == null) {
            throw new IllegalArgumentException("Tree node not found: " + treeNodeArg.getId());
        }
        for (Reference ref : references) {
            if (ref.getTargetId() == null) {
                throw new IllegalArgumentException("Cannot add child without an id");
            }
            if (ref.getTargetClassName() == null) {
                throw new IllegalArgumentException("Cannot add child without a target class name");
            }
            treeNode.removeChild(ref);
        }
        log.info("Removing " + references.size() + " objects from " + treeNode.getName());
        saveImpl(subjectKey, treeNode);
        return getDomainObject(subjectKey, treeNode);
    }

    public TreeNode removeReference(String subjectKey, TreeNode treeNodeArg, Reference reference) throws Exception {
        TreeNode treeNode = getDomainObject(subjectKey, TreeNode.class, treeNodeArg.getId());
        if (treeNode == null) {
            throw new IllegalArgumentException("Tree node not found: " + treeNodeArg.getId());
        }
        if (treeNode.hasChildren()) {
            for (Iterator<Reference> i = treeNode.getChildren().iterator(); i.hasNext();) {
                Reference iref = i.next();
                if (iref.equals(reference)) {
                    i.remove();
                }
            }
            log.info("Removing reference from '{}'", treeNode.getName());
            saveImpl(subjectKey, treeNode);
        }
        return getDomainObject(subjectKey, treeNode);
    }

    public <T extends DomainObject> T updateProperty(String subjectKey, Class<T> clazz, Long id, String propName, Object propValue) {
        return (T) updateProperty(subjectKey, clazz.getName(), id, propName, propValue);
    }

    public DomainObject updateProperty(String subjectKey, String className, Long id, String propName, Object propValue) {
        Class<? extends DomainObject> clazz = DomainUtils.getObjectClassByName(className);
        DomainObject domainObject = getDomainObject(subjectKey, clazz, id);
        try {
            set(domainObject, propName, propValue);
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not update object attribute " + propName, e);
        }
        String collectionName = DomainUtils.getCollectionName(className);
        MongoCollection collection = getCollectionByName(collectionName);
        WriteResult wr = collection.update("{_id:#,writers:#}", domainObject.getId(), subjectKey).with("{$set: {" + propName + ":#, updatedDate:#}}", propValue, new Date());
        if (wr.getN() != 1) {
            log.warn("Could not update " + collectionName + "#" + domainObject.getId() + "." + propName);
        }
        return getDomainObject(subjectKey, domainObject);
    }

    public <T extends DomainObject> void deleteProperty(String ownerKey, Class<T> clazz, String propName) {
        String collectionName = DomainUtils.getCollectionName(clazz);
        MongoCollection collection = getCollectionByName(collectionName);
        WriteResult wr = collection.update("{ownerKey:#}", ownerKey).with("{$unset: {" + propName + ":\"\"}}");
        if (wr.getN() != 1) {
            log.warn("Could not delete property " + collectionName + "." + propName);
        }
    }

    public void changePermissions(String subjectKey, String className, Long id, String granteeKey, String rights, boolean grant) throws Exception {
        changePermissions(subjectKey, className, Arrays.asList(id), granteeKey, rights, grant);
    }

    public void changePermissions(String subjectKey, String className, Collection<Long> ids, String granteeKey, String rights, boolean grant) throws Exception {
        changePermissions(subjectKey, className, ids, Arrays.asList(granteeKey), rights, grant);
    }

    public void changePermissions(String subjectKey, String className, Long id, Collection<String> granteeKeys, String rights, boolean grant) throws Exception {
        changePermissions(subjectKey, className, Arrays.asList(id), granteeKeys, rights, grant);
    }

    public void changePermissions(String subjectKey, String className, Collection<Long> ids, Collection<String> granteeKeys, String rights, boolean grant) throws Exception {

        String collectionName = DomainUtils.getCollectionName(className);

        String op = grant ? "$addToSet" : "$pull";
        String iter = grant ? "$each" : "$in";
        String withClause = "{"+op+":{readers:{"+iter+":#},writers:{"+iter+":#}}}";

        List<String> readers = new ArrayList<>();
        List<String> writers = new ArrayList<>();
        for(String granteeKey : granteeKeys) {
            if (rights.contains("r")) readers.add(granteeKey);
            if (rights.contains("w")) writers.add(granteeKey);
        }

        log.debug("withClause: " + withClause);

        String logIds = ids.size() < 6 ? "" + ids : ids.size() + " ids";

        if (grant) {
            log.info("Granting {} permissions on all {} documents with ids {} to {}", rights, collectionName, logIds, granteeKeys);
        }
        else {
            log.info("Revoking {} permissions on all {} documents with ids {} to {}", rights, collectionName, logIds, granteeKeys);
        }

        MongoCollection collection = getCollectionByName(collectionName);
        WriteResult wr = collection.update("{_id:{$in:#},writers:#}", ids, subjectKey).multi().with(withClause, readers, writers);
        log.info("Changed permissions on {} documents",wr.getN());

        if (wr.getN() > 0) {
            if ("treeNode".equals(collectionName)) {
                log.info("Changing permissions on all members of the folders: {}", logIds);
                for (Long id : ids) {
                    TreeNode node = collection.findOne("{_id:#,writers:#}", id, subjectKey).as(TreeNode.class);
                    if (node == null) {
                        throw new IllegalArgumentException("Could not find folder with id=" + id);
                    }

                    if (node.hasChildren()) {
                        Multimap<String, Long> groupedIds = HashMultimap.create();
                        for (Reference ref : node.getChildren()) {
                            groupedIds.put(ref.getTargetClassName(), ref.getTargetId());
                        }

                        for (String refClassName : groupedIds.keySet()) {
                            Collection<Long> refIds = groupedIds.get(refClassName);
                            changePermissions(subjectKey, refClassName, refIds, granteeKeys, rights, grant);
                        }
                    }
                }
            }
            else if ("sample".equals(collectionName)) {

                log.info("Changing permissions on all fragments and lsms associated with samples: {}", logIds);

                List<String> sampleRefs = new ArrayList<>();
                for(Long id : ids) {
                    sampleRefs.add("Sample#"+id);
                }

                WriteResult wr1 = fragmentCollection.update("{sampleRef:{$in:#},writers:#}", sampleRefs, subjectKey).multi().with(withClause, readers, writers);
                log.info("Updated permissions on {} fragments", wr1.getN());

                WriteResult wr2 = imageCollection.update("{sampleRef:{$in:#},writers:#}", sampleRefs, subjectKey).multi().with(withClause, readers, writers);
                log.info("Updated permissions on {} lsms", wr2.getN());

            }
            if ("dataSet".equals(collectionName)) {
                log.info("Changing permissions on all samples and LSMs of the data sets: {}", logIds);
                for (Long id : ids) {
                    DataSet dataSet = collection.findOne("{_id:#,writers:#}", id, subjectKey).as(DataSet.class);
                    if (dataSet == null) {
                        throw new IllegalArgumentException("Could not find data set with id=" + id);
                    }

                    // Get all sample ids for a given data set
                    List<String> sampleRefs = new ArrayList<>();
                    List<Document> sampleIdDocs = m.getDatabase(databaseName)
                            .getCollection(DomainUtils.getCollectionName(Sample.class))
                            .find(new Document("dataSet",dataSet.getIdentifier()))
                            .projection(fields(include("_id")))
                            .into(new ArrayList());
                    for(Document doc : sampleIdDocs) {
                        sampleRefs.add("Sample#"+doc.get("_id"));
                    }

                    // This could just call changePermissions recursively, but batching is far more efficient.
                    WriteResult wr1 = sampleCollection.update("{dataSet:#,writers:#}", dataSet.getIdentifier(), subjectKey).multi().with(withClause, readers, writers);
                    log.info("Changed permissions on {} samples",wr1.getN());

                    WriteResult wr2 = fragmentCollection.update("{sampleRef:{$in:#},writers:#}", sampleRefs, subjectKey).multi().with(withClause, readers, writers);
                    log.info("Updated permissions on {} fragments", wr2.getN());

                    WriteResult wr3 = imageCollection.update("{sampleRef:{$in:#},writers:#}", sampleRefs, subjectKey).multi().with(withClause, readers, writers);
                    log.info("Updated permissions on {} lsms", wr3.getN());
                }
            }
        }
    }

    public void syncPermissions(String ownerKey, String className, Long id, DomainObject permissionTemplate) throws Exception {
        // TODO: this could be optimized to do both r/w at the same time
        changePermissions(ownerKey, className, id, permissionTemplate.getReaders(), "r", true);
        changePermissions(ownerKey, className, id, permissionTemplate.getWriters(), "w", true);
        // TODO: should be deleted if they dont exist in the permission template?
    }

    // Copy and pasted from ReflectionUtils in shared module
    private void set(Object obj, String attributeName, Object value) throws Exception {
        Class<?>[] argTypes = {value.getClass()};
        Object[] argValues = {value};
        String methodName = getAccessor("set", attributeName);
        obj.getClass().getMethod(methodName, argTypes).invoke(obj, argValues);
    }

    // Copy and pasted from ReflectionUtils in shared module
    private static String getAccessor(String prefix, String attributeName) {
        String firstChar = attributeName.substring(0, 1).toUpperCase();
        return prefix + firstChar + attributeName.substring(1);
    }

    public Long getNewId() {
        return TimebasedIdentifierGenerator.generateIdList(1).get(0);
    }

    public List<LineRelease> getLineReleases(String subjectKey) {
        Set<String> subjects = getSubjectSet(subjectKey);
        if (subjects == null) {
            return toList(releaseCollection.find().as(LineRelease.class));
        }
        else {
            return toList(releaseCollection.find("{readers:{$in:#}}", subjects).as(LineRelease.class));
        }
    }

    public LineRelease createLineRelease(String subjectKey, String name, Date releaseDate, Integer lagTimeMonths, List<String> dataSets) throws Exception {
        LineRelease release = new LineRelease();
        release.setName(name);
        release.setReleaseDate(releaseDate);
        release.setLagTimeMonths(lagTimeMonths);
        release.setDataSets(dataSets);
        return save(subjectKey, release);
    }

    public static void main(String[] args) throws Exception {
        
        String MONGO_SERVER_URL = "dev-mongodb";
        String MONGO_DATABASE = "jacs";
        DomainDAO dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE);

//        String owner = "user:rokickik";
//        for(Workspace workspace : dao.getWorkspaces(owner)) {
//            System.out.println(""+workspace.getName());
//            for(DomainObject topLevelObj : dao.getDomainObjects(owner, workspace.getChildren())) {
//                System.out.println("  "+topLevelObj.getName());
//                if (topLevelObj instanceof TreeNode) {
//                    for(DomainObject domainObject : dao.getDomainObjects(owner, ((TreeNode)topLevelObj).getChildren())) {
//                        System.out.println("    "+domainObject.getName());
//                    }
//                }
//            }
//        }

        dao.changePermissions("group:heberleinlab", DataSet.class.getSimpleName(), 1831437750079848537L, Arrays.asList("user:rokickik", "user:saffordt"), "r", false);
    }
}
