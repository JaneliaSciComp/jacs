
package org.janelia.it.jacs.compute.service.recruitment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.export.writers.ExportFastaWriter;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentDataFastaBuilderTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 14, 2007
 * Time: 10:11:38 AM
 */
public class FrvDataFastaNonGridService implements IService {

    protected Logger logger;
    public static final int DATABASE_QUERY_CHUNK_SIZE = SystemConfigurationProperties.getInt("RecruitmentViewer.DatabaseQueryChunkSize");//500;
    public static final String SAMPLE_FILE_NAME = SystemConfigurationProperties.getString("RecruitmentViewer.SampleFile.Name");
    public static final String BASE_FILE_PATH = SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("RecruitmentViewer.PerlBaseDir");

    protected Task task;
    protected IProcessData processData;
    protected RecruitmentResultFileNode resultFileNode;
    protected Set<String> jobSet;
    protected ComputeDAO computeDAO;

    protected void init(IProcessData processData) throws MissingDataException, IOException {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        computeDAO = new ComputeDAO(logger);
        task = ProcessDataHelper.getTask(processData);
        resultFileNode = (RecruitmentResultFileNode) computeDAO.getNodeById(
                Long.valueOf(task.getParameter(RecruitmentDataFastaBuilderTask.RECRUITMENT_RESULT_NODE_ID)));
    }

    public void execute(IProcessData processData) throws ServiceException {
        // Need to set the task and result node information before the Grid base class takes over
        try {
            init(processData);
            RecruitmentViewerFilterDataTask rvTask = (RecruitmentViewerFilterDataTask) resultFileNode.getTask();
            RecruitmentFileNode recruitmentDataNode = (RecruitmentFileNode) rvTask.getInputNodes().iterator().next();
            String pathToAnnotationFile = null;
            // Since annotations don't exist for user-uploaded sequences, there is no
            if (User.SYSTEM_USER_LOGIN.equalsIgnoreCase(recruitmentDataNode.getOwner())) {
                GenomeProjectFileNode gpNode = (GenomeProjectFileNode) computeDAO.getNodeById(Long.valueOf(rvTask.getParameter(RecruitmentViewerTask.GENOME_PROJECT_NODE_ID)));
                pathToAnnotationFile = gpNode.getDirectoryPath() + File.separator +
                        rvTask.getParameter(RecruitmentViewerFilterDataTask.GENBANK_FILE_NAME);
            }

            // Accession id list means nothing.  The bounds determine the reads involved
            ArrayList<SortArgument> attributeList = new ArrayList<SortArgument>();
            attributeList.add(new SortArgument("defline"));
            attributeList.add(new SortArgument("sequence"));
            String absoluteFastaFilePath = recruitmentDataNode.getDirectoryPath() + File.separator +
                    RecruitmentFileNode.RECRUITED_READ_FASTA_FILENAME;
            logger.debug("\nThe fasta file will be located at: " + absoluteFastaFilePath);
            ExportFastaWriter exportWriter = new ExportFastaWriter(absoluteFastaFilePath, attributeList);

            String startBasePair = "0";
            String endBasePair = Double.toString(rvTask.getReferenceEnd());
            String startPctId = "50";
            String endPctId = "100";

            RecruitmentDataHelper helper = new RecruitmentDataHelper(recruitmentDataNode.getDirectoryPath(), resultFileNode.getDirectoryPath(),
                    pathToAnnotationFile,
                    BASE_FILE_PATH + File.separator + SAMPLE_FILE_NAME, rvTask.getSampleListAsCommaSeparatedString(),
                    startPctId, endPctId, startBasePair, endBasePair,
                    rvTask.getParameterVO(RecruitmentViewerFilterDataTask.MATE_BITS).getStringValue(), "", "", "");

            // Get the accessions which match the rubberbanded region
            Set<String> readAccs = helper.exportSelectedSequenceIds();
            System.out.println("Total reads:" + readAccs.size());
            exportWriter.start();
            int totalExported = 0;
            HashSet<String> tmpAccChunk = new HashSet<String>();
            for (Iterator it = readAccs.iterator(); it.hasNext();) {
                tmpAccChunk.add((String) it.next());
                if (!it.hasNext() || tmpAccChunk.size() % DATABASE_QUERY_CHUNK_SIZE == 0) {
                    // Processing...
                    List<Object[]> data = computeDAO.getReadsByAccessions(tmpAccChunk);
                    // Check that the db returned fasta material for all accessions
                    if (tmpAccChunk.size() != data.size()) {
                        //                    logger.error("Verifying disconnect between selected reads and db read information.");
                        verifyDisconnect(tmpAccChunk, data);
                    }
                    // output resultset
                    //                if (logger.isDebugEnabled()) logger.debug("Starting data output for " + (chunkNo - 1)+"/"+totalChunks);
                    List<List<String>> exportItems = new ArrayList<List<String>>();
                    for (Object[] aBseSet : data) {
                        // instead of reading the defline from the result node defline map
                        // use the one from the base sequence entity
                        // The writers take List<List<String>>.  The header order must match with the items in this list.
                        // Probably a better way to explicitly set up the header-to-list association
                        ArrayList<String> tmpItemList = new ArrayList<String>();
                        // defline
                        tmpItemList.add(aBseSet[0].toString());
                        // seq
                        tmpItemList.add(aBseSet[1].toString());
                        exportItems.add(tmpItemList);
                    }
                    exportWriter.writeItems(exportItems);
                    totalExported += exportItems.size();
                    System.out.println("Total exported: " + totalExported);
                    tmpAccChunk = new HashSet<String>();
                }
            }
            System.out.println("Total Exported:" + totalExported);

            // Clean up the stream
            exportWriter.end();
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }


    /**
     * Verifying disconnect between selected reads and db read information.
     *
     * @param subArr items searched for
     * @param data   info for fasta
     */
    private void verifyDisconnect(HashSet<String> subArr, List<Object[]> data) {
        for (String s : subArr) {
            boolean found = false;
            for (Object[] objects : data) {
                // If the accession is found, then continue
                if (((String) objects[0]).indexOf(s) >= 0) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("Warning: Could not find data for accession: " + s);
            }
        }
    }

}