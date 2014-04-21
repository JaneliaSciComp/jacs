package org.janelia.it.jacs.compute.access.mongodb;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SubjectDAO;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.Folder;
import org.janelia.it.jacs.model.domain.HasFilepath;
import org.janelia.it.jacs.model.domain.LSMImage;
import org.janelia.it.jacs.model.domain.NeuronFragment;
import org.janelia.it.jacs.model.domain.NeuronSeparation;
import org.janelia.it.jacs.model.domain.ObjectiveSample;
import org.janelia.it.jacs.model.domain.PipelineResult;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.SampleTile;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.marshall.jackson.JacksonMapper;

import com.fasterxml.jackson.databind.MapperFeature;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

/**
 * Data access to the MongoDB data store.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDbDomainDAO extends AnnotationDAO {
	
	protected static final String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
	protected static final String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");

    private static final int SAMPLES_PER_DATA_SET = 4;
    
    protected Jongo jongo;
	protected MongoCollection  folderCollection;
	protected MongoCollection  sampleCollection;
    protected MongoCollection  lsmCollection;
    protected MongoCollection  neuronFragmentCollection;
	
    public MongoDbDomainDAO(Logger log) {
    	super(log);
    }
    
    private void init() throws DaoException {
        try {
            MongoClient m = new MongoClient(MONGO_SERVER_URL);
        	m.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
        	DB db = m.getDB(MONGO_DATABASE);
        	jongo = new Jongo(db, 
        	        new JacksonMapper.Builder()
        	            .enable(MapperFeature.AUTO_DETECT_GETTERS)
                        .enable(MapperFeature.AUTO_DETECT_SETTERS)
        	            .build());
        	folderCollection = jongo.getCollection("folder").withWriteConcern(WriteConcern.UNACKNOWLEDGED);
        	sampleCollection = jongo.getCollection("sample").withWriteConcern(WriteConcern.UNACKNOWLEDGED);
            lsmCollection = jongo.getCollection("lsm").withWriteConcern(WriteConcern.UNACKNOWLEDGED);
        	neuronFragmentCollection = jongo.getCollection("neuronFragment").withWriteConcern(WriteConcern.UNACKNOWLEDGED);
        }
		catch (UnknownHostException e) {
			throw new RuntimeException("Unknown host given in MongoDB.ServerURL value in system properties: "+MONGO_SERVER_URL);
		}
    }
    
    public void loadAllEntities() throws DaoException {

        long startAll = System.currentTimeMillis(); 
        
        String subjectKey = null;//"user:asoy";

    	log.info("Adding samples");
    
    	// Try some samples from each data set
//    	for(Entity dataSet : getEntitiesByTypeName(subjectKey, EntityConstants.TYPE_DATA_SET)) {
//    	    String dataSetId = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
//    	    List<Entity> samples = getUserEntitiesWithAttributeValue(dataSet.getOwnerKey(), EntityConstants.TYPE_SAMPLE, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetId);
//    	    List<Entity> shuffled = new ArrayList<Entity>(samples);
//            Collections.shuffle(shuffled);
//            int loaded = 0;
//            int i = 0;
//            while (loaded<SAMPLES_PER_DATA_SET) {
//                int last = i+10;
//                if (last>shuffled.size()-1) last=shuffled.size()-1;
//                if (last<=i) break;
//                loaded += loadSamples(shuffled.subList(i, last));
//                i = last;
//            }
//    	}
    	
    	SubjectDAO subjectDao = new SubjectDAO(log);
    	for(Subject subject : subjectDao.getSubjects()) {
    	    long start = System.currentTimeMillis();
    	    Deque<Entity> samples = new LinkedList<Entity>(getUserEntitiesByTypeName(subject.getKey(), EntityConstants.TYPE_SAMPLE));
    	    log.info("Loading "+samples.size()+" samples for "+subject.getKey());
    	    loadSamples(samples);
            log.info("Loading "+samples.size()+" samples for "+subject.getKey()+" took "+(System.currentTimeMillis()-start)+" ms");
    	}
    	
        log.info("Adding folders");
        List<Folder> folders = getRootFolders(subjectKey);
        for(Folder folder : folders) {
            long start = System.currentTimeMillis();
            folderCollection.insert(folder);
            log.info("  Adding "+folder.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
        }
        
        log.info("Creating indexes");

        ensureDomainIndexes(sampleCollection);
        sampleCollection.ensureIndex("{name:1}");
        sampleCollection.ensureIndex("{dataSet:1}");
        sampleCollection.ensureIndex("{line:1}");

        ensureDomainIndexes(folderCollection);
        folderCollection.ensureIndex("{name:1}");

        ensureDomainIndexes(neuronFragmentCollection);
        neuronFragmentCollection.ensureIndex("{sampleId:1}");
        
        ensureDomainIndexes(lsmCollection);
        lsmCollection.ensureIndex("{sampleId:1}");
        lsmCollection.ensureIndex("{sageId:1}");
        lsmCollection.ensureIndex("{slideCode:1}");
        lsmCollection.ensureIndex("{filepath:1}");
        
        log.info("Loading MongoDB took "+(System.currentTimeMillis()-startAll)+" ms");
    }
    
    private void ensureDomainIndexes(MongoCollection mc) {
        mc.ensureIndex("{ownerKey:1}");
        mc.ensureIndex("{readers:1}");
        mc.ensureIndex("{writers:1}");
    }
    
    private int loadSamples(Deque<Entity> samples) {

        int c = 0;
        for(Iterator<Entity> i = samples.iterator(); i.hasNext(); ) {
            Entity sampleEntity = i.next();
            i.remove(); // Free memory by releasing the reference to this entire sample tree
             
            try {
                // Skip these samples
                if (sampleEntity.getName().contains("~")) continue;
                if (EntityConstants.VALUE_ERROR.equals(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS))) {
                    continue;
                }
                
                long start = System.currentTimeMillis();
                Sample sample = getSampleObject(sampleEntity);
                if (sample!=null) {
                    log.info("  Retrieving "+sampleEntity.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
                    long start2 = System.currentTimeMillis();
                    sampleCollection.insert(sample);
                    log.info("  Inserting "+sample.getName()+" took "+(System.currentTimeMillis()-start2)+" ms");
                    c++;
                }
                else {
                    log.info("  Retrieving "+sampleEntity.getName()+" failed, and took "+(System.currentTimeMillis()-start)+" ms");
                }
            }
            catch (Throwable e) {
                log.error("Error loading sample "+sampleEntity.getId(),e);
            }
        }
        
        return c;
    }
    
    private List<Folder> getRootFolders(String subjectKey) throws DaoException {

        List<Folder> folders = new ArrayList<Folder>();
        for(Entity folder : getEntitiesWithTag(subjectKey, EntityConstants.ATTRIBUTE_COMMON_ROOT)) {
            folders.addAll(getFolders(folder));
        }

        return folders;
    }
    
    private List<Folder> getFolders(Entity entity) {
        
        List<Folder> folders = new ArrayList<Folder>();
        
        Folder folder = new Folder();
        folder.setId(entity.getId());
        folder.setOwnerKey(entity.getOwnerKey());
        folder.setName(entity.getName());
        folder.setCreationDate(entity.getCreationDate());
        folder.setReaders(getSubjectKeysWithPermission(entity, "r"));
        folder.setWriters(getSubjectKeysWithPermission(entity, "w"));

        List<Reference> references = new ArrayList<Reference>();
        
        for(Entity childEntity : entity.getOrderedChildren()) {
            if (childEntity.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
                folders.addAll(getFolders(childEntity));
            }
            String type = getType(childEntity);
            if (type!=null) {
                Reference ref = new Reference();
                ref.setId(childEntity.getId());
                ref.setType(childEntity.getEntityTypeName());
                references.add(ref);
            }
        }
        
        if (!references.isEmpty()) {
            folder.setReferences(references);
        }
        
        folders.add(folder);
        
        return folders;
    }
    
    private String getType(Entity entity) {
        String entityType = entity.getEntityTypeName();
        if (EntityConstants.TYPE_SAMPLE.equals(entityType)) {
            return "sample";
        }
        else if (EntityConstants.TYPE_NEURON_FRAGMENT.equals(entityType)) {
            return "neuronFragment";
        }
        else if (EntityConstants.TYPE_FOLDER.equals(entityType)) {
            return "folder";
        }
        return null;
    }
    
    private Sample getSampleObject(Entity sampleEntity) {
        
        if (sampleEntity.getName().contains("~")) {
            log.warn("  Cannot process sub-sample as sample: "+sampleEntity.getId());
            return null;
        }
        
        if (sampleEntity.getEntityData()==null) {
            log.warn("  Cannot process sample with null entityData: "+sampleEntity.getId());
            return null;
        }
        
        Sample sample = new Sample();
                
        sample.setId(sampleEntity.getId());
        sample.setOwnerKey(sampleEntity.getOwnerKey());
        sample.setReaders(getSubjectKeysWithPermission(sampleEntity, "r"));
        sample.setWriters(getSubjectKeysWithPermission(sampleEntity, "w"));
        sample.setCreationDate(sampleEntity.getCreationDate());
        sample.setUpdatedDate(sampleEntity.getUpdatedDate());
        sample.setName(sampleEntity.getName());
        sample.setAge(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_AGE));
        sample.setChanSpec(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
        sample.setDataSet(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER));
        sample.setEffector(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_EFFECTOR));
        sample.setLine(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_LINE));
        sample.setSlideCode(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SLIDE_CODE));
        sample.setStatus(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS));
        sample.setVisited(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISITED));
        
        Map<String, ObjectiveSample> objectiveSamples = new HashMap<String, ObjectiveSample>();
        
        List<Entity> objSampleEntities = EntityUtils.getChildrenOfType(sampleEntity, EntityConstants.TYPE_SAMPLE);
        if (objSampleEntities.isEmpty()) {
            String objective = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
            if (objective==null) {
                objective = "63x";
            }
            ObjectiveSample os = getObjectiveSampleObject(sampleEntity, sampleEntity);
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
                ObjectiveSample os = getObjectiveSampleObject(sampleEntity, objSampleEntity);
                if (os!=null) {
                    objectiveSamples.put(objective, os);
                }
            }
        }
        
        if (objectiveSamples.isEmpty()) {
            log.warn("  Sample has no objectives: "+sampleEntity.getId());
            return null;
        }
        
        sample.setObjectives(objectiveSamples);
        return sample;
    }

    private ObjectiveSample getObjectiveSampleObject(Entity parentSampleEntity, Entity sampleEntity) {
        
        ObjectiveSample sample = new ObjectiveSample();
        
        Entity supportingDataEntity = EntityUtils.getSupportingData(sampleEntity);
        if (supportingDataEntity==null) {
            log.warn("  Sample has no supporting data: "+sampleEntity.getId());
            return null;
        }
        
        List<SampleTile> tiles = new ArrayList<SampleTile>();
        for(Entity tileEntity : supportingDataEntity.getOrderedChildren()) {
            
            List<Long> lsmIds = new ArrayList<Long>();
            for(Entity lsmEntity : EntityUtils.getChildrenOfType(tileEntity, EntityConstants.TYPE_LSM_STACK)) {
                LSMImage lsmImage = getLSMImage(parentSampleEntity, lsmEntity);
                lsmCollection.insert(lsmImage);
                lsmIds.add(lsmImage.getId());
            }
            
            SampleTile tile = new SampleTile();
            tile.setName(tileEntity.getName());
            tile.setAnatomicalArea(tileEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA));
            tile.setLsmIds(lsmIds);
            tiles.add(tile);
        }
        
        sample.setTiles(tiles);

        List<SamplePipelineRun> runs = new ArrayList<SamplePipelineRun>();
        for(Entity runEntity : EntityUtils.getChildrenOfType(sampleEntity, EntityConstants.TYPE_PIPELINE_RUN)) {

            List<PipelineResult> results = new ArrayList<PipelineResult>();
            for(Entity resultEntity : EntityUtils.getChildrenForAttribute(runEntity, EntityConstants.ATTRIBUTE_RESULT)) {
                Entity resultSupportingData = EntityUtils.getSupportingData(resultEntity);

                if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {

                    List<PipelineResult> sprResults = new ArrayList<PipelineResult>();
                    for(Entity separationEntity : EntityUtils.getChildrenOfType(resultEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                        sprResults.add(getNeuronSeparation(parentSampleEntity, separationEntity));
                    }
                    
                    SampleProcessingResult result = getSampleProcessingResult(resultEntity);
                    
                    if (!sprResults.isEmpty()) {
                        result.setResults(sprResults);
                    }
                    
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
                        if (resultImages.size()==1 || "63".equals(objective) || "".equals(objective)) {
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
                        alignmentResult.setName(imageEntity.getName());
                        alignmentResult.setFilepath(resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                        
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
            if (!results.isEmpty()) {
                run.setResults(results);
            }
            runs.add(run);
        }

        sample.setPipelineRuns(runs);
        
        return sample;
    }
    
    private SampleProcessingResult getSampleProcessingResult(Entity resultEntity) {
        SampleProcessingResult result = new SampleProcessingResult();
        result.setCreationDate(resultEntity.getCreationDate());
        result.setFilepath(resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        String area = resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
        if (!StringUtils.isEmpty(area)) {
            result.setAnatomicalArea(area);
        }
        Entity stackEntity = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        if (stackEntity!=null) {
            result.setStackFilepath(getRelativeFilename(result,stackEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)));
            result.setImageSize(stackEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION));
            result.setOpticalResolution(stackEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION));
            result.setReferenceMipFilepath(getRelativeFilename(result,stackEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE)));
            result.setSignalMipFilepath(getRelativeFilename(result,stackEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE)));
        }
        else {
            log.warn("  Sample processing result has no stack: "+resultEntity.getId());
        }
        return result;
    }
    
    private SampleAlignmentResult getAlignmentResult(Entity alignmentEntity, Entity imageEntity, Entity movieEntity) {
        SampleAlignmentResult result = new SampleAlignmentResult();
        result.setCreationDate(imageEntity.getCreationDate());
        result.setAlignmentSpace(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE));
        result.setBoundingBox(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_BOUNDING_BOX));
        result.setChannelColors(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS));
        result.setChanSpec(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
        result.setFilepath(alignmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        result.setStackFilepath(getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)));
        result.setImageSize(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION));
        if (movieEntity!=null) {
            result.setVerifyMovieFilepath(movieEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        }
        result.setObjective(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE));
        result.setOpticalResolution(imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION));
        result.setReferenceMipFilepath(getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE)));
        result.setSignalMipFilepath(getRelativeFilename(result,imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE)));
        return result;
    }

    private LSMImage getLSMImage(Entity sampleEntity, Entity lsmEntity) {

        LSMImage lsm = new LSMImage();
        lsm.setId(lsmEntity.getId());
        lsm.setSampleId(sampleEntity.getId());
        lsm.setOwnerKey(lsmEntity.getOwnerKey());
        lsm.setReaders(getSubjectKeysWithPermission(lsmEntity, "r"));
        lsm.setWriters(getSubjectKeysWithPermission(lsmEntity, "w"));
        lsm.setAge(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_AGE));
        lsm.setAnatomicalArea(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA));
        lsm.setChannelColors(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS));
        lsm.setChannelDyeNames(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES));
        lsm.setChanSpec(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
        lsm.setEffector(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_EFFECTOR));
        lsm.setFilename(lsmEntity.getName());
        lsm.setLsmFilepath(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        lsm.setGender(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_GENDER));
        lsm.setLine(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_LINE));
        lsm.setSignalMipFilepath(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE));
        lsm.setReferenceMipFilepath(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE));
        lsm.setMountingProtocol(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_MOUNTING_PROTOCOL));
        String numChannels = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS);
        if (numChannels!=null) {
            lsm.setNumChannels(Integer.parseInt(numChannels));
        }
        lsm.setObjective(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE));
        lsm.setOpticalResolution(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION));
        lsm.setPixelResolution(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION));
        String sageId = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_ID);
        if (sageId!=null) {
            lsm.setSageId(Integer.parseInt(sageId));
        }
        lsm.setSlideCode(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SLIDE_CODE));
        return lsm;
    }
    
    private NeuronSeparation getNeuronSeparation(Entity sampleEntity, Entity separationEntity) {

        List<Long> neuronFragmentIds = new ArrayList<Long>();
        Entity nfCollectionEntity = EntityUtils.getLatestChildOfType(separationEntity, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
        for(Entity fragmentEntity : EntityUtils.getChildrenOfType(nfCollectionEntity, EntityConstants.TYPE_NEURON_FRAGMENT)) {
            NeuronFragment neuronFragment = getNeuronFragment(sampleEntity, separationEntity, fragmentEntity);
            neuronFragmentCollection.insert(neuronFragment);
            neuronFragmentIds.add(neuronFragment.getId());
        }
        
        NeuronSeparation neuronSeparation = new NeuronSeparation();
        neuronSeparation.setCreationDate(separationEntity.getCreationDate());
        neuronSeparation.setFilepath(separationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        neuronSeparation.setNeuronFragmentIds(neuronFragmentIds);
        return neuronSeparation;
    }
    
    private String getRelativeFilename(HasFilepath result, String filepath) {
        if (filepath==null) return null;
        String parentFilepath = result.getFilepath();
        String prefix = parentFilepath.endsWith("/") ? parentFilepath : parentFilepath+"/";
        if (!filepath.startsWith(prefix)) {
            //log.trace("File "+filepath+" is not relative to prefix "+prefix);
            return filepath;
        }
        return filepath.replaceFirst(prefix, "");
    }
    
    private NeuronFragment getNeuronFragment(Entity sampleEntity, Entity separationEntity, Entity neuronFragmentEntity) {
        NeuronFragment neuronFragment = new NeuronFragment();
        neuronFragment.setId(neuronFragmentEntity.getId());
        neuronFragment.setSampleId(sampleEntity.getId());
        neuronFragment.setOwnerKey(neuronFragmentEntity.getOwnerKey());
        neuronFragment.setReaders(getSubjectKeysWithPermission(neuronFragmentEntity, "r"));
        neuronFragment.setWriters(getSubjectKeysWithPermission(neuronFragmentEntity, "w"));
        String number = neuronFragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER);
        if (number!=null) {
            neuronFragment.setNumber(Integer.parseInt(number));
        }
        neuronFragment.setSeparationFilepath(separationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        neuronFragment.setMaskFilepath(getRelativeFilename(neuronFragment,neuronFragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_MASK_IMAGE)));
        neuronFragment.setChanFilepath(getRelativeFilename(neuronFragment,neuronFragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHAN_IMAGE)));
        neuronFragment.setSignalMipFilepath(getRelativeFilename(neuronFragment,neuronFragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE)));
        return neuronFragment;
    }
    
    private List<String> getSubjectKeysWithPermission(Entity entity, String rights) {
        List<String> subjectKeys = new ArrayList<String>();
        for(EntityActorPermission permission : entity.getEntityActorPermissions()) {
            if (permission.getPermissions().contains(rights)) {
                subjectKeys.add(permission.getSubjectKey());
            }
        }
        if (subjectKeys.isEmpty()) return null;
        return subjectKeys;
    }

    public void dropDatabase() throws DaoException {
    	init();
		try {
		    jongo.getDatabase().dropDatabase();
		}
		catch (Exception e) {
			throw new DaoException("Error clearing index with MongoDB",e);
		}
    }
}
