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

package org.janelia.it.jacs.web.gwt.common.client.model.tasks;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author Michael Press
 */
public class LegendItem implements Comparable, IsSerializable {
    private String _itemAcc;
    private String _displayValue;
    private String _red;
    private String _green;
    private String _blue;
    private Float _hue;

    /**
     * required for GWT
     */
    public LegendItem() {
    }

    public LegendItem(String itemAcc, String displayValue, String red, String green, String blue, Float hue) {
        _itemAcc = itemAcc;
        _displayValue = displayValue;
        _red = red;
        _green = green;
        _blue = blue;
        _hue = hue;
    }

    public String getBlue() {
        return _blue;
    }

    public void setBlue(String blue) {
        _blue = blue;
    }

    public String getDisplayValue() {
        return _displayValue;
    }

    public void setDisplayValue(String displayValue) {
        _displayValue = displayValue;
    }

    public String getGreen() {
        return _green;
    }

    public void setGreen(String green) {
        _green = green;
    }

    public String getId() {
        return _itemAcc;
    }

    public void setId(String itemAcc) {
        _itemAcc = itemAcc;
    }

    public String getRed() {
        return _red;
    }

    public void setRed(String red) {
        _red = red;
    }


    public Float getHue() {
        return _hue;
    }

    public int compareTo(Object o) {
        LegendItem li1 = (LegendItem) o;
        return this.getHue().compareTo(li1.getHue());
    }
}
