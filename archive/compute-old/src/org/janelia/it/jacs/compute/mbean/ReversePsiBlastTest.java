
package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.tasks.psiBlast.ReversePsiBlastTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;

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
public class ReversePsiBlastTest implements ReversePsiBlastTestMBean {

    private static final Logger logger = Logger.getLogger(ReversePsiBlastTest.class);

    private static final String PFAM_PROFILES_NODE_ID = "1281708109705773435";
    private static final String PROJECT_ID = "600002";

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

    public ReversePsiBlastTest() {
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
        // User user=EJBFactory.getRemoteComputeBean().getUserByName(username);
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

    public void submitReversePsiBlastSmallSingleTest() {
        try {
            logger.info("Starting submitReversePsiBlastSmallSingleTest()");
            Long queryNodeId = createFastaNodeId_seq_usr_name_desc_type(
                    NOT5_SUBUNIT_PROTEIN_SEQUENCE, "aresnick", "reversePsiBlast test", "reversePsiBlast test sequence",
                    FastaFileNode.PEPTIDE);
            submitReversePsiBlastTestByQueryNodeId(queryNodeId);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void submitReversePsiBlastTestByQueryNodeId(long queryNodeId) {
        try {
            ReversePsiBlastTask reversePsiBlastTask = new ReversePsiBlastTask();
            reversePsiBlastTask.setParameter(ReversePsiBlastTask.PARAM_query_node_id, "" + queryNodeId);
            reversePsiBlastTask.setParameter(ReversePsiBlastTask.PARAM_subjectDatabases, "" + PFAM_PROFILES_NODE_ID);
            reversePsiBlastTask.setParameter(ReversePsiBlastTask.PARAM_querySequenceProtein, Boolean.TRUE.toString());
            reversePsiBlastTask.setParameter(ReversePsiBlastTask.PARAM_project, PROJECT_ID);
            // I (aresnick) am not an existing camera user?
            reversePsiBlastTask.setOwner("aresnick");
            reversePsiBlastTask.setJobName("test for ReversePsiBlastTestMBean submitReversePsiBlastTestByQueryNodeId=" + queryNodeId);
            reversePsiBlastTask = (ReversePsiBlastTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(reversePsiBlastTask);
            EJBFactory.getRemoteComputeBean().submitJob("ReversePsiBlast", reversePsiBlastTask.getObjectId());
            logger.info("Task " + reversePsiBlastTask.getObjectId() + " submitted");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* don't think I need this...
    public void createReversePsiDatabaseNode(String databaseFilePath, String name, String description, int numberOfHmms) {
        try {
            ComputeBeanRemote computeBean=EJBFactory.getRemoteComputeBean();
            User systemUser=computeBean.getUserByName(User.SYSTEM_USER_LOGIN);
            ReversePsiBlastDatabaseNode reversePsiBlastDatabaseNode=new ReversePsiBlastDatabaseNode();
            reversePsiBlastDatabaseNode.setUser(systemUser);
            reversePsiBlastDatabaseNode.setName(name);
            reversePsiBlastDatabaseNode.setDescription(description);
            reversePsiBlastDatabaseNode=(ReversePsiBlastDatabaseNode)computeBean.createNode(reversePsiBlastDatabaseNode);
            File reversePsiDbFile=new File(reversePsiBlastDatabaseNode.getFilePathByTag(ReversePsiBlastDatabaseNode.TAG_RPSDB));
            File sourceFile=new File(databaseFilePath);
            if (!sourceFile.exists()) {
                throw new Exception("Specified source file does not exist: "+sourceFile.getAbsolutePath());
            }
            File parentDir= reversePsiDbFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdir();
            }
            FileUtil.copyFileUsingSystemCall(sourceFile,  reversePsiDbFile);
            logger.info("Created ReversePsiDatabaseNode id="+reversePsiBlastDatabaseNode.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    */
}