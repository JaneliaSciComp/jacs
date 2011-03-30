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

package org.janelia.it.jacs.model.common;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/*
 * SortArgument
 */
public class SortArgument implements Serializable, IsSerializable {
    public static int SORT_NOTSET = 0;
    public static int SORT_ASC = 1;
    public static int SORT_DESC = 2;

    private String sortArgumentName;
    private int sortDirection;

    public SortArgument() {
    }

    public SortArgument(String sortArgumentName) {
        this(sortArgumentName, SORT_ASC);
    }

    public SortArgument(SortArgument sortArg) {
        this(sortArg.sortArgumentName, sortArg.sortDirection);
    }

    public SortArgument(String sortArgumentName, int sortDirection) {
        setSortArgumentName(sortArgumentName);
        setSortDirection(sortDirection);
    }

    public String getSortArgumentName() {
        return sortArgumentName;
    }

    public void setSortArgumentName(String sortArgumentName) {
        this.sortArgumentName = sortArgumentName;
    }

    public int getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(int sortDirection) {
        if (sortDirection == SORT_ASC) {
            this.sortDirection = SORT_ASC;
        }
        else if (sortDirection == SORT_DESC) {
            this.sortDirection = SORT_DESC;
        }
        else {
            this.sortDirection = SORT_NOTSET;
        }
    }

    public boolean isAsc() {
        return this.sortDirection == SORT_ASC;
    }

    public boolean isDesc() {
        return this.sortDirection == SORT_DESC;
    }

    public boolean equals(Object o) {
        boolean bresult = false;
        if (o instanceof SortArgument) {
            SortArgument a = (SortArgument) o;
            bresult = sortArgumentName != null && sortArgumentName.length() > 0 &&
                    a.sortArgumentName != null && a.sortArgumentName.length() > 0 &&
                    sortArgumentName.equals(a.sortArgumentName) &&
                    sortDirection == a.sortDirection;
        }
        return bresult;
    }

    public String toString() {
        if (isAsc()) {
            return sortArgumentName + " asc";
        }
        else if (isDesc()) {
            return sortArgumentName + " desc";
        }
        else {
            return "none";
        }
    }

}
