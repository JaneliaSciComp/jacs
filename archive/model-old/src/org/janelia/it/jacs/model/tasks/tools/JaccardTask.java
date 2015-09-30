
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Aug 13, 2010
 * Time: 12:16:04 PM
 */
public class JaccardTask extends Task {

    /*
     The Jaccard service is comprised of running 3 executables
     
     * bsml2idlookup
        - Program parameters are not passed into service. Input is created by modifing the input_file_list
          input_file_list is a list of fasta files
          
     * clusterBsmlPairwiseAlignments.pl
      USAGE:  clusterBsmlPairwiseAlignments.pl  -b bsml_list -m match_list -k linkscore -p percent_identity -u p_value [-l log] [-h help] [-o outdir] [-p percent_identity] [-u p_value] [-t tmpdir]
      <--bsml_list,-b>  List of bsml files containing polypeptides and assemblies
      <--match_list,-m> List of bsml files containing pairwise matches
      <--linkscore,-k>  Link score: <0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9>
      <--percent_identity, -p> Percent identity threshold value.  Default is set to 75
      <--p_value, -u> P_value threshold value.  Default is set to 1e-15


      Note: <--tmpdir, -t> Temporary directory (for clustering algorithm files)
      tmpdir is created directly in init, and is not passed in

     * CogProteinFasta.pl
       <--maxCogSeqCount>

     */

    transient public static final String PARAM_input_file_list = "input file list";
   
    //Parameters for clusterBsmlPairwiseAlignments.pl
    transient public static final String PARAM_bsmlSearchList = "bsml Search List";
    transient public static final String PARAM_link_score = "link score";
    transient public static final String PARAM_percent_identity = "percent identity";
    transient public static final String PARAM_percent_coverage = "percent coverage";
    transient public static final String PARAM_p_value = "p value";

    //Parameters for CogProteinFasta.pl
    transient public static final String PARAM_max_cog_seq_count = "max cog seq count";
    
    public JaccardTask(){
        super();
        setTaskName("JaccardTask");

    }

    public String getDisplayName() {
        return "JaccardTask";
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

    public String generateCommandOptions() throws ParameterException {
        StringBuffer sb=new StringBuffer();

        addRequiredCommandParameter(sb, "--input_file_list", PARAM_input_file_list);
      
        return sb.toString();
    }

    public String generateCommandOptionsClusters() throws ParameterException {
        StringBuffer sb=new StringBuffer();

        //Parameters for clusterBsmlPairwiseAlignments.pl
        addRequiredCommandParameter(sb, "--bsmlSearchList", PARAM_bsmlSearchList);
        addCommandParameter(sb, "--linkscore", PARAM_link_score);
        addCommandParameter(sb, "--percent_identity", PARAM_percent_identity);
        addCommandParameter(sb, "--percent_coverage", PARAM_percent_coverage);
        addCommandParameter(sb, "--p_value", PARAM_p_value);

        return sb.toString();
    }

    public String generateCommandOptionsJaccard2Fasta() throws ParameterException {
        StringBuffer sb=new StringBuffer();

        //Parameters for CogProteinFasta.pl
        addCommandParameter(sb, "--maxCogSeqCount", PARAM_max_cog_seq_count);

        return sb.toString();
    }

}