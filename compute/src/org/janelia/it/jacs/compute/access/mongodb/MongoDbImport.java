package org.janelia.it.jacs.compute.access.mongodb;

import java.io.File;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SubjectDAO;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.compute.access.large.MongoLargeOperations;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.compartments.Compartment;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.enums.AlignmentScoreType;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.enums.SampleImageType;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.EnumText;
import org.janelia.it.jacs.model.domain.ontology.Interval;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineError;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SampleCellCountingResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.screen.FlyLine;
import org.janelia.it.jacs.model.domain.screen.PatternMask;
import org.janelia.it.jacs.model.domain.screen.ScreenSample;
import org.janelia.it.jacs.model.domain.support.MongoUtils;
import org.janelia.it.jacs.model.domain.support.SAGEAttribute;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.jongo.MongoCollection;
import org.reflections.ReflectionUtils;

import com.google.common.collect.ComparisonChain;
import com.mongodb.WriteConcern;

/**
 * Data access to the MongoDB data store.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDbImport extends AnnotationDAO {

    private static final Logger logger = Logger.getLogger(MongoDbImport.class);

    private static String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
    private static String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");
    private static String MONGO_USERNAME = SystemConfigurationProperties.getString("MongoDB.Username");
    private static String MONGO_PASSWORD = SystemConfigurationProperties.getString("MongoDB.Password");

	protected static final int INSERTION_BATCH_SIZE = 1000;
	protected static final String ONTOLOGY_TERM_TYPES_PACKAGE = "org.janelia.it.jacs.model.domain.ontology";
	protected static final String NO_CONSENSUS_VALUE = "NO_CONSENSUS";
	protected static final boolean TRANSLATE_ENTITIES = true;
	protected static final boolean INSERT_ROGUE_ENTITIES = true;
	
    private static final String[] entityTranslationPriority = { EntityConstants.TYPE_SAMPLE, EntityConstants.TYPE_SCREEN_SAMPLE };

    protected DomainDAO dao;
	protected SubjectDAO subjectDao;
	
	// Cached collections
	protected MongoCollection subjectCollection;
    protected MongoCollection treeNodeCollection;
    protected MongoCollection objectSetCollection;
    protected MongoCollection dataSetCollection;
	protected MongoCollection sampleCollection;
    protected MongoCollection screenSampleCollection;
    protected MongoCollection patternMaskCollection;
    protected MongoCollection flyLineCollection;
    protected MongoCollection imageCollection;
    protected MongoCollection fragmentCollection;
    protected MongoCollection annotationCollection;
    protected MongoCollection ontologyCollection;
    protected MongoCollection compartmentSetCollection;
    protected MongoCollection alignmentBoardCollection;
    
    // Load state
    private MongoLargeOperations largeOp;
    private String genderConsensus = null;
    private Map<String,String> lsmJsonFiles = new HashMap<String,String>();
    protected Map<Long,Long> ontologyTermIdToOntologyId = new HashMap<Long,Long>();
    
    public MongoDbImport() throws UnknownHostException {
        super(logger);
        
        this.subjectDao = new SubjectDAO(log);
		this.dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
    	dao.getMongo().setWriteConcern(WriteConcern.UNACKNOWLEDGED);
    	
    	this.subjectCollection = dao.getCollectionByClass(Subject.class);
    	this.treeNodeCollection = dao.getCollectionByClass(TreeNode.class);
    	this.objectSetCollection = dao.getCollectionByClass(ObjectSet.class);
    	this.dataSetCollection = dao.getCollectionByClass(DataSet.class);
    	this.sampleCollection = dao.getCollectionByClass(Sample.class);
    	this.screenSampleCollection = dao.getCollectionByClass(ScreenSample.class);
    	this.patternMaskCollection = dao.getCollectionByClass(PatternMask.class);
    	this.flyLineCollection = dao.getCollectionByClass(FlyLine.class);
        this.imageCollection = dao.getCollectionByClass(Image.class);
        this.fragmentCollection = dao.getCollectionByClass(NeuronFragment.class);
        this.annotationCollection = dao.getCollectionByClass(Annotation.class);
        this.ontologyCollection = dao.getCollectionByClass(Ontology.class);
        this.compartmentSetCollection = dao.getCollectionByClass(CompartmentSet.class);
        this.alignmentBoardCollection = dao.getCollectionByClass(AlignmentBoard.class);
    }

    public void loadAllEntities() throws DaoException {

        log.info("Building disk-based SAGE property map");
        this.largeOp = new MongoLargeOperations(dao, this);
        largeOp.buildSageImagePropMap();

        log.info("Building LSM property map");
        buildLsmAttributeMap();
        
        log.info("Loading data into MongoDB");
        getSession().setFlushMode(FlushMode.MANUAL);
        
        long startAll = System.currentTimeMillis(); 

        log.info("Adding subjects");
        loadSubjects();

        log.info("Adding samples");
        // TODO: handle curated neurons
        // TODO: handle pattern mask results in samples (knappj)
        loadSamples();

        log.info("Adding data sets");
        loadDataSets();
        
        log.info("Adding fly lines");
        loadFlyLines();
        
        log.info("Adding screen data");
        loadScreenData();
        
        log.info("Adding compartment sets");
        loadCompartmentSets();

        log.info("Adding alignment boards");
        loadAlignmentBoards();

        log.info("Adding folders");
        loadWorkspaces();

        log.info("Adding ontologies");
        loadOntologies(); // must come before loadAnnotations to populate the term maps

        log.info("Adding annotations");
        loadAnnotations();
        
        // TODO: add large image viewer workspaces and associated entities
        
        log.info("Loading MongoDB took "+((double)(System.currentTimeMillis()-startAll)/1000/60/60)+" hours");
    }

    Map<String,LsmSageAttribute> lsmSageAttrs = new HashMap<>();
    
    private void buildLsmAttributeMap() {
        for (Field field : ReflectionUtils.getAllFields(LSMImage.class)) {
            SAGEAttribute sageAttribute = field.getAnnotation(SAGEAttribute.class);
            if (sageAttribute!=null) {
                LsmSageAttribute attr = new LsmSageAttribute();
                attr.cvName = sageAttribute.cvName();
                attr.termName = sageAttribute.termName();
                attr.field = field;
                log.info("  "+attr.getKey()+" -> LsmImage."+field.getName());
                lsmSageAttrs.put(attr.getKey(), attr);
            }
        }
    }
    
    private class LsmSageAttribute {
        String cvName;
        String termName;
        Field field;
        public String getKey() {
            return cvName+"_"+termName;
        }
    }
    
    private void resetSession() {
        long start = System.currentTimeMillis();
        getSession().flush();
        getSession().clear();
        log.trace("Flushing and clearing the session took "+(System.currentTimeMillis()-start)+" ms");
    }
    
    
    /* SUBJECT */
    
    private void loadSubjects() {
        for (org.janelia.it.jacs.model.user_data.Subject subject : subjectDao.getSubjects()) {
            Subject newSubject = new Subject();
            newSubject.setId(subject.getId());
            newSubject.setKey(subject.getKey());
            newSubject.setName(subject.getName());
            newSubject.setEmail(subject.getEmail());
            newSubject.setFullName(subject.getFullName());
            newSubject.setGroups(new HashSet<String>(getGroupKeysForUsernameOrSubjectKey(subject.getKey())));
            subjectCollection.insert(newSubject);
        }
    }
    

	/* FLY LINES */
    
    private void loadFlyLines() throws DaoException {
        long start = System.currentTimeMillis();
        Deque<Entity> lines = new LinkedList<Entity>(getEntitiesByTypeName(null, EntityConstants.TYPE_FLY_LINE));
        resetSession();
        int loaded = loadFlyLines(lines);
        log.info("Loading " + loaded + " fly lines took " + (System.currentTimeMillis() - start) + " ms");
    }
    
    private int loadFlyLines(Deque<Entity> flyLines) {

    	int loaded = 0;
        for(Iterator<Entity> i = flyLines.iterator(); i.hasNext(); ) {
            Entity flyLineEntity = i.next();
            
            try {
                long start = System.currentTimeMillis();
                FlyLine flyLine = getFlyLineObject(flyLineEntity);
                if (flyLine!=null) {
                    flyLineCollection.insert(flyLine);
                }
                
                // Free memory by releasing the reference to this entire entity tree
                i.remove();
                resetSession();

                if (flyLine!=null) {
                    log.info("  Loading "+flyLineEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                    loaded++;
                }
                else {
                    log.info("  Failure loading "+flyLineEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                }
            }
            catch (Throwable e) {
                log.error("Error loading fly line "+flyLineEntity.getId(),e);
            }
        }
        
        return loaded;
    }

    private FlyLine getFlyLineObject(Entity flyLineEntity) throws Exception {

        populateChildren(flyLineEntity);
        
        FlyLine flyline = new FlyLine();
        flyline.setId(flyLineEntity.getId());
        flyline.setName(flyLineEntity.getName());
        flyline.setOwnerKey(flyLineEntity.getOwnerKey());
        flyline.setReaders(getSubjectKeysWithPermission(flyLineEntity, "r"));
        flyline.setWriters(getSubjectKeysWithPermission(flyLineEntity, "w"));
        flyline.setCreationDate(flyLineEntity.getCreationDate());
        flyline.setUpdatedDate(flyLineEntity.getUpdatedDate());
        flyline.setSplitPart(flyLineEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART));
        
        String robotId = flyLineEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ROBOT_ID);
        if (robotId!=null) {
            flyline.setRobotId(Integer.parseInt(robotId));
        }
        
        Entity balanced = flyLineEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_BALANCED_FLYLINE);
        if (balanced!=null) {
            flyline.setBalancedLine(getReference(balanced));
        }

        Entity original = flyLineEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_ORIGINAL_FLYLINE);
        if (original!=null) {
            flyline.setOriginalLine(getReference(original));
        }

        Entity representative = flyLineEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
        if (representative!=null) {
            flyline.setRepresentativeScreenSample(getReference(representative));
        }
        
        return flyline;
    }
    

    /* DATA SETS */
    

    private void loadDataSets() throws DaoException {
        long start = System.currentTimeMillis();
        Deque<Entity> dataSets = new LinkedList<Entity>(getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET));
        resetSession();
        int loaded = loadDataSets(dataSets);
        log.info("Loading " + loaded + " data sets took " + (System.currentTimeMillis() - start) + " ms");
    }
    
    private int loadDataSets(Deque<Entity> dataSets) {

    	int loaded = 0;
        for(Iterator<Entity> i = dataSets.iterator(); i.hasNext(); ) {
            Entity dataSetEntity = i.next();
            
            try {
                long start = System.currentTimeMillis();
                DataSet dataSet = getDataSetObject(dataSetEntity);
                if (dataSet!=null) {
                    dataSetCollection.insert(dataSet);
                }
                
                // Free memory by releasing the reference to this entire entity tree
                i.remove();
                resetSession();

                if (dataSet!=null) {
                    log.info("  Loading "+dataSetEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                    loaded++;
                }
                else {
                    log.info("  Failure loading "+dataSetEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                }
            }
            catch (Throwable e) {
                log.error("Error loading dataset "+dataSetEntity.getId(),e);
            }
        }
        
        return loaded;
    }

    private DataSet getDataSetObject(Entity dataSetEntity) {
    	DataSet dataset = new DataSet();
        dataset.setId(dataSetEntity.getId());
        dataset.setName(dataSetEntity.getName());
        dataset.setOwnerKey(dataSetEntity.getOwnerKey());
        dataset.setReaders(getSubjectKeysWithPermission(dataSetEntity, "r"));
        dataset.setWriters(getSubjectKeysWithPermission(dataSetEntity, "w"));
        dataset.setCreationDate(dataSetEntity.getCreationDate());
        dataset.setUpdatedDate(dataSetEntity.getUpdatedDate());
        dataset.setIdentifier(dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER));
        
    	if (dataSetEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC)!=null) {
    		dataset.setSageSync(true);
    	}
    	
        String sampleFileType = dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAMPLE_IMAGE_TYPE);
        if (sampleFileType!=null) {
            dataset.setSampleImageType(SampleImageType.valueOf(sampleFileType));
        }
        
        String sampleNamePattern = dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAMPLE_NAME_PATTERN);
        if (sampleNamePattern!=null) {
            dataset.setSampleNamePattern(sampleNamePattern);
        }
        
        String pipelineProcess = dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
        if (pipelineProcess!=null) {
        	List<String> processNames = new ArrayList<String>();
        	for(String processName : pipelineProcess.split(",")) {
        		if (!StringUtils.isEmpty(processName)) {
        			processNames.add(processName);
        		}
        	}
        	if (!processNames.isEmpty()) {
        		dataset.setPipelineProcesses(processNames);
        	}
        }
        
        return dataset;
    }
    
    /* SAMPLES */
    
    private void loadSamples() {
        for (org.janelia.it.jacs.model.user_data.Subject subject : subjectDao.getSubjects()) {
            String subjectKey = subject.getKey();
            try { 
                loadSamples(subjectKey);
            }
            catch (Exception e) {
                log.error("Error loading samples for " + subjectKey, e);
            }
        }
    }

    private void loadSamples(String subjectKey) throws Exception {
        long start = System.currentTimeMillis();
        Deque<Entity> samples = new LinkedList<Entity>(getUserEntitiesByTypeName(subjectKey, EntityConstants.TYPE_SAMPLE));
        log.info("Got "+samples.size()+" samples for "+subjectKey);
        resetSession();
        int loaded = loadSamples(samples);
        log.info("Loading " + loaded + " samples for " + subjectKey + " took "
                + (System.currentTimeMillis() - start) + " ms");
    }
    
    private int loadSamples(Deque<Entity> samples) {

        int loaded = 0;
        for(Iterator<Entity> i = samples.iterator(); i.hasNext(); ) {
            Entity sampleEntity = i.next();
            // Skip sub-samples
            if (sampleEntity.getName().contains("~")) continue;
            // Skip retired samples
            if (sampleEntity.getName().endsWith("-Retired")) continue;
            
            try {
                long start = System.currentTimeMillis();
                Sample sample = getSampleObject(sampleEntity);
                if (sample!=null) {
                    sampleCollection.insert(sample);
                }
                
                // Free memory by releasing the reference to this entire entity tree
                i.remove();
                resetSession();

                if (sample!=null) {
                    log.info("  Loading "+sampleEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                    loaded++;
                }
                else {
                    log.info("  Failure loading "+sampleEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                }
            }
            catch (Throwable e) {
                log.error("Error loading sample "+sampleEntity.getId(),e);
            }
        }
        
        return loaded;
    }

    private Sample getSampleObject(Entity sampleEntity) throws Exception {
        
        if (sampleEntity.getName().contains("~")) {
            log.warn("  Cannot process sub-sample as sample: "+sampleEntity.getId());
            return null;
        }
        
        if (sampleEntity.getEntityData()==null) {
            log.warn("  Cannot process sample with null entityData: "+sampleEntity.getId());
            return null;
        }
        
        populateChildren(sampleEntity);
        
        Sample sample = new Sample();
                
        sample.setId(sampleEntity.getId());
        sample.setName(sampleEntity.getName());
        sample.setOwnerKey(sampleEntity.getOwnerKey());
        sample.setReaders(getSubjectKeysWithPermission(sampleEntity, "r"));
        sample.setWriters(getSubjectKeysWithPermission(sampleEntity, "w"));
        sample.setCreationDate(sampleEntity.getCreationDate());
        sample.setUpdatedDate(sampleEntity.getUpdatedDate());
        sample.setAge(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_AGE));
        sample.setChanSpec(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
        sample.setDataSet(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER));
        sample.setEffector(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_EFFECTOR));
        sample.setLine(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_LINE));
        sample.setSlideCode(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SLIDE_CODE));
        sample.setStatus(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS));
        sample.setVisited(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISITED));
        
        Map<String, ObjectiveSample> objectiveSamples = new HashMap<String, ObjectiveSample>();
        
        // Reset consensus values
        this.genderConsensus = null;
        
        // Reset JSON file map
        lsmJsonFiles.clear();
        
        List<Entity> objSampleEntities = EntityUtils.getChildrenOfType(sampleEntity, EntityConstants.TYPE_SAMPLE);
        if (objSampleEntities.isEmpty()) {
            String objective = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
            if (objective==null) {
                objective = "63x";
            }
            ObjectiveSample os = getObjectiveSampleObject(sampleEntity, sampleEntity, objective);
            if (os!=null) {
                objectiveSamples.put(objective, os);
            }
        }
        else {
            for (Entity objSampleEntity : objSampleEntities) {
                String objective = objSampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
                if (objective==null) {
                    objective = "63x";
                }
                ObjectiveSample os = getObjectiveSampleObject(sampleEntity, objSampleEntity, objective);
                if (os!=null) {
                    objectiveSamples.put(objective, os);
                }
            }
        }
        
        if (objectiveSamples.isEmpty()) {
            log.warn("  Sample has no objectives: "+sampleEntity.getId());
        }
        else {
            sample.setObjectives(objectiveSamples);    
        }
        
        // Set derived consensus values
        if (!StringUtils.isEmpty(genderConsensus)) {
            sample.setGender(sanitizeGender(genderConsensus));
        }
        
        return sample;
    }

    private ObjectiveSample getObjectiveSampleObject(Entity parentSampleEntity, Entity sampleEntity, String sampleObjective) throws Exception {

        populateChildren(sampleEntity);
        
        ObjectiveSample sample = new ObjectiveSample();
        
        Entity supportingDataEntity = EntityUtils.getSupportingData(sampleEntity);
        if (supportingDataEntity==null) {
            log.warn("  Sample has no supporting data: "+sampleEntity.getId());
            return null;
        }

        List<SamplePipelineRun> runs = new ArrayList<SamplePipelineRun>();
        for(Entity runEntity : EntityUtils.getChildrenOfType(sampleEntity, EntityConstants.TYPE_PIPELINE_RUN)) {
            populateChildren(runEntity);
            
            List<PipelineResult> results = new ArrayList<PipelineResult>();
            
            for(Entity resultEntity : EntityUtils.getChildrenForAttribute(runEntity, EntityConstants.ATTRIBUTE_RESULT)) {
                populateChildren(resultEntity);
                
                if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {

                    List<PipelineResult> sprResults = new ArrayList<PipelineResult>();
                    for(Entity separationEntity : EntityUtils.getChildrenOfType(resultEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                        sprResults.add(getNeuronSeparation(parentSampleEntity, separationEntity));
                    }
                    
                    SampleProcessingResult result = getSampleProcessingResult(resultEntity);
                    result.setObjective(sampleObjective);
                    
                    if (!sprResults.isEmpty()) {
                        result.setResults(sprResults);
                    }
                    
                    results.add(result);
                }
                else if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_CELL_COUNTING_RESULT)) {
                    SampleCellCountingResult result = getSampleCellCountingResult(resultEntity);
                    results.add(result);
                }
                else if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
                    
                    Map<String,PipelineResult> nsResultMap = new HashMap<String,PipelineResult>();
                    for(Entity separationEntity : EntityUtils.getChildrenOfType(resultEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                        String objective = separationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
                        
                        if (StringUtils.isEmpty(objective)) objective = "";
                        nsResultMap.put(objective,getNeuronSeparation(parentSampleEntity, separationEntity));
                    }
                    
                    Entity verifyMovie = null;
                    List<Entity> resultImages = new ArrayList<Entity>();
                    
                    Entity resultSupportingData = EntityUtils.getSupportingData(resultEntity);
                    populateChildren(resultSupportingData);
                    List<Entity> children = resultSupportingData.getOrderedChildren();
                    for(Entity resultFile : children) {
                        if (resultFile.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
                            if (!resultFile.getName().startsWith("Aligned")) continue;
                            resultImages.add(resultFile);
                        }
                        else if (resultFile.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
                            verifyMovie = resultEntity;
                        }
                    }
                    
                    for(Entity imageEntity : resultImages) {
                        Entity movieEntity = null;
                        String objective = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
                        if (resultImages.size()==1 || "63x".equals(objective) || "".equals(objective)) {
                            movieEntity = verifyMovie;
                        }

                        PipelineResult ns = nsResultMap.get(objective);
                        if (ns==null) {
                            ns = nsResultMap.get("");
                        }
                        List<PipelineResult> sprResults = new ArrayList<PipelineResult>();
                        if (ns!=null) {
                            sprResults.add(ns);
                        }
                        
                        SampleAlignmentResult alignmentResult = getAlignmentResult(resultEntity, imageEntity, movieEntity);
                        if (imageEntity.getName().contains("VNC")) {
                            alignmentResult.setAnatomicalArea("VNC");    
                        }
                        else {
                            alignmentResult.setAnatomicalArea("Brain");
                        }
                        
                        if (!sprResults.isEmpty()) {
                            alignmentResult.setResults(sprResults);
                        }
                        
                        results.add(alignmentResult);
                    }
                }
                
            }
            
            SamplePipelineRun run = new SamplePipelineRun();
            run.setName(runEntity.getName());
            run.setCreationDate(runEntity.getCreationDate());
            run.setPipelineProcess(runEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS));
            run.setPipelineVersion(1);
            
            Entity errorEntity = EntityUtils.findChildWithType(runEntity, EntityConstants.TYPE_ERROR);
            if (errorEntity!=null) {
                PipelineError error = new PipelineError();
                error.setFilepath(errorEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                run.setError(error);
            }

            if (!results.isEmpty()) {
                run.setResults(results);
            }
            
            runs.add(run);
        }

        sample.setPipelineRuns(runs);

        List<SampleTile> tiles = new ArrayList<SampleTile>();
        populateChildren(supportingDataEntity);
        for(Entity tileEntity : supportingDataEntity.getOrderedChildren()) {

            Map<FileType,String> images = new HashMap<FileType,String>();
            addImage(images,FileType.ReferenceMip,tileEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE));
            addImage(images,FileType.SignalMip,tileEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE));
            
            List<LSMImage> lsmImages = new ArrayList<LSMImage>();
            List<Reference> lsmReferences = new ArrayList<Reference>();
            
            populateChildren(tileEntity);
            for(Entity lsmEntity : EntityUtils.getChildrenOfType(tileEntity, EntityConstants.TYPE_LSM_STACK)) {
                LSMImage lsmImage = getLSMImage(parentSampleEntity, lsmEntity);
                if (lsmImage!=null) {
                    // Denormalize tile MIPs into the LSMs
                    lsmImage.getFiles().putAll(images);
                    lsmImages.add(lsmImage);
                    lsmReferences.add(getReference(lsmEntity));
                }
            }

            imageCollection.insert(lsmImages.toArray());
            
            SampleTile tile = new SampleTile();
            tile.setName(tileEntity.getName());
            tile.setAnatomicalArea(tileEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA));
            tile.setLsmReferences(lsmReferences);
            tile.setFiles(images);
            tiles.add(tile);
        }
        
        sample.setTiles(tiles);
        
        return sample;
    }
    
    private void addStackFiles(Entity imageEntity, Map<FileType,String> files, HasFilepath result) throws Exception {
        addImage(files,FileType.Stack,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)));
        addImage(files,FileType.ReferenceMip,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE)));
        addImage(files,FileType.SignalMip,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE)));
        populateChildren(imageEntity);
        Entity slightlyLossy = imageEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_SLIGHTLY_LOSSY_IMAGE);
        if (slightlyLossy!=null) {
            addImage(files,FileType.VisuallyLosslessStack,getRelativeFilename(result,slightlyLossy.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)));
        }
        Entity fast3dImage = imageEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE);
        if (fast3dImage!=null) {
            addImage(files,FileType.FastStack,getRelativeFilename(result,fast3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)));
        }
    }
    
    private SampleProcessingResult getSampleProcessingResult(Entity resultEntity) throws Exception {
        SampleProcessingResult result = new SampleProcessingResult();
        result.setName(resultEntity.getName());
        result.setCreationDate(resultEntity.getCreationDate());
        result.setFilepath(resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        String area = resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
        if (!StringUtils.isEmpty(area)) {
            result.setAnatomicalArea(area);
        }
        else {
            result.setAnatomicalArea("Brain");
        }
        
        Map<FileType,String> files = new HashMap<FileType,String>();
        
        Entity imageEntity = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        if (imageEntity!=null) {
            result.setImageSize(sanitizeCSV(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)));
            result.setOpticalResolution(sanitizeCSV(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)));
            result.setChannelColors(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS));
            result.setChannelSpec(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
            addStackFiles(imageEntity, files, result);
        }
        else {
            log.warn("  Sample processing result has no default stack: "+resultEntity.getId());
        }

        Entity supportingDataEntity = EntityUtils.getSupportingData(resultEntity);
        if (supportingDataEntity!=null) {
        	for(Entity child : supportingDataEntity.getChildren()) {
        		String childName = child.getName();
        		String childFilepath = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        		if (childName.endsWith("lsm.json")) {
        			String name = ArchiveUtils.getDecompressedFilepath(childName);
        			lsmJsonFiles.put(name, childFilepath);
        		}
        	}
        }

        if (!files.isEmpty()) result.setFiles(files);
        
        return result;
    }

    private SampleCellCountingResult getSampleCellCountingResult(Entity resultEntity) {
    	SampleCellCountingResult result = new SampleCellCountingResult();
    	result.setName(resultEntity.getName());
        result.setCreationDate(resultEntity.getCreationDate());
        result.setFilepath(resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        
        String cellCountStr = resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CELL_COUNT);
        if (cellCountStr!=null) {
        	if ("UNK".equals(cellCountStr) || "".equals(cellCountStr)) {
        		result.setCellCount(null);
        	}
        	else {
	        	try {
	        		result.setCellCount(Integer.parseInt(cellCountStr));	
	        	}
	        	catch (NumberFormatException e) {
	        		log.info("Could not parse cell count: "+cellCountStr);
	        	}
        	}
        }

        Entity supportingDataEntity = EntityUtils.getSupportingData(resultEntity);
        if (supportingDataEntity!=null) {
        	
        	Map<FileType,String> files = new HashMap<FileType,String>();
        	
        	for(Entity child : supportingDataEntity.getChildren()) {
        		String childName = child.getName();
        		String childFilepath = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        		
        		if ("cellCounterPlan.txt".equals(childName)) {
        			files.put(FileType.CellCountPlan, getRelativeFilename(result,childFilepath));
        		}
        		else if (childName.endsWith("CellCounterReport.txt")) {
        			files.put(FileType.CellCountReport, getRelativeFilename(result,childFilepath));	
        		}
        		else if (childName.contains("CellCounterImage")) { 
	        		if (childName.endsWith(".v3dpbd")) {
	        			files.put(FileType.CellCountImage, getRelativeFilename(result,childFilepath));	
	        		}
	        		else if (childName.endsWith("_signal.png")) {
	        			files.put(FileType.CellCountImageMip, getRelativeFilename(result,childFilepath));	
	        		}
        		}
        		else if (childName.endsWith(".v3dpbd")) {
        			files.put(FileType.CellCountStack, getRelativeFilename(result,childFilepath));	
        		}
        		else if (childName.endsWith("_signal.png")) {
        			files.put(FileType.CellCountStackMip, getRelativeFilename(result,childFilepath));	
        		}
        	}
            if (!files.isEmpty()) result.setFiles(files);
        }
        
        return result;
    }
    
    private SampleAlignmentResult getAlignmentResult(Entity alignmentEntity, Entity imageEntity, Entity movieEntity) throws Exception {
        
        SampleAlignmentResult result = new SampleAlignmentResult();
        result.setName(alignmentEntity.getName());
        result.setCreationDate(imageEntity.getCreationDate());
        result.setAlignmentSpace(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE));
        result.setBoundingBox(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_BOUNDING_BOX));
        result.setChannelColors(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS));
        result.setChannelSpec(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
        result.setFilepath(alignmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        result.setImageSize(sanitizeCSV(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)));
        result.setObjective(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE));
        result.setOpticalResolution(sanitizeCSV(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)));
        
        Map<AlignmentScoreType,String> scores = new HashMap<AlignmentScoreType,String>();
        String qiScore = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE);
        if (!StringUtils.isEmpty(qiScore)) {
        	scores.put(AlignmentScoreType.Qi,qiScore);
        }
        String qiScores = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORES);
        if (!StringUtils.isEmpty(qiScores)) {
        	scores.put(AlignmentScoreType.QiByRegion,qiScores);
        }
        String qmScore = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_MODEL_VIOLATION_SCORE);
        if (!StringUtils.isEmpty(qmScore)) {
        	scores.put(AlignmentScoreType.ModelViolation,qmScore);
        }
        String incScore = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORE);
        if (!StringUtils.isEmpty(incScore)) {
        	scores.put(AlignmentScoreType.Inconsistency,incScore);
        }
        String incScores = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORES);
        if (!StringUtils.isEmpty(incScores)) {
        	scores.put(AlignmentScoreType.InconsistencyByRegion,incScores);
        }
        String nccScore = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_NCC_SCORE);
        if (!StringUtils.isEmpty(qmScore)) {
        	scores.put(AlignmentScoreType.NormalizedCrossCorrelation,nccScore);
        }
        if (!scores.isEmpty()) result.setScores(scores);
        
        Map<FileType,String> files = new HashMap<FileType,String>();
        addStackFiles(imageEntity, files, result);
        if (movieEntity!=null) {
            addImage(files,FileType.AlignmentVerificationMovie,getRelativeFilename(result,movieEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)));
        }
        if (!files.isEmpty()) result.setFiles(files);
        
        return result;
    }

    private LSMImage getLSMImage(Entity sampleEntity, Entity lsmEntity) throws Exception {

        LSMImage lsm = (LSMImage)getImage(lsmEntity);
        // An LSM file must have a stack file path
        String filepath = lsm.getFiles().get(FileType.Stack);
        if (filepath==null) {
            log.error("LSM cannot be imported because it has no filepath: "+lsmEntity.getId());
            return null;
        }
        
        String name = ArchiveUtils.getDecompressedFilepath(lsm.getName());
        String jsonFilepath = lsmJsonFiles.get(name);
        if (jsonFilepath!=null) {
            addImage(lsm.getFiles(),FileType.LsmMetadata,jsonFilepath);
        }
        
        if (sampleEntity!=null) lsm.setSample(getReference(sampleEntity));
        lsm.setAge(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_AGE));
        lsm.setAnatomicalArea(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA));
        lsm.setChannelColors(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS));
        lsm.setChannelDyeNames(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES));
        lsm.setChanSpec(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
        lsm.setEffector(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_EFFECTOR));
        lsm.setLine(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_LINE));
        lsm.setMountingProtocol(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_MOUNTING_PROTOCOL));
        lsm.setTissueOrientation(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_TISSUE_ORIENTATION));
        lsm.setObjective(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE));
        lsm.setSlideCode(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SLIDE_CODE));
        
        lsm.setGender(sanitizeGender(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_GENDER)));
        if (genderConsensus==null) {
        	genderConsensus = lsm.getGender();
        }
        else if (!genderConsensus.equals(lsm.getGender())) {
        	genderConsensus = NO_CONSENSUS_VALUE;
        }
        
        String sageId = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_ID);
        if (sageId!=null) {
            lsm.setSageId(Integer.parseInt(sageId));
        }

        Map<String,Object> sageProps = (Map<String,Object>)largeOp.getValue(LargeOperations.SAGE_IMAGEPROP_MAP, sageId);
        if (sageProps==null) {
            log.warn("Cannot find LSM#"+lsm.getId()+" in SAGE, with SAGE Id "+sageId);
        }
        else {
            for(String key : lsmSageAttrs.keySet()) {
                try {
                    LsmSageAttribute attr = lsmSageAttrs.get(key);
                    Object value = sageProps.get(key);
                    Object trueValue = null;
                    if (value!=null) {
                        Class<?> fieldType = attr.field.getType();
                        // Convert the incoming value from SAGE to the correct type in our domain model
                        if (fieldType.equals(String.class)) {
                            trueValue = value.toString();
                        }
                        else if (fieldType.equals(Date.class)) {
                            // Dates are represented as java.sql.Timestamps, which is a subclass of Date, 
                            // so this should be safe to assign directly
                            trueValue = value;
                        }
                        else if (fieldType.equals(Integer.class)) {
                            trueValue = Integer.parseInt(value.toString());
                        }
                        else if (fieldType.equals(Boolean.class)) {
                            if (value instanceof Boolean) {
                                trueValue = value;
                            }
                            else if (value instanceof Integer) {
                                trueValue = new Boolean(((Integer)value)!=0);
                            }
                            else {
                                throw new Exception("Cannot parse "+value+" into a Boolean");
                            }
                        }
                        else {
                            // This might take care of future types we may not have anticipated
                            trueValue = value;
                        }
                    }
                    org.janelia.it.jacs.shared.utils.ReflectionUtils.setFieldValue(lsm, attr.field, trueValue);
                }
                catch (Exception e) {
                    log.error("Error setting SAGE attribute value "+key+" for LSM#"+lsm.getId(),e);
                }
            }
        }
        
        return lsm;
    }

    private Image getImage(Entity imageEntity) throws Exception {

    	Image image = null;
    	if (imageEntity.getEntityTypeName().equals(EntityConstants.TYPE_LSM_STACK)) {
    		image = new LSMImage();
    	}
    	else {
    		image = new Image();
    	}
    	
    	image.setId(imageEntity.getId());
        image.setName(imageEntity.getName());
        image.setOwnerKey(imageEntity.getOwnerKey());
        image.setReaders(getSubjectKeysWithPermission(imageEntity, "r"));
        image.setWriters(getSubjectKeysWithPermission(imageEntity, "w"));
        image.setCreationDate(imageEntity.getCreationDate());
        image.setUpdatedDate(imageEntity.getUpdatedDate());
        image.setFilepath(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        
        String numChannels = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS);
        if (numChannels!=null) {
            image.setNumChannels(Integer.parseInt(numChannels));
        }
        image.setObjective(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE));
        image.setOpticalResolution(sanitizeCSV(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)));
        image.setImageSize(sanitizeCSV(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)));

        
        Map<FileType,String> files = new HashMap<FileType,String>();
        String path = image.getFilepath();
        if (path.endsWith(".png") || path.endsWith(".gif") || path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            addImage(files,FileType.Unclassified2d,path);
        }
        else {
            addStackFiles(imageEntity, files, null);    
        }
        image.setFiles(files);
        
        return image;
    }
    
    private NeuronSeparation getNeuronSeparation(Entity sampleEntity, Entity separationEntity) throws Exception {
        populateChildren(separationEntity);
        
        List<NeuronFragment> neuronFragments = new ArrayList<NeuronFragment>();
        Entity nfCollectionEntity = EntityUtils.getLatestChildOfType(separationEntity, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
        
        populateChildren(nfCollectionEntity);
        for(Entity fragmentEntity : EntityUtils.getChildrenOfType(nfCollectionEntity, EntityConstants.TYPE_NEURON_FRAGMENT)) {
            NeuronFragment neuronFragment = getNeuronFragment(sampleEntity, separationEntity, fragmentEntity);
            neuronFragments.add(neuronFragment);
        }

        fragmentCollection.insert(neuronFragments.toArray());
        
        ReverseReference fragmentsReference = new ReverseReference();
        fragmentsReference.setCount(new Long(neuronFragments.size()));
        fragmentsReference.setReferringType("fragment");
        fragmentsReference.setReferenceAttr("separationId");
        fragmentsReference.setReferenceId(separationEntity.getId());
        
        NeuronSeparation neuronSeparation = new NeuronSeparation();
        neuronSeparation.setName(separationEntity.getName());
        neuronSeparation.setCreationDate(separationEntity.getCreationDate());
        neuronSeparation.setFilepath(separationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        neuronSeparation.setFragmentsReference(fragmentsReference);
        
        return neuronSeparation;
    }
    
    private NeuronFragment getNeuronFragment(Entity sampleEntity, Entity separationEntity, Entity fragmentEntity) throws Exception {
        NeuronFragment neuronFragment = new NeuronFragment();
        neuronFragment.setId(fragmentEntity.getId());
        neuronFragment.setSample(getReference(sampleEntity));
        neuronFragment.setName(fragmentEntity.getName());
        neuronFragment.setOwnerKey(fragmentEntity.getOwnerKey());
        neuronFragment.setReaders(getSubjectKeysWithPermission(fragmentEntity, "r"));
        neuronFragment.setWriters(getSubjectKeysWithPermission(fragmentEntity, "w"));
        neuronFragment.setCreationDate(fragmentEntity.getCreationDate());
        neuronFragment.setUpdatedDate(fragmentEntity.getUpdatedDate());
        String number = fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER);
        if (number!=null) {
            neuronFragment.setNumber(Integer.parseInt(number));
        }
        neuronFragment.setSeparationId(separationEntity.getId());
        neuronFragment.setFilepath(separationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        
        Map<FileType,String> images = new HashMap<FileType,String>();
        addImage(images,FileType.SignalMip,getRelativeFilename(neuronFragment,fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE)));
        addImage(images,FileType.MaskFile,getRelativeFilename(neuronFragment,fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_MASK_IMAGE)));
        addImage(images,FileType.ChanFile,getRelativeFilename(neuronFragment,fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHAN_IMAGE)));
        neuronFragment.setFiles(images);
        
        return neuronFragment;
    }

	/* SCREEN SAMPLES */
    
    private void loadScreenData() throws DaoException {

        Deque<Entity> flyLines = new LinkedList<Entity>(getEntitiesByTypeName(null, EntityConstants.TYPE_FLY_LINE));

        for(Iterator<Entity> i = flyLines.iterator(); i.hasNext(); ) {
            Entity flyLine = i.next();
            try {
                long start = System.currentTimeMillis();
                populateChildren(flyLine);
                Deque<Entity> screenSamples = new LinkedList<Entity>(EntityUtils.getChildrenOfType(flyLine, EntityConstants.TYPE_SCREEN_SAMPLE));
                int loaded = loadScreenSamples(flyLine, screenSamples);
                // Free memory 
                i.remove();
                resetSession();
                log.info("  Loading "+loaded+" screen samples took "+(System.currentTimeMillis()-start)+" ms");
            }
            catch (Exception e) {
                log.error("Error loading screen samples for fly line " + flyLine.getName(), e);
            }
        }
    }
    
    private int loadScreenSamples(Entity flyLineEntity, Deque<Entity> samples) {

        int loaded = 0;
        for(Iterator<Entity> i = samples.iterator(); i.hasNext(); ) {
            Entity screenSampleEntity = i.next();
            
            try {
                long start = System.currentTimeMillis();
                ScreenSample screenSample = getScreenSampleObject(flyLineEntity, screenSampleEntity);
                if (screenSample!=null) {
                    screenSampleCollection.insert(screenSample);
                }
                if (screenSample!=null) {
                    log.info("  Loading "+screenSampleEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                    loaded++;
                }
                else {
                    log.info("  Failure loading "+screenSampleEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                }
            }
            catch (Throwable e) {
                log.error("Error loading screen sample "+screenSampleEntity.getId(),e);
            }
        }
        
        return loaded;
    }
    
    private ScreenSample getScreenSampleObject(Entity flyLineEntity, Entity screenSampleEntity) throws Exception {
        
        if (screenSampleEntity.getEntityData()==null) {
            log.warn("  Cannot process screen sample with null entityData: "+screenSampleEntity.getId());
            return null;
        }
        
        populateChildren(screenSampleEntity);
        
        ScreenSample screenSample = new ScreenSample();
                
        screenSample.setId(screenSampleEntity.getId());
        screenSample.setName(screenSampleEntity.getName());
        screenSample.setOwnerKey(screenSampleEntity.getOwnerKey());
        screenSample.setReaders(getSubjectKeysWithPermission(screenSampleEntity, "r"));
        screenSample.setWriters(getSubjectKeysWithPermission(screenSampleEntity, "w"));
        screenSample.setCreationDate(screenSampleEntity.getCreationDate());
        screenSample.setUpdatedDate(screenSampleEntity.getUpdatedDate());
        screenSample.setName(screenSampleEntity.getName());
        screenSample.setFlyLine(flyLineEntity.getName());
        
        List<PatternMask> masks = new ArrayList<PatternMask>();
        
        Entity patternAnnotationEntity = EntityUtils.findChildWithNameAndType(screenSampleEntity, "Pattern Annotation", EntityConstants.TYPE_FOLDER);
        if (patternAnnotationEntity==null) {
            log.warn("Cannot process screen sample with no pattern annotation folder: "+screenSampleEntity.getId());
            return null;
        }
        
        String paFilepath = patternAnnotationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        screenSample.setFilepath(paFilepath.replaceFirst("patternAnnotation", ""));
        
        addMasks(screenSample, patternAnnotationEntity, masks);
        
        Entity maskAnnotationEntity = EntityUtils.findChildWithNameAndType(screenSampleEntity, "Mask Annotation", EntityConstants.TYPE_FOLDER);
        if (maskAnnotationEntity!=null) {
            populateChildren(maskAnnotationEntity);
            for(Entity maskUpdateEntity : EntityUtils.getChildrenOfType(maskAnnotationEntity, EntityConstants.TYPE_FOLDER)) {
                addMasks(screenSample, maskUpdateEntity, masks);   
            }            
        }
        
        patternMaskCollection.insert(masks.toArray());

        Map<FileType,String> images = new HashMap<FileType,String>();
        Entity alignedStack = EntityUtils.findChildWithType(screenSampleEntity, EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
        if (alignedStack!=null) {
            addImage(images,FileType.Stack,getRelativeFilename(screenSample,alignedStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)));
            addImage(images,FileType.CompleteMip,getRelativeFilename(screenSample,alignedStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH)));
        }
        Entity heatmap = EntityUtils.findChildWithName(patternAnnotationEntity, "Heatmap");
        if (heatmap!=null) {
            addImage(images,FileType.HeatmapStack,getRelativeFilename(screenSample,screenSampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)));
            addImage(images,FileType.HeatmapMip,getRelativeFilename(screenSample,screenSampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH)));
        }
        screenSample.setImages(images);
        
        ReverseReference masksRef = new ReverseReference();
        masksRef.setCount(new Long(masks.size()));
        masksRef.setReferringType("patternMask");
        masksRef.setReferenceAttr("screenSample.targetId");
        masksRef.setReferenceId(screenSample.getId());
        screenSample.setPatternMasks(masksRef);
        
        return screenSample;
    }

    private void addMasks(ScreenSample screenSample, Entity patternMaskFolderEntity, List<PatternMask> masks) throws Exception {

        populateChildren(patternMaskFolderEntity);
        
        Map<Long,Integer> intensityMap = new HashMap<Long,Integer>();
        Map<Long,Integer> distributionMap = new HashMap<Long,Integer>();
        for(Entity annotation : getAnnotationsForChildren(patternMaskFolderEntity.getOwnerKey(), patternMaskFolderEntity.getId())) {
            String value = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM);
            if (value.length()!=2) continue;
            String targetIdStr = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
            if (targetIdStr==null) continue;
            Long targetId = Long.parseLong(targetIdStr);
            if (value.startsWith("i")) {
                Integer intensity = Integer.parseInt(value.substring(1));
                intensityMap.put(targetId, intensity);
            }
            else if (value.startsWith("d")) {
                Integer distribution = Integer.parseInt(value.substring(1));
                distributionMap.put(targetId, distribution);
            }
        }
        
        String maskSetName = patternMaskFolderEntity.getName().replaceAll("\\s+", "");
        
        for(Entity maskEntity : EntityUtils.getChildrenOfType(patternMaskFolderEntity, EntityConstants.TYPE_ALIGNED_BRAIN_STACK)) {
            if (maskEntity.getName().equals("Heatmap")) continue;
            PatternMask mask = getPatternMaskObject(screenSample, maskEntity, maskSetName, intensityMap.get(maskEntity.getId()), distributionMap.get(maskEntity.getId()));
            masks.add(mask);
        }

        Entity normalizedEntity = EntityUtils.findChildWithNameAndType(patternMaskFolderEntity, "normalized", EntityConstants.TYPE_FOLDER);
        if (normalizedEntity!=null) {
            addMasks(screenSample, normalizedEntity, masks);
        }
    }
    
    private PatternMask getPatternMaskObject(ScreenSample screenSample, Entity maskEntity, String maskSetName, Integer intensity, Integer distribution) throws Exception {
        PatternMask mask = new PatternMask();
        mask.setId(maskEntity.getId());
        mask.setScreenSample(getReference(screenSample));
        mask.setName(maskEntity.getName());
        mask.setOwnerKey(maskEntity.getOwnerKey());
        mask.setReaders(getSubjectKeysWithPermission(maskEntity, "r"));
        mask.setWriters(getSubjectKeysWithPermission(maskEntity, "w"));
        mask.setCreationDate(maskEntity.getCreationDate());
        mask.setUpdatedDate(maskEntity.getUpdatedDate());
        mask.setFilepath(screenSample.getFilepath());
        mask.setMaskSetName(maskSetName);
        mask.setIntensityScore(intensity);
        mask.setDistributionScore(distribution);
        mask.setNormalized("normalized".equals(maskSetName));
        
        String default2dImageFilepath = maskEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
        if (default2dImageFilepath==null) {
        	default2dImageFilepath = maskEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        }
        
        Map<FileType,String> images = new HashMap<FileType,String>();
        addImage(images,FileType.Stack,getRelativeFilename(mask,maskEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)));
        addImage(images,FileType.HeatmapMip,getRelativeFilename(mask,default2dImageFilepath));
        mask.setImages(images);
        return mask;
    }
    
    
    /* ONTOLOGIES */
    
    private void loadOntologies() throws DaoException {
        long start = System.currentTimeMillis();
        Deque<Entity> ontologyRoots = new LinkedList<Entity>(getEntitiesByTypeName(null, EntityConstants.TYPE_ONTOLOGY_ROOT));
        int loaded = loadOntologies(ontologyRoots);
        log.info("  Loading "+loaded+" ontologies took "+(System.currentTimeMillis()-start)+" ms");
    }

    private int loadOntologies(Deque<Entity> ontologyRoots) {

        int loaded = 0;
        for(Iterator<Entity> i = ontologyRoots.iterator(); i.hasNext(); ) {
            Entity ontologyRootEntity = i.next();

            // Skip these unused/large ontologies
            if (ontologyRootEntity.getName().equals("Fly anatomy") 
                    || ontologyRootEntity.getName().equals("Fly Taxonomy")) {
                continue;
            }
            
            Session session = null;
            try {
                long start = System.currentTimeMillis();

                session = openNewExternalSession();
                Ontology ontology = getOntology(ontologyRootEntity);
                ontologyCollection.insert(ontology);

                // Free memory by releasing the reference to this entire entity tree
                i.remove(); 
                resetSession();

                if (ontology!=null) {
                    log.info("  Loading "+ontologyRootEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                    loaded++;
                }
                else {
                    log.info("  Failure loading "+ontologyRootEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                }
                
            }
            catch (Throwable e) {
                log.error("Error loading ontology "+ontologyRootEntity.getId(),e);
            }
            finally {
                if (session==null) closeExternalSession();
            }
        }
        
        return loaded;
    }

    private Ontology getOntology(Entity ontologyEntity) throws Exception {
       
        Ontology ontology = (Ontology)getOntologyTerm(ontologyEntity, ontologyEntity);
        ontology.setOwnerKey(ontologyEntity.getOwnerKey());
        ontology.setReaders(getSubjectKeysWithPermission(ontologyEntity, "r"));
        ontology.setWriters(getSubjectKeysWithPermission(ontologyEntity, "w"));
        ontology.setCreationDate(ontologyEntity.getCreationDate());
        ontology.setUpdatedDate(ontologyEntity.getUpdatedDate());
        
        return ontology;
    }
    
    private OntologyTerm getOntologyTerm(Entity ontologyEntity, Entity ontologyTermEntity) throws Exception {

        populateChildren(ontologyTermEntity);
        
        OntologyTerm ontologyTerm = null;
        if (ontologyEntity.getId().equals(ontologyTermEntity.getId())) {
            ontologyTerm = new Ontology();
        }
        else {
            String typeName = ontologyTermEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
            ontologyTerm = createOntologyTypeByName(typeName);   
        }
        
        ontologyTerm.setId(ontologyTermEntity.getId());
        ontologyTerm.setName(ontologyTermEntity.getName());
        
        if (ontologyTerm instanceof Interval) {
            Interval interval = (Interval)ontologyTerm;
            String lower = ontologyTermEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_LOWER);
            String upper = ontologyTermEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_UPPER);
            if (lower!=null) {
                interval.setLowerBound(Long.parseLong(lower));
            }
            if (upper!=null) {
                interval.setUpperBound(Long.parseLong(upper));
            }
        }
        else if (ontologyTerm instanceof EnumText) {
            EnumText enumText = (EnumText)ontologyTerm;
            String enumId = ontologyTermEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_ENUMTEXT_ENUMID);
            if (enumId!=null) {
                enumText.setValueEnumId(Long.parseLong(enumId));
            }
        }

        List<OntologyTerm> terms = new ArrayList<OntologyTerm>();
        for(Entity childEntity : EntityUtils.getChildrenForAttribute(ontologyTermEntity, EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT)) {
            OntologyTerm term = getOntologyTerm(ontologyEntity, childEntity);
            if (term!=null) {
                terms.add(term);
            }
        }
        if (!terms.isEmpty()) {
            ontologyTerm.setTerms(terms);
        }
        
        ontologyTermIdToOntologyId.put(ontologyTermEntity.getId(), ontologyEntity.getId());
        
        return ontologyTerm;
    }
    
    private OntologyTerm createOntologyTypeByName(String className) {
        try {
            if (className==null) return new Ontology();
            return (OntologyTerm)Class.forName(ONTOLOGY_TERM_TYPES_PACKAGE+"."+className).newInstance();
        }
        catch (Exception ex) {
            log.error("Could not instantiate ontology term for type "+className);
        }
        return null;
    }


    /* ANNOTATIONS */
    
    private void loadAnnotations() {

        long start = System.currentTimeMillis();
        List<Annotation> queue = new LinkedList<Annotation>();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getJdbcConnection();
            
            StringBuilder sql = new StringBuilder();
            sql.append("select a.id, a.name, a.owner_key, a.creation_date, a.updated_date, target.id, target.entity_type, ");
            sql.append("ked.value, ved.value, keed.child_entity_id, veed.child_entity_id ");
            sql.append("from entity a ");
            sql.append("join entityData ted on ted.parent_entity_id=a.id and ted.entity_att=? ");
            sql.append("join entity target on ted.value=target.id "); 
            sql.append("left outer join entityData ked on ked.parent_entity_id=a.id and ked.entity_att=? ");
            sql.append("left outer join entityData ved on ved.parent_entity_id=a.id and ved.entity_att=? ");
            sql.append("left outer join entityData keed on keed.parent_entity_id=a.id and keed.entity_att=? ");
            sql.append("left outer join entityData veed on veed.parent_entity_id=a.id and veed.entity_att=? ");
            sql.append("where a.entity_type=? ");
            //sql.append("and a.owner_key not in ('user:jenetta','group:flylight') ");
            
            stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(Integer.MIN_VALUE);
            
            stmt.setString(1, EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
	        stmt.setString(2, EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM);
	        stmt.setString(3, EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM);
            stmt.setString(4, EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
            stmt.setString(5, EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID);
            stmt.setString(6, EntityConstants.TYPE_ANNOTATION);
            
            rs = stmt.executeQuery();
            int i = 0;
            while (rs.next()) {
                Long annotationId = rs.getBigDecimal(1).longValue();
                String annotationName = rs.getString(2);
                String owner = rs.getString(3);
                Date creationDate = rs.getTimestamp(4);
                Date updatedDate = rs.getTimestamp(5);
                Long targetId = rs.getBigDecimal(6).longValue();
                String targetType = rs.getString(7);
                String keyStr = rs.getString(8);
                String valueStr = rs.getString(9);
                String keyIdStr = rs.getString(10);
                String valueIdStr = rs.getString(11);
                
                Long keyId = null;
                if (!StringUtils.isEmpty(keyIdStr)) {
                    try {
                        keyId = new Long(keyIdStr);
                    }
                    catch (NumberFormatException e) {
                        log.warn("Cannot parse annotation key id for annotation="+annotationId);
                    }
                }
                
                Long valueId = null;
                if (!StringUtils.isEmpty(valueIdStr)) {
                    try {
                        valueId = new Long(valueIdStr);
                    }
                    catch (NumberFormatException e) {
                        log.warn("Cannot parse annotation value id for annotation="+annotationId);
                    }
                }
                
                Annotation annotation = getAnnotation(annotationId, annotationName, owner, 
                		creationDate, updatedDate, targetId, targetType, keyId, valueId, keyStr, valueStr);
                queue.add(annotation);
                
                if (queue.size()>INSERTION_BATCH_SIZE) {
                    annotationCollection.insert(queue.toArray());
                    queue.clear();
                }
                
                i++;
            }
            
            if (!queue.isEmpty()) {
                annotationCollection.insert(queue.toArray());
            }
            
            log.info("  Loading "+i+" annotations took "+(System.currentTimeMillis()-start)+" ms");
        }
        catch (Exception e) {
            log.error("Error loading annotations",e);
        }
        finally {
            try {
                if (rs!=null) rs.close();
                if (stmt!=null) stmt.close();
                if (conn!=null) conn.close();   
            }
            catch (Exception e) {
                log.warn("Error closing JDBC connection",e);
            }
        }
    }
    
    private Annotation getAnnotation(Long annotationId, String annotationName, String ownerKey, Date creationDate, Date updatedDate,
            Long targetId, String targetType, Long keyId, Long valueId, String key, String value) {
        
        Annotation annotation = new Annotation();
        annotation.setId(annotationId);
        annotation.setName(annotationName);
        annotation.setOwnerKey(ownerKey);
        annotation.setReaders(getDefaultSubjectKeys(ownerKey));
        annotation.setWriters(annotation.getReaders());
        annotation.setCreationDate(creationDate);
        annotation.setUpdatedDate(updatedDate);
        annotation.setKey(key);
        annotation.setValue(value);
        
        if (keyId!=null) {
            OntologyTermReference keyTerm = new OntologyTermReference();
            keyTerm.setOntologyTermId(keyId);
            keyTerm.setOntologyId(ontologyTermIdToOntologyId.get(keyId));
            annotation.setKeyTerm(keyTerm);
        }

        if (valueId!=null) {
            OntologyTermReference valueTerm = new OntologyTermReference();
            valueTerm.setOntologyTermId(valueId);
            valueTerm.setOntologyId(ontologyTermIdToOntologyId.get(valueId));
            annotation.setValueTerm(valueTerm);
        }
        
        Reference target = new Reference(getCollectionName(targetType), targetId);
        annotation.setTarget(target);
        
        return annotation;
    }
    
    
    /* COMPARTMENT SETS */
    
    private void loadCompartmentSets() throws DaoException {
        long start = System.currentTimeMillis();
        Deque<Entity> compartmentSets = new LinkedList<Entity>(getEntitiesByTypeName(null, EntityConstants.TYPE_COMPARTMENT_SET));
        int loaded = loadCompartmentSets(compartmentSets);
        log.info("  Loading "+loaded+" compartment sets took "+(System.currentTimeMillis()-start)+" ms");
    }

    private int loadCompartmentSets(Deque<Entity> ontologyRoots) {

        int loaded = 0;
        for(Iterator<Entity> i = ontologyRoots.iterator(); i.hasNext(); ) {
            Entity compartmentSetEntity = i.next();

            Session session = null;
            try {
                long start = System.currentTimeMillis();

                session = openNewExternalSession();
                CompartmentSet compartmentSet = getCompartmentSet(compartmentSetEntity);
                compartmentSetCollection.insert(compartmentSet);

                // Free memory by releasing the reference to this entire entity tree
                i.remove(); 
                resetSession();

                if (compartmentSet!=null) {
                    log.info("  Loading "+compartmentSetEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                    loaded++;
                }
                else {
                    log.info("  Failure loading "+compartmentSetEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                }
                
            }
            catch (Throwable e) {
                log.error("Error loading compartment set "+compartmentSetEntity.getId(),e);
            }
            finally {
                if (session==null) closeExternalSession();
            }
        }
        
        return loaded;
    }

    private CompartmentSet getCompartmentSet(Entity compartmentSetEntity) throws Exception {
       
    	CompartmentSet compartmentSet = new CompartmentSet();
    	compartmentSet.setId(compartmentSetEntity.getId());
    	compartmentSet.setName(compartmentSetEntity.getName());
    	compartmentSet.setOwnerKey(compartmentSetEntity.getOwnerKey());
    	compartmentSet.setReaders(getSubjectKeysWithPermission(compartmentSetEntity, "r"));
    	compartmentSet.setWriters(getSubjectKeysWithPermission(compartmentSetEntity, "w"));
    	compartmentSet.setCreationDate(compartmentSetEntity.getCreationDate());
    	compartmentSet.setUpdatedDate(compartmentSetEntity.getUpdatedDate());
    	compartmentSet.setAlignmentSpace(compartmentSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE));
    	compartmentSet.setImageSize(sanitizeCSV(compartmentSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)));
    	compartmentSet.setOpticalResolution(sanitizeCSV(compartmentSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)));
        
    	Pattern p = Pattern.compile("(.*?) \\((.*?)\\)");
    	
    	String rootDir = null;
    	
    	List<Compartment> compartments = new ArrayList<Compartment>();
    	for(Entity compartmentEntity : compartmentSetEntity.getOrderedChildren()) {
    		Compartment compartment = new Compartment();
    		compartment.setColor(compartmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COLOR));
    	    Matcher m = p.matcher(compartmentEntity.getName());
    		if (m.matches()) {
    			String name = m.group(1);
    			String code = m.group(2);
        		compartment.setName(name);
        		compartment.setCode(code);
    		}
    		else {
    			log.error("Error parsing compartment name: "+compartmentEntity.getName());
    		}

            Map<FileType,String> files = new HashMap<FileType,String>();
            String maskFile = compartmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_MASK_IMAGE);
            String chanFile = compartmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHAN_IMAGE);
            
            String parent = new File(maskFile).getParent();
            if (rootDir==null || parent.equals(rootDir)) {
            	rootDir = parent;
            }
            else {
            	log.warn("Comparment set "+compartmentSet.getName()+" does not agree on root dir: "+parent+" != "+rootDir);
            }

            parent = new File(chanFile).getParent();
            if (rootDir==null || parent.equals(rootDir)) {
            	rootDir = parent;
            }
            else {
            	log.warn("Comparment set "+compartmentSet.getName()+" does not agree on root dir: "+parent+" != "+rootDir);
            }
            
            addImage(files,FileType.MaskFile,maskFile);
            addImage(files,FileType.ChanFile,chanFile);
            compartment.setFiles(files);
            
    		compartments.add(compartment);
    	}
    	
    	compartmentSet.setFilepath(rootDir);
    	    	
    	for(Compartment compartment : compartments) {
            Map<FileType,String> relativeFiles = new HashMap<FileType,String>();
    		for(FileType fileType : compartment.getFiles().keySet()) {
    			relativeFiles.put(fileType, compartment.getFiles().get(fileType).replaceFirst(rootDir+"/?", ""));
    		}
            compartment.setFiles(relativeFiles);
    	}
    	
    	if (!compartments.isEmpty()) compartmentSet.setCompartments(compartments);
    	
        return compartmentSet;
    }
    

    /* ALIGNMENT BOARDS */
    
    private void loadAlignmentBoards() throws DaoException {
        Deque<Entity> alignmentBoards = new LinkedList<Entity>(getEntitiesByTypeName(null, EntityConstants.TYPE_ALIGNMENT_BOARD));
        loadAlignmentBoards(alignmentBoards);
    }

    private int loadAlignmentBoards(Deque<Entity> alignmentBoards) {

        int c = 0;
        for(Iterator<Entity> i = alignmentBoards.iterator(); i.hasNext(); ) {
            Entity alignmentBoardEntity = i.next();

            Session session = null;
            try {
                long start = System.currentTimeMillis();

                session = openNewExternalSession();
                AlignmentBoard alignmentBoard = getAlignmentBoard(alignmentBoardEntity);
                alignmentBoardCollection.insert(alignmentBoard);

                // Free memory by releasing the reference to this entire entity tree
                i.remove(); 
                resetSession();

                if (alignmentBoard!=null) {
                    log.info("  Loading "+alignmentBoardEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                }
                else {
                    log.info("  Failure loading "+alignmentBoardEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                }
                
            }
            catch (Throwable e) {
                log.error("Error loading alignment board "+alignmentBoardEntity.getId(),e);
            }
            finally {
                if (session==null) closeExternalSession();
            }
            
            c++;
        }
        
        return c;
    }

    private AlignmentBoard getAlignmentBoard(Entity alignmentBoardEntity) throws Exception {
    	AlignmentBoard alignmentBoard = new AlignmentBoard();
    	alignmentBoard.setId(alignmentBoardEntity.getId());
    	alignmentBoard.setName(alignmentBoardEntity.getName());
    	alignmentBoard.setOwnerKey(alignmentBoardEntity.getOwnerKey());
    	alignmentBoard.setReaders(getSubjectKeysWithPermission(alignmentBoardEntity, "r"));
    	alignmentBoard.setWriters(getSubjectKeysWithPermission(alignmentBoardEntity, "w"));
    	alignmentBoard.setCreationDate(alignmentBoardEntity.getCreationDate());
    	alignmentBoard.setUpdatedDate(alignmentBoardEntity.getUpdatedDate());
    	alignmentBoard.setAlignmentSpace(alignmentBoardEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE));
    	alignmentBoard.setImageSize(sanitizeCSV(alignmentBoardEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)));
    	alignmentBoard.setOpticalResolution(sanitizeCSV(alignmentBoardEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)));
    	List<AlignmentBoardItem> children = getAlignmentBoardChildren(alignmentBoardEntity);
    	if (!children.isEmpty()) alignmentBoard.setChildren(children);
    	
        return alignmentBoard;
    }
    
    private List<AlignmentBoardItem> getAlignmentBoardChildren(Entity alignmentBoardItem) {

    	List<AlignmentBoardItem> items = new ArrayList<AlignmentBoardItem>();
    	
    	for(Entity alignmentBoardItemEntity : EntityUtils.getChildrenForAttribute(alignmentBoardItem, EntityConstants.ATTRIBUTE_ITEM)) {
    		Entity targetEntity = alignmentBoardItemEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_ENTITY);
    		if (targetEntity==null) {
    			log.info("Target no longer exists for alignment board item: "+alignmentBoardItemEntity.getId());
    		}
    		else {
        		AlignmentBoardItem item = new AlignmentBoardItem();
        		item.setInclusionStatus(alignmentBoardItemEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_INCLUSION_STATUS));
        		item.setVisible("true".equalsIgnoreCase(alignmentBoardItemEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISIBILITY)));
        		item.setColor(alignmentBoardItemEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COLOR));
        		item.setRenderMethod(alignmentBoardItemEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_RENDER_METHOD));
        		Reference target = getReference(targetEntity);
        		item.setTarget(target);
        		List<AlignmentBoardItem> children = getAlignmentBoardChildren(alignmentBoardItemEntity);
        		if (!children.isEmpty()) item.setChildren(children);	
        		items.add(item);
    		}
    	}
    	
    	return items;
    }

    
    /* TREE NODES (WORKSPACES, FOLDERS, VIEWS) */
    
    private void loadWorkspaces() throws DaoException {
    	Map<Long,DomainObject> visited = new HashMap<Long,DomainObject>();
        for (org.janelia.it.jacs.model.user_data.Subject subject : subjectDao.getSubjects()) {
            String subjectKey = subject.getKey();
            try {
                loadWorkspace(subjectKey, visited);
            }
            catch (Exception e) {
                log.error("Error loading workspace for " + subjectKey, e);
            }
        }
    }
    
    private void loadWorkspace(String subjectKey, Map<Long,DomainObject> visited) throws Exception {

        long start = System.currentTimeMillis();

        log.info("Loading workspace for "+subjectKey);

        Entity workspaceEntity = getDefaultWorkspace(subjectKey);
        if (workspaceEntity==null) {
        	log.error("Could not find default workspace for "+subjectKey);	
        	return;
        }
        
        LinkedList<Entity> rootFolders = new LinkedList<Entity>(workspaceEntity.getOrderedChildren());
        Collections.sort(rootFolders, new EntityRootComparator(subjectKey));
        List<Reference> children = loadRootFolders(rootFolders, visited);
        
        Workspace workspace = new Workspace();
        workspace.setId(workspaceEntity.getId());
        workspace.setName(workspaceEntity.getName());
        workspace.setOwnerKey(workspaceEntity.getOwnerKey());
        workspace.setReaders(getDefaultSubjectKeys(subjectKey));
        workspace.setWriters(workspace.getReaders());
        workspace.setCreationDate(workspaceEntity.getCreationDate());
        workspace.setUpdatedDate(workspaceEntity.getUpdatedDate());
        workspace.setChildren(children);
        treeNodeCollection.insert(workspace);
        
        log.info("Loading workspace for "+subjectKey+" took "+(System.currentTimeMillis()-start)+" ms");
    }
    
    private List<Reference> loadRootFolders(Deque<Entity> rootFolders, Map<Long,DomainObject> visited) {

        List<Reference> roots = new ArrayList<Reference>();
        
        for(Iterator<Entity> i = rootFolders.iterator(); i.hasNext(); ) {
            Entity folderEntity = i.next();
            
            Session session = null;
            try {
                long start = System.currentTimeMillis();
                
                session = openNewExternalSession();
                DomainObject domainObject = loadFolderHierarchy(folderEntity, visited, "  ");
                if (domainObject!=null) {
                    Reference ref = getReference(domainObject);
                    roots.add(ref);
                }
                
                // Free memory by releasing the reference to this entire entity tree
                i.remove(); 
                resetSession();

                log.info("  Loading "+folderEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
            }
            catch (Throwable e) {
                log.error("Error loading folder "+folderEntity.getId(),e);
            }
            finally {
                if (session==null) closeExternalSession();
            }
        }
        
        return roots;
    }
    
    private DomainObject loadFolderHierarchy(Entity folderEntity, Map<Long,DomainObject> visited, String indent) throws Exception {

    	if ("supportingFiles".equals(folderEntity.getName())) {
    		log.info(indent+"Skipping "+folderEntity.getName());
    		return null;
    	}
    	
        log.trace(indent+"Loading "+folderEntity.getName());
        
        if (visited.containsKey(folderEntity.getId())) {
            return visited.get(folderEntity.getId());
        }

        if (EntityUtils.getChildrenOfType(folderEntity, EntityConstants.TYPE_FOLDER).isEmpty()) {
        	// This folder contains only non-Folder objects, so we'll consider it an object set
        	ObjectSet objectSet = getObjectSet(folderEntity, indent);
            objectSetCollection.insert(objectSet);
            visited.put(objectSet.getId(), objectSet);
            return objectSet;
        }
        
        // This folder contains other folders, so we'll just load those as part of the folder hierarchy
        TreeNode treeNode = new TreeNode();
        treeNode.setId(folderEntity.getId());
        treeNode.setName(folderEntity.getName());
        treeNode.setOwnerKey(folderEntity.getOwnerKey());
        treeNode.setReaders(getSubjectKeysWithPermission(folderEntity, "r"));
        treeNode.setWriters(getSubjectKeysWithPermission(folderEntity, "w"));
        treeNode.setCreationDate(folderEntity.getCreationDate());
        treeNode.setUpdatedDate(folderEntity.getUpdatedDate());
    
	    // Using a hash here to eliminate duplicates, especially those caused by folders which contain multiple descendants of the same sample
	    HashSet<Reference> children = new LinkedHashSet<Reference>();
	    
	    getCollectionName(EntityConstants.TYPE_FOLDER);
	    
	    // Load children
	    for(Entity childFolder : EntityUtils.getChildrenOfType(folderEntity, EntityConstants.TYPE_FOLDER)) {
	    	
	    	DomainObject domainObject = loadFolderHierarchy(childFolder, visited, indent+"  ");
            if (domainObject!=null) {
                Reference ref = getReference(domainObject);
                children.add(ref);
            }
	    }

	    
        if (!children.isEmpty()) {
            treeNode.setChildren(new ArrayList<Reference>(children));
        }

        treeNodeCollection.insert(treeNode);
        visited.put(treeNode.getId(), treeNode);
        return treeNode;
    }

    private ObjectSet getObjectSet(Entity folderEntity, String indent) throws Exception {
		ObjectSet objectSet = new ObjectSet();
		objectSet.setId(folderEntity.getId());
		objectSet.setName(folderEntity.getName());
		objectSet.setOwnerKey(folderEntity.getOwnerKey());
		objectSet.setReaders(getSubjectKeysWithPermission(folderEntity, "r"));
		objectSet.setWriters(getSubjectKeysWithPermission(folderEntity, "w"));
		objectSet.setCreationDate(folderEntity.getCreationDate());
		objectSet.setUpdatedDate(folderEntity.getUpdatedDate());

	    // --------------------------------------------------------------------------------
		// 1) Figure out which entity type has the majority. An object set can only contain one type, everything else will be discarded.
		int totalNumChildren = 0;
		Map<String,Integer> typeCounts = new HashMap<String,Integer>();
		for(Entity childEntity : folderEntity.getChildren()) {
			Integer count = typeCounts.get(childEntity.getEntityTypeName());
			if (count==null) {
				count = new Integer(0);
			}
			count += 1;
			totalNumChildren += 1;
			typeCounts.put(childEntity.getEntityTypeName(), count);
		}
		 
		Integer maxCount = 0;
		String maxCountType = null;
		for(String type : typeCounts.keySet()) {
			Integer count = typeCounts.get(type);
			if (count>maxCount) {
				maxCount = count;
				maxCountType = type;
			}
		}

		if (maxCount==0) {
			// Object set has no members 
			return objectSet;
		}

		objectSet.setTargetType(getCollectionName(maxCountType));
				
		if (maxCount!=totalNumChildren) {
			log.info(indent+"  Importing "+maxCount+" entities of type "+maxCountType+" in folder "+folderEntity.getId());
			for(String type : typeCounts.keySet()) {
				Integer count = typeCounts.get(type);
				if (!type.equals(maxCountType)) {
					log.warn(indent+"  Discarding "+count+" entities of type "+type+" in folder "+folderEntity.getId());
				}
			}
		}
		
	    // --------------------------------------------------------------------------------
		// 2) Get the objects in the set
		List<Entity> entityMembers = new ArrayList<Entity>(); 
		for(Entity childEntity : folderEntity.getOrderedChildren()) {
			if (childEntity.getEntityTypeName().equals(maxCountType)) {
				entityMembers.add(childEntity);
			}
		}

	    // --------------------------------------------------------------------------------
	    // 3) Preprocess all children and see if we need to do a bulk mapping
		Map<Long,Entity> translatedEntities = new HashMap<Long,Entity>();
		
		if (EntityConstants.TYPE_SAMPLE.equals(maxCountType)) {    
		    // Case 1: Sub samples
		    List<Long> subsampleIds = new ArrayList<Long>();
		    for(Entity childEntity : entityMembers) {
		        String childType = childEntity.getEntityTypeName();
	        	if ((childType.equals(EntityConstants.TYPE_SAMPLE) && childEntity.getName().contains("~"))) {
	        		subsampleIds.add(childEntity.getId());
	        	}
		    }
		    if (!subsampleIds.isEmpty()) {
				List<String> upMapping = new ArrayList<String>();
				upMapping.add("Sample");
				List<String> downMapping = new ArrayList<String>();
				List<MappedId> mappings = getProjectedResults(null, subsampleIds, upMapping, downMapping);
				Map<Long,Entity> mappedEntities = getMappedEntities(mappings);
	            for(MappedId mappedId : mappings) {
	            	translatedEntities.put(mappedId.getOriginalId(),  mappedEntities.get(mappedId.getOriginalId()));
	            }
	            if (subsampleIds.size()!=translatedEntities.size()) {
	            	log.warn(indent+"  Translated "+subsampleIds.size()+ " sub samples to "+translatedEntities.size()+" parent samples");	
	            }
			}
		}
	    
	    // --------------------------------------------------------------------------------
		// 4) Load the members, translating them if necessary
	    List<Long> memberIds = new ArrayList<Long>();
	    
	    for(Entity childEntity : entityMembers) {
	    	
	        String childType = childEntity.getEntityTypeName();
        	String childColName = getCollectionName(childType);
        	
        	Entity importEntity = childEntity;
	        Long importEntityId = childEntity.getId();
	        
        	Entity translatedEntity = translatedEntities.get(childEntity.getId());
        	if (translatedEntity!=null) {
        		// already translate this above
        		logger.info(indent+"  Will reference "+translatedEntity.getEntityTypeName()+"#"+translatedEntity.getId()+" instead of "+childType+"#"+importEntityId);
        		importEntityId = translatedEntity.getId();
        	}
        	else if (TRANSLATE_ENTITIES && "unknown".equals(childColName)) {
        		// See if we can substitute a higher-level entity for the one that the user referenced. For example, 
        		// if they referenced a sample processing result, we find the parent sample. Same goes for neuron separations, etc.
        		// The priority list defines the ordered list of possible entity types to try as ancestors.  
        		for (String entityType : entityTranslationPriority) {
        			Entity owningEntity = getAncestorWithType(childEntity.getOwnerKey(), importEntityId, entityType);
                	if (owningEntity!=null) {
                		importEntity = owningEntity;
                		importEntityId = owningEntity.getId();
                		logger.info(indent+"  Will reference "+entityType+"#"+importEntityId+" instead of unknown "+childType+"#"+importEntityId);
                		break;
                	}
        		}
        	}
        
	        if (importEntity!=null) {
	            String type = getCollectionName(importEntity.getEntityTypeName());
	            if (INSERT_ROGUE_ENTITIES && !"unknown".equals(type)) {
	                // A minor optimization, since we can only do rogue imports on images
	                if ("image".equals(type)) {
    		            // Attempt imports of rogue entities which map to domain objects, but which have not been loaded by any other part of the import procedure
    		            if (dao.getCollectionByName(type).count("{_id:#}",importEntityId)<1) {
    		            	attemptRogueImport(importEntity, indent);
    		            }
	                }
	            }
	            memberIds.add(importEntityId);
	        }
	    }

	    
        if (!memberIds.isEmpty()) {
            objectSet.setMembers(memberIds);
        }
		
		return objectSet;
	}

	private void attemptRogueImport(Entity entity, String indent) {
		
    	String entityType = entity.getEntityTypeName();
        String type = getCollectionName(entityType);

        if (entity.getName().endsWith(".mask") || entity.getName().endsWith(".chan") ) {
            return;
        }
        
        log.info(indent+"  Attempting import of rogue entity: "+entityType+"#"+entity.getId());
        
        try {
            if (EntityConstants.TYPE_LSM_STACK.equals(entityType)) {
            	LSMImage image = getLSMImage(null, entity);
            	if (image!=null) {
            	    dao.getCollectionByName(type).save(image);
            	}
            }
            else if (EntityConstants.TYPE_IMAGE_3D.equals(entityType)) {
            	Image image = getImage(entity);
            	dao.getCollectionByName(type).save(image);
            }
            else if (EntityConstants.TYPE_IMAGE_2D.equals(entityType)) {
            	Image image = getImage(entity);
            	dao.getCollectionByName(type).save(image);
            }
            else {
            	log.warn(indent+"  Cannot handle rogue entity type: "+entityType+"#"+entity.getId());
            }
        }
    	catch (Exception e) {
    		log.error(indent+"  Rogue import failed for "+entity.getId(),e);
    	}
	}

	private Map<Long, Entity> getMappedEntities(List<MappedId> mappings) throws Exception {
		List<Long> entityIds = new ArrayList<Long>(); 
		for(MappedId mappedId : mappings) {
			entityIds.add(mappedId.getMappedId());
		}
		return EntityUtils.getEntityMap(getEntitiesInList(null, entityIds));
	}
    
    /* UTILITY METHODS */

    private Reference getReference(Entity entity) {
        return new Reference(getCollectionName(entity.getEntityTypeName()), entity.getId());
	}

    private Reference getReference(DomainObject domainObject) {
    	return new Reference(MongoUtils.getCollectionName(domainObject),domainObject.getId());
    }

    private Set<String> getDefaultSubjectKeys(String subjectKey) {
        Set<String> subjectKeys = new HashSet<String>();
        subjectKeys.add(subjectKey); // owner has all permissions
        return subjectKeys;
    }
    
    private Set<String> getSubjectKeysWithPermission(Entity entity, String rights) {
        Set<String> subjectKeys = new HashSet<String>();
        subjectKeys.add(entity.getOwnerKey()); // owner has all permissions
        for(EntityActorPermission permission : entity.getEntityActorPermissions()) {
            if (permission.getPermissions().contains(rights)) {
                subjectKeys.add(permission.getSubjectKey());
            }
        }
        return subjectKeys;
    }

    private void addImage(Map<FileType,String> images, FileType key, String value) {
        if (value==null) return;
        images.put(key, value);
    }
    
    private String getCollectionName(String entityType) {
        if (EntityConstants.TYPE_SAMPLE.equals(entityType)) {
            return "sample";
        }
        else if (EntityConstants.TYPE_NEURON_FRAGMENT.equals(entityType)) {
            return "fragment";
        }
        else if (EntityConstants.TYPE_FOLDER.equals(entityType)) {
            return "treeNode";
        }
        else if (EntityConstants.TYPE_ANNOTATION.equals(entityType)) {
            return "annotation";
        }
        else if (EntityConstants.TYPE_ONTOLOGY_ROOT.equals(entityType)) {
            return "ontology";
        }
        else if (EntityConstants.TYPE_SCREEN_SAMPLE.equals(entityType)) {
            return "screenSample";
        }
        else if (EntityConstants.TYPE_ALIGNED_BRAIN_STACK.equals(entityType)) {
            return "patternMask";
        }
        else if (EntityConstants.TYPE_FLY_LINE.equals(entityType)) {
            return "flyLine";
        }
        else if (EntityConstants.TYPE_COMPARTMENT_SET.equals(entityType)) {
            return "compartmentSet";
        }
        else if (EntityConstants.TYPE_ALIGNMENT_BOARD.equals(entityType)) {
            return "alignmentBoard";
        }
        else if (EntityConstants.TYPE_LSM_STACK.equals(entityType)) {
            return "image";
        }
        else if (EntityConstants.TYPE_IMAGE_3D.equals(entityType)) {
            return "image";
        }
        else if (EntityConstants.TYPE_IMAGE_2D.equals(entityType)) {
            return "image";
        }
        return "unknown";
    }

    /**
     * Convert non-standard gender values like "Female" into standardized codes like "f". The
     * four standardized codes are "m", "f", "x", and "NO_CONSENSUS" in the case of samples.
     * @param sageGender
     * @return
     */
    private String sanitizeGender(String gender) {
        if (gender==null) {
            return null;
        }
        if (gender.equals(NO_CONSENSUS_VALUE)) {
            return NO_CONSENSUS_VALUE;
        }
        String genderLc = gender.toLowerCase();
        if (genderLc.startsWith("f")) {
            return "f";
        }
        else if (genderLc.startsWith("m")) {
            return "m";
        }
        else if (genderLc.startsWith("x")) {
            return "x";
        } 
        else {
            log.warn("Invalid value for sample gender: "+gender);
            return null;
        }
    }
    
    /**
     * Remove stray quotes from a CSV string.
     * @param res
     * @return
     */
    private String sanitizeCSV(String res) {
        if (res==null) return res;
        return res.replaceAll("'", "");
    }
    
    private String getRelativeFilename(HasFilepath result, String filepath) {
        if (filepath==null) return null;
        if (result==null) return filepath;
        String parentFilepath = result.getFilepath();
        if (parentFilepath==null) throw new IllegalArgumentException("Result "+filepath+" has null parent filepath");
        String prefix = parentFilepath.endsWith("/") ? parentFilepath : parentFilepath+"/";
        if (!filepath.startsWith(prefix)) {
            return filepath;
        }
        return filepath.replaceFirst(prefix, "");
    }
    
    public void dropDatabase() throws DaoException {
		try {
		    dao.getJongo().getDatabase().dropDatabase();
		}
		catch (Exception e) {
			throw new DaoException("Error clearing index with MongoDB",e);
		}
    }
    
    private class EntityRootComparator implements Comparator<Entity> {
        private String owner;
        public EntityRootComparator(String owner) {
            this.owner = owner;
        }
        public int compare(Entity o1, Entity o2) {
            return ComparisonChain.start()
                .compareTrueFirst(o1.getOwnerKey().equals(owner), o2.getOwnerKey().equals(owner))
                .compare(o1.getOwnerKey(), o2.getOwnerKey())
                .compareTrueFirst(EntityUtils.isProtected(o1), EntityUtils.isProtected(o2))
                .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_DATA_SETS), o2.getName().equals(EntityConstants.NAME_DATA_SETS))
                .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_SHARED_DATA), o2.getName().equals(EntityConstants.NAME_SHARED_DATA))
                .compare(o1.getId(), o2.getId()).result();
        }
    };
}
