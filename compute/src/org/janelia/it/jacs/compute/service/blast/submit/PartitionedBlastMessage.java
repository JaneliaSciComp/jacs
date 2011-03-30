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

package org.janelia.it.jacs.compute.service.blast.submit;

import org.janelia.it.jacs.compute.jtc.BaseMessage;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import java.io.File;
import java.io.Serializable;

/**
 * @author Sean Murphy
 */
public class PartitionedBlastMessage extends BaseMessage implements Serializable {
    private static final String SUBJECT_DATABASE = "SubjectDatabase";
    private static final String PARTITION = "Partition";
    private static final String SIZE = "Size";
    private static final String TASK_ID = "TaskId";
    private static final String RESULT_ID = "ResultId";

    public static final String STATUS_OK = "STATUS_OK";
    public static final String STATUS_ERROR = "STATUS_ERROR";

    public PartitionedBlastMessage(ObjectMessage msg) throws
            JMSException {
        super(msg, false);
    }

    /**
     * Use this constructor to construct a new message
     *
     * @param msg
     */
    public PartitionedBlastMessage(ObjectMessage msg,
                                   Queue replyQueue,
                                   File subjectDatabase,
                                   Long resultId,
                                   String partition,
                                   Long taskId,
                                   Long size) throws Exception {
        super(msg, true);
        msg.setJMSReplyTo(replyQueue);
        setObjectProperty(SUBJECT_DATABASE, subjectDatabase);
        setObjectProperty(PARTITION, partition);
        setObjectProperty(TASK_ID, taskId);
        setObjectProperty(RESULT_ID, resultId);
        setObjectProperty(SIZE, size);
        finishedObjectMsgConstruction();
    }

    public boolean isValid() {
        try {
            return (super.isValid() &&
                    getMessage().getJMSReplyTo() != null &&
                    getSubjectDatabase() != null &&
                    getPartition() != null &&
                    getTaskId() != null &&
                    getResultId() != null &&
                    getSize() != null);
        }
        catch (JMSException ex) {
            return false;
        }
    }

    public Destination getJMSReplyTo() {
        try {
            return getMessage().getJMSReplyTo();
        }
        catch (Exception ex) {
            return null;
        }
    }

    public File getSubjectDatabase() {
        Object name = getObjectProperty(SUBJECT_DATABASE);
        if (name instanceof File) return (File) name;
        return null;
    }

    public String getPartition() {
        Object name = getObjectProperty(PARTITION);
        if (name instanceof String) return (String) name;
        return null;
    }

    public Long getTaskId() {
        Object name = getObjectProperty(TASK_ID);
        if (name instanceof Long) return (Long) name;
        return null;
    }

    public Long getResultId() {
        Object name = getObjectProperty(RESULT_ID);
        if (name instanceof Long) return (Long) name;
        return null;
    }

    public Long getSize() {
        Object name = getObjectProperty(SIZE);
        if (name instanceof Long) return (Long) name;
        return null;
    }

}
