package org.janelia.it.jacs.compute.service.tic;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.tic.TicTask;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 4/10/12
 * Time: 2:20 PM
 * This class looks for all the tif files provided in the inputFile path
 */
public class TIFInputFileDiscoveryService implements IService {


    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            Task task = ProcessDataHelper.getTask(processData);
            String inputFilePath = task.getParameter(TicTask.PARAM_inputFile);
            List<String> tmpFiles = new ArrayList<String>();
            List<String> finalFileList = new ArrayList<String>();
            if (null!=inputFilePath) {
                tmpFiles = Task.listOfStringsFromCsvString(inputFilePath);
                for (String tmpFilePath : tmpFiles) {
                    File tmpFile = new File(tmpFilePath);
                    if (tmpFile.isDirectory()) {
                        String[] tmpTifFiles = tmpFile.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File file, String accession) {
                                return file.getName().toLowerCase().contains(".tif");
                            }
                        });
                        finalFileList.addAll(Arrays.asList(tmpTifFiles));
                    }
                    else {
                        if (tmpFile.getName().toLowerCase().contains(".tif")&&tmpFile.exists()) {finalFileList.add(tmpFile.getAbsolutePath());}
                    }
                }
            }

            processData.putItem("INPUT_FILES", Task.csvStringFromCollection(finalFileList));
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
    }
}
