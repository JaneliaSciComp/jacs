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

package org.janelia.it.jacs.web.gwt.common.shared.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Sep 26, 2007
 * Time: 9:13:01 AM
 */
public class ChartDataEntry implements Serializable, IsSerializable, Comparable {

    private String category;
    private String name;
    private String description;
    private Number value;
    private Number percentValue;

    public ChartDataEntry() {
    }

    public ChartDataEntry(String name, Number value) {
        this.name = name;
        this.value = value;
    }

    public ChartDataEntry(String category, String name, Number value) {
        this.category = category;
        this.name = name;
        this.value = value;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    public Number getPercentValue() {
        return percentValue;
    }

    public void setPercentValue(Number percentValue) {
        this.percentValue = percentValue;
    }

    public int compareTo(Object o) {
        if (o == null)
            return 1;

        ChartDataEntry other = (ChartDataEntry) o;
        return (this.value.doubleValue() > other.getValue().doubleValue() ? 1 : (this.value.doubleValue() == other.getValue().doubleValue() ? 0 : -1));
    }

}
