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

package org.janelia.it.jacs.compute.service.common.grid.submit;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Jun 2, 2009
 * Time: 5:50:06 PM
 */
public class GridProcessResult implements Serializable {
    private String gridSubmissionKey;
    private long taskId;
    private boolean completed;
    private String error;

    public GridProcessResult() {
    }

    public GridProcessResult(long actualTaskId, boolean jobCompleted) {
        taskId = actualTaskId;
        completed = jobCompleted;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean result) {
        this.completed = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getGridSubmissionKey() {
        return gridSubmissionKey;
    }

    public void setGridSubmissionKey(String gridSubmissionKey) {
        this.gridSubmissionKey = gridSubmissionKey;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Task ID=").append(taskId).append("; ");
        sb.append("Result=").append((completed ? "completed" : "failed")).append("; ");
        sb.append("Error=").append(error).append("; ");
        return sb.toString();
    }
}
