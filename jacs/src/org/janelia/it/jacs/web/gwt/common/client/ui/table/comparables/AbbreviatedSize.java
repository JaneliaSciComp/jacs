
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
