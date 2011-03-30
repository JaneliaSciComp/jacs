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

package org.janelia.it.jacs.model.vo;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2006
 * Time: 11:25:35 AM
 */
public class LongParameterVO extends ParameterVO {

    /**
     * NOTE: As this class is used by GWT, and it's values converted into javascript, we have to be careful exactly
     * what we set the values of Max and Min to.  Calling Long.MIN Long.MAX was preventing the bridge from javascript
     * back to this class.  This must always be kept in mind when setting min and max values.
     * For this reason I am forcing people to set min and max when using this object;
     */
    public static final String PARAM_LONG    = "Long";
    public static final transient Long LONG_MIN = (long) -1000000;
    public static final transient Long LONG_MAX = (long) 1000000;
    private Long minValue;
    private Long maxValue;
    private Long actualValue;

    /**
     * For GWT use
     */
    public LongParameterVO() {
        super();
    }

    public LongParameterVO(Long defaultValue) {
        this.minValue = LONG_MIN;
        this.maxValue = LONG_MAX;
        this.actualValue = defaultValue;
    }

    public LongParameterVO(Long minValue, Long maxValue, Long defaultValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.actualValue = defaultValue;
    }

    public Long getMinValue() {
        return minValue;
    }

    public Long getMaxValue() {
        return maxValue;
    }

    // These private accessors needed for Hibernate
    private void setMinValue(Long minValue) {
        this.minValue = minValue;
    }

    private void setMaxValue(Long maxValue) {
        this.maxValue = maxValue;
    }

    public void setActualValue(Long newValue) throws ParameterException {
        this.actualValue = newValue;
    }

    public Long getActualValue() {
        return actualValue;
    }

    public String getStringValue() {
        return actualValue.toString();
    }

    public boolean isValid() {
        return null != this.actualValue && !((null == minValue) || (null == maxValue) ||
                (actualValue < minValue) ||
                (actualValue > maxValue));
    }

    public String getType() {
        return PARAM_LONG;
    }

    public String toString() {
        return "LongParameterVO{" +
                "minValue=" + minValue +
                "maxValue=" + maxValue +
                "actualValue=" + actualValue +
                '}';
    }

}
