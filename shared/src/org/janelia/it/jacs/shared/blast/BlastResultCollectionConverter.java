
package org.janelia.it.jacs.shared.blast;

import org.apache.log4j.PropertyConfigurator;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.tasks.blast.IBlastOutputFormatTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 11, 2008
 * Time: 2:37:13 PM
 * <p/>
 * This class is used to convert BlastResultCollection into multiple formats
 * based on input.
 * Currently supported formats are:
 * NCBI XML
 * NCBI Text
 * BTAB (JCVI's internal format)
 */
public class BlastResultCollectionConverter {

    public static final String KEY_BLAST_OUTPUT_DIR = "dir";
    public static final String KEY_START_INTERATION = "start";
    public static final String KEY_EXPECTED_INTERATIONS = "expect";
    public static final String KEY_IS_FIRST = "first";
    public static final String KEY_IS_LAST = "last";
    public static final String KEY_OUTPUT_FORMATS = "formats";

    BrccArgs brccArgs;

    public BlastResultCollectionConverter(BrccArgs args) {
        brccArgs = args;
    }

    /**
     * Need this constructor for unit test
     *
     * @param dir
     * @param last
     * @param expected
     * @param top
     * @param bottom
     * @param formats
     */
    public BlastResultCollectionConverter(File dir, long last, long expected, boolean top, boolean bottom,
                                          String[] formats) {
        StringBuffer formatList = new StringBuffer();
        for (int i = 0; i < formats.length; i++) {
            formatList.append(formats[i]);
            if (i < formats.length - 1)
                formatList.append(",");
        }


        brccArgs = new BrccArgs(new String[]
                {
                        KEY_BLAST_OUTPUT_DIR + "=" + dir.getAbsolutePath(),
                        KEY_EXPECTED_INTERATIONS + "=" + expected,
                        KEY_START_INTERATION + "=" + last,
                        KEY_IS_FIRST + "=" + (top ? "yes" : "no"),
                        KEY_IS_LAST + "=" + (bottom ? "yes" : "no"),
                        KEY_OUTPUT_FORMATS + "=" + formatList.toString()
                });
    }

    public void process() throws Exception {
        ParsedBlastResultCollection parsedBlastCollection = getParsedBlastCollection();
        List<Map<String, List<ParsedBlastResult>>> queryCollection = groupBy(parsedBlastCollection);
        IBlastOutputFormatTask blastTask = getBlastTask();

        // initialize writers
        List<BlastWriter> writers = new LinkedList<BlastWriter>();
        for (String outFormat : brccArgs.formats) {
            BlastWriter bw = BlastWriterFactory.getWriterForFormat(brccArgs.dir, outFormat);
            bw.setBlastDataSources(parsedBlastCollection, blastTask);
            writers.add(bw);
        }

        if (brccArgs.writeTop) {
            System.out.println("Writing top...");
            for (BlastWriter bw : writers)
                bw.writeTopPortion();
        }

        for (BlastWriter bw : writers)
            bw.startQueryPortion();

        long cnt = brccArgs.lastIteration.longValue() + 1;

        for (Map<String, List<ParsedBlastResult>> map : queryCollection) {

            String qID = map.keySet().iterator().next();
            // write out a single query set of hits
            for (BlastWriter bw : writers)
                bw.writeSingleQueryPortion(qID, map.get(qID), cnt);
            cnt++;
        }
        BigInteger lastIterationCounter = BigInteger.valueOf(cnt - 1);

        for (BlastWriter bw : writers)
            bw.endQueryPortion();

//        if (null!=parsedBlastCollection && 0!=parsedBlastCollection.size()) {
//            finishedIterationCount = writeBody(jbBlastOutput, bufferedWriter, blastXMLWriter, brccArgs.lastIteration);
//        }
        if (brccArgs.writeBottom) {
            System.out.println("Writing bottom...");
            for (BlastWriter bw : writers)
                bw.writeBottomPortion();
        }

        // finish serializing
        for (BlastWriter bw : writers)
            bw.finish();

        BigInteger actualIterations = lastIterationCounter.subtract(brccArgs.lastIteration);
        if (actualIterations.longValue() != brccArgs.expectedIterations.longValue()) {
            String errMsg = "IterationCount=" + actualIterations + " but expected " + brccArgs.expectedIterations;
            throw new Exception(errMsg);
        }
    }

    private ParsedBlastResultCollection getParsedBlastCollection() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(brccArgs.dir.getAbsolutePath() + File.separator + BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_FILENAME));
        ParsedBlastResultCollection parsedBlastCollection = (ParsedBlastResultCollection) ois.readObject();
        ois.close();
        return parsedBlastCollection;
    }

    private IBlastOutputFormatTask getBlastTask() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(brccArgs.dir.getAbsolutePath() + File.separator + BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_BASENAME + ".blastTask"));
        IBlastOutputFormatTask blastTask = (IBlastOutputFormatTask) ois.readObject();
        ois.close();
        return blastTask;
    }

    /**
     * Thei method creates an ordered list of maps. Each map contains a query ID as a key,
     * and a list of  ParsedBlastResult as a value. In effect it creates a data structure grouped by
     * query ID. Each map instance will have only one key - group query ID
     * The reason for List of single entry Maps is to preserve the order of entities
     *
     * @param parsedBlastCollection
     * @return
     */

    private List<Map<String, List<ParsedBlastResult>>> groupBy(ParsedBlastResultCollection parsedBlastCollection) {
        List<Map<String, List<ParsedBlastResult>>> groupedList = new LinkedList<Map<String, List<ParsedBlastResult>>>();
        List<ParsedBlastResult> currentGroup = null;
        String lastQueryId = "";
        for (ParsedBlastResult pbr : parsedBlastCollection.getParsedBlastResults()) {
            String queryID = pbr.getQueryId();
            if (!lastQueryId.equals(queryID)) {
                // create map for new group
                Map<String, List<ParsedBlastResult>> map = new HashMap<String, List<ParsedBlastResult>>();

                // create new group - not to overide old one
                currentGroup = new LinkedList<ParsedBlastResult>();

                // store new group
                map.put(queryID, currentGroup);
                groupedList.add(map);
            }

            // add pbr to the current group
            if (null!=currentGroup) {
                currentGroup.add(pbr);
            }

            // keep track of where we are
            lastQueryId = queryID;
        }

        return groupedList;
    }


    public static void main(String[] args) {
        configureLog4j();
        BrccArgs brccArgs = null;
        try {
            brccArgs = new BrccArgs(args);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        BlastResultCollectionConverter boc = new BlastResultCollectionConverter(brccArgs);
        try {
            boc.process();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            System.exit(2);
        }
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

class BrccArgs {

    File dir;
    BigInteger lastIteration;
    BigInteger expectedIterations;
    boolean writeTop;
    boolean writeBottom;
    List<String> formats = new LinkedList<String>();

    private String usage() {
        return ("usage: dir=<blast output directory> start=<start iteration> expect=<expected iterations> [first=<yes|no>] [last=<yes|no>] formats=<xml,btab,txt>\n"
                + "default values: first=yes last=yes formats=xml");
    }


    public BrccArgs(String[] args) {
        // set defaults
        formats.add(BlastTask.FORMAT_XML);
        writeTop = true;
        writeBottom = true;

        if (args == null || args.length < 3) {
            throw new IllegalArgumentException(usage());
        }

        String argName, argVal;
        for (String arg : args) {
            String[] nameValue = arg.split("=");
            if (nameValue.length > 1) {
                argName = nameValue[0].trim();
                argVal = nameValue[1].trim();
            }
            else {
                throw new IllegalArgumentException("argument and it's value must be separated by =");
            }

            if (argName.equals(BlastResultCollectionConverter.KEY_BLAST_OUTPUT_DIR)) {
                try {
                    dir = new File(argVal);
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("'" + argVal + "' does not exist");
                }
                if (!dir.isDirectory())
                    throw new IllegalArgumentException("'" + argVal + "' is not a directory");

            }
            else if (argName.equals(BlastResultCollectionConverter.KEY_START_INTERATION)) {
                lastIteration = getBigIntValue(argVal);
            }
            else if (argName.equals(BlastResultCollectionConverter.KEY_EXPECTED_INTERATIONS)) {
                expectedIterations = getBigIntValue(argVal);
            }
            else if (argName.equals(BlastResultCollectionConverter.KEY_IS_FIRST)) {
                if (!argVal.equalsIgnoreCase("yes"))
                    writeTop = false;
            }
            else if (argName.equals(BlastResultCollectionConverter.KEY_IS_LAST)) {
                if (!argVal.equalsIgnoreCase("yes"))
                    writeBottom = false;
            }
            else if (argName.equals(BlastResultCollectionConverter.KEY_OUTPUT_FORMATS)) {
                formats.clear(); // reset default value
                String[] vals = argVal.split(",");
                for (String frm : vals) {
                    frm = frm.trim();
                    // A little cheesy, but it works
                    if (BlastTask.getValidOutputFormatList().contains(frm))
                        formats.add(frm);
                    else
                        throw new IllegalArgumentException("Invalid format '" + frm + "' specified");
                }
            }
            else {
                throw new IllegalArgumentException("Invalid argument: " + argName);
            }
        }
    }

    private BigInteger getBigIntValue(String val) throws IllegalArgumentException {
        try {
            return new BigInteger(val);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("expect argument must have numeric value");
        }
    }
}
