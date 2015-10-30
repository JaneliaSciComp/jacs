
package org.janelia.it.jacs.compute.service.export.processor;

import org.janelia.it.jacs.compute.access.ExportDAO;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.export.model.BseCsvWriter;
import org.janelia.it.jacs.compute.service.export.model.BseFastaWriter;
import org.janelia.it.jacs.compute.service.export.writers.ExportCsvWriter;
import org.janelia.it.jacs.compute.service.export.writers.ExportFastaWriter;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.Peptide;
import org.janelia.it.jacs.model.tasks.export.ClusterProteinAnnotationExportTask;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.model.user_data.export.ExportFileNode;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 6, 2008
 * Time: 4:20:37 PM
 */
public class ClusterExportProcessor extends ExportProcessor {
    public ClusterExportProcessor(ExportTask exportTask, ExportFileNode exportFileNode) throws IOException, MissingDataException {
        super(exportTask, exportFileNode);
    }

    public void execute() throws Exception {
        if (exportTask instanceof ClusterProteinAnnotationExportTask) {
            // Common attributes
            ClusterProteinAnnotationExportTask clusterTask = (ClusterProteinAnnotationExportTask) exportTask;
            // todo Sorting support for this is involved and that work has been deferred
            //SortArgument[] sortArgumentArr = convertStringListToSortArgumentArr(exportTask.getExportAttributeList());
            String clusterAcc = clusterTask.getAccessionList().get(0);
            boolean onlyNRSeqFlag = Boolean.valueOf(clusterTask.getParameter(ClusterProteinAnnotationExportTask.NR_ONLY));
            String annotationID = clusterTask.getParameter(ClusterProteinAnnotationExportTask.ANNOTATION_ID);
            ExportDAO exportDAO = new ExportDAO(_logger);
            List<String> proteinAccList;
            if (annotationID != null) {
                proteinAccList = exportDAO.getProteinIDsByClusterAnnotation(clusterAcc, annotationID);
            }
            else if (onlyNRSeqFlag) {
                proteinAccList = exportDAO.getNonRedundantProteinIDsByClusterID(clusterAcc);
            }
            else {
                proteinAccList = exportDAO.getMemberProteinIDsByClusterID(clusterAcc);
            }
            List<Peptide> proteinList = exportDAO.getProteinsByIDList(proteinAccList, null /* comparator*/);

            // Fasta case
            if (clusterTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_FASTA)) {
                ExportFastaWriter exportFastaWriter = (ExportFastaWriter) exportWriter;
                BseFastaWriter proteinFastaWriter = new BseFastaWriter(exportFastaWriter, proteinList);
                proteinFastaWriter.write();

                // CSV case
            }
            else if (clusterTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_CSV) ||
                    clusterTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_EXCEL)) {
                ExportCsvWriter exportCsvWriter = (ExportCsvWriter) exportWriter;
                BseCsvWriter proteinCsvWriter = new BseCsvWriter(exportCsvWriter, proteinList);
                proteinCsvWriter.write();
            }
        }
        else {
            throw new Exception("Do not recognize export task type=" + exportTask.getDataType());
        }
    }

    public String getProcessorType() {
        return "Cluster";
    }

    protected List<SortArgument> getDataHeaders() {
        return null;
    }

}
