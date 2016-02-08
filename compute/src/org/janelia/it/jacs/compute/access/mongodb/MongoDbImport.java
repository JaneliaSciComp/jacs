package org.janelia.it.jacs.compute.access.mongodb;

import java.io.File;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SubjectDAO;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
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
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.gui.search.criteria.FacetCriteria;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.EnumText;
import org.janelia.it.jacs.model.domain.ontology.Interval;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.FileGroup;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LSMSummaryResult;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineError;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SampleCellCountingResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SamplePostProcessingResult;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.screen.FlyLine;
import org.janelia.it.jacs.model.domain.screen.PatternMask;
import org.janelia.it.jacs.model.domain.screen.ScreenSample;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
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
	protected MongoCollection preferenceCollection;
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
    protected MongoCollection filterCollection;
    
    // Load state
    private LargeOperations largeOp;
    private String genderConsensus = null;
    private Map<String,String> lsmJsonFiles = new HashMap<String,String>();
    protected Map<Long,Long> ontologyTermIdToOntologyId = new HashMap<Long,Long>();
    
    public MongoDbImport() throws UnknownHostException {
        super(logger);
        
        this.subjectDao = new SubjectDAO(log);
		this.dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
    	dao.getMongo().setWriteConcern(WriteConcern.UNACKNOWLEDGED);
    	
    	this.subjectCollection = dao.getCollectionByClass(Subject.class);
        this.preferenceCollection = dao.getCollectionByClass(Preference.class);
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
        this.filterCollection = dao.getCollectionByClass(Filter.class);
    }

    public void dropDatabase() throws DaoException {
		try {
		    dao.getJongo().getDatabase().dropDatabase();
		}
		catch (Exception e) {
			throw new DaoException("Error clearing index with MongoDB",e);
		}
    }

    public void loadAllEntities() throws DaoException {

        log.info("Building disk-based SAGE property map");
        this.largeOp = new LargeOperations(this);
        largeOp.buildSageImagePropMap();

        log.info("Building LSM property map");
        buildLsmAttributeMap();
        
        log.info("Loading data into MongoDB");
        getSession().setFlushMode(FlushMode.MANUAL);
        
        long startAll = System.currentTimeMillis(); 

        log.info("Adding subjects");
        loadSubjects();
        
        log.info("Adding samples");
        // TODO: handle deleted (i.e. "hidden") neurons
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
            for(org.janelia.it.jacs.model.user_data.prefs.SubjectPreference sp : subject.getPreferenceMap().values()) {
                Preference preference = new Preference(subject.getKey(), sp.getCategory(), sp.getName(), sp.getValue());
                preference.setId(dao.getNewId());
                preferenceCollection.insert(preference);
            }
            
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
                dataSetCollection.insert(dataSet);
                
                // Free memory by releasing the reference to this entire entity tree
                i.remove();
                resetSession();

                log.info("  Loading "+dataSetEntity.getName()+" ("+dataSetEntity.getOwnerKey()+") took "+(System.currentTimeMillis()-start)+" ms");
                loaded++;
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
//                if (!"user:dolanm".equals(subjectKey)) continue;
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
        if (!samples.isEmpty()) {
            int loaded = loadSamples(samples);
            log.info("Loading " + loaded + " samples for " + subjectKey + " took "
                    + (System.currentTimeMillis() - start) + " ms");
        }
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

        List<LSMImage> allLsms = new ArrayList<LSMImage>();
        
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
                    lsmImages.add(lsmImage);
                    lsmReferences.add(getReference(lsmEntity));
                }
            }

            allLsms.addAll(lsmImages);
            imageCollection.insert(lsmImages.toArray());
            
            SampleTile tile = new SampleTile();
            tile.setName(tileEntity.getName());
            tile.setAnatomicalArea(tileEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA));
            tile.setLsmReferences(lsmReferences);
            tile.setFiles(images);
            tiles.add(tile);
        }
        
        sample.setTiles(tiles);
        
        List<SamplePipelineRun> runs = new ArrayList<>();
        for(Entity runEntity : EntityUtils.getChildrenOfType(sampleEntity, EntityConstants.TYPE_PIPELINE_RUN)) {
            populateChildren(runEntity);
            
            List<PipelineResult> results = new ArrayList<>();
            
            for(Entity resultEntity : EntityUtils.getChildrenForAttribute(runEntity, EntityConstants.ATTRIBUTE_RESULT)) {
                populateChildren(resultEntity);

                if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_LSM_SUMMARY_RESULT)) {
                    LSMSummaryResult result = getLSMSummaryResult(allLsms, resultEntity);
                    results.add(result);
                }
                else if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {

                    List<PipelineResult> sprResults = new ArrayList<PipelineResult>();
                    for(Entity separationEntity : EntityUtils.getChildrenOfType(resultEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                        NeuronSeparation ns = getNeuronSeparation(parentSampleEntity, separationEntity);
                        if (ns!=null) sprResults.add(ns);
                    }
                    
                    SampleProcessingResult result = getSampleProcessingResult(resultEntity);
                    if (!sprResults.isEmpty()) {
                        result.setResults(sprResults);
                    }
                    
                    results.add(result);
                }
                else if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_POST_PROCESSING_RESULT)) {
                    SamplePostProcessingResult result = getSamplePostProcessingResult(sample, resultEntity);
                    results.add(result);
                }
                else if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_CELL_COUNTING_RESULT)) {
                    SampleCellCountingResult result = getSampleCellCountingResult(resultEntity);
                    results.add(result);
                }
                else if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
                    
                    Map<String,PipelineResult> nsResultMap = new HashMap<>();
                    for(Entity separationEntity : EntityUtils.getChildrenOfType(resultEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                        String objective = separationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
                        if (StringUtils.isEmpty(objective)) objective = "";
                        NeuronSeparation ns = getNeuronSeparation(parentSampleEntity, separationEntity);
                        if (ns!=null) nsResultMap.put(objective,ns);
                    }
                    
                    Entity verifyMovie = null;
                    List<Entity> resultImages = new ArrayList<>();
                    
                    Entity resultSupportingData = EntityUtils.getSupportingData(resultEntity);
                    populateChildren(resultSupportingData);
                    List<Entity> children = resultSupportingData.getOrderedChildren();
                    for(Entity resultFile : children) {
                        if (resultFile.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
                            if (!resultFile.getName().startsWith("Aligned")) continue;
                            resultImages.add(resultFile);
                        }
                        else if (resultFile.getEntityTypeName().equals(EntityConstants.TYPE_MOVIE)) {
                            verifyMovie = resultFile;
                        }
                    }
                    
                    if (resultImages.isEmpty()) {
                        resultImages.add(resultEntity);
                    }
                    
                    for(Entity imageEntity : resultImages) {
                        String objective = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
                        if (StringUtils.isEmpty(objective)) objective = "";
                        PipelineResult ns = nsResultMap.get(objective);
                        List<PipelineResult> sprResults = new ArrayList<>();
                        if (ns!=null) {
                            sprResults.add(ns);
                        }
                        
                        SampleAlignmentResult alignmentResult = getAlignmentResult(resultEntity, imageEntity, verifyMovie);
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
            run.setId(runEntity.getId());
            run.setName(runEntity.getName());
            run.setCreationDate(runEntity.getCreationDate());
            run.setPipelineProcess(runEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS));
            run.setPipelineVersion(1);
            
            Entity errorEntity = EntityUtils.findChildWithType(runEntity, EntityConstants.TYPE_ERROR);
            if (errorEntity!=null) {
                PipelineError error = new PipelineError();
                error.setFilepath(getFilepath(errorEntity));
                run.setError(error);
            }

            if (!results.isEmpty()) {
                run.setResults(results);
            }
            
            runs.add(run);
        }

        sample.setPipelineRuns(runs);
        
        return sample;
    }
    
    private void addStackFiles(Entity imageEntity, Map<FileType,String> files, HasFilepath result) throws Exception {
        
        populateChildren(imageEntity);
        
        // Add the MIPs and movies
        addImage(files,FileType.ReferenceMip,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE)));
        addImage(files,FileType.SignalMip,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE)));
        addImage(files,FileType.AllMip,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALL_MIP_IMAGE)));
        addImage(files,FileType.Signal1Mip,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_1_MIP_IMAGE)));
        addImage(files,FileType.Signal2Mip,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_2_MIP_IMAGE)));
        addImage(files,FileType.Signal3Mip,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_3_MIP_IMAGE)));
        addImage(files,FileType.RefSignal1Mip,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_1_REF_MIP_IMAGE)));
        addImage(files,FileType.RefSignal2Mip,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_2_REF_MIP_IMAGE)));
        addImage(files,FileType.RefSignal3Mip,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_3_REF_MIP_IMAGE)));
        addImage(files,FileType.FastStack,getRelativeFilename(result,getFilepath(imageEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE))));
        
        if (files.isEmpty()) {
            // This is a special case where no explicit MIPs have been defined. Let's assume that the default 2d image is a signal MIP.
            addImage(files,FileType.SignalMip,getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE)));
        }

        // Add the stack
        if (imageEntity.getName().endsWith("h5j")) {
            addStack(imageEntity, files, FileType.VisuallyLosslessStack, result);
            addStack(imageEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_LOSSLESS_IMAGE), files, FileType.LosslessStack, result);
        }
        else {
            addStack(imageEntity, files, FileType.LosslessStack, result);
            addStack(imageEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_SLIGHTLY_LOSSY_IMAGE), files, FileType.VisuallyLosslessStack, result);
        }
    }
    
    private void addStack(Entity imageEntity, Map<FileType,String> files, FileType type, HasFilepath result) {
    	if (imageEntity==null) return;
        addImage(files,type,getRelativeFilename(result,getFilepath(imageEntity)));
    }

    private LSMSummaryResult getLSMSummaryResult(List<LSMImage> lsms, Entity resultEntity) throws Exception {
    	LSMSummaryResult result = new LSMSummaryResult();
    	result.setId(resultEntity.getId());
        result.setName(resultEntity.getName());
        result.setCreationDate(resultEntity.getCreationDate());
        result.setFilepath(getFilepath(resultEntity));
        Set<String> keys = new HashSet<>();
        for(LSMImage lsm : lsms) {
            String name = lsm.getName();
            int index = name.indexOf('.');
            String key = index<1 ? name : name.substring(0, index);
            keys.add(key);
        }
        keys.add("montage");
        result.setGroups(createFileGroups(result, resultEntity, keys));
        return result;
    }
    
    private SampleProcessingResult getSampleProcessingResult(Entity resultEntity) throws Exception {
        SampleProcessingResult result = new SampleProcessingResult();
        result.setId(resultEntity.getId());
        result.setName(resultEntity.getName());
        result.setCreationDate(resultEntity.getCreationDate());
        result.setFilepath(getFilepath(resultEntity));
        String area = resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
        if (!StringUtils.isEmpty(area)) {
            result.setAnatomicalArea(area);
        }
        else {
            result.setAnatomicalArea("Brain");
        }
        
        Map<FileType,String> files = new HashMap<FileType,String>();

        // In some cases, the stack no longer has access to the MIPs (e.g. GMR_33A12_AE_01-57C10PEST_attp8_3stop1-A-20120608_20_A3), so we need to take them from the result itself
        addImage(files,FileType.ReferenceMip,getRelativeFilename(result,resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE)));
        addImage(files,FileType.SignalMip,getRelativeFilename(result,resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE)));
        
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
        		String childFilepath = getFilepath(child);
        		if (childName.endsWith("lsm.json")) {
        			String name = ArchiveUtils.getDecompressedFilepath(childName);
        			lsmJsonFiles.put(name, childFilepath);
        		}
        	}
        }

        if (!files.isEmpty()) result.setFiles(files);
        
        return result;
    }

    private SamplePostProcessingResult getSamplePostProcessingResult(ObjectiveSample objectiveSample, Entity resultEntity) throws Exception {
        SamplePostProcessingResult result = new SamplePostProcessingResult();
        result.setId(resultEntity.getId());
        result.setName(resultEntity.getName());
        result.setCreationDate(resultEntity.getCreationDate());
        result.setFilepath(getFilepath(resultEntity));
        Set<String> keys = new HashSet<>();
        for(SampleTile tile : objectiveSample.getTiles()) {
            keys.add(tile.getName());
            keys.add(tile.getAnatomicalArea());
        }
        keys.add("montage");
        result.setGroups(createFileGroups(result, resultEntity, keys));
        return result;
    }

    private Map<String,FileGroup> createFileGroups(HasFilepath parent, Entity resultEntity, Set<String> keys) throws Exception {

    	Map<String,FileGroup> groups = new HashMap<>();
        populateChildren(resultEntity);
        Entity supportingDataEntity = EntityUtils.getSupportingData(resultEntity);
        if (supportingDataEntity!=null) {
            for(Entity child : supportingDataEntity.getChildren()) {
                String childName = child.getName();
                String childFilepath = getFilepath(child);

                int d = childName.indexOf('.');
                String name = childName.substring(0, d);
                String ext = childName.substring(d+1);
                
                FileType fileType = null;

                String key = null;
                if ("lsm.json".equals(ext)) {
                	key = name;
                	fileType = FileType.LsmMetadata;
                }
                else if ("lsm.metadata".equals(ext)) {
                    // Ignore, to get rid of the old-style Perl metadata files
                    continue;
                }
                else if ("properties".equals(ext)) {
                    // Ignore, properties should already be entity attributes
                    continue;
                }
                else {
                	int u = name.lastIndexOf('_');
                	key = name.substring(0, u);
                	String type = name.substring(u+1);
                	if ("png".equals(ext)) {
                		if ("all".equals(type)) {
                			fileType = FileType.AllMip;	
                		}
                		else if ("reference".equals(type)) {
                			fileType = FileType.ReferenceMip;	
                		}
                		else if ("signal".equals(type)) {
                			fileType = FileType.SignalMip;	
                		}
                		else if ("signal1".equals(type)) {
                			fileType = FileType.Signal1Mip;	
                		}
                		else if ("signal2".equals(type)) {
                			fileType = FileType.Signal2Mip;	
                		}
                		else if ("signal3".equals(type)) {
                			fileType = FileType.Signal3Mip;	
                		}
                		else if ("refsignal1".equals(type)) {
                			fileType = FileType.RefSignal1Mip;	
                		}
                		else if ("refsignal2".equals(type)) {
                			fileType = FileType.RefSignal2Mip;	
                		}
                		else if ("refsignal3".equals(type)) {
                			fileType = FileType.RefSignal3Mip;	
                		}
	                }
	                else if ("mp4".equals(ext)) {
	                	if ("all".equals(type) || "movie".equals(type)) {
                			fileType = FileType.AllMovie;	
                		}
                		else if ("reference".equals(type)) {
                			fileType = FileType.ReferenceMovie;	
                		}
                		else if ("signal".equals(type)) {
                			fileType = FileType.SignalMovie;	
                		}
	                }
	            }
                
                if (fileType==null) {
                    log.warn("  Could not determine file type for: "+childName);
                    continue;
                }
                
                String translatedKey = key;
                for(String trueKey : keys) {
                    if (key.equals(trueKey)) {
                        translatedKey = trueKey;
                        break;
                    }
                    else if (key.contains("-"+trueKey+"-")) {
                        translatedKey = trueKey;
                        break;
                    }
                }
                
                FileGroup group = groups.get(translatedKey);
                if (group==null) {
                	group = new FileGroup();
                	group.setFilepath(parent.getFilepath());
                	group.setFiles(new HashMap<FileType,String>());
                	groups.put(translatedKey, group);
                }
                
                group.getFiles().put(fileType, getRelativeFilename(group, childFilepath));
            }
        }
        
        return groups;
    }
    
    private SampleCellCountingResult getSampleCellCountingResult(Entity resultEntity) {
    	SampleCellCountingResult result = new SampleCellCountingResult();
        result.setId(resultEntity.getId());
    	result.setName(resultEntity.getName());
        result.setCreationDate(resultEntity.getCreationDate());
        result.setFilepath(getFilepath(resultEntity));
        
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
        		String childFilepath = getFilepath(child);
        		
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
        result.setId(alignmentEntity.getId());
        result.setName(alignmentEntity.getName());
        result.setCreationDate(imageEntity.getCreationDate());
        result.setAlignmentSpace(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE));
        result.setBoundingBox(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_BOUNDING_BOX));
        result.setChannelColors(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS));
        result.setChannelSpec(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
        result.setFilepath(getFilepath(alignmentEntity));
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
            addImage(files,FileType.AlignmentVerificationMovie,getRelativeFilename(result,getFilepath(movieEntity)));
        }
        if (!files.isEmpty()) result.setFiles(files);
        
        return result;
    }

    private LSMImage getLSMImage(Entity sampleEntity, Entity lsmEntity) throws Exception {

        LSMImage lsm = (LSMImage)getImage(lsmEntity);
        // An LSM file must have a lossless stack
        String filepath = lsm.getFiles().get(FileType.LosslessStack);
        if (filepath==null) {
            log.warn("    LSM cannot be imported because it has no filepath: "+lsmEntity.getId());
            return null;
        }
        
        Map<FileType, String> files = lsm.getFiles();
        
        String name = ArchiveUtils.getDecompressedFilepath(lsm.getName());
        String jsonFilepath = lsmJsonFiles.get(name);
        if (jsonFilepath!=null) {
            addImage(files,FileType.LsmMetadata,jsonFilepath);
        }
        
        // Add movies, based on the MIP filenames
        String allMip = files.get(FileType.AllMip);
        if (allMip!=null) {
        	files.put(FileType.AllMovie,allMip.replace("png", "mp4"));
        }
        String referenceMip = files.get(FileType.ReferenceMip);
        if (referenceMip!=null) {
        	files.put(FileType.ReferenceMovie,referenceMip.replace("png", "mp4"));
        }
        String signalMip = files.get(FileType.SignalMip);
        if (signalMip!=null) {
        	files.put(FileType.SignalMovie,signalMip.replace("png", "mp4"));
        }
        
        if (sampleEntity!=null) lsm.setSample(getReference(sampleEntity));
        lsm.setAge(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_AGE));
        lsm.setAnatomicalArea(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA));
        lsm.setChannelColors(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS));
        lsm.setChannelDyeNames(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES));
        lsm.setBrightnessCompensation(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_BRIGHTNESS_COMPENSATION));
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
            log.warn("    Cannot find LSM#"+lsm.getId()+" in SAGE, with SAGE Id "+sageId);
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
                        else if (fieldType.equals(Long.class)) {

                            log.info(key+" is a "+value.getClass().getName()+" with value "+value);
                            
                            trueValue = Long.parseLong(value.toString());
                        }
                        else if (fieldType.equals(Integer.class)) {
                            if (!StringUtils.isEmpty(value.toString())) {
                                trueValue = Integer.parseInt(value.toString());
                            }
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
        image.setFilepath(getFilepath(imageEntity));
        String numChannels = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS);
        if (numChannels!=null) {
            image.setNumChannels(Integer.parseInt(numChannels));
        }
        image.setObjective(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE));
        image.setOpticalResolution(sanitizeCSV(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)));
        image.setImageSize(sanitizeCSV(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)));

        Map<FileType,String> files = new HashMap<FileType,String>();
        String path = image.getFilepath();
        if (path!=null) {
            if (path.endsWith(".png") || path.endsWith(".gif") || path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                addImage(files,FileType.Unclassified2d,path);
            }
            else {
                addStackFiles(imageEntity, files, null);    
            }
        }
        image.setFiles(files);
        
        return image;
    }
    
    private NeuronSeparation getNeuronSeparation(Entity sampleEntity, Entity separationEntity) throws Exception {
        if (separationEntity==null) return null;
        populateChildren(separationEntity);
        
        List<NeuronFragment> neuronFragments = new ArrayList<NeuronFragment>();
        Entity nfCollectionEntity = EntityUtils.getLatestChildOfType(separationEntity, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
        if (nfCollectionEntity==null) return null;
        
        populateChildren(nfCollectionEntity);
        for(Entity fragmentEntity : EntityUtils.getChildrenOfType(nfCollectionEntity, EntityConstants.TYPE_NEURON_FRAGMENT)) {
            NeuronFragment neuronFragment = getNeuronFragment(sampleEntity, separationEntity, fragmentEntity);
            neuronFragments.add(neuronFragment);
        }

        fragmentCollection.insert(neuronFragments.toArray());
        
        ReverseReference fragmentsReference = new ReverseReference();
        fragmentsReference.setCount(new Long(neuronFragments.size()));
        fragmentsReference.setReferringClassName(NeuronFragment.class.getName());
        fragmentsReference.setReferenceAttr("separationId");
        fragmentsReference.setReferenceId(separationEntity.getId());
        
        NeuronSeparation neuronSeparation = new NeuronSeparation();
        neuronSeparation.setId(separationEntity.getId());
        neuronSeparation.setName(separationEntity.getName());
        neuronSeparation.setCreationDate(separationEntity.getCreationDate());
        neuronSeparation.setFilepath(getFilepath(separationEntity));
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
        neuronFragment.setFilepath(getFilepath(separationEntity));
        
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
        
        String paFilepath = getFilepath(patternAnnotationEntity);
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
            addImage(images,FileType.LosslessStack,getRelativeFilename(screenSample,getFilepath(alignedStack)));
            addImage(images,FileType.AllMip,getRelativeFilename(screenSample,alignedStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH)));
        }
        Entity heatmap = EntityUtils.findChildWithName(patternAnnotationEntity, "Heatmap");
        if (heatmap!=null) {
            addImage(images,FileType.HeatmapStack,getRelativeFilename(screenSample,getFilepath(heatmap)));
            addImage(images,FileType.HeatmapMip,getRelativeFilename(screenSample,heatmap.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH)));
        }
        screenSample.setImages(images);
        
        ReverseReference masksRef = new ReverseReference();
        masksRef.setCount(new Long(masks.size()));
        masksRef.setReferringClassName(PatternMask.class.getName());
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
        addImage(images,FileType.LosslessStack,getRelativeFilename(mask,getFilepath(maskEntity)));
        addImage(images,FileType.AllMip,getRelativeFilename(mask,default2dImageFilepath));
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
        
        Reference target = new Reference(getClassName(targetType), targetId);
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
        		// TODO: Fix this. It creates references to samples and neurons just fine, but compartments are not domain objects so the reference type is null
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

        Workspace workspace = new Workspace();
        workspace.setOwnerKey(subjectKey);
        workspace.setReaders(getDefaultSubjectKeys(subjectKey));
        workspace.setWriters(workspace.getReaders());
        
        Entity workspaceEntity = getDefaultWorkspace(subjectKey);
        if (workspaceEntity==null) {
            workspace.setId(dao.getNewId());
            workspace.setName("Default Workspace");
            Date now = new Date();
            workspace.setCreationDate(now);
            workspace.setUpdatedDate(now);
            workspace.setChildren(new ArrayList<Reference>());
        }
        else {
            LinkedList<Entity> rootFolders = new LinkedList<Entity>(workspaceEntity.getOrderedChildren());
            Collections.sort(rootFolders, new EntityRootComparator(subjectKey));
            List<Reference> children = loadRootFolders(subjectKey, rootFolders, visited);
            workspace.setId(workspaceEntity.getId());
            workspace.setName(workspaceEntity.getName());
            workspace.setCreationDate(workspaceEntity.getCreationDate());
            workspace.setUpdatedDate(workspaceEntity.getUpdatedDate());
            workspace.setChildren(children);
        }
        
        treeNodeCollection.insert(workspace);
        
        log.info("Loading workspace for "+subjectKey+" took "+(System.currentTimeMillis()-start)+" ms");
    }
    
    private List<Reference> loadRootFolders(String subjectKey, Deque<Entity> rootFolders, Map<Long,DomainObject> loaded) {

        Set<Long> visitedSet = new HashSet<Long>();
        List<Reference> roots = new ArrayList<Reference>();
        
        for(Iterator<Entity> i = rootFolders.iterator(); i.hasNext(); ) {
            Entity folderEntity = i.next();
                        
            if (!subjectKey.equals(folderEntity.getOwnerKey())) continue;
            
            Session session = null;
            try {
                long start = System.currentTimeMillis();
                
                session = openNewExternalSession();
                DomainObject domainObject = loadFolderHierarchy(folderEntity, visitedSet, loaded, "  ");
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
    
    private DomainObject loadFolderHierarchy(Entity folderEntity, Set<Long> visitedSet, Map<Long,DomainObject> loaded, String indent) throws Exception {

    	// TODO: should we load these?
    	
    	if ("user:jenetta".equals(folderEntity.getOwnerKey())) {
    		return null;
    	}
    	
    	if ("supportingFiles".equals(folderEntity.getName())) {
    		return null;
    	}
    	
        log.info(indent+"Loading "+folderEntity.getName());
        
        if (loaded.containsKey(folderEntity.getId())) {
            return loaded.get(folderEntity.getId());
        }
        else if (visitedSet.contains(folderEntity.getId())) {
            // This should only happen when there are loops, which we don't want to load anyway
            log.warn(indent+"Skipping reference to visited but not yet loaded: "+folderEntity.getName());
            return null;
        }
        
        visitedSet.add(folderEntity.getId());
        
        // What does this folder contain?
        Multimap<String, Entity> childrenByType = ArrayListMultimap.<String, Entity>create();
		for(Entity childEntity : folderEntity.getOrderedChildren()) {
			childrenByType.put(childEntity.getEntityTypeName(), childEntity);
		}
        
        if (childrenByType.keySet().size()==1 && EntityUtils.getChildrenOfType(folderEntity, EntityConstants.TYPE_FOLDER).isEmpty()) {
        	// This folder contains only one type of non-Folder object, so we'll consider it an object set
        	ObjectSet objectSet = getObjectSet(folderEntity, folderEntity.getOrderedChildren(), indent);
            objectSetCollection.insert(objectSet);
            log.info(indent+"Generated object set '"+objectSet.getName()+"' with "+objectSet.getNumMembers()+" members");
            loaded.put(objectSet.getId(), objectSet);
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
	    Set<Reference> children = new LinkedHashSet<Reference>();
	    Map<String,ObjectSet> extraSetCache = new LinkedHashMap<>();
	    
	    if ("Data Sets".equals(folderEntity.getName())) {
	    	// Create a canned filter for each data set
	    	Date now = new Date();
	    	for(DataSet dataSet : dao.getDataSets(folderEntity.getOwnerKey())) {
	    		// We only want the data sets owned by this user
	    		if (!dataSet.getOwnerKey().equals(folderEntity.getOwnerKey())) continue;
                Filter filter = new Filter();
                filter.setId(dao.getNewId());
                filter.setName(dataSet.getName());
                filter.setOwnerKey(dataSet.getOwnerKey());
                filter.setSearchClass(Sample.class.getName());
                filter.setCreationDate(now);
                filter.setUpdatedDate(now);
                FacetCriteria dataSetCriteria = new FacetCriteria();
                dataSetCriteria.setAttributeName("dataSet");
                dataSetCriteria.setValues(Sets.newHashSet(dataSet.getIdentifier()));
                filter.addCriteria(dataSetCriteria);
                filterCollection.insert(filter);
                children.add(getReference(filter));
                log.info(indent+"  Added canned data set filter: "+filter.getName());
			}
		}
	    else {
		    // Load folder children
		    for(String childType : childrenByType.keySet()) {
		    	Collection<Entity> childEntities = childrenByType.get(childType);
		    	log.info(indent+"Processing "+childEntities.size()+" children of type "+childType);
		    	if (EntityConstants.TYPE_FOLDER.equals(childType)) {
		    		for(Entity childFolder : childEntities) {
		    			DomainObject domainObject = loadFolderHierarchy(childFolder, visitedSet, loaded, indent+"  ");
		    			if (domainObject!=null) {
			                children.add(getReference(domainObject));
		    			}
		    		}
		    	}
		    	else {
		    		// Create new object set for items of this type
		    		Entity newFolder = new Entity();
		    		newFolder.setId(dao.getNewId());
		    		newFolder.setName(childType);
		    		newFolder.setOwnerKey(folderEntity.getOwnerKey());
		    		newFolder.setEntityActorPermissions(folderEntity.getEntityActorPermissions());
		    		newFolder.setCreationDate(folderEntity.getCreationDate());
		    		newFolder.setUpdatedDate(folderEntity.getUpdatedDate());
			    	ObjectSet objectSet = getObjectSet(newFolder, childEntities, indent);
			    	if (objectSet!=null) {			    	    
			    		String setLabel = objectSet.getType();
			    		if (setLabel!=null) {
    			    		ObjectSet existingSet = extraSetCache.get(setLabel);
    			    		if (existingSet!=null) {
    			    			if (objectSet.hasMembers()) {
    				    			for(Long memberId : objectSet.getMembers()) {
    				    				existingSet.addMember(memberId);
    				    			}
    			    			}
    			    		}
    			    		else {
    					    	objectSet.setName(setLabel);
    					    	extraSetCache.put(setLabel, objectSet);
    			    		}
			    		}
			    		else {
			    		    log.warn(indent+"  Discarding "+childEntities.size()+" children of type "+childType);
			    		}
			    	}
		    	}
		    }
		    
		    for(ObjectSet objectSet : extraSetCache.values()) {
		    	log.info(indent+"  Generated extra object set: "+objectSet.getName()+" with "+objectSet.getNumMembers()+" members");
			    objectSetCollection.insert(objectSet);
		        children.add(getReference(objectSet));
		    }
	    }
	    
        if (!children.isEmpty()) {
            treeNode.setChildren(new ArrayList<Reference>(children));
        }

        treeNodeCollection.insert(treeNode);
        log.info(indent+"Created tree node: "+treeNode.getName());
        loaded.put(treeNode.getId(), treeNode);
        return treeNode;
    }

    private ObjectSet getObjectSet(Entity folderEntity, Collection<Entity> items, String indent) throws Exception {
		ObjectSet objectSet = new ObjectSet();
		objectSet.setId(folderEntity.getId());
		objectSet.setName(folderEntity.getName());
		objectSet.setOwnerKey(folderEntity.getOwnerKey());
		objectSet.setReaders(getSubjectKeysWithPermission(folderEntity, "r"));
		objectSet.setWriters(getSubjectKeysWithPermission(folderEntity, "w"));
		objectSet.setCreationDate(folderEntity.getCreationDate());
		objectSet.setUpdatedDate(folderEntity.getUpdatedDate());

	    // --------------------------------------------------------------------------------
		// Make sure all objects are of the same type
		String setType = null;
		List<Entity> entityMembers = new ArrayList<Entity>(); 
		for(Entity childEntity : items) {
			if (setType==null) {
				setType = childEntity.getEntityTypeName();	
			}
			else if (!setType.equals(childEntity.getEntityTypeName())) {
				log.warn(indent+"  Ignoring entity with type that does not match set ("+childEntity.getEntityTypeName()+"!="+setType+")");
				continue;
			}
			entityMembers.add(childEntity);
		}
		
		if (setType==null) {
			// Object set has no members 
			return objectSet;
		}

	    // --------------------------------------------------------------------------------
	    // Preprocess all objects and see if we need to do a bulk mapping
	    String translatedSetType = null;
		Map<Long,Entity> translatedEntities = new HashMap<Long,Entity>();
		
		if (getCollectionName(setType)==null) {
			// We don't know this type, attempt mass translation up to sample
			translatedEntities.putAll(translateToSample(indent, entityMembers, setType));
			if (!translatedEntities.isEmpty()) {
//				for(Long id : translatedEntities.keySet()) {
//					Entity translatedEntity = translatedEntities.get(id);
//					if (translatedEntity==null) {
//						log.info(indent+"  "+id+" --> null");
//					}
//					else {
//						log.info(indent+"  "+id+" --> "+translatedEntity.getId()+" ("+translatedEntity.getEntityTypeName()+")");
//					}
//				}
				translatedSetType = setType = "Sample";
			}
		}

//		log.info(indent+"  setType="+setType+", translatedEntities.size="+translatedEntities.size());
		
		// Translate sub-samples into samples
		if ("Sample".equals(setType)) {
			// We're at samples, but we may need to go one level higher to the parent sample
			Map<Long,Entity> secondaryTranslatedEntities = new HashMap<>();
			List<Long> subsampleIds = new ArrayList<>();
			Map<Long,Long> reverseMap = new HashMap<>();
		    for(Entity entity : entityMembers) {
		    	Long originalId = entity.getId();
		    	Entity sample = entity;
		    	if (translatedEntities.get(originalId)!=null) {
		    		sample = translatedEntities.get(originalId);
		    	}
	        	if (sample.getName().contains("~")) {
	        		subsampleIds.add(sample.getId());
	        		reverseMap.put(sample.getId(), originalId);
	        	}
		    }
		    
		    if (!subsampleIds.isEmpty()) {
				List<String> upMapping = new ArrayList<String>();
				upMapping.add("Sample");
				List<String> downMapping = new ArrayList<String>();
				List<MappedId> mappings = getProjectedResults(null, subsampleIds, upMapping, downMapping);
				Map<Long,Entity> mappedEntities = getMappedEntities(mappings);
	            for(MappedId mappedId : mappings) {
	            	Long subsampleId = mappedId.getOriginalId();
	            	Long originalId = reverseMap.get(subsampleId);
	            	secondaryTranslatedEntities.put(originalId,  mappedEntities.get(mappedId.getMappedId()));
	            }
            	log.info(indent+"  Translated "+subsampleIds.size()+ " sub samples to "+secondaryTranslatedEntities.size()+" parent samples");
            	if (!secondaryTranslatedEntities.isEmpty()) {
//    				for(Long id : secondaryTranslatedEntities.keySet()) {
//    					Entity translatedEntity = secondaryTranslatedEntities.get(id);
//    					if (translatedEntity==null) {
//    						log.info(indent+"  "+id+" --> null");
//    					}
//    					else {
//    						log.info(indent+"  "+id+" --> "+translatedEntity.getId()+" ("+translatedEntity.getEntityTypeName()+")");
//    					}
//    				}
            		translatedSetType = "Sample";
    			}
	            translatedEntities.putAll(secondaryTranslatedEntities);
			}
		}
//		log.info(indent+"  translatedEntities.size="+translatedEntities.size());
		
	    // --------------------------------------------------------------------------------
		// Load the members, translating them one-by-one if necessary
	    Collection<Long> memberIds = new LinkedHashSet<Long>();
	    
	    for(Entity childEntity : entityMembers) {
	    	
        	Entity importEntity = childEntity;
        	Entity translatedEntity = translatedEntities.get(childEntity.getId());
        	if (translatedEntity!=null) {
        		// already translated this above
        		logger.info(indent+"  Will reference "+translatedEntity.getEntityTypeName()+"#"+translatedEntity.getId()+" instead of "+importEntity.getEntityTypeName()+"#"+importEntity.getId());
        		importEntity = translatedEntity;
        	}
        	else if (TRANSLATE_ENTITIES && getCollectionName(importEntity.getEntityTypeName())==null) {
        		// See if we can substitute a higher-level entity for the one that the user referenced. For example, 
        		// if they referenced a sample processing result, we find the parent sample. Same goes for neuron separations, etc.
        		// The priority list defines the ordered list of possible entity types to try as ancestors.  
        		for (String entityType : entityTranslationPriority) {
        			Entity ancestor = getAncestorWithType(childEntity.getOwnerKey(), importEntity.getId(), entityType);
                	if (ancestor!=null) {
                		logger.info(indent+"  Will reference "+entityType+"#"+ancestor.getId()+" instead of unknown "+importEntity.getEntityTypeName()+"#"+importEntity.getId());                		
                        importEntity = ancestor;
                		break;
                	}
        		}
        	}

			if (translatedSetType==null) {
				translatedSetType = importEntity.getEntityTypeName();	
			}
			else if (!translatedSetType.equals(importEntity.getEntityTypeName())) {
				log.warn(indent+"  Ignoring entity with type that does not match set ("+importEntity.getEntityTypeName()+"!="+translatedSetType+")");
				continue;
			}
        
            String collectionName = getCollectionName(translatedSetType);
            if (INSERT_ROGUE_ENTITIES) {
                // A minor optimization, since we can only do rogue imports on images
                if ("image".equals(collectionName)) {
		            // Attempt imports of rogue entities which map to domain objects, but which have not been loaded by any other part of the import procedure
		            if (dao.getCollectionByName(collectionName).count("{_id:#}",importEntity.getId())<1) {
		            	attemptRogueImport(importEntity, indent);
		            }
                }
            }
            log.trace(indent+"  Adding "+importEntity.getEntityTypeName()+"#"+importEntity.getId()+" ("+importEntity.getName()+")");
            memberIds.add(importEntity.getId());
	    }
		
        if (translatedSetType==null) {
        	log.info(indent+"  Set type is null, with "+memberIds.size()+" ids");
        }

        String className = getClassName(translatedSetType);
        if (className==null) {
        	log.info(indent+"  Could not find domain class for set type: "+translatedSetType);
        }
        
        objectSet.setClassName(className);
        if (!memberIds.isEmpty()) {
            objectSet.setMembers(new ArrayList<>(memberIds));
        }
        
		return objectSet;
	}
    
    private Map<Long,Entity> translateToSample(String indent, List<Entity> entityMembers, String startingType) throws Exception {

		Map<Long,Entity> translatedEntities = new HashMap<Long,Entity>();
	    List<Long> originalIds = new ArrayList<Long>();
	    for(Entity childEntity : entityMembers) {
    		originalIds.add(childEntity.getId());
	    }
	    
		List<String> upMapping = new ArrayList<String>();
		List<String> downMapping = new ArrayList<String>();
		
		if (EntityConstants.TYPE_IMAGE_TILE.equals(startingType)) {
			upMapping.add("Supporting Data");
		}
		else if (EntityConstants.TYPE_PIPELINE_RUN.equals(startingType)) {
		}
		else if (EntityConstants.TYPE_LSM_SUMMARY_RESULT.equals(startingType)
				|| EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT.equals(startingType)
				|| EntityConstants.TYPE_POST_PROCESSING_RESULT.equals(startingType)
				|| EntityConstants.TYPE_ALIGNMENT_RESULT.equals(startingType)) {
			upMapping.add("Pipeline Run");
		}
		else if (EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT.equals(startingType)) {
			upMapping.add("Result");
			upMapping.add("Pipeline Run");
		}
		else if (EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION.equals(startingType)) {
			upMapping.add("Neuron Separator Pipeline Result");
			upMapping.add("Result");
			upMapping.add("Pipeline Run");
		}
		else {
			log.warn(indent+"  Cannot bulk translate from '"+startingType+"'");
			return translatedEntities;
		}
		upMapping.add("Sample");
		
		List<MappedId> mappings = getProjectedResults(null, originalIds, upMapping, downMapping);
		Map<Long,Entity> mappedEntities = getMappedEntities(mappings);
		
        for(MappedId mappedId : mappings) {
        	translatedEntities.put(mappedId.getOriginalId(), mappedEntities.get(mappedId.getMappedId()));
        }
        if (!translatedEntities.isEmpty()) {
        	log.info(indent+"  Translated "+originalIds.size()+ " "+startingType+"s to "+translatedEntities.size()+" Samples");
        }
        return translatedEntities;
    }

	private void attemptRogueImport(Entity entity, String indent) {
		
    	String entityType = entity.getEntityTypeName();
        String collectionName = getCollectionName(entityType);

        if (entity.getName().endsWith(".mask") || entity.getName().endsWith(".chan") ) {
            return;
        }
        
        log.info(indent+"  Attempting import of rogue entity: "+entityType+"#"+entity.getId());
        
        try {
            String filepath = getFilepath(entity);
            if (EntityConstants.TYPE_LSM_STACK.equals(entityType)) {
                if (filepath==null) {
                    // Attempt to find the "real" LSM entity in the objective sample
                    List<Long> lsmIds = new ArrayList<>();
                    lsmIds.add(entity.getId());
                    List<String> upMapping = new ArrayList<String>();
                    upMapping.add("Sample");
                    List<String> downMapping = new ArrayList<String>();
                    upMapping.add("Sample");
                    upMapping.add("Supporting Files");
                    upMapping.add("Image Tile");
                    upMapping.add("LSM Stack");
                    List<MappedId> mappings = getProjectedResults(null, lsmIds, upMapping, downMapping);
                    for(MappedId mappedId : mappings) {
                        entity = getEntityById(mappedId.getMappedId());
                        log.info(indent+"  Found real LSM in objective sample: "+entity.getId());
                        break;
                    }
                }
                
            	LSMImage image = getLSMImage(null, entity);
            	if (image!=null) {
            	    dao.getCollectionByName(collectionName).save(image);
            	}
            }
            else if (EntityConstants.TYPE_IMAGE_3D.equals(entityType)) {
            	Image image = getImage(entity);
            	dao.getCollectionByName(collectionName).save(image);
            }
            else if (EntityConstants.TYPE_IMAGE_2D.equals(entityType)) {
            	Image image = getImage(entity);
            	dao.getCollectionByName(collectionName).save(image);
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
        return new Reference(getClassName(entity.getEntityTypeName()), entity.getId());
	}

    private Reference getReference(DomainObject domainObject) {
    	return new Reference(domainObject.getClass().getName(),domainObject.getId());
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

    private Class<?> getClass(String entityType) {
        if (EntityConstants.TYPE_SAMPLE.equals(entityType)) {
            return Sample.class;
        }
        else if (EntityConstants.TYPE_NEURON_FRAGMENT.equals(entityType)) {
            return NeuronFragment.class;
        }
        else if (EntityConstants.TYPE_FOLDER.equals(entityType)) {
            return TreeNode.class;
        }
        else if (EntityConstants.TYPE_ANNOTATION.equals(entityType)) {
            return Annotation.class;
        }
        else if (EntityConstants.TYPE_ONTOLOGY_ROOT.equals(entityType)) {
            return Ontology.class;
        }
        else if (EntityConstants.TYPE_SCREEN_SAMPLE.equals(entityType)) {
            return ScreenSample.class;
        }
        else if (EntityConstants.TYPE_ALIGNED_BRAIN_STACK.equals(entityType)) {
            return PatternMask.class;
        }
        else if (EntityConstants.TYPE_FLY_LINE.equals(entityType)) {
            return FlyLine.class;
        }
        else if (EntityConstants.TYPE_COMPARTMENT_SET.equals(entityType)) {
            return CompartmentSet.class;
        }
        else if (EntityConstants.TYPE_ALIGNMENT_BOARD.equals(entityType)) {
            return AlignmentBoard.class;
        }
        else if (EntityConstants.TYPE_LSM_STACK.equals(entityType)) {
            return LSMImage.class;
        }
        else if (EntityConstants.TYPE_IMAGE_3D.equals(entityType)) {
            return Image.class;
        }
        else if (EntityConstants.TYPE_IMAGE_2D.equals(entityType)) {
            return Image.class;
        }
        return null;
    }

    private String getClassName(String entityType) {
        Class<?> clazz = getClass(entityType);
        return clazz==null?null:clazz.getName();
    }
    
    private String getCollectionName(String entityType) {
        return DomainUtils.getCollectionName(getClass(entityType));
    }

    /**
     * Convert non-standard gender values like "Female" into standardized codes like "f". The
     * four standardized codes are "m", "f", "x", and "NO_CONSENSUS" in the case of samples.
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
     */
    private String sanitizeCSV(String res) {
        if (res==null) return res;
        return res.replaceAll("'", "");
    }
    
    private String getFilepath(Entity entity) {
    	if (entity==null) return null;
        String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        if (filepath==null) filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_JFS_PATH);
        return filepath;
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
        
    private final Map<String,String> domainLabelCache = new HashMap<>();
    
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
