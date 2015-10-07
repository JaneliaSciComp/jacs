
package org.janelia.it.jacs.compute.service.export.processor;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.ExportDAO;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.export.model.BseCsvWriter;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.model.tasks.export.FrvReadExportTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.user_data.export.ExportFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Class to manage the export of recruited reads.
 * As the FRV uses the combined hits file as the database for this area, the export will use the
 * prefab, all-reads fasta as the export mechanism.  Still, it's hard-wired to defline and sequence data
 * and that may not be flexible enough for the Export Framework.
 * User: tsafford
 * Date: Jun 6, 2008
 * Time: 4:18:10 PM
 */
public class FrvExportProcessor extends ExportProcessor {
    public static final int DATABASE_QUERY_CHUNK_SIZE = SystemConfigurationProperties.getInt("RecruitmentViewer.DatabaseQueryChunkSize");//500;
    public static final String SAMPLE_FILE_NAME = SystemConfigurationProperties.getString("RecruitmentViewer.SampleFile.Name");
    public static final String BASE_FILE_PATH = SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("RecruitmentViewer.PerlBaseDir");
    private ComputeDAO computeDAO;

    public FrvExportProcessor(ExportTask exportTask, ExportFileNode exportFileNode) throws IOException, MissingDataException {
        super(exportTask, exportFileNode);
        computeDAO = new ComputeDAO(_logger);
    }

    public void execute() throws Exception {
        String resultId = exportTask.getParameter(FrvReadExportTask.RECRUITMENT_RESULT_FILE_NODE_ID);
        String startBasePair = exportTask.getParameter(FrvReadExportTask.START_BP_POSITION);
        String endBasePair = exportTask.getParameter(FrvReadExportTask.END_BP_POSITION);
        String startPctId = exportTask.getParameter(FrvReadExportTask.START_PCT_ID_POSITION);
        String endPctId = exportTask.getParameter(FrvReadExportTask.END_PCT_ID_POSITION);

        RecruitmentResultFileNode node = (RecruitmentResultFileNode) computeDAO.getNodeById(Long.valueOf(resultId));
        RecruitmentViewerFilterDataTask rvTask = (RecruitmentViewerFilterDataTask) node.getTask();
        RecruitmentFileNode dataNode = (RecruitmentFileNode) rvTask.getInputNodes().iterator().next();
        RecruitmentDataHelper helper = new RecruitmentDataHelper(dataNode.getDirectoryPath(), node.getDirectoryPath(),
                null/*Genbank File Path not necessary as we do nothing with annotations here*/,
                BASE_FILE_PATH + File.separator + SAMPLE_FILE_NAME, rvTask.getSampleListAsCommaSeparatedString(),
                startPctId, endPctId, startBasePair, endBasePair,
                rvTask.getParameterVO(RecruitmentViewerFilterDataTask.MATE_BITS).getStringValue(), "", "", "");
        // Check for the Export All FASTA case and if so ship the file back
        // Currently, reads are recruited only if 50<=Percent Identity<=100
        if ("0".equals(startBasePair) && rvTask.getParameter(RecruitmentViewerFilterDataTask.GENOME_SIZE).equals(endBasePair) &&
                "50".equals(startPctId) && "100".equals(endPctId) &&
                ExportWriterConstants.EXPORT_TYPE_FASTA.equals(exportTask.getExportFormatType())) {
            FileUtil.deleteFile(exportFileNode.getDirectoryPath(), exportTask.getSuggestedFilename());
            exportFileNode.dropExternalLink(dataNode.getObjectId().toString(), RecruitmentFileNode.RECRUITED_READ_FASTA_FILENAME,
                    exportTask.getSuggestedFilename());
        }
        else {
            // Get the accessions which match the rubberbanded region
            Set<String> readAccs = helper.exportSelectedSequenceIds();
            System.out.println("Total reads:" + readAccs.size());

            if (exportTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_FASTA)) {
                Scanner scanner = new Scanner(new File(dataNode.getFilePathByTag(RecruitmentFileNode.TAG_FASTA_READS)));
                int totalExported = 0;
                // Prime the pump
                String tmpDefline = null;
                if (scanner.hasNextLine()) {
                    tmpDefline = scanner.nextLine() + "\n";
                }

                // Look for the > of the defline
                while (null != tmpDefline && !"".equals(tmpDefline)) {
                    if (!tmpDefline.startsWith(">")) {
                        if (scanner.hasNextLine()) {
                            tmpDefline = scanner.nextLine() + "\n";
                            continue;
                        }
                        break;
                    }
                    // If the tmpEntry StringBuffer is full, write it
                    // Reset the tmpEntry to the new FASTA entry
                    String[] tmpPieces = tmpDefline.split("\\s");
                    String tmpAccession = tmpPieces[0].substring(1);
                    if (readAccs.contains(tmpAccession)) {
                        StringBuffer tmpEntry = new StringBuffer();
                        tmpEntry.append(tmpDefline);
                        while (scanner.hasNextLine()) {
                            String tmpSeq = scanner.nextLine();
                            // If next line exists and not another defline
                            if (null == tmpSeq) {
                                break;
                            }
                            if (tmpSeq.indexOf(">") < 0) {
                                tmpEntry.append(tmpSeq).append("\n");
                            }
                            else {
                                tmpDefline = tmpSeq + "\n";
                                break;
                            }
                        }
                        if (null != tmpEntry && !"".equals(tmpEntry.toString())) {
                            writeEntry(tmpEntry.toString());
                            totalExported++;
                        }

                        // If at the end of the file, make sure to stop the loop
                        if (!scanner.hasNextLine()) {
                            break;
                        }
                    }
                    else {
                        tmpDefline = scanner.nextLine() + "\n";
                    }
                }
                System.out.println("Total Exported:" + totalExported);
            }
            else if (exportTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_CSV) ||
                    exportTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_EXCEL)) {
                ArrayList<String> readList = new ArrayList<String>();
                readList.addAll(readAccs);
                ExportDAO exportDAO = new ExportDAO(_logger);
                List<BaseSequenceEntity> bseList = exportDAO.getBsesByIDList(readList, null /*comparator*/);
                BseCsvWriter csvWriter = new BseCsvWriter(exportWriter, bseList);
                csvWriter.write();
            }
            else {
                throw new Exception("Not configured to handle export format type=" + exportTask.getExportFormatType());
            }
        }
    }

    private void writeEntry(String fastaEntry) throws IOException {
        // The header order must match with the items in this list.
        // Probably a better way to explicitly set up the header-to-list association
        int newline = fastaEntry.indexOf("\n");
        ArrayList<String> tmpItemList = new ArrayList<String>();
        // defline
        tmpItemList.add(fastaEntry.substring(0, newline));
        // seq - strip out all newlines
        tmpItemList.add(fastaEntry.substring(newline + 1).replace("\n", ""));
        exportWriter.writeItem(tmpItemList);
    }

    public String getProcessorType() {
        return "Frv";
    }

    protected List<SortArgument> getDataHeaders() {
        return exportTask.getSortArguments();
    }

}
