
package org.janelia.it.jacs.model.tasks.hmmer;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 22, 2008
 * Time: 2:33:59 PM
 */
public class HmmpfamTask extends Task {
    transient public static final String FASTA_FORMAT = "fasta";
    transient public static final String DEFAULT_TBIT_THRESH = "-1000000.0"; // approx of very negative number
    transient public static final String DEFAULT_ZNUM = "59021"; // believe it or not, this is the default
    transient public static final String DEFAULT_DOM_E = "1000000000.0";

    transient public static final String PARAM_project = "project";
    transient public static final String PARAM_query_node_id = "Query node id";
    transient public static final String PARAM_pfam_db_node_id = "Pfam database node id";

    transient public static final String PARAM_use_nucleic_model = "Use nucleic model";
    transient public static final String PARAM_max_best_domain_aligns = "Max best domain aligns";
    transient public static final String PARAM_evalue_cutoff = "Evalue cutoff";
    transient public static final String PARAM_tbit_threshold = "Tbit threshold";
    transient public static final String PARAM_z_model_number = "Z model number";

    transient public static final String PARAM_acc = "use HMM accessions in output";
    transient public static final String PARAM_compat = "use legacy output style";
    transient public static final String PARAM_cut_ga = "use Pfam GA gathering threshold cutoffs";
    transient public static final String PARAM_cut_nc = "use Pfam NC noise threshold cutoffs";
    transient public static final String PARAM_cut_tc = "use Pfam TC trusted threshold cutoffs";
    transient public static final String PARAM_domE = "domain Eval cutoff 2nd threshold";
    transient public static final String PARAM_domT = "domain Tbit threshold 2nd threshold";
    transient public static final String PARAM_forward = "use full Forward algorithm";
    transient public static final String PARAM_null2 = "turn off post hoc 2nd null model";
    transient public static final String PARAM_xnu = "turn on xnu query protein sequence filter";

    public HmmpfamTask() {
        super();
        setParameter(PARAM_query_node_id, "");
        setParameter(PARAM_pfam_db_node_id, "");
        setParameter(PARAM_use_nucleic_model, Boolean.FALSE.toString());
        setParameter(PARAM_max_best_domain_aligns, Long.toString(0L)); // no limit
        setParameter(PARAM_evalue_cutoff, Double.toString(10.0));
        setParameter(PARAM_tbit_threshold, new Double(DEFAULT_TBIT_THRESH).toString()); // no threshold
        setParameter(PARAM_z_model_number, new Long(DEFAULT_ZNUM).toString()); // number of z models
        setParameter(PARAM_acc, Boolean.FALSE.toString());
        setParameter(PARAM_compat, Boolean.FALSE.toString());
        setParameter(PARAM_cut_ga, Boolean.FALSE.toString());
        setParameter(PARAM_cut_nc, Boolean.FALSE.toString());
        setParameter(PARAM_cut_tc, Boolean.FALSE.toString());
        setParameter(PARAM_domE, new Double(DEFAULT_DOM_E).toString()); // not applied
        setParameter(PARAM_domT, new Double(DEFAULT_TBIT_THRESH).toString()); // not applied
        setParameter(PARAM_forward, Boolean.FALSE.toString());
        setParameter(PARAM_null2, Boolean.FALSE.toString());
        setParameter(PARAM_xnu, Boolean.FALSE.toString());
        setTaskName("HmmpfamTask");
    }

    public HmmpfamTask(Long queryNodeId, Long pfamNodeId) {
        setParameter(PARAM_query_node_id, queryNodeId.toString());
        setParameter(PARAM_pfam_db_node_id, pfamNodeId.toString());
    }

    public String getDisplayName() {
        return "HMM Pfam Task";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_use_nucleic_model))
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        if (key.equals(PARAM_max_best_domain_aligns))
            return new LongParameterVO((long) 0, (long) 100000, new Long(value));
        if (key.equals(PARAM_evalue_cutoff))
            return new DoubleParameterVO((Double) 0.0, (Double) 1000000.0, new Double(value));
        if (key.equals(PARAM_tbit_threshold))
            return new DoubleParameterVO(new Double(DEFAULT_TBIT_THRESH), (Double) 100000.0, new Double(value));
        if (key.equals(PARAM_z_model_number))
            return new LongParameterVO((long) 0, (long) 1000000000, new Long(value));
        if (key.equals(PARAM_acc))
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        if (key.equals(PARAM_compat))
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        if (key.equals(PARAM_cut_ga))
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        if (key.equals(PARAM_cut_nc))
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        if (key.equals(PARAM_cut_tc))
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        if (key.equals(PARAM_domE))
            return new DoubleParameterVO((Double) 0.0, new Double(DEFAULT_DOM_E), new Double(value));
        if (key.equals(PARAM_domT))
            return new DoubleParameterVO(new Double(DEFAULT_TBIT_THRESH), (Double) 1000000.0, new Double(value));
        if (key.equals(PARAM_forward))
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        if (key.equals(PARAM_null2))
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        if (key.equals(PARAM_xnu))
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        // no match
        return null;
    }

    // Note: in this method, we only add arguments to the command line when the value differs from
    // default. Ie, if a boolean option is false in the task, and the program option switches it to
    // true (ie, the default is false), then we do not include this option on the command line.
    public String generateCommandLineOptionString() throws ParameterException {
        StringBuffer sb = new StringBuffer();
        for (String key : getParameterKeySet()) {
            if (key.equals(PARAM_use_nucleic_model)) {
                Boolean value = ((BooleanParameterVO) getParameterVO(key)).getBooleanValue();
                if (value)
                    sb.append("-n ");
            }
            else if (key.equals(PARAM_max_best_domain_aligns)) {
                LongParameterVO pvo = (LongParameterVO) getParameterVO(key);
                if (pvo.getActualValue() != 0L) {
                    sb.append("-A ").append(pvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_evalue_cutoff)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (dvo.getActualValue() != 10.0) {
                    sb.append("-E ").append(dvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_tbit_threshold)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (dvo.getActualValue() != Double.parseDouble(DEFAULT_TBIT_THRESH)) {
                    sb.append("-T ").append(dvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_z_model_number)) {
                LongParameterVO pvo = (LongParameterVO) getParameterVO(key);
                long actualValue = pvo.getActualValue();
                if (actualValue != 0 && actualValue != Long.parseLong(DEFAULT_ZNUM)) {
                    sb.append("-Z ").append(pvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_acc)) {
                Boolean value = ((BooleanParameterVO) getParameterVO(key)).getBooleanValue();
                if (value)
                    sb.append("--acc ");
            }
            else if (key.equals(PARAM_compat)) {
                Boolean value = ((BooleanParameterVO) getParameterVO(key)).getBooleanValue();
                if (value)
                    sb.append("--compat ");
            }
            else if (key.equals(PARAM_cut_ga)) {
                Boolean value = ((BooleanParameterVO) getParameterVO(key)).getBooleanValue();
                if (value)
                    sb.append("--cut_ga ");
            }
            else if (key.equals(PARAM_cut_nc)) {
                Boolean value = ((BooleanParameterVO) getParameterVO(key)).getBooleanValue();
                if (value)
                    sb.append("--cut_nc ");
            }
            else if (key.equals(PARAM_cut_tc)) {
                Boolean value = ((BooleanParameterVO) getParameterVO(key)).getBooleanValue();
                if (value)
                    sb.append("--cut_tc ");
            }
            else if (key.equals(PARAM_domE)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (dvo.getActualValue() != Double.parseDouble(DEFAULT_DOM_E)) {
                    sb.append("--domE ").append(dvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_domT)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (dvo.getActualValue() != Double.parseDouble(DEFAULT_TBIT_THRESH)) {
                    sb.append("--domT ").append(dvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_forward)) {
                Boolean value = ((BooleanParameterVO) getParameterVO(key)).getBooleanValue();
                if (value)
                    sb.append("--forward ");
            }
            else if (key.equals(PARAM_null2)) {
                Boolean value = ((BooleanParameterVO) getParameterVO(key)).getBooleanValue();
                if (value)
                    sb.append("--null2 ");
            }
            else if (key.equals(PARAM_xnu)) {
                Boolean value = ((BooleanParameterVO) getParameterVO(key)).getBooleanValue();
                if (value)
                    sb.append("--xnu ");
            }
            else {
                // ignore
            }
        }
        return sb.toString();
    }

}
