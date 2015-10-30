
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
public class SignalpTask extends Task {

    /*
    Relevant sections from the Signalp man page http://www.cbs.dtu.dk/cgi-bin/nph-runsafe?man=signalp

	  -t type | -type type
	       Use networks and	models trained on sequences from the
	       specified type of organisms. The	valid types are:

		    gram-  Gram-negative bacteria
		    gram+  Gram-positive bacteria
		    euk	   eukaryotes

	       This option must	always be set! There is	no default.
	       Only one	type can be selected in	a given	run.

	  -d dir | -destination	dir
	       Place output and	graphics files in directory dir. The
	       default is the current directory.

	  -f format | -format format
	       Produce output in the specified format. The valid
	       formats are:

	       full Write the entire output, including nn and hmm
		    score for each position in each sequence. This is
		    the	default.

	       summary
		    Write only the concluding scores for each
		    sequence.

	       short
		    Write only one line	of concluding scores per
		    sequence. Intended for analysis of large datasets
		    where machine-readable output is required.
		    Incompatible with graphics.

	  -m method | -method method
	       Use the specified prediction method. The	valid methods
	       are nn (neural networks), hmm (hidden Markov models)
	       and nn+hmm (both, the default).

	  -trunc n
	       Truncate	each sequence to maximally n N-terminal
	       residues. It is recommended that	the N-terminal part
	       only (typically,	50-70 amino acids) of each sequence is
	       used for	prediction. A longer sequence will increase
	       the risk	of false positives and make the	graphical
	       output difficult	to read. If n equals 0 (zero) or the
	       option is absent, truncation is disabled.

     */

    transient public static final String PARAM_type_of_organism = "type of organism";
    transient public static final String PARAM_format = "format";
    transient public static final String PARAM_method = "method";
    transient public static final String PARAM_truncate_length = "truncate length";
    transient public static final String PARAM_fasta_input_node_id = "input node id";

    transient public static final String format_DEFAULT = "full";
    transient public static final String method_DEFAULT = "nn+hmm";
    transient public static final String truncate_length_DEFAULT = "60";


    public SignalpTask() {
        super();
        setTaskName("SignalpTask");
        setParameter(PARAM_format, format_DEFAULT);
        setParameter(PARAM_method, method_DEFAULT);
        setParameter(PARAM_truncate_length, truncate_length_DEFAULT);
        setParameter(PARAM_fasta_input_node_id, "");
    }

    public String getDisplayName() {
        return "SignalpTask";
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
        addRequiredCommandParameter(sb, "-t", PARAM_type_of_organism);
        addCommandParameter(sb, "-f", PARAM_format);
        addCommandParameter(sb, "-m", PARAM_method);
        addCommandParameter(sb, "-trunc", PARAM_truncate_length);
        if (outputDirectory != null && outputDirectory.trim().length() > 0) {
            sb.append(" -d ").append(outputDirectory);
        }
        return sb.toString();
    }

}
