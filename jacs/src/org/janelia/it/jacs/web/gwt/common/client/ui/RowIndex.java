
package org.janelia.it.jacs.web.gwt.common.client.ui;

/**
 * Used to keep track of row counter when creating flex tables
 *
 * @author Tareq Nabeel
 */
public class RowIndex extends Index {

    public RowIndex(int startingRow) {
        super(startingRow);
    }

    public int getCurrentRow() {
        return getCurrentValue();
    }

    public RowIndex increment() {
        return (RowIndex) super.increment();
    }
}
