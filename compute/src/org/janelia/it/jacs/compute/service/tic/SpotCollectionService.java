package org.janelia.it.jacs.compute.service.tic;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.tic.TICResultNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * @author Todd Safford
 */
public class SpotCollectionService implements IService {

    public static final String SPOT_FILE_NAME = "spotFiles.txt";
    public static final int POSX_INDEX=1;
    public static final int POSY_INDEX=2;
    public static final int POSZ_INDEX=3;
    public static final int SIGX_INDEX=7;
    public static final int SIGY_INDEX=8;
    public static final int SIGZ_INDEX=9;
    private Logger _logger;
    private Task task;
    private TICResultNode resultFileNode;
    private String sessionName;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            _logger = ProcessDataHelper.getLoggerForTask(processData, SpotCollectionService.class);
            this.task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            resultFileNode = (TICResultNode)ProcessDataHelper.getResultFileNode(processData);

            // Find all the spot files
            SystemCall call = new SystemCall();
            call.emulateCommandLine("find "+resultFileNode.getDirectoryPath()+" -name *all_spots* > "+resultFileNode.getDirectoryPath()+ File.separator+SPOT_FILE_NAME, true);
            TreeMap<Integer, String> spotFiles = new TreeMap<Integer, String>();
            Scanner scanner = new Scanner(new File(resultFileNode.getDirectoryPath()+File.separator+SPOT_FILE_NAME));
            String targetName="";
            try {
                while(scanner.hasNextLine()) {
                    String tmpInputFile = scanner.nextLine();
                    // Cut off the spot text and the path information
                    String targetPrefix = tmpInputFile.substring(tmpInputFile.lastIndexOf(File.separator)+1, tmpInputFile.lastIndexOf(".tif"));
                    targetPrefix = targetPrefix.substring(0,targetPrefix.lastIndexOf("_"));
                    targetName = targetPrefix.substring(0,targetPrefix.lastIndexOf("_"));
                    String[] pieces = targetPrefix.split("_");
                    spotFiles.put(Integer.valueOf(pieces[(pieces.length-1)]),tmpInputFile.trim());
                }
            }
            finally {
                scanner.close();
            }

            // Write a final spot file
            FileWriter writer=new FileWriter(resultFileNode.getDirectoryPath()+File.separator+"FISH_QUANT_"+targetName+"final_spots.txt");
            Scanner spotScanner=null;
            try {
                int maxFramesPerFile = 0;
                boolean addedHeader = false;
                for (Integer integer : spotFiles.keySet()) {
                    spotScanner = new Scanner(new File(spotFiles.get(integer)));
                    boolean hitFileFlag = false;
                    while(spotScanner.hasNextLine()) {
                        String tmpLine = spotScanner.nextLine();
                        if (!addedHeader || hitFileFlag) {
                            if (hitFileFlag) {
                                String index = tmpLine.substring(0,tmpLine.indexOf("\t"));
                                maxFramesPerFile = Integer.valueOf(index)+1;
                                // Make sure the frame is correct
                                tmpLine = adjustFrameColumn(tmpLine, maxFramesPerFile, integer);
                            }
                            if (null!=tmpLine) {writer.write(tmpLine+"\n");}
                        }
                        if (!hitFileFlag) {
                            hitFileFlag = tmpLine.startsWith("File#");
                        }
                    }
                    if (!addedHeader) {addedHeader=true;}
                }
                _logger.debug("Max frames per file is "+maxFramesPerFile);
            }
            catch (Exception e) {
                throw new ServiceException(e);
            }
            finally {
                if (null!=writer) {writer.close();}
                if (null!=spotScanner) {spotScanner.close();}
            }
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private String adjustFrameColumn(String tmpLine, int maxFramesPerFile, Integer index) {
        String tmpFrame = tmpLine.substring(0,tmpLine.indexOf("\t"));
        String[] pieces = tmpLine.split("\t");
        if (valueFails(pieces[POSX_INDEX],-120,22000)) {
            _logger.debug("Throwing out data that exceeds X threshold: "+tmpLine);
            return null;
        }
        if (valueFails(pieces[POSY_INDEX],-120,22000)) {
            _logger.debug("Throwing out data that exceeds Y threshold: "+tmpLine);
            return null;
        }
        if (valueFails(pieces[POSZ_INDEX],-500,5000))  {
            _logger.debug("Throwing out data that exceeds Z threshold: "+tmpLine);
            return null;
        }
        if (valueFails(pieces[SIGX_INDEX],30,600)) {
            _logger.debug("Throwing out data that exceeds Sig X threshold: "+tmpLine);
            return null;
        }
        if (valueFails(pieces[SIGY_INDEX],30,600)) {
            _logger.debug("Throwing out data that exceeds Sig Y threshold: "+tmpLine);
            return null;
        }
        if (valueFails(pieces[SIGZ_INDEX],50,800)) {
            _logger.debug("Throwing out data that exceeds Sig Z threshold: "+tmpLine);
            return null;
        }

        try {
            Integer tmpFrameValue = Integer.valueOf(tmpFrame);
            tmpFrameValue = (index*maxFramesPerFile)+tmpFrameValue;
            tmpLine = tmpLine.replaceFirst(tmpFrame,tmpFrameValue.toString());
        }
        catch (Exception e) {
            // ignore errors here
        }
        return tmpLine;
    }

    private boolean valueFails(String columnValue, float minimumValue, float maximumValue) {
        Float testValue = Float.valueOf(columnValue);
        return (testValue < minimumValue || testValue > maximumValue);
    }

}