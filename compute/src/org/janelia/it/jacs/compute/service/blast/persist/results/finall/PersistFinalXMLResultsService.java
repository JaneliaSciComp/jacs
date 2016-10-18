
package org.janelia.it.jacs.compute.service.blast.persist.results.finall;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.shared.blast.ParsedBlastResultCollection;
import org.janelia.it.jacs.shared.blast.blastxmlparser.BlastObjectToXMLConverter;
import org.janelia.it.jacs.shared.blast.blastxmlparser.BlastXMLWriter;
import org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb.BlastOutputType;
import org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb.Iteration;
import org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb.IterationType;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.math.BigInteger;
import java.util.List;
import org.janelia.it.jacs.model.tasks.blast.IBlastOutputFormatTask;

/**
 * This service persists the final blast results to the file system.
 *
 * @author Tareq Nabeel
 */
public class PersistFinalXMLResultsService implements IService {

    private Logger logger;

    private BlastTask blastTask;
    private List<File> blastDestOutputDirs;
    private BlastResultFileNode resultFileNode;

    public PersistFinalXMLResultsService() {
    }

    public void execute(IProcessData processData) throws PersistBlastResultsException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            init(processData);
            writeBlastResults();
            saveBlastHitCount();
        }
        catch (PersistBlastResultsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new PersistBlastResultsException(e);
        }
    }

    /**
     * This method deserializes the parsedBlastResultCollection in each blastOutputDir and used JAXB based BlastXMLWriter
     * to write them to a single blastResult.xml file
     *
     * @throws Exception
     */
    private void writeBlastResults() throws Exception {
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FileUtil.ensureFileExists(getResultFilePath())), "UTF8"));
        BigInteger currentIterationCount = null;
        try {
            for (int i = 0; i < blastDestOutputDirs.size(); i++) {
                logger.debug("Reading final xml results from dir=" + blastDestOutputDirs.get(i).getAbsolutePath());
                BlastXMLWriter blastXMLWriter = new BlastXMLWriter();
                // Can't afford deserializedParsedBlastResultCollectionreference outside loop
                // i.e. make sure deserializedParsedBlastResultCollection goes out of scope in each iteration
                // and gets eventually garbage collected ... otherwise you'll go out of memory
                ParsedBlastResultCollection deserializedParsedBlastResultCollection = deserializeParsedBlastResultCollection(blastDestOutputDirs.get(i));
                BlastOutputType jbBlastOutput = getBlastOutput(deserializedParsedBlastResultCollection, blastXMLWriter);
                if (isFirstIteration(i)) {
                    BlastObjectToXMLConverter.writeTopPortion(jbBlastOutput, bufferedWriter, blastXMLWriter);
                }
                if (!isParsedBlastResultCollectionEmpty(deserializedParsedBlastResultCollection)) {
                    logger.debug("writeBody startIteration=" + currentIterationCount);
                    currentIterationCount = writeBody(jbBlastOutput, bufferedWriter, blastXMLWriter, currentIterationCount);
                    logger.debug("writeBody endIteration=" + currentIterationCount);
                }
                if (isLastIteration(i)) {
                    BlastObjectToXMLConverter.writeBottomPortion(bufferedWriter);
                }
                bufferedWriter.flush();
            }
        }
        finally {
            bufferedWriter.close();
        }
    }

    /**
     * Writes BlastOutput_iterations to blastResults.xml.  Each parsedBlastResultCollection would start its
     * hits iterations at 1.  We cannot reset to 1 as we move from one parsedBlastResultCollection to the next,
     * so we keep track of currenIterationCount to achieve that.
     *
     * @param jbBlastOutput        The root JAXB object representing the deserialized parsedBlastResultCollection
     * @param bufferedWriter       Used to write to blastResults.xml
     * @param blastXMLWriter       Used to write JAXB objects to blastResults.xml
     * @param currenIterationCount The current hit iteration count
     * @return
     * @throws Exception
     */
    private BigInteger writeBody(BlastOutputType jbBlastOutput, BufferedWriter bufferedWriter, BlastXMLWriter blastXMLWriter, BigInteger currenIterationCount) throws Exception {
        Iteration iterations = getIterations(jbBlastOutput, currenIterationCount);
        BigInteger newIterationCount = getLastIterationCount(iterations);
        blastXMLWriter.serialize(bufferedWriter, iterations, BlastObjectToXMLConverter.FORMAT_OUTPUT,
                BlastObjectToXMLConverter.INCLUDE_XML_DECLARATION_IN_CHUNCKS);
        return newIterationCount;
    }

    /**
     * We cannot reset to 1 as we move from one parsedBlastResultCollection to the next,
     * so we keep track of currenIterationCount to achieve that.
     *
     * @param iterations
     * @return
     */
    private BigInteger getLastIterationCount(Iteration iterations) {
        IterationType iteration = iterations.getIteration().get(iterations.getIteration().size() - 1);
        return iteration.getIterationIterNum();
    }

    /**
     * Returns the absolute path to blast results xml file
     *
     * @return
     */
    private String getResultFilePath() {
        String resultFilePath = resultFileNode.getDirectoryPath() + File.separator + "blastResults.xml";
        if (logger.isInfoEnabled()) {
            logger.info("Writing out blast results to " + resultFilePath);
        }
        return resultFilePath;
    }

    /**
     * @param parsedBlastCollection
     * @return
     */
    private boolean isParsedBlastResultCollectionEmpty(ParsedBlastResultCollection parsedBlastCollection) {
        if (parsedBlastCollection.size() == 0) {
            if (logger.isInfoEnabled()) logger.info("parsedBlastResultCollection.size()==0");
            // FileUtil.copyFile(resultFileDir + FileUtil.FILE_SEPARATOR  + "blast.outr_0",resultFilePath);
            // Decided not to produce anything to be consistent with the scenario where one query produced hits and other did not
            // (we don't write anything for query with no hits)
            return true;
        }
        else {
            return false;
        }
    }

    private BlastOutputType getBlastOutput(ParsedBlastResultCollection parsedBlastCollection, BlastXMLWriter blastXMLWriter) throws Exception {
        blastXMLWriter.setBlastDataSources(parsedBlastCollection, (IBlastOutputFormatTask)blastTask);
        return blastXMLWriter.getBlastOutput();
    }

    private void saveBlastHitCount() throws IOException, DaoException {
        String totalBlastHitsFilePath = resultFileNode.getFilePathByTag(BlastResultFileNode.TAG_TOTAL_BLAST_HITS);
        RandomAccessFile totalBlastHitsFile = null;
        try {
            // Using RandomAccessFile because it offers read and write lock
            totalBlastHitsFile = new RandomAccessFile(totalBlastHitsFilePath, "r");
            long totalBlastHits = 0;
            if (totalBlastHitsFile.length() > 0) {
                totalBlastHits = Long.parseLong(totalBlastHitsFile.readUTF());
            }

            this.resultFileNode.setBlastHitCount(totalBlastHits);
            new ComputeDAO().genericSave(resultFileNode);
        }
        finally {
            if (null != totalBlastHitsFile) {
                totalBlastHitsFile.close();
            }
        }
    }

    /**
     * This method using the running iteration count for deserialized ParsedBlastResultCollections to modify
     * the iteratino count in the JAXB IterationType object and return the corrected IterationType instance
     * before it's serialized to blastResults.xml
     *
     * @param jbBlastOutput
     * @param lastIterationCount
     * @return
     */
    private Iteration getIterations(BlastOutputType jbBlastOutput, BigInteger lastIterationCount) {
        Iteration iterations = jbBlastOutput.getBlastOutputIterations();
        List list = iterations.getIteration();
        long lastIterationCountVal = 0;
        if (lastIterationCount != null) {
            lastIterationCountVal = lastIterationCount.longValue();  // Fixed prior bug here where assignment not happening
        }
        for (Object aList : list) {
            IterationType iteration = (IterationType) aList;
            iteration.setIterationIterNum(new BigInteger(String.valueOf(++lastIterationCountVal)));
        }
        return iterations;
    }

    protected void init(IProcessData processData) throws MissingDataException, IOException, ClassNotFoundException {
        blastDestOutputDirs = (List<File>) processData.getMandatoryItem(BlastProcessDataConstants.BLAST_DEST_OUTPUT_DIR);
        resultFileNode = (BlastResultFileNode) ProcessDataHelper.getResultFileNode(processData);
        blastTask = (BlastTask) ProcessDataHelper.getTask(processData);
    }

    private boolean isLastIteration(int i) {
        return i == (blastDestOutputDirs.size() - 1);
    }

    private boolean isFirstIteration(int i) {
        return (i == 0);
    }

    /**
     * ParsedBlastResultCollection is serialized to file system by MergeSortService because we're planning on moving MergeSort to grid
     * and because run PersistBlastResults Node or PersistBlastResultsXML services after MergeSort is done
     *
     * @param blastDestOutputDir
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private ParsedBlastResultCollection deserializeParsedBlastResultCollection(File blastDestOutputDir) throws IOException, ClassNotFoundException {
        //To change body of created methods use File | Settings | File Templates.
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(blastDestOutputDir.getAbsolutePath() + File.separator + BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_FILENAME));
        ParsedBlastResultCollection parsedBlastResultCollection;
        try {
            parsedBlastResultCollection = (ParsedBlastResultCollection) ois.readObject();
        }
        finally {
            ois.close();
        }
        return parsedBlastResultCollection;
    }


}
