
package org.janelia.it.jacs.model.tasks.hmmer3;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.DoubleParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

public class HMMER3Task extends Task {

    public static final transient String TASK_NAME = "hmmer3Task";
    public static final transient String DISPLAY_NAME = "HMMER3";
    public static final transient String FASTA_FORMAT = "fasta";
    public static final transient String DEFAULT_TBIT_THRESH = "-1000000.0";
    public static final transient String DEFAULT_ZNUM = "59021";
    public static final transient String DEFAULT_DOM_E = "1000000000.0";
    public static final transient String DEFAULT_OUTPUT_WIDTH = "120";
    public static final transient String PARAM_project = "project";
    public static final transient String PARAM_query_node_id = "Query node id";
    public static final transient String PARAM_db_node_id = "database node id";
    public static final transient String PARAM_evalue_cutoff = "Evalue cutoff";
    public static final transient String PARAM_tbit_threshold = "Tbit threshold";
    public static final transient String PARAM_inc_evalue_cutoff = "inclusion Evalue cutoff";
    public static final transient String PARAM_inc_tbit_threshold = "inclusion Tbit threshold";
    public static final transient String PARAM_acc = "use HMM accessions in output";
    public static final transient String PARAM_cut_ga = "use profile's GA gathering threshold cutoffs";
    public static final transient String PARAM_cut_nc = "use profile's NC noise threshold cutoffs";
    public static final transient String PARAM_cut_tc = "use profile's TC trusted threshold cutoffs";
    public static final transient String PARAM_domE = "domain Eval cutoff 2nd threshold";
    public static final transient String PARAM_domT = "domain Tbit threshold 2nd threshold";
    public static final transient String PARAM_inc_domE = "domain inclusion Eval cutoff 2nd threshold";
    public static final transient String PARAM_inc_domT = "domain inclusion Tbit threshold 2nd threshold";
    public static final transient String PARAM_skip_alignment_output = "skip alignment output";
    public static final transient String PARAM_unlimit_text_width = "unlimit text file width";
    public static final transient String PARAM_max_ascii_output_width = "max width of text lines";
    public static final transient String PARAM_disable_heuristic_filters = "disable heurestic filters";
    public static final transient String PARAM_disable_bias_filter = "disable composition bias filter";
    public static final transient String PARAM_disable_score_corrections = "disable biased composition score corrections";
    public static final transient String PARAM_msv_threshold = "MSV threshold";
    public static final transient String PARAM_vit_threshold = "Vit threshold (1E)";
    public static final transient String PARAM_fwd_threshold = "Fwd threshold (1E)";

    public HMMER3Task() {
        setParameter("Query node id", "");
        setParameter("database node id", "");
//        setParameter("Evalue cutoff", Double.toString(10D));
//        setParameter("Tbit threshold", (new Double("-1000000.0")).toString());
//        setParameter("inclusion Evalue cutoff", null);
//        setParameter("inclusion Tbit threshold", null);
//        setParameter("use HMM accessions in output", Boolean.FALSE.toString());
//        setParameter("use profile's GA gathering threshold cutoffs", Boolean.FALSE.toString());
//        setParameter("use profile's NC noise threshold cutoffs", Boolean.TRUE.toString());
//        setParameter("use profile's TC trusted threshold cutoffs", Boolean.FALSE.toString());
//        setParameter("domain Eval cutoff 2nd threshold", (new Double("1000000000.0")).toString());
//        setParameter("domain Tbit threshold 2nd threshold", (new Double("-1000000.0")).toString());
//        setParameter("domain inclusion Eval cutoff 2nd threshold", null);
//        setParameter("domain inclusion Tbit threshold 2nd threshold", null);
//        setParameter("save per-sequence hits to file", Boolean.FALSE.toString());
//        setParameter("save per-domain hits to file", Boolean.FALSE.toString());
//        setParameter("skip alignment output", Boolean.FALSE.toString());
//        setParameter("unlimit text file width", Boolean.FALSE.toString());
//        setParameter("max width of text lines", Double.toString(120D));
//        setParameter("disable heurestic filters", Boolean.FALSE.toString());
//        setParameter("disable composition bias filter", Boolean.FALSE.toString());
//        setParameter("disable biased composition score corrections", Boolean.FALSE.toString());
//        setParameter("MSV threshold", Double.valueOf(0.02D).toString());
//        setParameter("Vit threshold (1E)", Integer.valueOf(-3).toString());
//        setParameter("Fwd threshold (1E)", Integer.valueOf(-5).toString());
        setTaskName("HMMER3Task");
    }

    public HMMER3Task(Long queryNodeId, Long libraryId) {
        setParameter("Query node id", queryNodeId.toString());
        setParameter("database node id", libraryId.toString());
    }

    public String getDisplayName() {
        return "HMMER3 Task";
    }

    public ParameterVO getParameterVO(String key)
            throws ParameterException {
        if (key == null) {
            return null;
        }
        String value = getParameter(key);
        if (value == null || "".equals(value)) {
            return null;
        }
        if (key.equals("Evalue cutoff")) {
            return new DoubleParameterVO(Double.valueOf(0.0D), Double.valueOf(1000000D), new Double(value));
        }
        if (key.equals("Tbit threshold")) {
            return new DoubleParameterVO(new Double("-1000000.0"), Double.valueOf(100000D), new Double(value));
        }
        if (key.equals("inclusion Evalue cutoff")) {
            return new DoubleParameterVO(Double.valueOf(0.0D), Double.valueOf(1000000D), new Double(value));
        }
        if (key.equals("inclusion Tbit threshold")) {
            return new DoubleParameterVO(new Double("-1000000.0"), Double.valueOf(100000D), new Double(value));
        }
        if (key.equals("domain Eval cutoff 2nd threshold")) {
            return new DoubleParameterVO(Double.valueOf(0.0D), new Double("1000000000.0"), new Double(value));
        }
        if (key.equals("domain Tbit threshold 2nd threshold")) {
            return new DoubleParameterVO(new Double("-1000000.0"), Double.valueOf(1000000D), new Double(value));
        }
        if (key.equals("domain inclusion Eval cutoff 2nd threshold")) {
            return new DoubleParameterVO(Double.valueOf(0.0D), new Double("1000000000.0"), new Double(value));
        }
        if (key.equals("domain inclusion Tbit threshold 2nd threshold")) {
            return new DoubleParameterVO(new Double("-1000000.0"), Double.valueOf(1000000D), new Double(value));
        }
        if (key.equals("MSV threshold")) {
            return new DoubleParameterVO(Double.valueOf(0.0D), Double.valueOf(1.0D), new Double(value));
        }
        if (key.equals("Vit threshold (1E)")) {
            return new DoubleParameterVO(Double.valueOf(-10D), Double.valueOf(0.0D), new Double(value));
        }
        if (key.equals("Fwd threshold (1E)")) {
            return new DoubleParameterVO(Double.valueOf(-10D), Double.valueOf(0.0D), new Double(value));
        }
        if (key.equals("use HMM accessions in output") || key.equals("use profile's GA gathering threshold cutoffs") ||
                key.equals("use profile's NC noise threshold cutoffs") || key.equals("use profile's TC trusted threshold cutoffs") || key.equals("save per-sequence hits to file") || key.equals("save per-domain hits to file") || key.equals("skip alignment output") || key.equals("unlimit text file width") || key.equals("disable heurestic filters") || key.equals("disable composition bias filter") || key.equals("disable biased composition score corrections")) {
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        }
        if (key.equals("max width of text lines")) {
            return new DoubleParameterVO(Double.valueOf(0.0D), Double.valueOf(2000D), new Double(value));
        }
        return null;
    }

    public String generateCommandLineOptionString()
            throws ParameterException {
        StringBuffer sb = new StringBuffer();
        for (String key : getParameterKeySet()) {
            // If the value is null, skip
            if (null==getParameter(key)) { continue; }
            if (key.equals(PARAM_evalue_cutoff)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (dvo.getActualValue().doubleValue() != 10D) {sb.append("-E ").append(dvo.getActualValue()).append(" ");}
            }
            else if (key.equals(PARAM_tbit_threshold)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (dvo.getActualValue().doubleValue() != Double.parseDouble("-1000000.0")) {
                    sb.append("-T ").append(dvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_inc_evalue_cutoff)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (null != dvo && dvo.getActualValue().doubleValue() != 10D) {sb.append("--incE ").append(dvo.getActualValue()).append(" ");}
            }
            else if (key.equals(PARAM_inc_tbit_threshold)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (null != dvo && dvo.getActualValue().doubleValue() != Double.parseDouble("-1000000.0")){
                    sb.append("--incT ").append(dvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_acc)) {
                Boolean value = Boolean.valueOf(((BooleanParameterVO) getParameterVO(key)).getBooleanValue());
                if (value.booleanValue()) {sb.append("--acc ");}
            }
            else if (key.equals(PARAM_cut_ga)) {
                Boolean value = Boolean.valueOf(((BooleanParameterVO) getParameterVO(key)).getBooleanValue());
                if (value.booleanValue()) {sb.append("--cut_ga ");}
            }
            else if (key.equals(PARAM_cut_nc)) {
                Boolean value = Boolean.valueOf(((BooleanParameterVO) getParameterVO(key)).getBooleanValue());
                if (value.booleanValue()) {sb.append("--cut_nc ");}
            }
            else if (key.equals(PARAM_cut_tc)) {
                Boolean value = Boolean.valueOf(((BooleanParameterVO) getParameterVO(key)).getBooleanValue());
                if (value.booleanValue()) {sb.append("--cut_tc ");}
            }
            else if (key.equals(PARAM_domE)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (dvo.getActualValue().doubleValue() != Double.parseDouble("1000000000.0")){
                    sb.append("--domE ").append(dvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_domT)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (dvo.getActualValue().doubleValue() != Double.parseDouble("-1000000.0")){
                    sb.append("--domT ").append(dvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_inc_domE)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (null != dvo && dvo.getActualValue().doubleValue() != Double.parseDouble("1000000000.0")){
                    sb.append("--incdomE ").append(dvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_inc_domT)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (null != dvo && dvo.getActualValue().doubleValue() != Double.parseDouble("-1000000.0")){
                    sb.append("--incdomT ").append(dvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_skip_alignment_output)) {
                Boolean value = Boolean.valueOf(((BooleanParameterVO) getParameterVO(key)).getBooleanValue());
                if (value.booleanValue()) {sb.append("--noali ");}
            }
            else if (key.equals(PARAM_unlimit_text_width)) {
                Boolean value = Boolean.valueOf(((BooleanParameterVO) getParameterVO(key)).getBooleanValue());
                if (value.booleanValue()) {sb.append("--notextw ");}
            }
            else if (key.equals(PARAM_max_ascii_output_width)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (dvo.getActualValue().doubleValue() != Double.parseDouble("120")){
                    sb.append("--textw ").append(dvo.getActualValue()).append(" ");
                }
            }
            else if (key.equals(PARAM_disable_heuristic_filters)) {
                Boolean value = Boolean.valueOf(((BooleanParameterVO) getParameterVO(key)).getBooleanValue());
                if (value.booleanValue()) {sb.append("--max ");}
            }
            else if (key.equals(PARAM_disable_bias_filter)) {
                Boolean value = Boolean.valueOf(((BooleanParameterVO) getParameterVO(key)).getBooleanValue());
                if (value.booleanValue()) {sb.append("--nobias ");}
            }
            else if (key.equals(PARAM_disable_score_corrections)) {
                Boolean value = Boolean.valueOf(((BooleanParameterVO) getParameterVO(key)).getBooleanValue());
                if (value.booleanValue()) {sb.append("--nonull2 ");}
            }
            else if (key.equals(PARAM_msv_threshold)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (null != dvo) {sb.append("--F1 ").append(dvo.getActualValue()).append(" ");}
            }
            else if (key.equals(PARAM_vit_threshold)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (null != dvo) {sb.append("--F2 1e").append(dvo.getActualValue()).append(" ");}
            }
            else if (key.equals(PARAM_fwd_threshold)) {
                DoubleParameterVO dvo = (DoubleParameterVO) getParameterVO(key);
                if (null != dvo) {sb.append("--F3 1e").append(dvo.getActualValue()).append(" ");}
            }
        }
        return sb.toString();
    }

}
