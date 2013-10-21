package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronSeparationPipelineGridService;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * A neuron separation results discovery service which can be re-run multiple times on the same separation and
 * discover additional files each time.
 * 
 * Input variables if adding files to an existing separation:
 * 	 SEPARATION or SEPARATION_ID  
 * 
 * Input variables if discovering new separation:
 *   ROOT_ENTITY_ID - the parent of the separation
 *   ROOT_FILE_NODE - the file node containing the separation files to be discovered
 *   OBJECTIVE - the objective of the separated image
 *   OPTICAL_RESOLUTION - the optical resolution of the separated image
 *   PIXEL_RESOLUTION - the pixel resolution of the separated image
 *   RESULT_ENTITY_NAME - the name of the new neuron separation result entity
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IncrementalSeparationDiscoveryService extends AbstractEntityService {

    private static final String NEURON_MIP_PREFIX = NeuronSeparationPipelineGridService.NAME+".PR.neuron";

    protected FileDiscoveryHelper helper;
    protected Date createDate;
    
    private Map<String,Entity> resultItems = new HashMap<String,Entity>();
	private List<Entity> newResultItems = new ArrayList<Entity>();
	
	@Override
    public void execute() throws Exception {

        this.createDate = new Date();
        
        this.helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        helper.addFileExclusion("*.log");
        helper.addFileExclusion("*.oos");
        helper.addFileExclusion("sge_*");
        helper.addFileExclusion("temp");
        helper.addFileExclusion("tmp.*");
        helper.addFileExclusion("core.*");
    	helper.addFileExclusion("*.sh");

        Entity separation = (Entity)processData.getItem("SEPARATION");
        if (separation==null) {
        	String separationId = (String)processData.getItem("SEPARATION_ID");
        	if (separationId!=null) {
        		separation = entityBean.getEntityTree(Long.parseLong(separationId));
        	}
        }
        
        if (separation==null) {
        	// A new neuron separation discovery

            String rootEntityId = (String)processData.getItem("ROOT_ENTITY_ID");
        	if (rootEntityId==null) {
        		throw new IllegalArgumentException("ROOT_ENTITY_ID may not be null");
        	}
        	Entity parentEntity = entityBean.getEntityTree(Long.parseLong(rootEntityId));
        	
        	String inputImageId = (String)processData.getItem("INPUT_IMAGE_ID");
        	if (inputImageId==null) {
        		throw new IllegalArgumentException("INPUT_IMAGE_ID may not be null");
        	}
        	Entity inputEntity = entityBean.getEntityTree(Long.parseLong(inputImageId));
        	
        	FileNode resultFileNode = (FileNode)processData.getItem("ROOT_FILE_NODE");

        	String objective = (String)processData.getItem("OBJECTIVE");
        	String opticalRes = (String)processData.getItem("OPTICAL_RESOLUTION");
        	String pixelRes = (String)processData.getItem("PIXEL_RESOLUTION");
        	
    	    String resultEntityName = (String)processData.getItem("RESULT_ENTITY_NAME");
            if (StringUtils.isEmpty(resultEntityName)) {
            	resultEntityName = "Neuron Separation";
            }

        	if (!StringUtils.isEmpty(objective)) {
        		resultEntityName += " "+objective;
        	}
        	
    		separation = createSeparation(resultFileNode.getDirectoryPath(), parentEntity, resultEntityName, objective, opticalRes, pixelRes, inputEntity);	
        }
        else {
        	// Find existing result items in the neuron separation
        	logger.info("Finding existing result items...");
        	findResultItems(separation);
        }
        
        // Add additional files to the neuron separation
    	discoverySeparationFiles(separation);
    }
    
    private Entity createSeparation(String separationDir, Entity parentEntity, String resultEntityName, 
    		String objective, String opticalRes, String pixelRes, Entity inputEntity) throws Exception {
        
        Entity separation = helper.createFileEntity(separationDir, resultEntityName, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
        helper.addToParent(parentEntity, separation, parentEntity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_RESULT);
        
        logger.info("Created new separation result: "+separation.getName()+" (id="+separation.getId()+")");
        
        if (opticalRes!=null) {
            entityHelper.setOpticalResolution(separation, opticalRes);
            logger.info("Set optical resolution to "+opticalRes+" on "+separation.getId());
        }
        else {
            logger.info("No optical resolution defined for separation "+separation.getId());
        }
        
        if (pixelRes!=null) {
            entityHelper.setPixelResolution(separation, pixelRes);
            logger.info("Set pixel resolution to "+pixelRes+" on "+separation.getId());
        }
        else {
            logger.info("No pixel resolution defined for separation "+separation.getId());
        }

        if (objective!=null) {
            entityHelper.setObjective(separation, objective);
            logger.info("Set objective to "+objective+" on "+separation.getId());
        }
        else {
            logger.info("No objective defined for separation "+separation.getId());
        }
        
        helper.addToParent(separation, inputEntity, null, EntityConstants.ATTRIBUTE_INPUT_IMAGE);
        
        return separation;
	}
    
    protected void discoverySeparationFiles(Entity separation) throws Exception {

    	if (!separation.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
    		throw new IllegalStateException("Expected Neuron Separation Result as input");
    	}

        File dir = new File(separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing "+separation.getName()+" results in "+dir.getAbsolutePath());
        
        if (!dir.canRead()) {
            logger.info("Cannot read from folder "+dir.getAbsolutePath());
            return;
        }
        
        Entity supportingFiles = helper.getOrCreateSupportingFilesFolder(separation);
        Entity fragmentsFolder = getOrCreateFragmentsFolder(separation);
        
        EntityType fragmentType = entityBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_FRAGMENT);

        Entity referenceVolume = null;
        Entity signalVolume = null;
        Entity labelVolume = null;
        Entity referenceMIP = null;
        Entity signalMIP = null;
        Entity fastSignal = null;
        Entity fastReference = null;
        Entity refMask = null;
        Entity refChan = null;
        List<File> fragmentMipFiles = new ArrayList<File>();
        Map<Integer,Entity> maskEntities = new HashMap<Integer,Entity>();
        Map<Integer,Entity> chanEntities = new HashMap<Integer,Entity>();

        List<File> files = helper.collectFiles(dir, true);
        logger.info("Collected "+files.size()+" files in "+dir);
        FileUtils.sortFilesByName(files);
        
        List<Entity> resultFiles = new ArrayList<Entity>();
        
        // Find files
        for(File file : files) {
            String filename = file.getName();

            if (filename.startsWith(NEURON_MIP_PREFIX) && filename.endsWith(".png")) {       
                fragmentMipFiles.add(file); // will be added to an entity later
            }
            else if ("Reference.v3dpbd".equals(filename) || "Reference.v3draw".equals(filename)) {
                referenceVolume = getOrCreateResultItem(separation, file);
            }
            else if ("ConsolidatedSignal.v3dpbd".equals(filename) || "ConsolidatedSignal.v3draw".equals(filename)) {
                signalVolume = getOrCreateResultItem(separation, file);
            }
            else if ("ConsolidatedLabel.v3dpbd".equals(filename) || "ConsolidatedLabel.v3draw".equals(filename)) {
            	labelVolume = getOrCreateResultItem(separation, file);
            }
            else if ("ReferenceMIP.png".equals(filename)) {
                referenceMIP = getOrCreateResultItem(separation, file);
            }
            else if ("ConsolidatedSignalMIP.png".equals(filename)) {
                signalMIP = getOrCreateResultItem(separation, file);
            }
            else if ("ConsolidatedSignal2_25.mp4".equals(filename)) {
                fastSignal = getOrCreateResultItem(separation, file);
            }
            else if ("Reference2_100.mp4".equals(filename)) {
                fastReference = getOrCreateResultItem(separation, file);
            }
            else if (filename.startsWith("SeparationResult") && filename.endsWith(".nsp")) {
            	resultFiles.add(getOrCreateResultItem(separation, file));
            }
            else if (filename.startsWith("SeparationResult") && filename.endsWith(".pbd")) {
            	resultFiles.add(getOrCreateResultItem(separation, file));
            }
            else if (filename.equals("mapping_issues.txt")) {
            	resultFiles.add(getOrCreateResultItem(separation, file));
            }
            else if (filename.equals("ref.mask")) {
            	refMask = getOrCreateResultItem(separation, file);
            }
            else if (filename.equals("ref.chan")) {
            	refChan = getOrCreateResultItem(separation, file);
            }
            else if (filename.endsWith("mask")) {
            	Entity maskImage = getOrCreateResultItem(separation, file);
                Integer index = getNeuronIndexFromMaskChanFile(filename);
                maskEntities.put(index, maskImage);
            }
            else if (filename.endsWith("chan")) {
            	Entity maskImage = getOrCreateResultItem(separation, file);
                Integer index = getNeuronIndexFromMaskChanFile(filename);
                chanEntities.put(index, maskImage);
            }
        }
        
        // Add all the new files to the Supporting Data folder
        addToParentIfNecessary(supportingFiles, labelVolume, EntityConstants.ATTRIBUTE_ENTITY);
        addToParentIfNecessary(supportingFiles, signalVolume, EntityConstants.ATTRIBUTE_ENTITY);
        addToParentIfNecessary(supportingFiles, referenceVolume, EntityConstants.ATTRIBUTE_ENTITY);
        for(Entity result : resultFiles) {
        	addToParentIfNecessary(supportingFiles, result, EntityConstants.ATTRIBUTE_ENTITY);
        }
        
        // Set default images
        helper.setImageIfNecessary(referenceVolume, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, referenceMIP);
        helper.setImageIfNecessary(signalVolume, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, signalMIP);
        helper.setImageIfNecessary(separation, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, referenceMIP);
        helper.setImageIfNecessary(separation, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMIP);
        helper.setImageIfNecessary(separation, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE,  signalMIP);
        helper.setImageIfNecessary(referenceVolume, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE, fastReference);
        helper.setImageIfNecessary(signalVolume, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE, fastSignal);
        helper.setImageIfNecessary(separation, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE,  signalVolume);
        helper.setImageIfNecessary(referenceVolume, EntityConstants.ATTRIBUTE_MASK_IMAGE, refMask);
        helper.setImageIfNecessary(referenceVolume, EntityConstants.ATTRIBUTE_CHAN_IMAGE, refChan);

        // Set the fast 3d image on the default 3d image for the result
        Set<Long> parentIds = entityBean.getParentIdsForAttribute(separation.getId(), EntityConstants.ATTRIBUTE_RESULT);
        if (parentIds.isEmpty() || parentIds.size()>1) {
            logger.warn("Unexpected number of result parents: "+parentIds.size());
        }
        else {
            Entity resultEntity = entityBean.getEntityById(parentIds.iterator().next());
            entityLoader.populateChildren(resultEntity);
            Entity default3dImage = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
            if (default3dImage!=null) {
            	helper.setImageIfNecessary(default3dImage, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE, fastSignal);
            }
        }

        // Process Neurons
        Collections.sort(fragmentMipFiles, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				Integer i1 = getIndex(o1.getName());
				Integer i2 = getIndex(o2.getName());
				if (i1 == null && i2 == null) return 0;
				if (i1 == null) return 1;
				if (i2 == null) return -1;
				return i1.compareTo(i2);
			}
        });
        
        for(File file : fragmentMipFiles) {
            Entity fragmentMIP = getOrCreateResultItem(separation, file);         
            Integer index = getIndex(fragmentMIP.getName());
            
            logger.trace("Processing neuron #"+index+" with MIP "+fragmentMIP.getName());
            
            if (index==null) continue;
            Entity fragmentEntity = getOrCreateFragmentEntity(fragmentsFolder, fragmentType, index, fragmentMIP);
            
            Entity maskEntity = maskEntities.get(index);
            if (maskEntity!=null) {
                logger.trace("  Adding mask entity");
                helper.setImageIfNecessary(fragmentEntity, EntityConstants.ATTRIBUTE_MASK_IMAGE, maskEntity);
            }

            Entity chanEntity = chanEntities.get(index);
            if (chanEntity!=null) {
                logger.trace("  Adding chan entity");
                helper.setImageIfNecessary(fragmentEntity, EntityConstants.ATTRIBUTE_CHAN_IMAGE, chanEntity);   
            }
        }
    }

	private void findResultItems(Entity entity) throws Exception {
		
		logger.trace("  findResultItems "+entity.getName()+" ("+entity.getId()+")");
		String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
		if (filepath!=null) {
			logger.trace("  "+entity.getName()+": "+filepath);
			resultItems.put(filepath, entity);
		}
		
		populateChildren(entity);
		for(Entity child : entity.getChildren()) {
			findResultItems(child);
		}
	}
	
    private Entity getOrCreateResultItem(Entity separation, File resultFile) throws Exception {
    	
    	logger.trace("Get or create "+resultFile.getAbsolutePath());
    	Entity resultItem = resultItems.get(resultFile.getAbsolutePath());
    	if (resultItem==null) {
    		resultItem = helper.createResultItemForFile(resultFile);
    		newResultItems.add(resultItem);
    		resultItems.put(resultFile.getAbsolutePath(), resultItem);
    	}
    	return resultItem;
    }
    
    private Integer getIndex(String filename) {
        Pattern p = Pattern.compile("[^\\d]*?(\\d+)\\.png");
        Matcher m = p.matcher(filename);
        if (m.matches()) {
            String mipNum = m.group(1);
            try {
                return Integer.parseInt(mipNum);
            }
            catch (NumberFormatException e) {
                logger.warn("Error parsing number from MIP filename: "+mipNum);
            }
        }
    	return null;
    }
        
    private Integer getNeuronIndexFromMaskChanFile(String filename) throws Exception {
    	try {String index = filename.substring(filename.indexOf('_')+1,filename.indexOf('.'));
        	return Integer.parseInt(index);
        }
        catch (Exception e) {
            logger.warn("Could not parse mask/chan file name: "+filename+", "+e.getMessage());
        }
    	return null;
    }


	private Entity getOrCreateFragmentsFolder(Entity separation) throws Exception {
        Entity fragmentsFolder = EntityUtils.findChildWithType(separation, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
        if (fragmentsFolder==null) {
            fragmentsFolder = createFragmentCollection();
            helper.addToParent(separation, fragmentsFolder, 1, EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION);	
        }
        return fragmentsFolder;
	}
	
    private Entity createFragmentCollection() throws Exception {
        Entity fragmentsEntity = new Entity();
        fragmentsEntity.setOwnerKey(ownerKey);
        fragmentsEntity.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION));
        fragmentsEntity.setCreationDate(createDate);
        fragmentsEntity.setUpdatedDate(createDate);
        fragmentsEntity.setName("Neuron Fragments");
        fragmentsEntity = entityBean.saveOrUpdateEntity(fragmentsEntity);
        logger.info("Saved fragment collection as "+fragmentsEntity.getId());
        return fragmentsEntity;
    }

	private Entity getOrCreateFragmentEntity(Entity fragmentsFolder, EntityType fragmentType, Integer index, Entity fragmentMIP) throws Exception {
		for(Entity fragment : fragmentsFolder.getChildren()) {
			if (index.toString().equals(fragment.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER))) {
				return fragment;
			}
		}
        Entity fragmentEntity = createFragmentEntity(fragmentType, index);
        helper.setDefault2dImage(fragmentEntity, fragmentMIP);
        helper.addToParent(fragmentsFolder, fragmentEntity, index, EntityConstants.ATTRIBUTE_ENTITY);
        return fragmentEntity;
	}
	
    private Entity createFragmentEntity(EntityType fragmentType, Integer index) throws Exception {
        Entity fragmentEntity = new Entity();
        fragmentEntity.setOwnerKey(ownerKey);
        fragmentEntity.setEntityType(fragmentType);
        fragmentEntity.setCreationDate(createDate);
        fragmentEntity.setUpdatedDate(createDate);
        fragmentEntity.setName("Neuron Fragment "+index);
        fragmentEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER, index.toString());
        fragmentEntity = entityBean.saveOrUpdateEntity(fragmentEntity);
        logger.info("Saved fragment entity as "+fragmentEntity.getId());
        return fragmentEntity;
    }

	private void addToParentIfNecessary(Entity parent, Entity child, String entityAttrName) throws Exception {
	    if (child==null) return;
		for(EntityData ed : parent.getOrderedEntityData()) {
			Entity existingChild = ed.getChildEntity();
			if (existingChild!=null) {
				if (ed.getEntityAttribute().getName().equals(entityAttrName) && existingChild.getId().equals(child.getId())) {
					return; // already an entity child
				}
			}
		}
        helper.addToParent(parent, child, parent.getMaxOrderIndex()+1, entityAttrName);
	}
}
