
package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.tasks.hmmer.HmmpfamTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.hmmer.HmmerPfamDatabaseNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2008
 * Time: 11:21:17 AM
 */
public class HmmerTest implements HmmerTestMBean {

    private static final Logger logger = Logger.getLogger(HmmerTest.class);

    private static final String TIGRFAM_NODE_ID = "1276628672714899831";

    private String NOT5_SUBUNIT_PROTEIN_SEQUENCE =
            ">CPCC7001_1011 /codon_start=1 /product=glutenin, high molecular weight subunit," +
                    " putative /note=[K] COG5665 CCR4-NOT transcriptional regulation complex, NOT5 subunit" +
                    " /transl_table=11 /coord_start=7309 /coord_stop=8676\n" +
                    "MQAHREQDGEEHQALKPAGSHHPGLLQPDGEEARHGGGHDAPGSEPAQQGLLLQREAHAP\n" +
                    "GGEGDRQRTHHQHQRQHAGHAAPAQRAELGEAHIGGQQRKQHGDRQLGQLADQGVELGGL\n" +
                    "ALQPPAHHHARGHRRRQARLRQHLLGPGEHQEQGAQGELQPQGFGAGLHRQALQHHREQA\n" +
                    "AGGGPEQHPAAEAQQQPEGQLPPAMQVAQHQLEQQQGQHGADRLQHEALPLQHLPQGGPQ\n" +
                    "PDLAHQRVHHSGTGGQHDGAEHRRQGPVQPCQAMGQQQSAQQGQQQPHPGNAQHRLAGAI\n" +
                    "GGQAQVEASVEQHQAHEQPHHGLETVPEVQGLHQPEARPADQQAGAEQQHHPGQAGEPGQ\n" +
                    "SLGDGSGKDRDPPEQTEPLRGHASSTALNRGSTWMPITAEGRTTMYSPSSSEIGTFTKVS\n" +
                    "GADAVSTPLYQSWKDWRVGKVTDCVCPPLMCRWTL\n";

    public HmmerTest() {
    }

    public void start() {
    }

    public void stop() {
    }

    public void createFastaNode_seqpath_usr_name_desc_type(String seqPath, String username, String name,
                                                           String description, String sequenceType) {
        try {
            File seqFile = new File(seqPath);
            if (!seqFile.exists()) {
                throw new Exception("Could not find file=" + seqFile.getAbsoluteFile());
            }
            StringBuffer sb = new StringBuffer();
            FileReader fr = new FileReader(seqFile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            long nodeId = createFastaNodeId_seq_usr_name_desc_type(sb.toString(), username, name, description,
                    sequenceType);
            logger.info("Created new fasta file node id=" + nodeId);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected long createFastaNodeId_seq_usr_name_desc_type(String fastaSequence, String username, String name, String description, String sequenceType) throws Exception {
        if (!fastaSequence.startsWith(">"))
            throw new Exception("Fasta entry must have defline");
        FastaFileNode queryNode = new FastaFileNode();
        queryNode.setLength((long) fastaSequence.length());
        queryNode.setSequenceType(sequenceType);
        int entryCount = fastaSequence.split(">").length;
        if (entryCount == 0) {
            throw new Exception("Could not parse fasta string=" + fastaSequence);
        }
        queryNode.setSequenceCount(entryCount);
        queryNode.setOwner(username);
        queryNode.setName(name);
        queryNode.setDescription(description);
        queryNode = (FastaFileNode) EJBFactory.getRemoteComputeBean().createNode(queryNode);
        File queryFile = new File(queryNode.getFilePathByTag(FastaFileNode.TAG_FASTA));
        File parentDir = queryFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        FileWriter writer = new FileWriter(queryFile);
        writer.write(fastaSequence);
        writer.close();
        return queryNode.getObjectId();
    }

    public void submitHmmpfamSmallSingleTest() {
        try {
            logger.info("Starting submitHmmpfamSmallSingleTest()");
            Long queryNodeId = createFastaNodeId_seq_usr_name_desc_type(
                    NOT5_SUBUNIT_PROTEIN_SEQUENCE, "smurphy", "hmmpfam test", "hmmpfam test sequence",
                    FastaFileNode.PEPTIDE);
            submitHmmpfamTIGRFAMTestByQueryNodeId(queryNodeId);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void submitHmmpfamTIGRFAMTestByQueryNodeId(long queryNodeId) {
        try {
            HmmpfamTask hmmpfamTask = new HmmpfamTask();
            hmmpfamTask.setParameter(HmmpfamTask.PARAM_query_node_id, "" + queryNodeId);
            hmmpfamTask.setParameter(HmmpfamTask.PARAM_pfam_db_node_id, TIGRFAM_NODE_ID);
            hmmpfamTask.setOwner("smurphy");
            hmmpfamTask.setJobName("test for HmmerTestMBean submitHmmpfamTIGRFAMTestByQueryNodeId=" + queryNodeId);
            hmmpfamTask = (HmmpfamTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(hmmpfamTask);
            EJBFactory.getRemoteComputeBean().submitJob("HmmPfam", hmmpfamTask.getObjectId());
            logger.info("Task " + hmmpfamTask.getObjectId() + " submitted");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void createHmmpfamDatabaseNode(String databaseFilePath, String name, String description, int numberOfHmms) {
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            HmmerPfamDatabaseNode hmmDbNode = new HmmerPfamDatabaseNode();
            hmmDbNode.setOwner(User.SYSTEM_USER_LOGIN);
            hmmDbNode.setName(name);
            hmmDbNode.setDescription(description);
            hmmDbNode.setNumberOfHmms(numberOfHmms);
            hmmDbNode = (HmmerPfamDatabaseNode) computeBean.createNode(hmmDbNode);
            File hmmDbFile = new File(hmmDbNode.getFilePathByTag(HmmerPfamDatabaseNode.TAG_PFAM));
            File sourceFile = new File(databaseFilePath);
            if (!sourceFile.exists()) {
                throw new Exception("Specified source file does not exist: " + sourceFile.getAbsolutePath());
            }
            File parentDir = hmmDbFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            FileUtil.copyFileUsingSystemCall(sourceFile, hmmDbFile);
            logger.info("Created HmmpfamDatabaseNode id=" + hmmDbNode.getObjectId());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
