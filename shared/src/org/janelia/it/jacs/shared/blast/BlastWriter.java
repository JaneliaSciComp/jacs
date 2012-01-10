
package org.janelia.it.jacs.shared.blast;

import org.janelia.it.jacs.model.tasks.blast.IBlastOutputFormatTask;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 18, 2009
 * Time: 1:39:03 PM
 */
public abstract class BlastWriter {
    private final static String PERCENT_IDENTITY_FORMAT = "###.00";

    protected final static String UNSPECIFIED_STR = "unspecified";
    protected final static String BLAST_REFERENCE =
            "~Reference: Altschul, Stephen F., Thomas L. Madden, Alejandro A. Schaffer, " +
                    "~Jinghui Zhang, Zheng Zhang, Webb Miller, and David J. Lipman (1997), " +
                    "~&quot;Gapped BLAST and PSI-BLAST: a new generation of protein database search" +
                    "~programs&quot;,  Nucleic Acids Res. 25:3389-3402.";


    private DecimalFormat percentIdentityFormat;

    protected Collection<ParsedBlastResult> blastCollection = null;
    protected IBlastOutputFormatTask task = null;
    protected Map<String, String> deflineMap = null;
    protected PrintWriter bufferedWriter;

    public void init(File outFile) throws IOException {
        bufferedWriter = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(FileUtil.ensureFileExists(outFile.getAbsolutePath())))); // not a UTF8 - just plain text
        // set up identity format object
        percentIdentityFormat = new DecimalFormat(PERCENT_IDENTITY_FORMAT);
        percentIdentityFormat.setRoundingMode(RoundingMode.HALF_UP);
    }

    public void finish() throws Exception {
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public void setBlastDataSources(ParsedBlastResultCollection bhCol, IBlastOutputFormatTask task) throws Exception {
        blastCollection = bhCol.getParsedBlastResults();  // set of ParsedBlastResult
        this.task = task;
        deflineMap = bhCol.getDeflineMap();
    }

    protected String getPercentIdentity(ParsedBlastHSP hsp) {
        String percentIdentity = null;
        if (hsp.getNumberIdentical() != null
                && hsp.getLengthAlignment() != null) {
            // convert int values to doubles to avoid ugly hacks in
            // percentage calculation equation
            double identical = hsp.getNumberIdentical().doubleValue();
            double alignmentSize = hsp.getLengthAlignment().doubleValue();

            double value = (identical / alignmentSize) * 100;
            percentIdentity = percentIdentityFormat.format(value);
        }
        return percentIdentity;
    }

    protected String getPercentSimilarity(ParsedBlastHSP hsp) {
        String percentIdentity = null;
        if (hsp.getNumberIdentical() != null
                && hsp.getNumberSimilar() != null
                && hsp.getLengthAlignment() != null) {
            // convert int values to doubles to avoid ugly hacks in
            // percentage calculation equation
            double identical = hsp.getNumberIdentical().doubleValue();
            double similar = hsp.getNumberSimilar().doubleValue();
            double alignmentSize = hsp.getLengthAlignment().doubleValue();


            double value = ((identical + similar) / alignmentSize) * 100;
            percentIdentity = percentIdentityFormat.format(value);
        }
        return percentIdentity;
    }

    // For now, simply returns the unspecified string
    // @todo: re-enable getBlastDBsFromBlastTask call for blast db list
    protected String getBlastDB() {
        /*
        if ( task != null ) {
            String taskData = getBlastDBsFromBlastTask((BlastTask)task);
            if ( taskData != null && taskData.trim().length() > 0 ) {
                blastDB = taskData;
            }
        }
        */
        return UNSPECIFIED_STR;
    }

    /* @todo: convert BlastTask subject db ids to subject db names
    private String getBlastDBsFromBlastTask(BlastTask blastTask) {
        String blastDBs = null;
        try {
            StringBuffer buf = new StringBuffer();

            MultiSelectVO databaseFileNodeList = (MultiSelectVO) blastTask.getParameterVO(BlastTask.PARAM_subjectDatabases);
            for ( String subjectDatabaseId : databaseFileNodeList.getValuesAsStringArray() ) {
                if ( buf.length() != 0 ) {
                    buf.append("|");
                }
                buf.append(subjectDatabaseId);
            }

            blastDBs = buf.toString();
        } catch (ParameterException e) {
            Logger logger = Logger.getLogger(this.getClass());
            logger.error(e,e);
        }
        return blastDBs;
    }
    */

    public abstract void writeTopPortion() throws Exception;

    public abstract void writeBottomPortion() throws Exception;

    public abstract void startQueryPortion() throws Exception;

    public abstract void endQueryPortion() throws Exception;

    public abstract void writeSingleQueryPortion(String qID, List<ParsedBlastResult> pbrList, long queryCounter) throws Exception;
}
