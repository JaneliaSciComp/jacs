package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 10/18/11
 * Time: 11:01 AM
 */
public class Vaa3DHelper {

//	buf.append("INPUT_FILE="+inputFilepath);
//	buf.append("OUTPUT_FILE="+outputFilepath);
//	
//	buf.append("EXT=${INPUT_FILE#*.}\n");
//	buf.append("if [ $EXT == \"v3dpbd\" ]; then\n");
//	buf.append("    PBD_INPUT_FILE=$INPUT_FILE");
//	buf.append("    INPUT_FILE_STUB=`basename $PBD_INPUT_FILE`");
//	buf.append("    INPUT_FILE=\"${INPUT_FILE_STUB%.*}.v3draw\"");
//	buf.append("    "+VAA3D_BASE_CMD+" -cmd image-loader -convert \"$PBD_INPUT_FILE\" \"$INPUT_FILE\"");
//	buf.append("fi");	

//	buf.append("EXT=${OUTPUT_FILE#*.}\n");
//	buf.append("if [ $EXT == \"v3draw\" ]; then\n");
//	buf.append("    mv output.v3draw $OUTPUT_FILE");
//	buf.append("else");
//	buf.append("    "+VAA3D_BASE_CMD+" -cmd image-loader -convert output.v3draw $OUTPUT_FILE");
//	buf.append("fi");	
	
    protected static final int STARTING_DISPLAY_PORT = 966;

    protected static final String VAA3D_BASE_CMD = "export LD_LIBRARY_PATH="+
            SystemConfigurationProperties.getString("VAA3D.LDLibraryPath")+":$LD_LIBRARY_PATH\n"+
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("VAA3D.CMD");

    protected static final String MERGE_PIPELINE_CMD =
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
                    SystemConfigurationProperties.getString("MergePipeline.ScriptPath");

    protected static final String INTERSECTION_CMD =
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
                    SystemConfigurationProperties.getString("Intersection.ScriptPath");
    
    public static String getFormattedMergePipelineCommand(String inputFilePath1, String inputFilePath2, String outputFilePath) {
        return "sh "+MERGE_PIPELINE_CMD+" \""+inputFilePath1+"\" \""+inputFilePath2+"\" \""+outputFilePath+"\"";
    }

    public static String getFormattedMergeCommand(String inputFilePath1, String inputFilePath2, String outputFilePath) {
        return VAA3D_BASE_CMD +" -x libblend_multiscanstacks.so -f multiscanblend -p \"#k 1\" -i \"" + inputFilePath1
                + "\" \"" +inputFilePath2+ "\" -o \"" + outputFilePath+"\"";
    }

    public static String getFormattedGrouperCommand(int referenceChannelIndex, String inputDirectoryPath, String outputFilePath) {
        return VAA3D_BASE_CMD +" -x imageStitch.so -f istitch-grouping -p \"#c "+referenceChannelIndex+"\" -i \""+inputDirectoryPath+"\" -o \""+outputFilePath+"\";";
    }

    public static String getFormattedStitcherCommand(int referenceChannelIndex, String inputDirectoryPath) {
        return VAA3D_BASE_CMD +" -x imageStitch.so -f v3dstitch -i \""+inputDirectoryPath+"\" -p \"#c "+referenceChannelIndex+" #si 0\";";
    }

    public static String getFormattedBlendCommand(String inputDirectoryPath, String outputFilePath) {
    	StringBuffer buf = new StringBuffer();
    	buf.append("OUTPUT_FILE="+outputFilePath+"\n");
    	buf.append(VAA3D_BASE_CMD +" -x ifusion.so -f iblender -i \""+inputDirectoryPath+"\" -o \"output.v3draw\" -p \"#s 1\"\n");
    	buf.append("EXT=${OUTPUT_FILE#*.}\n");
    	buf.append("if [ $EXT == \"v3draw\" ]; then\n");
    	buf.append("    mv output.v3draw $OUTPUT_FILE\n");
    	buf.append("else\n");
    	buf.append("    "+VAA3D_BASE_CMD+" -cmd image-loader -convert output.v3draw $OUTPUT_FILE\n");
    	buf.append("fi\n");	
    	return buf.toString();
    }

    /**
     * For V3D plugins that are truly headless.
     * @return
     */
    public static String getHeadlessGridCommandPrefix() {
        StringBuffer prefix = new StringBuffer();
        prefix.append("set -o errexit\n");
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
        prefix.append("PORT="+displayPort+"\n");
        prefix.append("while (test -f \"/tmp/.X${PORT}-lock\") || (netstat -atwn | grep \"^.*:${PORT}.*:\\*\\s*LISTEN\\s*$\")\n");
        prefix.append("do PORT=$(( ${PORT} + 1 ))\n");
        prefix.append("done\n");

        // Run Xvfb (virtual framebuffer) on the chosen port
        prefix.append("/usr/bin/Xvfb :${PORT} -screen 0 1x1x24 -sp /usr/lib64/xserver/SecurityPolicy -fp /usr/share/X11/fonts/misc &\n");

        // Save the PID so that we can kill it when we're done
        prefix.append("MYPID=$!\n");
        prefix.append("export DISPLAY=\"localhost:${PORT}.0\"\n");

        return prefix.toString();
    }

    public static String getVaa3DGridCommandSuffix() {
        // Kill the Xvfb
        return "kill $MYPID";
    }

    public static String getFormattedMIPCommand(String inputFilepath, String outputFilepath, String extraOptions) throws ServiceException {
        String cmd = VAA3D_BASE_CMD +" -cmd image-loader -mip \""+inputFilepath+"\" \""+outputFilepath+"\" "+extraOptions;
        return cmd+" ;";
    }

    public static String getMapChannelCommand(String inputFilepath, String outputFilepath, String mapchannelString) throws ServiceException {
        String cmd = VAA3D_BASE_CMD +" -cmd image-loader -mapchannels \""+inputFilepath+"\" \""+outputFilepath+"\" "+mapchannelString;
        return cmd+" ;";
    }

    public static String getPatternAnnotationCommand(String inputStackFilepath, int patternChannel, String outputPrefix, String resourceDirPath, String outputDirPath) throws ServiceException {
        String cmd = VAA3D_BASE_CMD +" -cmd screen-pattern-annotator -input \""+inputStackFilepath+"\" -pattern_channel "+patternChannel+" -prefix "+outputPrefix+" -resourceDir "+resourceDirPath+" -outputDir \""+outputDirPath+"\"";
        return cmd+" ;";
    }

    public static String getMaskAnnotationCommand(String inputStackFilepath, int patternChannel, String outputPrefix, String resourceDirPath, String outputDirPath) throws ServiceException {
        String cmd = VAA3D_BASE_CMD +" -cmd screen-pattern-annotator -input \""+inputStackFilepath+"\" -pattern_channel "+patternChannel+" -prefix "+outputPrefix+" -resourceDir "+resourceDirPath+" -outputDir \""+outputDirPath+"\"";
        return cmd+" ;";
    }

    public static String getMaskGuideCommand(String inputNameIndexFile, String inputRGBFile, String outputMaskGuideDirectory) throws ServiceException {
        String cmd = VAA3D_BASE_CMD +" -cmd screen-pattern-annotator -inputNameIndexFile \""+inputNameIndexFile+"\" -inputRGBFile "+inputRGBFile+" -outputMaskDirectory "+outputMaskGuideDirectory;
        return cmd+" ;";
    }

    public static String getFormattedConvertCommand(String inputFilepath, String outputFilepath) throws ServiceException {
        return getFormattedConvertCommand(inputFilepath, outputFilepath, "");
    }

    public static String getFormattedConvertCommand(String inputFilepath, String outputFilepath, String saveTo8bit) throws ServiceException {
    	return VAA3D_BASE_CMD +" -cmd image-loader -convert"+saveTo8bit+" \""+inputFilepath+"\" \""+outputFilepath+"\" ;";
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
    
    public static int getRandomPort() {
        return getRandomPort(STARTING_DISPLAY_PORT);
    }

    public static int getRandomPort(int startDisplayPort) {
        return ((int)(100.0 * Math.random()) + startDisplayPort);
    }

    public static String getVaa3dLibrarySetupCmd() {
        return "export LD_LIBRARY_PATH=" + SystemConfigurationProperties.getString("VAA3D.LDLibraryPath") + ":$LD_LIBRARY_PATH";
    }

    public static String getVaa3dExecutableCmd() {
        return SystemConfigurationProperties.getString("Executables.ModuleBase") + SystemConfigurationProperties.getString("VAA3D.CMD");
    }

}
