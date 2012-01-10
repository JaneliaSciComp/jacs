
package org.janelia.it.jacs.web.gwt.common.client.ui.table.columns;

/**
 * Identical to ImageColumn for purposes of getting checkboxes to not be ignored by the SortableTable due to having
 * empty inner HTML.
 *
 * @author Michael Press
 */
public class CheckBoxColumn extends ImageColumn {
    public CheckBoxColumn(String displayName) {
        super(displayName);
    }
}

