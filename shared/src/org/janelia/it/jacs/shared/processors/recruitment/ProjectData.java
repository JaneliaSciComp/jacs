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

package org.janelia.it.jacs.shared.processors.recruitment;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 9, 2008
 * Time: 12:40:50 AM
 */
public class ProjectData implements Comparable, IsSerializable {

    private String projectName;
    private String projectDescription;

    public ProjectData() {
    }

    public ProjectData(String projectName, String projectDescription) {
        this.projectDescription = projectDescription;
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public int compareTo(Object o) {
        ProjectData pd2 = (ProjectData) o;
        int compared = this.projectDescription.compareTo(pd2.getProjectDescription());
        if (0 != compared) {
            return compared;
        }
        else {
            return this.projectName.compareTo(pd2.getProjectName());
        }
    }
}
