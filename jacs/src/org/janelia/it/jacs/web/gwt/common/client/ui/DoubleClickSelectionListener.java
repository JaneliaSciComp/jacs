
package org.janelia.it.jacs.web.gwt.common.client.ui;

/**
 * Listener for widgets to report that an item was selected with a double click.  There's no concept of unselecting
 * with a double click.
 *
 * @author Michael Press
 */
public interface DoubleClickSelectionListener {
    public void onSelect(String value);
}
