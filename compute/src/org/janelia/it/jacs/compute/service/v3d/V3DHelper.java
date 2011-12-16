package org.janelia.it.jacs.compute.service.v3d;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 10/18/11
 * Time: 11:01 AM
 */
public class V3DHelper {
	
	protected static final int STARTING_DISPLAY_PORT = 966;
	
    protected static final String V3D_BASE_CMD = "export LD_LIBRARY_PATH="+
    		SystemConfigurationProperties.getString("V3D.LDLibraryPath")+":$LD_LIBRARY_PATH\n"+
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("V3D.CMD");

    public static String getFormattedMergeCommand(String inputFilePath1, String inputFilePath2, String outputFilePath) throws Exception {
        try {
            return V3D_BASE_CMD+" -x libblend_multiscanstacks.so -f multiscanblend -p \"#k 1\" -i \"" + inputFilePath1
                    + "\" \"" +inputFilePath2+ "\" -o \"" + outputFilePath+"\"";
        }
        catch (Exception e) {
            throw new ServiceException("Cannot format the command for V3D Merge",e);
        }
    }

    public static String getFormattedStitcherCommand(String inputDirectoryPath) throws ServiceException {
        return V3D_BASE_CMD+" -x imageStitch.so -f v3dstitch -i \""+inputDirectoryPath+"\" -p \"#c 4 #si 0\";";
    }

    /**
     * Why does this look like it is actually stitching the files?  Why does the blender need a directory of images?
     * SHouldn't there only be one image by now?
     */
    public static String getFormattedBlendCommand(String inputDirectoryPath, String outputFilePath) throws ServiceException {
        return V3D_BASE_CMD+" -x ifusion.so -f iblender -i \""+inputDirectoryPath+"\" -o \""+outputFilePath+"\" -p \"#s 1\"";
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
    
    public static String getV3DGridCommandPrefix() {
        return getV3DGridCommandPrefix(getRandomPort()+"");
    }
    
    public static String getV3DGridCommandPrefix(String displayPort) {
    	StringBuffer prefix = new StringBuffer();
    	
    	// Exit the entire script if anything returns a non-zero exit code
    	prefix.append("set -o errexit\n");
    	
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

    public static String getHeadlessGridCommandSuffix() {
        return "";
    }
    
    public static String getV3DGridCommandSuffix() {
    	// Kill the Xvfb
        return "kill $MYPID";
    }

    public static String getFormattedMIPCommand(String inputFilepath, String outputFilepath, boolean flipy) throws ServiceException {
    	String cmd = V3D_BASE_CMD+" -cmd image-loader -mip "+inputFilepath+" "+outputFilepath;
    	
    	// Flip the Y axis (needed for historical reasons)
    	if (flipy) cmd += " -flipy";
    	
    	return cmd+" ;";
    }

    public static String getMapChannelCommand(String inputFilepath, String outputFilepath, String mapchannelString) throws ServiceException {
        String cmd = V3D_BASE_CMD+" -cmd image-loader -mapchannels "+inputFilepath+" "+outputFilepath+" "+mapchannelString;
        return cmd+" ;";
    }
    
    public static String getFormattedConvertCommand(String inputFilepath, String outputFilepath) throws ServiceException {
    	return getFormattedConvertCommand(inputFilepath, outputFilepath, "");
    }

    public static String getFormattedConvertCommand(String inputFilepath, String outputFilepath, String saveTo8bit) throws ServiceException {
    	return V3D_BASE_CMD+" -cmd image-loader -convert"+saveTo8bit+" "+inputFilepath+" "+outputFilepath+" ;";
    }
    
    public static int getRandomPort() {
    	return getRandomPort(STARTING_DISPLAY_PORT);
    }
    
    public static int getRandomPort(int startDisplayPort) {
       return ((int)(100.0 * Math.random()) + startDisplayPort);
    }

    public static String getV3dLibrarySetupCmd() {
        return "export LD_LIBRARY_PATH=" + SystemConfigurationProperties.getString("V3D.LDLibraryPath") + ":$LD_LIBRARY_PATH";
    }

    public static String getV3dExecutableCmd() {
        return SystemConfigurationProperties.getString("Executables.ModuleBase") + SystemConfigurationProperties.getString("V3D.CMD");
    }

}
