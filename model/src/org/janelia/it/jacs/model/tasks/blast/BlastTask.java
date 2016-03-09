
package org.janelia.it.jacs.model.tasks.blast;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 24, 2006
 * Time: 4:47:12 PM
 *
 * @version $Id: BlastTask.java 1 2011-02-16 21:07:19Z tprindle $
 */
public abstract class BlastTask extends Task implements IBlastOutputFormatTask {
    // Different Output Format Types - Note: The output writer parses the formats to the first space for the file extension
    public static final String FORMAT_XML = "xml"; // ncbi -m7 otuput format option
    public static final String FORMAT_BTAB = "btab"; // jcvi bpbtab output format
    public static final String FORMAT_TEXT = "txt";
    public static final String FORMAT_TAB = "tab"; // ncbi -m8 output format option
    public static final String FORMAT_TAB_WITH_HEADER = "tabh"; // ncbi -m9 output format option (tab with comment lines)

    // Base-level default values - these are the same for all flavors of Blast
    transient public static final String formatTypesCsv_DEFAULT = "xml,tab";
    transient public static final Long evalue_DEFAULT = (long) 1;
    transient public static final String filter_DEFAULT = "T";
    transient public static final Long gapOpenCost_DEFAULT = (long) -1;
    transient public static final Long gapExtendCost_DEFAULT = (long) -1;
    transient public static final Boolean showGIs_DEFAULT = Boolean.FALSE;
    transient public static final Long blastnMismatchPenalty_DEFAULT = (long) -3;
    transient public static final Long blastnMatchReward_DEFAULT = (long) 1;
    transient public static final Long databaseDescriptions_DEFAULT = (long) 250;
    transient public static final Long databaseAlignments_DEFAULT = (long) 25; // Was 250 default
    transient public static final Boolean believeDefline_DEFAULT = Boolean.FALSE;
    transient public static final String matrix_DEFAULT = "BLOSUM62";
    transient public static final Double databaseSize_DEFAULT = 0.0;
    transient public static final Long bestHitsToKeep_DEFAULT = (long) 0;
    transient public static final String searchMode_DEFAULT = "multi-hit";
    transient public static final Double searchSize_DEFAULT = 0.0;
    transient public static final Boolean generateHtml_DEFAULT = Boolean.TRUE;
    transient public static final Boolean lowerCaseFiltering_DEFAULT = Boolean.FALSE;

    // Parameter Keys
    //todo: subdivide parameters into program parameters and environment parameters
    transient public static final String PARAM_evalue = "evalue exponent (1Ex)";// (-e)";
    transient public static final String PARAM_wordsize = "wordsize";//-W)";
    transient public static final String PARAM_filter = "filter query sequence";//-F)";
    transient public static final String PARAM_gapOpenCost = "gap open cost";//-G)";
    transient public static final String PARAM_gapExtendCost = "gap extend cost";//-E)";
    transient public static final String PARAM_gappedAlignmentDropoff = "gapped alignment dropoff";//-X)";
    transient public static final String PARAM_showGIs = "show GIs";//-I)";
    transient public static final String PARAM_databaseDescriptions = "database descriptions";//-v)";
    transient public static final String PARAM_databaseAlignments = "db alignments per query";//-b)";
    transient public static final String PARAM_hitExtensionThreshold = "hit extension threshold";//-f)";
    transient public static final String PARAM_believeDefline = "believe defline";//-J)";
    transient public static final String PARAM_matrix = "matrix";//-M)";
    transient public static final String PARAM_databaseSize = "database size";//-z)";
    transient public static final String PARAM_bestHitsToKeep = "best hits to keep";//-K)";
    transient public static final String PARAM_searchSize = "search size";//-Y)";
    transient public static final String PARAM_lowerCaseFiltering = "lower case filtering";//-U)";
    transient public static final String PARAM_ungappedExtensionDropoff = "ungapped extension dropoff";//-y)";
    transient public static final String PARAM_finalGappedDropoff = "final gapped dropoff";//-Z)";
    transient public static final String PARAM_multiHitWindowSize = "multihit window size";//-A)";
    transient public static final String PARAM_subjectDatabases = "subject databases";//-d)";
    transient public static final String PARAM_query = "query";//-i)";
    transient public static final String PARAM_largeBlastOutputDir = "largeBlastOutputDir";
    transient public static final String PARAM_formatTypesCsv = "format";//-m)";
    transient public static final String PARAM_mismatchPenalty = "mismatch penalty (-q)";
    transient public static final String PARAM_matchReward = "match reward (-r)";
    transient public static final String PARAM_gappedAlignment = "gapped alignment (-g)";
    transient public static final String PARAM_searchStrand = "search strand (-S)";
    transient public static final String PARAM_frameshiftPenalty = "frameshift penalty";//-w)";

    protected List<String> potentialSubjectDatabases = new ArrayList<String>();

    public BlastTask() {
        setDefaultValues();
    }

    protected void setDefaultValues() {
        // Enumerate all parameter keys and add default string values to taskParameterSet
        setParameter(PARAM_evalue, evalue_DEFAULT.toString());
        setParameter(PARAM_wordsize, BlastNTask.wordsize_BLASTN_DEFAULT.toString());
        setParameter(PARAM_filter, filter_DEFAULT);
        setParameter(PARAM_gapOpenCost, gapOpenCost_DEFAULT.toString());
        setParameter(PARAM_gapExtendCost, gapExtendCost_DEFAULT.toString());
        setParameter(PARAM_gappedAlignmentDropoff, BlastNTask.gappedAlignmentDropoff_BLASTN_DEFAULT.toString());
        setParameter(PARAM_showGIs, showGIs_DEFAULT.toString());
        setParameter(PARAM_databaseAlignments, databaseAlignments_DEFAULT.toString());
        setParameter(PARAM_hitExtensionThreshold, BlastNTask.hitExtensionThreshold_BLASTN_DEFAULT.toString());
        setParameter(PARAM_believeDefline, believeDefline_DEFAULT.toString());
        setParameter(PARAM_matrix, matrix_DEFAULT);
        setParameter(PARAM_databaseSize, databaseSize_DEFAULT.toString());
        setParameter(PARAM_bestHitsToKeep, bestHitsToKeep_DEFAULT.toString());
        setParameter(PARAM_searchSize, searchSize_DEFAULT.toString());
        setParameter(PARAM_lowerCaseFiltering, lowerCaseFiltering_DEFAULT.toString());
        setParameter(PARAM_ungappedExtensionDropoff, BlastNTask.ungappedExtensionDropoff_BLASTN_DEFAULT.toString());
        setParameter(PARAM_finalGappedDropoff, BlastNTask.finalGappedDropoff_BLASTN_DEFAULT.toString());
        setParameter(PARAM_multiHitWindowSize, BlastNTask.multiHitWindowSize_BLASTN_DEFAULT.toString());
        setParameter(PARAM_subjectDatabases, "");
        setParameter(PARAM_query, "");
        setParameter(PARAM_formatTypesCsv, formatTypesCsv_DEFAULT);
    }

    // Note: if a common default is not available, BlastN defaults are used below
    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_evalue)) {
            return new LongParameterVO(new Long("-100"), new Long("3"), new Long(value));
        }
        if (key.equals(PARAM_wordsize)) {
            return new LongParameterVO(new Long(value));
        }
        if (key.equals(PARAM_filter)) {
            return new TextParameterVO(value);
        }
        if (key.equals(PARAM_gapOpenCost)) {
            return new LongParameterVO(new Long(value));
        }
        if (key.equals(PARAM_gapExtendCost)) {
            return new LongParameterVO(new Long(value));
        }
        if (key.equals(PARAM_gappedAlignmentDropoff)) {
            return new LongParameterVO(new Long(value));
        }
        if (key.equals(PARAM_showGIs)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        if (key.equals(PARAM_databaseAlignments)) {
            return new LongParameterVO(1l, (long) 50000, new Long(value));
        }
        if (key.equals(PARAM_hitExtensionThreshold)) {
            return new LongParameterVO(new Long(value));
        }
        if (key.equals(PARAM_believeDefline)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        if (key.equals(PARAM_matrix)) {
            return new SingleSelectVO(getBlastMatrixList(), value);
        }
        if (key.equals(PARAM_databaseSize)) {
            return new DoubleParameterVO(new Double(value));
        }
        if (key.equals(PARAM_bestHitsToKeep)) {
            return new LongParameterVO(0l, LongParameterVO.LONG_MAX, new Long(value));
        }
        if (key.equals(PARAM_searchSize)) {
            return new DoubleParameterVO(new Double(value));
        }
        if (key.equals(PARAM_lowerCaseFiltering)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        if (key.equals(PARAM_ungappedExtensionDropoff)) {
            return new DoubleParameterVO(new Double(value));
        }
        if (key.equals(PARAM_finalGappedDropoff)) {
            return new DoubleParameterVO(new Double(value));
        }
        if (key.equals(PARAM_multiHitWindowSize)) {
            return new LongParameterVO(new Long(value));
        }
        if (key.equals(PARAM_subjectDatabases)) {
            return new MultiSelectVO(potentialSubjectDatabases, listOfStringsFromCsvString(value));
        }
        if (key.equals(PARAM_query))
            return new TextParameterVO(value);
        if (key.equals(PARAM_formatTypesCsv)) {
            return new MultiSelectVO(getValidOutputFormatList(), listOfStringsFromCsvString(value));
        }
        // No match
        return null;
    }

    public List<String> getPotentialSubjectDatabases() {
        return potentialSubjectDatabases;
    }

    public void setPotentialSubjectDatabases(List<String> choices) {
        this.potentialSubjectDatabases = choices;
    }

    /**
     * full constructor
     */
    public BlastTask(Set<Node> inputNodes,
                     String owner,
                     List<Event> events,
                     Set<TaskParameter> parameters) {
        super(inputNodes, owner, events, parameters);
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName) && (PARAM_evalue.equalsIgnoreCase(parameterKeyName) || PARAM_databaseAlignments.equalsIgnoreCase(parameterKeyName) ||
                PARAM_lowerCaseFiltering.equalsIgnoreCase(parameterKeyName) || PARAM_filter.equalsIgnoreCase(parameterKeyName));
    }

    protected List<String> getBlastMatrixList() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("PAM30");
        list.add("PAM70");
        list.add("BLOSUM45");
        list.add("BLOSUM62");
        list.add("BLOSUM80");
        return list;
    }

    public static List<String> getValidOutputFormatList() {
        ArrayList<String> validOutputFormatList = new ArrayList<String>();
        validOutputFormatList.add(FORMAT_XML);
        validOutputFormatList.add(FORMAT_BTAB);
        validOutputFormatList.add(FORMAT_TAB);
        validOutputFormatList.add(FORMAT_TAB_WITH_HEADER);
//        validOutputFormatList.add(FORMAT_TEXT);
        return validOutputFormatList;
    }

    public Long getQueryId() {
        String queryParam = null;
        try {
            queryParam = ((TextParameterVO) getParameterVO(PARAM_query)).getTextValue();
        }
        catch (Exception ignore) {
        }
        Long queryId = (long) 0;
        if (queryParam != null && queryParam.trim().length() > 0) {
            queryId = Long.valueOf(queryParam.trim());
        }
        return queryId;
    }

    public List<String> getSearchStrandList() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("top");
        list.add("bottom");
        list.add("both");
        return list;
    }

    public String generateDefaultCommandStringNotIncludingIOParams() throws ParameterException {
        StringBuffer sb = new StringBuffer();
        for (String key : getParameterKeySet()) {
            if (key.equals(PARAM_evalue)) {
                sb.append("-e 1e").append(getParameterVO(PARAM_evalue).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_wordsize)) {
                sb.append("-W ").append(getParameterVO(PARAM_wordsize).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_filter)) {
//                System.out.println(PARAM_filter+" -F is "+((ParameterVO) (getParameterVO(PARAM_filter))).getStringValue());
                sb.append("-F \"").append(getParameterVO(PARAM_filter).getStringValue()).append("\" ");
            }
            if (key.equals(PARAM_gapOpenCost)) {
                sb.append("-G ").append(getParameterVO(PARAM_gapOpenCost).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_gapExtendCost)) {
                sb.append("-E ").append(getParameterVO(PARAM_gapExtendCost).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_gappedAlignmentDropoff)) {
                sb.append("-X ").append(getParameterVO(PARAM_gappedAlignmentDropoff).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_showGIs)) {
//                System.out.println(PARAM_showGIs+" -I is "+((ParameterVO) (getParameterVO(PARAM_showGIs))).getStringValue());
                sb.append("-I ").append(getParameterVO(PARAM_showGIs).getStringValue().equals("true") ? "T" : "F").append(" ");
            }
            // Commenting out and using the PARAM_databaseAlignments value to keep them in sync
//            if (key.equals(PARAM_databaseDescriptions)) {
//                sb.append("-v " + ((ParameterVO) (getParameterVO(PARAM_databaseDescriptions))).getStringValue() + " ");
//            }
            if (key.equals(PARAM_databaseAlignments)) {
                sb.append("-v ").append(getParameterVO(PARAM_databaseAlignments).getStringValue()).append(" ");
                sb.append("-b ").append(getParameterVO(PARAM_databaseAlignments).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_hitExtensionThreshold)) {
                sb.append("-f ").append(getParameterVO(PARAM_hitExtensionThreshold).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_believeDefline)) {
//                System.out.println(PARAM_believeDefline+" -J is "+((ParameterVO) (getParameterVO(PARAM_believeDefline))).getStringValue());
//                sb.append("-J ").append(getParameterVO(PARAM_believeDefline).getStringValue().equals("true") ? "T" : "F").append(" ");
                // NOTE:  Making this false ALWAYS because xml result parsing critically depends upon it
                sb.append("-J ").append("F").append(" ");
            }
            if (key.equals(PARAM_matrix)) {
                sb.append("-M ").append(getParameterVO(PARAM_matrix).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_bestHitsToKeep)) {
                sb.append("-K ").append(getParameterVO(PARAM_bestHitsToKeep).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_searchSize)) {
                sb.append("-Y ").append(getParameterVO(PARAM_searchSize).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_lowerCaseFiltering)) {
//                System.out.println(PARAM_lowerCaseFiltering+" -U is "+((ParameterVO) (getParameterVO(PARAM_lowerCaseFiltering))).getStringValue());
                sb.append("-U ").append(getParameterVO(PARAM_lowerCaseFiltering).getStringValue().equalsIgnoreCase("true") ? "T" : "F").append(" ");
            }
            if (key.equals(PARAM_ungappedExtensionDropoff)) {
                sb.append("-y ").append(getParameterVO(PARAM_ungappedExtensionDropoff).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_finalGappedDropoff)) {
                sb.append("-Z ").append(getParameterVO(PARAM_finalGappedDropoff).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_multiHitWindowSize)) {
                sb.append("-A ").append(getParameterVO(PARAM_multiHitWindowSize).getStringValue()).append(" ");
            }
        }
        return sb.toString();
    }

    public abstract String generateCommandStringNotIncludingIOParams() throws ParameterException;

    public String searchStrandTranslator(String str) {
        if (str.trim().equals("both")) {
            return "3";
        }
        if (str.trim().equals("top")) {
            return "1";
        }
        if (str.trim().equals("bottom")) {
            return "2";
        }
        return null;
    }

    // IBlastOutputFormatTask interface implementations
    // methods required to return non null values to avoid BlastXMLWriter null pointer errors
    public Double getExpectationValue() throws ParameterException {
        return Double.valueOf(getParameterVO(PARAM_evalue).getStringValue());
    }

    public String getFilterQuerySequence() throws ParameterException {
        return getParameterVO(PARAM_filter).getStringValue();
    }

    public BigInteger getGapExtensionCost() throws ParameterException {
        return new BigInteger(getParameterVO(PARAM_gapExtendCost).getStringValue());
    }

    public BigInteger getGapOpeningCost() throws ParameterException {
        return new BigInteger(getParameterVO(PARAM_gapOpenCost).getStringValue());
    }

    public String getMatrix() throws ParameterException {
        return getParameterVO(PARAM_matrix).getStringValue();
    }

    // IBlastOutputFormatTask interface implementations
    // methods can return nulls without risk of BlastXMLWriter null pointer errors
    public BigInteger getMatchReward() throws ParameterException {
        return null; // value only meaningful for BlastNTask, so default is to return null
    }

    public BigInteger getMismatchPenalty() throws ParameterException {
        return null; // value only meaningful for BlastNTask, so default is to return null
    }
}
