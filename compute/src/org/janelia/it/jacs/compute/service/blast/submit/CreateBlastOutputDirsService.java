
package org.janelia.it.jacs.compute.service.blast.submit;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.CreateOutputDirsService;
import org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobException;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 28, 2008
 * Time: 1:28:38 PM
 */

public class CreateBlastOutputDirsService extends CreateOutputDirsService {

    public void execute(IProcessData processData) throws SubmitJobException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            init(processData);
            createOutputDirs();
            processData.putItem(BlastProcessDataConstants.BLAST_DEST_OUTPUT_DIR, outputDirs);
            processData.putItem(BlastProcessDataConstants.BLAST_QUERY_FILES, queryFiles);
            processData.putItem(BlastProcessDataConstants.BLAST_QUERY_OUTPUT_FILE_MAP, inputOutputDirMap);
        }
        catch (Exception e) {
            throw new SubmitJobException(e);
        }
    }

    protected void doAdditionalIntegrationPerInputOutput(File queryFile, File outputDir) throws Exception {
        FileUtil.copyFile(queryFile.getAbsolutePath() + ".seqCount", outputDir.getAbsolutePath() + File.separator + "seqCount");
    }

}
