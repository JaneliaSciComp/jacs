
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jun 14, 2010
 * Time: 10:50:04 AM
 */
public class RepeatMaskerTask extends Task {

    /*
     usage: RepeatMasker [-options] <seqfile(s) in fasta format>

    -h(elp)        detailed help

    default settings are for masking all type of repeats in a primate sequence

    repeat options
    -nolow         does not mask low_complexity DNA or simple repeats
    -l(ow)         same as nolow (historical)
    -(no)int       only masks low complex/simple repeats (no interspersed repeats)
    -norna         does not mask small RNA (pseudo) genes
    -alu           only masks Alus (and 7SLRNA, SVA and LTR5)(only for primate DNA)
    -div [number]  masks only those repeats that are less than [number] percent
                   diverged from the consensus sequence
    -lib [filename] allows usage of a custom library (e.g. from another species)
    -cutoff [number] sets cutoff score for masking repeats when using -lib
                   (default 225 should not give false positives)

    species options (choose only one!)
    -m(us)         masks rodent specific and mammalian wide repeats
    -rod(ent)      same as -mus
    -cow           masks artiodactyl, whale, and mammalian wide repeats
    -pig, -cet(acea), -art(iodactyl)  same as -cow
    -car(nivore)   mask carnivore-specific and mammalian wide repeats
    -cat -dog      same as -car
    -mam(mal)      masks repeats found in mammals not mentioned above
    -ch(icken)     masks repeats found in chicken and related birds
    -ar(abidopsis) masks repeats found in Arabidopsis
    -dr(osophila)  masks repeats found in Drosophilas
    -el(egans)     masks repeats found in C. elegans
    -fugu          masks repeats found in Takifugu rubripres

    contamination options
    -is_only       only clips E coli insertion elements out of fasta and .qual files
    -is_clip       clips IS elements before analysis (default: IS only reported)
    -no_is         skips bacterial insertion element check
    -rodspec       only checks for rodent specific repeats (no repeatmasker run)
    -primspec      only checks for primate specific repeats (no repeatmasker run)

    running options
    -w(ublast)     use WU-blast, rather than cross_match as engine
    -pa(rallel)    number of processors to use in parallel (only works for batch files or
        [number]      sequences over 50 kb)
    -s             slow search; 0-5% more sensitive, 2-3 times slower than default
    -q             quick search; 5-10% less sensitive, 2-5 times faster than default
    -qq            rush job; about 10% less sensitive, 4->10 times faster than default
    -gc [number]   use matrices calculated for 'number' percentage background GC level
    -gccalc        RepeatMasker calculates the GC content even for batch files/small seqs
    -frag [number] maximum sequence length masked without fragmenting (default 51000)
    -maxsize [nr]  maximum length for which IS- or repeat clipped sequences can be produced
                   (default 4000000). Memory requirements go up with higher maxsize.
    -nocut         skips the steps in which repeats are excised
    -noisy         prints cross_match progress report to screen (defaults to .stderr file)

    output options
    -dir [directory name] output will be written to this directory
             (default is query file directory, "-dir ." will write to current directory)
    -a       shows the alignments in a .align output file; -ali(gnments) also works
               (this option does not work in combination with -wublast)
    -inv     alignments are presented in the orientation of the repeat (with option -a)
    -cut     saves a sequence (in file.cut) from which full-length repeats are excised
    -small   returns complete .masked sequence in lower case
    -xsmall  returns repetitive regions in lowercase (rest capitals) rather than masked
    -x       returns repetitive regions masked with Xs rather than Ns
    -poly    reports simple repeats that may be polymorphic (in file.poly)
    -ace     creates an additional output file in ACeDB format
    -gff     creates an additional Gene Feature Finding format output
    -u       creates an additional annotation file not processed by ProcessRepeats
    -xm      creates an additional output file in cross_match format (for parsing)
    -fixed   creates an (old style) annotation file with fixed width columns
    -no_id   leaves out final column with unique ID for each element (was default)
    -e(xcln) calculates repeat densities (in .tbl) excluding runs of >25 Ns in the query
    */

    /*

      */
    transient public static final String PARAM_fasta_input_node_id = "input node id";

    // Repeat options
    transient public static final String PARAM_nolow = "nolow";
    transient public static final String PARAM_noint = "noint";
    transient public static final String PARAM_norna = "norna";
    transient public static final String PARAM_alu = "alu";
    transient public static final String PARAM_div = "div";
    transient public static final String PARAM_lib = "lib";
    transient public static final String PARAM_cutoff = "cutoff";

    // species options (choose only one!)
    transient public static final String PARAM_species = "species";

    // contamination options
    transient public static final String PARAM_is_only = "is only";
    transient public static final String PARAM_is_clip = "is_clip";
    transient public static final String PARAM_no_is = "no_is";
    transient public static final String PARAM_rodspec = "rodspec";
    transient public static final String PARAM_primspec = "primspec";

    // Running options
    transient public static final String PARAM_wublast = "wublast";
    transient public static final String PARAM_s = "s";
    transient public static final String PARAM_q = "q";
    transient public static final String PARAM_qq = "qq";
    transient public static final String PARAM_gc = "gc";
    transient public static final String PARAM_gccalc = "gccalc";
    transient public static final String PARAM_frag = "frag";
    transient public static final String PARAM_maxsize = "maxsize";
    transient public static final String PARAM_nocut = "nocut";
    transient public static final String PARAM_noisy = "noisy";

    // output options
    transient public static final String PARAM_ali = "ali";
    transient public static final String PARAM_inv = "inv";
    transient public static final String PARAM_cut = "cut";
    transient public static final String PARAM_small = "small";
    transient public static final String PARAM_xsmall = "xsmall";
    transient public static final String PARAM_x = "x";
    transient public static final String PARAM_poly = "poly";
    transient public static final String PARAM_ace = "ace";
    transient public static final String PARAM_gff = "gff";
    transient public static final String PARAM_u = "u";
    transient public static final String PARAM_xm = "xm";
    transient public static final String PARAM_fixed = "fixed";
    transient public static final String PARAM_no_id = "no_id";
    transient public static final String PARAM_excln = "excln";

    transient public static final String cutoff_DEFAULT = "225";


    public RepeatMaskerTask() {
        super();
        setTaskName("RepeatMaskerTask");
        setParameter(PARAM_cutoff, cutoff_DEFAULT);
        setParameter(PARAM_fasta_input_node_id, "");
    }

    public String getDisplayName() {
        return "RepeatMaskerTask";
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

    public String addParamFlag(String key, String flag) {
        String param = "";
        if (getParameter(key) != null) {
            param = flag;
        }
        return param;
    }

    // Many (often overlapping) options for species but only one choice...
    // Let's use an enum to simplify the choosing of the species.
    private enum Species {
        m, mus, rod, rodent, cow, pig, cet, cetacea, art, artiodactyl,
        car, carnivore, cat, dog, mam, mammal, ch, chicken, ar, arabidopsis,
        dr, drosophila, el, elegans, fugu, NOVALUE;

        public static Species toSpecies(String str) {
            try {
                return valueOf(str);
            }
            catch (Exception ex) {
                return NOVALUE;
            }
        }
    }

    public String addSpecies(String species) {
        String flag;

        switch (Species.toSpecies(species)) {
            case m:
            case mus:
            case rod:
            case rodent:
                flag = "-m ";
                break;
            case cow:
            case pig:
            case cet:
            case cetacea:
            case art:
            case artiodactyl:
                flag = "-cow ";
                break;
            case car:
            case carnivore:
            case cat:
            case dog:
                flag = "-car ";
                break;
            case mam:
            case mammal:
                flag = "-mam ";
                break;
            case ch:
            case chicken:
                flag = "-ch ";
                break;
            case ar:
            case arabidopsis:
                flag = "-ar";
                break;
            case dr:
            case drosophila:
                flag = "-dr ";
                break;
            case el:
            case elegans:
                flag = "-el ";
                break;
            case fugu:
                flag = "-fugu ";
                break;
            default:    // default is to assume primate origin
                flag = "";
        }
        return flag;
    }

    public String generateCommandOptions(String outputDirectory) throws ParameterException {
        StringBuffer sb = new StringBuffer();
        // Repeat options
        sb.append(addParamFlag(PARAM_nolow, "-nolow "));
        sb.append(addParamFlag(PARAM_noint, "-noint "));
        sb.append(addParamFlag(PARAM_norna, "-norna "));
        sb.append(addParamFlag(PARAM_alu, "-noalu "));
        addCommandParameter(sb, "-div", PARAM_div);
        addCommandParameter(sb, "-lib", PARAM_lib);
        addRequiredCommandParameter(sb, "-cutoff", PARAM_cutoff);

        // Species options
        sb.append(addSpecies(PARAM_species));

        // contamination options
        sb.append(addParamFlag(PARAM_is_only, "-is_only "));
        sb.append(addParamFlag(PARAM_is_clip, "-is_clip "));
        sb.append(addParamFlag(PARAM_no_is, "-no_is "));
        sb.append(addParamFlag(PARAM_rodspec, "-rodspec "));
        sb.append(addParamFlag(PARAM_primspec, "-primspec "));

        // Running options
        sb.append(addParamFlag(PARAM_wublast, "-w "));
        sb.append(addParamFlag(PARAM_s, "-s "));
        sb.append(addParamFlag(PARAM_q, "-q "));
        sb.append(addParamFlag(PARAM_qq, "-qq "));
        addCommandParameter(sb, "-gc", PARAM_gc);
        sb.append(addParamFlag(PARAM_gccalc, "-gccalc "));
        addCommandParameter(sb, " -frag", PARAM_frag);
        addCommandParameter(sb, " -maxsize", PARAM_maxsize);
        sb.append(addParamFlag(PARAM_nocut, "-nocut "));
        sb.append(addParamFlag(PARAM_noisy, "-noisy "));

        // Output options
        sb.append(addParamFlag(PARAM_ali, "-ali "));
        sb.append(addParamFlag(PARAM_inv, "-inv "));
        sb.append(addParamFlag(PARAM_cut, "-cut "));
        sb.append(addParamFlag(PARAM_small, "-small "));
        sb.append(addParamFlag(PARAM_xsmall, "-xsmall "));
        sb.append(addParamFlag(PARAM_x, "-x "));
        sb.append(addParamFlag(PARAM_poly, "-poly "));
        sb.append(addParamFlag(PARAM_ace, "-ace "));
        sb.append(addParamFlag(PARAM_gff, "-gff "));
        sb.append(addParamFlag(PARAM_u, "-u "));
        sb.append(addParamFlag(PARAM_xm, "-xm "));
        sb.append(addParamFlag(PARAM_fixed, "-fixed "));
        sb.append(addParamFlag(PARAM_no_id, "-no_id "));
        sb.append(addParamFlag(PARAM_excln, "-excln "));

        // Output directory is determined by jacs
        if (outputDirectory != null && outputDirectory.trim().length() > 0) {
            sb.append(" -dir ");
            sb.append(outputDirectory);
        }

        return sb.toString();
    }

}