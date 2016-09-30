
package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.user_data.DataSource;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.shared.blast.CreateBlastDatabaseFromFastaTool;
import org.janelia.it.jacs.shared.blast.FormatDBTool;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import java.io.*;
import java.sql.Connection;
import java.util.Properties;
import java.util.Scanner;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 15, 2006
 * Time: 1:05:05 PM
 */
@Singleton
@Startup
@Remote(FileNodeManagerMBean.class)
public class FileNodeManager extends AbstractComponentMBean implements FileNodeManagerMBean {
    private static final Logger logger = Logger.getLogger(FileNodeManager.class);

    // Property Keys
    public static final String SHELL_PATH_PROP = "SystemCall.ShellPath";
    public static final String STREAM_DIRECTOR_PROP = "SystemCall.StreamDirector";
    public static final String PARTITION_SIZE_PROP = "BlastServer.PartitionSize";
    public static final String PARTITION_ENTRIES_PROP = "BlastServer.PartitionEntries";

    public static final String DEFAULT_VISIBILITY = Node.VISIBILITY_INACTIVE;

    public FileNodeManager() {
        super("jacs");
    }

    public void start() {
        //computeBean = EJBFactory.getRemoteComputeBean();
    }

    public void stop() {

    }

    /**
     * Method to create a blastable nucleotide database
     *
     * @param name        - unique name for the database
     * @param description - full description of the data within the database
     * @param sourcePath  - location of the original fasta file
     */
    public void createNucleotideBlastableDatabase(String name, String description, String sourcePath) {
        createBlastableDatabase(name, description, sourcePath, CreateBlastDatabaseFromFastaTool.NUCLEOTIDE);
    }

    /**
     * Method to create a blastable peptide database
     *
     * @param name        - unique name for the database
     * @param description - full description of the data within the database
     * @param sourcePath  - location of the original file
     */
    public void createPeptideBlastableDatabase(String name, String description, String sourcePath) {
        createBlastableDatabase(name, description, sourcePath, CreateBlastDatabaseFromFastaTool.PEPTIDE);
    }

    protected void createBlastableDatabase(String name, String description, String sourcePath, String type) {
        if (logger.isInfoEnabled()) logger.info("Starting createBlastableDatabase with source path: " + sourcePath);
        try {

            CreateBlastDatabaseFromFastaTool pf = new CreateBlastDatabaseFromFastaTool(logger);
            pf.setFastaFilePath(sourcePath);
            pf.setResidueType(type);
            BlastDatabaseFileNode bdfn = new BlastDatabaseFileNode(User.SYSTEM_USER_LOGIN, null/*createtask*/, name, description,
                    DEFAULT_VISIBILITY, type, null);
            bdfn.setDataSource(DataSource.getDataSourceByName(SystemConfigurationProperties.getString("Datasource.Default")));
            bdfn.setPartitionCount(0); // this is necessary for initialization, to be modified below
            bdfn = (BlastDatabaseFileNode) EJBFactory.getRemoteComputeBean().saveOrUpdateNode(bdfn);
            logger.info("new BlastDatabaseFileNode id=" + bdfn.getObjectId());
            pf.setOutputPath(bdfn.getDirectoryPath());
            pf.setPartitionPrefix(BlastDatabaseFileNode.PARTITION_PREFIX);
            pf.setPartitionSize(SystemConfigurationProperties.getLong(PARTITION_SIZE_PROP)); // experimental
            pf.setPartitionEntries(SystemConfigurationProperties.getLong(PARTITION_ENTRIES_PROP));
            Properties prop = new Properties();
            prop.setProperty(FormatDBTool.FORMATDB_PATH_PROP,
                    SystemConfigurationProperties.getString("Executables.ModuleBase")+
                        SystemConfigurationProperties.getString(FormatDBTool.FORMATDB_PATH_PROP));
            prop.setProperty(SystemCall.SCRATCH_DIR_PROP,
                    SystemConfigurationProperties.getString(SystemCall.SCRATCH_DIR_PROP));
            prop.setProperty(SystemCall.SHELL_PATH_PROP,
                    SystemConfigurationProperties.getString(SystemCall.SHELL_PATH_PROP));
            prop.setProperty(SystemCall.STREAM_DIRECTOR_PROP,
                    SystemConfigurationProperties.getString(SystemCall.STREAM_DIRECTOR_PROP));
            pf.setProperties(prop);
            pf.partition();
            bdfn.setPartitionCount(new Integer("" + pf.getNumPartitions()));
            bdfn.setLength(pf.getNumResidues());
            EJBFactory.getRemoteComputeBean().saveOrUpdateNode(bdfn);
            logger.debug("Done - createBlastableDatabase");
        }
        catch (Throwable t) {
            t.printStackTrace();
            logger.error("Error in createBlastableDatabase: " + t, t);
        }
    }

    public void createBlastDatabaseFileNodeMovePartitionFiles(String name, String description, String sourcePath, boolean isNucleotide,
                                                              Integer partitionCount, Long length) {
        if (logger.isInfoEnabled())
            logger.info("Starting createBlastDatabaseFileNodeCopyPartitions with source path: " + sourcePath);
        try {
            // Make the node and save it
            String type = (isNucleotide) ? BlastDatabaseFileNode.NUCLEOTIDE : BlastDatabaseFileNode.PEPTIDE;
            BlastDatabaseFileNode bdfn = new BlastDatabaseFileNode(User.SYSTEM_USER_LOGIN, null/*createtask*/, name, description,
                    DEFAULT_VISIBILITY, type, null);
            bdfn.setDataSource(DataSource.getDataSourceByName(SystemConfigurationProperties.getString("Datasource.Default")));
            bdfn.setPartitionCount(partitionCount); // this is necessary for initialization, to be modified below
            bdfn.setLength(length);
            bdfn = (BlastDatabaseFileNode) EJBFactory.getRemoteComputeBean().saveOrUpdateNode(bdfn);

            // Now make the dir and copy all the files over from the source
            File outputDir = new File(bdfn.getDirectoryPath());
            outputDir.mkdirs();
            File inputDir = new File(sourcePath);
            File[] inputFiles = inputDir.listFiles();
            for (File inputFile : inputFiles) {
                FileUtil.moveFileUsingSystemCall(inputFile, new File(outputDir + File.separator + inputFile.getName()));
            }
            System.out.println("\nFound " + inputFiles.length + " in " + inputDir.getAbsolutePath() + " and moved " +
                    outputDir.listFiles().length + " to " + outputDir.getAbsolutePath());
        }
        catch (Throwable t) {
            t.printStackTrace();
            logger.error("Error in createBlastDatabaseFileNodeCopyPartitions: " + t, t);
        }
    }


    protected void createFastaFileNode(String name, String description, String sourcePath, String type) {
        if (logger.isInfoEnabled()) logger.info("Starting createFastaFileNode() with source path: " + sourcePath);
        try {
            File sourceFile = new File(sourcePath);
            long[] sequenceCountAndTotalLength = FastaUtil.findSequenceCountAndTotalLength(sourceFile);
            FastaFileNode ffn = new FastaFileNode(User.SYSTEM_USER_LOGIN, null/*Task*/, name, description,
                    DEFAULT_VISIBILITY, type, (int) sequenceCountAndTotalLength[0], null);
            ffn.setLength(sequenceCountAndTotalLength[1]);
            ffn = (FastaFileNode) EJBFactory.getRemoteComputeBean().saveOrUpdateNode(ffn);
            File ffnDir = new File(ffn.getDirectoryPath());
            ffnDir.mkdirs();
            String copyCmd = "cp " + sourcePath + " " + ffn.getFastaFilePath();
            if (logger.isInfoEnabled()) logger.info("Executing: " + copyCmd);
            SystemCall call = new SystemCall(logger);
            int exitVal = call.emulateCommandLine(copyCmd, true);
            if (logger.isInfoEnabled()) logger.info("Exit value: " + exitVal);
        }
        catch (Throwable t) {
            t.printStackTrace();
            logger.error("Error in createFastaFileNode: " + t.getMessage(), t);
        }
    }

    public void createNucleotideFastaFileNode(String name, String description, String sourcePath) {
        createFastaFileNode(name, description, sourcePath, FastaFileNode.NUCLEOTIDE);
    }

    public void createPeptideFastaFileNode(String name, String description, String sourcePath) {
        createFastaFileNode(name, description, sourcePath, FastaFileNode.PEPTIDE);
    }

    protected User getDefaultUser() throws Exception {
        User fileNodeManagerUser = EJBFactory.getRemoteComputeBean().getUserByNameOrKey(User.SYSTEM_USER_LOGIN);
        if (fileNodeManagerUser == null) {
            fileNodeManagerUser = new User(User.SYSTEM_USER_LOGIN, User.SYSTEM_USER_LOGIN + "Fullname");
            EJBFactory.getRemoteComputeBean().saveOrUpdateSubject(fileNodeManagerUser);
        }
        return fileNodeManagerUser;
    }

    public void createEmptyBlastableDatabase_name_desc_type_length_pnum(String s) {
        String sa[] = s.split(",");
        String name = sa[0].trim();
        String description = sa[1].trim();
        String type = sa[2].trim();
        String length = sa[3].trim();
        String partitionNumber = sa[4].trim();
        try {
            BlastDatabaseFileNode bdfn = new BlastDatabaseFileNode(User.SYSTEM_USER_LOGIN, null/*createtask*/, name, description,
                    DEFAULT_VISIBILITY, type, null);
            bdfn.setLength(new Long(length));
            bdfn.setPartitionCount(new Integer(partitionNumber));
            EJBFactory.getRemoteComputeBean().saveOrUpdateNode(bdfn);
            if (logger.isInfoEnabled()) logger.info("Created Empty Blastable Database of name: " + name);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void createFastaFileNode_name_desc_path_type(String s) {
        String sa[] = s.split(",");
        String name = sa[0].trim();
        String description = sa[1].trim();
        String path = sa[2].trim();
        String type = sa[3].trim();
        createFastaFileNode(name, description, path, type);
    }

    public void createBlastableDatabase_name_desc_path_type(String s) {
        String sa[] = s.split(",");
        String name = sa[0].trim();
        String description = sa[1].trim();
        String path = sa[2].trim();
        String type = sa[3].trim();
        createBlastableDatabase(name, description, path, type);
    }

    public void createMultipleFastaFileNode_name_desc_path_type(String sPath) {
        BufferedReader br = null;
        try {
            File file = new File(sPath);
            br = new BufferedReader(new FileReader(file));
            String s;
            while ((s = br.readLine()) != null) {
                if (logger.isInfoEnabled()) logger.info("Creating fasta file persist for info: " + s);
                s = s.trim();
                String sa[] = s.split(",");
                String name = sa[0].trim();
                String description = sa[1].trim();
                String path = sa[2].trim();
                String type = sa[3].trim();
                createFastaFileNode(name, description, path, type);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (null != br) {
                try {
                    br.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void createMultipleBlastableDatabase_name_desc_path_type(String sPath) {
        BufferedReader br = null;
        try {
            File file = new File(sPath);
            br = new BufferedReader(new FileReader(file));
            String s;
            while ((s = br.readLine()) != null) {
                if (logger.isInfoEnabled()) logger.info("Creating blastable database persist for info: " + s);
                s = s.trim();
                String sa[] = s.split(",");
                String name = sa[0].trim();
                String description = sa[1].trim();
                String path = sa[2].trim();
                String type = sa[3].trim();
                createBlastableDatabase(name, description, path, type);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (null != br) {
                try {
                    br.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void gzipFileUtility(String pathToCompress) {
        try {
            SystemCall call = new SystemCall(logger);
            String copyCmd = "cp " + pathToCompress + " " + pathToCompress + ".copy";
            call.emulateCommandLine(copyCmd, true);
            String compressCmd = "gzip " + pathToCompress;
            call.emulateCommandLine(compressCmd, true);
            String revertCmd = "mv " + pathToCompress + ".copy " + pathToCompress;
            call.emulateCommandLine(revertCmd, true);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e, e);
        }
    }

    protected Connection getJdbcConnection() throws Exception {
        InitialContext ic = new InitialContext();
        javax.sql.DataSource ds;
        ds = (javax.sql.DataSource) ic.lookup("java:jboss/datasources/ComputeServer_DataSource");
        Connection connection = ds.getConnection();
        //connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        connection.setAutoCommit(false);
        return connection;
    }

    /*
        We can derive the number of sequences and the sequence length in a RPS DB as follows:

        sequence length = cat *.psq | wc -c
        minus number of sequences
        minus 1    
     */
//    public Long getReversePsiBlastDBSequenceLength(File reversePsiBlastDBDir) throws Exception {
//        // get sequence count
//        long sequenceCount = getReversePsiBlastDBSequenceCount(reversePsiBlastDBDir);
//
//        // get sequence file byte size
//        File targetDBFile = SubjectDBUtils.getSubjectDBFile(reversePsiBlastDBDir, ".psq");
//        long wordCount = getReversePsiBlastDBCount(targetDBFile, "wc -c");
//
//        return wordCount - sequenceCount - 1;
//    }

    /* db sequence count derivation option #1 using database .psd file

           number of sequences = cat *.psd | wc -l

           removed because reverse psi blast dbs not guaranteed to contain a .psd file
    public Long getReversePsiBlastDBSequenceCount(File reversePsiBlastDBDir) throws Exception {
        File targetDBFile = SubjectDBUtils.getSubjectDBFile(reversePsiBlastDBDir,".psd");
        return getReversePsiBlastDBCount(targetDBFile,"wc -l");
    }
    */

    /* db sequence count derivation option #2 using database .aux file

       my $content_count = `cat $dbpath/*.aux | wc -l`;
       $content_count = ( $content_count - 8 ) / 2;

     */

//    public Long getReversePsiBlastDBSequenceCount(File reversePsiBlastDBDir) throws Exception {
//
//        File targetDBFile = SubjectDBUtils.getSubjectDBFile(reversePsiBlastDBDir, ".aux");
//        Long fileCount = getReversePsiBlastDBCount(targetDBFile, "wc -l");
//        return (fileCount - 8) / 2;
//    }
//
//    private long getReversePsiBlastDBCount(File dbFile, String wordCountCommand) throws Exception {
//        String systemShell = SystemConfigurationProperties.getString(SystemCall.SHELL_PATH_PROP);
//        String[] cmd = {systemShell, "-c", "cat " + dbFile.getAbsolutePath() + " | " + wordCountCommand};
//        String fullCmd = cmd[0] + " " + cmd[1] + " " + cmd[2];
//        logger.debug("getReversePsiBlastDBCount using cmd=" + fullCmd);
//        Process process = Runtime.getRuntime().exec(cmd);
//        process.waitFor();
//        if (process.exitValue() != 0) {
//            throw new Exception("Error from cmd=" + fullCmd + " : " + getStringFromStream(process.getErrorStream()));
//        }
//        String valueString = getStringFromStream(process.getInputStream());
//        if (valueString == null || valueString.length() == 0) {
//            throw new Exception("Expected number rather than empty string from cmd=" + fullCmd);
//        }
//        return new Long(valueString);
//    }
//
//    public int getHmmCountFromHmmerDatabaseNode(File hmmDbFile) throws Exception {
//        if (!hmmDbFile.exists()) {
//            throw new Exception("Could not locate file=" + hmmDbFile);
//        }
//        String systemShell = SystemConfigurationProperties.getString(SystemCall.SHELL_PATH_PROP);
//        String[] cmd = {systemShell, "-c", "cat " + hmmDbFile.getAbsolutePath() + " | grep \"^NAME\" | wc -l"};
//        String fullCmd = cmd[0] + " " + cmd[1] + " " + cmd[2];
//        logger.debug("getHmmCountFromHmmerDatabaseNode using cmd=" + fullCmd);
//        Process process = Runtime.getRuntime().exec(cmd);
//        process.waitFor();
//        if (process.exitValue() != 0) {
//            throw new Exception("Error from cmd=" + fullCmd + " : " + getStringFromStream(process.getErrorStream()));
//        }
//        String valueString = getStringFromStream(process.getInputStream());
//        if (valueString == null || valueString.length() == 0) {
//            throw new Exception("Expected number rather than empty string from cmd=" + fullCmd);
//        }
//        return new Integer(valueString);
//    }

//    private static String getStringFromStream(InputStream inputStream) throws IOException {
//        StringBuilder strBuilder = new StringBuilder();
//        int ch;
//        if (inputStream != null) {
//            while ((ch = inputStream.read()) != -1) {
//                if (!Character.isWhitespace(ch)) {
//                    strBuilder.append((char) ch);
//                }
//            }
//            inputStream.close();
//        }
//        return strBuilder.toString();
//    }
//
    /**
     * This method was populated with data from the node table and then I looked in the filestore to see if the
     * blast database dirs still existed.  The dir att below was the node id (dir name) and the user told us where
     * to look in the filestore.
     */
    public void findDeprecatedBlastDBDirs() {
        try {
            String fileStore = SystemConfigurationProperties.getString("FileStore.CentralDir");
            File blastDBs = new File(fileStore + File.separator + "BlastDBs.txt");
            Scanner scanner = new Scanner(blastDBs);
            while (scanner.hasNextLine()) {
                String[] pieces = scanner.nextLine().split("\t");
                String dir = pieces[0];
                String user = pieces[3];
                File tmpDir = new File(fileStore + File.separator + user + File.separator + dir);
                if (!tmpDir.exists() || !tmpDir.isDirectory()) {
                    System.out.println("The blast database (" + tmpDir.getAbsolutePath() + ") does not exist.");
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
