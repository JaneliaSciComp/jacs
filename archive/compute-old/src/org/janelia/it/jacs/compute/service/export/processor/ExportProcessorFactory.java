
package org.janelia.it.jacs.compute.service.export.processor;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.model.tasks.export.*;
import org.janelia.it.jacs.model.user_data.export.ExportFileNode;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jun 27, 2008
 * Time: 10:29:23 AM
 */
public class ExportProcessorFactory {

    public static ExportProcessor createProcessor(ExportTask exportTask, ExportFileNode resultNode) throws IOException, MissingDataException {
        if (exportTask instanceof BlastResultExportTask) {
            BlastResultExportTask brExportTask = (BlastResultExportTask) exportTask;
            String formatType = brExportTask.getExportFormatType();
            if (formatType.equals(ExportWriterConstants.EXPORT_TYPE_NCBI_BLAST_XML)) {
                return new NcbiBlastXmlExportProcessor(brExportTask, resultNode);
            }
            else if (formatType.equals(ExportWriterConstants.EXPORT_TYPE_FASTA) ||
                    formatType.equals(ExportWriterConstants.EXPORT_TYPE_CSV) ||
                    formatType.equals(ExportWriterConstants.EXPORT_TYPE_EXCEL)) {
                return new MatchingResultsExportProcessor(brExportTask, resultNode);
            }
            else {
                return null;
            }
        }
        else if (exportTask instanceof ClusterProteinAnnotationExportTask) {
            return new ClusterExportProcessor(exportTask, resultNode);
        }
        else if (exportTask instanceof ClusterSearchExportTask) {
            return new SearchResultsExportProcessor(exportTask, resultNode);
        }
        else if (exportTask instanceof FileNodeExportTask) {
            return new FileNodeExportProcessor(exportTask, resultNode);
        }
        else if (exportTask instanceof FrvReadExportTask) {
            return new FrvExportProcessor(exportTask, resultNode);
        }
        else if (exportTask instanceof ProteinSearchExportTask) {
            return new SearchResultsExportProcessor(exportTask, resultNode);
        }
        else if (exportTask instanceof SampleExportTask) {
            return new SampleReadsExportProcessor(exportTask, resultNode);
        }
        else if (exportTask instanceof SequenceExportTask) {
            return new SequenceExportProcessor(exportTask, resultNode);
        }
        else {
            return null; // placeholder
        }
    }

}
