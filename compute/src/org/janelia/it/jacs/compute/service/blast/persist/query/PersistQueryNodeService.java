
package org.janelia.it.jacs.compute.service.blast.persist.query;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastNTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * This class creates the Blast Query Sequence DataNode and persists it.  It also sets the BlastTask
 * DataSetNodeId and saves it.  This class is used by Junit tests for Blast tests.
 *
 * @author Tareq Nabeel
 */
public class PersistQueryNodeService implements IService {

    private Logger logger;
    protected IProcessData processData;
    protected ComputeDAO computeDAO;
    private String userLogin;
    private String fastaFilePath;
    private Task task;
    private FastaFileNode fastaFileNode;
    private String sessionName;

    public PersistQueryNodeService() {
    }

    public void execute(IProcessData processData) throws PersistQNodeException {
        try {
            init(processData);
            saveFastaFileNode();
            updateBlastTask();
        }
        catch (PersistQNodeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new PersistQNodeException(e);
        }
    }

    protected void init(IProcessData processData) throws MissingDataException, IOException {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        this.processData = processData;
        computeDAO = new ComputeDAO();
        task = ProcessDataHelper.getTask(processData);
        userLogin = (String) processData.getMandatoryItem(IProcessData.USER_NAME);
        fastaFilePath = (String) processData.getMandatoryItem("fastaFilePath");
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
    }


    public Long saveFastaFileNode() throws PersistQNodeException {
        return saveFastaFileNode(userLogin, fastaFilePath);
    }

    public Long saveFastaText(String userLogin, String fastaText) throws PersistQNodeException {
        try {
            File fastaFileInput = FileUtil.ensureFileExists(FileUtil.checkFileExists(SystemConfigurationProperties.getString("FileStore.CentralDir")).getAbsolutePath() +
                    File.separator + userLogin + File.separator + "FastInput" + System.currentTimeMillis());
            PrintWriter pw = new PrintWriter(fastaFileInput);
            try {
                pw.write(fastaText);
            }
            finally {
                pw.close();
            }
            return saveFastaFileNode(userLogin, fastaFileInput.getAbsolutePath(), true);
        }
        catch (Exception e) {
            throw new PersistQNodeException(e); // this method can be accessed throw remote ejb interface so we'll throw PersistQNodeException
        }
    }


    public Long saveFastaFileNode(String userLogin, String fastaFilePath) throws PersistQNodeException {
        return saveFastaFileNode(userLogin, fastaFilePath, false);
    }

    public Long saveFastaFileNode(String userLogin, String fastaFilePath, boolean deleteFastaFileInput) throws PersistQNodeException {
        try {
            File fastaFileInput = FileUtil.checkFileExists(fastaFilePath);
            logger.info("Received fasta file of length " + fastaFileInput.length());
            // Make sure that user exists
            // Create and save fasta file node
            fastaFileNode = createFastaFileNode(userLogin, fastaFileInput);
            // Copy over fasta file over to file store
            FileUtil.ensureDirExists(fastaFileNode.getDirectoryPath());
            // This is how getFastaFilePath method of FastaFileNode expects it and it's how SubmitDrmaaJobService looks for it
            String fastFilePath = fastaFileNode.getFastaFilePath();

            FileUtil.copyFile(fastaFileInput, new File(fastFilePath));
            if (deleteFastaFileInput) {
                fastaFileInput.delete();
            }
            return fastaFileNode.getObjectId();
        }
        catch (Exception e) {
            throw new PersistQNodeException(e); // this method can be accessed throw remote ejb interface so we'll throw PersistQNodeException
        }
    }

    private FastaFileNode createFastaFileNode(String user, File fastFileInput) throws DaoException, IOException {
        String sequenceType = FastaUtil.determineSequenceType(fastFileInput);
        long[] sequenceCountAndTotalLength = FastaUtil.findSequenceCountAndTotalLength(fastFileInput);
        logger.info("Fasta file " + fastFileInput.getAbsolutePath() + " sequence count=" + sequenceCountAndTotalLength[0] + " sequence length=" + sequenceCountAndTotalLength[1]);
        FastaFileNode fastaFileNode = new FastaFileNode(user, null/*Task*/, user + "FASTA",
                "Fasta file uploaded on " + new Date(),
                Node.VISIBILITY_PRIVATE, sequenceType, (int) sequenceCountAndTotalLength[0], sessionName);
        fastaFileNode.setLength(sequenceCountAndTotalLength[1]);
        computeDAO.saveOrUpdate(fastaFileNode);
        return fastaFileNode;
    }

    private void updateBlastTask() throws DaoException {
        task.setParameter(BlastNTask.PARAM_query, String.valueOf(fastaFileNode.getObjectId()));
        computeDAO.saveOrUpdate(task);
    }

}
