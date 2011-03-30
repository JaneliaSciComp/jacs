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

package org.janelia.it.jacs.model.tasks;
// Generated Aug 17, 2006 3:17:24 PM by Hibernate Tools 3.2.0.beta6a

import com.google.gwt.user.client.rpc.IsSerializable;

public class TaskParameter implements java.io.Serializable, IsSerializable {

    // Fields
    private Task task;
    private String name;
    private String value;

    // Constructors

    /**
     * default constructor
     */
    public TaskParameter() {
    }

    /**
     * full constructor
     */
    public TaskParameter(String name, String value, Task task) {
        this.name = name;
        this.value = value;
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof TaskParameter)) return false;

        TaskParameter that = (TaskParameter) o;

        return !(task != null ? !task.equals(that.task) : that.task != null) &&
               !(name != null ? !name.equals(that.name) : that.name != null);
    }

    public int hashCode() {
        int result;
        result = (task != null && task.getObjectId() != null ? task.getObjectId().hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "TaskParameter{" +
                "task='" + (task != null && task.getObjectId() != null ? task.getObjectId().toString() : "<none>") + '\'' + "," +
                "name='" + name + '\'' + "," +
                "value='" + value +
                '}';
    }

}
