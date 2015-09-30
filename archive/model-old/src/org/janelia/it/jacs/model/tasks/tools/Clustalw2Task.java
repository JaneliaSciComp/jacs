
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 30, 2010
 * Time: 12:16:04 PM
 */
public class Clustalw2Task extends Task {

    /*
        Help for command line parameters

                        DATA (sequences)

        -PROFILE1=file.ext  and  -PROFILE2=file.ext  :profiles (old alignment).


                        VERBS (do things)

        -ALIGN              :do full multiple alignment.
        -TREE               :calculate NJ tree.
        -PIM                :output percent identity matrix (while calculating the tree)
        -BOOTSTRAP(=n)      :bootstrap a NJ tree (n= number of bootstraps; def. = 1000).
        -CONVERT            :output the input sequences in a different file format.


                        PARAMETERS (set things)

        ***General settings:****
        -QUICKTREE   :use FAST algorithm for the alignment guide tree
        -TYPE=       :PROTEIN or DNA sequences
        -NEGATIVE    :protein alignment with negative values in matrix
        -OUTFILE=    :sequence alignment file name
        -OUTPUT=     :GCG, GDE, PHYLIP, PIR or NEXUS
        -OUTORDER=   :INPUT or ALIGNED
        -CASE        :LOWER or UPPER (for GDE output only)
        -SEQNOS=     :OFF or ON (for Clustal output only)
        -SEQNO_RANGE=:OFF or ON (NEW: for all output formats)
        -RANGE=m,n   :sequence range to write starting m to m+n
        -MAXSEQLEN=n :maximum allowed input sequence length
        -QUIET       :Reduce console output to minimum
        -STATS=      :Log some alignents statistics to file

        ***Fast Pairwise Alignments:***
        -KTUPLE=n    :word size
        -TOPDIAGS=n  :number of best diags.
        -WINDOW=n    :window around best diags.
        -PAIRGAP=n   :gap penalty
        -SCORE       :PERCENT or ABSOLUTE


        ***Slow Pairwise Alignments:***
        -PWMATRIX=    :Protein weight matrix=BLOSUM, PAM, GONNET, ID or filename
        -PWDNAMATRIX= :DNA weight matrix=IUB, CLUSTALW or filename
        -PWGAPOPEN=f  :gap opening penalty
        -PWGAPEXT=f   :gap opening penalty


        ***Multiple Alignments:***
        -NEWTREE=      :file for new guide tree
        -USETREE=      :file for old guide tree
        -MATRIX=       :Protein weight matrix=BLOSUM, PAM, GONNET, ID or filename
        -DNAMATRIX=    :DNA weight matrix=IUB, CLUSTALW or filename
        -GAPOPEN=f     :gap opening penalty
        -GAPEXT=f      :gap extension penalty
        -ENDGAPS       :no end gap separation pen.
        -GAPDIST=n     :gap separation pen. range
        -NOPGAP        :residue-specific gaps off
        -NOHGAP        :hydrophilic gaps off
        -HGAPRESIDUES= :list hydrophilic res.
        -MAXDIV=n      :% ident. for delay
        -TYPE=         :PROTEIN or DNA
        -TRANSWEIGHT=f :transitions weighting
        -ITERATION=    :NONE or TREE or ALIGNMENT
        -NUMITER=n     :maximum number of iterations to perform
        -NOWEIGHTS     :disable sequence weighting


        ***Profile Alignments:***
        -PROFILE      :Merge two alignments by profile alignment
        -NEWTREE1=    :file for new guide tree for profile1
        -NEWTREE2=    :file for new guide tree for profile2
        -USETREE1=    :file for old guide tree for profile1
        -USETREE2=    :file for old guide tree for profile2


        ***Sequence to Profile Alignments:***
        -SEQUENCES   :Sequentially add profile2 sequences to profile1 alignment
        -NEWTREE=    :file for new guide tree
        -USETREE=    :file for old guide tree


        ***Structure Alignments:***
        -NOSECSTR1     :do not use secondary structure-gap penalty mask for profile 1
        -NOSECSTR2     :do not use secondary structure-gap penalty mask for profile 2
        -SECSTROUT=STRUCTURE or MASK or BOTH or NONE   :output in alignment file
        -HELIXGAP=n    :gap penalty for helix core residues
        -STRANDGAP=n   :gap penalty for strand core residues
        -LOOPGAP=n     :gap penalty for loop regions
        -TERMINALGAP=n :gap penalty for structure termini
        -HELIXENDIN=n  :number of residues inside helix to be treated as terminal
        -HELIXENDOUT=n :number of residues outside helix to be treated as terminal
        -STRANDENDIN=n :number of residues inside strand to be treated as terminal
        -STRANDENDOUT=n:number of residues outside strand to be treated as terminal


        ***Trees:***
        -OUTPUTTREE=nj OR phylip OR dist OR nexus
        -SEED=n        :seed number for bootstraps.
        -KIMURA        :use Kimura's correction.
        -TOSSGAPS      :ignore positions with gaps.
        -BOOTLABELS=node OR branch :position of bootstrap values in tree display
        -CLUSTERING=   :NJ or UPGMA
    
     */

    // VERBS input
    transient public static final String PARAM_align = "align";
    transient public static final String PARAM_tree = "tree";
    transient public static final String PARAM_pim = "pim";
    transient public static final String PARAM_bootstrap = "bootstrap";
    transient public static final String PARAM_convert = "convert";

    // General Settings Parameters
    transient public static final String PARAM_quicktree = "quicktree";
    transient public static final String PARAM_type = "type";
    transient public static final String PARAM_negative = "negative";
    transient public static final String PARAM_output = "output";
    transient public static final String PARAM_outorder = "outorder";
    transient public static final String PARAM_clustal_case = "clustal case";
    transient public static final String PARAM_seqnos = "seqnos";
    transient public static final String PARAM_seqno_range = "seqno_range";
    transient public static final String PARAM_range = "range";
    transient public static final String PARAM_maxseqlen = "maxseqlen";
    transient public static final String PARAM_quiet = "quiet";
    transient public static final String PARAM_stats = "stats";

    // Fast Pairwaise Alignments Parameters
    transient public static final String PARAM_ktuple = "ktuple";
    transient public static final String PARAM_topdiags = "topdiags";
    transient public static final String PARAM_window = "window";
    transient public static final String PARAM_pairgap = "pairgap";
    transient public static final String PARAM_score = "score";

    // Slow Pairwise Alignments Parameters
    transient public static final String PARAM_pwmatrix = "pwmatrix";
    transient public static final String PARAM_pwdnamatrix = "pwdnamatrix";
    transient public static final String PARAM_pwgapopen = "pwgapopen";
    transient public static final String PARAM_pwgapext = "pwgapext";

    // Multiple Alignments Parameters
    transient public static final String PARAM_newtree = "newtree";
    transient public static final String PARAM_usetree = "usetree";
    transient public static final String PARAM_matrix = "matrix";
    transient public static final String PARAM_dnamatrix = "dnamatrix";
    transient public static final String PARAM_gapopen = "gapopen";
    transient public static final String PARAM_gapext = "gapext";
    transient public static final String PARAM_endgaps = "endgaps";
    transient public static final String PARAM_gapdist = "gapdist";
    transient public static final String PARAM_nopgap = "nopgap";
    transient public static final String PARAM_nohgap = "nohgap";
    transient public static final String PARAM_hgapresidues = "hgapresidues";
    transient public static final String PARAM_maxdiv = "maxdiv";
    transient public static final String PARAM_transweight = "transweight";
    transient public static final String PARAM_iteration = "iteration";
    transient public static final String PARAM_numiter = "numiter";
    transient public static final String PARAM_noweights = "noweights";

    // Profile Alignments Paramaters
    transient public static final String PARAM_profile = "profile";
    transient public static final String PARAM_newtree1 = "newtree1";
    transient public static final String PARAM_usetree1 = "usetree1";
    transient public static final String PARAM_newtree2 = "newtree2";
    transient public static final String PARAM_usetree2 = "usetree2";

    // Sequence to Profile Alignments Parameters
    transient public static final String PARAM_sequences = "sequence";

    // Structure Alignments Parameters
    transient public static final String PARAM_nosecstr1 = "nosecstr1";
    transient public static final String PARAM_nosecstr2 = "nosecstr2";
    transient public static final String PARAM_secstrout = "secstrout";
    transient public static final String PARAM_helixgap = "helixgap";
    transient public static final String PARAM_strandgap = "strandgap";
    transient public static final String PARAM_loopgap = "loopgap";
    transient public static final String PARAM_terminalgap = "terminalgap";
    transient public static final String PARAM_helixendin = "helixendin";
    transient public static final String PARAM_helixendout = "helixendout";
    transient public static final String PARAM_strandendin = "strandendin";
    transient public static final String PARAM_strandendout = "strandendout";

    //TREES
    transient public static final String PARAM_outputtree = "outputtree";
    transient public static final String PARAM_seed = "seed";
    transient public static final String PARAM_kimura = "kimura";
    transient public static final String PARAM_tossgaps = "tossgaps";
    transient public static final String PARAM_bootlabels = "bootlabels";
    transient public static final String PARAM_clustering = "clustering";
    transient public static final String PARAM_batch = "batch";

    transient public static final String PARAM_fasta_input_node_id = "input node id";
    transient public static final String PARAM_fasta_input_file_list = "input file list";

    public Clustalw2Task() {
        super();
        setTaskName("Clustalw2Task");
        setParameter(PARAM_fasta_input_node_id, "");
        setParameter(PARAM_fasta_input_file_list, "");
    }

    public String getDisplayName() {
        return "Clustalw2Task";
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
        StringBuffer sb = new StringBuffer();

        // VERBS input
        addCommandParameterFlag(sb, "-align", PARAM_align);
        addCommandParameterFlag(sb, "-tree", PARAM_tree);
        addCommandParameterFlag(sb, "-bootstrap", PARAM_bootstrap);
        addCommandParameterFlag(sb, "-convert", PARAM_convert);

        // General Settings Parameters
        addCommandParameterFlag(sb, "-quicktree", PARAM_quicktree);
        addCommandParameterEquals(sb, "-type", PARAM_type);
        addCommandParameterFlag(sb, "-negative", PARAM_negative);
        addCommandParameterEquals(sb, "-output", PARAM_output);
        addCommandParameterEquals(sb, "-outorder", PARAM_outorder);
        addCommandParameterEquals(sb, "-case", PARAM_clustal_case);
        addCommandParameterEquals(sb, "-seqnos", PARAM_seqnos);
        addCommandParameterEquals(sb, "-seqno_range", PARAM_seqno_range);
        addCommandParameterEquals(sb, "-range", PARAM_range);
        addCommandParameterEquals(sb, "-maxseqlen", PARAM_maxseqlen);
        addCommandParameterFlag(sb, "-quiet", PARAM_quiet);
        addCommandParameterEquals(sb, "-stats", PARAM_stats);

        // Fast Pairwaise Alignments Parameters
        addCommandParameterEquals(sb, "-ktuple", PARAM_ktuple);
        addCommandParameterEquals(sb, "-topdiags", PARAM_topdiags);
        addCommandParameterEquals(sb, "-window", PARAM_window);
        addCommandParameterEquals(sb, "-pairgap", PARAM_pairgap);
        addCommandParameterEquals(sb, "-score", PARAM_score);

        // Slow Pairwise Alignments Parameters
        addCommandParameterEquals(sb, "-pwmatrix", PARAM_pwmatrix);
        addCommandParameterEquals(sb, "-pwdnamatrix", PARAM_pwdnamatrix);
        addCommandParameterEquals(sb, "-pwgapopen", PARAM_pwgapopen);
        addCommandParameterEquals(sb, "-pwgapext", PARAM_pwgapext);

        // Multiple Alignments Parameters
        addCommandParameterEquals(sb, "-newtree", PARAM_newtree);
        addCommandParameterEquals(sb, "-usetree", PARAM_usetree);
        addCommandParameterEquals(sb, "-matrix", PARAM_matrix);
        addCommandParameterEquals(sb, "-dnamatrix", PARAM_dnamatrix);
        addCommandParameterEquals(sb, "-gapopen", PARAM_gapopen);
        addCommandParameterEquals(sb, "-gapext", PARAM_gapext);
        addCommandParameterFlag(sb, "-endgaps", PARAM_endgaps);
        addCommandParameterEquals(sb, "-gapdist", PARAM_gapdist);
        addCommandParameterFlag(sb, "-nopgap", PARAM_nopgap);
        addCommandParameterFlag(sb, "-nohpgap", PARAM_nohgap);
        addCommandParameterEquals(sb, "-hgapresidues", PARAM_hgapresidues);
        addCommandParameterEquals(sb, "-maxdiv", PARAM_maxdiv);
        addCommandParameterEquals(sb, "-transweight", PARAM_transweight);
        addCommandParameterEquals(sb, "-iteration", PARAM_iteration);
        addCommandParameterEquals(sb, "-numiter", PARAM_numiter);
        addCommandParameterFlag(sb, "-noweights", PARAM_noweights);

        // Profile Alignments Paramaters
        addCommandParameterFlag(sb, "-profile", PARAM_profile);
        addCommandParameterEquals(sb, "-newtree1", PARAM_newtree1);
        addCommandParameterEquals(sb, "-usetree1", PARAM_usetree1);
        addCommandParameterEquals(sb, "-newtree2", PARAM_newtree2);
        addCommandParameterEquals(sb, "-usetree", PARAM_usetree2);

        // Sequence to Profile Alignments Parameters
        addCommandParameterFlag(sb, "-sequences", PARAM_sequences);

        // Structure Alignments Parameters
        addCommandParameterFlag(sb, "-nosecstr1", PARAM_nosecstr1);
        addCommandParameterFlag(sb, "-nosecstr2", PARAM_nosecstr2);
        addCommandParameterEquals(sb, "-secstrout", PARAM_secstrout);
        addCommandParameterEquals(sb, "-helixgap", PARAM_helixgap);
        addCommandParameterEquals(sb, "-strandga", PARAM_strandgap);
        addCommandParameterEquals(sb, "-loopgap", PARAM_loopgap);
        addCommandParameterEquals(sb, "-terminalgap", PARAM_terminalgap);
        addCommandParameterEquals(sb, "-helixendin", PARAM_helixendin);
        addCommandParameterEquals(sb, "-helixendout", PARAM_helixendout);
        addCommandParameterEquals(sb, "-strandendin", PARAM_strandendin);
        addCommandParameterEquals(sb, "-strandendout", PARAM_strandendout);

        //TREES Parameters
        addCommandParameterEquals(sb, "-outputtree", PARAM_outputtree);
        addCommandParameterEquals(sb, "-seed", PARAM_seed);
        addCommandParameterFlag(sb, "-kimura", PARAM_kimura);
        addCommandParameterFlag(sb, "-tossgaps", PARAM_tossgaps);
        addCommandParameterEquals(sb, "-bootlabels", PARAM_bootlabels);
        addCommandParameterEquals(sb, "-clustering", PARAM_clustering);
        addCommandParameterFlag(sb, "-batch", PARAM_batch);

        return sb.toString();
    }

}