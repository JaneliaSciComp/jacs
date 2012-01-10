
package org.janelia.it.jacs.compute.service.hmmer3;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobException;
import org.janelia.it.jacs.model.tasks.hmmer3.HMMER3Task;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.hmmer3.HMMER3ResultFileNode;
import org.janelia.it.jacs.model.user_data.hmmer3.Hmmer3DatabaseNode;

import java.io.*;
import java.util.List;
import java.util.Map;

public class HMMER3MergeService
        implements IService {
    private Logger logger;
    private List<File> queryFiles;
    private Map<File, List<File>> inputOutputFileListMap;
    private HMMER3ResultFileNode resultFileNode;
    protected ComputeDAO computeDAO;
    private String queryName;
    private String databaseName;

    public HMMER3MergeService() {
    }

    public void execute(IProcessData processData)
            throws SubmitJobException {
        try {
            init(processData);
            logger.info("Hmmer3MergeService start");
            mergeResultFiles();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new SubmitJobException(e);
        }
        logger.info("Hmmer3MergeService end");
    }

    protected void init(IProcessData processData)
            throws MissingDataException, IOException {
        logger = ProcessDataHelper.getLoggerForTask(processData, getClass());
        logger.debug("Hmmer3MergeService - init...");
        computeDAO = new ComputeDAO(logger);
        queryFiles = (List) processData.getMandatoryItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST);
        HMMER3Task hmmpfamTask = (HMMER3Task) ProcessDataHelper.getTask(processData);
        inputOutputFileListMap = (Map) processData.getMandatoryItem(FileServiceConstants.INPUT_OUTPUT_FILE_LIST_MAP);
        resultFileNode = (HMMER3ResultFileNode) ProcessDataHelper.getResultFileNode(processData);
        String queryNodeIdString = hmmpfamTask.getParameter(HMMER3Task.PARAM_query_node_id);
        String dbNodeIdString = hmmpfamTask.getParameter(HMMER3Task.PARAM_db_node_id);
        Long queryNodeId = new Long(queryNodeIdString);
        Long dbNodeId = new Long(dbNodeIdString);
        try {
            FastaFileNode queryNode = (FastaFileNode) computeDAO.genericLoad(FastaFileNode.class, queryNodeId);
            queryName = getNameFromNode(queryNode);
            Hmmer3DatabaseNode pfamdbNode = (Hmmer3DatabaseNode) computeDAO.genericLoad(Hmmer3DatabaseNode.class, dbNodeId);
            databaseName = getNameFromNode(pfamdbNode);
        }
        catch (Exception e) {
            throw new MissingDataException("Could not obtain queryNode or dbNode information");
        }
    }

    private static String getNameFromNode(Node node) {
        String name = node.getName();
        if (name == null || name.trim().length() == 0) {
            name = node.getDescription();
            if (name == null || name.trim().length() == 0)
                name = (new StringBuilder()).append("Node_").append(node.getObjectId()).toString();
        }
        return name;
    }

    private void mergeResultFiles()
            throws Exception {

        // Merge output_file (the so-called 'raw' file)
        File resultFile = new File(resultFileNode.getFilePathByTag(HMMER3ResultFileNode.TAG_OUTPUT_FILE));
        Writer writer = new BufferedWriter(new FileWriter(resultFile));
        // ... meanwhile, also merge the dom file
        File domResultFile = new File(resultFileNode.getFilePathByTag(HMMER3ResultFileNode.TAG_PER_DOMAIN_HITS_FILE));
        Writer domWriter = new BufferedWriter(new FileWriter(domResultFile));
        // ... and may as well merge the SeqHits file
        File seqHitsResultFile = new File(resultFileNode.getFilePathByTag(HMMER3ResultFileNode.TAG_PER_SEQ_HITS_FILE));
        Writer seqHitsWriter = new BufferedWriter(new FileWriter(seqHitsResultFile));
           
        boolean initialFile = true;
        long hitCount = 0L;
        
        for (File queryFile : queryFiles) {
            List<File> outputFiles = inputOutputFileListMap.get(queryFile);
            if (outputFiles == null)
                throw new Exception((new StringBuilder()).append("Unexpectedly found no output files in processData hash for query file=").append(queryFile.getAbsolutePath()).toString());
            for (File outputFile : outputFiles) {

                // merge 'raw'
                hitCount += processOutputFile(writer, outputFile, initialFile);

                // Get Dom and SeqHits file names based on outputFile
                String outputFilePath = outputFile.getAbsolutePath();
                int pos = outputFilePath.lastIndexOf('.');
                String domFilePath = outputFilePath.substring(0,pos) + "DomainHits.tblr_0";
                File domFile = new File (domFilePath);
                String seqHitsFilePath = outputFilePath.substring(0,pos) + "SeqHits.tblr_0";
                File seqHitsFile = new File(seqHitsFilePath);

                // Now merge the dom and seqHits output files
                processTblFile(domWriter, domFile, initialFile);
                processTblFile(seqHitsWriter, seqHitsFile, initialFile);

                if (initialFile)
                    initialFile = false;
            }
        }

        writer.close();
        domWriter.close();
        seqHitsWriter.close();
        resultFileNode.setHitCount(hitCount);
        computeDAO.saveOrUpdate(resultFileNode);
    }

    private void processTblFile(Writer writer, File tblFile, boolean initialFile) throws Exception {

        BufferedReader reader;

        if (!tblFile.exists())
            throw new Exception((new StringBuilder()).append("Could not find initial dom file=").append(tblFile.getAbsolutePath()).toString());
        if (tblFile.length() == 0L)
            throw new Exception((new StringBuilder()).append("Initial dom file unexpectedly has zero size=").append(tblFile.getAbsolutePath()).toString());
        reader = new BufferedReader(new FileReader(tblFile));

        int lineCount = 0;
        String line;
        for (; (line = reader.readLine()) != null; lineCount++) {

            if (line.startsWith("#")) {
                if (lineCount > 4)  {
                    logger.info("Line Count: " + lineCount + "\nLine = \n" + line);
                    throw new Exception((new StringBuilder()).append("Could not parse beginning of initial HMMER3 dom file=").append(tblFile.getAbsolutePath()).toString());
                }
                if (initialFile) {
                    writer.write((new StringBuilder()).append(line).append("\n").toString());
                }

            } else {
                writer.write((new StringBuilder()).append(line).append("\n").toString());
            }



        }
        reader.close();
    }



    private long processOutputFile(Writer writer, File outputFile, boolean initialFile)
            throws Exception {
        BufferedReader reader;
        long hitCount;
        if (!outputFile.exists())
            throw new Exception((new StringBuilder()).append("Could not find initial output file=").append(outputFile.getAbsolutePath()).toString());
        if (outputFile.length() == 0L)
            throw new Exception((new StringBuilder()).append("Initial output file unexpectedly has zero size=").append(outputFile.getAbsolutePath()).toString());
        reader = new BufferedReader(new FileReader(outputFile));
        hitCount = 0L;
        int lineCount = 0;
        boolean initDone = false;
        boolean initStarted = false;
        boolean inScoreListMode = false;
        String line;
        for (; (line = reader.readLine()) != null; lineCount++) {
            if (lineCount > 25 && !initDone)
                throw new Exception((new StringBuilder()).append("Could not parse beginning of initial HMMER3 output file=").append(outputFile.getAbsolutePath()).toString());
            //logger.debug( "line[" + lineCount + "/" + line + "]" );
            if (!initStarted && line.startsWith("# hmmscan")) {
                initStarted = true;
                logger.debug( "line[" + lineCount + "/" + line + "] - START" );
                if (initialFile)
                    writer.write((new StringBuilder()).append(line).append("\n").toString());
                continue;
            }
            if (initStarted && !initDone) {
                if (initialFile) {
                    if (line.startsWith("Query:")) {
                        initDone = true;
                        logger.debug( "line[" + lineCount + "/" + line + "] - DONE-1" );
                        writer.write((new StringBuilder()).append(line).append("\n").toString());
                    }
                    else {
                        writer.write((new StringBuilder()).append(line).append("\n").toString());
                    }
                    continue;
                }
                if( line.startsWith( "# query sequence file:" ) ){
                    initDone = true;
                    logger.debug( "line[" + lineCount + "/" + line + "] - DONE-2" );
                }
                continue;
            }
            if( line.startsWith( "Scores for complete sequence (score includes all domains):" ) ){
                logger.debug( "line[" + lineCount + "/" + line + "] - ScoreListMode" );
                if (inScoreListMode)
                    throw new Exception((new StringBuilder()).append("Could not parse score section of file=").append(outputFile.getAbsolutePath()).toString());
                inScoreListMode = true;
                writer.write((new StringBuilder()).append(line).append("\n").toString());
                continue;
            }
            if (inScoreListMode) {
                String entryArr[] = line.split("\\s+");
                if ((!entryArr[0].equals("Model") || !entryArr[1].equals("Description")) && (!entryArr[0].startsWith("---") || !entryArr[1].startsWith("---")))
                    logger.debug( "line[" + lineCount + "/" + line + "] - ScoreListMode - Model/Description" );
                    if (entryArr.length > 4)
                        hitCount++;
                    else
                        inScoreListMode = false;
                writer.write((new StringBuilder()).append(line).append("\n").toString());
            }
            else {
                writer.write((new StringBuilder()).append(line).append("\n").toString());
            }
        }

        reader.close();
        return hitCount;
    }

}
