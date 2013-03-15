package org.janelia.it.jacs.compute.service.vaa3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * Convert a single tile to a sample image. Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   BULK_MERGE_PARAMETERS - a list of MergedLsmPair (may only contain MergedLsmPairs with a single image each)
 *   IMAGE_CHANNEL_SPEC - channel specification for the original input image
 *   SAMPLE_CHANNEL_SPEC - channel specification for the resulting sample image 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DConvertToSampleImageService extends Vaa3DBulkMergeService {

	private static final int START_DISPLAY_PORT = 890;
    private static final String CONFIG_PREFIX = "convertConfiguration.";
    private static int randomPort;
    private String imageChanSpec;
    private String sampleChanSpec;
    
    @Override
    protected String getGridServicePrefixName() {
        return "convert";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        imageChanSpec = (String)processData.getItem("IMAGE_CHANNEL_SPEC");
        if (imageChanSpec ==null) {
            throw new ServiceException("Input parameter IMAGE_CHANNEL_SPEC may not be null");
        }
        
        sampleChanSpec = (String)processData.getItem("SAMPLE_CHANNEL_SPEC");
        if (sampleChanSpec==null) {
            throw new ServiceException("Input parameter SAMPLE_CHANNEL_SPEC may not be null");
        }

        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
        if (bulkMergeParamObj==null) {
        	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
        }

        if (bulkMergeParamObj instanceof List) {
        	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;

            int configIndex = 1;
            randomPort = Vaa3DHelper.getRandomPort(START_DISPLAY_PORT);
            
        	for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
                if (mergedLsmPair.getFilepath2()!=null) {
                    throw new IllegalStateException("Tiles with more than one image cannot make use of this service, they must be merged instead.");
                }
                writeInstanceFiles(mergedLsmPair, configIndex++);        
        	}
            
        	createShellScript(writer);
            setJobIncrementStop(configIndex-1);
        }
        else {
        	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS must be an ArrayList<MergedLsmPair>");
        }
    }

    private void writeInstanceFiles(MergedLsmPair mergedLsmPair, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            fw.write(mergedLsmPair.getLsmFilepath1() + "\n");
            fw.write(mergedLsmPair.getMergedFilepath() + "\n");
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
        script.append("read LSM_FILENAME_1\n");
        script.append("read MERGED_FILENAME\n");
        script.append("read DISPLAY_PORT\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix("$DISPLAY_PORT"));
        script.append("\n");
        
        if (imageChanSpec.equals(sampleChanSpec)) {
            script.append(Vaa3DHelper.getFormattedConvertCommand("$LSM_FILENAME_1", "$MERGED_FILENAME"));    
        }
        else {
            List<Integer> refSourceIndexes = new ArrayList<Integer>();
            List<Integer> sigSourceIndexes = new ArrayList<Integer>();
            for(int sourceIndex=0; sourceIndex<imageChanSpec.length(); sourceIndex++) {
                char imageChanCode = imageChanSpec.charAt(sourceIndex);
                switch (imageChanCode) {
                case 's':
                    sigSourceIndexes.add(sourceIndex);
                    break;
                case 'r':
                    refSourceIndexes.add(sourceIndex);
                    break;
                default:
                    new IllegalStateException("Unknown channel code: "+imageChanCode);
                }
            }
            
            StringBuilder mapChannelString = new StringBuilder();
            for(int targetIndex=0; targetIndex<sampleChanSpec.length(); targetIndex++) {
                char sampleChanCode = sampleChanSpec.charAt(targetIndex);
                switch (sampleChanCode) {
                case 's':
                    if (refSourceIndexes.isEmpty()) throw new IllegalStateException("Not enough signal channels in source image");
                    if (mapChannelString.length()>0) mapChannelString.append(",");
                    mapChannelString.append(sigSourceIndexes.remove(0)+","+targetIndex);
                    break;
                case 'r':
                    if (refSourceIndexes.isEmpty()) throw new IllegalStateException("Not enough reference channels in source image");
                    if (mapChannelString.length()>0) mapChannelString.append(",");
                    mapChannelString.append(refSourceIndexes.remove(0)+","+targetIndex);
                    break;
                default:
                    throw new IllegalStateException("Unknown channel code: "+sampleChanCode);
                }
            }
                        
            script.append(Vaa3DHelper.getMapChannelCommand("$LSM_FILENAME_1", "$MERGED_FILENAME", "\"" + mapChannelString + "\""));
        }
        
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }
}
