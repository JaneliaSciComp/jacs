
package org.janelia.it.jacs.web.gwt.common.client.ui.table.columns;

/**
 * Specifies that a TableColumn will have image content;  this is important because the SortableTable detects
 * Widgets with empty HTML output and inserts blank content so the table cell borders don't get messed up on
 * some browsers.  Images also have empty HTML output, so use of this TableColumn notifies SortableTable not
 * to replace its content with generic blank content.
 *
 * @author Michael Press
 */
public class ImageColumn extends BaseTableColumn {
    public ImageColumn(String displayName) {
        this(displayName, /*isVisible*/ true);
    }

    public ImageColumn(String displayName, boolean isVisible) {
        this(displayName, /*isSortable*/ false, isVisible);
    }

    public ImageColumn(String displayName, boolean isSortable, boolean isVisible) {
        super(displayName, isSortable, isVisible);
    }

    public boolean hasImageContent() {
        return true;
    }
}