package org.janelia.it.jacs.compute.service.v3d;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 10/18/11
 * Time: 11:01 AM
 */
public class V3DHelper {

    protected static final String V3D_BASE_CMD = SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("V3D.CMD");

    public static String getFormattedMergeCommand(String inputFilePath1, String inputFilePath2, String outputFilePath) throws Exception {
        try {
            File inputFile1 = new File(inputFilePath1);
            File inputFile2 = new File(inputFilePath2);
            File outputFile = new File(outputFilePath);

            // Check for the output file location
            if (!outputFile.getParentFile().exists()) {
                FileUtil.ensureDirExists(outputFile.getParentFile().getAbsolutePath());
            }

            if (inputFile1.exists()&&inputFile2.exists()&&outputFile.getParentFile().exists()) {
                return V3D_BASE_CMD+" -x libblend_multiscanstacks.so -f multiscanblend -p \"#k 1\" -i" + inputFilePath1
                        + " " +inputFilePath2+ " -o" + outputFilePath;
            }
            else {
                throw new Exception("Cannot format ");
            }
        }
        catch (Exception e) {
            throw new ServiceException("Cannot format the command for the V3D Merge Piece");
        }
    }

    public static String getFormattedStitcherCommand(String inputDirectoryPath) throws ServiceException {
        File inputDrectory = new File(inputDirectoryPath);

        if (inputDrectory.exists()) {
            return V3D_BASE_CMD+" -x imageStitch.so -f v3dstitch -i '"+inputDirectoryPath+"' -p \"#c 4 #si 0\";";
        }
        else {
            throw new ServiceException("Cannot format the command for the V3D Stitcher Piece");
        }
    }

    /**
     * Why does this look like it is actually stitching the files?  Why does the blender need a directory of images?
     * SHouldn't there only be one image by now?
     */
    public static String getFormattedBlendCommand(String inputDirectoryPath, String outputFilePath) throws ServiceException {
        File inputDirectory = new File(inputDirectoryPath);

        if (inputDirectory.exists()) {
            return V3D_BASE_CMD+" -x ifusion.so -f iblender -i '"+inputDirectoryPath+"' -o "+outputFilePath+" -p \"#s 1\"";
        }
        else {
            throw new ServiceException("Cannot format the command for the V3D Blender Command");
        }
    }

    public static String getV3DGridCommandPrefix() {
        // todo This is a little sketchy.  Get V3D to run headless for plug-ins!!!
        int randomPort = ((int)(998.0 * Math.random()) + 1);
        return "/usr/bin/Xvfb :910 -screen 0 1x1x24 -sp\n" +
               "/usr/lib64/xserver/SecurityPolicy -fp /usr/share/X11/fonts/misc &\n" +
               "MYPID=$! export DISPLAY=\"localhost:"+randomPort+"\"\n";
    }

    public static String getV3DGridCommandSuffix() {
        return "kill -9 $MYPID";
    }
}
