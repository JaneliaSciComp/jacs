package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronSeparationPipelineGridService;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * File discovery service for neuron separation results.
 * 
 * @deprecated Use the IncrementalSeparationDiscoveryService instead. This service will be removed in the next release.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparatorResultsDiscoveryService extends SupportingFilesDiscoveryService {
	
    public static final String NEURON_MIP_PREFIX = NeuronSeparationPipelineGridService.NAME+".PR.neuron";
    
    private String objective;
    private String opticalRes;
    private String pixelRes;
    
	@Override
    public void execute(IProcessData processData) throws ServiceException {
    	this.objective = (String)processData.getItem("OBJECTIVE");
    	processData.putItem("RESULT_ENTITY_TYPE", EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
    	
    	if (objective!=null) {
        	String resultEntityName = (String)processData.getItem("RESULT_ENTITY_NAME")+" "+objective;
        	processData.putItem("RESULT_ENTITY_NAME", resultEntityName);
    	}

        this.opticalRes = (String)processData.getItem("OPTICAL_RESOLUTION");
        this.pixelRes = (String)processData.getItem("PIXEL_RESOLUTION");
        
    	super.execute(processData);
    }
    
    @Override
    protected void processFolderForData(Entity separationEntity) throws Exception {

    	if (!separationEntity.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
    		throw new IllegalStateException("Expected Separator Result as input");
    	}
    	
    	helper.addFileExclusion("*.sh");

        File dir = new File(separationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing "+separationEntity.getName()+" results in "+dir.getAbsolutePath());
        
        if (!dir.canRead()) {
            logger.info("Cannot read from folder "+dir.getAbsolutePath());
            return;
        }
        
        EntityHelper entityHelper = new EntityHelper(entityBean, computeBean, ownerKey, logger);
        if (opticalRes!=null) {
            entityHelper.setOpticalResolution(separationEntity, opticalRes);
            logger.info("Set optical resolution to "+opticalRes+" on "+separationEntity.getId());
        }
        else {
            logger.info("No optical resolution defined for separation "+separationEntity.getId());
        }
        
        if (pixelRes!=null) {
            entityHelper.setPixelResolution(separationEntity, pixelRes);
            logger.info("Set pixel resolution to "+pixelRes+" on "+separationEntity.getId());
        }
        else {
            logger.info("No pixel resolution defined for separation "+separationEntity.getId());
        }

        if (objective!=null) {
            entityHelper.setObjective(separationEntity, objective);
            logger.info("Set objective to "+objective+" on "+separationEntity.getId());
        }
        else {
            logger.info("No objective defined for separation "+separationEntity.getId());
        }
        
        processSeparationFolder(separationEntity, dir);
    }
    
    protected void processSeparationFolder(Entity separationEntity, File dir) throws Exception {

        Entity supportingFiles = helper.getOrCreateSupportingFilesFolder(separationEntity);
        
        Entity fragmentsFolder = createFragmentCollection();
        helper.addToParent(separationEntity, fragmentsFolder, 1, EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION);
        
        EntityType fragmentType = entityBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_FRAGMENT);

        Entity referenceVolume = null;
        Entity signalVolume = null;
        Entity labelVolume = null;
        Entity referenceMIP = null;
        Entity signalMIP = null;
        Entity fastSignal = null;
        Entity fastReference = null;
        List<File> fragmentMipFiles = new ArrayList<File>();

        List<File> files = helper.collectFiles(dir, true);
        logger.info("Collected "+files.size()+" files in "+dir);
        FileUtils.sortFilesByName(files);
        
        for(File file : files) {
            String filename = file.getName();

            if (filename.startsWith(NEURON_MIP_PREFIX) && filename.endsWith(".png")) {       
                fragmentMipFiles.add(file); // will be added to an entity later
            }
            else if ("Reference.v3dpbd".equals(filename) || "Reference.v3draw".equals(filename)) {
                referenceVolume = helper.createResultItemForFile(file);
                helper.addToParent(supportingFiles, referenceVolume, supportingFiles.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            }
            else if ("ConsolidatedSignal.v3dpbd".equals(filename) || "ConsolidatedSignal.v3draw".equals(filename)) {
                signalVolume = helper.createResultItemForFile(file);
                helper.addToParent(supportingFiles, signalVolume, supportingFiles.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            }
            else if ("ConsolidatedLabel.v3dpbd".equals(filename) || "ConsolidatedLabel.v3draw".equals(filename)) {
                labelVolume = helper.createResultItemForFile(file);
                helper.addToParent(supportingFiles, labelVolume, supportingFiles.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            }
            else if ("ReferenceMIP.png".equals(filename)) {
                referenceMIP = helper.createResultItemForFile(file);
            }
            else if ("ConsolidatedSignalMIP.png".equals(filename)) {
                signalMIP = helper.createResultItemForFile(file);
            }
            else if ("ConsolidatedSignal2_25.mp4".equals(filename)) {
                fastSignal = helper.createResultItemForFile(file);
            }
            else if ("Reference2_100.mp4".equals(filename)) {
                fastReference = helper.createResultItemForFile(file);
            }
            else if (filename.endsWith(".pbd") || filename.endsWith(".nsp")) {
                Entity resultItem = helper.createResultItemForFile(file);
                helper.addToParent(supportingFiles, resultItem, supportingFiles.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            }
            else if (filename.endsWith(".txt")) {
                Entity resultItem = helper.createResultItemForFile(file);
                helper.addToParent(supportingFiles, resultItem, supportingFiles.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            }
            else {
                // Ignore other files
            }
        }
        
        // Set default images
        helper.setImageIfNecessary(referenceVolume, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, referenceMIP);
        helper.setImageIfNecessary(signalVolume, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, signalMIP);
        helper.setImageIfNecessary(separationEntity, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, referenceMIP);
        helper.setImageIfNecessary(separationEntity, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMIP);
        helper.setImageIfNecessary(separationEntity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE,  signalMIP);
        helper.setImageIfNecessary(referenceVolume, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE, fastReference);
        helper.setImageIfNecessary(signalVolume, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE, fastSignal);
        helper.setImageIfNecessary(separationEntity, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE,  signalVolume);

        // Set the fast 3d image on the default 3d image for the result
        // TODO: this is kind of a hack, because this service should really be ignorant of the result entity, but it will do for now.
        Set<Long> parentIds = entityBean.getParentIdsForAttribute(separationEntity.getId(),  EntityConstants.ATTRIBUTE_RESULT);
        if (parentIds.isEmpty() || parentIds.size()>1) {
            logger.warn("Unexpected number of result parents: "+parentIds.size());
        }
        else {
            Entity resultEntity = entityBean.getEntityById(parentIds.iterator().next());
            entityLoader.populateChildren(resultEntity);
            Entity default3dImage = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
            helper.setImageIfNecessary(default3dImage, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE, fastSignal);    
        }
        
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


        // Find mask/chan files
        String refMaskFile = null;
        String refChanFile = null;
        Map<Integer,String> maskFiles = new HashMap<Integer,String>();
        Map<Integer,String> chanFiles = new HashMap<Integer,String>();
        File maskChanDir = new File(dir.getAbsolutePath()+"/archive/maskChan");
        if (maskChanDir.exists()) {
            logger.info("Looking for mask/chan files in: "+maskChanDir.getAbsolutePath());
            List<File> maskChanFiles = helper.collectFiles(maskChanDir, true);
            logger.info("Collected "+maskChanFiles.size()+" files in "+dir);
        
            for(File file : maskChanFiles) {
                String name = file.getName();
                if (!name.endsWith("mask") && !name.endsWith("chan")) continue;
                if (name.equals("ref.mask")) {
                	refMaskFile = file.getAbsolutePath();
                }
                else if (name.equals("ref.chan")) {
                	refChanFile = file.getAbsolutePath();
                }
                else {
                    Integer index = null;
                    try {
                        index = NeuronSeparatorResultsDiscoveryService.getNeuronIndexFromMaskChanFile(name);
                    }
                    catch (Exception e) {
                        logger.warn("Could not parse mask/chan file name: "+name+", "+e.getMessage());
                    }
                    if (index==null) continue;
                    if (name.endsWith("mask")) {
                        maskFiles.put(index, file.getAbsolutePath());
                    }
                    else if (name.endsWith("chan")) {
                        chanFiles.put(index, file.getAbsolutePath());
                    }
                }
            }
        }
        else {
            logger.warn("The mask/chan dir does not exist at: "+maskChanDir);
        }

        if (refMaskFile!=null) {
            Entity maskImage = helper.create3dImage(refMaskFile);
            helper.setImage(referenceVolume, EntityConstants.ATTRIBUTE_MASK_IMAGE, maskImage);
        }
        else {
        	logger.warn("Missing ref mask file");
        }
        
        if (refChanFile!=null) {
            Entity chanImage = helper.create3dImage(refChanFile);
            helper.setImage(referenceVolume, EntityConstants.ATTRIBUTE_CHAN_IMAGE, chanImage);
    	}
        else {
        	logger.warn("Missing ref chan file");
        }
        
        for(File file : fragmentMipFiles) {
            Entity fragmentMIP = helper.createResultItemForFile(file);         
            Integer index = getIndex(fragmentMIP.getName());
            if (index==null) continue;
            Entity fragmentEntity = createFragmentEntity(fragmentType, index);
            helper.setDefault2dImage(fragmentEntity, fragmentMIP);
            helper.addToParent(fragmentsFolder, fragmentEntity, index, EntityConstants.ATTRIBUTE_ENTITY);
            
            String maskFilepath = maskFiles.get(index);
            if (maskFilepath!=null) {
                Entity maskImage = helper.create3dImage(maskFilepath);
                helper.setImage(fragmentEntity, EntityConstants.ATTRIBUTE_MASK_IMAGE, maskImage);
            }

            String chanFilepath = chanFiles.get(index);
            if (chanFilepath!=null) {
                Entity chanImage = helper.create3dImage(chanFilepath);
                helper.setImage(fragmentEntity, EntityConstants.ATTRIBUTE_CHAN_IMAGE, chanImage);   
            }
        }
    }
    
    protected Integer getIndex(String filename) {
        Pattern p = Pattern.compile("[^\\d]*?(\\d+)\\.png");
        Matcher m = p.matcher(filename);
        if (m.matches()) {
            String mipNum = m.group(1);
            try {
                return Integer.parseInt(mipNum);
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                //logger.warn("Error parsing number from MIP filename: "+mipNum);
            }
        }
    	return null;
    }
        
    public static Integer getNeuronIndexFromMaskChanFile(String filename) throws Exception {
        String index = filename.substring(filename.indexOf('_')+1,filename.indexOf('.'));
        return Integer.parseInt(index);
    }
    
    protected Entity createFragmentEntity(EntityType fragmentType, Integer index) throws Exception {
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
	
    protected Entity createFragmentCollection() throws Exception {
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
}
