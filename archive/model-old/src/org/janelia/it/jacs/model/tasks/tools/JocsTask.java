
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Aug 26, 2010
 * Time: 12:16:04 PM
 */
public class JocsTask extends Task {

    /*
    
    Parameter options for CogBsmlLoader.pl -  Preprocess data stored in BSML pairwise alignment documents into
    BTAB structure for COG analysis using best_hits.pl.

    --bsmlModelList         List file containing results from legacy2bsml
    --bsmlSearchList        List file containing blast results
    --bsmlJaccardList       List file containing clustalw results from Jaccard run
    --pvalue                P value cut off for run
    --coverageCutoff

    Parameter options for best_hit.pl -  Generates # clusters of proteins that are connected by bidirectional
    best hits from a single btab file.

    -c                      e-value cutoff, best hits with an e-value above this cutoff will be ignored
    -i                      input btab file
    -j                      Jaccard coefficient cutoff

    Parameter option for CogProteinFasta.pl
    -cogFile                Output from best_hit.pl, not passed into service
    -bsmlModelList          List file containing results from legacy2bsml
    -maxCogSeqCount
    -use_feature_ids_in_fasta

     */

    //Parameters for CogBsmlLoader.pl
    transient public static final String PARAM_bsmlSearchList = "bsml search list";
    transient public static final String PARAM_bsmlModelList = "bsml model list";
    transient public static final String PARAM_bsmlJaccardList = "bsml jaccard list";
    transient public static final String PARAM_pvlaue = "p value";
    transient public static final String PARAM_coverageCutoff = "coverage cutoff";

    //Parameters for best_hit.pl
    transient public static final String PARAM_jaccard_coefficient = "jaccard coefficient";
    transient public static final String PARAM_j_cutoff = "j cutoff";

    //Parameters for CogProteinFasta
    transient public static final String PARAM_max_cog_seq_count = "max cog seq count";
    transient public static final String PARAM_use_feature_ids_in_fasta = "use feature ids in fasta";
    
    public JocsTask() {
        super();
        setTaskName("JocsTask");
    }

    public String getDisplayName() {
        return "JocsTask";
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

    public String generateParseBsmlOptions() throws ParameterException {
        StringBuffer sb = new StringBuffer();

        //Parameters for CogBsmlLoader.pl
        addRequiredCommandParameterEquals(sb, "--bsmlSearchList", PARAM_bsmlSearchList);
        addRequiredCommandParameterEquals(sb, "--bsmlModelList", PARAM_bsmlModelList);
        addRequiredCommandParameterEquals(sb, "--bsmlJaccardList", PARAM_bsmlJaccardList);
        addCommandParameterEquals(sb, "--pvalcut", PARAM_pvlaue);
        addCommandParameterEquals(sb, "--coverageCutoff", PARAM_coverageCutoff);

        return sb.toString();
    }

    public String generateBestHitOptions() throws ParameterException {
        StringBuffer sb = new StringBuffer();

        //Parameters for best_hit.pl
        addCommandParameter(sb, "-j", PARAM_jaccard_coefficient);
        addCommandParameter(sb, "-c", PARAM_j_cutoff);

        return sb.toString();
    }

    public String generateCogs2FastaOptions() throws ParameterException {
        StringBuffer sb = new StringBuffer();
        
        //Parameters for CogProteinFasta
        addCommandParameterEquals(sb, "--maxCogSeqCount", PARAM_max_cog_seq_count);
        addRequiredCommandParameterEquals(sb, "--fastaInputFile", PARAM_bsmlModelList);
        
        return sb.toString();
    }

}