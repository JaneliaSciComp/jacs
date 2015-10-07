
package org.janelia.it.jacs.compute.service.blast.createDB;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.blast.persist.results.initial.CreateBlastFileNodeException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.CreateBlastDatabaseTask;
import org.janelia.it.jacs.model.tasks.blast.CreateMpiBlastDatabaseTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.MpiBlastDatabaseFileNode;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;

/**
 * This service creates a blast result file node.  It's entirely extracted from work done by Sean Murphy and Todd Safford.
 * This service needs to run in a different transaction context.
 *
 * @author Tareq Nabeel
 */
public class CreateMpiBlastDBFileNodeService implements IService {

    public void execute(IProcessData processData) throws CreateBlastFileNodeException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            ComputeDAO computeDAO = new ComputeDAO(logger);
            String sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            FastaFileNode fastaFile = (FastaFileNode) computeDAO.getNodeById(Long.valueOf(task.getParameter(CreateMpiBlastDatabaseTask.PARAM_FASTA_NODE_ID)));
            // Calculate the FASTA numbers - these might be too large to do from clients
            File tmpFasta = new File(fastaFile.getFilePathByTag(FastaFileNode.TAG_FASTA));
            long[] seqCountAndLength = FastaUtil.findSequenceCountAndTotalLength(tmpFasta);
            fastaFile.setSequenceCount((int) seqCountAndLength[0]);
            fastaFile.setLength(seqCountAndLength[1]);
            computeDAO.saveOrUpdate(fastaFile);

            // Make the BlastDatabaseFileNode
            MpiBlastDatabaseFileNode bdfn = new MpiBlastDatabaseFileNode(task.getOwner(), task,
                    task.getParameter(CreateBlastDatabaseTask.PARAM_BLAST_DB_NAME),
                    task.getParameter(CreateBlastDatabaseTask.PARAM_BLAST_DB_DESCRIPTION),
                    Node.VISIBILITY_PRIVATE, SequenceType.NOT_SPECIFIED, sessionName);
            // Have to set a dummy value to make the Hibernate mapping happy.  Why does this enforce not-null?
            // It's bad enough we have to save the thing to get a path
            bdfn.setPartitionCount(0);
            bdfn.setLength(fastaFile.getLength());
            bdfn.setSequenceCount(fastaFile.getSequenceCount());
            bdfn = (MpiBlastDatabaseFileNode) EJBFactory.getRemoteComputeBean().saveOrUpdateNode(bdfn);
            FileUtil.ensureDirExists(bdfn.getDirectoryPath());
            FileUtil.cleanDirectory(bdfn.getDirectoryPath());
            processData.putItem(BlastProcessDataConstants.RESULT_FILE_NODE_ID, bdfn.getObjectId());
            processData.putItem(BlastProcessDataConstants.RESULT_FILE_NODE_DIR, bdfn.getDirectoryPath());
            logger.debug("Created MPI blast database file node and placed in processData id=" + bdfn.getObjectId());
        }
        catch (Exception e) {
            throw new CreateBlastFileNodeException(e);
        }
    }

}