
package org.janelia.it.jacs.compute.drmaa;

import org.ggf.drmaa.FileTransferMode;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.PartialTimestamp;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: May 4, 2009
 * Time: 2:53:19 PM
 * This class is a facade for DRMAA JobTemplate. It is needed for serialization purposes
 * All values are stored in the map, and flushed into related jobTemplate before it is used in submission.
 * NOTE: Many methods are not supported by SGE but are reflected in the API; for example setHardRunDurationLimit()
 */
public class SerializableJobTemplate implements Serializable {


    private Map<String, Object> attributeMap = new HashMap<String, Object>();

    transient private JobTemplate drmaaJobTemplate;

    /**
     * this method should remain on package access level!!!
     *
     * @return
     */
    JobTemplate getDrmaaJobTemplate() {
        return drmaaJobTemplate;
    }

    /**
     * this method should remain on package access level!!!
     *
     * @return
     */
    void setDrmaaJobTemplate(JobTemplate drmaaJobTemplate) {
        this.drmaaJobTemplate = drmaaJobTemplate;
    }

    /**
     * this method is left on 'package' level access for a purpose. DO NOT change it! -LK
     */
    void flush() {
        // only set properties that were changed!
        for (String key : attributeMap.keySet()) {
            String methodName = "set" + key;
            Object val = attributeMap.get(key);
            if (val != null) {
                // figure out parameter types
                Class paramType;
                // 2 method accept java collections as parameters
                // but getMethod() requires specific type name, not a subclass
                // so for Map and List parameter types, we need to provide our own types
                if (val instanceof List)
                    paramType = List.class;
                else if (val instanceof Map)
                    paramType = Map.class;
                else
                    paramType = val.getClass();

                try {
                    // find setter
                    Method targetM = drmaaJobTemplate.getClass().getMethod(methodName, paramType);
                    // execute copy
                    targetM.invoke(drmaaJobTemplate, val);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    // method not found?
                }
            }
        }
    }

    public String toString() {
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            StringBuffer sb = new StringBuffer();
            sb.append(this.getClass().getSimpleName()).append(" object:\n");
            for (Field f : fields) {
                sb.append(f.getName()).append(" = ").append(f.get(this)).append("\n");
            }
            return sb.toString();
        }
        catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private void setValue(String sgeMethodName, Object val) {
        attributeMap.put(sgeMethodName, val);
    }

    private Object getValue(String targetKey) {
        return attributeMap.get(targetKey);
    }

    public void setRemoteCommand(String s) {
        setValue("RemoteCommand", s);
    }

    public String getRemoteCommand() {
        return (String) getValue("RemoteCommand");
    }

    public void setArgs(List list) {
        setValue("Args", list);
    }

    public List getArgs() {
        return (List) getValue("Args");
    }

    public void setJobSubmissionState(int i) {
        setValue("JobSubmissionState", i);
    }

    public int getJobSubmissionState() {
        return (Integer) getValue("JobSubmissionState");
    }

    public void setJobEnvironment(Map map) {
        setValue("JobEnvironment", map);
    }

    public Map getJobEnvironment() {
        return (Map) getValue("JobEnvironment");
    }

    public void setWorkingDirectory(String s) {
        setValue("WorkingDirectory", s);
    }

    public String getWorkingDirectory() {
        return (String) getValue("WorkingDirectory");
    }

    public void setJobCategory(String s) {
        setValue("JobCategory", s);
    }

    public String getJobCategory() {
        return (String) getValue("JobCategory");
    }

    public void setNativeSpecification(String s) {
        if (s == null)
            return;
        else {
            // clean up
            s = s.trim();
            s = s.replaceAll("\\s+", " ");
        }
        // this hack is required for SGE. SGE does not recognize
        // explicit requests for default queue!
        if (s.equals("-l default"))
            return; // DO NOT SET EXPLICIT DEFAULT QUEUE

        String realValue;
        if (attributeMap.containsKey("NativeSpecification"))
            realValue = attributeMap.get("NativeSpecification") + " " + s;
        else
            realValue = s;
        setValue("NativeSpecification", realValue);

    }

    public String getNativeSpecification() {
        return (String) getValue("NativeSpecification");
    }

    public void setEmail(Set set) {
        setValue("Email", set);
    }

    public Set getEmail() {
        return (Set) getValue("Email");
    }

    public void setBlockEmail(boolean b) {
        setValue("BlockEmail", b);
    }

    public boolean getBlockEmail() {
        return (Boolean) getValue("BlockEmail");
    }

    public void setStartTime(PartialTimestamp partialTimestamp) {
        setValue("StartTime", partialTimestamp);
    }

    public PartialTimestamp getStartTime() {
        return (PartialTimestamp) getValue("StartTime");
    }

    public void setJobName(String s) {
        setValue("JobName", s);
    }

    public String getJobName() {
        return (String) getValue("JobName");
    }

    public void setInputPath(String s) {
        setValue("InputPath", s);
    }

    public String getInputPath() {
        return (String) getValue("InputPath");
    }

    public void setOutputPath(String s) {
        setValue("OutputPath", s);
    }

    public String getOutputPath() {
        return (String) getValue("OutputPath");
    }

    public void setErrorPath(String s) {
        setValue("ErrorPath", s);
    }

    public String getErrorPath() {
        return (String) getValue("ErrorPath");
    }

    public void setJoinFiles(boolean b) {
        setValue("JoinFiles", b);
    }

    public boolean getJoinFiles() {
        return (Boolean) getValue("JoinFiles");
    }

    public void setTransferFiles(FileTransferMode fileTransferMode) {
        setValue("TransferFiles", fileTransferMode);
    }

    public FileTransferMode getTransferFiles() {
        return (FileTransferMode) getValue("TransferFiles");
    }

    public void setDeadlineTime(PartialTimestamp partialTimestamp) {
        setValue("DeadlineTime", partialTimestamp);
    }

    public PartialTimestamp getDeadlineTime() {
        return (PartialTimestamp) getValue("DeadlineTime");
    }

    public void setHardWallclockTimeLimit(long l) {
        setValue("HardWallclockTimeLimit", l);
    }

    public long getHardWallclockTimeLimit() {
        return (Long) getValue("HardWallclockTimeLimit");
    }

    public void setSoftWallclockTimeLimit(long l) {
        setValue("SoftWallclockTimeLimit", l);
    }

    public long getSoftWallclockTimeLimit() {
        return (Long) getValue("SoftWallclockTimeLimit");
    }

    public void setHardRunDurationLimit(long l) {
        setValue("HardRunDurationLimit", l);
    }

    public long getHardRunDurationLimit() {
        return (Long) getValue("HardRunDurationLimit");
    }

    public void setSoftRunDurationLimit(long l) {
        setValue("SoftRunDurationLimit", l);
    }

    public long getSoftRunDurationLimit() {
        return (Long) getValue("SoftRunDurationLimit");
    }

}
