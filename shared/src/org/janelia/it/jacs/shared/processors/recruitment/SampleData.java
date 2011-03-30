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
 * Date: Sep 11, 2007
 * Time: 8:53:34 AM
 */
public class SampleData implements Comparable, IsSerializable {
    private Integer orderNumber;
    private String name;
    private String description;
    private String projectAccession;
    private String projectName;

    public SampleData() {
    }

    public SampleData(String name, String description, Integer orderNumber, String projectAccession, String projectName) {
        this.name = name;
        this.description = description;
        this.orderNumber = orderNumber;
        this.projectAccession = projectAccession;
        this.projectName = projectName;
    }

    public int compareTo(Object o) {
        return this.orderNumber.compareTo(((SampleData) o).getOrderNumber());
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public String getProjectAccession() {
        return projectAccession;
    }

    public String getProjectName() {
        return projectName;
    }
}
