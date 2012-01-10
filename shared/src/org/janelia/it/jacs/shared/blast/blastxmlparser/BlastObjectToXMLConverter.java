
package org.janelia.it.jacs.shared.blast.blastxmlparser;

import org.apache.log4j.PropertyConfigurator;
import org.janelia.it.jacs.model.tasks.blast.IBlastOutputFormatTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.shared.blast.ParsedBlastResultCollection;
import org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb.BlastOutputType;
import org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb.Iteration;
import org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb.IterationType;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 11, 2008
 * Time: 2:37:13 PM
 */
public class BlastObjectToXMLConverter {
    File blastOutputDir;
    BigInteger lastIteration;
    BigInteger expectedIterations;
    boolean writeTop = false;
    boolean writeBottom = false;

    public static final String BEGINING = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<!DOCTYPE BlastOutput PUBLIC \"-//NCBI//NCBI BlastOutput/EN\" \"http://www.ncbi.nlm.nih.gov/dtd/NCBI_BlastOutput.dtd\">\n<BlastOutput>\n";
    public static final String END = "\n</BlastOutput>";


    public static final boolean FORMAT_OUTPUT = true;  // necessary to be readline-friendly for parsers
    public static final boolean INCLUDE_XML_DECLARATION_IN_CHUNCKS = false;  // cannot have multiple xml declarations in one xml file !!!

    public BlastObjectToXMLConverter(File blastOutputDir, BigInteger lastIteration, BigInteger expectedIterations,
                                     boolean writeTop, boolean writeBottom) {
        this.blastOutputDir = blastOutputDir;
        this.lastIteration = lastIteration;
        this.expectedIterations = expectedIterations;
        this.writeTop = writeTop;
        this.writeBottom = writeBottom;
    }

    public void process() throws Exception {
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(FileUtil.ensureFileExists(getResultFilePath())), "UTF8"));
        BlastXMLWriter blastXMLWriter = new BlastXMLWriter();
        ParsedBlastResultCollection parsedBlastCollection = getParsedBlastCollection();
        IBlastOutputFormatTask blastTask = getBlastTask();
        blastXMLWriter.setBlastDataSources(parsedBlastCollection, blastTask);
        BlastOutputType jbBlastOutput = blastXMLWriter.getBlastOutput();
        if (writeTop) {
            System.out.println("Writing top...");
            writeTopPortion(jbBlastOutput, bufferedWriter, blastXMLWriter);
        }
        BigInteger finishedIterationCount = new BigInteger("0");
        if (null != parsedBlastCollection && 0 != parsedBlastCollection.size()) {
            finishedIterationCount = writeBody(jbBlastOutput, bufferedWriter, blastXMLWriter, lastIteration);
        }
        if (writeBottom) {
            System.out.println("Writing bottom...");
            writeBottomPortion(bufferedWriter);
        }
        bufferedWriter.close();
        BigInteger actualIterations = finishedIterationCount.subtract(lastIteration);
        if (actualIterations.longValue() != expectedIterations.longValue()) {
            String errMsg = "IterationCount=" + actualIterations + " but expected " + expectedIterations;
            throw new Exception(errMsg);
        }
    }

    private ParsedBlastResultCollection getParsedBlastCollection() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(blastOutputDir.getAbsolutePath() + File.separator + BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_FILENAME));
        ParsedBlastResultCollection parsedBlastCollection = (ParsedBlastResultCollection) ois.readObject();
        ois.close();
        return parsedBlastCollection;
    }

    private IBlastOutputFormatTask getBlastTask() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(blastOutputDir.getAbsolutePath() + File.separator + BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_BASENAME + ".blastTask"));
        IBlastOutputFormatTask blastTask = (IBlastOutputFormatTask) ois.readObject();
        ois.close();
        return blastTask;
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
    private BigInteger writeBody(BlastOutputType jbBlastOutput, BufferedWriter bufferedWriter,
                                 BlastXMLWriter blastXMLWriter, BigInteger currenIterationCount) throws Exception {
        Iteration iterations = getIterations(jbBlastOutput, currenIterationCount);
        BigInteger newIterationCount = getLastIterationCount(iterations);
        blastXMLWriter.serialize(bufferedWriter, iterations, FORMAT_OUTPUT, INCLUDE_XML_DECLARATION_IN_CHUNCKS);
        return newIterationCount;
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
            lastIterationCountVal = lastIterationCount.longValue(); // fixed but from prior version
        }
        for (Object aList : list) {
            IterationType iteration = (IterationType) aList;
            iteration.setIterationIterNum(new BigInteger(String.valueOf(++lastIterationCountVal)));
        }
        return iterations;
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
        return blastOutputDir.getAbsolutePath() + File.separator + BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_BASENAME + ".xml";
    }

    public static void writeTopPortion(BlastOutputType jbBlastOutput, BufferedWriter bufferedWriter, BlastXMLWriter blastXMLWriter) throws Exception {
        bufferedWriter.write(BEGINING);

        // Unfortunately, JAXB only outputs XMLSerializable objects.  Primitive wrapper
        // objects are not XMLSerializable .. so have to manually write these
        bufferedWriter.write("<BlastOutput_program>");
        bufferedWriter.write(jbBlastOutput.getBlastOutputProgram());
        bufferedWriter.write("</BlastOutput_program>");

        bufferedWriter.write("<BlastOutput_version>");
        bufferedWriter.write(jbBlastOutput.getBlastOutputVersion());
        bufferedWriter.write("</BlastOutput_version>");

        bufferedWriter.write("<BlastOutput_reference>");
        bufferedWriter.write(jbBlastOutput.getBlastOutputReference());
        bufferedWriter.write("</BlastOutput_reference>");

        bufferedWriter.write("<BlastOutput_db>");
        bufferedWriter.write(jbBlastOutput.getBlastOutputDb());
        bufferedWriter.write("</BlastOutput_db>");

        bufferedWriter.write("<BlastOutput_query-ID>");
        bufferedWriter.write(jbBlastOutput.getBlastOutputQueryID() == null ? "" : jbBlastOutput.getBlastOutputQueryID());
        bufferedWriter.write("</BlastOutput_query-ID>");

        bufferedWriter.write("<BlastOutput_query-def>");
        bufferedWriter.write(jbBlastOutput.getBlastOutputQueryDef() == null ? "" : jbBlastOutput.getBlastOutputQueryDef());
        bufferedWriter.write("</BlastOutput_query-def>");

        bufferedWriter.write("<BlastOutput_query-len>");
        bufferedWriter.write(jbBlastOutput.getBlastOutputQueryLen() == null ? "" : jbBlastOutput.getBlastOutputQueryLen().toString());
        bufferedWriter.write("</BlastOutput_query-len>");

        blastXMLWriter.serialize(bufferedWriter, jbBlastOutput.getBlastOutputParam(), FORMAT_OUTPUT, INCLUDE_XML_DECLARATION_IN_CHUNCKS);

    }

    public static void writeBottomPortion(BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write(END);
    }

    public static void main(String[] args) {
        configureLog4j();
        if (args.length < 3) {
            usage();
        }
        File blastDir = new File(args[0]);
        BigInteger lastIteration = new BigInteger(args[1]);
        BigInteger expectedIterations = new BigInteger(args[2]);
        boolean writeTop = false;
        boolean writeBottom = false;
        if (args.length > 3) {
            for (int i = 3; i < args.length; i++) {
                if (args[i].trim().equals("-first")) {
                    System.out.println("writeTop=true");
                    writeTop = true;
                }
                else if (args[i].trim().equals("-last")) {
                    System.out.println("writeBottom=true");
                    writeBottom = true;
                }
            }
        }
        if (!blastDir.exists() && !blastDir.isDirectory()) {
            System.err.println("Could not find directory " + blastDir.getAbsolutePath());
            System.exit(1);
        }
        BlastObjectToXMLConverter boc = new BlastObjectToXMLConverter(
                blastDir, lastIteration, expectedIterations, writeTop, writeBottom);
        try {
            boc.process();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public static void usage() {
        System.out.println("usage: <blast output directory> <start iteration> <expected iterations> [-first -last]");
    }

    private static void configureLog4j() {
        Properties log4jProps = new Properties();
        log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
        log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
        log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d [%t] %-5p %c - %m%n");
        log4jProps.setProperty("log4j.logger.org.jboss", "WARN");
        log4jProps.setProperty("log4j.logger.org.hibernate", "ERROR");
        log4jProps.setProperty("log4j.logger.org.janelia.it.jacs.model", "ERROR");
        PropertyConfigurator.configure(log4jProps);
    }

}
