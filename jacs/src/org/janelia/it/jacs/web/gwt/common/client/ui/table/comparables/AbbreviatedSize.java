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

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.FileSizeUtils;

/**
 * A Comparable that allows sorting by the provided long, but overrides toString() to provide a display value
 * in the form "X.Y MB".
 *
 * @author Michael Press
 */
public class AbbreviatedSize implements Comparable {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.AbbreviatedSize");

    long _size;
    String displaySize = null;

    public AbbreviatedSize(long size) {
        _size = size;
    }

    public AbbreviatedSize(String size) {
        try {
            _size = Long.parseLong(size);
        }
        catch (NumberFormatException e) {
            _logger.error("AbbreviatedSize: could not parse size " + size);
        }
    }

    public long getSize() {
        return _size;
    }

    public int compareTo(Object o) {
        // Can't rely on subtraction so just do direct comparison
        long otherSize = ((o == null) ? 0 : ((AbbreviatedSize) o).getSize());
        if (_size > otherSize)
            return 1;
        else if (_size < otherSize)
            return -1;
        else
            return 0;
    }

    public String toString() {
        if (displaySize == null)
            displaySize = FileSizeUtils.abbreviateFileSizeMB(_size);

        return displaySize;
    }
}
