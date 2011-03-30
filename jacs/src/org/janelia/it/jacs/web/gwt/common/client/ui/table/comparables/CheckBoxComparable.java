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

import com.google.gwt.user.client.ui.CheckBox;

/**
 * Wraps a checkbox in a Comparable so a column of Checkboxes can be sorted
 *
 * @author Michael Press
 */
public class CheckBoxComparable implements Comparable {
    private CheckBox _checkbox;

    public CheckBoxComparable(CheckBox checkbox) {
        super();
        _checkbox = checkbox;
    }

    public boolean isChecked() {
        return _checkbox.getValue();
    }

    public int compareTo(Object o) {
        if (o == null)
            return 1;

        CheckBoxComparable other = (CheckBoxComparable) o;

        if (isChecked() == other.isChecked())
            return 0;
        else
            return (isChecked() ? 1 : -1);
    }
}
