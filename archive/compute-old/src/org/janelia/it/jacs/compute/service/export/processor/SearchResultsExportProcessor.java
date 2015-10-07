
package org.janelia.it.jacs.compute.service.export.processor;

import org.janelia.it.jacs.compute.access.ExportDAO;
import org.janelia.it.jacs.compute.access.SearchDAO;
import org.janelia.it.jacs.compute.access.search.ClusterResult;
import org.janelia.it.jacs.compute.access.search.ProteinClusterSearchDAO;
import org.janelia.it.jacs.compute.access.search.ProteinSearchDAO;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.export.model.BseCsvWriter;
import org.janelia.it.jacs.compute.service.export.model.BseFastaWriter;
import org.janelia.it.jacs.compute.service.export.model.ClusterResultWriter;
import org.janelia.it.jacs.compute.service.export.writers.ExportFastaWriter;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.Peptide;
import org.janelia.it.jacs.model.tasks.export.ClusterSearchExportTask;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.model.tasks.export.ProteinSearchExportTask;
import org.janelia.it.jacs.model.user_data.export.ExportFileNode;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 6, 2008
 * Time: 4:15:33 PM
 */
public class SearchResultsExportProcessor extends ExportProcessor {
    public SearchResultsExportProcessor(ExportTask exportTask, ExportFileNode exportFileNode) throws IOException, MissingDataException {
        super(exportTask, exportFileNode);
    }

    public void execute() throws Exception {
        if (exportTask instanceof ClusterSearchExportTask) {
            ClusterSearchExportTask clusterSearchExportTask = (ClusterSearchExportTask) exportTask;
            String search_id_str = clusterSearchExportTask.getParameter(ClusterSearchExportTask.SEARCH_TASK_ID);
            Long searchId = Long.valueOf(search_id_str);
            SortArgument[] sortArgs = null;
            if (clusterSearchExportTask.getSortArguments() != null) {
                sortArgs = new SortArgument[clusterSearchExportTask.getSortArguments().size()];
                sortArgs = clusterSearchExportTask.getSortArguments().toArray(sortArgs);
            }
            if (exportTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_CSV)) {
                ProteinClusterSearchDAO clusterSearchDAO = new ProteinClusterSearchDAO(_logger);
                SearchDAO searchDAO = new ProteinClusterSearchDAO(_logger);
                Long searchResultNodeId = searchDAO.retrieveSearchNodeByTask(searchId).getObjectId();
                List<ClusterResult> clusterResults = clusterSearchDAO.getPagedCategoryResultsByNodeId(searchResultNodeId,
                        0 /*start*/, 0 /*numrows*/, sortArgs);
                ClusterResultWriter clusterResultWriter = new ClusterResultWriter(exportWriter, clusterResults);
                clusterResultWriter.write();
            }
            else {
                throw new Exception("Do not support exportType=" + exportTask.getDataType());
            }
        }
        else if (exportTask instanceof ProteinSearchExportTask) {
            ProteinSearchExportTask proteinSearchExportTask = (ProteinSearchExportTask) exportTask;
            String search_id_str = proteinSearchExportTask.getParameter(ProteinSearchExportTask.SEARCH_TASK_ID);
            Long searchId = Long.valueOf(search_id_str);
            // todo Sorting support for this is involved and that work has been deferred
//            SortArgument[] sortArgs;
//            if (proteinSearchExportTask.getSortArguments() != null) {
//                sortArgs = new SortArgument[proteinSearchExportTask.getSortArguments().size()];
//                sortArgs = proteinSearchExportTask.getSortArguments().toArray(sortArgs);
//            }
            ExportDAO exportDAO = new ExportDAO(_logger);
            SearchDAO searchDAO = new ProteinSearchDAO(_logger);
            SearchResultNode resultNode = searchDAO.getSearchTaskResultNode(searchId);
            List<String> proteinAccList = exportDAO.getProteinIDsBySearchID(resultNode.getObjectId() + "");
            _logger.debug("proteinSearch returned list of ids of this size=" + proteinAccList.size());
            List<Peptide> proteinList = exportDAO.getProteinsByIDList(proteinAccList, null /*comparator*/);
            _logger.debug("proteinSearch returned list of proteins of this size=" + proteinList.size());
            if (exportTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_CSV)) {
                BseCsvWriter proteinCsvWriter = new BseCsvWriter(exportWriter, proteinList);
                proteinCsvWriter.write();
            }
            else if (exportTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_FASTA)) {
                BseFastaWriter proteinFastaWriter = new BseFastaWriter((ExportFastaWriter) exportWriter, proteinList);
                proteinFastaWriter.write();
            }
            else {
                throw new Exception("Do not support exportFormatType=" + exportTask.getExportFormatType());
            }
        }
        else {
            throw new Exception("Not configured to handle exportType=" + exportTask.getClass().getName());
        }
    }

    public String getProcessorType() {
        return "Search";
    }

    protected List<SortArgument> getDataHeaders() {
        return null;
    }

}
