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

package org.janelia.it.jacs.compute.engine.data;

import org.janelia.it.jacs.compute.engine.def.ActionDef;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This interface is implemented by ProcessData and QueueMessage
 *
 * @author Tareq Nabeel
 */
public interface IProcessData {

    public List getProcessIds();

    public void setProcessIds(List<Long> ids);

    public Long getProcessId() throws MissingDataException;

    public void setProcessId(Long id);

    public Object getMandatoryItem(String key) throws MissingDataException;

    public Object getItem(String key);

    public void putItem(String key, Object value);

    public void removeItem(String key);

    public Long getLong(String property);

    public Integer getInt(String property);

    public Float getFloat(String property);

    public Double getDouble(String property);

    public String getString(String property);

    public Boolean getBoolean(String property);

    public ProcessDef getProcessDef() throws MissingDataException;

    public void setProcessDef(ProcessDef processDef);

    public ActionDef getProcessedAction() throws MissingDataException;

    public void setProcessedAction(ActionDef actionDef);

    public ActionDef getActionToProcess() throws MissingDataException;

    public void setActionToProcess(ActionDef actionDef);

    public Set<Map.Entry<String, Object>> entrySet();

    public static final String PROCESS_ID = "PROCESS_ID";
    public static final String TASK = "TASK";
    public static final String USER_NAME = "USER_NAME";
    public static final String JOB_NAME = "JOB_NAME";
    public static final String PROCESSED_SUCCESSFULLY = "PROCESSED_SUCCESSFULLY";
    public static final String ACTION_TO_PROCESS = "ACTION_TO_PROCESS";   // FOR JMS QUEUE
    public static final String PROCESSED_ACTION = "PROCESSED_ACTION";   //JMS REPLY
    public static final String PROCESS_DEFINITION = "PROCESS_DEFINITION";
    public static final String ORIGINAL_MESSAGE_ID = "ORIGINAL_MESSAGE_ID";
    public static final String MESSAGE_ID = "MESSAGE_ID";
    public static final String PROCESSING_EXCEPTION = "PROCESSING_EXCEPTION";


}
