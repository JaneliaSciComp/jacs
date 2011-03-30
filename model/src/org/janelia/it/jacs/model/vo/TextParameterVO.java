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
 * User: tsafford
 * Date: Aug 25, 2006
 * Time: 4:15:13 PM
 */
public class TextParameterVO extends ParameterVO {
    public static final String PARAM_TEXT    = "Text";
    private String textValue;
    private int maxLength;

    public String getType() {
        return PARAM_TEXT;
    }

    public TextParameterVO() {
        super();
    }

    public TextParameterVO(String textValue) {
        this.textValue = textValue;
    }

    public TextParameterVO(String textValue, int maxLength) {
        this.maxLength = maxLength;
        this.textValue = textValue;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String newValue) throws ParameterException {
        this.textValue = newValue;
    }

    public String getStringValue() {
        return getTextValue();
    }

    public int getMaxLength() {
        return maxLength;
    }

    // needed for Hibernate
    private void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public boolean isValid() {
        if (null == this.textValue) {
            return false;
        }
        else if (maxLength > 0 && maxLength < this.textValue.length()) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "TextParameterVO{" +
                "textValue='" + textValue + '\'' + "," +
                "maxLength=" + maxLength +
                '}';
    }
}
