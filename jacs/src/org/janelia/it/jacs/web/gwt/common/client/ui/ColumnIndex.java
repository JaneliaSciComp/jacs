
package org.janelia.it.jacs.web.gwt.common.client.ui;

/**
 * @author Tareq Nabeel
 */
public class ColumnIndex extends Index {

    public ColumnIndex(int startingRow) {
        super(startingRow);
    }

    public ColumnIndex increment() {
        return (ColumnIndex) super.increment();
    }
}
