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

package org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables;

/**
 * Comparable that sorts on a provided int value but outputs a formatted String
 */
public class IntegerString implements Comparable {
    private long _value;
    private String _formattedValue;

    public IntegerString(Integer value, String displayValue) {
        _value = value.longValue();
        _formattedValue = displayValue;
    }

    public IntegerString(Long value, String displayValue) {
        _value = value.longValue();
        _formattedValue = displayValue;
    }

    public IntegerString(int value, String displayValue) {
        _value = value;
        _formattedValue = displayValue;
    }

    public IntegerString(long value, String displayValue) {
        _value = value;
        _formattedValue = displayValue;
    }

    public String toString() {
        return _formattedValue;
    }

    public long getValue() {
        return _value;
    }

    public int compareTo(Object o) {
        if (o == null)
            return 1;

        IntegerString other = (IntegerString) o;
        return (_value > other.getValue() ? 1 : (_value == other.getValue() ? 0 : -1));
    }
}
