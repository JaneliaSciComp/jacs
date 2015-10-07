
package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 15, 2006
 * Time: 1:04:46 PM
 */
public interface RecruitmentNodeManagerMBean {

    // older methods not used in a while.  would need verification before using
    public void checkAllSubdirectoriesForErrors(String pathToSystemRecruitmentFileDirectory);

    public void regenerateSystemRecruitedImages();

    public void regenerateSystemRecruitedImagesForSpecificNodeId(String nodeId);

    public void regenerateSystemRecruitmentData();

    public void regenerateSystemRecruitmentDataForSpecificNodeId(String nodeId);

    // Method used to generate the sample.info file used by everyone.  Can be used on-the-fly
    public void generateSampleInfoFile();

    // Current methods to manage the Genome Project to FRV data pipeline 
    public void genomeProjectBlastFrvServiceForEachDataset();

    public void updateSystemRecruitmentDataWithNewBlastDatabases(String commaSeparatedListOfBlastDatabaseNodeIds);

    public void blastFrvASingleGenbankFile(String pathToGenbankFile, String ownerLogin);

    public void updateDataSetFastaFiles();

    // Test combined hits files
    public void profileCombinedHitsFile(String pathToCombinedHitsFile);

    public void recombineHitsFiles(boolean debugOnly);

    //    public void recruitHMPData() throws Exception;
    public void updateNumRecruitmentData();
//    public void accCheck();

    public void blastPartitionCheck(String pathToBlastDirectories);

    public void runRecruitmentSamplingTask(String blastDbIds, String owner) throws Exception;

    public void buildRecruitmentSamplingFasta();

    public void buildRecruitmentSamplingBlastDatabases(String originalNucleotideBlastDbIds, String samplingDbName,
                                                       String samplingDbDescription);

    public void recruitBlastOutput(String blastResultNodeId, String owner);

    public void generateRecruitmentStatistics(String recruitmentNodeId);
    public void testGPParsing(String filePath);
    public void fileCheck(String path);
    public void testUsers(String pathUserFile);
    public void restartSamplingRecruitmentRuns(String pathToRecruitmentTasksTabFile, boolean restartJobs);

}
