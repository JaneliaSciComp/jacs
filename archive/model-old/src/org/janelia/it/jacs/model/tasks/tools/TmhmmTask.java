
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: April 15, 2010
 * Time: 12:16:04 PM
 */
public class TmhmmTask extends Task {

    /*

    The options needed by tmhmm aren't presented in any documentation I have been able to find.
    The following are the options presented for the tmhmm perl script, used as a wrapper around
    the binary executable 'decodeanhmm', the heart and guts of the tmhmm service.

        # $opt_basedir: basis directory for TMHMM package
        # $opt_scrdir: Script directory (defaults basedir/bin)
        # $opt_bindir: Bin directory (defaults basedir/bin)
        # $opt_libdir: Library directory (defaults basedir/lib)

        $opt_d = 0;          # DEBUGGING
        $opt_workdir = ".";  # Working dir.
        $opt_wwwdir = ".";   # The place where the www server looks for files
                             # (The www name for the working dir)
        $opt_serverhome = ".";
        $opt_html = 0;       # Produce HTML output
        $opt_short = 0;      # Short output format
        $opt_plot = 0;       # Produce graphics
        $opt_v1 = 0;         # Use old model (version 1)

    Even though those first four opt_ variables aren't actually declared, nothing prevents you from running
    the tmhmm command and specifying --basedir, --srcdir, etc... the author(s) use the variables in the code
    elsewhere and since they're not using the strict pragma (or even the warnings pragma, for that matter)
    the values are taken in and used anyway by GetOpts::Long.

    Also, note that none of these really matter for our use case.

    The tmhmm command runs something like this:

        % tmhmm small_Trna_test.fsa
        /usr/local/bin/tmhmmformat.pl -workdir ./1028 -wwwdir . -serverhome . -noplot
        /usr/local/bin/decodeanhmm /usr/local/db/tmhmm/TMHMM2.0.model -f /usr/local/db/tmhmm/TMHMM2.0.options -plp
        # F75LRFB02J1S7F Length: 136
        # F75LRFB02J1S7F Number of predicted TMHs:  0
        # F75LRFB02J1S7F Exp number of AAs in TMHs: 1.26029
        # F75LRFB02J1S7F Exp number, first 60 AAs:  0.98782
        # F75LRFB02J1S7F Total prob of N-in:        0.20072
        F75LRFB02J1S7F  TMHMM2.0        outside      1   136

    */

    transient public static final String PARAM_fasta_input_node_id = "input node id";
    transient public static final String PARAM_html = "html";
    transient public static final String PARAM_short = "short";
    transient public static final String PARAM_plot = "plot";
    transient public static final String PARAM_v1 = "v1";

    transient public static final String html_DEFAULT = "0";
    transient public static final String short_DEFAULT = "0";
    transient public static final String plot_DEFAULT = "0";
    transient public static final String v1_DEFAULT = "0";

    public TmhmmTask() {
        super();
        setTaskName("TmhmmTask");
        setParameter(PARAM_fasta_input_node_id, "");
        setParameter(PARAM_html, html_DEFAULT);
        setParameter(PARAM_short, short_DEFAULT);
        setParameter(PARAM_plot, plot_DEFAULT);
        setParameter(PARAM_v1, v1_DEFAULT);
    }

    public String getDisplayName() {
        return "TmhmmTask";
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

        if (getParameter(PARAM_html).equals("1")) {
            sb.append(" --html");
        }
        if (getParameter(PARAM_short).equals("1")) {
            sb.append(" --short");
        }
        if (getParameter(PARAM_plot).equals("1")) {
            sb.append(" --plot");
        }
        if (getParameter(PARAM_v1).equals("1")) {
            sb.append(" --v1");
        }

        return sb.toString();
    }

}