
package org.janelia.it.jacs.web.gwt.common.client.ui;

/**
 * Listener for widgets to report that an item was definitively selected.
 *
 * @author Michael Press
 */
public interface SelectionListener {
    public void onSelect(String value);

    public void onUnSelect(String value);
}
