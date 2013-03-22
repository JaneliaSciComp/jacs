package org.janelia.it.jacs.compute.service.vaa3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.Channel;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.DetectionChannel;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Convert a single tile to a sample image. Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   BULK_MERGE_PARAMETERS - a list of MergedLsmPair (may only contain MergedLsmPairs with a single image each)
 *   METADATA_RESULT_FILE_NODE - the directory in which we can find json-format LSM metadata files
 *   RUN_MERGE - was merge actually run on the pairs in BULK_MERGE_PARAMETERS?
 *   SAMPLE_ENTITY_ID - the sample entity, with channel specifications
 *   CHANNEL_DYE_SPEC (optional) - the dye specification
 *   OUTPUT_CHANNEL_ORDER (optional) - the requested channel ordering 
 * 
 * Outputs:
 *   SIGNAL_CHANNELS - space delimited list of zero-indexed signal channel indicies
 *   REFERENCE_CHANNEL - space delimited list of zero-indexed reference channel indicies
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DConvertToSampleImageService extends Vaa3DBulkMergeService {

	private static final int START_DISPLAY_PORT = 890;
    private static final String CONFIG_PREFIX = "convertConfiguration.";
    
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;
    protected String ownerKey;
    protected SampleHelper sampleHelper;
    protected EntityBeanEntityLoader entityLoader;
    
    protected FileNode metadataFileNode;
    protected Entity sampleEntity;
    protected List<MergedLsmPair> mergedLsmPairs;
    protected int randomPort;
    protected boolean merged = false;
        
    // Data lookup maps, keys by the LSM file name
    protected Map<String,Entity> lsmEntityMap = new HashMap<String,Entity>();
    protected Map<String,LSMMetadata> lsmMetadataMap = new HashMap<String,LSMMetadata>();
    
    // State for processing
    protected String referenceDye;
    
    @Override
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
        try {
            this.entityBean = EJBFactory.getLocalEntityBean();
            this.computeBean = EJBFactory.getLocalComputeBean();
            this.annotationBean = EJBFactory.getLocalAnnotationBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
            this.entityLoader = new EntityBeanEntityLoader(entityBean);
            this.randomPort = Vaa3DHelper.getRandomPort(START_DISPLAY_PORT);
            
            metadataFileNode = (FileNode)processData.getItem("METADATA_RESULT_FILE_NODE");
            if (metadataFileNode==null) {
                metadataFileNode = resultFileNode;
            }
            
            Boolean runMerge = (Boolean)processData.getItem("RUN_MERGE");
            if (runMerge!=null) {
                merged = runMerge.booleanValue();
            }
            
            String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
            if (sampleEntityId == null || "".equals(sampleEntityId)) {
                throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
            }
            
            this.sampleEntity = entityBean.getEntityById(sampleEntityId);
            if (sampleEntity == null) {
                throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
            }
            
            if (!EntityConstants.TYPE_SAMPLE.equals(sampleEntity.getEntityType().getName())) {
                throw new IllegalArgumentException("Entity is not a sample: "+sampleEntityId);
            }

            Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
            if (bulkMergeParamObj==null) {
                throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
            }

            if (bulkMergeParamObj instanceof List) {
                this.mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
            }
            else {
                throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS must be an ArrayList<MergedLsmPair>");
            }
            
            populateMaps();
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    @Override
    protected String getGridServicePrefixName() {
        return "convert";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        String channelDyeSpec = (String)processData.getItem("CHANNEL_DYE_SPEC");
        String outputChannelOrder = (String)processData.getItem("OUTPUT_CHANNEL_ORDER");
        if (channelDyeSpec!=null && outputChannelOrder!=null) {
            logger.info("Using channel dye spec: "+channelDyeSpec);
            logger.info("Using output channel order: "+outputChannelOrder);
        }
        
        int configIndex = 1;

        String consensusSignalChannels = null;
        String consensusReferenceChannels = null;
        String consensusChanSpec = null;
        
        for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {

            logger.info("Processing tile: "+mergedLsmPair.getTag());
            
            File lsm1 = new File(mergedLsmPair.getLsmFilepath1());
            Entity lsm1Entity = lsmEntityMap.get(lsm1.getName());
            LSMMetadata lsm1Metadata = lsmMetadataMap.get(lsm1.getName());
            logger.info("Parsing file 1: "+lsm1);
            
            List<String> inputChannelList = new ArrayList<String>();
            List<String> outputChannelList = new ArrayList<String>();
            
            if (channelDyeSpec!=null && outputChannelOrder!=null) {
            
                // The dye-mapping method of channel ordering relies on a dye mapping and output channel order.
                
                outputChannelList.addAll(Arrays.asList(outputChannelOrder.split(",")));
                
                Multimap<String,String> tagToDyesMap = LinkedHashMultimap.<String,String>create();
                Map<String,String> dyeToTagMap = new HashMap<String,String>();
                
                String[] channels = channelDyeSpec.split(";");
                int c = 0;
                for(String channel : channels) {
                    String[] parts = channel.split("=");
                    String channelTag = parts[0];
                    String[] channelDyes = parts[1].split(",");
                    for(String dye : channelDyes) {
                        tagToDyesMap.put(channelTag, dye);
                        if (dyeToTagMap.containsKey(dye)) {
                            throw new IllegalStateException("Dye "+dye+" is already mapped as "+dyeToTagMap.get(dye));
                        }
                        dyeToTagMap.put(dye, channelTag);
                    }
                    c++;
                }

                List<String> mergedDyeArray = new ArrayList<String>();
                Collection<String> referenceDyes = tagToDyesMap.get("reference");
                logger.info("Reference dyes: "+referenceDyes);
                
                collectDyes(lsm1Metadata, referenceDyes, mergedDyeArray);
                logger.info("Dyes from file 1: "+mergedDyeArray);
                
                if (merged) {
                    File lsm2 = new File(mergedLsmPair.getLsmFilepath2());
                    LSMMetadata lsm2Metadata = lsmMetadataMap.get(lsm2.getName());
                    logger.info("Parsing file 2: "+lsm2);
                    collectDyes(lsm2Metadata, referenceDyes, mergedDyeArray);
                    logger.info("Dyes from files 1 and 2: "+mergedDyeArray);
                    
                    // Add the reference dye back in, at the end, where the merge operation placed it
                    logger.info("    Adding reference dye to the end");
                    mergedDyeArray.add(referenceDye);
                    logger.info("All dyes: "+mergedDyeArray);
                }
                
                for(String dye : mergedDyeArray) {
                    String tag = dyeToTagMap.get(dye);
                    inputChannelList.add(tag);
                }
            }
            else {
                // If there is no dye-mapping then we have to fall back on the old chan spec method.
                
                logger.info("Falling back on chanspec...");
                
                String outputChanSpec = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
                String chanSpec1 = sampleHelper.getLSMChannelSpec(lsm1Entity);
                
                logger.info("  Output spec: "+outputChanSpec);
                logger.info("  Input spec 1: "+chanSpec1);
                
                if (merged) {
                    // When two files are merged, the reference is automatically relocated to the end. So in this case
                    // the input chan spec consists of the non-reference channels of both files, followed by the  
                    // reference channel. 
                    
                    File lsm2 = new File(mergedLsmPair.getLsmFilepath2());
                    Entity lsm2Entity = lsmEntityMap.get(lsm2.getName());
                
                    String chanSpec2 = sampleHelper.getLSMChannelSpec(lsm2Entity);
                    logger.info("  Input spec 2: "+chanSpec2);
                    
                    String chanSpec = chanSpec1.replaceAll("r", "")+chanSpec2.replaceAll("r", "")+"r";
                    inputChannelList.addAll(convertChanSpecToList(chanSpec));
                    
                    // The output channel spec is defined by the sample (usually set during SAGE sync) and is 
                    // probably the same. If the output spec is not defined by the sample, then the input spec is used.
                    
                    if (outputChanSpec==null) {
                        outputChannelList.addAll(inputChannelList);
                    }
                    else {
                        outputChannelList.addAll(convertChanSpecToList(outputChanSpec));
                    }
                    
                }
                else {
                    // When there is no merging, then the input chan spec is equal to the LSM's chan spec. 
                    inputChannelList.addAll(convertChanSpecToList(chanSpec1));

                    // The output channel spec is defined by the sample (usually set during SAGE sync) and is 
                    // probably the same. If the output spec is not defined by the sample, then the reference channel 
                    // is just normalized to the end of the file. 
                    
                    if (outputChanSpec==null) {
                        String chanSpec = chanSpec1.replaceAll("r", "")+"r";
                        outputChannelList.addAll(convertChanSpecToList(chanSpec));
                    }
                    else {
                        outputChannelList.addAll(convertChanSpecToList(outputChanSpec));
                    }
                }
            }

            logger.info("Input channel order: "+inputChannelList);
            logger.info("Output channel order: "+outputChannelList);

            StringBuffer signalChannels = new StringBuffer();            
            StringBuffer referenceChannels = new StringBuffer();
            StringBuffer chanSpec = new StringBuffer();
            
            int index = 0;
            for(String tag : outputChannelList) {
                if (tag.equals("reference") || tag.matches("r\\d+")) {
                    if (referenceChannels.length()>0) {
                        throw new IllegalStateException("More than one reference channel detected: "+referenceChannels+" "+index);
                    }
                    referenceChannels.append(index+"");
                    chanSpec.append("r");
                }
                else {
                    if (signalChannels.length()>0) signalChannels.append(" ");
                    signalChannels.append(index+"");
                    chanSpec.append("s");
                }
                index++;
            }
            
            if (consensusReferenceChannels==null) {
                consensusReferenceChannels = referenceChannels.toString();
            }
            else if (!consensusReferenceChannels.equals(referenceChannels.toString())) {
                throw new IllegalStateException("No reference channel consensus among tiles ("+consensusReferenceChannels+"!="+referenceChannels+")");
            }
                
            if (consensusSignalChannels==null) {
                consensusSignalChannels = signalChannels.toString();
            }
            else if (!consensusSignalChannels.equals(signalChannels.toString())) {
                throw new IllegalStateException("No signal channel consensus among tiles ("+consensusSignalChannels+"!="+signalChannels+")");
            }

            if (consensusChanSpec==null) {
                consensusChanSpec = chanSpec.toString();
            }
            else if (!consensusChanSpec.equals(chanSpec.toString())) {
                throw new IllegalStateException("No channel specification consensus among tiles ("+consensusChanSpec+"!="+chanSpec+")");
            }
            
            String mapping = generateChannelMapping(inputChannelList, outputChannelList);
            writeInstanceFiles(mergedLsmPair, mapping, configIndex++);        
        }
        
        createShellScript(writer);
        setJobIncrementStop(configIndex-1);
        
        logger.info("Putting '"+consensusChanSpec+"' in CHANNEL_SPEC");
        processData.putItem("CHANNEL_SPEC", consensusChanSpec);
        logger.info("Putting '"+consensusSignalChannels+"' in SIGNAL_CHANNELS");
        processData.putItem("SIGNAL_CHANNELS", consensusSignalChannels);
        logger.info("Putting '"+consensusReferenceChannels+"' in REFERENCE_CHANNEL");
        processData.putItem("REFERENCE_CHANNEL", consensusReferenceChannels);
    }
    
    private void collectDyes(LSMMetadata lsmMetadata, Collection<String> referenceDyes, List<String> dyes) {

        for(Channel channel : lsmMetadata.getChannels()) {
            DetectionChannel detection = lsmMetadata.getDetectionChannel(channel);
            if (detection!=null) {
                String dye = detection.getDyeName();
                logger.info("  Considering dye: "+dye);
                if (merged && referenceDyes.contains(dye)) {
                    logger.info("    This is a reference dye");
                    // If this is a merged pair, treat the reference dye differently from the rest
                    if (referenceDye==null) {
                        referenceDye = dye;    
                    }
                    else if (!referenceDye.equals(dye)) {
                        logger.warn("  Multiple reference dyes detected ("+referenceDye+"!="+dye+")");
                    }
                }
                else {
                    dyes.add(dye);    
                }
                
            }
        }
    }

    private void writeInstanceFiles(MergedLsmPair mergedLsmPair, String mapping, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        File tempFile = new File(resultFileNode.getDirectoryPath(), "tmp.v3draw");
        try {
            if (merged) {
                fw.write(mergedLsmPair.getMergedFilepath() + "\n");
                fw.write(mergedLsmPair.getMergedFilepath() + "\n");
            }
            else {
                fw.write(mergedLsmPair.getLsmFilepath1() + "\n");
                fw.write(mergedLsmPair.getMergedFilepath() + "\n");
            }
            fw.write(mapping + "\n");
            fw.write(tempFile.getAbsolutePath() + "\n");
            fw.write((randomPort+configIndex) + "\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    private void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read INPUT_FILENAME\n");
        script.append("read OUTPUT_FILENAME\n");
        script.append("read CHANNEL_MAPPING\n");
        script.append("read TEMP_FILE\n");
        script.append("read DISPLAY_PORT\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix("$DISPLAY_PORT"));
        script.append("\n");
        script.append(Vaa3DHelper.getMapChannelCommand("$INPUT_FILENAME", "$TEMP_FILE", "\"$CHANNEL_MAPPING\""));
        script.append("\n");
        script.append("mv $TEMP_FILE $OUTPUT_FILENAME\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }
    
    private String generateChannelMapping(List<String> inputChannelList, List<String> outputChannelList) throws Exception {
        
        logger.info("Input channels: "+inputChannelList);
        logger.info("Output channels: "+outputChannelList);
        
        Map<String,Integer> sourceIndexMap = new HashMap<String,Integer>();
        int index = 0;
        for(String tag : inputChannelList) {
            sourceIndexMap.put(tag, index);
            index++;
        }
        
        StringBuilder mapChannelString = new StringBuilder();
        int targetIndex=0;
        for(String tag : outputChannelList) {
            Integer sourceIndex = sourceIndexMap.get(tag);
            if (sourceIndex==null) {
                throw new IllegalStateException("No such tag in source image: "+tag);
            }
            if (targetIndex>0) mapChannelString.append(",");
            mapChannelString.append(sourceIndex+","+targetIndex);
            targetIndex++;
        }
        
        logger.info("Channel mapping string: "+mapChannelString);
        return mapChannelString.toString();
    }
    
    private List<String> convertChanSpecToList(String chanSpec) { 

        int s = 0;
        int r = 0;
        List<String> channelList = new ArrayList<String>();
        for(int sourceIndex=0; sourceIndex<chanSpec.length(); sourceIndex++) {
            char imageChanCode = chanSpec.charAt(sourceIndex);
            switch (imageChanCode) {
            case 's':
                channelList.add("s"+s);
                s++;
                break;
            case 'r':
                channelList.add("r"+r);
                r++;
                break;
            default:
                new IllegalStateException("Unknown channel code: "+imageChanCode);
            }
        }
        return channelList;
    }
    
    private void populateMaps() throws Exception {
        
        entityLoader.populateChildren(sampleEntity);
        Entity supportingData = EntityUtils.getSupportingData(sampleEntity);
        entityLoader.populateChildren(supportingData);
        for(Entity tile : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_IMAGE_TILE)) {
            entityLoader.populateChildren(tile);
            for(Entity image : EntityUtils.getChildrenOfType(tile, EntityConstants.TYPE_LSM_STACK)) {    
                lsmEntityMap.put(image.getName(), image);
                File jsonFile = new File(metadataFileNode.getDirectoryPath(), image.getName()+".json");
                try {
                    LSMMetadata metadata = LSMMetadata.fromFile(jsonFile);
                    lsmMetadataMap.put(image.getName(), metadata);    
                }
                catch (Exception e) {
                    throw new Exception("Error parsing LSM metadata file: "+jsonFile,e);
                }
            }
        }
    }
}
