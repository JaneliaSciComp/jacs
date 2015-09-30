
package org.janelia.it.jacs.compute.service.export.processor;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.export.BlastResultExportTask;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.model.user_data.export.ExportFileNode;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.shared.blast.blastxmlparser.BlastXMLWriter;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jun 27, 2008
 * Time: 3:49:25 PM
 */
public class NcbiBlastXmlExportProcessor extends ExportProcessor {

    public NcbiBlastXmlExportProcessor(ExportTask exportTask, ExportFileNode exportFileNode) throws IOException, MissingDataException {
        super(exportTask, exportFileNode);
    }

    protected void initializeWriter() throws IOException {
        // nothing to do for this processor
    }

    public void execute() throws Exception {
        ParameterVO blastTaskIdParameterVO = exportTask.getParameterVO(BlastResultExportTask.BLAST_TASK_ID);
        if (blastTaskIdParameterVO == null)
            throw new Exception("blastTaskIdParameterVO is null for exportTask=" + exportTask.getObjectId());
        Long blastTaskId = new Long(blastTaskIdParameterVO.getStringValue());
        ComputeDAO computeDAO = new ComputeDAO(_logger);
        BlastResultFileNode blastResultFileNode = computeDAO.getBlastResultFileNodeByTaskId(blastTaskId);
        if (blastResultFileNode == null)
            throw new Exception("Could not find BlastResultFileNode for taskId=" + blastTaskId);
        String zipFilepath = blastResultFileNode.getFilePathByTag(BlastResultFileNode.TAG_ZIP);
        File zipFile = new File(zipFilepath);
        if (zipFile.exists()) {
            // Check for large blast result
            copyFileToExportFileNode(zipFile, exportFileNode);
        }
        else {
            // Since no large blast result, assume BlastResultNode exists
            BlastResultNode blastResultNode = computeDAO.getBlastHitResultDataNodeByTaskId(blastTaskId);
            if (blastResultNode == null)
                throw new Exception("Could not find BlastResultNode for taskId=" + blastTaskId);
            BlastXMLWriter bxmlw = new BlastXMLWriter();
            bxmlw.setBlastSource(blastResultNode);
            File outputFile = new File(exportFileNode.getDirectoryPath(), getSuggestedFilename("blastResults", "xml"));
            FileWriter fileWriter = new FileWriter(outputFile);
            bxmlw.serialize(fileWriter);
            fileWriter.flush();
            fileWriter.close();
        }
    }

    protected void copyFileToExportFileNode(File file, ExportFileNode exportFileNode) throws IOException {
        File exportFileNodeDir = new File(exportFileNode.getDirectoryPath());
        File targetFile = new File(exportFileNodeDir, file.getName());
        FileUtil.copyFileUsingSystemCall(file, targetFile);
    }

    public String getProcessorType() {
        return "NCBI Blast XML Export";
    }

    /**
     * Methods stubbed out for this Processor case
     *
     * @return null
     */
    protected List<SortArgument> getDataHeaders() {
        return null;
    }

}
