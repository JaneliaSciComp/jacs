
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: May 4, 2010
 * Time: 12:16:04 PM
 */
public class PrositeScanTask extends Task {

    /*
        ps_scan.pl [options] sequence-file(s)
        ps_scan version 1.20 options:
        -h : this help screen
        Input/Output:
          -e : specify the ID or AC of an entry in sequence-file
          -o : specify an output format : scan | fasta | psa | msa | gff | pff | epff | sequence | matchlist
          -d : specify a prosite.dat file
          -p : specify a pattern or the ID or AC of a prosite pattern
        Selection:
          -r : do not scan profiles
          -s : skip frequently matching (unspecific) patterns and profiles
          -l : cut-off level for profiles (default : 0)
        Pattern match mode:
          -x : specify maximum number of accepted matches of X's in sequence
               (default=1)
          -g : Turn greediness off
          -v : Turn overlaps off
          -i : Allow included matches

        The sequence-file may be in Swiss-Prot or FASTA format.
        If no PROSITE file is submitted, it will be searched in the paths
        $PROSITE/prosite.dat and $SPROT/prosite/prosite.dat.
        There may be several -d, -p and -e arguments.

        Pfsearch options:
          -w pfsearch : Compares a query profile against a protein sequence library.
          A profile file must be specified with option -d.

           ps_scan.pl -w pfsearch [-C cutoff] [-R] -d profile-file seq-library-file(s)

         -R: use raw scores rather than normalized scores for match selection
         -C=# : Cut-off value. Reports only match score higher than the specified parameter.
        An integer argument is interpreted as a raw score value,
        a decimal argument as a normalized score value. An integer value forces option -R.

     */

    transient public static final String PARAM_specific_entry = "specific entry";
    transient public static final String PARAM_output_format = "output format";
    transient public static final String PARAM_prosite_database_file = "prosite database file";
    transient public static final String PARAM_prosite_pattern = "prosite pattern";
    transient public static final String PARAM_do_not_scan_profiles = "do not scan profiles";
    transient public static final String PARAM_skip_unspecific_profiles = "skip unspecific profiles";
    transient public static final String PARAM_profile_cutoff_level = "profile cutoff level";
    transient public static final String PARAM_maximum_x_count = "maximum x count";
    transient public static final String PARAM_no_greediness = "no greediness";
    transient public static final String PARAM_no_overlaps = "no overlaps";
    transient public static final String PARAM_allow_included_matches = "allow included matches";
    transient public static final String PARAM_pfsearch = "pfsearch";
    transient public static final String PARAM_use_raw_scores = "use raw scores";
    transient public static final String PARAM_cutoff_value = "cutoff value";
    transient public static final String PARAM_fasta_input_node_id = "input node id";


    transient public static final String prosite_database_file_DEFAULT = "/usr/local/db/prosite/prosite.dat";
    transient public static final String profile_cutoff_level_DEFAULT = "0";
    transient public static final String maximum_x_count_DEFAULT = "1";


    public PrositeScanTask() {
        super();
        setTaskName("PrositeScanTask");
        setParameter(PARAM_prosite_database_file, prosite_database_file_DEFAULT);
        setParameter(PARAM_profile_cutoff_level, profile_cutoff_level_DEFAULT);
        setParameter(PARAM_fasta_input_node_id, "");
    }

    public String getDisplayName() {
        return "PrositeScanTask";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        // no match
        return null;
    }

    public String generateCommandOptions(String outputDirectory) throws ParameterException {
        StringBuffer sb = new StringBuffer();
        addCommandParameter(sb, "-e", PARAM_specific_entry);
        addCommandParameter(sb, "-o", PARAM_output_format);
        addCommandParameter(sb, "-d", PARAM_prosite_database_file);
        addCommandParameter(sb, "-p", PARAM_prosite_pattern);
        addCommandParameter(sb, "-r", PARAM_do_not_scan_profiles);
        addCommandParameter(sb, "-s", PARAM_skip_unspecific_profiles);
        addCommandParameter(sb, "-l", PARAM_profile_cutoff_level);
        addCommandParameter(sb, "-g", PARAM_no_greediness);
        addCommandParameter(sb, "-v", PARAM_no_overlaps);
        addCommandParameter(sb, "-i", PARAM_allow_included_matches);
        addCommandParameter(sb, "-w", PARAM_pfsearch);
        addCommandParameter(sb, "-R", PARAM_use_raw_scores);
        addCommandParameter(sb, "-C", PARAM_cutoff_value);
        return sb.toString();
    }

}