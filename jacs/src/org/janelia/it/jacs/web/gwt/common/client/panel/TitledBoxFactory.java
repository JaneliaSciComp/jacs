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

package org.janelia.it.jacs.web.gwt.common.client.panel;

/**
 * Provides a set of enumerated type constants and a factory method for creating the desired TitledBox type
 *
 * @author Michael Press
 */
public class TitledBoxFactory {
    public enum BoxType {
        TITLED_BOX, SECONDARY_BOX, TERTIARY_BOX, CLEAR_BOX
    }

    public static TitledBox createTitledBox(String title, BoxType boxType) {
        return createTitledBox(title, boxType, /*show action links*/ true);
    }

    public static TitledBox createTitledBox(String title, BoxType boxType, boolean showActionLinks) {
        if (boxType == BoxType.TITLED_BOX)
            return new TitledBox(title, showActionLinks);
        else if (boxType == BoxType.SECONDARY_BOX)
            return new SecondaryTitledBox(title, showActionLinks);
        else if (boxType == BoxType.CLEAR_BOX)
            return new ClearTitledBox(title, showActionLinks);
        else if (boxType == BoxType.TERTIARY_BOX)
            return new TertiaryTitledBox(title, showActionLinks);
        else // default to TitledBox
            return new TitledBox(title, showActionLinks);
    }
}
