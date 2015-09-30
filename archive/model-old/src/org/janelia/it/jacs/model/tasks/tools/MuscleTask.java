
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Basic usage
 * <p/>
 * muscle -in <inputfile> -out <outputfile>
 * <p/>
 * Common options (for a complete list please see the User Guide):
 * <p/>
 * -in <inputfile>    Input file in FASTA format (default stdin)
 * -out <outputfile>  Output alignment in FASTA format (default stdout)
 * -diags             Find diagonals (faster for similar sequences)
 * -maxiters <n>      Maximum number of iterations (integer, default 16)
 * -maxhours <h>      Maximum time to iterate in hours (default no limit)
 * -maxmb <m>         Maximum memory to allocate in Mb (default 80% of RAM)
 * -html              Write output in HTML format (default FASTA)
 * -msf               Write output in GCG MSF format (default FASTA)
 * -clw               Write output in CLUSTALW format (default FASTA)
 * -clwstrict         As -clw, with 'CLUSTAL W (1.81)' header
 * -log[a] <logfile>  Log to file (append if -loga, overwrite if -log)
 * -quiet             Do not write progress messages to stderr
 * -stable            Output sequences in input order (default is -group)
 * -group             Group sequences by similarity (this is the default)
 * -version           Display version information and exit
 * <p/>
 * Without refinement (very fast, avg accuracy similar to T-Coffee): -maxiters 2
 * Fastest possible (amino acids): -maxiters 1 -diags -sv -distance1 kbit20_3
 * Fastest possible (nucleotides): -maxiters 1 -diags
 */
public class MuscleTask extends Task {
    transient public static final String TASK_NAME = "muscleTask";
    transient public static final String DISPLAY_NAME = "Muscle";

    public static final String ORDER_BY_SIMILARITY = "group (similarity)";
    public static final String ORDER_BY_INPUT = "stable (input order)";

    public static final String OUTPUT_TYPE_FASTA = "fasta";
    public static final String OUTPUT_TYPE_HTML = "html";
    public static final String OUTPUT_TYPE_MSF = "msf";
    public static final String OUTPUT_TYPE_CLW = "clw";
    public static final String OUTPUT_TYPE_CLWSTRICT = "clwstrict";

    // Parameters
    public static final String PARAM_diags = "find diagonals";
    public static final String PARAM_maxIterations = "max iterations";
    public static final String PARAM_maxHours = "max hours";
    public static final String PARAM_outputType = "output type";
    public static final String PARAM_orderBy = "order by";
//    public static final String PARAM_maxMB          = "max MB memory";

    public MuscleTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public MuscleTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        this.taskName = TASK_NAME;
        setParameter(PARAM_maxIterations, "16");
        setParameter(PARAM_orderBy, ORDER_BY_SIMILARITY);
        setParameter(PARAM_outputType, OUTPUT_TYPE_FASTA);
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null) {
            return null;
        }
        String value = getParameter(key);
        if (value == null) {
            return null;
        }
        else if (key.equals(PARAM_diags)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        else if (key.equals(PARAM_maxHours) || key.equals(PARAM_maxIterations)) {
            return new LongParameterVO(1l, 100l, Long.valueOf(value));
        }
        else if (key.equals(PARAM_orderBy)) {
            ArrayList<String> values = new ArrayList<String>();
            values.add(value);
            ArrayList<String> choices = new ArrayList<String>();
            choices.add(ORDER_BY_SIMILARITY);
            choices.add(ORDER_BY_INPUT);
            return new MultiSelectVO(values, choices);
        }
        else if (key.equals(PARAM_outputType)) {
            ArrayList<String> values = new ArrayList<String>();
            values.add(value);
            ArrayList<String> choices = new ArrayList<String>();
            choices.add(OUTPUT_TYPE_CLW);
            choices.add(OUTPUT_TYPE_CLWSTRICT);
            choices.add(OUTPUT_TYPE_FASTA);
            choices.add(OUTPUT_TYPE_HTML);
            choices.add(OUTPUT_TYPE_MSF);
            return new MultiSelectVO(values, choices);
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName);
    }

    public String generateCommandOptions() {
        StringBuffer returnString = new StringBuffer();
        if (parameterDefined(PARAM_diags)) {
            returnString.append("-diags ");
        }
        if (parameterDefined(PARAM_maxIterations)) {
            returnString.append("-maxiters ").append(getParameter(PARAM_maxIterations)).append(" ");
        }
        if (parameterDefined(PARAM_maxHours)) {
            returnString.append("-maxhours ").append(getParameter(PARAM_maxHours)).append(" ");
        }
        if (parameterDefined(PARAM_orderBy)) {
            String orderBy = getParameter(PARAM_orderBy);
            String flag = ORDER_BY_INPUT.equals(orderBy) ? "stable" : "group";
            returnString.append("-").append(flag).append(" ");
        }
        if (parameterDefined(PARAM_outputType) && !OUTPUT_TYPE_FASTA.equals(getParameter(PARAM_outputType))) {
            String outputType = getParameter(PARAM_outputType);
            returnString.append("-").append(outputType).append(" ");
        }
        returnString.append("-version ");
        return returnString.toString();
    }
}