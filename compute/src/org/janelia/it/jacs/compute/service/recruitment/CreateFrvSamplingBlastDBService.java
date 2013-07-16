
package org.janelia.it.jacs.compute.service.recruitment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentSamplingDatabaseFileNode;
import org.janelia.it.jacs.shared.blast.CreateBlastDatabaseFromFastaTool;
import org.janelia.it.jacs.shared.blast.FormatDBTool;
import org.janelia.it.jacs.shared.fasta.FastaFile;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 3, 2008
 * Time: 2:53:59 PM
 */
public class CreateFrvSamplingBlastDBService implements IService {
    private Logger logger;

    private Task task;
    private ComputeDAO computeDAO;

    protected void init(IProcessData processData) throws MissingDataException, IOException {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        computeDAO = new ComputeDAO(logger);
        task = ProcessDataHelper.getTask(processData);
    }

    public void execute(IProcessData processData) throws ServiceException {
        try {
            init(processData);
            EJBFactory.getRemoteComputeBean().saveEvent(task.getObjectId(), "Creating Sampling DB", "Creating Sampling DB", new Date());
            RecruitmentSamplingDatabaseFileNode rsdfn = (RecruitmentSamplingDatabaseFileNode) ProcessDataHelper.getResultFileNode(processData);

            // Now populate the blast partitions from the FASTA file
            String fastaPath = rsdfn.getDirectoryPath() + File.separator + RecruitmentSamplingDatabaseFileNode.TAG_SAMPLING_FASTA_NAME;
            CreateBlastDatabaseFromFastaTool pf = new CreateBlastDatabaseFromFastaTool(logger);
            pf.setFastaFilePath(fastaPath);
            String residueType = rsdfn.getSequenceType();
            rsdfn.setSequenceType(residueType);
            pf.setResidueType(residueType);
            pf.setFastaFile(new FastaFile(fastaPath));
            pf.setOutputPath(rsdfn.getDirectoryPath());
            pf.setPartitionPrefix(RecruitmentSamplingDatabaseFileNode.PARTITION_PREFIX);
            pf.setPartitionSize(SystemConfigurationProperties.getLong("BlastServer.PartitionSize")); // experimental
            pf.setPartitionEntries(SystemConfigurationProperties.getLong("BlastServer.PartitionEntries"));
            Properties prop = new Properties();
            prop.setProperty(FormatDBTool.FORMATDB_PATH_PROP,
                    SystemConfigurationProperties.getString("Executables.ModuleBase")+
                        SystemConfigurationProperties.getString(FormatDBTool.FORMATDB_PATH_PROP));
            prop.setProperty(SystemCall.SCRATCH_DIR_PROP, rsdfn.getDirectoryPath());
            prop.setProperty(SystemCall.SHELL_PATH_PROP,
                    SystemConfigurationProperties.getString(SystemCall.SHELL_PATH_PROP));
            prop.setProperty(SystemCall.STREAM_DIRECTOR_PROP,
                    SystemConfigurationProperties.getString(SystemCall.STREAM_DIRECTOR_PROP));
            pf.setProperties(prop);
            pf.partition();
            rsdfn.setPartitionCount(new Integer("" + pf.getNumPartitions()));
            checkForFormatDbErrors(new File(rsdfn.getDirectoryPath()));
            computeDAO.saveOrUpdate(rsdfn);
            if (!(new File(fastaPath).delete())) {
                throw new ServiceException("Unable to delete the FRV final sampling fasta.");
            }
        }
        catch (Exception e) {
            String error = "There was a problem creating the blast database:\n" + e.getMessage();
            System.err.println(error);
            try {
                EJBFactory.getRemoteComputeBean().addEventToTask(task.getObjectId(), new Event(error, new Date(), Event.ERROR_EVENT));
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
            throw new ServiceException(error, e);
        }
    }

    private void checkForFormatDbErrors(File blastDBOutputDir) throws IOException, InterruptedException {
        // grep 'ERROR' /db/cameradb/dma/system/**/formatdb.log | wc -l
        String formatdbLogPath = blastDBOutputDir.getAbsolutePath() + File.separator + "formatdb.log";
        int count = FileUtil.getCountUsingUnixCall("grep 'ERROR' " + formatdbLogPath + " | wc -l");
        logger.info(blastDBOutputDir.getName() + " formatdb error count=" + count);
        if (count > 0) {
            throw new RuntimeException("formatdb failed");
        }
    }

}