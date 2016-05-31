package org.janelia.it.jacs.compute.access.mongodb;

import java.io.File;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
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

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.SubjectDAO;
import org.janelia.it.jacs.compute.access.large.MongoLargeOperations;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
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
import org.janelia.it.jacs.model.domain.sample.CuratedNeuron;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.FileGroup;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LSMSummaryResult;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineError;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SampleCellCountingResult;
import org.janelia.it.jacs.model.domain.sample.SamplePatternAnnotationNormalizedResult;
import org.janelia.it.jacs.model.domain.sample.SamplePatternAnnotationResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SamplePostProcessingResult;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.screen.FlyLine;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.SAGEAttribute;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.ISO8601Utils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.jongo.MongoCollection;
import org.reflections.ReflectionUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Sets;
import com.mongodb.WriteConcern;

import net.sf.ehcache.Cache;

/**
 * Data access to the MongoDB data store.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDbImport extends AnnotationDAO {

    private static final Logger log = Logger.getLogger(MongoDbImport.class);

	protected static final String ONTOLOGY_TERM_TYPES_PACKAGE = "org.janelia.it.jacs.model.domain.ontology";
	protected static final String NO_CONSENSUS_VALUE = "NO_CONSENSUS";
	protected static final boolean TRANSLATE_ENTITIES = true;
	protected static final boolean INSERT_ROGUE_ENTITIES = true;
	
    private static final String[] entityTranslationPriority = { EntityConstants.TYPE_SAMPLE, EntityConstants.TYPE_SCREEN_SAMPLE };

    private static final String SCREEN_OBJECTIVE = "20x";
    private static final String SCREEN_CHAN_SPEC = "src";
    private static final String SCREEN_ALIGNMENT_RESULT_NAME = "JBA Alignment";
    private static final String SCREEN_ALIGNMENT_SPACE = "Unified 20x Alignment Space";
    private static final String SCREEN_ANATOMICAL_AREA = "Brain";
    private static final String SCREEN_DEFAULT_DATA_SET = "flylight_gen1_gal4";
    private static final NumberFormat ALIGNMENT_SCORE_FORMATTER = new DecimalFormat("#0.00000");

    private static final String TEMP_REF_CLASS = "TEMP_REF";
    
    protected final DomainDAO dao;
	protected final SubjectDAO subjectDao;
	protected final SageDAO sageDao;
	
	// Cached collections
	protected final MongoCollection subjectCollection;
	protected final MongoCollection preferenceCollection;
    protected final MongoCollection treeNodeCollection;
    protected final MongoCollection dataSetCollection;
    protected final MongoCollection releaseCollection;
	protected final MongoCollection sampleCollection;
    protected final MongoCollection flyLineCollection;
    protected final MongoCollection imageCollection;
    protected final MongoCollection fragmentCollection;
    protected final MongoCollection annotationCollection;
    protected final MongoCollection ontologyCollection;
    protected final MongoCollection compartmentSetCollection;
    protected final MongoCollection alignmentBoardCollection;
    protected final MongoCollection filterCollection;
    
    // Load state
    private MongoLargeOperations largeOp;
    private String genderConsensus = null;
    private Map<String,String> lsmJsonFiles = new HashMap<>();
    private Map<Long,Long> ontologyTermIdToOntologyId = new HashMap<>();
    private Set<Annotation> currAnnotations = new HashSet<>();
    
    public MongoDbImport() throws UnknownHostException {
        super(log);
        
        this.subjectDao = new SubjectDAO(log);
        this.sageDao = new SageDAO(log);
		this.dao = DomainDAOManager.getInstance().getDao();
    	dao.getMongo().setWriteConcern(WriteConcern.UNACKNOWLEDGED);
    	
    	this.subjectCollection = dao.getCollectionByClass(Subject.class);
        this.preferenceCollection = dao.getCollectionByClass(Preference.class);
    	this.treeNodeCollection = dao.getCollectionByClass(TreeNode.class);
    	this.dataSetCollection = dao.getCollectionByClass(DataSet.class);
    	this.releaseCollection = dao.getCollectionByClass(LineRelease.class);
    	this.sampleCollection = dao.getCollectionByClass(Sample.class);
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
        this.largeOp = new MongoLargeOperations(dao);

        log.info("Building LSM property map");
        largeOp.buildSageImagePropMap();
        buildLsmAttributeMap();

        log.info("Loading data into MongoDB");
        getSession().setFlushMode(FlushMode.MANUAL);
        
        long startAll = System.currentTimeMillis();

        log.info("Adding subjects");
        loadSubjects();

        log.info("Adding ontologies");
        loadOntologies(); // must come before buildAnnotationMap to preload the necessary maps

        log.info("Building disk-based Annotation map");
        buildAnnotationMap();

        log.info("Adding data sets");
        loadDataSets();

        log.info("Adding fly lines");
        loadFlyLines();

        log.info("Adding samples");
        // TODO: handle deleted (i.e. "hidden") neurons
        // TODO: handle pattern mask results in samples (knappj)
        loadSamples();

        log.info("Adding screen samples");
        loadScreenData();

        log.info("Adding compartment sets");
        loadCompartmentSets();

        log.info("Adding alignment boards");
        loadAlignmentBoards();

        log.info("Adding folders");
        loadWorkspaces();
        
        log.info("Verify annotations");
        verifyAnnotations();
        
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
            newSubject.setGroups(new HashSet<>(getGroupKeysForUsernameOrSubjectKey(subject.getKey())));
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
        Deque<Entity> dataSets = new LinkedList<>(getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET));
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

    private DataSet getDataSetObject(Entity dataSetEntity) throws Exception {
    	DataSet dataset = new DataSet();
        dataset.setId(dataSetEntity.getId());
        dataset.setName(dataSetEntity.getName());
        dataset.setOwnerKey(dataSetEntity.getOwnerKey());
        dataset.setReaders(getSubjectKeysWithPermission(dataSetEntity, "r"));
        dataset.setWriters(getSubjectKeysWithPermission(dataSetEntity, "w"));
        dataset.setCreationDate(dataSetEntity.getCreationDate());
        dataset.setUpdatedDate(dataSetEntity.getUpdatedDate());
        dataset.setIdentifier(dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER));
        dataset.setSageSync(dataSetEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC)!=null);

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
        	List<String> processNames = new ArrayList<>();
        	for(String processName : pipelineProcess.split(",")) {
        		if (!StringUtils.isEmpty(processName)) {
        			processNames.add(processName);
        		}
        	}
        	if (!processNames.isEmpty()) {
        		dataset.setPipelineProcesses(processNames);
        	}
        }

        // Look up data set folder and apply the same permissions to the data set object, since the folders will not be migrated

        List<Entity> folders = getEntitiesByNameAndTypeName(dataSetEntity.getOwnerKey(), dataSetEntity.getName(), EntityConstants.TYPE_FOLDER);
        if (folders.isEmpty()) {
            log.warn("Could not find data set folder for "+dataSetEntity.getName());
        }
        else {
            if (folders.size()>1) {
                log.warn("More than one data set folder with name "+dataSetEntity.getName());
            }

            Entity dataSetFolder = folders.get(0);
            dataset.setReaders(getSubjectKeysWithPermission(dataSetFolder, "r"));
            dataset.setWriters(getSubjectKeysWithPermission(dataSetFolder, "w"));
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
        Deque<Entity> samples = new LinkedList<>(getUserEntitiesByTypeName(subjectKey, EntityConstants.TYPE_SAMPLE));
        if (!samples.isEmpty()) {
            log.info("Got "+samples.size()+" samples for "+subjectKey);
            resetSession();
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
            //if (sampleEntity.getName().endsWith("-Retired")) continue;
            
            try {
                long start = System.currentTimeMillis();
                
                currAnnotations.clear();
                
                Sample sample = getSampleObject(sampleEntity);
                if (sample!=null) {
                    sampleCollection.insert(sample);
                    insertAnnotations(currAnnotations, sample);
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
        collectAnnotations(sampleEntity.getId());
        
        Sample sample = new Sample();
                
        sample.setId(sampleEntity.getId());
        sample.setName(sampleEntity.getName());
        sample.setOwnerKey(sampleEntity.getOwnerKey());
        sample.setReaders(getSubjectKeysWithPermission(sampleEntity, "r"));
        sample.setWriters(getSubjectKeysWithPermission(sampleEntity, "w"));
        sample.setCreationDate(sampleEntity.getCreationDate());
        sample.setUpdatedDate(sampleEntity.getUpdatedDate());
        sample.setAge(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_AGE));
        sample.setDataSet(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER));
        sample.setEffector(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_EFFECTOR));
        sample.setLine(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_LINE));
        sample.setSlideCode(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SLIDE_CODE));
        sample.setCompressionType(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMRESSION_TYPE));
        sample.setCompletionDate(ISO8601Utils.parse(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMPLETION_DATE)));
        sample.setTmogDate(ISO8601Utils.parse(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_TMOG_DATE)));

        String status = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS);
        sample.setStatus(status);
        sample.setSageSynced(!EntityConstants.VALUE_DESYNC.equals(status) && !EntityConstants.VALUE_RETIRED.equals(status));
        
        List<ObjectiveSample> objectiveSamples = new ArrayList<ObjectiveSample>();
        
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
                objectiveSamples.add(os);
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
                    objectiveSamples.add(os);
                }
                collectAnnotations(objSampleEntity.getId());
            }
        }
        
        if (objectiveSamples.isEmpty()) {
            log.warn("  Sample has no objectives: "+sampleEntity.getId());
        }
        else {
            sample.setObjectiveSamples(objectiveSamples);    
        }
        
        // Set derived consensus values
        if (!StringUtils.isEmpty(genderConsensus)) {
            sample.setGender(sanitizeGender(genderConsensus));
        }
        
        return sample;
    }

    private ObjectiveSample getObjectiveSampleObject(Entity parentSampleEntity, Entity sampleEntity, String sampleObjective) throws Exception {

        populateChildren(sampleEntity);
        
        ObjectiveSample sample = new ObjectiveSample(sampleObjective);
        sample.setChanSpec(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
        
        Entity supportingDataEntity = EntityUtils.getSupportingData(sampleEntity);
        if (supportingDataEntity==null) {
            log.warn("  Sample has no supporting data: "+sampleEntity.getId());
            return null;
        }

        List<LSMImage> allLsms = new ArrayList<>();
        List<SampleTile> tiles = new ArrayList<>();
        
        populateChildren(supportingDataEntity);
        collectAnnotations(supportingDataEntity.getId());

        for(Entity tileEntity : supportingDataEntity.getOrderedChildren()) {

            populateChildren(tileEntity);
            collectAnnotations(tileEntity.getId());
            
            Map<FileType,String> images = new HashMap<>();
            addImage(images,FileType.ReferenceMip,tileEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE));
            addImage(images,FileType.SignalMip,tileEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE));
            
            List<Reference> lsmReferences = new ArrayList<>();
            
            for(Entity lsmEntity : EntityUtils.getChildrenOfType(tileEntity, EntityConstants.TYPE_LSM_STACK)) {
                LSMImage lsmImage = getLSMImage(parentSampleEntity, lsmEntity);
                if (lsmImage!=null) {
                    allLsms.add(lsmImage);
                    lsmReferences.add(getReference(lsmEntity));
                }
                insertAnnotations(getAnnotations(lsmImage.getId()), lsmImage);
            }
            
            SampleTile tile = new SampleTile();
            tile.setName(tileEntity.getName());
            String area = tileEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
            if (!StringUtils.isEmpty(area)) {
                tile.setAnatomicalArea(area);
            }
            else {
                tile.setAnatomicalArea("");
            }

            tile.setLsmReferences(lsmReferences);
            tile.setFiles(images);
            tiles.add(tile);
            
        }
        
        sample.setTiles(tiles);
        
        List<SamplePipelineRun> runs = new ArrayList<>();
        for(Entity runEntity : EntityUtils.getChildrenOfType(sampleEntity, EntityConstants.TYPE_PIPELINE_RUN)) {
            populateChildren(runEntity);
            collectAnnotations(runEntity.getId());
            
            List<PipelineResult> results = new ArrayList<>();
            
            for(Entity resultEntity : EntityUtils.getChildrenForAttribute(runEntity, EntityConstants.ATTRIBUTE_RESULT)) {
                populateChildren(resultEntity);
                collectAnnotations(resultEntity.getId());

                if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_LSM_SUMMARY_RESULT)) {
                    LSMSummaryResult result = getLSMSummaryResult(allLsms, resultEntity);
                    results.add(result);
                }
                else if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {

                    List<PipelineResult> sprResults = new ArrayList<>();
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
                    if (resultSupportingData!=null) {
                        populateChildren(resultSupportingData);
                        collectAnnotations(resultSupportingData.getId());
                        
                        List<Entity> children = resultSupportingData.getOrderedChildren();
                        for(Entity resultFile : children) {
                            collectAnnotations(resultFile.getId());
                            
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
                error.setDescription(errorEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DESCRIPTION));
                error.setClassification(errorEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CLASSIFICATION));
                error.setCreationDate(errorEntity.getCreationDate());
                run.setError(error);
            }

            if (!results.isEmpty()) {
                run.setResults(results);
            }
            
            runs.add(run);
        }

        sample.setPipelineRuns(runs);

        // Save LSMs
        imageCollection.insert(allLsms.toArray());
        
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
        
        String fastPath = getFilepath(imageEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE));
        if (fastPath==null) {
            fastPath = getFilepath(imageEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_PERFORMANCE_PROXY_IMAGE));
        }
        addImage(files,FileType.FastStack,getRelativeFilename(result,fastPath));
        
        if (files.isEmpty()) {
            // This is a special case where no explicit MIPs have been defined, so we need to try to import the default 2d image.
            String defaultPath = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
            if (defaultPath==null) {
                // The file tree loader doesn't correctly populate the denormalized path above, so we need to get path from the actual image.
                defaultPath = getFilepath(imageEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE));
            }
            addImage(files,FileType.Unclassified2d,getRelativeFilename(result,defaultPath));
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

        // Populate LSMMetadata on the LSMs themselves 
        for(LSMImage lsm : lsms) {
            String name = ArchiveUtils.getDecompressedFilepath(lsm.getName());
            String key = FilenameUtils.getBaseName(name);
            FileGroup lsmSummaryResultGroup = result.getGroup(key);
            String metadataFilepath = DomainUtils.getFilepath(lsmSummaryResultGroup, FileType.LsmMetadata);
            addImage(lsm.getFiles(),FileType.LsmMetadata,getRelativeFilename(lsm,metadataFilepath));
        }
        
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
            result.setAnatomicalArea("");
        }
        
        Map<FileType,String> files = new HashMap<>();

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

        Entity supportingDataEntity = EntityUtils.getSupportingData(resultEntity);
        if (supportingDataEntity!=null) {
            collectAnnotations(supportingDataEntity.getId());
            
        	for(Entity child : supportingDataEntity.getChildren()) {
                collectAnnotations(child.getId());
                
        		String childName = child.getName();
        		String childFilepath = getFilepath(child);
                // TODO: this doesn't seem to work at all, do we need it, since we're getting LSM metadata from the LSM summary result?
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
            keys.add(tile.getName().replaceAll(" ", "_"));
            keys.add(tile.getAnatomicalArea().replaceAll(" ", "_"));
        }
        keys.add("stitched");
        result.setGroups(createFileGroups(result, resultEntity, keys));
        return result;
    }

    private List<FileGroup> createFileGroups(HasFilepath parent, Entity resultEntity, Set<String> keys) throws Exception {

    	Map<String,FileGroup> groups = new HashMap<>();
        populateChildren(resultEntity);
        Entity supportingDataEntity = EntityUtils.getSupportingData(resultEntity);
        if (supportingDataEntity!=null) {
            collectAnnotations(supportingDataEntity.getId());
            
            for(Entity child : supportingDataEntity.getChildren()) {
                collectAnnotations(child.getId());
                
                String childName = child.getName();
                String childFilepath = getFilepath(child);

                int d = childName.lastIndexOf('.');
                String name = childName.substring(0, d);
                String ext = childName.substring(d+1);
                
                FileType fileType = null;

                String key;
                if (childName.endsWith(".lsm.json")) {
                	key = FilenameUtils.getBaseName(name);
                	fileType = FileType.LsmMetadata;
                }
                else if (childName.endsWith(".lsm.metadata")) {
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
                    else if (key.contains("-"+trueKey)) {
                        translatedKey = trueKey;
                        break;
                    }
                }
                
                FileGroup group = groups.get(translatedKey);
                if (group==null) {
                	group = new FileGroup(translatedKey);
                	group.setFilepath(parent.getFilepath());
                	group.setFiles(new HashMap<FileType,String>());
                	groups.put(translatedKey, group);
                }
                
                group.getFiles().put(fileType, getRelativeFilename(group, childFilepath));
            }
        }
        
        return new ArrayList<>(groups.values());
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
            collectAnnotations(supportingDataEntity.getId());
        	Map<FileType,String> files = new HashMap<>();
        	
        	for(Entity child : supportingDataEntity.getChildren()) {
        		String childName = child.getName();
        		String childFilepath = getFilepath(child);
                collectAnnotations(child.getId());
        		
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
        
        Map<AlignmentScoreType,String> scores = new HashMap<>();
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
        String overlapScore = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_OVERLAP_COEFFICIENT);
        if (!StringUtils.isEmpty(overlapScore)) {
            scores.put(AlignmentScoreType.OverlapCoefficient,overlapScore);
        }
        String pearsonScore = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_OBJECT_PEARSON_COEFFICIENT);
        if (!StringUtils.isEmpty(pearsonScore)) {
            scores.put(AlignmentScoreType.ObjectPearsonCoefficient,pearsonScore);
        }
        String otsunaPearsonScore = imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_OTSUNA_OBJECT_PEARSON_COEFFICIENT);
        if (!StringUtils.isEmpty(otsunaPearsonScore)) {
            scores.put(AlignmentScoreType.OtsunaObjectPearsonCoefficient,otsunaPearsonScore);
        }
        if (!scores.isEmpty()) result.setScores(scores);
        
        Map<FileType,String> files = new HashMap<>();
        addStackFiles(imageEntity, files, result);
        if (movieEntity!=null) {
            addImage(files,FileType.AlignmentVerificationMovie,getRelativeFilename(result,getFilepath(movieEntity)));
        }
        if (!files.isEmpty()) result.setFiles(files);
        
        return result;
    }

    private LSMImage getLSMImage(Entity sampleEntity, Entity lsmEntity) throws Exception {

        LSMImage lsm = (LSMImage)getImage(lsmEntity);
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
        
        // Populate attributes that we can't get from SAGE
        lsm.setSageSynced(true);
        lsm.setChannelColors(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS));
        lsm.setChannelDyeNames(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES));
        lsm.setBrightnessCompensation(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_BRIGHTNESS_COMPENSATION));
        lsm.setCompletionDate(ISO8601Utils.parse(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMPLETION_DATE)));
        lsm.setTmogDate(ISO8601Utils.parse(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_TMOG_DATE)));
        
        // Populate SAGE attributes
        Map<String,Object> sageProps = (Map<String,Object>)largeOp.getValue(MongoLargeOperations.SAGE_IMAGEPROP_MAP, name);
        if (sageProps==null) {
            log.warn("    Cannot find LSM#"+lsm.getId()+" in SAGE with name: "+name);
        }
        else {
            
            // Use the current path from SAGE
            String path = (String)sageProps.get("image_query_path");
            if (path!=null) {
                lsm.setFilepath(path);
            }
            
            // Use JFS path if available
            String jfsPath = (String)sageProps.get("image_query_jfs_path");
            if (jfsPath!=null) {
                lsm.setFilepath(jfsPath);
            }
            
            addImage(files,FileType.LosslessStack,lsm.getFilepath());
            
            for(String key : lsmSageAttrs.keySet()) {
                try {
                    LsmSageAttribute attr = lsmSageAttrs.get(key);
                    Object value = sageProps.get(key);
                    String strValue = value==null?null:value.toString();
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
                            if (value instanceof Long) {
                                trueValue = (Long)value;
                            }
                            else {
                                if (!StringUtils.isEmpty(strValue)) {
                                    trueValue = new Long(strValue);
                                }
                            }
                        }
                        else if (fieldType.equals(Integer.class)) {
                            if (value instanceof Integer) {
                                trueValue = (Integer)value;
                            }
                            else {
                                if (!StringUtils.isEmpty(strValue)) {
                                    trueValue = new Integer(strValue);
                                }
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
                                if (!StringUtils.isEmpty(strValue)) {
                                    trueValue = new Boolean(strValue);
                                }
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

        if (lsm.getFilepath()==null) {
            log.warn("    LSM cannot be imported because it has no filepath: "+lsmEntity.getId());
            return null;
        }
        
        if (lsm.getVoxelSizeX()!=null) {
            String opticalRes = lsm.getVoxelSizeX()+"x"+lsm.getVoxelSizeY()+"x"+lsm.getVoxelSizeZ();
            lsm.setOpticalResolution(opticalRes);
        }
        
        if (lsm.getDimensionX()!=null) {
            String imageSize = lsm.getDimensionX()+"x"+lsm.getDimensionY()+"x"+lsm.getDimensionZ();
            lsm.setImageSize(imageSize);
        }
        
        if (lsm.getGender()!=null) {
            lsm.setGender(sanitizeGender(lsm.getGender()));
        }

        if (lsm.getAnatomicalArea()==null) {
            lsm.setAnatomicalArea("");
        }
        
        if (genderConsensus==null) {
            genderConsensus = lsm.getGender();
        }
        else if (!genderConsensus.equals(lsm.getGender())) {
            genderConsensus = NO_CONSENSUS_VALUE;
        }
        
        return lsm;
    }

    private Image getImage(Entity imageEntity) throws Exception {

    	Image image;
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

        Map<FileType,String> files = new HashMap<>();
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
        collectAnnotations(separationEntity.getId());
        
        List<NeuronFragment> neuronFragments = new ArrayList<>();
        Entity nfCollectionEntity = EntityUtils.getLatestChildOfType(separationEntity, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
        if (nfCollectionEntity==null) return null;
        
        populateChildren(nfCollectionEntity);
        collectAnnotations(nfCollectionEntity.getId());
        for(Entity fragmentEntity : EntityUtils.getChildrenOfType(nfCollectionEntity, EntityConstants.TYPE_NEURON_FRAGMENT)) {
            NeuronFragment neuronFragment = getNeuronFragment(sampleEntity, separationEntity, fragmentEntity);
            neuronFragments.add(neuronFragment);
            insertAnnotations(getAnnotations(neuronFragment.getId()), neuronFragment);
        }

        Entity cnCollectionEntity = EntityUtils.getLatestChildOfType(separationEntity, EntityConstants.TYPE_CURATED_NEURON_COLLECTION);
        if (cnCollectionEntity!=null) {
            collectAnnotations(cnCollectionEntity.getId());
            for(Entity fragmentEntity : EntityUtils.getChildrenOfType(cnCollectionEntity, EntityConstants.TYPE_CURATED_NEURON)) {
                CuratedNeuron neuronFragment = getCuratedNeuron(sampleEntity, separationEntity, fragmentEntity);
                neuronFragments.add(neuronFragment);
                insertAnnotations(getAnnotations(neuronFragment.getId()), neuronFragment);
            }
        }
        
        fragmentCollection.insert(neuronFragments.toArray());
        
        ReverseReference fragmentsReference = new ReverseReference();
        fragmentsReference.setCount(new Long(neuronFragments.size()));
        fragmentsReference.setReferringClassName(NeuronFragment.class.getSimpleName());
        fragmentsReference.setReferenceAttr("separationId");
        fragmentsReference.setReferenceId(separationEntity.getId());
        
        NeuronSeparation neuronSeparation = new NeuronSeparation();
        neuronSeparation.setId(separationEntity.getId());
        neuronSeparation.setName(separationEntity.getName());
        neuronSeparation.setCreationDate(separationEntity.getCreationDate());
        neuronSeparation.setFilepath(getFilepath(separationEntity));
        neuronSeparation.setFragmentsReference(fragmentsReference);
        
        Entity nsSupportingFiles = EntityUtils.getSupportingData(separationEntity);
        Entity resultFile = null;
        if (nsSupportingFiles!=null) { 
            populateChildren(nsSupportingFiles);
            collectAnnotations(nsSupportingFiles.getId());
            for(Entity child : nsSupportingFiles.getChildren()) {
                collectAnnotations(child.getId());
            }
            resultFile = EntityUtils.findChildWithNameAndType(nsSupportingFiles, "SeparationResult.nsp", EntityConstants.TYPE_IMAGE_3D);
            if (resultFile==null) {
                resultFile = EntityUtils.findChildWithNameAndType(nsSupportingFiles, "SeparationResultUnmapped.nsp", EntityConstants.TYPE_IMAGE_3D);
            }
        }

        Map<FileType,String> files = new HashMap<>();
        if (resultFile!=null) {
            addImage(files,FileType.NeuronSeparatorResult,getRelativeFilename(neuronSeparation, resultFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)));
        }
        neuronSeparation.setFiles(files);
        
        return neuronSeparation;
    }

    private NeuronFragment getNeuronFragment(Entity sampleEntity, Entity separationEntity, Entity fragmentEntity) throws Exception {
        NeuronFragment neuronFragment = new NeuronFragment();
        neuronFragment.setId(fragmentEntity.getId());
        if (sampleEntity!=null) {
            neuronFragment.setSample(getReference(sampleEntity));
        }
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
        if (separationEntity!=null) {
            neuronFragment.setSeparationId(separationEntity.getId());
            neuronFragment.setFilepath(getFilepath(separationEntity));
        }
        
        Map<FileType,String> images = new HashMap<>();
        addImage(images,FileType.SignalMip,getRelativeFilename(neuronFragment,fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE)));
        addImage(images,FileType.MaskFile,getRelativeFilename(neuronFragment,fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_MASK_IMAGE)));
        addImage(images,FileType.ChanFile,getRelativeFilename(neuronFragment,fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHAN_IMAGE)));
        neuronFragment.setFiles(images);
        
        return neuronFragment;
    }

    private CuratedNeuron getCuratedNeuron(Entity sampleEntity, Entity separationEntity, Entity fragmentEntity) throws Exception {
        CuratedNeuron curatedFragment = new CuratedNeuron();
        curatedFragment.setId(fragmentEntity.getId());
        if (sampleEntity!=null) {
            curatedFragment.setSample(getReference(sampleEntity));
        }
        curatedFragment.setName(fragmentEntity.getName());
        curatedFragment.setOwnerKey(fragmentEntity.getOwnerKey());
        curatedFragment.setReaders(getSubjectKeysWithPermission(fragmentEntity, "r"));
        curatedFragment.setWriters(getSubjectKeysWithPermission(fragmentEntity, "w"));
        curatedFragment.setCreationDate(fragmentEntity.getCreationDate());
        curatedFragment.setUpdatedDate(fragmentEntity.getUpdatedDate());
        String number = fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER);
        if (number!=null) {
            curatedFragment.setNumber(Integer.parseInt(number));
        }

        if (separationEntity!=null) {
            curatedFragment.setSeparationId(separationEntity.getId());
        }

        populateChildren(fragmentEntity);
        for(Entity childFragment : EntityUtils.getChildrenOfType(fragmentEntity, EntityConstants.TYPE_NEURON_FRAGMENT)) {
            curatedFragment.getComponentFragments().add(getReference(childFragment));
        }
        
        Map<FileType,String> images = new HashMap<>();
        
        String stackFilepath = fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        if (stackFilepath!=null) {
            File stackFile = new File(stackFilepath);
            curatedFragment.setFilepath(stackFile.getParent());
            addImage(images,stackFilepath.endsWith("h5j")?FileType.VisuallyLosslessStack:FileType.LosslessStack,getRelativeFilename(curatedFragment,stackFilepath));
        }
        else {
            String mipPath = fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
            if (mipPath!=null) {
                File mipFile = new File(mipPath);
                curatedFragment.setFilepath(mipFile.getParent());
            }
        }
        addImage(images,FileType.SignalMip,getRelativeFilename(curatedFragment,fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE)));
        curatedFragment.setFiles(images);
        
        return curatedFragment;
    }
    
	/* SCREEN SAMPLES */
    
    private void loadScreenData() throws DaoException {

        Deque<Entity> flyLines = new LinkedList<>(getEntitiesByTypeName(null, EntityConstants.TYPE_FLY_LINE));

        for(Iterator<Entity> i = flyLines.iterator(); i.hasNext(); ) {
            Entity flyLine = i.next();
            try {
                long start = System.currentTimeMillis();
                populateChildren(flyLine);
                Deque<Entity> screenSamples = new LinkedList<>(EntityUtils.getChildrenForAttribute(flyLine, EntityConstants.ATTRIBUTE_ENTITY));
                int loaded = loadScreenSamples(flyLine, screenSamples);
                // Free memory 
                i.remove();
                resetSession();
                log.info("  Loading "+loaded+" screen samples for "+flyLine+" took "+(System.currentTimeMillis()-start)+" ms");
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
            if (!EntityConstants.TYPE_SCREEN_SAMPLE.equals(screenSampleEntity.getEntityTypeName())) continue;
            
            try {
                long start = System.currentTimeMillis();
                
                currAnnotations.clear();
                
                Sample screenSample = getScreenSampleObject(flyLineEntity, screenSampleEntity);
                if (screenSample!=null) {
                    sampleCollection.insert(screenSample);
                    insertAnnotations(currAnnotations, screenSample);
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
    
    private Sample getScreenSampleObject(Entity flyLineEntity, Entity screenSampleEntity) throws Exception {
        
        if (screenSampleEntity.getEntityData()==null) {
            log.warn("  Cannot process screen sample with null entityData: "+screenSampleEntity.getId());
            return null;
        }
        
        populateChildren(screenSampleEntity);
        collectAnnotations(screenSampleEntity.getId());
        
        Sample screenSample = new Sample();
                
        screenSample.setId(screenSampleEntity.getId());
        screenSample.setName(screenSampleEntity.getName());
        screenSample.setOwnerKey(screenSampleEntity.getOwnerKey());
        screenSample.setReaders(getSubjectKeysWithPermission(screenSampleEntity, "r"));
        screenSample.setWriters(getSubjectKeysWithPermission(screenSampleEntity, "w"));
        screenSample.setCreationDate(screenSampleEntity.getCreationDate());
        screenSample.setUpdatedDate(screenSampleEntity.getUpdatedDate());
        screenSample.setLine(flyLineEntity.getName());
        screenSample.setStatus(EntityConstants.VALUE_COMPLETE);
        screenSample.setCompressionType(EntityConstants.VALUE_COMPRESSION_LOSSLESS);
        
        
        List<Reference> lsmReferences = new ArrayList<Reference>();
    
        Entity lsmEntity = new Entity();
        lsmEntity.setId(dao.getNewId());
        lsmEntity.setEntityTypeName(EntityConstants.TYPE_LSM_STACK);
        lsmEntity.setName(screenSample.getName()+".lsm.bz2");
        lsmEntity.setOwnerKey(screenSampleEntity.getOwnerKey());
        lsmEntity.setCreationDate(screenSampleEntity.getCreationDate());
        lsmEntity.setUpdatedDate(screenSampleEntity.getUpdatedDate());
        lsmEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_LINE, flyLineEntity.getName());
        lsmEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE, SCREEN_OBJECTIVE);
        
        // Mock up some fake entities so we can reuse the getLSMImage loading procedure
        Entity sampleEntity = new Entity();
        sampleEntity.setId(screenSampleEntity.getId());
        sampleEntity.setEntityTypeName(EntityConstants.TYPE_SAMPLE);
        
        // Put the LSM into Mongo
        LSMImage lsmImage = getLSMImage(sampleEntity, lsmEntity);
        
        if (lsmImage!=null) {
            if (lsmImage.getDataSet()==null) {
                log.info("    Setting default data set for screen LSM "+lsmImage.getId());
                lsmImage.setDataSet(SCREEN_DEFAULT_DATA_SET);
            }
            
            imageCollection.insert(lsmImage);
            
            // Reference it in the sample
            lsmReferences.add(getReference(lsmEntity));
            
            // Propagate properties to sample
            screenSample.setAge(lsmImage.getAge());
            screenSample.setEffector(lsmImage.getEffector());
            screenSample.setGender(lsmImage.getGender());
            screenSample.setDataSet(lsmImage.getDataSet());
            screenSample.setSageSynced(true);
        }
        else {
            log.info("    Setting default data set for screen sample "+screenSample.getId());
            screenSample.setDataSet(SCREEN_DEFAULT_DATA_SET);
            screenSample.setSageSynced(false);
        }
        
        // Even though the chacrm image family has VNC images, we can assume brain here because the screen data in the Workstation is just brains. 
        SampleTile tile = new SampleTile();
        tile.setName("brain");
        tile.setAnatomicalArea(SCREEN_ANATOMICAL_AREA);
        tile.setLsmReferences(lsmReferences);
        
        List<SampleTile> tiles = new ArrayList<SampleTile>();
        tiles.add(tile);
        
        SamplePipelineRun run = new SamplePipelineRun();
        run.setId(dao.getNewId());
        run.setName("Screen Sample Pipeline");
        run.setCreationDate(screenSampleEntity.getCreationDate());
        run.setPipelineProcess("FlyLightScreen");
        run.setPipelineVersion(1);

        List<SamplePipelineRun> runs = new ArrayList<>();
        runs.add(run);

        // Alignment result 
        
        Entity alignedStack = EntityUtils.findChildWithType(screenSampleEntity, EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
        if (alignedStack!=null) {
            SampleAlignmentResult alignmentResult = new SampleAlignmentResult();
            alignmentResult.setId(alignedStack.getId());
            alignmentResult.setName(SCREEN_ALIGNMENT_RESULT_NAME);
            alignmentResult.setCreationDate(alignedStack.getCreationDate());
            alignmentResult.setAlignmentSpace(SCREEN_ALIGNMENT_SPACE);
            alignmentResult.setChannelSpec(SCREEN_CHAN_SPEC);
            alignmentResult.setFilepath(new File(getFilepath(alignedStack)).getParent());
            alignmentResult.setObjective(SCREEN_OBJECTIVE);
            alignmentResult.setAnatomicalArea(SCREEN_ANATOMICAL_AREA);
            
            Map<AlignmentScoreType,String> scores = new HashMap<>();
            String qmScore = alignedStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_MODEL_VIOLATION_SCORE);
            if (!StringUtils.isEmpty(qmScore)) {
                scores.put(AlignmentScoreType.ModelViolation,qmScore);
            } 
            String incScore = alignedStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORE);
            if (!StringUtils.isEmpty(incScore)) {
                scores.put(AlignmentScoreType.Inconsistency,incScore);
                scores.put(AlignmentScoreType.Qi,ALIGNMENT_SCORE_FORMATTER.format(1-Double.parseDouble(incScore)));
            }
    
            if (!scores.isEmpty()) alignmentResult.setScores(scores);
            
            Map<FileType,String> images = new HashMap<>();
            if (alignedStack!=null) {
                addImage(images,FileType.LosslessStack,getRelativeFilename(alignmentResult,getFilepath(alignedStack)));
                addImage(images,FileType.AllMip,getRelativeFilename(alignmentResult,alignedStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH)));
            }
            alignmentResult.setFiles(images);
            run.addResult(alignmentResult);
        }
        
        // Pattern and Mask annotation results 
        
        Entity patternAnnotationEntity = EntityUtils.findChildWithNameAndType(screenSampleEntity, "Pattern Annotation", EntityConstants.TYPE_FOLDER);
        if (patternAnnotationEntity==null) {
            log.warn("Cannot process screen sample with no pattern annotation folder: "+screenSampleEntity.getId());
            return null;
        };
        
        addMasks(run, patternAnnotationEntity.getName(), patternAnnotationEntity);
        
        Entity maskAnnotationEntity = EntityUtils.findChildWithNameAndType(screenSampleEntity, "Mask Annotation", EntityConstants.TYPE_FOLDER);
        if (maskAnnotationEntity!=null) {
            populateChildren(maskAnnotationEntity);
            for(Entity maskUpdateEntity : EntityUtils.getChildrenOfType(maskAnnotationEntity, EntityConstants.TYPE_FOLDER)) {
                addMasks(run, maskAnnotationEntity.getName(), maskUpdateEntity);  
            }            
        }

        List<ObjectiveSample> objectiveSamples = new ArrayList<>();
        ObjectiveSample os = new ObjectiveSample(SCREEN_OBJECTIVE);
        os.setChanSpec(SCREEN_CHAN_SPEC);
        os.setTiles(tiles);
        os.setPipelineRuns(runs);
        objectiveSamples.add(os);
        screenSample.setObjectiveSamples(objectiveSamples); 
        
        for(Entity child : screenSampleEntity.getChildren()) {
            collectAnnotations(child.getId());
        }
        
        return screenSample;
    }
    
    private void addMasks(SamplePipelineRun run, String namePrefix, Entity patternMaskFolderEntity) throws Exception {
        
        String nameSuffix = patternMaskFolderEntity.getName().equals(namePrefix) ? "" : " - "+patternMaskFolderEntity.getName();
        
        SamplePatternAnnotationResult paResult = new SamplePatternAnnotationResult();
        paResult.setId(patternMaskFolderEntity.getId());
        paResult.setName(namePrefix+nameSuffix);
        paResult.setCreationDate(patternMaskFolderEntity.getCreationDate());
        addMasks(patternMaskFolderEntity, paResult);
        run.addResult(paResult);
        
        Entity normalizedEntity = EntityUtils.findChildWithNameAndType(patternMaskFolderEntity, "normalized", EntityConstants.TYPE_FOLDER);
        if (normalizedEntity!=null) {
            SamplePatternAnnotationNormalizedResult paNormResult = new SamplePatternAnnotationNormalizedResult();
            paNormResult.setId(normalizedEntity.getId());
            paNormResult.setName(namePrefix+nameSuffix+" - Normalized");
            paNormResult.setCreationDate(patternMaskFolderEntity.getCreationDate());
            addMasks(normalizedEntity, paNormResult);
            run.addResult(paNormResult);
            
        }
    }

    private void addMasks(Entity patternAnnotationEntity, SamplePatternAnnotationResult result) throws Exception {

        populateChildren(patternAnnotationEntity);
        
        // TODO: do something with these scores, since they're no longer present as annotations. Are they needed?
        Map<Long,Integer> intensityMap = new HashMap<>();
        Map<Long,Integer> distributionMap = new HashMap<>();
        for(Entity annotation : getAnnotationsForChildren(patternAnnotationEntity.getOwnerKey(), patternAnnotationEntity.getId())) {
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
        
        for(Entity maskEntity : EntityUtils.getChildrenOfType(patternAnnotationEntity, EntityConstants.TYPE_ALIGNED_BRAIN_STACK)) {

            String paFilepath = getFilepath(patternAnnotationEntity);

            String default2dImageFilepath = maskEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
            if (default2dImageFilepath==null) {
                default2dImageFilepath = maskEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
            }
            
            if (maskEntity.getName().equals("Heatmap")) {
                result.setFilepath(paFilepath);
                Map<FileType,String> images = new HashMap<>();
                addImage(images,FileType.LosslessStack,getRelativeFilename(result,getFilepath(maskEntity)));
                addImage(images,FileType.AllMip,getRelativeFilename(result,default2dImageFilepath));
                result.setFiles(images);
            }
            else {
                FileGroup group = new FileGroup(maskEntity.getName().replaceAll(" normalized", ""));
                group.setFilepath(paFilepath);
                Map<FileType,String> images = new HashMap<>();
                addImage(images,FileType.LosslessStack,getRelativeFilename(group,getFilepath(maskEntity)));
                addImage(images,FileType.AllMip,getRelativeFilename(group,default2dImageFilepath));
                group.setFiles(images);
                result.getGroups().add(group);
            }
        }
    }
    
    
    /* ONTOLOGIES */
    
    private void loadOntologies() throws DaoException {
        long start = System.currentTimeMillis();
        Deque<Entity> ontologyRoots = new LinkedList<>(getEntitiesByTypeName(null, EntityConstants.TYPE_ONTOLOGY_ROOT));
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

        List<OntologyTerm> terms = new ArrayList<>();
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
    
    private void buildAnnotationMap() {

        largeOp.clearCache(MongoLargeOperations.ETL_ANNOTATION_MAP);
        
        long start = System.currentTimeMillis();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getJdbcConnection();
            
            StringBuilder sql = new StringBuilder();
            sql.append("select a.id, a.name, a.owner_key, a.creation_date, a.updated_date, target.id, target.entity_type, ");
            sql.append("ked.value, ved.value, keed.child_entity_id, veed.child_entity_id, ised.value ");
            sql.append("from entity a ");
            sql.append("join entityData ted on ted.parent_entity_id=a.id and ted.entity_att=? ");
            sql.append("join entity target on ted.value=target.id "); 
            sql.append("left outer join entityData ked on ked.parent_entity_id=a.id and ked.entity_att=? ");
            sql.append("left outer join entityData ved on ved.parent_entity_id=a.id and ved.entity_att=? ");
            sql.append("left outer join entityData keed on keed.parent_entity_id=a.id and keed.entity_att=? ");
            sql.append("left outer join entityData veed on veed.parent_entity_id=a.id and veed.entity_att=? ");
            sql.append("left outer join entityData ised on ised.parent_entity_id=a.id and ised.entity_att=? ");
            sql.append("where a.entity_type=? ");
            sql.append("and a.owner_key not in ('user:jenetta','group:flylight') ");
            
            stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(Integer.MIN_VALUE);
            
            stmt.setString(1, EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
	        stmt.setString(2, EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM);
	        stmt.setString(3, EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM);
            stmt.setString(4, EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
            stmt.setString(5, EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID);
            stmt.setString(6, EntityConstants.ATTRIBUTE_ANNOTATION_IS_COMPUTATIONAL);
            stmt.setString(7, EntityConstants.TYPE_ANNOTATION);
            
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
                String isComp = rs.getString(12);
                
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
                annotation.setComputational(!StringUtils.isEmpty(isComp));

                Set<Annotation> annots = (Set<Annotation>)largeOp.getValue(MongoLargeOperations.ETL_ANNOTATION_MAP, targetId);
                if (annots == null) {
                    annots = new HashSet<>();
                }
                annots.add(annotation);
                largeOp.putValue(MongoLargeOperations.ETL_ANNOTATION_MAP, targetId, annots);
                
                i++;
            }
            
            log.info("  Creating "+i+" annotation map took "+(System.currentTimeMillis()-start)+" ms");
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
            Long targetId, String targetEntityType, Long keyId, Long valueId, String key, String value) {
        
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
        
        String className = getClassName(targetEntityType);
        if (className==null) { 
            className = TEMP_REF_CLASS;
        }
        Reference target = Reference.createFor(className, targetId);
        annotation.setTarget(target);
        
        return annotation;
    }
    
    private void verifyAnnotations() {
        Cache annotationCache = largeOp.getCache(MongoLargeOperations.ETL_ANNOTATION_MAP);
        List<Long> targetIds = new ArrayList<>();
        for(Object key : annotationCache.getKeys()) {
            Set<Annotation> annotations = (Set<Annotation>)largeOp.getValue(MongoLargeOperations.ETL_ANNOTATION_MAP, key);
            Map<Long,Annotation> map = DomainUtils.getMapById(annotations);
            
            List<Long> annotationIds = DomainUtils.getIds(annotations);
            for(Annotation annotation : annotationCollection.find("{_id:{$in:#}}", annotationIds).as(Annotation.class)) {
                map.remove(annotation.getId());
            }
            
            for(Annotation annotation : map.values()) {
                log.warn("Missing: "+annotation);
            }
            
            if (!map.isEmpty()) {
                targetIds.add((Long)key);
            }
        }
        
        if (targetIds.size()>=1000) {
            log.info("Targets missing annotations: ");
            try {
                for(Entity entity : getEntitiesInList(null, targetIds)) {
                    log.warn("Target: "+entity.getEntityTypeName()+" # "+entity.getId()+" : "+entity.getName());
                }
            }
            catch (Exception e) {
                log.error("Could not retrieve targets missing annotations",e);
            }
            targetIds.clear();
        }   
    }
    
    
    /* COMPARTMENT SETS */
    
    private void loadCompartmentSets() throws DaoException {
        long start = System.currentTimeMillis();
        Deque<Entity> compartmentSets = new LinkedList<>(getEntitiesByTypeName(null, EntityConstants.TYPE_COMPARTMENT_SET));
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
    	
    	List<Compartment> compartments = new ArrayList<>();
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

            Map<FileType,String> files = new HashMap<>();
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
            Map<FileType,String> relativeFiles = new HashMap<>();
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
        Deque<Entity> alignmentBoards = new LinkedList<>(getEntitiesByTypeName(null, EntityConstants.TYPE_ALIGNMENT_BOARD));
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

    	List<AlignmentBoardItem> items = new ArrayList<>();
    	
    	for(Entity alignmentBoardItemEntity : EntityUtils.getChildrenForAttribute(alignmentBoardItem, EntityConstants.ATTRIBUTE_ITEM)) {
    		Entity targetEntity = alignmentBoardItemEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_ENTITY);
    		if (targetEntity==null) {
    			log.info("    Target no longer exists for alignment board item: "+alignmentBoardItemEntity.getId());
    		}
    		else {
        		AlignmentBoardItem item = new AlignmentBoardItem();
        		item.setInclusionStatus(alignmentBoardItemEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_INCLUSION_STATUS));
        		item.setVisible("true".equalsIgnoreCase(alignmentBoardItemEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISIBILITY)));
        		item.setColor(alignmentBoardItemEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COLOR));
        		item.setRenderMethod(alignmentBoardItemEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_RENDER_METHOD));
        		// TODO: Fix this. It creates references to samples and neurons just fine, but compartments are not domain objects so the reference type is null. We need some way to reference compartments. 
        		if (!targetEntity.getEntityTypeName().equals(EntityConstants.TYPE_COMPARTMENT)) {
             		Reference target = getReference(targetEntity);
            		item.setTarget(target);
            		List<AlignmentBoardItem> children = getAlignmentBoardChildren(alignmentBoardItemEntity);
            		if (!children.isEmpty()) item.setChildren(children);	
            		items.add(item);
        		}
    		}
    	}
    	
    	return items;
    }

    
    /* TREE NODES (WORKSPACES, FOLDERS, VIEWS) */
    
    private Map<String,String> dataSetLookup = new HashMap<>();
    
    private void loadWorkspaces() throws DaoException {
        
        for(Entity dataSetEntity : getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET)) {
            String key = dataSetEntity.getOwnerKey()+"/"+dataSetEntity.getName();
            String identifier = dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
            dataSetLookup.put(key,identifier);
        }
        
        Map<Long,TreeNode> visited = new HashMap<>();
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
    
    private void loadWorkspace(String subjectKey, Map<Long,TreeNode> visited) throws Exception {

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
            LinkedList<Entity> rootFolders = new LinkedList<>(workspaceEntity.getOrderedChildren());
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
    
    private List<Reference> loadRootFolders(String subjectKey, Deque<Entity> rootFolders, Map<Long,TreeNode> loaded) {

        Set<Long> visitedSet = new HashSet<>();
        List<Reference> roots = new ArrayList<>();
        
        for(Iterator<Entity> i = rootFolders.iterator(); i.hasNext(); ) {
            Entity folderEntity = i.next();
                        
            if (!subjectKey.equals(folderEntity.getOwnerKey())) continue;
            
            Session session = null;
            try {
                long start = System.currentTimeMillis();
                
                session = openNewExternalSession();
                TreeNode treeNode = loadFolderHierarchy(folderEntity, visitedSet, loaded, "  ");
                if (treeNode!=null) {
                    Reference ref = getReference(treeNode);
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
    
    private TreeNode loadFolderHierarchy(Entity folderEntity, Set<Long> visitedSet, Map<Long,TreeNode> loaded, String indent) throws Exception {
        
        // Arnim's stuff is no longer needed.
    	if ("user:jenetta".equals(folderEntity.getOwnerKey())) {
    		return null;
    	}
    	
    	// Stuff from file tree loader is no longer needed.
    	if ("supportingFiles".equals(folderEntity.getName())) {
    		return null;
    	}

        // Dump all user search results, because it takes forever to import them all (due to translation), and they don't really jive with the new way of Filtering anyway.
        if ("Search Results".equals(folderEntity.getName()) && EntityUtils.isProtected(folderEntity)) {
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
	    
	    if ("Data Sets".equals(folderEntity.getName())) {
	    	// Create a canned filter for each data set
	    	for(DataSet dataSet : dao.getDataSets(folderEntity.getOwnerKey())) {
	    		// We only want the data sets owned by this user
	    		if (!dataSet.getOwnerKey().equals(folderEntity.getOwnerKey())) continue;
	    		Filter filter = getDataSetFilter(dataSet.getName(), dataSet.getOwnerKey(), dataSet.getIdentifier());
                children.add(getReference(filter));
                log.info(indent+"  Added canned data set filter: "+filter.getName());
			}
		}
	    else {
	        List<Entity> unmappedChildren = new ArrayList<>();
	        
		    // Load folder children
	        for(Entity childEntity : folderEntity.getOrderedChildren()) {
	            String childType = childEntity.getEntityTypeName();
		    	if (EntityConstants.TYPE_FOLDER.equals(childType)) {
                    String dataSetKey = childEntity.getOwnerKey()+"/"+childEntity.getName();
                    String dataSetIdentifier = dataSetLookup.get(dataSetKey);
                    if (dataSetIdentifier!=null) {
                        // This looks like a reference to a data set. Important: the new filter is owned by the parent owner
                        Filter filter = getDataSetFilter(childEntity.getName(), folderEntity.getOwnerKey(), dataSetIdentifier);
                        children.add(getReference(filter));
                        log.info(indent+"  Added canned data set filter: "+filter.getName()+" to data set owned by "+childEntity.getOwnerKey());
                    }
                    else {
		    			TreeNode childTreeNode = loadFolderHierarchy(childEntity, visitedSet, loaded, indent+"  ");
		    			if (childTreeNode!=null) {
			                children.add(getReference(childTreeNode));
		    			}
                    }
		    	}
		    	else {
		    	    unmappedChildren.add(childEntity);
		    	    String className = getClassName(childEntity.getEntityTypeName());
		            children.add(Reference.createFor(className==null?TEMP_REF_CLASS:className, childEntity.getId()));
		    	}
		    }

            Map<Long,Entity> mappedEntities = getMappedEntities(unmappedChildren, indent);
            for(Iterator<Reference> iterator = children.iterator(); iterator.hasNext(); ) {
                Reference ref = iterator.next();
                Entity mapped = mappedEntities.get(ref.getTargetId());
                if (mapped!=null) {
                    String className = getClassName(mapped.getEntityTypeName());
                    if (className!=null) {
                        ref.setTargetClassName(className);
                        ref.setTargetId(mapped.getId());
                    }
                    else {
                        log.error(indent+"  getMappedEntities returned illegal entity type: "+mapped.getEntityTypeName());
                        log.info(indent+"  Removing unrepresentable entity: "+ref.getTargetId());
                        iterator.remove();
                    }
                }
                else if (ref.getTargetClassName().equals(TEMP_REF_CLASS)) {
                    // No mapping, try the original class
                    log.info(indent+"  Removing unrepresentable entity: "+ref.getTargetId());
                    iterator.remove();
                }
            }
	    }
	    
        treeNode.setChildren(new ArrayList<>(children));
        treeNodeCollection.insert(treeNode);
        log.info(indent+"Created tree node: "+treeNode.getName());
        loaded.put(treeNode.getId(), treeNode);
        
        insertAnnotations(getAnnotations(treeNode.getId()), treeNode);
        
        return treeNode;
    }
    
    private Filter getDataSetFilter(String name, String ownerKey, String dataSetIdentifier) {
        Date now = new Date();
        Filter filter = new Filter();
        filter.setId(dao.getNewId());
        filter.setName(name);
        filter.setOwnerKey(ownerKey); // Important: the new filter is owned by the parent owner
        filter.setSearchClass(Sample.class.getName());
        filter.setCreationDate(now);
        filter.setUpdatedDate(now);
        FacetCriteria dataSetCriteria = new FacetCriteria();
        dataSetCriteria.setAttributeName("dataSet");
        dataSetCriteria.setValues(Sets.newHashSet(dataSetIdentifier));
        filter.addCriteria(dataSetCriteria);
        filterCollection.insert(filter);
        return filter;
    }
    
    private Map<Long,Entity> getMappedEntities(Collection<Entity> items, String indent) throws Exception {

        ArrayListMultimap<String, Entity> childrenByType = ArrayListMultimap.<String, Entity>create();
        for(Entity childEntity : items) {
            childrenByType.put(childEntity.getEntityTypeName(), childEntity);
        }
        
        Map<Long,Entity> mapping = new HashMap<>();
        for(String childType : childrenByType.keySet()) {
            mapping.putAll(translateEntities(childType, childrenByType.get(childType), indent));
        }
     
        return mapping;
    }
    
    private Map<Long,Entity> translateEntities(String setEntityType, List<Entity> entityMembers, String indent) throws DaoException {
        
	    // --------------------------------------------------------------------------------
	    // Preprocess all objects and see if we need to do a bulk mapping
		Map<Long,Entity> translatedEntities = new HashMap<>();
		
		if (getCollectionName(setEntityType)==null) {
			// We don't know this type, attempt mass translation up to sample
			translatedEntities.putAll(translateToSample(indent, entityMembers, setEntityType));
			if (!translatedEntities.isEmpty()) {
			    if (log.isTraceEnabled()) {
    				for(Long id : translatedEntities.keySet()) {
    					Entity translatedEntity = translatedEntities.get(id);
    					if (translatedEntity==null) {
    						log.trace(indent+"  "+id+" --> null");
    					}
    					else {
    						log.trace(indent+"  "+id+" --> "+translatedEntity.getId()+" ("+translatedEntity.getEntityTypeName()+")");
    					}
    				}
			    }
			}
		}

		log.debug(indent+"  setType="+setEntityType+", translatedEntities.size="+translatedEntities.size());
		
		// Translate sub-samples into samples
		if (EntityConstants.TYPE_SAMPLE.equals(setEntityType)) {
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
				List<String> upMapping = new ArrayList<>();
				upMapping.add("Sample");
				List<String> downMapping = new ArrayList<>();
				List<MappedId> mappings = getProjectedResults(null, subsampleIds, upMapping, downMapping);
				Map<Long,Entity> mappedEntities = getMappedEntities(mappings);
	            for(MappedId mappedId : mappings) {
	            	Long subsampleId = mappedId.getOriginalId();
	            	Long originalId = reverseMap.get(subsampleId);
	            	secondaryTranslatedEntities.put(originalId,  mappedEntities.get(mappedId.getMappedId()));
	            }
            	log.info(indent+"  Translated "+subsampleIds.size()+ " sub samples to "+secondaryTranslatedEntities.size()+" parent samples");
            	if (!secondaryTranslatedEntities.isEmpty()) {
            	    if (log.isTraceEnabled()) {
        				for(Long id : secondaryTranslatedEntities.keySet()) {
        					Entity translatedEntity = secondaryTranslatedEntities.get(id);
        					if (translatedEntity==null) {
        						log.trace(indent+"  "+id+" --> null");
        					}
        					else {
        						log.trace(indent+"  "+id+" --> "+translatedEntity.getId()+" ("+translatedEntity.getEntityTypeName()+")");
        					}
        				}
            	    }
    			}
	            translatedEntities.putAll(secondaryTranslatedEntities);
			}
		}
		
		log.debug(indent+"  translatedEntities.size="+translatedEntities.size());
		
	    // --------------------------------------------------------------------------------
		// Make sure each entity can be loaded
	    for(Entity childEntity : entityMembers) {

	        Entity importEntity = childEntity;
        	Entity translatedEntity = translatedEntities.get(childEntity.getId());
        	
        	if (translatedEntity!=null) {
        		// already translated this above
        		log.info(indent+"  Will reference "+translatedEntity.getEntityTypeName()+"#"+translatedEntity.getId()+" instead of "+childEntity.getEntityTypeName()+"#"+childEntity.getId());
        		importEntity = translatedEntity;
        	}
        	else if (TRANSLATE_ENTITIES) {
                String importCollection = getCollectionName(childEntity.getEntityTypeName());
        	    if ((importCollection==null || "image".equals(importCollection)) && !EntityConstants.TYPE_LSM_STACK.equals(childEntity.getEntityTypeName())) {
            		// If we don't know how to import this entity, see if we can substitute a higher-level entity for the one that the user referenced. 
        	        // For example, if they referenced a sample processing result, we find the parent sample. Same goes for neuron separations, etc.
                    log.debug(indent+"  Finding higher-level ancestors for unknown entity "+childEntity.getEntityTypeName()+"#"+childEntity.getId());
        	        Entity ancestor = getHigherLevelAncestor(childEntity, new HashSet<Long>());
        	        if (ancestor!=null) {
                        log.info(indent+"  Will reference "+ancestor.getEntityTypeName()+"#"+ancestor.getId()+" instead of unknown "+childEntity.getEntityTypeName()+"#"+childEntity.getId());
                        importEntity = ancestor;
                        translatedEntities.put(childEntity.getId(), ancestor);
                    }
        	    }
        	}

            String collectionName = getCollectionName(importEntity.getEntityTypeName());
            
            if (INSERT_ROGUE_ENTITIES) {
                // A minor optimization, since we can only do rogue imports on images and neuron fragments
                if ("image".equals(collectionName) || "fragment".equals(collectionName)) {
		            // Attempt imports of rogue entities which map to domain objects, but which have not been loaded by any other part of the import procedure
		            if (dao.getCollectionByName(collectionName).count("{_id:#}",importEntity.getId())<1) {
		            	attemptRogueImport(importEntity, indent);
		            }
                }
            }
            log.trace(indent+"  Adding "+importEntity.getEntityTypeName()+"#"+importEntity.getId()+" ("+importEntity.getName()+")");
	    }
		
		return translatedEntities;
	}

    private Entity getHigherLevelAncestor(Entity entity, Set<Long> visited) throws DaoException {

        if (visited.contains(entity.getId())) {
            // We've already been here, don't need to search it again
            return null;
        }
        visited.add(entity.getId());
        
        // Do not return the starting node as the ancestor, even if type matches
        for (String entityType : entityTranslationPriority) {
            if (entity.getEntityTypeName().equals(entityType)) {
                return entity;
            }
        }
        
        for(Entity parent : getParentEntities(entity.getId())) {
            Entity ancestor = getHigherLevelAncestor(parent, visited);
            if (ancestor != null) return ancestor;
        }
        
        return null;
    }
    
    private Map<Long,Entity> translateToSample(String indent, List<Entity> entityMembers, String startingType) throws DaoException  {

		Map<Long,Entity> translatedEntities = new HashMap<>();
	    List<Long> originalIds = new ArrayList<>();
	    for(Entity childEntity : entityMembers) {
    		originalIds.add(childEntity.getId());
	    }
	    
		List<String> upMapping = new ArrayList<>();
		List<String> downMapping = new ArrayList<>();
		
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
                    List<String> upMapping = new ArrayList<>();
                    upMapping.add("Sample");
                    List<String> downMapping = new ArrayList<>();
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
                    imageCollection.insert(image);
                    insertAnnotations(getAnnotations(image.getId()), image);
            	}
            }
            else if (EntityConstants.TYPE_IMAGE_3D.equals(entityType)) {
            	Image image = getImage(entity);
            	imageCollection.insert(image);
            	insertAnnotations(getAnnotations(image.getId()), image);
            }
            else if (EntityConstants.TYPE_IMAGE_2D.equals(entityType)) {
            	Image image = getImage(entity);
            	imageCollection.insert(image);
            	insertAnnotations(getAnnotations(image.getId()), image);
            }
            else if (EntityConstants.TYPE_NEURON_FRAGMENT.equals(entityType)) {
                NeuronFragment fragment = getNeuronFragment(null, null, entity);
                fragmentCollection.insert(fragment);
                insertAnnotations(getAnnotations(fragment.getId()), fragment);
            }
            else if (EntityConstants.TYPE_CURATED_NEURON.equals(entityType)) {
                NeuronFragment fragment = getCuratedNeuron(null, null, entity);
                fragmentCollection.insert(fragment);
                insertAnnotations(getAnnotations(fragment.getId()), fragment);
            }
            else {
            	log.warn(indent+"  Cannot handle rogue entity type: "+entityType+"#"+entity.getId());
            }
        }
    	catch (Exception e) {
    		log.error(indent+"  Rogue import failed for "+entity.getId(),e);
    	}
	}

	private Map<Long, Entity> getMappedEntities(List<MappedId> mappings) throws DaoException {
		List<Long> entityIds = new ArrayList<>();
		for(MappedId mappedId : mappings) {
			entityIds.add(mappedId.getMappedId());
		}
		return EntityUtils.getEntityMap(getEntitiesInList(null, entityIds));
	}
    
    /* UTILITY METHODS */

    private Reference getReference(Entity entity) {
        String className = getClassName(entity.getEntityTypeName());
        if (className==null) {
            log.warn("Cannot create reference to "+entity.getEntityTypeName());
            return null;
        }
        return Reference.createFor(className, entity.getId());
	}

    private Reference getReference(DomainObject domainObject) {
    	return Reference.createFor(domainObject);
    }
    
    private Set<String> getDefaultSubjectKeys(String subjectKey) {
        Set<String> subjectKeys = new HashSet<>();
        subjectKeys.add(subjectKey); // owner has all permissions
        return subjectKeys;
    }
    
    private Set<String> getSubjectKeysWithPermission(Entity entity, String rights) {
        Set<String> subjectKeys = new HashSet<>();
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
            return Sample.class;
        }
        else if (EntityConstants.TYPE_ALIGNED_BRAIN_STACK.equals(entityType)) {
            return Sample.class;
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
        return clazz==null?null:clazz.getSimpleName();
    }
    
    private String getCollectionName(String entityType) {
        return DomainUtils.getCollectionName(getClass(entityType));
    }

    private Set<Annotation> getAnnotations(Long id) {
        return (Set<Annotation>)largeOp.getValue(MongoLargeOperations.ETL_ANNOTATION_MAP, id);
    }
    
    private void collectAnnotations(Long id) {
        Set<Annotation> annotations = getAnnotations(id);
        if (annotations!=null) {
            for(Annotation annotation : annotations) {
                boolean exists = false;
                for (Annotation currAnnotation : currAnnotations) {
                    if (currAnnotation.getName().equals(annotation.getName()) && currAnnotation.getOwnerKey().equals(annotation.getOwnerKey())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    currAnnotations.add(annotation);
                }
            }
        }
    }

    private void insertAnnotations(Set<Annotation> annotations, DomainObject domainObject) {
        insertAnnotations(annotations, domainObject.getClass(), domainObject.getId());
    }
    
    private void insertAnnotations(Set<Annotation> annotations, Class<?> clazz, Long id) {
        if (annotations==null || annotations.isEmpty()) return;
        Reference ref = Reference.createFor(clazz, id);
        for(Annotation annotation : annotations) {
            annotation.setTarget(ref);
        }
        log.debug("  Adding "+annotations.size()+" annotations for "+ref);
        annotationCollection.insert(annotations.toArray());
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
        if (parentFilepath==null) return filepath;
        String prefix = parentFilepath.endsWith("/") ? parentFilepath : parentFilepath+"/";
        if (!filepath.startsWith(prefix)) {
            return filepath;
        }
        return filepath.replaceFirst(prefix, "");
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
