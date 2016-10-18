
package org.janelia.it.jacs.compute.service.blast.createDB;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.CreateBlastDatabaseTask;
import org.janelia.it.jacs.model.tasks.utility.UploadFileTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.shared.blast.CreateBlastDatabaseFromFastaTool;
import org.janelia.it.jacs.shared.blast.FormatDBTool;
import org.janelia.it.jacs.shared.fasta.FastaFile;
import org.janelia.it.jacs.shared.node.FastaUtil;
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
public class CreateBlastDBService implements IService {
    protected Logger logger;

    protected Task task;
    protected String sessionName;
    protected FastaFileNode fastaFile;
    protected ComputeDAO computeDAO;

    protected void init(IProcessData processData) throws MissingDataException, IOException, DaoException, ServiceException {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        computeDAO = new ComputeDAO();
        task = ProcessDataHelper.getTask(processData);
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        // If the node id exists, use it
        if (null != task.getParameter(CreateBlastDatabaseTask.PARAM_FASTA_NODE_ID)) {
            fastaFile = (FastaFileNode) computeDAO.getNodeById(Long.valueOf(task.getParameter(CreateBlastDatabaseTask.PARAM_FASTA_NODE_ID)));
        }
        // otherwise, check for the path
        else if (null != task.getParameter(CreateBlastDatabaseTask.PARAM_FASTA_FILE_PATH)) {
            fastaFile = createFastaNode(task.getParameter(CreateBlastDatabaseTask.PARAM_FASTA_FILE_PATH));
        }
    }

    private FastaFileNode createFastaNode(String fastaFilePath) throws IOException, DaoException {
        UploadFileTask fileTask = new UploadFileTask(null, task.getOwner(), null, null, fastaFilePath);
        fileTask.setParentTaskId(task.getObjectId());
        fileTask = (UploadFileTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(fileTask);
        EJBFactory.getLocalComputeBean().submitJob("UploadFile", fileTask.getObjectId());
        return (FastaFileNode) EJBFactory.getLocalComputeBean().getResultNodeByTaskId(fileTask.getObjectId());

//        File fastaFile = new File(fastaFilePath);
//        FastaFileNode fastaNode = new FastaFileNode(task.getOwner(), null, "Fasta for blast db", "Fasta for blast db:"+fastaFilePath,
//                Node.VISIBILITY_PRIVATE, FastaFileNode.FILE_DATA_TYPE, 0, null);
//        long[] seqCountTotalLength = FastaUtil.findSequenceCountAndTotalLength(fastaFile);
//        fastaNode.setSequenceCount((int)seqCountTotalLength[0]);
//        fastaNode.setLength(seqCountTotalLength[1]);
//        fastaNode.setSequenceType(FastaUtil.determineSequenceType(fastaFile));
//        computeDAO.saveOrUpdate(fastaNode);
//        return fastaNode;
    }

    public void execute(IProcessData processData) throws ServiceException {
        try {
            init(processData);
            EJBFactory.getRemoteComputeBean().addEventToTask(task.getObjectId(), new Event(Event.RUNNING_EVENT, new Date(), Event.RUNNING_EVENT));

            // Calculate the FASTA numbers - these might be too large to do from clients
            File tmpFasta = new File(fastaFile.getFilePathByTag(FastaFileNode.TAG_FASTA));
            long[] seqCountAndLength = FastaUtil.findSequenceCountAndTotalLength(tmpFasta);
            fastaFile.setSequenceCount((int) seqCountAndLength[0]);
            fastaFile.setLength(seqCountAndLength[1]);
            computeDAO.saveOrUpdate(fastaFile);
            String visibility = task.getParameter(CreateBlastDatabaseTask.PARAM_DB_VISIBILITY);
            // Make the BlastDatabaseFileNode
            BlastDatabaseFileNode bdfn = new BlastDatabaseFileNode(task.getOwner(), task,
                    task.getParameter(CreateBlastDatabaseTask.PARAM_BLAST_DB_NAME),
                    task.getParameter(CreateBlastDatabaseTask.PARAM_BLAST_DB_DESCRIPTION),
                    (null==visibility)?Node.VISIBILITY_PRIVATE:visibility, SequenceType.NOT_SPECIFIED, sessionName);
            // Have to set a dummy value to make the Hibernate mapping happy.  Why does this enforce not-null?
            // It's bad enough we have to save the thing to get a path
            bdfn.setPartitionCount(0);
            bdfn.setLength(fastaFile.getLength());
            bdfn.setSequenceCount(fastaFile.getSequenceCount());
            computeDAO.saveOrUpdate(bdfn);
            FileUtil.ensureDirExists(bdfn.getDirectoryPath());
            FileUtil.cleanDirectory(bdfn.getDirectoryPath());

            // Now populate the blast partitions from the FASTA file
            CreateBlastDatabaseFromFastaTool pf = new CreateBlastDatabaseFromFastaTool(logger);
            pf.setFastaFilePath(fastaFile.getDirectoryPath());
            String residueType = fastaFile.getSequenceType();
            bdfn.setSequenceType(residueType);
            pf.setResidueType(residueType);
            pf.setFastaFile(new FastaFile(fastaFile.getFilePathByTag(FastaFileNode.TAG_FASTA)));
            pf.setOutputPath(bdfn.getDirectoryPath());
            pf.setPartitionPrefix(BlastDatabaseFileNode.PARTITION_PREFIX);
            pf.setPartitionSize(SystemConfigurationProperties.getLong("BlastServer.PartitionSize")); // experimental
            pf.setPartitionEntries(SystemConfigurationProperties.getLong("BlastServer.PartitionEntries"));
            Properties prop = new Properties();
            prop.setProperty(FormatDBTool.FORMATDB_PATH_PROP,
                    SystemConfigurationProperties.getString("Executables.ModuleBase")+
                        SystemConfigurationProperties.getString(FormatDBTool.FORMATDB_PATH_PROP));
            prop.setProperty(SystemCall.SCRATCH_DIR_PROP, bdfn.getDirectoryPath());
            prop.setProperty(SystemCall.SHELL_PATH_PROP,
                    SystemConfigurationProperties.getString(SystemCall.SHELL_PATH_PROP));
            prop.setProperty(SystemCall.STREAM_DIRECTOR_PROP,
                    SystemConfigurationProperties.getString(SystemCall.STREAM_DIRECTOR_PROP));
            pf.setProperties(prop);
            pf.partition();
            bdfn.setPartitionCount(new Integer("" + pf.getNumPartitions()));
            checkForFormatDbErrors(new File(bdfn.getDirectoryPath()));

            // build the tera-blast database
            try {
                if ( bdfn.getOwner().equals(User.SYSTEM_USER_LOGIN) ) {
                    String buildCmd = "dc_run -query " + tmpFasta.getAbsolutePath()
                            + " -database VICS_" + bdfn.getObjectId().toString()
                            + " -description \"" + bdfn.getName() + "\"";
                    SystemCall system = new SystemCall( logger );
                    if ( residueType.equals("peptide") ) {
                        buildCmd = buildCmd.concat(" -parameters format_aa_into_aa");
                    } else {
                        buildCmd = buildCmd.concat(" -parameters format_nt_into_nt_and_aa");
                    }
                    system.emulateCommandLine( buildCmd + " > " + bdfn.getDirectoryPath() + "/tera_formatdb.log", true );
/*
                    String logContents = TextFileIO.readTextFile(bdfn.getDirectoryPath() + "/tera_formatdb.log");
                    if ( logContents.contains("Error=") ) {
                        throw new Exception(logContents);
                    }
*/
                    bdfn.setDecypherDbId("VICS_" + bdfn.getObjectId().toString());
                }
            } catch (Exception e) {
                throw new Exception(e.getMessage() + " (build tera-blast db)");
            }
            computeDAO.saveOrUpdate(bdfn);
        }

        // handle exceptions
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
