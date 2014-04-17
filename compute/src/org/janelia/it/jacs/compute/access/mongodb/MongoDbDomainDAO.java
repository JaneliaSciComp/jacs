package org.janelia.it.jacs.compute.access.mongodb;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.Folder;
import org.janelia.it.jacs.model.domain.Image3d;
import org.janelia.it.jacs.model.domain.LSMImage;
import org.janelia.it.jacs.model.domain.NeuronFragment;
import org.janelia.it.jacs.model.domain.NeuronSeparation;
import org.janelia.it.jacs.model.domain.ObjectiveSample;
import org.janelia.it.jacs.model.domain.PipelineResult;
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

    protected final static int MONGODB_LOADER_BATCH_SIZE = 20000;
    protected final static int MONGODB_INSERTS_PER_SECOND = 5000;
	
	protected static final String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
	protected static final String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");

	protected LargeOperations largeOp;
	protected MongoClient m;
	protected DB db;
	protected MongoCollection  folderCollection;
	protected MongoCollection  sampleCollection;
	protected Jongo jongo;
	
    public MongoDbDomainDAO(Logger log) {
    	super(log);
    }
    
    private void init() throws DaoException {
    	if (m!=null) return;
        try {
        	m = new MongoClient(MONGO_SERVER_URL);
        	m.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
        	db = m.getDB(MONGO_DATABASE);
        	jongo = new Jongo(db, 
        	        new JacksonMapper.Builder()
        	            .enable(MapperFeature.AUTO_DETECT_GETTERS)
                        .enable(MapperFeature.AUTO_DETECT_SETTERS)
        	            .build());
        	folderCollection = jongo.getCollection("folder").withWriteConcern(WriteConcern.UNACKNOWLEDGED);
        	sampleCollection = jongo.getCollection("sample").withWriteConcern(WriteConcern.UNACKNOWLEDGED);
        }
		catch (UnknownHostException e) {
			throw new RuntimeException("Unknown host given in MongoDB.ServerURL value in system properties: "+MONGO_SERVER_URL);
		}
    }
    
    public void loadAllEntities() throws DaoException {

//        String subjectKey = "group:heberleinlab";
        String subjectKey = "user:asoy";

        log.info("Adding folders");
        List<Folder> folders = getRootFolders(getSubjectByNameOrKey(subjectKey));
        for(Folder folder : folders) {
            long start = System.currentTimeMillis();
            folderCollection.save(folder);
            log.info("  Adding "+folder.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
        }
        
    	log.info("Adding samples");
    	
//    	Entity folder = getEntitiesByNameAndTypeName(subjectKey, "MB Split MCFO Case 1", EntityConstants.TYPE_FOLDER).get(0);
    
    	for(Entity entity : getEntitiesByTypeName(subjectKey, EntityConstants.TYPE_SAMPLE)) {
//    	for(Entity entity : folder.getOrderedChildren()) {
    	    
    	    if (entity.getName().contains("~")) continue;
    	    
            long start = System.currentTimeMillis();
            
    	    Sample sample = getSampleObject(entity);
    	    if (sample!=null) {
    	        sampleCollection.save(sample);
    	    }
            
            log.info("  Adding "+sample.getName()+" took "+(System.currentTimeMillis()-start)+" ms");
            
            // Free memory
            entity.setEntityData(null);
    	}
        
        log.info("Creating indexes");
        sampleCollection.ensureIndex("{name:1, ownerKey:1, readers:1, writers:1, dataSet:1, line:1}");
        folderCollection.ensureIndex("{name:1, ownerKey:1, readers:1, writers:1}");
    }
    
    private List<Folder> getRootFolders(Subject subject) throws DaoException {

        List<Folder> folders = new ArrayList<Folder>();
        for(Entity folder : getEntitiesWithTag(subject.getKey(), EntityConstants.ATTRIBUTE_COMMON_ROOT)) {
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

        List<Long> itemIds = new ArrayList<Long>();
        String itemType = null;
        
        for(Entity childEntity : entity.getOrderedChildren()) {
            if (childEntity.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
                folders.addAll(getFolders(childEntity));
            }
            itemIds.add(childEntity.getId());
            if (itemType!=null && !itemType.equals(childEntity.getEntityTypeName())) {
                log.warn("Inconsistent entity types within folder: "+entity.getId()+" ("+itemType+"!="+childEntity.getEntityTypeName()+")");
            }
            itemType = childEntity.getEntityTypeName();
        }
        
        if (!itemIds.isEmpty()) {
            folder.setItemIds(itemIds);
            folder.setItemType(itemType);
        }
        
        folders.add(folder);
        
        return folders;
    }
    
    private Sample getSampleObject(Entity entity) {
        
        if (entity.getName().contains("~")) {
            log.warn("Cannot process sub-sample as sample: "+entity.getName());
            return null;
        }
        
        if (entity.getEntityData()==null) {
            log.warn("Cannot process sample with null entityData: "+entity.getName());
            return null;
        }
        
        Sample sample = new Sample();
                
        sample.setId(entity.getId());
        sample.setOwnerKey(entity.getOwnerKey());
        sample.setReaders(getSubjectKeysWithPermission(entity, "r"));
        sample.setWriters(getSubjectKeysWithPermission(entity, "w"));
        sample.setCreationDate(entity.getCreationDate());
        sample.setUpdatedDate(entity.getUpdatedDate());
        sample.setName(entity.getName());
        sample.setAge(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_AGE));
        sample.setChanSpec(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
        sample.setDataSet(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER));
        sample.setEffector(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_EFFECTOR));
        sample.setLine(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_LINE));
        sample.setSlideCode(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SLIDE_CODE));
        sample.setStatus(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS));
        sample.setVisited(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISITED));
        
        Map<String, ObjectiveSample> objectiveSamples = new HashMap<String, ObjectiveSample>();
        
        List<Entity> objSampleEntities = EntityUtils.getChildrenOfType(entity, EntityConstants.TYPE_SAMPLE);
        if (objSampleEntities.isEmpty()) {
            String objective = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
            if (objective==null) {
                log.warn("Sample has no objective: "+entity.getName());
                objective = "63x";
            }
            objectiveSamples.put(objective, getObjectiveSampleObject(entity));
        }
        else {
            for (Entity objSampleEntity : objSampleEntities) {
                String objective = objSampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
                if (objective==null) {
                    log.warn("Sample has no objective: "+objSampleEntity.getName());
                    objective = "63x";
                }
                objectiveSamples.put(objective, getObjectiveSampleObject(objSampleEntity));
            }
        }
        
        sample.setObjectives(objectiveSamples);
        
        return sample;
    }

    private ObjectiveSample getObjectiveSampleObject(Entity entity) {
        
        ObjectiveSample sample = new ObjectiveSample();
        
        Entity supportingDataEntity = EntityUtils.getSupportingData(entity);
        
        List<SampleTile> tiles = new ArrayList<SampleTile>();
        for(Entity tileEntity : supportingDataEntity.getOrderedChildren()) {
            
            List<LSMImage> lsms = new ArrayList<LSMImage>();
            for(Entity lsmEntity : EntityUtils.getChildrenOfType(tileEntity, EntityConstants.TYPE_LSM_STACK)) {
                LSMImage lsm = new LSMImage();
                lsm.setAge(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_AGE));
                lsm.setAnatomicalArea(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA));
                lsm.setChannelColors(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS));
                lsm.setChannelDyeNames(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES));
                lsm.setChanSpec(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
                lsm.setEffector(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_EFFECTOR));
                lsm.setFilepath(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                lsm.setGender(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_GENDER));
                lsm.setLine(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_LINE));
                lsm.setMipFilepath(lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE));
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
                lsms.add(lsm);
            }
            
            SampleTile tile = new SampleTile();
            tile.setName(tileEntity.getName());
            tile.setAnatomicalArea(tileEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA));
            tile.setLsms(lsms);
            tiles.add(tile);
        }
        
        sample.setTiles(tiles);

        List<SamplePipelineRun> runs = new ArrayList<SamplePipelineRun>();
        for(Entity runEntity : EntityUtils.getChildrenOfType(entity, EntityConstants.TYPE_PIPELINE_RUN)) {

            List<PipelineResult> results = new ArrayList<PipelineResult>();
            for(Entity resultEntity : EntityUtils.getChildrenForAttribute(runEntity, EntityConstants.ATTRIBUTE_RESULT)) {
                Entity resultSupportingData = EntityUtils.getSupportingData(resultEntity);

                if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {

                    List<PipelineResult> sprResults = new ArrayList<PipelineResult>();
                    for(Entity separationEntity : EntityUtils.getChildrenOfType(resultEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                        sprResults.add(getNeuronSeparation(separationEntity));
                    }
                    
                    SampleProcessingResult result = new SampleProcessingResult();
                    result.setCreationDate(resultEntity.getCreationDate());
                    result.setFilepath(resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                    result.setAnatomicalArea(resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA));
                    
                    Entity stackEntity = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                    if (stackEntity!=null) {
                        result.setResultStack(getImage3d(stackEntity));
                    }
                    result.setResults(sprResults);
                    
                    results.add(result);
                }
                else if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
                    
                    List<PipelineResult> sprResults = new ArrayList<PipelineResult>();
                    for(Entity separationEntity : EntityUtils.getChildrenOfType(resultEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                        sprResults.add(getNeuronSeparation(separationEntity));
                    }
                    
                    SampleAlignmentResult result = new SampleAlignmentResult();
                    result.setCreationDate(resultEntity.getCreationDate());
                    result.setName(resultEntity.getName());
                    result.setFilepath(resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                    result.setAlignmentSpace(resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE));
                                        
                    result.setResults(sprResults);
                    
                    results.add(result);
                }
                
            }
            
            SamplePipelineRun run = new SamplePipelineRun();
            run.setName(runEntity.getName());
            run.setCreationDate(runEntity.getCreationDate());
            run.setPipelineProcess(runEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS));
            run.setResults(results);
            runs.add(run);
        }

        sample.setPipelineRuns(runs);
        
        return sample;
    }
    
    private Image3d getImage3d(Entity stackEntity) {

        Image3d resultStack = new Image3d();
        resultStack.setFile(stackEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        resultStack.setImageSize(stackEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION));
        resultStack.setOpticalResolution(stackEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION));
        resultStack.setReferenceMipFilepath(stackEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE));
        resultStack.setSignalMipFilepath(stackEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE));
        resultStack.setMovieFile(stackEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE));
        
        return resultStack;
    }
    
    private NeuronSeparation getNeuronSeparation(Entity separationEntity) {

        List<NeuronFragment> neuronFragments = new ArrayList<NeuronFragment>();
        Entity nfCollectionEntity = EntityUtils.getLatestChildOfType(separationEntity, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
        for(Entity fragmentEntity : EntityUtils.getChildrenOfType(nfCollectionEntity, EntityConstants.TYPE_NEURON_FRAGMENT)) {
            neuronFragments.add(getNeuronFragment(fragmentEntity));
        }
        
        NeuronSeparation neuronSeparation = new NeuronSeparation();
        neuronSeparation.setCreationDate(separationEntity.getCreationDate());
        neuronSeparation.setFilepath(separationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        neuronSeparation.setNeuronFragments(neuronFragments);
        
        return neuronSeparation;
    }
    
    private NeuronFragment getNeuronFragment(Entity neuronFragmentEntity) {
        NeuronFragment neuronFragment = new NeuronFragment();
        String number = neuronFragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER);
        if (number!=null) {
            neuronFragment.setNumber(Integer.parseInt(number));
        }
        neuronFragment.setMaskFile(neuronFragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_MASK_IMAGE));
        neuronFragment.setChanFile(neuronFragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHAN_IMAGE));
        neuronFragment.setSignalMipImage(neuronFragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE));
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
	    	db.dropDatabase();
		}
		catch (Exception e) {
			throw new DaoException("Error clearing index with MongoDB",e);
		}
    }
}
