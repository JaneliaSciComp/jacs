package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronSeparationPipelineGridService;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * A neuron separation results discovery service which can be re-run multiple times on the same separation and
 * discover additional files each time.
 * 
 * Input variables if adding files to an existing separation:
 * 	 RESULT_ENTITY or RESULT_ENTITY_ID  
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
public class IncrementalSeparationDiscoveryService extends IncrementalResultDiscoveryService {

    private static final String NEURON_MIP_PREFIX = NeuronSeparationPipelineGridService.NAME+".PR.neuron";

	@Override
	protected Entity createNewResultEntity(String resultName) throws Exception {

        Long inputImageId = data.getRequiredItemAsLong("INPUT_IMAGE_ID");
        Entity inputEntity = entityBean.getEntityTree(inputImageId);
        
        String objective = data.getItemAsString("OBJECTIVE");
        String opticalRes = data.getItemAsString("OPTICAL_RESOLUTION");
        String pixelRes = data.getItemAsString("PIXEL_RESOLUTION");
        
        Long sourceSeparationId = data.getItemAsLong("SOURCE_SEPARATION_ID");
        Entity sourceSeparation = null;
        if (sourceSeparationId!=null) {
            sourceSeparation = entityBean.getEntityById(sourceSeparationId);
        }
        
        boolean isWarped = !StringUtils.isEmpty(data.getItemAsString("ALIGNED_CONSOLIDATED_LABEL_FILEPATH"));
        
        if (StringUtils.isEmpty(resultName)) {
            resultName = "Neuron Separation";
        }

        if (!StringUtils.isEmpty(objective)) {
            resultName += " "+objective;
        }
        
        Entity separation = helper.createFileEntity(resultFileNode.getDirectoryPath(), resultName, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
        
        contextLogger.info("Created new separation result: "+separation.getName()+" (id="+separation.getId()+")");
        
        if (opticalRes!=null) {
            entityHelper.setOpticalResolution(separation, opticalRes);
            contextLogger.info("Set optical resolution to "+opticalRes+" on "+separation.getId());
        }
        else {
            contextLogger.info("No optical resolution defined for separation "+separation.getId());
        }
        
        if (pixelRes!=null) {
            entityHelper.setPixelResolution(separation, pixelRes);
            contextLogger.info("Set pixel resolution to "+pixelRes+" on "+separation.getId());
        }
        else {
            contextLogger.info("No pixel resolution defined for separation "+separation.getId());
        }

        if (objective!=null) {
            entityHelper.setObjective(separation, objective);
            contextLogger.info("Set objective to "+objective+" on "+separation.getId());
        }
        else {
            contextLogger.info("No objective defined for separation "+separation.getId());
        }
        
        if (sourceSeparation!=null) {
            helper.addToParent(separation, sourceSeparation, null, EntityConstants.ATTRIBUTE_SOURCE_SEPARATION);
            contextLogger.info("Set source separation to "+sourceSeparation.getId()+" on "+separation.getId());
        }
        else {
            contextLogger.info("No source defined for separation "+separation.getId());
            contextLogger.info("Marking "+separation.getId()+" as a warped separation");
        }
        
        if (isWarped) {
            EntityUtils.addAttributeAsTag(separation, EntityConstants.ATTRIBUTE_IS_WARPED_SEPARATION);
            entityBean.saveOrUpdateEntity(separation);
        }
        
        helper.addToParent(separation, inputEntity, null, EntityConstants.ATTRIBUTE_INPUT_IMAGE);
        
        return separation;
	}
    
	@Override
    protected void discoverResultFiles(Entity separation) throws Exception {

    	if (!separation.getEntityTypeName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
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
        contextLogger.info("Collected "+files.size()+" files in "+dir);
        FileUtils.sortFilesByName(files);
        
        List<Entity> resultFiles = new ArrayList<Entity>();
        
        // Find files
        for(File file : files) {
            String filename = file.getName();

            if (filename.startsWith(NEURON_MIP_PREFIX) && filename.endsWith(".png")) {       
                fragmentMipFiles.add(file); // will be added to an entity later
            }
            else if ("Reference.v3dpbd".equals(filename) || "Reference.v3draw".equals(filename)) {
                referenceVolume = getOrCreateResultItem(file);
            }
            else if ("ConsolidatedSignal.v3dpbd".equals(filename) || "ConsolidatedSignal.v3draw".equals(filename)) {
                signalVolume = getOrCreateResultItem(file);
            }
            else if ("ConsolidatedLabel.v3dpbd".equals(filename) || "ConsolidatedLabel.v3draw".equals(filename)) {
            	labelVolume = getOrCreateResultItem(file);
            }
            else if ("ReferenceMIP.png".equals(filename)) {
                referenceMIP = getOrCreateResultItem(file);
            }
            else if ("ConsolidatedSignalMIP.png".equals(filename)) {
                signalMIP = getOrCreateResultItem(file);
            }
            else if ("ConsolidatedSignal2_25.mp4".equals(filename)) {
                fastSignal = getOrCreateResultItem(file);
            }
            else if ("Reference2_100.mp4".equals(filename)) {
                fastReference = getOrCreateResultItem(file);
            }
            else if (filename.startsWith("SeparationResult") && filename.endsWith(".nsp")) {
            	resultFiles.add(getOrCreateResultItem(file));
            }
            else if (filename.startsWith("SeparationResult") && filename.endsWith(".pbd")) {
            	resultFiles.add(getOrCreateResultItem(file));
            }
            else if (filename.startsWith("mapping_issues")) {
            	resultFiles.add(getOrCreateResultItem(file));
            }
            else if (filename.equals("ref.mask")) {
            	refMask = getOrCreateResultItem(file);
            }
            else if (filename.equals("ref.chan")) {
            	refChan = getOrCreateResultItem(file);
            }
            else if (filename.endsWith("mask")) {
            	Entity maskImage = getOrCreateResultItem(file);
                Integer index = getNeuronIndexFromMaskChanFile(filename);
                maskEntities.put(index, maskImage);
            }
            else if (filename.endsWith("chan")) {
            	Entity maskImage = getOrCreateResultItem(file);
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

        Entity inputImage = separation.getChildByAttributeName(EntityConstants.ATTRIBUTE_INPUT_IMAGE);
        if (inputImage!=null) {
            contextLogger.info("Setting fast 3d image on the separation's input image: "+inputImage.getName());
        	helper.setImageIfNecessary(inputImage, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE, fastSignal);
        }
        else {
            contextLogger.warn("Could not find input image for separation: "+separation.getId());
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
            Entity fragmentMIP = getOrCreateResultItem(file);         
            Integer index = getIndex(fragmentMIP.getName());
            
            logger.trace("Processing neuron #"+index+" with MIP "+fragmentMIP.getName());
            
            if (index==null) continue;
            Entity fragmentEntity = getOrCreateFragmentEntity(fragmentsFolder, index, fragmentMIP);
            
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
            contextLogger.warn("Could not parse mask/chan file name: "+filename+", "+e.getMessage());
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
        fragmentsEntity.setEntityTypeName(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
        fragmentsEntity.setCreationDate(createDate);
        fragmentsEntity.setUpdatedDate(createDate);
        fragmentsEntity.setName("Neuron Fragments");
        fragmentsEntity = entityBean.saveOrUpdateEntity(fragmentsEntity);
        contextLogger.info("Saved fragment collection as "+fragmentsEntity.getId());
        return fragmentsEntity;
    }

	private Entity getOrCreateFragmentEntity(Entity fragmentsFolder, Integer index, Entity fragmentMIP) throws Exception {
		for(Entity fragment : fragmentsFolder.getChildren()) {
			if (index.toString().equals(fragment.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER))) {
				return fragment;
			}
		}
        Entity fragmentEntity = createFragmentEntity(index);
        helper.setDefault2dImage(fragmentEntity, fragmentMIP);
        helper.addToParent(fragmentsFolder, fragmentEntity, index, EntityConstants.ATTRIBUTE_ENTITY);
        return fragmentEntity;
	}
	
    private Entity createFragmentEntity(Integer index) throws Exception {
        Entity fragmentEntity = new Entity();
        fragmentEntity.setOwnerKey(ownerKey);
        fragmentEntity.setEntityTypeName(EntityConstants.TYPE_NEURON_FRAGMENT);
        fragmentEntity.setCreationDate(createDate);
        fragmentEntity.setUpdatedDate(createDate);
        fragmentEntity.setName("Neuron Fragment "+index);
        fragmentEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER, index.toString());
        fragmentEntity = entityBean.saveOrUpdateEntity(fragmentEntity);
        contextLogger.info("Saved fragment entity as "+fragmentEntity.getId());
        return fragmentEntity;
    }
}
