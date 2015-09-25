package org.janelia.it.jacs.compute.service.vaa3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.compute.service.exceptions.EntityException;
import org.janelia.it.jacs.compute.service.exceptions.LSMMetadataException;
import org.janelia.it.jacs.compute.service.exceptions.MetadataConsensusException;
import org.janelia.it.jacs.compute.service.exceptions.MetadataException;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.MergeAlgorithm;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.Channel;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.DetectionChannel;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Convert a single tile to a sample image. Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   METADATA_RESULT_FILE_NODE - the directory in which we can find json-format LSM metadata files
 *   RUN_MERGE - was merge actually run on the pairs?
 *   SAMPLE_ENTITY_ID - the sample entity, with channel specifications
 *   SAMPLE_AREA - the anatomical area we are interested in (merged LSM pairs may only contain a single image each)
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
    protected AnatomicalArea sampleArea;
    protected List<MergedLsmPair> mergedLsmPairs;
    protected int randomPort;
    protected boolean merged = false;
    protected String mergeAlgorithm;    
    
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
            this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger, contextLogger);
            this.entityLoader = new EntityBeanEntityLoader(entityBean);
            this.randomPort = Vaa3DHelper.getRandomPort(START_DISPLAY_PORT);

            // retrieve sample first so that it gets added to log context
            sampleEntity = sampleHelper.getRequiredSampleEntity(data);

            metadataFileNode = (FileNode) data.getItem("METADATA_RESULT_FILE_NODE");
            if (metadataFileNode == null) {
                metadataFileNode = resultFileNode;
            }

            merged = data.getItemAsBoolean("RUN_MERGE");
            sampleArea = (AnatomicalArea) data.getRequiredItem("SAMPLE_AREA");
            mergedLsmPairs = sampleArea.getMergedLsmPairs();
            
            Object mergeAlgoObj = data.getItem("MERGE_ALGORITHM");
            if (mergeAlgoObj instanceof String) {
            	mergeAlgorithm = (String)mergeAlgoObj;
            }
            else {
	            List<String> mergeAlgorithms = (List<String>)mergeAlgoObj;
	            if (mergeAlgorithms != null && !mergeAlgorithms.isEmpty()) {
	                mergeAlgorithm = mergeAlgorithms.get(0);
	            }
            }
            
            if (mergeAlgorithm==null) {
            	logger.warn("Merge algorithm is not specified, defaulting to FLYLIGHT (RGB ordering) algorithm.");
            }
            
            populateMaps();

        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    @Override
    protected String getGridServicePrefixName() {
        return "convert";
    }
    
    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        String channelDyeSpec = data.getItemAsString("CHANNEL_DYE_SPEC");
        String outputChannelOrder = data.getItemAsString("OUTPUT_CHANNEL_ORDER");

        int configIndex = 1;

        String consensusChannelMapping = null;
        String consensusSignalChannels = null;
        String consensusReferenceChannels = null;
        String consensusChanSpec = null;
        
        for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {

            contextLogger.info("Processing tile: "+mergedLsmPair.getTag());
            
            File lsm1 = new File(mergedLsmPair.getLsmFilepath1());
            LSMMetadata lsm1Metadata = lsmMetadataMap.get(lsm1.getName());
            contextLogger.info("Parsing file 1: "+lsm1);
            
            List<String> unmergedChannelList = new ArrayList<String>();
            List<String> inputChannelList = new ArrayList<String>();
            List<String> outputChannelList = new ArrayList<String>();
            
            if (channelDyeSpec!=null && outputChannelOrder!=null) {

                List<String> input1ChannelList = new ArrayList<String>();
                List<String> input2ChannelList = new ArrayList<String>();
                
                // The dye-mapping method of channel ordering relies on a dye mapping and output channel order.
                
                outputChannelList.addAll(Arrays.asList(outputChannelOrder.split(",")));
                
                Multimap<String,String> tagToDyesMap = LinkedHashMultimap.create();
                Map<String,String> dyeToTagMap = new HashMap<String,String>();
                
                String[] channels = channelDyeSpec.split(";");
                for(String channel : channels) {
                    String[] parts = channel.split("=");
                    String channelTag = parts[0];
                    String[] channelDyes = parts[1].split(",");
                    for(String dye : channelDyes) {
                        tagToDyesMap.put(channelTag, dye);
                        if (dyeToTagMap.containsKey(dye)) {
                            throw new ServiceException("Dye "+dye+" is already mapped as "+dyeToTagMap.get(dye));
                        }
                        dyeToTagMap.put(dye, channelTag);
                        contextLogger.info("Mapping dye '"+dye+"' to channel tag '"+channelTag+"'");
                    }
                }

                List<String> lsm1DyeArray = new ArrayList<String>();
                List<String> lsm2DyeArray = new ArrayList<String>();
                List<String> mergedDyeArray = new ArrayList<String>();
                
                Collection<String> referenceDyes = tagToDyesMap.get("reference");
                contextLogger.info("Reference dyes: "+referenceDyes);
                
                collectDyes(lsm1Metadata, referenceDyes, mergedDyeArray, lsm1DyeArray);
                contextLogger.info("Dyes from file 1: "+mergedDyeArray);
                
                if (merged) {
                    File lsm2 = new File(mergedLsmPair.getLsmFilepath2());
                    LSMMetadata lsm2Metadata = lsmMetadataMap.get(lsm2.getName());

                    contextLogger.info("Parsing file 2: "+lsm2);
                    collectDyes(lsm2Metadata, referenceDyes, mergedDyeArray, lsm2DyeArray);
                    contextLogger.info("Dyes from files 1 and 2: "+mergedDyeArray);
                    
                    // Add the reference dye back in, at the end, where the merge operation placed it
                    contextLogger.info("    Adding reference dye to the end");
                    mergedDyeArray.add(referenceDye);
                    contextLogger.info("All dyes: "+mergedDyeArray);
                }
                
                input1ChannelList.addAll(convertDyesToTags(lsm1DyeArray, dyeToTagMap));
                input2ChannelList.addAll(convertDyesToTags(lsm2DyeArray, dyeToTagMap));
                inputChannelList.addAll(convertDyesToTags(mergedDyeArray, dyeToTagMap));
                
                unmergedChannelList.addAll(input1ChannelList);
                unmergedChannelList.addAll(input2ChannelList);
            }
            else {
                // If there is no dye-mapping then we have to fall back on the old chan spec method.

                contextLogger.info("Falling back on chanspec...");

                Entity lsm1Entity = lsmEntityMap.get(lsm1.getName());
                if (lsm1Entity==null) throw new EntityException("Could not find LSM entity for first LSM: "+lsm1.getName());

                String outputChanSpec = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
                
                int refIndex1 = getRefIndex(lsm1Metadata);
                String chanSpec1 = sampleHelper.getLSMChannelSpec(lsm1Entity, refIndex1);

                contextLogger.info("  Output spec: "+outputChanSpec);
                contextLogger.info("  Input spec 1: "+chanSpec1);
                
                if (merged) {
                    
                    // When two files are merged, the reference is automatically relocated to the end. So in this case
                    // the input chan spec consists of the non-reference channels of both files, followed by the  
                    // reference channel. 
                    
                    File lsm2 = new File(mergedLsmPair.getLsmFilepath2());
                    Entity lsm2Entity = lsmEntityMap.get(lsm2.getName());
                    if (lsm2Entity==null) throw new EntityException("Could not find LSM entity for second LSM: "+lsm2.getName());
                
                    LSMMetadata lsm2Metadata = lsmMetadataMap.get(lsm2.getName());
                    int refIndex2 = getRefIndex(lsm2Metadata);
                    
                    String chanSpec2 = sampleHelper.getLSMChannelSpec(lsm2Entity, refIndex2);
                    contextLogger.info("  Input spec 2: "+chanSpec2);
                    
                    unmergedChannelList.addAll(ChanSpecUtils.convertChanSpecToList(chanSpec1+chanSpec2));
                    
                    if (MergeAlgorithm.FLYLIGHT_ORDERED.getName().equals(mergeAlgorithm)) {
                        
                        // Channels were kept in original order, but the reference was moved to the end

                        String chanSpec = chanSpec1.replaceAll("r", "")+chanSpec2.replaceAll("r", "")+"r";
                        contextLogger.info("  Using ordered merge algorithm with chanspec: "+chanSpec);
                        inputChannelList.addAll(ChanSpecUtils.convertChanSpecToList(chanSpec));
                        
                        // The output channel spec is defined by the sample (usually set during SAGE sync) and is 
                        // probably the same. If the output spec is not defined by the sample, then the input spec is used.
                        
                        if (outputChanSpec==null) {
                            outputChannelList.addAll(inputChannelList);
                        }
                        else {
                            outputChannelList.addAll(ChanSpecUtils.convertChanSpecToList(outputChanSpec));
                        }
                    }
                    else {
                        // Channels were reordered to RGB order by the merge step, and the reference was moved to the end
                        contextLogger.info("  Using RGB merge algorithm");

                        String redTag = null;
                        String greenTag = null;
                        String blueTag = null;
                        String refTag = null;
                        
                        List<String> tags = new ArrayList<String>(unmergedChannelList);

                        // The RGB merge algorithm discards the second reference
                        tags.remove("r1");
                        
                        List<Channel> allChannels = new ArrayList<Channel>();
                        allChannels.addAll(lsm1Metadata.getChannels());
                        allChannels.addAll(lsm2Metadata.getChannels());
                        
                        for(Channel channel : allChannels) {
                            String color = channel.getColor();
                            if (tags.isEmpty()) {
                            	contextLogger.error("    Ran out of tags while processing channel with color "+color);
                            	break;
                            }
                            if ("#FF0000".equalsIgnoreCase(color)) {
                                redTag = tags.remove(0);
                            }
                            else if ("#00FF00".equalsIgnoreCase(color)) {
                                greenTag = tags.remove(0);
                            }
                            else if ("#0000FF".equalsIgnoreCase(color)) {
                                blueTag = tags.remove(0);
                            }
                            else if (refTag==null) {
                                refTag = tags.remove(0);
                            }
                        }

                        if (!tags.isEmpty()) {
                        	// Fill in any missing color channels with the left over tags. 
                        	// This is a bit of a hack to deal with bad data. It should never happen if the LSM metadata is good. 
                        	contextLogger.info("    Leftover tags: "+tags);
                            if (!tags.isEmpty() && redTag==null) {
                            	redTag = tags.remove(0);
                            	contextLogger.warn("    Overriding tag for red channel: "+redTag);
                            }
                            if (!tags.isEmpty() && greenTag==null) {
                            	greenTag = tags.remove(0);
                            	contextLogger.warn("    Overriding tag for green channel: "+greenTag);
                            }
                            if (!tags.isEmpty() && blueTag==null) {
                            	blueTag = tags.remove(0);
                            	contextLogger.warn("    Overriding tag for blue channel: "+blueTag);
                            }
                        }
                        
                        
                        contextLogger.info("    Found RGB tags "+redTag+","+greenTag+","+blueTag+" and Reference tag "+refTag);
                        
                        inputChannelList.add(redTag);
                        inputChannelList.add(greenTag);
                        inputChannelList.add(blueTag);
                        inputChannelList.add(refTag);
                    }

                    // The channels are already ordered by the merge step
                    outputChannelList.addAll(inputChannelList);
                }
                else {
                    contextLogger.info("  Using unmerged algorithm");
                    
                    unmergedChannelList.addAll(ChanSpecUtils.convertChanSpecToList(chanSpec1));
                    // When there is no merging, then the input chan spec is equal to the LSM's chan spec. 
                    inputChannelList.addAll(ChanSpecUtils.convertChanSpecToList(chanSpec1));

                    // The output channel spec is defined by the sample (usually set during SAGE sync) and is 
                    // probably the same. If the output spec is not defined by the sample, then the reference channel 
                    // is just normalized to the end of the file. 
                    
                    if (outputChanSpec==null) {
                        String chanSpec = chanSpec1.replaceAll("r", "")+"r";
                        outputChannelList.addAll(ChanSpecUtils.convertChanSpecToList(chanSpec));
                    }
                    else {
                        outputChannelList.addAll(ChanSpecUtils.convertChanSpecToList(outputChanSpec));
                    }
                }
            }

            contextLogger.info("Unmerged input channel order: "+unmergedChannelList);
            contextLogger.info("Input channel order: "+inputChannelList);
            contextLogger.info("Output channel order: "+outputChannelList);

            List<Integer> channelMapping = new ArrayList<Integer>();
            for(Iterator<String> iterator = outputChannelList.iterator(); iterator.hasNext(); ) {
            	String outputTag = iterator.next();
                int originalIndex = unmergedChannelList.indexOf(outputTag);
                if (originalIndex<0) {
                	contextLogger.warn("Cannot find output channel in the input LSMs: "+outputTag);
                	iterator.remove();
                }
                else {
                	channelMapping.add(originalIndex);
                }
            }
            
            String channelMappingStr = Task.csvStringFromCollection(channelMapping);
            
            StringBuilder signalChannels = new StringBuilder();            
            StringBuilder referenceChannels = new StringBuilder();
            StringBuilder chanSpec = new StringBuilder();
            
            int index = 0;
            for(String tag : outputChannelList) {
                if (tag.equals("reference") || tag.matches("r\\d+")) {
                    if (referenceChannels.length() > 0) {
                        throw new MetadataException("More than one reference channel detected: "+referenceChannels+" "+index);
                    }
                    referenceChannels.append(index);
                    chanSpec.append("r");
                }
                else {
                    if (signalChannels.length() > 0) {
                        signalChannels.append(" ");
                    }
                    signalChannels.append(index);
                    chanSpec.append("s");
                }
                index++;
            }
            

            if (consensusChannelMapping==null) {
                consensusChannelMapping = channelMappingStr;
            }
            else if (!consensusChannelMapping.equals(channelMappingStr)) {
                throw new MetadataConsensusException("No channel mapping consensus among tiles ("+consensusChannelMapping+"!="+channelMappingStr+")");
            }
            
            if (consensusReferenceChannels==null) {
                consensusReferenceChannels = referenceChannels.toString();
            }
            else if (!consensusReferenceChannels.equals(referenceChannels.toString())) {
                throw new MetadataConsensusException("No reference channel consensus among tiles ("+consensusReferenceChannels+"!="+referenceChannels+")");
            }
                
            if (consensusSignalChannels==null) {
                consensusSignalChannels = signalChannels.toString();
            }
            else if (!consensusSignalChannels.equals(signalChannels.toString())) {
                throw new MetadataConsensusException("No signal channel consensus among tiles ("+consensusSignalChannels+"!="+signalChannels+")");
            }

            if (consensusChanSpec==null) {
                consensusChanSpec = chanSpec.toString();
            }
            else if (!consensusChanSpec.equals(chanSpec.toString())) {
                throw new MetadataConsensusException("No channel specification consensus among tiles ("+consensusChanSpec+"!="+chanSpec+")");
            }
            
            String mapping = generateChannelMapping(inputChannelList, outputChannelList);
            // TODO: Make sure we're actually doing something. If the file is merged and the channel mapping is 1-to-1, then there's no point in running.
            // Right now this doesn't work because the SubmitDrmaaJobService doesn't support aborted runs.  
            //if (!merged || !mapping.replaceAll("([0-9]),\\1,?", "").equals("")) { 
            	writeInstanceFiles(mergedLsmPair, mapping, configIndex++);
            //}
        }
        
        createShellScript(writer);
        setJobIncrementStop(configIndex-1);
        
        data.putItem("LSM_CHANNEL_MAPPING", consensusChannelMapping);
        data.putItem("CHANNEL_SPEC", consensusChanSpec);
        data.putItem("SIGNAL_CHANNELS", consensusSignalChannels);
        data.putItem("REFERENCE_CHANNEL", consensusReferenceChannels);
    }

    private void writeInstanceFiles(MergedLsmPair mergedLsmPair, String mapping, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
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
            fw.write((randomPort+configIndex) + "\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }
    
    private int getRefIndex(LSMMetadata lsmMetadata) {
        int i = 0;
        int refIndex = 0;
        for(Channel channel : lsmMetadata.getChannels()) {
            if ("#FFFFFF".equalsIgnoreCase(channel.getColor())) {
                refIndex = i;
            }
            i++;
        }
        return refIndex;
    }
    
    private void collectDyes(LSMMetadata lsmMetadata, Collection<String> referenceDyes, Collection<String> signalDyes, Collection<String> allDyes) throws LSMMetadataException {

        for(Channel channel : lsmMetadata.getChannels()) {
            DetectionChannel detection = lsmMetadata.getDetectionChannel(channel);
            if (detection!=null) {
                String dye = detection.getDyeName();
                contextLogger.debug("  Considering dye: "+dye);
                if (merged && referenceDyes.contains(dye)) {
                    contextLogger.debug("    This is a reference dye");
                    // If this is a merged pair, treat the reference dye differently from the rest
                    if (referenceDye==null) {
                        referenceDye = dye;    
                    }
                    else if (!referenceDye.equals(dye)) {
                        throw new LSMMetadataException("Multiple reference dyes detected in a single image ("+referenceDye+"!, "+dye+")");
                    }
                }
                else {
                    signalDyes.add(dye);    
                }
                allDyes.add(dye);
            }
        }
    }
    
    private Collection<String> convertDyesToTags(Collection<String> dyes, Map<String,String> dyeToTagMap) {
        List<String> tags = new ArrayList<String>();
        for(String dye : dyes) {
            String tag = dyeToTagMap.get(dye);
            if (tag!=null) {
                tags.add(tag);
            }
        }
        return tags;
    }
    
    private void createShellScript(FileWriter writer) throws Exception {
        StringBuilder script = new StringBuilder();
        script.append("read INPUT_FILENAME\n");
        script.append("read OUTPUT_FILENAME\n");
        script.append("read CHANNEL_MAPPING\n");
        script.append("read DISPLAY_PORT\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix("$DISPLAY_PORT"));
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedMapChannelPipelineCommand("$INPUT_FILENAME", "$OUTPUT_FILENAME", "$CHANNEL_MAPPING"));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }
    
    private String generateChannelMapping(List<String> inputChannelList, List<String> outputChannelList) throws Exception {

        contextLogger.debug("Input channels: "+inputChannelList);
        contextLogger.debug("Output channels: "+outputChannelList);
        
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
                throw new LSMMetadataException("No such tag in source image: "+tag);
            }
            if (targetIndex>0) mapChannelString.append(",");
            mapChannelString.append(sourceIndex).append(",").append(targetIndex);
            targetIndex++;
        }

        contextLogger.info("Channel mapping string: "+mapChannelString);
        return mapChannelString.toString();
    }
    
    private void populateMaps() throws Exception {

		List<Entity> tileEntities = entityBean.getEntitiesById(sampleArea.getTileIds());
        for(Entity tile : tileEntities) {
        	
        	entityLoader.populateChildren(tile);
            contextLogger.info("Populating maps for tile: "+tile.getName());
            
            for(Entity lsmStack : EntityUtils.getChildrenOfType(tile, EntityConstants.TYPE_LSM_STACK)) {
                
                // The actual filename of the LSM we're dealing with is not compressed
                String lsmFilename = ArchiveUtils.getDecompressedFilepath(lsmStack.getName());
                
                contextLogger.info("Populating maps for image: "+lsmFilename);
                lsmEntityMap.put(lsmFilename, lsmStack);
                
                File jsonFile = new File(metadataFileNode.getDirectoryPath(), lsmFilename+".json");
                
                if (!jsonFile.exists()) {
                	String jsonFilepath = findExistingJson(lsmFilename);
                	if (jsonFilepath!=null) {
                		jsonFile = new File(jsonFilepath);
                	}
                	else {
                		throw new LSMMetadataException("Could not find existing JSON metadata for "+lsmFilename);
                	}
                }
                
                try {
                    LSMMetadata metadata = LSMMetadata.fromFile(jsonFile);
                    contextLogger.info("Parsed metadata from: "+jsonFile);
                    lsmMetadataMap.put(lsmFilename, metadata);
                }
                catch (Exception e) {
                    throw new LSMMetadataException("Error parsing LSM metadata file: "+jsonFile,e);
                }
            }
        }
    }

    /**
     * Find an existing JSON metadata file for the given LSM. 
     * TODO: in the future, the JSON metadata filepath should be directly accessible from the LSM, so this code could be much simplified.
     */
	private String findExistingJson(final String lsmName) throws Exception {
		final StringHolder stringHolder = new StringHolder();
        EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sampleEntity)
                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
                .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
                .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA).first()
                .childrenOfType(EntityConstants.TYPE_TEXT_FILE)
                .run(new EntityVisitor() {
            public void visit(Entity textFile) throws Exception {
                if (textFile.getName().startsWith(lsmName) && textFile.getName().endsWith(".json")) {
                	stringHolder.s = textFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                }
            }
        });
        return stringHolder.s;
	}
	
	private class StringHolder {
		String s = null;
	}
}
