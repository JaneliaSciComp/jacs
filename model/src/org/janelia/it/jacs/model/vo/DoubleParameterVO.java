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
 * Date: Aug 28, 2006
 * Time: 10:58:00 AM
 */
public class DoubleParameterVO extends ParameterVO {
    public static final String PARAM_DOUBLE  = "Double";
    private Double minValue = Double.MIN_VALUE;
    private Double maxValue = Double.MAX_VALUE;
    private Double actualValue;

    /**
     * Required for GWT
     */
    public DoubleParameterVO() {
        super();
    }

    /**
     * Manual boxing
     */
    public DoubleParameterVO(double defaultValue) {
        this(new Double(defaultValue));
    }

    public DoubleParameterVO(Double defaultValue) {
        this.actualValue = defaultValue;
    }

    public String getStringValue() {
        return actualValue.toString();
    }

    /**
     * Manual boxing
     */
    public DoubleParameterVO(double minValue, double maxValue, double defaultValue) {
        this(new Double(minValue), new Double(maxValue), new Double(defaultValue));
    }

    public DoubleParameterVO(Double minValue, Double maxValue, Double defaultValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.actualValue = defaultValue;
    }

    public Double getMinValue() {
        return minValue;
    }

    public Double getMaxValue() {
        return maxValue;
    }

    // These private accessors needed for Hibernate
    private void setMinValue(Double minValue) {
        this.minValue = minValue;
    }

    private void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }

    public void setActualValue(Double newValue) throws ParameterException {
        this.actualValue = newValue;
    }

    public Double getActualValue() {
        return actualValue;
    }

    public boolean isValid() {
        return null != actualValue && !(null == minValue || null == maxValue ||
                minValue > actualValue || maxValue < actualValue);
    }

    public String getType() {
        return PARAM_DOUBLE;
    }

    public String toString() {
        return "DoubleParameterVO{" +
                "minValue=" + minValue +
                "maxValue=" + maxValue +
                "actualValue=" + actualValue +
                '}';
    }

}

