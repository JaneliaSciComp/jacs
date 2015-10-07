
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jul 6, 2010
 * Time: 09:11:00 AM
 */
public class ExonerateTask extends Task {

    /*  Exonerate -h output below.  For more explenation run exonerate with --help, or see relevant
        sections from the Exonerate man page at http://www.ebi.ac.uk/~guy/exonerate/exonerate.man.html

        exonerate from exonerate version 2.2.0
        Using glib version 2.12.3
        Built on Feb  9 2009
        Branch: unnamed branch

        exonerate: A generic sequence comparison tool
        Guy St.C. Slater. guy@ebi.ac.uk. 2000-2008.


        Examples of use:

        1. Ungapped alignment of any DNA or protein sequences:
            exonerate queries.fa targets.fa
        2. Gapped alignment of Mouse proteins to Fugu proteins:
            exonerate --model affine:local mouse.fa fugu.fa
        3. Find top 10 matches of each EST to a genome:
            exonerate --model est2genome --bestn 10 est.fa genome.fa
        4. Find proteins with at least a 50% match to a genome:
            exonerate --model protein2genome --percent 50 p.fa g.fa
        5. Perform a full Smith-Waterman-Gotoh alignment:
            exonerate --model affine:local --exhaustive yes a.fa b.fa
        6. Many more combinations are possible.  To find out more:
            exonerate --help
            man exonerate


        General Options:
        ---------------
        -h --shorthelp [FALSE] <TRUE>
           --help [FALSE]
        -v --version [FALSE]
        
        Sequence Input Options:
        ----------------------
        -q --query [mandatory]  <*** empty list ***>
        -t --target [mandatory]  <*** empty list ***>
        -Q --querytype [unknown]
        -T --targettype [unknown]
           --querychunkid [0]
           --targetchunkid [0]
           --querychunktotal [0]
           --targetchunktotal [0]
        -V --verbose [1]
        
        Analysis Options:
        ----------------
        -E --exhaustive [FALSE]
        -B --bigseq [FALSE]
           --forcescan [none]
           --saturatethreshold [0]
           --customserver [NULL]
        
        Fasta Database Options:
        ----------------------
           --fastasuffix [.fa]
        
        Gapped Alignment Options:
        ------------------------
        -m --model [ungapped]
        -s --score [100]
           --percent [0.0]
           --showalignment [TRUE]
           --showsugar [FALSE]
           --showcigar [FALSE]
           --showvulgar [TRUE]
           --showquerygff [FALSE]
           --showtargetgff [FALSE]
           --ryo [NULL]
        -n --bestn [0]
        -S --subopt [TRUE]
        -g --gappedextension [TRUE]
           --refine [none]
           --refineboundary [32]
        
        Viterbi algorithm options:
        -------------------------
        -D --dpmemory [32]
        
        Code generation options:
        -----------------------
        -C --compiled [TRUE]
        
        Heuristic Options:
        -----------------
           --terminalrangeint [12]
           --terminalrangeext [12]
           --joinrangeint [12]
           --joinrangeext [12]
           --spanrangeint [12]
           --spanrangeext [12]
        
        Seeded Dynamic Programming options:
         ----------------------------------
        -x --extensionthreshold [50]
           --singlepass [TRUE]
        
        BSDP algorithm options:
        ----------------------
           --joinfilter [0]
        
        Sequence Options:
        ----------------
        -A --annotation [none]
        
        Symbol Comparison Options:
        -------------------------
           --softmaskquery [FALSE]
           --softmasktarget [FALSE]
        -d --dnasubmat [nucleic]
        -p --proteinsubmat [blosum62]
        
        Alignment Seeding Options:
        -------------------------
        -M --fsmmemory [64]
           --forcefsm [none]
           --wordjump [1]
        
        Affine Model Options:
        --------------------
        -o --gapopen [-12]
        -e --gapextend [-4]
           --codongapopen [-18]
           --codongapextend [-8]
        
        NER Model Options:
        -----------------
           --minner [10]
           --maxner [50000]
           --neropen [-20]
        
        Intron Modelling Options:
        ------------------------
           --minintron [30]
           --maxintron [200000]
        -i --intronpenalty [-30]
        
        Frameshift Options:
        ------------------
        -f --frameshift [-28]
        
        Alphabet Options:
        ----------------
           --useaatla [TRUE]
        
        Translation Options:
        -------------------
           --geneticcode [1]
        
        HSP creation options:
        --------------------
           --hspfilter [0] 
           --useworddropoff [TRUE] 
           --seedrepeat [1] 
           --dnawordlen [12] 
           --proteinwordlen [6] 
           --codonwordlen [12] 
           --dnahspdropoff [30] 
           --proteinhspdropoff [20] 
           --codonhspdropoff [40] 
           --dnahspthreshold [75] 
           --proteinhspthreshold [30] 
           --codonhspthreshold [50] 
           --dnawordlimit [0] 
           --proteinwordlimit [4] 
           --codonwordlimit [4] 
           --geneseed [0] 
           --geneseedrepeat [3] 
        
        Alignment options:
        -----------------
           --alignmentwidth [80] 
           --forwardcoordinates [TRUE] 
        
        SAR Options:
        -----------
           --quality [0] 
        
        Splice Site Prediction Options:
        ------------------------------
           --splice3 [primate] 
           --splice5 [primate] 
           --forcegtag [FALSE] 
        
        --
     */

    // Sequence Input Options
    transient public static final String PARAM_query_fasta_node_id = "query node id";
    transient public static final String PARAM_target_fasta_node_id = "target node id";
    transient public static final String PARAM_querytype = "querytype";
    transient public static final String PARAM_targettype = "targettype";
    transient public static final String PARAM_querychunkidtype = "querychunkidtype";
    transient public static final String PARAM_targetchunkidtype = "targetchunkidtype";
    transient public static final String PARAM_querychunktotaltype = "querychunktotaltype";
    transient public static final String PARAM_targetchunktotaltype = "targetchunktotaltype";
    transient public static final String PARAM_verbose = "verbose";
    // Analysis Options
    transient public static final String PARAM_exhaustive = "exhaustive";
    transient public static final String PARAM_bigseq = "bigseq";
    transient public static final String PARAM_forcescan = "forcescan";
    transient public static final String PARAM_saturatethreshold = "saturatethreshold";
    transient public static final String PARAM_customserver = "customserver";
    // Fasta Database Options
    transient public static final String PARAM_fastasuffix = "fastasuffix";
    // Gapped Alignment Options
    transient public static final String PARAM_model = "model";
    transient public static final String PARAM_score = "score";
    transient public static final String PARAM_percent = "percent";
    transient public static final String PARAM_showalignment = "showalignment";
    transient public static final String PARAM_showsugar = "showsugar";
    transient public static final String PARAM_showcigar = "showcigar";
    transient public static final String PARAM_showvulgar = "showvulgar";
    transient public static final String PARAM_showquerygff = "showquerygff";
    transient public static final String PARAM_showtargetgff = "showtargetgff";
    transient public static final String PARAM_ryo = "ryo";
    transient public static final String PARAM_bestn = "bestn";
    transient public static final String PARAM_subopt = "subopt";
    transient public static final String PARAM_gappedextension = "gappedextension";
    transient public static final String PARAM_refine = "refine";
    transient public static final String PARAM_refineboundary = "refineboundary";
    // Viterbi algorithm options
    transient public static final String PARAM_dpmemory = "dpmemory";
    // Code generation options
    transient public static final String PARAM_compiled = "compiled";
    // Heuristic Options
    transient public static final String PARAM_terminalrangeint = "terminalrangeint";
    transient public static final String PARAM_terminalrangeext = "terminalrangeext";
    transient public static final String PARAM_joinrangeint = "joinrangeint";
    transient public static final String PARAM_joinrangeext = "joinrangeext";
    transient public static final String PARAM_spanrangeint = "spanrangeint";
    transient public static final String PARAM_spanrangeext = "spanrangeext";
    // Seeded Dynamic Programming options
    transient public static final String PARAM_extensionthreshold = "extensionthreshold";
    transient public static final String PARAM_singlepass = "singlepass";
    // BSDP algorithm options
    transient public static final String PARAM_joinfilter = "joinfilter";
    // Sequence Options
    transient public static final String PARAM_annotation = "annotation";
    // Symbol Comparison Options
    transient public static final String PARAM_softmaskquery = "softmaskquery";
    transient public static final String PARAM_softmasktarget = "softmasktarget";
    transient public static final String PARAM_dnasubmat = "dnasubmat";
    transient public static final String PARAM_proteinsubmat = "proteinsubmat";
    // Alignment Seeding Options
    transient public static final String PARAM_fsmmemory = "fsmmemory";
    transient public static final String PARAM_forcefsm = "forcefsm";
    transient public static final String PARAM_wordjump = "wordjump";
    // Affine Model Options
    transient public static final String PARAM_gapopen = "gapopen";
    transient public static final String PARAM_gapextend = "gapextend";
    transient public static final String PARAM_codongapopen = "codongapopen";
    transient public static final String PARAM_codongapextend = "codongapextend";
    // NER Model Options
    transient public static final String PARAM_minner = "minner";
    transient public static final String PARAM_maxner = "maxner";
    transient public static final String PARAM_neropen = "neropen";
    // Intron Modelling Options
    transient public static final String PARAM_minintron = "minintron";
    transient public static final String PARAM_maxintron = "maxintron";
    transient public static final String PARAM_intronpenalty = "intronpenalty";
    // Frameshift Options
    transient public static final String PARAM_frameshift = "frameshift";
    // Alphabet Options
    transient public static final String PARAM_useaatla = "useaatla";
    // Translation Options
    transient public static final String PARAM_geneticcode = "geneticcode";
    // HSP creation options
    transient public static final String PARAM_hspfilter = "hspfilter";
    transient public static final String PARAM_useworddropoff = "useworddropoff";
    transient public static final String PARAM_seedrepeat = "seedrepeat";
    transient public static final String PARAM_dnawordlen = "dnawordlen";
    transient public static final String PARAM_proteinwordlen = "proteinwordlen";
    transient public static final String PARAM_codonwordlen = "codonwordlen";
    transient public static final String PARAM_dnahspdropoff = "dnahspdropoff";
    transient public static final String PARAM_proteinhspdropoff = "proteinhspdropoff";
    transient public static final String PARAM_codonhspdropoff = "codonhspdropoff";
    transient public static final String PARAM_dnahspthreshold = "dnahspthreshold";
    transient public static final String PARAM_proteinhspthreshold = "proteinhspthreshold";
    transient public static final String PARAM_codonhspthreshold = "codonhspthreshold";
    transient public static final String PARAM_dnawordlimit = "dnawordlimit";
    transient public static final String PARAM_proteinwordlimit = "proteinwordlimit";
    transient public static final String PARAM_codonwordlimit = "codonwordlimit";
    transient public static final String PARAM_geneseed = "geneseed";
    transient public static final String PARAM_geneseedrepeat = "geneseedrepeat";
    // Alignment options
    transient public static final String PARAM_alignmentwidth = "alignmentwidth";
    transient public static final String PARAM_forwardcoordinates = "forwardcoordinates";
    // SAR Options
    transient public static final String PARAM_quality = "quality";
    // Splice Site Prediction Options
    transient public static final String PARAM_splice3 = "splice3";
    transient public static final String PARAM_splice5 = "splice5";
    transient public static final String PARAM_forcegtag = "forcegtag";

    public ExonerateTask() {
        super();
        setTaskName("ExonerateTask");
        setParameter(PARAM_query_fasta_node_id, "");
        setParameter(PARAM_target_fasta_node_id, "");
    }

    public String getDisplayName() {
        return "ExonerateTask";
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
        // Sequence Input Options
        addCommandParameter(sb, "--querytype", PARAM_querytype);
        addCommandParameter(sb, "--targettype", PARAM_targettype);
        addCommandParameter(sb, "--querychunkidtype", PARAM_querychunkidtype);
        addCommandParameter(sb, "--targetchunkidtype", PARAM_targetchunkidtype);
        addCommandParameter(sb, "--querychunktotaltype", PARAM_querychunktotaltype);
        addCommandParameter(sb, "--targetchunktotaltype", PARAM_targetchunktotaltype);
        addCommandParameter(sb, "--verbose", PARAM_verbose);
        // Analysis Options
        addCommandParameter(sb, "--exhaustive", PARAM_exhaustive);
        addCommandParameter(sb, "--bigseq", PARAM_bigseq);
        addCommandParameter(sb, "--forcescan", PARAM_forcescan);
        addCommandParameter(sb, "--saturatethreshold", PARAM_saturatethreshold);
        addCommandParameter(sb, "--customserver", PARAM_customserver);
        // Fasta Database Options
        addCommandParameter(sb, "--fastasuffix", PARAM_fastasuffix);
        // Gapped Alignment Options
        addCommandParameter(sb, "--model", PARAM_model);
        addCommandParameter(sb, "--score", PARAM_score);
        addCommandParameter(sb, "--percent", PARAM_percent);
        addCommandParameter(sb, "--showalignment", PARAM_showalignment);
        addCommandParameter(sb, "--showsugar", PARAM_showsugar);
        addCommandParameter(sb, "--showcigar", PARAM_showcigar);
        addCommandParameter(sb, "--showvulgar", PARAM_showvulgar);
        addCommandParameter(sb, "--showquerygff", PARAM_showquerygff);
        addCommandParameter(sb, "--showtargetgff", PARAM_showtargetgff);
        addCommandParameterQuoted(sb, "--ryo", PARAM_ryo, "\"");
        addCommandParameter(sb, "--bestn", PARAM_bestn);
        addCommandParameter(sb, "--subopt", PARAM_subopt);
        addCommandParameter(sb, "--gappedextension", PARAM_gappedextension);
        addCommandParameter(sb, "--refine", PARAM_refine);
        addCommandParameter(sb, "--refineboundary", PARAM_refineboundary);
        // Viterbi algorithm options
        addCommandParameter(sb, "--dpmemory", PARAM_dpmemory);
        // Code generation options
        addCommandParameter(sb, "--compiled", PARAM_compiled);
        // Heuristic Options
        addCommandParameter(sb, "--terminalrangeint", PARAM_terminalrangeint);
        addCommandParameter(sb, "--terminalrangeext", PARAM_terminalrangeext);
        addCommandParameter(sb, "--joinrangeint", PARAM_joinrangeint);
        addCommandParameter(sb, "--joinrangeext", PARAM_joinrangeext);
        addCommandParameter(sb, "--spanrangeint", PARAM_spanrangeint);
        addCommandParameter(sb, "--spanrangeext", PARAM_spanrangeext);
        // Seeded Dynamic Programming options
        addCommandParameter(sb, "--extensionthreshold", PARAM_extensionthreshold);
        addCommandParameter(sb, "--singlepass", PARAM_singlepass);
        // BSDP algorithm options
        addCommandParameter(sb, "--joinfilter", PARAM_joinfilter);
        // Sequence Options
        addCommandParameter(sb, "--annotation", PARAM_annotation);
        // Symbol Comparison Options
        addCommandParameter(sb, "--softmaskquery", PARAM_softmaskquery);
        addCommandParameter(sb, "--softmasktarget", PARAM_softmasktarget);
        addCommandParameter(sb, "--dnasubmat", PARAM_dnasubmat);
        addCommandParameter(sb, "--proteinsubmat", PARAM_proteinsubmat);
        // Alignment Seeding Options
        addCommandParameter(sb, "--fsmmemory", PARAM_fsmmemory);
        addCommandParameter(sb, "--forcefsm", PARAM_forcefsm);
        addCommandParameter(sb, "--wordjump", PARAM_wordjump);
        // Affine Model Options
        addCommandParameter(sb, "--gapopen", PARAM_gapopen);
        addCommandParameter(sb, "--gapextend", PARAM_gapextend);
        addCommandParameter(sb, "--codongapopen", PARAM_codongapopen);
        addCommandParameter(sb, "--codongapextend", PARAM_codongapextend);
        // NER Model Options
        addCommandParameter(sb, "--minner", PARAM_minner);
        addCommandParameter(sb, "--maxner", PARAM_maxner);
        addCommandParameter(sb, "--neropen", PARAM_neropen);
        // Intron Modelling Options
        addCommandParameter(sb, "--minintron", PARAM_minintron);
        addCommandParameter(sb, "--maxintron", PARAM_maxintron);
        addCommandParameter(sb, "--intronpenalty", PARAM_intronpenalty);
        // Frameshift Options
        addCommandParameter(sb, "--frameshift", PARAM_frameshift);
        // Alphabet Options
        addCommandParameter(sb, "--useaatla", PARAM_useaatla);
        // Translation Options
        addCommandParameter(sb, "--geneticcode", PARAM_geneticcode);
        // HSP creation options
        addCommandParameter(sb, "--hspfilter", PARAM_hspfilter);
        addCommandParameter(sb, "--useworddropoff", PARAM_useworddropoff);
        addCommandParameter(sb, "--seedrepeat", PARAM_seedrepeat);
        addCommandParameter(sb, "--dnawordlen", PARAM_dnawordlen);
        addCommandParameter(sb, "--proteinwordlen", PARAM_proteinwordlen);
        addCommandParameter(sb, "--codonwordlen", PARAM_codonwordlen);
        addCommandParameter(sb, "--dnahspdropoff", PARAM_dnahspdropoff);
        addCommandParameter(sb, "--proteinhspdropoff", PARAM_proteinhspdropoff);
        addCommandParameter(sb, "--codonhspdropoff", PARAM_codonhspdropoff);
        addCommandParameter(sb, "--dnahspthreshold", PARAM_dnahspthreshold);
        addCommandParameter(sb, "--proteinhspthreshold", PARAM_proteinhspthreshold);
        addCommandParameter(sb, "--codonhspthreshold", PARAM_codonhspthreshold);
        addCommandParameter(sb, "--dnawordlimit", PARAM_dnawordlimit);
        addCommandParameter(sb, "--proteinwordlimit", PARAM_proteinwordlimit);
        addCommandParameter(sb, "--codonwordlimit", PARAM_codonwordlimit);
        addCommandParameter(sb, "--geneseed", PARAM_geneseed);
        addCommandParameter(sb, "--geneseedrepeat", PARAM_geneseedrepeat);
        // Alignment options
        addCommandParameter(sb, "--alignmentwidth", PARAM_alignmentwidth);
        addCommandParameter(sb, "--forwardcoordinates", PARAM_forwardcoordinates);
        // SAR Options
        addCommandParameter(sb, "--quality", PARAM_quality);
        // Splice Site Prediction Options
        addCommandParameter(sb, "--splice3", PARAM_splice3);
        addCommandParameter(sb, "--splice5", PARAM_splice5);
        addCommandParameter(sb, "--forcegtag", PARAM_forcegtag);
        return sb.toString();
    }

}