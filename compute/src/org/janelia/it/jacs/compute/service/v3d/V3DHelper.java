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

    public static String getV3DGridCommandPrefix() {
        int randomPort = ((int)(998.0 * Math.random()) + 1);
        return getV3DGridCommandPrefix(randomPort+"");
    }
    
    public static String getV3DGridCommandPrefix(String displayPort) {
    	StringBuffer prefix = new StringBuffer();
    	prefix.append("set -o errexit\n");
        // todo This is a little sketchy.  Get V3D to run headless for plug-ins!!!
    	prefix.append("/usr/bin/Xvfb :"+displayPort+" -screen 0 1x1x24 -sp /usr/lib64/xserver/SecurityPolicy -fp /usr/share/X11/fonts/misc &\n");
    	prefix.append("MYPID=$!\n");
    	prefix.append("export DISPLAY=\"localhost:"+displayPort+".0\"\n");
    	return prefix.toString();
    }

    public static String getV3DGridCommandSuffix() {
        return "kill -9 $MYPID";
    }

    public static String getFormattedMIPCommand(String inputFilepath, String outputFilepath, boolean flipy) throws ServiceException {
    	String cmd = V3D_BASE_CMD+" -cmd image-loader -mip "+inputFilepath+" "+outputFilepath;
    	if (flipy) cmd += " -flipy";
    	return cmd+" ;";
    }
    
    public static String getFormattedPBDCommand(String inputFilepath, String outputFilepath) throws ServiceException {
    	return V3D_BASE_CMD+" -cmd image-loader -convert "+inputFilepath+" "+outputFilepath+" ;";
    }
}
