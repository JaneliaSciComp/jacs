/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
 * All values are stored in the map, and flushed into rela jobTemplate before it is used in submission
 */
public class SerializableJobTemplate implements Serializable {


//    public SerializableJobTemplate(JobTemplate jt) throws IllegalAccessException, InvocationTargetException
//    {
//       copyProperties(this, jt);
//    }

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
                if (val instanceof java.util.List)
                    paramType = java.util.List.class;
                else if (val instanceof java.util.Map)
                    paramType = java.util.Map.class;
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
//        copyProperties(drmaaJobTemplate, this);
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

    private void setValue(Object val) {
        // get calling method
        // first method on the stack is getStackTrace, second setValue, third is what we need
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        if (ste != null && ste.length > 2) {
            String methodName = ste[2].getMethodName();
            // cut 'set' out
            attributeMap.put(methodName.substring(3), val);
        }
        else {
            // error
        }
    }

    private Object getValue() {
        // get calling method
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        if (ste != null && ste.length > 2) {
            String methodName = ste[2].getMethodName();
            // cut 'get' out
            return attributeMap.get(methodName.substring(3));
        }
        else
            return null;
    }

    public void setRemoteCommand(String s) {
        setValue(s);
    }

    public String getRemoteCommand() {
        return (String) getValue();
    }

    public void setArgs(List list) {
        setValue(list);
//        args = list;
    }

    public List getArgs() {
        return (List) getValue();
//        return args;
    }

    public void setJobSubmissionState(int i) {
        setValue(i);
//        jobSubmissionState = i;
    }

    public int getJobSubmissionState() {
        return (Integer) getValue();
//        return jobSubmissionState;
    }

    public void setJobEnvironment(Map map) {
        setValue(map);
//        jobEnvironment = map;
    }

    public Map getJobEnvironment() {
        return (Map) getValue();
//        return jobEnvironment;
    }

    public void setWorkingDirectory(String s) {
        setValue(s);
//        workingDirectory = s;
    }

    public String getWorkingDirectory() {
        return (String) getValue();
//        return workingDirectory;
    }

    public void setJobCategory(String s) {
        setValue(s);
//        jobCategory = s;
    }

    public String getJobCategory() {
        return (String) getValue();
//        return jobCategory;
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
        setValue(realValue);

    }

    public String getNativeSpecification() {
        return (String) getValue();
//        return nativeSpecification;
    }

    public void setEmail(Set set) {
        setValue(set);
//        email = set;
    }

    public Set getEmail() {
        return (Set) getValue();
//        return email;
    }

    public void setBlockEmail(boolean b) {
        setValue(b);
//        blockEmail = b;
    }

    public boolean getBlockEmail() {
        return (Boolean) getValue();
//        return blockEmail;
    }

    public void setStartTime(PartialTimestamp partialTimestamp) {
        setValue(partialTimestamp);
//        startTime = partialTimestamp;
    }

    public PartialTimestamp getStartTime() {
        return (PartialTimestamp) getValue();
//        return startTime;
    }

    public void setJobName(String s) {
        setValue(s);
//        jobName = s;
    }

    public String getJobName() {
        return (String) getValue();
//        return jobName;
    }

    public void setInputPath(String s) {
        setValue(s);
//        inputPath = s;
    }

    public String getInputPath() {
        return (String) getValue();
//        return inputPath;
    }

    public void setOutputPath(String s) {
        setValue(s);
//        outputPath = s;
    }

    public String getOutputPath() {
        return (String) getValue();
//        return outputPath;
    }

    public void setErrorPath(String s) {
        setValue(s);
//        errorPath = s;
    }

    public String getErrorPath() {
        return (String) getValue();
//        return errorPath;
    }

    public void setJoinFiles(boolean b) {
        setValue(b);
//        joinFiles = b;
    }

    public boolean getJoinFiles() {
        return (Boolean) getValue();
//        return joinFiles;
    }

    public void setTransferFiles(FileTransferMode fileTransferMode) {
        setValue(fileTransferMode);
//        transferFiles = fileTransferMode;
    }

    public FileTransferMode getTransferFiles() {
        return (FileTransferMode) getValue();
//        return transferFiles;
    }

    public void setDeadlineTime(PartialTimestamp partialTimestamp) {
        setValue(partialTimestamp);
//        deadlineTime = partialTimestamp;
    }

    public PartialTimestamp getDeadlineTime() {
        return (PartialTimestamp) getValue();
//        return deadlineTime;
    }

    public void setHardWallclockTimeLimit(long l) {
        setValue(l);
//        hardWallclockTimeLimit = l;
    }

    public long getHardWallclockTimeLimit() {
        return (Long) getValue();
//        return hardWallclockTimeLimit;
    }

    public void setSoftWallclockTimeLimit(long l) {
        setValue(l);
//        softWallclockTimeLimit = l;
    }

    public long getSoftWallclockTimeLimit() {
        return (Long) getValue();
//        return softWallclockTimeLimit;
    }

    public void setHardRunDurationLimit(long l) {
        setValue(l);
//        hardRunDurationLimit = l;
    }

    public long getHardRunDurationLimit() {
        return (Long) getValue();
//        return hardRunDurationLimit;
    }

    public void setSoftRunDurationLimit(long l) {
        setValue(l);
//        softRunDurationLimit = l;
    }

    public long getSoftRunDurationLimit() {
        return (Long) getValue();
//        return softRunDurationLimit;
    }

//    private Set attributeNames;
//    // leave it at package access level
//    void setAttributeNames(Set set)
//    {
//        attributeNames = set;
//    }
//    public Set getAttributeNames()  {
//        return attributeNames;
//    }


    static private void copyProperties(Object target, Object src) {
        // list all getters on the source
        Method[] srcMethods = src.getClass().getMethods();
        for (Method srcM : srcMethods) {
            // check if a getter
            if (srcM.getName().startsWith("get")) {
                // figure out the attribute name
                String attrName = srcM.getName().substring(3);
                // copy value
                try {
                    // figure out parameter types
                    Class paramType = srcM.getReturnType();
                    // find setter
                    Method targetM = target.getClass().getMethod("set" + attrName, paramType);

                    // execute copy
                    Object value = srcM.invoke(src);
                    if (value != null)
                        targetM.invoke(target, value);
                }
                catch (Exception e) {
                    //System.err.println("Unable to copy attribute '" + attrName + "'. Error:" + e.getMessage());
                }
            }
        }
    }

}
