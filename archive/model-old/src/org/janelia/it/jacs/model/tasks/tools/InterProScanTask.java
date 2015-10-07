
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: May 18, 2010
 * Time: 2:16:04 PM
 */
public class InterProScanTask extends Task {

    /*
    % /usr/local/packages/iprscan/bin/iprscan -cli -help
        usage: /usr/local/packages/iprscan/bin/iprscan -cli [-email <addr>] [-appl <name> ...]
               [-nocrc] [-altjobs] [-seqtype p|n] [-trlen <N>] [-trtable <table>]
               [-iprlookup] [-goterms] -i <seqfile> [-o <output file>]

      -i <seqfile>      Your sequence file (mandatory).
      -o <output file>  The output file where to write results (optional), default is STDOUT.
      -email <addr>     Submitter email address (required for non-interactive).
      -appl <name>      Application(s) to run (optional), default is all.
                        Possible values (dependent on set-up):
                                 blastprodom
                                 fprintscan
                                    hamap
                                 hmmpfam
                                 hmmpir
                                 hmmpanther
                                 hmmtigr
                                 hmmsmart
                                 superfamily
                                 gene3d
                                 patternscan
                                 profilescan
                                 seg
                                 coils
                                 [tmhmm]
                                 [signalp]
      -nocrc            Don't perform CRC64 check, default is on.
      -altjobs          Launch jobs alternatively, chunk after chunk. Default is off.
      -seqtype <type>   Sequence type: n for DNA/RNA, p for protein (default).
      -trlen <n>        Transcript length threshold (20-150).
      -trtable <table>  Codon table number.
      -goterms          Show GO terms if iprlookup option is also given.
      -iprlookup        Switch on the InterPro lookup for results.
      -format <format>  Output results format (raw, txt, html, xml(default), ebixml(EBI header on top of xml), gff)
      -verbose          Print messages during run
    */

    transient public static final String PARAM_fasta_input_node_id = "input node id";
    transient public static final String PARAM_goterms = "goterms";
    transient public static final String PARAM_iprlookup = "iprlookup";
    transient public static final String PARAM_format = "format";

    transient public static final String format_DEFAULT = "raw";
    transient public static final String goterms_DEFAULT = "1";
    transient public static final String iprlookup_DEFAULT = "1";

    public InterProScanTask() {
        super();
        setTaskName("InterProScanTask");
        setParameter(PARAM_goterms, goterms_DEFAULT);
        setParameter(PARAM_iprlookup, iprlookup_DEFAULT);
        setParameter(PARAM_format, format_DEFAULT);
        setParameter(PARAM_fasta_input_node_id, "");
    }

    public String getDisplayName() {
        return "InterProScanTask";
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
        if (getParameter(PARAM_goterms).equals("1")) {
            sb.append(" -goterms");
        }
        if (getParameter(PARAM_iprlookup).equals("1")) {
            sb.append(" -iprlookup");
        }
        addCommandParameter(sb, "-format", PARAM_format);
        return sb.toString();
    }

}