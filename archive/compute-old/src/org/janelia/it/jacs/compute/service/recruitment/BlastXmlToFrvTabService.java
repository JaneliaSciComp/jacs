
package org.janelia.it.jacs.compute.service.recruitment;

import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 10, 2009
 * Time: 11:38:40 AM
 */
public class BlastXmlToFrvTabService implements IService {
    public static final String BLAST_OUTPUT_PROGRAM_OPEN = "<BlastOutput_program>";
    public static final String HSP_OPEN = "<Hsp>";
    public static final String HIT_DEF_OPEN = "<Hit_def>";
    public static final String HIT_LENGTH_OPEN = "<Hit_len>";
    public static final String HSP_SCORE_OPEN = "<Hsp_score>";
    public static final String HSP_EVALUE_OPEN = "<Hsp_evalue>";
    public static final String HSP_HIT_FROM_OPEN = "<Hsp_hit-from>";
    public static final String HSP_HIT_TO_OPEN = "<Hsp_hit-to>";
    public static final String HSP_QUERY_FRAME_OPEN = "<Hsp_query-frame>";
    public static final String HSP_HIT_FRAME_OPEN = "<Hsp_hit-frame>";
    public static final String HSP_IDENTITY_OPEN = "<Hsp_identity>";
    public static final String HSP_POSITIVE_OPEN = "<Hsp_positive>";
    public static final String HSP_GAPS_OPEN = "<Hsp_gaps>";
    public static final String HSP_ALIGN_LENGTH_OPEN = "<Hsp_align-len>";
    public static final String BLAST_OUTPUT_QUERY_DEF_OPEN = "<BlastOutput_query-def>";
    public static final String BLAST_OUTPUT_QUERY_LENGTH_OPEN = "<BlastOutput_query-len>";
    public static final String ITERATION_QUERY_DEF_OPEN = "<Iteration_query-def>";
    public static final String ITERATION_QUERY_LENGTH_OPEN = "<Iteration_query-len>";
    public static final String HSP_QUERY_FROM_OPEN = "<Hsp_query-from>";
    public static final String HSP_QUERY_TO_OPEN = "<Hsp_query-to>";
    public static final String HSP_CLOSE = "</Hsp>";
    private Task task;

    public void execute(IProcessData processData) throws ServiceException {
        // Get the values from the task
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        FileWriter writer = null;
        FileWriter originalWriter = null;
        try {
            // Prep for execution
            //Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);
            Double percentIdCutoff = null;
            String pidCutoff = processData.getString("percentIdCutoff");
            if (null!=pidCutoff && !"".equals(pidCutoff)) {
                percentIdCutoff = Double.valueOf(pidCutoff);
            }
            EJBFactory.getRemoteComputeBean().saveEvent(task.getObjectId(), "Transforming Blast Results", "Transforming Blast Results", new Date());
            RecruitmentFileNode resultNode = (RecruitmentFileNode) computeBean.getNodeById(processData.getLong(ProcessDataConstants.RECRUITMENT_FILE_NODE_ID));
            BlastResultFileNode blastInputNode = null;
            if (null != task.getInputNodes() && 1 == task.getInputNodes().size()) {
                blastInputNode = (BlastResultFileNode) task.getInputNodes().iterator().next();
            }
            // If the input node doesn't exist, look for the input node id
            if (null == blastInputNode && null != processData.getItem(ProcessDataConstants.INPUT_FILE_NODE_ID)) {
                blastInputNode = (BlastResultFileNode) EJBFactory.getRemoteComputeBean().getNodeById(processData.getLong(ProcessDataConstants.INPUT_FILE_NODE_ID));
            }
            // STEP 1: Go through the blast results and convert into the tab file
            File resultDirectory = new File(resultNode.getDirectoryPath());
            File blastCombinedFile = new File(resultDirectory.getAbsolutePath() + File.separator + "blast_comb_file");
            File originalBlastResultsFile = new File(resultDirectory.getAbsolutePath() + File.separator + "original_blast_results");
            if (blastCombinedFile.exists()) {
                boolean newFileSuccessful = blastCombinedFile.createNewFile();
                if (!newFileSuccessful) {
                    throw new ServiceException("Could not create a new blast results tab file.");
                }
            }
            writer = new FileWriter(blastCombinedFile);
            originalWriter = new FileWriter(originalBlastResultsFile);
            // Get all the result directories
            File[] blastDirList = new File(blastInputNode.getDirectoryPath()).listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("r_");
                }
            });
            // Loop through the result dirs and walk the blast result files
            for (File file : blastDirList) {
                File[] blastOutputFiles = file.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith("blast.outr_") && !name.endsWith(".oos");
                    }
                });
                // Loop through the blast results and resolve the offset with the begin and end locations
                for (File blastOutputFile : blastOutputFiles) {
                    Scanner scanner = new Scanner(blastOutputFile);
                    try {
                        /**
                         *    0) query id
                         *    1) score
                         *    2) query begin
                         *    3) query end
                         *    4) query strand
                         *    5) subject id
                         *    6) subject begin
                         *    7) subject end
                         *    8) subject strand
                         *    9) note
                         *    10) number of identities
                         *    11) number of similarities
                         *    12) length of alignment
                         *    13) number of gap characters in query
                         *    14) number of gap characters in subject
                         *    15) query length
                         *    16) subject length
                         *    17) number of non-alignable characters in query (N\u2019s or X\u2019s)
                         *    18) number of non-alignable characters in subject (N\u2019s or X\u2019s)
                         *    19) search type
                         *    20) number of stops in query
                         *    21) number of stops in subject
                         *    22) number of gaps in query
                         *    23) number of gaps in subject
                         **/
                        String currentQueryDefLine;
                        String queryId = "";
                        String sampleName = "";
                        int hspScore = -1;
                        int queryBegin = -1, queryEnd = -1;
                        String queryStrand = "UNK";
                        String subjectId = "";
                        int subjectBegin = -1, subjectEnd = -1;
                        String subjectStrand = "UNK";
                        String note = "NA";
                        // NOTE:  The 8 values below, which are 0, are never calculated
                        int numIdentities = -1, numSimilarities = -1, alignmentLength = -1, numQueryGapChars = 0, numSubjectGapChars = 0, queryLength = -1, subjectLength = -1;
                        int numNonAlignableQueryChars = 0, numNonAlignableSubjectChars = 0;
                        String searchType = "";
                        int numQueryStops = 0, numSubjectStops = 0, numQueryGaps = 0, numSubjectGaps = 0;

                        while (scanner.hasNextLine()) {
                            String nextLine = scanner.nextLine();
                            if (nextLine.indexOf(ITERATION_QUERY_DEF_OPEN) >= 0) {
                                currentQueryDefLine = nextLine.trim();
                                currentQueryDefLine = currentQueryDefLine.substring(ITERATION_QUERY_DEF_OPEN.length(), currentQueryDefLine.length() - (ITERATION_QUERY_DEF_OPEN.length() + 1));
                                String[] defPieces = currentQueryDefLine.split("\\s+");
                                queryId = defPieces[0];
                                //logger.debug("Iteration - currentDeflineId: "+currentDeflineId+", offset: "+offset);
                            }
                            else if (nextLine.indexOf(HSP_SCORE_OPEN) >= 0) {
                                hspScore = Integer.valueOf(nextLine.trim().substring(HSP_SCORE_OPEN.length(), nextLine.trim().length() - (HSP_SCORE_OPEN.length() + 1)));
                            }
                            else if (nextLine.indexOf(HSP_QUERY_FROM_OPEN) >= 0) {
                                queryBegin = Integer.valueOf(nextLine.trim().substring(HSP_QUERY_FROM_OPEN.length(), nextLine.trim().length() - (HSP_QUERY_FROM_OPEN.length() + 1)));
                            }
                            else if (nextLine.indexOf(HSP_QUERY_TO_OPEN) >= 0) {
                                queryEnd = Integer.valueOf(nextLine.trim().substring(HSP_QUERY_TO_OPEN.length(), nextLine.trim().length() - (HSP_QUERY_TO_OPEN.length() + 1)));
                            }
                            else if (nextLine.indexOf(HSP_QUERY_FRAME_OPEN) >= 0) {
                                queryStrand = nextLine.trim().substring(HSP_QUERY_FRAME_OPEN.length(), nextLine.trim().length() - (HSP_QUERY_FRAME_OPEN.length() + 1));
                            }
                            else if (nextLine.indexOf(HIT_DEF_OPEN) >= 0) {
                                String subjectDefline = nextLine.trim().substring(HIT_DEF_OPEN.length(), nextLine.trim().length() - (HIT_DEF_OPEN.length() + 1));
                                if (subjectDefline.indexOf(" ") > 0) {
                                    subjectId = getReadAccession(subjectDefline);
                                }
                                else {
                                    subjectId = subjectDefline;
                                }
                                sampleName = getSampleName(subjectDefline);
                            }
                            else if (nextLine.indexOf(HSP_HIT_FROM_OPEN) >= 0) {
                                subjectBegin = Integer.valueOf(nextLine.trim().substring(HSP_HIT_FROM_OPEN.length(), nextLine.trim().length() - (HSP_HIT_FROM_OPEN.length() + 1)));
                            }
                            else if (nextLine.indexOf(HSP_HIT_TO_OPEN) >= 0) {
                                subjectEnd = Integer.valueOf(nextLine.trim().substring(HSP_HIT_TO_OPEN.length(), nextLine.trim().length() - (HSP_HIT_TO_OPEN.length() + 1)));
                            }
                            else if (nextLine.indexOf(HSP_HIT_FRAME_OPEN) >= 0) {
                                subjectStrand = nextLine.trim().substring(HSP_HIT_FRAME_OPEN.length(), nextLine.trim().length() - (HSP_HIT_FRAME_OPEN.length() + 1));
                            }
                            else if (nextLine.indexOf(HSP_IDENTITY_OPEN) >= 0) {
                                numIdentities = Integer.valueOf(nextLine.trim().substring(HSP_IDENTITY_OPEN.length(), nextLine.trim().length() - (HSP_IDENTITY_OPEN.length() + 1)));
                            }
                            else if (nextLine.indexOf(HSP_POSITIVE_OPEN) >= 0) {
                                numSimilarities = Integer.valueOf(nextLine.trim().substring(HSP_POSITIVE_OPEN.length(), nextLine.trim().length() - (HSP_POSITIVE_OPEN.length() + 1)));
                            }
                            else if (nextLine.indexOf(HSP_ALIGN_LENGTH_OPEN) >= 0) {
                                alignmentLength = Integer.valueOf(nextLine.trim().substring(HSP_ALIGN_LENGTH_OPEN.length(), nextLine.trim().length() - (HSP_ALIGN_LENGTH_OPEN.length() + 1)));
                            }
                            else if (nextLine.indexOf(BLAST_OUTPUT_QUERY_LENGTH_OPEN) >= 0) {
                                queryLength = Integer.valueOf(nextLine.trim().substring(BLAST_OUTPUT_QUERY_LENGTH_OPEN.length(), nextLine.trim().length() - (BLAST_OUTPUT_QUERY_LENGTH_OPEN.length() + 1)));
                            }
                            else if (nextLine.indexOf(HIT_LENGTH_OPEN) >= 0) {
                                subjectLength = Integer.valueOf(nextLine.trim().substring(HIT_LENGTH_OPEN.length(), nextLine.trim().length() - (HIT_LENGTH_OPEN.length() + 1)));
                            }
                            else if (nextLine.indexOf(BLAST_OUTPUT_PROGRAM_OPEN) >= 0) {
                                searchType = nextLine.trim().substring(BLAST_OUTPUT_PROGRAM_OPEN.length(), nextLine.trim().length() - (BLAST_OUTPUT_PROGRAM_OPEN.length() + 1));
                            }
                            else if (nextLine.indexOf(HSP_CLOSE) >= 0) {
                                // Flush the HSP to the file
                                StringBuffer hspLine = new StringBuffer();
                                // Column 0
                                hspLine.append(queryId).append("\t");
                                // Column 1
                                hspLine.append(hspScore).append("\t");
                                // Resolve from-to, begin-end difference
                                if (queryBegin < queryEnd) {
                                    // Column 2
                                    hspLine.append(queryBegin).append("\t");
                                    // Column 3
                                    hspLine.append(queryEnd).append("\t");
                                }
                                else {
                                    // Column 2
                                    hspLine.append(queryEnd).append("\t");
                                    // Column 3
                                    hspLine.append(queryBegin).append("\t");
                                }
                                // Column 4
                                hspLine.append(queryStrand).append("\t");
                                // Column 5
                                hspLine.append(subjectId).append("\t");
                                if (subjectBegin < subjectEnd) {
                                    // Column 6
                                    hspLine.append(subjectBegin).append("\t");
                                    // Column 7
                                    hspLine.append(subjectEnd).append("\t");
                                }
                                else {
                                    // Column 6
                                    hspLine.append(subjectEnd).append("\t");
                                    // Column 7
                                    hspLine.append(subjectBegin).append("\t");
                                }
                                // Column 8
                                hspLine.append(subjectStrand).append("\t");
                                // Column 9
                                hspLine.append(note).append("\t");
                                // Column 10
                                hspLine.append(numIdentities).append("\t");
                                // Column 11
                                hspLine.append(numSimilarities).append("\t");
                                // Column 12
                                hspLine.append(alignmentLength).append("\t");
                                // Column 13
                                hspLine.append(numQueryGapChars).append("\t");
                                // Column 14
                                hspLine.append(numSubjectGapChars).append("\t");
                                // Column 15
                                hspLine.append(queryLength).append("\t");
                                // Column 16
                                hspLine.append(subjectLength).append("\t");
                                // Column 17
                                hspLine.append(numNonAlignableQueryChars).append("\t");
                                // Column 18
                                hspLine.append(numNonAlignableSubjectChars).append("\t");
                                // Column 19
                                hspLine.append(searchType).append("\t");
                                // Column 20
                                hspLine.append(numQueryStops).append("\t");
                                // Column 21
                                hspLine.append(numSubjectStops).append("\t");
                                // Column 22
                                hspLine.append(numQueryGaps).append("\t");
                                // Column 23
                                hspLine.append(numSubjectGaps).append("\t");
                                // Column 24
                                hspLine.append(sampleName).append("\t");
                                hspLine.append("\n");

                                // Check the percent identity cutoff - if nothing defined write to file, else check quality
                                double pid = (subjectLength<=0)?0:((double)numIdentities+(double)numNonAlignableQueryChars)/(double)subjectLength;
                                if (null==percentIdCutoff || percentIdCutoff<=pid) {
                                    // Finish and write
                                    writer.append(hspLine.toString());
                                }
                                originalWriter.append(hspLine.toString());
                            }
                        }
                    }
                    finally {
                        scanner.close();
                    }
                }
            }
        }
        catch (Exception e) {
            System.out.println("\n\n\nError resolving the blast data for user " + task.getOwner() + ", task=" + task.getObjectId() + "\nERROR:" + e.getMessage());
            // Try to record the error
            try {
                computeBean.saveEvent(task.getObjectId(), Event.ERROR_EVENT, "Error executing the FRV pipeline", new Date());
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
            throw new ServiceException(e);
        }
        finally {
            if (null != writer) {
                try {
                    writer.flush();
                    writer.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != originalWriter) {
                try {
                    originalWriter.flush();
                    originalWriter.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getReadAccession(String line) {
        int endPoint;
        // If the defline has attribute info, then try to figure out the accession
        if (line.indexOf(" ") >= 0) {
            endPoint = line.indexOf(" ");
        }
        // if no attributes, then assume the entire line is unique
        else {
            endPoint = line.length() - 1;
        }
        return line.substring(0, endPoint).trim();
    }

    private String getSampleName(String line) {
        String sampleName = RecruitmentDataHelper.DEFLINE_SAMPLE_NAME;
        if (line.indexOf(sampleName) < 0) {
            System.out.println("ERROR!!! - The reads in the blast db do not have "+RecruitmentDataHelper.DEFLINE_SAMPLE_NAME+" attributes!");
            return "Undetermined";
        }
        String tmpLine = line.substring(line.indexOf(sampleName) + sampleName.length());
        if (tmpLine.indexOf(" ")>0){
            return tmpLine.substring(0, tmpLine.indexOf(" ")).trim();
        }
        return tmpLine.trim();
    }


}