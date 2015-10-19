
package org.janelia.it.jacs.compute.service.export.processor;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.model.tasks.export.FileNodeExportTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.export.ExportFileNode;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 1, 2008
 * Time: 4:13:22 PM
 */
public class FileNodeExportProcessor extends ExportProcessor {

    public FileNodeExportProcessor(ExportTask exportTask, ExportFileNode exportFileNode) throws IOException, MissingDataException {
        super(exportTask, exportFileNode);
    }

    protected void initializeWriter() throws IOException {
        // nothing to do for this processor
    }

    public void execute() throws Exception {
        FileNodeExportTask fnTask = (FileNodeExportTask) exportTask;
        String fileNodeId = fnTask.getParameter(FileNodeExportTask.FILE_NODE_ID);
        ComputeDAO computeDAO = new ComputeDAO(_logger);
        Node targetNode = computeDAO.getNodeById(Long.valueOf(fileNodeId));
        if (exportTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_FASTA) ||
                exportTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_CURRENT)) {
            if (targetNode instanceof FastaFileNode) {
                FastaFileNode targetFileNode = (FastaFileNode) targetNode;
                File targetFile = new File(targetFileNode.getFilePathByTag(FastaFileNode.TAG_FASTA));
                String suggestedFilename = exportTask.getSuggestedFilename();
                // If the new suggested name is bad, use the original one
                if (null == suggestedFilename || "".equalsIgnoreCase(suggestedFilename)) {
                    suggestedFilename = targetFile.getName();
                }
                exportFileNode.dropExternalLink(targetFileNode.getObjectId().toString(), targetFile.getName(), suggestedFilename);
            }
            else {
                throw new Exception("FileNodeExportProcessor unable to handle node type=" + targetNode.getClass().getName());
            }
        }
        else {
            throw new Exception("FileNodeExportProcesser unable to handle exportFormatType=" + exportTask.getExportFormatType());
        }
    }

    public String getProcessorType() {
        return "File Node Export";
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
