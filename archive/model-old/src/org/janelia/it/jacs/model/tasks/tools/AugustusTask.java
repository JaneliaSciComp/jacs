
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jul 1, 2010
 * Time: 11:00:00 AM
 */
public class AugustusTask extends Task {

    /*  Augustus helpinfo:
        AUGUSTUS (2.3) is a gene prediction tool for eukaryotes
        written by Mario Stanke (mstanke@gwdg.de) and Oliver Keller (keller@cs.uni-goettingen.de).

        usage:
        augustus [parameters] --species=SPECIES queryfilename

        'queryfilename' is the filename (including relative path) to the file containing the query sequence(s)
        in fasta format.

        SPECIES is an identifier for the species. Use --species=help to see a list.

        parameters:
        --strand=both, --strand=forward or --strand=backward
        --genemodel=partial, --genemodel=intronless, --genemodel=complete, --genemodel=atleastone or --genemodel=exactlyone
          partial      : allow prediction of incomplete genes at the sequence boundaries (default)
          intronless   : only predict single-exon genes like in prokaryotes and some eukaryotes
          complete     : only predict complete genes
          atleastone   : predict at least one complete gene
          exactlyone   : predict exactly one complete gene
        --singlestrand=true
          predict genes independently on each strand, allow overlapping genes on opposite strands
          This option is turned off by default.
        --hintsfile=hintsfilename
          When this option is used the prediction considering hints (extrinsic information) is turned on.
          hintsfilename contains the hints in gff format.
        --AUGUSTUS_CONFIG_PATH=path
          path to config directory (if not specified as environment variable)
        --alternatives-from-evidence=true/false
          report alternative transcripts when they are suggested by hints
        --alternatives-from-sampling=true/false
          report alternative transcripts generated through probabilistic sampling
        --sample=n
        --minexonintronprob=p
        --minmeanexonintronprob=p
        --maxtracks=n
          For a description of these parameters see section 4 of README.TXT.
        --progress=true
          show a progressmeter
        --gff3=on/off
          output in gff3 format
        --predictionStart=A, --predictionEnd=B
          A and B define the range of the sequence for which predictions should be found.
        --UTR=on/off
          predict the untranslated regions in addition to the coding sequence. This currently works only for a subset of species.
        --noInFrameStop=true/false
          Do not report transcripts with in-frame stop codons. Otherwise, intron-spanning stop codons could occur. Default: false
        --noprediction=true/false
          If true and input is in genbank format, no prediction is made. Useful for getting the annotated protein sequences.
        --uniqueGeneId=true/false
          If true, output gene identifyers like this: seqname.gN

        For a complete list of parameters, type "augustus --paramlist".
        An exhaustive description can be found in the file README.TXT.
     */

    transient public static final String PARAM_species = "species";
    transient public static final String PARAM_strand = "strand";
    transient public static final String PARAM_geneModel = "genemodel";
    transient public static final String PARAM_singleStrand = "singlestrand";
    transient public static final String PARAM_hintsFile = "hintsfile";
    transient public static final String PARAM_augustusConfigPath = "AUGUSTUS_CONFIG_PATH";
    transient public static final String PARAM_alternativesFromEvidence = "alternatives-from-evidence";
    transient public static final String PARAM_alternativesFromSampling = "alternatives-from-sampling";
    transient public static final String PARAM_sample = "sample";
    transient public static final String PARAM_minExonIntronProb = "minexonintronprob";
    transient public static final String PARAM_minMeanExonIntronProb = "minmeanexonintronprob";
    transient public static final String PARAM_maxTracks = "maxtracks";
    transient public static final String PARAM_progress = "progress";
    transient public static final String PARAM_gff3 = "gff3";
    transient public static final String PARAM_predictionStart = "predictionStart";
    transient public static final String PARAM_predictionEnd = "predictionEnd";
    transient public static final String PARAM_UTR = "UTR";
    transient public static final String PARAM_noInFrameStop = "noInFrameStop";
    transient public static final String PARAM_noPrediction = "noprediction";
    transient public static final String PARAM_uniqueGeneId = "uniqueGeneId";
    transient public static final String PARAM_fasta_input_node_id = "input node id";

    transient public static final String strand_DEFAULT = "both";
    transient public static final String geneModel_DEFAULT = "partial";
    transient public static final String noInFrameStop_DEFAULT = "false";
    transient public static final String augustusConfigPath_DEFAULT = SystemConfigurationProperties.getString("Augustus.ConfigPath");

    public AugustusTask() {
        super();
        setTaskName("AugustusTask");
        setParameter(PARAM_strand, strand_DEFAULT);
        setParameter(PARAM_geneModel, geneModel_DEFAULT);
        setParameter(PARAM_noInFrameStop, noInFrameStop_DEFAULT);
        setParameter(PARAM_augustusConfigPath, augustusConfigPath_DEFAULT);
        setParameter(PARAM_fasta_input_node_id, "");
    }

    public String getDisplayName() {
        return "AugustusTask";
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

    // Overiding the next two methods because augustus REQUIRES an = between the parameter name and value.
    protected void addRequiredCommandParameter(StringBuffer sb, String prefix, String parameter) throws ParameterException {
        if (!addCommandParameter(sb, prefix, parameter)) {
            throw new ParameterException("Could not find required parameter=" + parameter);
        }
    }

    protected boolean addCommandParameter(StringBuffer sb, String prefix, String parameter) {
        if (parameterDefined(parameter)) {
            sb.append(" ").append(prefix).append("=").append(getParameter(parameter));
            return true;
        }
        else {
            return false;
        }
    }

    public String generateCommandOptions() throws ParameterException {
        StringBuffer sb = new StringBuffer();
        addRequiredCommandParameter(sb, "--species", PARAM_species);
        addCommandParameter(sb, "--strand", PARAM_strand);
        addCommandParameter(sb, "--genemodel", PARAM_geneModel);
        addCommandParameter(sb, "--singlestrand", PARAM_singleStrand);
        addCommandParameter(sb, "--hintsfile", PARAM_hintsFile);
        addRequiredCommandParameter(sb, "--AUGUSTUS_CONFIG_PATH", PARAM_augustusConfigPath);
        addCommandParameter(sb, "--alternatives_from_evidence", PARAM_alternativesFromEvidence);
        addCommandParameter(sb, "--alternatives_from_sampling", PARAM_alternativesFromSampling);
        addCommandParameter(sb, "--sample", PARAM_sample);
        addCommandParameter(sb, "--minexonintronprob", PARAM_minExonIntronProb);
        addCommandParameter(sb, "--minmeanexonintronprob", PARAM_minMeanExonIntronProb);
        addCommandParameter(sb, "--maxtracks", PARAM_maxTracks);
        addCommandParameter(sb, "--progress", PARAM_progress);
        addCommandParameter(sb, "--gff3", PARAM_gff3);
        addCommandParameter(sb, "--predictionStart", PARAM_predictionStart);
        addCommandParameter(sb, "--predictionEnd", PARAM_predictionEnd);
        addCommandParameter(sb, "--UTR", PARAM_UTR);
        addCommandParameter(sb, "--noInFrameStop", PARAM_noInFrameStop);
        addCommandParameter(sb, "--noprediction", PARAM_noPrediction);
        addCommandParameter(sb, "--uniqueGeneId", PARAM_uniqueGeneId);
        return sb.toString();
    }

}