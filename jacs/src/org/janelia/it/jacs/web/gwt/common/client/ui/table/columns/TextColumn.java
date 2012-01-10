
package org.janelia.it.jacs.web.gwt.common.client.ui.table.columns;

/**
 * @author Michael Press
 */
public class TextColumn extends BaseTableColumn {
    public TextColumn(String displayName) {
        this(displayName, true);
    }

    public TextColumn(String displayName, boolean isSortable) {
        this(displayName, isSortable, true);
    }

    public TextColumn(String displayName, String popupText) {
        super(displayName, popupText);
    }

    public TextColumn(String displayName, boolean isSortable, boolean isVisible) {
        super(displayName, isSortable, isVisible);
    }

}
