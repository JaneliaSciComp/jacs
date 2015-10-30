
package org.janelia.it.jacs.compute.service.recruitment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Scanner;

/**
 * @author Todd Safford
 */
public class FRVRecruitFromBlastService implements IService {

    private Task task;
    private ComputeDAO computeDAO;
    protected Logger _logger;
    private String headerFileName = SystemConfigurationProperties.getString("RecruitmentViewer.HeaderFile.Name");

    public void execute(IProcessData processData) throws ServiceException {
        try {
            init(processData);
            String perlModulePath = SystemConfigurationProperties.getString("Executables.ModuleBase") + SystemConfigurationProperties.getString("RecruitmentViewer.PerlBaseDir");
            String perlBinPath = SystemConfigurationProperties.getString("Perl.Path");
            String perlRecruitmentProgram = SystemConfigurationProperties.getString("RecruitmentViewer.PerlRecruitmentProgram.Name");
            String cmdPrefix = "export PATH=$PATH:" + perlModulePath + ";export PERL5LIB=$PERL5LIB:" + perlModulePath + ";";
            RecruitmentFileNode recruitmentNode = (RecruitmentFileNode) computeDAO.getNodeById(processData.getLong(ProcessDataConstants.RECRUITMENT_FILE_NODE_ID));
            // Assumes only one blast result node
            BlastResultFileNode inputNode = (BlastResultFileNode) task.getInputNodes().iterator().next();
            File[] outputDirs = (new File(inputNode.getDirectoryPath()).listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith("r_");
                }
            }));
            buildLocalMateInformationFile(outputDirs, recruitmentNode.getDirectoryPath());

            // Now run the Perl script
            FileWriter writer = new FileWriter(recruitmentNode.getDirectoryPath() + File.separator + "recScript.sh");
            try {
                String tmpCommand = "chmod ugo+r " + recruitmentNode.getDirectoryPath() + File.separator + headerFileName;
                SystemCall cmd = new SystemCall(_logger);
                cmd.emulateCommandLine(tmpCommand, true);
                writer.write(tmpCommand + "\n");
                tmpCommand = cmdPrefix+perlBinPath + " " + perlModulePath + File.separator + perlRecruitmentProgram + " -B " + recruitmentNode.getDirectoryPath() +
                        " -O " + recruitmentNode.getDirectoryPath() + " -P " + perlModulePath;
                cmd.emulateCommandLine(tmpCommand, true);
                writer.write(tmpCommand + "\n");
            }
            finally {
                writer.close();
            }
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }


    /**
     * This method looks through the blast output and builds a lookup file of rows with: read acc, sample acc, mate acc
     *
     * @param outputDirs               - path to the blast results we'll get the read accs from
     * @param recruitmentDirectoryPath - path to the place the file should go
     * @throws org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException
     *          - problem building the all.headerfile
     */
    // todo This needs to get absorbed into BlastXmlToFrvTabService.  It is already parsing this data!  Originally, the former Service didn't exist.
    private void buildLocalMateInformationFile(File[] outputDirs, String recruitmentDirectoryPath) throws CreateRecruitmentFileNodeException {
        // File created: all.header
        // ><JCVI READ ACC> /src = <sample name> /mate=<JCVI MATE ACC>
        // NOTE: Case sensitivity could be an issue.
        String OPEN_HIT_DEF = "<Hit_def>";
        //String CLOSE_HIT_DEF= "</Hit_def>";
        FileWriter writer = null;
        try {
            writer = new FileWriter(new File(recruitmentDirectoryPath + File.separator + headerFileName));
            for (File tmpDir : outputDirs) {
                File[] outputFiles = tmpDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return (name.startsWith("blast.") && !name.endsWith(".oos"));
                    }
                });

                // Get the accession data from all output files
                if (null != outputFiles) {
                    for (File outputFile : outputFiles) {
                        Scanner scanner = new Scanner(outputFile);
                        try {
                            while (scanner.hasNextLine()) {
                                // todo Remove duplicate reads if they multiply align!!!!!!!!
                                // NOTE: This loop is to compare (read accession - sample - mate accession) ONLY and SKIPS individual HSPs
                                // The query below looks for distinctness within the 5000 item chunk, but not across partitions.
                                // That could lead to redundant queries and items across partitions.
                                String line = scanner.nextLine().trim();
                                if (line.startsWith(OPEN_HIT_DEF)) {
                                    // We need data in this format:
                                    // ><JCVI READ ACC>/ src=<sample name>/ mate=<JCVI MATE ACC>
                                    String readAcc = getReadAccession(line);
                                    String sampleName = getSampleName(line);
                                    String mateAcc = getMateAcc(line);

                                    String newLine = ">" + readAcc + " /src=" + sampleName + " /mate=" + mateAcc + "\n";
                                    writer.write(newLine);
                                }
                            }
                        }
                        finally {
                            scanner.close();
                        }
                    }
                }
                else {
                    System.out.println("ERROR: There are no blast output files to parse.");
                }
            }
        }
        catch (Throwable e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
        finally {
            if (null != writer) {
                try {
                    writer.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getReadAccession(String line) {
        String OPEN_HIT_DEF = "<Hit_def>";
        int endPoint;
        // If the defline has attribute info, then try to figure out the accession
        // assuming the accession is to the first space found
        if (line.indexOf(" ") >= 0) {
            endPoint = line.indexOf(" ");
        }
        // if no attributes, then assume the entire line is unique
        else {
            endPoint = line.indexOf("</Hit_def>");
        }
        return line.substring(line.indexOf(OPEN_HIT_DEF) + OPEN_HIT_DEF.length(), endPoint).trim();
    }

    private String getSampleName(String line) {
        String sampleName = RecruitmentDataHelper.DEFLINE_SAMPLE_NAME;
        if (line.indexOf(sampleName) < 0) {
            return "";
        }
        String tmpLine = line.substring(line.indexOf(sampleName) + sampleName.length());
        int nextSpace = tmpLine.indexOf(" ");
        int nextElement = tmpLine.indexOf("<");
        return tmpLine.substring(0, (nextSpace>0 && nextSpace<nextElement)?nextSpace:nextElement).trim();
    }

    private String getMateAcc(String line) {
        String mate = "/mate=";
        if (line.indexOf(mate) < 0) {
            return "";
        }
        String tmpLine = line.substring(line.indexOf(mate) + mate.length());
        int nextSpace = tmpLine.indexOf(" ");
        int nextElement = tmpLine.indexOf("<");
        return tmpLine.substring(0, (nextSpace>0 && nextSpace<nextElement)?nextSpace:nextElement).trim();
    }

    protected void init(IProcessData processData) throws MissingDataException, IOException {
        _logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        this.task = ProcessDataHelper.getTask(processData);
        computeDAO = new ComputeDAO(_logger);
    }

//    public static void main(String[] args) {
//        // File created: all.header
//        // ><JCVI READ ACC> /src = <sample name> /mate=<JCVI MATE ACC>
//        // NOTE: Case sensitivity could be an issue.
//        String OPEN_HIT_DEF = "<Hit_def>";
//        //String CLOSE_HIT_DEF= "</Hit_def>";
//        FileWriter writer = null;
//        try {
//            writer = new FileWriter(new File("S:\\runtime-shared\\filestore\\system\\Recruitment\\1512584969380167973\\all.headers"));
//            File[] outputDirs = new File[]{new File("S:\\runtime-shared\\filestore\\system\\BlastResults\\1512537473882784037\\r_nucleotide.fasta.q1_q190")};
//            for (File tmpDir : outputDirs) {
//                File[] outputFiles = tmpDir.listFiles(new FilenameFilter() {
//                    public boolean accept(File dir, String name) {
//                        return (name.startsWith("blast.") && !name.endsWith(".oos"));
//                    }
//                });
//
//                // Get the accession data from all output files
//                if (null != outputFiles) {
//                    for (File outputFile : outputFiles) {
//                        Scanner scanner = new Scanner(outputFile);
//                        try {
//                            while (scanner.hasNextLine()) {
//                                // NOTE: This loop is to compare (read accession - sample - mate accession) ONLY and SKIPS individual HSPs
//                                // The query below looks for distinctness within the 5000 item chunk, but not across partitions.
//                                // That could lead to redundant queries and items across partitions.
//                                String line = scanner.nextLine().trim();
//                                if (line.startsWith(OPEN_HIT_DEF)) {
//                                    // We need data in this format:
//                                    // ><JCVI READ ACC>/ src=<sample name>/ mate=<JCVI MATE ACC>
//                                    int endPoint;
//                                    // If the defline has attribute info, then try to figure out the accession
//                                    if (line.indexOf(" ") >= 0) {
//                                        endPoint = line.indexOf(" ");
//                                    }
//                                    // if no attributes, then assume the entire line is unique
//                                    else {
//                                        endPoint = line.indexOf("</Hit_def>");
//                                    }
//                                    String readAcc = line.substring(line.indexOf(OPEN_HIT_DEF) + OPEN_HIT_DEF.length(), endPoint).trim();
//
//                                    String sampleNameField = RecruitmentDataHelper.DEFLINE_SAMPLE_NAME;
//                                    String sampleName = "";
//                                    if (line.indexOf(sampleNameField) < 0) {
//                                        sampleName = "";
//                                    }
//                                    else {
//                                        String tmpLine = line.substring(line.indexOf(sampleNameField) + sampleNameField.length());
//                                        int nextSpace = tmpLine.indexOf(" ");
//                                        int nextElement = tmpLine.indexOf("<");
//                                        sampleName = tmpLine.substring(0, (nextSpace>0 && nextSpace<nextElement)?nextSpace:nextElement).trim();
//                                    }
//
//                                    String mate = "/mate=";
//                                    String mateAcc = "";
//                                    if (line.indexOf(mate) < 0) {
//                                        mateAcc = "";
//                                    }
//                                    else {
//                                        String tmpLine = line.substring(line.indexOf(mate) + mate.length());
//                                        int nextSpace = tmpLine.indexOf(" ");
//                                        int nextElement = tmpLine.indexOf("<");
//                                        mateAcc = tmpLine.substring(0, (nextSpace>0 && nextSpace<nextElement)?nextSpace:nextElement).trim();
//                                    }
//
//                                    String newLine = ">" + readAcc + " /src=" + sampleName + " /mate=" + mateAcc + "\n";
//                                    writer.write(newLine);
//                                }
//                            }
//                        }
//                        finally {
//                            scanner.close();
//                        }
//                    }
//                }
//                else {
//                    System.out.println("ERROR: There are no blast output files to parse.");
//                }
//            }
//        }
//        catch (Throwable e) {
//            e.getStackTrace();
//        }
//        finally {
//            if (null != writer) {
//                try {
//                    writer.close();
//                }
//                catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
}