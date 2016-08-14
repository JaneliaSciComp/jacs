package org.janelia.it.jacs.model.domain.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;

/**
 * Identifies a particular result within a Sample.
 * 
 * The key is parsed as follows:
 * [objective] [result name]
 * 
 * Where [result name] is parsed as:
 * [result name prefix] - [group name] 
 * 
 * The group name is optional. 
 * 
 * Some example keys:
 * "20x JBA Alignment"
 * "63x Sample Processing Results (Brain)"
 * "63x Post-Processing Result - brain"
 * 
 * One special value is "Latest" which always identifies the latest result in a Sample.
 * 
 * Another is "LSMs", which describes the Sample's LSM files.
 */
public class ResultDescriptor {

    public static final ResultDescriptor LATEST = ResultDescriptor.create();
    public static final ResultDescriptor LATEST_ALIGNED = ResultDescriptor.create().setAligned(true);
    public static final ResultDescriptor LATEST_UNALIGNED = ResultDescriptor.create().setAligned(false);

    private String objective;
    private String resultName;
    private String groupName;
    private Boolean aligned;

    public ResultDescriptor() {
    }

    public ResultDescriptor(String objective, String resultName, String groupName) {
        this.objective = objective;
        this.resultName = resultName;
        this.groupName = groupName;
    }

    public ResultDescriptor(PipelineResult result) {
        this(result.getParentRun().getParent().getObjective(), result.getName(), null);
    }

    public String getObjective() {
        return objective;
    }

    public String getResultName() {
        return resultName;
    }

    public String getGroupName() {
        return groupName;
    }

    public Boolean isAligned() {
        return aligned;
    }

    public static ResultDescriptor create() {
        return new ResultDescriptor();
    }

    public ResultDescriptor setObjective(String objective) {
        this.objective = objective;
        return this;
    }

    public ResultDescriptor setResultName(String resultName) {
        this.resultName = resultName;
        return this;
    }

    public ResultDescriptor setGroupName(String groupName) {
        this.groupName = groupName;
        return this;
    }

    public ResultDescriptor setAligned(Boolean aligned) {
        this.aligned = aligned;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb =  new StringBuilder();
        if (objective!=null) {
            sb.append(objective).append(" ");
        }
        if (resultName!=null) {
            sb.append(resultName).append(" ");
        }
        else {
            sb.append("Latest ");
        }
        if (aligned!=null) {
            String alignment = aligned ? "Aligned" : "Unaligned";
            if (resultName==null) {
                sb.append(alignment).append(" ");
            }
            else {
                sb.append("(").append(alignment).append(") ");
            }
        }
        if (groupName!=null) {
            sb.append("- ").append(groupName).append(" ");
        }
        if (sb.length()>0 && sb.charAt(sb.length()-1)==' ') {
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResultDescriptor that = (ResultDescriptor) o;
        if (objective != null ? !objective.equals(that.objective) : that.objective != null) return false;
        if (resultName != null ? !resultName.equals(that.resultName) : that.resultName != null) return false;
        if (groupName != null ? !groupName.equals(that.groupName) : that.groupName != null) return false;
        return aligned != null ? aligned.equals(that.aligned) : that.aligned == null;
    }

    @Override
    public int hashCode() {
        int result = objective != null ? objective.hashCode() : 0;
        result = 31 * result + (resultName != null ? resultName.hashCode() : 0);
        result = 31 * result + (groupName != null ? groupName.hashCode() : 0);
        result = 31 * result + (aligned != null ? aligned.hashCode() : 0);
        return result;
    }

    public static String serialize(ResultDescriptor resultDescriptor) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(resultDescriptor);
    }

    public static ResultDescriptor deserialize(String json) throws Exception  {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, ResultDescriptor.class);
    }
}