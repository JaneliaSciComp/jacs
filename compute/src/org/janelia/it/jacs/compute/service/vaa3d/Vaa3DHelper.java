package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 10/18/11
 * Time: 11:01 AM
 */
public class Vaa3DHelper {

    protected static final int STARTING_DISPLAY_PORT = 5200;

    protected static final int XVFB_RETRIES = 10;
    
    protected static final String COPY_CMD = "cp";

    protected static final String VAA3D_LIBRARY_PATH = "export LD_LIBRARY_PATH="+
            SystemConfigurationProperties.getString("VAA3D.LDLibraryPath")+":$LD_LIBRARY_PATH";
    		
    protected static final String VAA3D_BASE_CMD = 
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("VAA3D.CMD");

    protected static final String MERGE_PIPELINE_CMD =
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
                    SystemConfigurationProperties.getString("MergePipeline.ScriptPath");

    protected static final String MAP_CHANNEL_PIPELINE_CMD =
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
                    SystemConfigurationProperties.getString("MapChannelPipeline.ScriptPath");
    
    protected static final String INTERSECTION_CMD =
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
                    SystemConfigurationProperties.getString("Intersection.ScriptPath");

    protected static final String SPLIT_CHANNELS_CMD =
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
                    SystemConfigurationProperties.getString("SplitChannels.ScriptPath");

    protected static final String FFMPEG_CMD =
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
                    SystemConfigurationProperties.getString("FFMPEG.Bin.Path");
    
    protected static final String SCRATCH_DIR =
            SystemConfigurationProperties.getString("computeserver.ClusterScratchDir");

    public static String getVaa3dLibrarySetupCmd() {
        return VAA3D_LIBRARY_PATH;
    }
    
    public static String getVaa3dExecutableCmd() {
        return VAA3D_BASE_CMD;
    }
    
    public static String getScratchDirCreationScript(String scratchDirVariableName) {
        StringBuffer buf = new StringBuffer();
        buf.append("export TMPDIR=\""+SCRATCH_DIR+"\"\n");
        buf.append(scratchDirVariableName+"=`mktemp -d`\n");
        return buf.toString();
    }

    public static String getScratchDirCleanupScript(String scratchDirVariableName) {
        StringBuffer buf = new StringBuffer();
        buf.append("rm -rf $"+scratchDirVariableName+"\n");
        return buf.toString();
    }
    
    public static String getFormattedMergePipelineCommand(String inputFilePath1, String inputFilePath2, String outputFilePath, String multiscanBlendVersion) {
        StringBuffer buf = new StringBuffer();
        buf.append("sh "+MERGE_PIPELINE_CMD);
        buf.append(" -o \""+outputFilePath+"\""); 
        if (!StringUtils.isEmpty(multiscanBlendVersion)) {
            buf.append(" -m \""+multiscanBlendVersion+"\""); 
        }
        buf.append(" \""+inputFilePath1+"\""); 
        buf.append(" \""+inputFilePath2+"\""); 
        return buf.toString();
    }

    public static String getFormattedMapChannelPipelineCommand(String inputFilePath, String outputFilePath, String channelMapping) {
        StringBuffer buf = new StringBuffer();
        buf.append("sh "+MAP_CHANNEL_PIPELINE_CMD);
        buf.append(" -i \""+inputFilePath+"\""); 
        buf.append(" -o \""+outputFilePath+"\""); 
        if (!StringUtils.isEmpty(channelMapping)) {
            buf.append(" -m \""+channelMapping+"\""); 
        }
        return buf.toString();
    }
    
    public static String getFormattedGrouperCommand(int referenceChannelIndex, String inputDirectoryPath, String outputFilePath) {
        return getVaa3dExecutableCmd() +" -x imageStitch.so -f istitch-grouping -p \"#c "+referenceChannelIndex+"\" -i \""+inputDirectoryPath+"\" -o \""+outputFilePath+"\";";
    }

    public static String getFormattedSimilarityCommand(String targetPath, String inputListPath, String outputFilePath) {
        return getVaa3dExecutableCmd() +" -cmd screen-pattern-annotator -targetStack \""+targetPath+"\" -subjectStackList \""+inputListPath+"\" -outputSimilarityList  \""+outputFilePath+"\";";
    }

    public static String getFormattedStitcherCommand(int referenceChannelIndex, String inputDirectoryPath) {
        return getVaa3dExecutableCmd() +" -x imageStitch.so -f v3dstitch -i \""+inputDirectoryPath+"\" -p \"#c "+referenceChannelIndex+" #si 0\";";
    }
    
    public static String getFormattedBlendCommand(String inputDirectoryPath, String outputFilePath) {
    	StringBuffer buf = new StringBuffer();
    	buf.append("OUTPUT_FILE="+outputFilePath+"\n");
    	buf.append(VAA3D_BASE_CMD +" -x ifusion.so -f iblender -i \""+inputDirectoryPath+"\" -o \"output.v3draw\" -p \"#s 1\"\n");
    	buf.append("EXT=${OUTPUT_FILE#*.}\n");
    	buf.append("if [ $EXT == \"v3draw\" ]; then\n");
    	buf.append("    mv output.v3draw $OUTPUT_FILE\n");
    	buf.append("else\n");
    	buf.append("    "+getVaa3dExecutableCmd()+" -cmd image-loader -convert output.v3draw $OUTPUT_FILE\n");
    	buf.append("    rm -f output.v3draw\n");
    	buf.append("fi\n");	
    	return buf.toString();
    }
    
    private static String getHostnameEcho() {
    	return "echo \"Running on \"`hostname`\n";
    }

    /**
     * For V3D plugins that are truly headless.
     * @return
     */
    public static String getVaa3dHeadlessGridCommandPrefix() {
        StringBuffer prefix = new StringBuffer();
        prefix.append(getHostnameEcho());
        prefix.append("set -o errexit\n\n");
        prefix.append(getVaa3dLibrarySetupCmd()).append("\n");
        return prefix.toString();
    }

    public static String getHeadlessGridCommandSuffix() {
        return "";
    }

    /**
     * For V3D plugins that need a virtual framebuffer because they are not headless.
     * @return
     */
    public static String getVaa3DGridCommandPrefix() {
        return getVaa3DGridCommandPrefix(getRandomPort() + "");
    }

    public static String getVaa3DGridCommandPrefix(String displayPort) {
        StringBuffer prefix = new StringBuffer();

        // Skip ports that are currently in use, or "locked"
        prefix.append(getHostnameEcho());
        prefix.append("echo \"Finding a port for Xvfb, starting at "+displayPort+"...\"\n");
        prefix.append("PORT="+displayPort+" COUNTER=0 RETRIES="+XVFB_RETRIES+"\n");
        
        // Clean up Xvfb on any exit
        prefix.append("function cleanXvfb {\n");
        prefix.append("    kill $MYPID\n");
        prefix.append("    rm -f /tmp/.X${PORT}-lock\n");
        prefix.append("    rm -f /tmp/.X11-unix/X${PORT}\n");
        prefix.append("    echo \"Cleaned up Xvfb\"\n");
        prefix.append("}\n");
        prefix.append("trap cleanXvfb EXIT\n");
        
        prefix.append("while [ \"$COUNTER\" -lt \"$RETRIES\" ]; do\n");
        prefix.append("    while (test -f \"/tmp/.X${PORT}-lock\") || (test -f \"/tmp/.X11-unix/X${PORT}\") || (netstat -atwn | grep \"^.*:${PORT}.*:\\*\\s*LISTEN\\s*$\")\n");
        prefix.append("        do PORT=$(( ${PORT} + 1 ))\n");
        prefix.append("    done\n");
        prefix.append("    echo \"Found the first free port: $PORT\"\n");
        
        // Run Xvfb (virtual framebuffer) on the chosen port
        prefix.append("    /usr/bin/Xvfb :${PORT} -screen 0 1x1x24 -fp /usr/share/X11/fonts/misc > Xvfb.${PORT}.log 2>&1 &\n");
        prefix.append("    echo \"Started Xvfb on port $PORT\"\n");
        
        // Save the PID so that we can kill it when we're done
        prefix.append("    MYPID=$!\n");
        prefix.append("    export DISPLAY=\"localhost:${PORT}.0\"\n");
        
        // Wait some time and check to make sure Xvfb is actually running, and retry if not. 
        prefix.append("    sleep 3\n");
        prefix.append("    if kill -0 $MYPID >/dev/null 2>&1; then\n");
        prefix.append("        echo \"Xvfb is running as $MYPID\"\n");
        prefix.append("        break\n");
        prefix.append("    else\n");
        prefix.append("        echo \"Xvfb died immediately, trying again...\"\n");
        prefix.append("        cleanXvfb\n");
        prefix.append("        PORT=$(( ${PORT} + 1 ))\n");
        prefix.append("    fi\n");
        
        prefix.append("    COUNTER=\"$(( $COUNTER + 1 ))\"\n");
        prefix.append("done\n\n");
        prefix.append(getVaa3dLibrarySetupCmd()).append("\n");

        return prefix.toString();
    }

    public static String getVaa3DGridCommandSuffix() {
        // No need to clean anything, because the cleanXvfb trap will clean it for us
        return "";
    }

    public static String getFormattedMIPCommand(String inputFilepath, String outputFilepath, String extraOptions) throws ServiceException {
        String cmd = getVaa3dExecutableCmd() +" -cmd image-loader -mip \""+inputFilepath+"\" \""+outputFilepath+"\" "+extraOptions;
        return cmd+" ;";
    }

    public static String getMapChannelCommand(String inputFilepath, String outputFilepath, String mapchannelString) throws ServiceException {
        String cmd = getVaa3dExecutableCmd() +" -cmd image-loader -mapchannels \""+inputFilepath+"\" \""+outputFilepath+"\" "+mapchannelString;
        return cmd+" ;";
    }

    public static String getPatternAnnotationCommand(String inputStackFilepath, int patternChannel, String outputPrefix, String resourceDirPath, String outputDirPath) throws ServiceException {
        String cmd = getVaa3dExecutableCmd() +" -cmd screen-pattern-annotator -input \""+inputStackFilepath+"\" -pattern_channel "+patternChannel+" -prefix "+outputPrefix+" -resourceDir "+resourceDirPath+" -outputDir \""+outputDirPath+"\"";
        return cmd+" ;";
    }

    public static String getMaskAnnotationCommand(String inputStackFilepath, int patternChannel, String outputPrefix, String resourceDirPath, String outputDirPath) throws ServiceException {
        String cmd = getVaa3dExecutableCmd() +" -cmd screen-pattern-annotator -input \""+inputStackFilepath+"\" -pattern_channel "+patternChannel+" -prefix "+outputPrefix+" -resourceDir "+resourceDirPath+" -outputDir \""+outputDirPath+"\"";
        return cmd+" ;";
    }

    public static String getMaskGuideCommand(String inputNameIndexFile, String inputRGBFile, String outputMaskGuideDirectory) throws ServiceException {
        String cmd = getVaa3dExecutableCmd() +" -cmd screen-pattern-annotator -inputNameIndexFile \""+inputNameIndexFile+"\" -inputRGBFile "+inputRGBFile+" -outputMaskDirectory "+outputMaskGuideDirectory;
        return cmd+" ;";
    }

    public static String getFormattedConvertCommand(String inputFilepath, String outputFilepath) throws ServiceException {
        return getFormattedConvertCommand(inputFilepath, outputFilepath, "");
    }

    public static String getFormattedConvertCommand(String inputFilepath, String outputFilepath, String saveTo8bit) throws ServiceException {
    	return getVaa3dExecutableCmd() +" -cmd image-loader -convert"+saveTo8bit+" \""+inputFilepath+"\" \""+outputFilepath+"\" ;";
    }

    public static String getFormattedCopyCommand(String inputFilepath, String outputFilepath) throws ServiceException {
        return COPY_CMD +" \""+inputFilepath+"\" \""+outputFilepath+"\" ;";
    }

    public static String getEnsureRawFunction() throws ServiceException {
    	StringBuilder sb = new StringBuilder();
    	sb.append("ensureRawFile()\n");
    	sb.append("{\n");
    	sb.append("    local _WORKING_DIR=\"$1\"\n");
    	sb.append("    local _FILE=\"$2\"\n");
    	sb.append("    local _RESULTVAR=\"$3\"\n");
    	sb.append("    local _EXT=${_FILE#*.}\n");
    	sb.append("    if [ \"$_EXT\" == \"v3dpbd\" ]; then\n");
    	sb.append("        local _PBD_FILE=$_FILE\n");
    	sb.append("        local _FILE_STUB=`basename $_PBD_FILE`\n");
    	sb.append("        _FILE=\"$_WORKING_DIR/${_FILE_STUB%.*}.v3draw\"\n");
    	sb.append("        echo \"Converting PBD to RAW format\"\n");
    	sb.append("        "+getVaa3dExecutableCmd()+" -cmd image-loader -convert \"$_PBD_FILE\" \"$_FILE\"\n");
    	sb.append("    fi\n");
    	sb.append("    eval $_RESULTVAR=\"'$_FILE'\"\n");
    	sb.append("}\n");
        return sb.toString();
    }

    public static String getEnsureRawCommand(String workDir, String inputFilepath, String outputVarName) throws ServiceException {
    	return "ensureRawFile \""+workDir+"\" \""+inputFilepath+"\" "+outputVarName;
    }
    
    /**
     * Compute the intersection of two images using one of the following methods to combine 2 pixel intensity values:
     * 0: minimum value 
     * 1: geometric mean 
     * 2: scaled product
     * @param inputFilepath1
     * @param inputFilepath2
     * @param outputFilepath
     * @param method
     * @return
     * @throws ServiceException
     */
    public static String getFormattedIntersectionCommand(String inputFilepath1, String inputFilepath2, String outputFilepath, String method, String kernelSize) throws ServiceException {
        return INTERSECTION_CMD +" \""+inputFilepath1+"\" \""+inputFilepath2+"\" \""+outputFilepath+"\" "+method+" "+kernelSize;
    }

    public static String getFormattedSplitChannelsCommand(String inputFilepath, String outputFilepath, String outputExtension) throws ServiceException {
        return SPLIT_CHANNELS_CMD +" \""+inputFilepath+"\" \""+outputFilepath+"\" "+outputExtension;
    }
    
    public static String getFormattedNeuronMergeCommand(String originalImageFilePath, String consolidatedSignalLabelIndexFilePath,
                                                        String commaSeparatedFragmentList, String newOutputMIPPath, String newOutputStackPath) throws ServiceException {
        return getVaa3dExecutableCmd() + " -cmd neuron-fragment-editor -sourceImage \""+originalImageFilePath+"\" -labelIndex \"" +consolidatedSignalLabelIndexFilePath+
                "\" -fragments "+commaSeparatedFragmentList+" -outputMip \""+newOutputMIPPath+"\" -outputStack \""+newOutputStackPath+"\" ;";
    }

    public static String getFormattedCellCounterCommand(String planPath, String convertedFilePath, String cellChannel, String bkgdChannel) {
        return getVaa3dExecutableCmd() + " -cmd cell-counter -cch "+cellChannel+" -bch "+bkgdChannel+" -plan "+planPath+" -i "+convertedFilePath;
    }

    public static String getFormattedMaskFromStackCommand(String refPath, String outputDir, String outputPrefix, String channel, String threshold) {
        return getVaa3dExecutableCmd() + " -cmd neuron-fragment-editor -mode mask-from-stack -sourceImage \""+refPath+"\" -channel "+channel+" -threshold "+threshold+" -outputDir \""+outputDir+"\" -outputPrefix "+outputPrefix;
    }
    
    public static String getFormattedMaskSearchCommand(String indexFilePath, String queryChannel, String matrix,
                                                       String maxHits, String skipZeroes, String outputFilePath) {
        String tmpString = getVaa3dExecutableCmd() + " -cmd volume-pattern-index -mode search -indexFile \""+indexFilePath+
                "\" -query $INPUT_FILE -outputFile \""+outputFilePath+"\" -queryChannel "+queryChannel;
        if (null!=matrix && !"".equals(matrix)) {
            tmpString+=" -matrix \""+matrix+"\"";
        }
        if (null!=maxHits&& !"".equals(maxHits)) {
            tmpString+=" -maxHits "+maxHits;
        }
        if (null!=skipZeroes&& !"".equals(skipZeroes) && Boolean.valueOf(skipZeroes)) {
            tmpString+=" -skipzeros";
        }
        return tmpString;
    }
    
    public static String getFormattedH264ConvertCommand(String inputFile, String outputFile, boolean truncateToEvenSize) {
    	String trunc = truncateToEvenSize? "-vf \"scale=trunc(iw/2)*2:trunc(ih/2)*2\" " : "";
    	return FFMPEG_CMD+" -y -r 7 -i \""+inputFile+"\" -vcodec libx264 -b:v 2000000 -preset slow -tune film -pix_fmt yuv420p "+trunc+" \""+outputFile+"\"";
    }
    
    public static int getRandomPort() {
        return getRandomPort(STARTING_DISPLAY_PORT);
    }

    public static int getRandomPort(int startDisplayPort) {
        return ((int)(100.0 * Math.random()) + startDisplayPort);
    }

}
