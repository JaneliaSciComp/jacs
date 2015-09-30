
package org.janelia.it.jacs.compute.service.recruitment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentSamplingBlastDatabaseBuilderTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentSamplingDatabaseFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 23, 2010
 * Time: 4:25:31 PM
 */
public class CreateFrvSamplingDBFastaService implements IService {
    private Long entryCounter = 0l;
    private Long seqCounter = 0l;
    private String tmpLine;

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            ComputeDAO computeDAO = new ComputeDAO(logger);
            RecruitmentSamplingBlastDatabaseBuilderTask builderTask =
                    (RecruitmentSamplingBlastDatabaseBuilderTask) ProcessDataHelper.getTask(processData);
            EJBFactory.getRemoteComputeBean().saveEvent(builderTask.getObjectId(), "Creating Sampling DB FASTA", "Creating Sampling DB FASTA", new Date());
            List<String> blastDbIds = Task.listOfStringsFromCsvString(builderTask.getOriginalBlastDbNodeIds());
            // Create the recruitment sampling database entry
            RecruitmentSamplingDatabaseFileNode rsdfn = new RecruitmentSamplingDatabaseFileNode(builderTask.getOwner(),
                    builderTask, builderTask.getSamplingDbName(), builderTask.getSamplingDbDescription(), Node.VISIBILITY_PRIVATE,
                    BlastDatabaseFileNode.NUCLEOTIDE, null);
            // Have to set a dummy value to make the Hibernate mapping happy.  Why does this enforce not-null?
            // It's bad enough we have to save the thing to get a path
            rsdfn.setPartitionCount(0);
            rsdfn.setLength(0l);
            rsdfn.setSequenceCount(0);
            computeDAO.saveOrUpdate(rsdfn);
            FileUtil.ensureDirExists(rsdfn.getDirectoryPath());
            FileUtil.cleanDirectory(rsdfn.getDirectoryPath());

            // Loop through the blast db's used for this sampling
            File finalFasta = new File(rsdfn.getDirectoryPath() + File.separator + RecruitmentSamplingDatabaseFileNode.TAG_SAMPLING_FASTA_NAME);
            FileWriter finalFastaWriter = new FileWriter(finalFasta);
            try {
                for (String blastDbId : blastDbIds) {
                    BlastDatabaseFileNode bdfn = (BlastDatabaseFileNode) computeDAO.getNodeById(Long.valueOf(blastDbId));
                    // Now work with the original database - build the fasta
                    File[] originalFastaFiles = new File(bdfn.getDirectoryPath()).listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.toLowerCase().endsWith(".fasta");
                        }
                    });
                    // The metrics we're using to create the sampling db:
                    // 1% of the total number of entries if > 5000
                    // or max number of entries <=5000
                    // NOTE: Small blast db's can probably be simply copied as we already know the seq and entry count stats
                    long dbEntries = bdfn.getSequenceCount();
                    boolean randomlySelect = (dbEntries > 5000);
                    double targetEntryCount = ((dbEntries * 0.01) <= 5000) ? 5000 : (dbEntries * 0.01);
                    if (!randomlySelect) {
                        targetEntryCount = dbEntries;
                    }
                    logger.debug("Adding to sampling db " + targetEntryCount + " entries.");
                    Scanner scanner = null;
                    try {
                        for (File originalFastaFile : originalFastaFiles) {
                            scanner = new Scanner(originalFastaFile);
                            tmpLine = scanner.nextLine();
                            while (tmpLine != null) {
                                if (tmpLine.startsWith(">")) {
                                    if (!randomlySelect) {
                                        writeEntry(scanner, finalFastaWriter);
                                    }
                                    else {
                                        Long tmpValue = Math.round(Math.random() * dbEntries);
                                        // Ensuring 1% of X entries (with minor variation)
                                        if (tmpValue <= targetEntryCount) {
                                            writeEntry(scanner, finalFastaWriter);
                                        }
                                        else {
                                            while (scanner.hasNextLine()) {
                                                tmpLine = scanner.nextLine();
                                                if (tmpLine.startsWith(">")) {
                                                    break;
                                                }
                                            }
                                            if (!scanner.hasNextLine()) {
                                                tmpLine = null;
                                            }
                                        }
                                    }
                                }
                                else {
                                    if (scanner.hasNextLine()) {
                                        tmpLine = scanner.nextLine();
                                    }
                                    else {
                                        tmpLine = null;
                                    }
                                }
                            }
                            scanner.close();
                        }
                    }
                    finally {
                        scanner.close();
                    }
                }
            }
            finally {
                finalFastaWriter.close();
            }
            logger.debug("Wrote " + entryCounter + " fasta entries to " + finalFasta.getAbsolutePath());
            logger.debug("Wrote " + seqCounter + " bases.");
            // Now, update the RecruitmentSamplingDatabaseFileNode with the final numbers
            rsdfn.setPartitionCount(0);
            rsdfn.setLength(seqCounter);
            rsdfn.setSequenceCount(entryCounter.intValue());
            computeDAO.saveOrUpdate(rsdfn);
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, rsdfn);
        }
        catch (Exception e) {
            throw new ServiceException("There was an error in CreateFrvSamplingDBFasta.\n" + e.getMessage());
        }
    }

    private void writeEntry(Scanner scanner, FileWriter finalFastaWriter)
            throws IOException {
        finalFastaWriter.write(tmpLine);
        finalFastaWriter.write("\n");
        while (scanner.hasNextLine()) {
            tmpLine = scanner.nextLine();
            if (tmpLine.startsWith(">")) {
                entryCounter++;
                break;
            }
            else {
                finalFastaWriter.write(tmpLine);
                finalFastaWriter.write("\n");
                seqCounter += tmpLine.length();
            }
        }
        if (!scanner.hasNextLine()) {
            tmpLine = null;
        }
    }

}
