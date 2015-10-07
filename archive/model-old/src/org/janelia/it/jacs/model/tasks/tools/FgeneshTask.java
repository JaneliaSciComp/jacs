
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jun 10, 2010
 * Time: 12:16:04 PM
 */
public class FgeneshTask extends Task {

    /* fgenesh helpinfo:
     FGENESH/FGENSH+ programm. v.2.4
     Usage:
     Programm Par_file Seq_File <Other options>
         Par_File - Parameters for GeneFinder.
         Seq_File - Nucleotide sequences for for scan.

       Option:
       -GC:xxx            -  Use potentioal GC donor splice sites with score above
                             xxx. xxx is floating point value corresponded the number
                             of matched letters in site consensus (xxx >= 0).
       -GC                -  Use all potentioal GC donor splice sites.
       -p1:xxx            -  Get sequence from position xxx.
       -p2:xxx            -  Get sequence to position xxx.
       -c                 -  Use condensed sequence.
       -exon_table:file   -  File with table of exons.
       -exon_bonus:xxx    -  Add bonus xxx for all exons from table.
       -pmrna             -  Print mRNA sequences for predicted genes.
       -pexons            -  Print exons sequences for predicted genes.
       -min_thr:xx        -  Treshold for exons.
       -scip_prom         -  Do not consider BAD promoters.
       -scip_term         -  Do not consider BAD terminators (polyA signals).
       -min_f_exon:xx     -  Set minimal first exon length to xx.
       -min_i_exon:xx     -  Set minimal internal exon length to xx.
       -min_t_exon:xx     -  Set minimal terminal exon length to xx.
       -min_s_exon:xx     -  Set minimal single exon length to xx.

    ------- Multivariants options --------------------------------------------------------------
       -nvar:N            -  Output N best alternate prediction variants.
       -try_best_exons:N  -  Tray include to prediction N best exons, that is not included
                             to initial prediction.
       -try_best_sites:N  -  Tray include to prediction N best splice sites, that is not
                             ncluded to initial prediction.
       -not_rem           -  Do not tray to remove initially predicted exons.
       -vthr:N            -  Do not conside prediction variant with score less then N percent
                             of initial prediction (default value 75%.

    ------- Translation table selection --------------------------------------------------------
       -t:table           -  Use translation table.
       -st:table          -  Print selected translation table.

         Table values are -
         1 - Standard. (Default)
         2 - Vertebrate Mitochondrial.
         3 - Yeast Mitochondrial.
         4 - Mold Mitochondria, Protozoan Mitochondrial, Colenterate Mitochondrial,
             Mycoplasma, Spiroplasma.
         5 - Invertebrate Mitochondrial.
         6 - Ciliate Nuclear, Dasycladacean Nuclear, Hexamita Nuclear.
         9 - Echinoderm Nuclear.
        10 - Euplotid Nuclear.
        11 - Bacterial.
        12 - Alternative Yeast Nuclear.
        13 - Ascidian Mitochondrial.
        14 - Flatworm Mitochondrial.
        15 - Blepharisma Macronuclear.


     */


    transient public static final String PARAM_fasta_input_node_id = "input node id";
    transient public static final String PARAM_par_file = "par file";
    transient public static final String PARAM_GC_cutoff = "GC cutoff";
    transient public static final String PARAM_position_1 = "position 1";
    transient public static final String PARAM_position_2 = "position 2";
    transient public static final String PARAM_condensed = "condensed";
    transient public static final String PARAM_exon_table = "exon table";
    transient public static final String PARAM_exon_bonus = "exon bonus";
    transient public static final String PARAM_pmrna = "pmrna";
    transient public static final String PARAM_pexons = "pexons";
    transient public static final String PARAM_min_thr = "min thr";
    transient public static final String PARAM_scp_prom = "scp prom";
    transient public static final String PARAM_scp_term = "scp term";
    transient public static final String PARAM_min_f_exon = "min f exon";
    transient public static final String PARAM_min_i_exon = "min i exon";
    transient public static final String PARAM_min_t_exon = "min t exon";
    transient public static final String PARAM_min_s_exon = "min s exon";
    //------- Multivariants options
    transient public static final String PARAM_nvar = "nvar";
    transient public static final String PARAM_try_best_exons = "try best exons";
    transient public static final String PARAM_try_best_sites = "try best sites";
    transient public static final String PARAM_not_rem = "not rem";
    transient public static final String PARAM_vthr = "vthr";
    //------- Translation table selection
    transient public static final String PARAM_use_table = "use table";
    transient public static final String PARAM_show_table = "show table";


    public FgeneshTask() {
        super();
        setTaskName("FgeneshTask");
        setParameter(PARAM_fasta_input_node_id, "");
    }

    public String getDisplayName() {
        return "FgeneshTask";
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

    public String getParFile() throws ParameterException {

        if (getParameter(PARAM_par_file) != null) {
            return getParameter(PARAM_par_file);
        }
        else return null;

    }

    public String addParamWithValue(String key, String flag) {
        String param = "";
        if (getParameter(key) != null) {
            param = flag;
            param += getParameter(key);
            param += " ";
        }
        return param;

    }

    public String addParamFlag(String key, String flag) {
        String param = "";
        if (getParameter(key) != null) {
            param = flag;
        }
        return param;
    }

    public String generateCommandOptions() throws ParameterException {
        StringBuffer sb = new StringBuffer();

        sb.append(addParamWithValue(PARAM_GC_cutoff, "-GC:"));
        sb.append(addParamWithValue(PARAM_position_1, "-p1:"));
        sb.append(addParamWithValue(PARAM_position_2, "-p2:"));
        sb.append(addParamFlag(PARAM_condensed, "-c "));
        sb.append(addParamWithValue(PARAM_exon_table, "-exon_table:"));
        sb.append(addParamWithValue(PARAM_exon_bonus, "-exon_bonus:"));
        sb.append(addParamFlag(PARAM_pmrna, "-pmrna "));
        sb.append(addParamFlag(PARAM_pexons, "-pexons "));
        sb.append(addParamWithValue(PARAM_min_thr, "-min_thr:"));
        sb.append(addParamFlag(PARAM_scp_prom, "-scp_prom "));
        sb.append(addParamFlag(PARAM_scp_term, "-scp_term "));
        sb.append(addParamWithValue(PARAM_min_f_exon, "-min_f_exon:"));
        sb.append(addParamWithValue(PARAM_min_i_exon, "-min_i_exon:"));
        sb.append(addParamWithValue(PARAM_min_t_exon, "-min_t_exon:"));
        sb.append(addParamWithValue(PARAM_min_s_exon, "-min_s_exon:"));
        sb.append(addParamWithValue(PARAM_nvar, "-nvar:"));
        sb.append(addParamWithValue(PARAM_try_best_exons, "-try_best_exons:"));
        sb.append(addParamWithValue(PARAM_try_best_sites, "-try_best_sites:"));
        sb.append(addParamFlag(PARAM_not_rem, "-not_rem "));
        sb.append(addParamWithValue(PARAM_vthr, "-vthr:"));
        sb.append(addParamWithValue(PARAM_use_table, "-t:"));
        sb.append(addParamWithValue(PARAM_show_table, "-st:"));

        return sb.toString();
    }

}