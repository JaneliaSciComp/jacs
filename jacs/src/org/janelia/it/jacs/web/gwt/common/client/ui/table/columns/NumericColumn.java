
package org.janelia.it.jacs.web.gwt.common.client.ui.table.columns;

/**
 * @author Michael Press
 */
public class NumericColumn extends BaseTableColumn {
    public NumericColumn(String displayName) {
        super(displayName);
    }

    public NumericColumn(String displayName, String popupText) {
        super(displayName, popupText);
    }

    public NumericColumn(String displayName, String popupText, boolean isSortable, boolean isVisible) {
        super(displayName, popupText, isSortable, isVisible);
    }

    public String getTextStyleName() {
        return "tableColumnNumeric";
    }
}
