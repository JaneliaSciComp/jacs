
package org.janelia.it.jacs.web.gwt.common.client.ui.table.columns;

/**
 * @author Michael Press
 */
public interface TableColumn {
    public String getTextStyleName();   // header text style

    public String getDisplayName();     // column header text

    public String getPopupText();       // detailed text for popup (can be null for no popup)

    public boolean isSortable();

    public boolean isVisible();

    public boolean hasImageContent();      // may appear to have null content but actually showing an image

    public void setVisible(boolean visible);

    public void setDisplayName(String displayName);
}
