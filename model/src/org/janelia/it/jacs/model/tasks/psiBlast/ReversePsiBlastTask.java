
package org.janelia.it.jacs.model.tasks.psiBlast;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.blast.IBlastOutputFormatTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 11, 2008
 * Time: 3:29:01 PM
 */
public class ReversePsiBlastTask extends Task implements IBlastOutputFormatTask {

    transient private static final int MAX_QUERY_LENGTH_CHARS = 10000000; // 10 million
    transient public static final String BLAST_NAME = "rpsblast";
    transient public static final String DISPLAY_NAME = "Reverse Psi-Blast";

    // Default values - default overrides
    transient public static final Long evalue_DEFAULT = (long) 1;
    // default (0) implies  length is actual length of profile library
    transient public static final Long databaseSize_DEFAULT = (long) 0;
    // todo Get this from the result node
    transient public static final String outputFileName_DEFAULT = "rpsBlast";
    transient public static final int alignmentViewOptions_DEFAULT = 7;
    transient public static final int blastExtensionDropoff_DEFAULT = 7;
    transient public static final int hitType_DEFAULT = 0;
    transient public static final int gappedAlignmentDropoff_DEFAULT = 15;
    transient public static final int gappingBitTrigger_DEFAULT = 22;
    transient public static final int finalGappedAlignmentDropoff_DEFAULT = 25;
    transient public static final int databaseAlignments_DEFAULT = 25;
    transient public static final String PARAM_project = "project";

    // unsettable defaults used by the underlying blastp task rspblast uses
    // to compute its results; used by the BlastXMLWriter object
    // to successfully output several required task output data elements
    transient public static final String gapOpenCost_DEFAULT = "11";
    transient public static final String gapExtendCost_DEFAULT = "1";
    transient public static final String matrix_DEFAULT = "BLOSUM62";

    /**
     * Arguments of RPS Blast
     * -i  query sequence file (required)
     * -d  RPS BLAST Database [File In]
     * -p  if query sequence protein (if FALSE 6 frame translation will be
     * conducted as in blastx program)
     * -P  0 for multiple hits 1-pass, 1 for single hit 1-pass [Integer]
     * default = 0
     * -o  output file (optional)
     * default = stdout
     * -e  Expectation value threshold  (E), (optional, same as for BLAST)
     * default = 10
     * -m  alignment view (optional, same as for BLAST) (-m 7, yields XML Blast output, for instance)
     * -m  alignment view options:
     * 0 = pairwise,
     * 1 = query-anchored showing identities,
     * 2 = query-anchored no identities,
     * 3 = flat query-anchored, show identities,
     * 4 = flat query-anchored, no identities,
     * 5 = query-anchored no identities and blunt ends,
     * 6 = flat query-anchored, no identities and blunt ends,
     * 7 = XML Blast output,
     * 8 = tabular,
     * 9 tab ular with comment lines
     * 10 ASN, text
     * 11 ASN, binary [Integer]
     * default = 0
     * range from 0 to 11
     * -z  effective length of database (optional)
     * -1 = length given via -z option to makemat
     * default (0) implies  length is actual length of profile library
     * adjusted for end effects
     * -y  Dropoff (X) for blast extensions in bits (default if zero) [Real]
     * default = 7.0
     * -P  0 for multiple hit, 1 for single hit [Integer]
     * default = 0
     * -F  Filter query sequence with SEG [String]
     * default = F
     * -X  X dropoff value for gapped alignment (in bits) [Integer]
     * default = 15
     * -N  Number of bits to trigger gapping [Real]
     * default = 22.0
     * -I  Show GI's in deflines [T/F]
     * default = F
     * -J  Believe the query defline [T/F]
     * default = F
     * -Z  X dropoff value for final gapped alignment (in bits) [Integer]
     * default = 25
     * -O  SeqAlign file ('Believe the query defline' must be TRUE) [File Out]  Optional
     * -v  Number of database sequences to show one-line descriptions for (V) [Integer]
     * default = 500
     * -b  Number of database sequence to show alignments for (B) [Integer]
     * default = 250
     * -U  Use lower case filtering of FASTA sequence [T/F]  Optional
     * default = F
     * -V  Force use of the legacy BLAST engine [T/F]  Optional
     * default = F
     */

    // Parameter Keys
    // todo Should be query node id
    transient public static final String PARAM_query_node_id = "query";
    // todo Should be psi-Blast db node id
    transient public static final String PARAM_subjectDatabases = "subject databases";
    // todo Should be dynamic - figure sequence type from query node
    transient public static final String PARAM_querySequenceProtein = "query sequence protein";
    // todo Should contain to 0 or 1 only?
    transient public static final String PARAM_loopParameter = "loop parameter";
    // todo Shouldn't be configurable
    transient public static final String PARAM_outputFileName = "output file name";
    transient public static final String PARAM_evalue = "evalue exponent (1Ex) (-e)";
    // todo Should this be configurable, blast isn't.  Any merge/sorting?
    transient public static final String PARAM_alignmentViewOptions = "alignment view options";
    // todo this should be exact, from the psi-blast db node
    transient public static final String PARAM_databaseSize = "effective length of database";
    transient public static final String PARAM_blastExtensionDropoff = "dropoff for blast extensions (bits) (-y)";
    transient public static final String PARAM_segFilter = "filter query with SEG (-F)";
    transient public static final String PARAM_gappedAlignmentDropoff = "gapped alignment dropoff (-X)";
    transient public static final String PARAM_gappingBitTrigger = "trigger gapping bits (-N)";
    transient public static final String PARAM_giInDeflines = "show GI's (-I)";
    transient public static final String PARAM_believeQueryDefline = "believe defline (-J)";
    transient public static final String PARAM_finalGappedAlignmentDropoff = "final gapped alignment dropoff (-Z)";
    transient public static final String PARAM_databaseAlignments = "db alignments per query (-b)";
    transient public static final String PARAM_useLowercaseFiltering = "lower case filtering (-U)";
    transient public static final String PARAM_forceLegacyBlast = "force legacy blast engine (-V)";

    protected List<String> potentialSubjectDatabases = new ArrayList<String>();

    // No-arg constructor for Hibernate
    public ReversePsiBlastTask() {
        setDefaultValues();
    }

    public ReversePsiBlastTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_databaseSize, databaseSize_DEFAULT.toString());
        setParameter(PARAM_alignmentViewOptions, Integer.toString(alignmentViewOptions_DEFAULT));
        setParameter(PARAM_evalue, evalue_DEFAULT.toString());
        setParameter(PARAM_outputFileName, outputFileName_DEFAULT);
        setParameter(PARAM_querySequenceProtein, Boolean.FALSE.toString());
        setParameter(PARAM_subjectDatabases, "");
        setParameter(PARAM_query_node_id, "");
        setParameter(PARAM_blastExtensionDropoff, Integer.toString(blastExtensionDropoff_DEFAULT));
        setParameter(PARAM_segFilter, Boolean.FALSE.toString());
        setParameter(PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff_DEFAULT));
        setParameter(PARAM_gappingBitTrigger, Integer.toString(gappingBitTrigger_DEFAULT));
        setParameter(PARAM_giInDeflines, Boolean.FALSE.toString());
        setParameter(PARAM_believeQueryDefline, Boolean.FALSE.toString());
        setParameter(PARAM_finalGappedAlignmentDropoff, Integer.toString(finalGappedAlignmentDropoff_DEFAULT));
        setParameter(PARAM_databaseAlignments, Integer.toString(databaseAlignments_DEFAULT));
        setParameter(PARAM_useLowercaseFiltering, Boolean.FALSE.toString());
        setParameter(PARAM_forceLegacyBlast, Boolean.FALSE.toString());
        this.taskName = BLAST_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_databaseSize)) {
            return new LongParameterVO(new Long("-1"), 10000000000l, new Long(value));
        }
        if (key.equals(PARAM_alignmentViewOptions)) {
            return new LongParameterVO(new Long("0"), new Long("11"), new Long(value));
        }
        if (key.equals(PARAM_evalue)) {
            return new LongParameterVO(new Long("-100"), new Long("3"), new Long(value));
        }
        if (key.equals(PARAM_outputFileName)) {
            return new TextParameterVO(value);
        }
        if (key.equals(PARAM_querySequenceProtein) || key.equals(PARAM_segFilter) || key.equals(PARAM_giInDeflines) ||
                key.equals(PARAM_believeQueryDefline) || key.equals(PARAM_useLowercaseFiltering) || key.equals(PARAM_forceLegacyBlast)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        if (key.equals(PARAM_loopParameter)) {
            return new LongParameterVO(new Long("-1"), new Long("10000"), new Long(value));
        }
        if (key.equals(PARAM_subjectDatabases)) {
            return new MultiSelectVO(potentialSubjectDatabases, listOfStringsFromCsvString(value));
        }
        if (key.equals(PARAM_query_node_id)) {
            return new TextParameterVO(value, MAX_QUERY_LENGTH_CHARS);
        }
        if (key.equals(PARAM_blastExtensionDropoff)) {
            return new LongParameterVO(new Long("0"), new Long("20"), new Long(value));
        }
        if (key.equals(PARAM_gappedAlignmentDropoff)) {
            return new LongParameterVO(new Long("0"), new Long("25"), new Long(value));
        }
        if (key.equals(PARAM_gappingBitTrigger)) {
            return new LongParameterVO(new Long("0"), new Long("40"), new Long(value));
        }
        if (key.equals(PARAM_finalGappedAlignmentDropoff)) {
            return new LongParameterVO(new Long("0"), new Long("40"), new Long(value));
        }
        if (key.equals(PARAM_databaseAlignments)) {
            return new LongParameterVO(new Long("0"), (long) 50000, new Long(value));
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public List<String> getPotentialSubjectDatabases() {
        return potentialSubjectDatabases;
    }

    public void setPotentialSubjectDatabases(List<String> choices) {
        this.potentialSubjectDatabases = choices;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName) && PARAM_evalue.equalsIgnoreCase(parameterKeyName);
    }

    public String generateDefaultCommandStringNotIncludingIOParams() throws ParameterException {
        StringBuffer sb = new StringBuffer();
        for (String key : getParameterKeySet()) {
            if (key.equals(PARAM_evalue)) {
                sb.append("-e 1e").append(getParameterVO(PARAM_evalue).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_blastExtensionDropoff)) {
                sb.append("-y ").append(getParameterVO(PARAM_blastExtensionDropoff).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_segFilter)) {
                sb.append("-F ").append(getParameterVO(PARAM_segFilter).getStringValue().equals("true") ? "T" : "F").append(" ");
            }
            if (key.equals(PARAM_gappedAlignmentDropoff)) {
                sb.append("-X ").append(getParameterVO(PARAM_gappedAlignmentDropoff).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_gappingBitTrigger)) {
                sb.append("-N ").append(getParameterVO(PARAM_gappingBitTrigger).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_giInDeflines)) {
                sb.append("-I ").append(getParameterVO(PARAM_giInDeflines).getStringValue().equals("true") ? "T" : "F").append(" ");
            }
            if (key.equals(PARAM_believeQueryDefline)) {
//                sb.append("-J ").append(getParameterVO(PARAM_believeDefline).getStringValue().equals("true") ? "T" : "F").append(" ");
                // NOTE:  Making this false ALWAYS because xml result parsing critically depends upon it
                sb.append("-J ").append("F").append(" ");
            }
            if (key.equals(PARAM_finalGappedAlignmentDropoff)) {
                sb.append("-Z ").append(getParameterVO(PARAM_finalGappedAlignmentDropoff).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_databaseAlignments)) {
                sb.append("-b ").append(getParameterVO(PARAM_databaseAlignments).getStringValue()).append(" ");
                sb.append("-v ").append(getParameterVO(PARAM_databaseAlignments).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_useLowercaseFiltering)) {
                sb.append("-U ").append(getParameterVO(PARAM_useLowercaseFiltering).getStringValue().equals("true") ? "T" : "F").append(" ");
            }
            if (key.equals(PARAM_forceLegacyBlast)) {
                sb.append("-V ").append(getParameterVO(PARAM_forceLegacyBlast).getStringValue().equals("true") ? "T" : "F").append(" ");
            }
        }
        return sb.toString();
    }

    // IBlastOutputFormatTask interface implementations
    // methods required to return non null values to avoid BlastXMLWriter null pointer errors
    public Double getExpectationValue() throws ParameterException {
        return Double.valueOf(getParameterVO(PARAM_evalue).getStringValue());
    }

    public String getFilterQuerySequence() throws ParameterException {
        return getParameterVO(PARAM_segFilter).getStringValue();
    }

    public BigInteger getGapExtensionCost() throws ParameterException {
        return new BigInteger(gapOpenCost_DEFAULT);
    }

    public BigInteger getGapOpeningCost() throws ParameterException {
        return new BigInteger(gapExtendCost_DEFAULT);
    }

    public String getMatrix() throws ParameterException {
        return matrix_DEFAULT;
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
